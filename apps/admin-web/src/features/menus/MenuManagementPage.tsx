import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState, type CSSProperties, type ReactNode } from "react";
import {
  DndContext,
  DragOverlay,
  KeyboardSensor,
  MouseSensor,
  TouchSensor,
  closestCenter,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragMoveEvent,
  type DragStartEvent,
} from "@dnd-kit/core";
import { SortableContext, sortableKeyboardCoordinates, useSortable, verticalListSortingStrategy } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { ArrowDown, ArrowLeft, ArrowRight, ArrowUp, GripVertical, Save } from "lucide-react";
import { useTranslation } from "react-i18next";
import {
  createMenu,
  getMenus,
  reorderMenus,
  updateMenu,
  type ManagedMenu,
} from "@/features/menus/api/menusApi";
import { ApiClientError } from "@/api/client";
import { menuKeys } from "@/features/menus/hook/menuKeys";
import { LoadingPanel, PageHeader, StatusPanel } from "@/components/layout/Page";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button, buttonVariants } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { MenuDialog } from "@/features/menus/component/MenuDialog";
import { buildMenuTree, moveMenu, projectMenuMove, type DropPlacement, type MenuTreeNode } from "@/features/menus/menuTree";
import {
  Item,
  ItemActions,
  ItemContent,
  ItemDescription,
  ItemGroup,
  ItemTitle,
} from "@/components/ui/item";

function SortableMenuNode({
  node,
  depth,
  activeId,
  actions,
}: {
  node: MenuTreeNode;
  depth: number;
  activeId: string | null;
  actions: ReactNode;
}) {
  const { t } = useTranslation("menus");
  const { attributes, isDragging, listeners, setActivatorNodeRef, setNodeRef, transform, transition } = useSortable({ id: node.menu.id });
  const style: CSSProperties = {
    marginInlineStart: `${(depth - 1) * 24}px`,
    opacity: isDragging ? 0.35 : 1,
    transform: CSS.Transform.toString(transform),
    transition,
  };
  return (
    <div className="flex flex-col gap-1" key={node.menu.id}>
      <div data-menu-id={node.menu.id} data-dragging={activeId === node.menu.id} ref={setNodeRef} style={style}>
        <Item className="bg-sidebar text-sidebar-foreground" variant="outline">
          <button
            aria-label={t("dragHandle", { name: node.menu.label })}
            className={buttonVariants({ className: "cursor-grab touch-none", size: "icon-sm", variant: "ghost" })}
            ref={setActivatorNodeRef}
            type="button"
            {...attributes}
            {...listeners}
          ><GripVertical /></button>
          <ItemContent>
            <ItemTitle>{node.menu.label}</ItemTitle>
            <ItemDescription className="break-all">{node.menu.route} · {node.menu.requiredPermission ?? t("public")}</ItemDescription>
          </ItemContent>
          <ItemActions>{actions}</ItemActions>
        </Item>
      </div>
    </div>
  );
}

export function MenuManagementPage() {
  const { t } = useTranslation("menus");
  const { t: common } = useTranslation("common");
  const queryClient = useQueryClient();
  const menus = useQuery({ queryKey: menuKeys.all, queryFn: getMenus });
  const [error, setError] = useState<string | null>(null);
  const [draftMenus, setDraftMenus] = useState<ManagedMenu[]>([]);
  const [draggedId, setDraggedId] = useState<string | null>(null);
  const [projectedDepth, setProjectedDepth] = useState<number | null>(null);
  const [dirty, setDirty] = useState(false);
  const sensors = useSensors(
    useSensor(MouseSensor, { activationConstraint: { distance: 6 } }),
    useSensor(TouchSensor, { activationConstraint: { delay: 180, tolerance: 5 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );
  useEffect(() => {
    if (menus.data && !dirty) setDraftMenus(menus.data);
  }, [dirty, menus.data]);
  const save = useMutation({
    mutationFn: createMenu,
    onSuccess: async () => {
      setError(null);
      await queryClient.invalidateQueries({ queryKey: menuKeys.root });
    },
    onError: (e) =>
      setError(e instanceof ApiClientError ? e.message : t("saveError")),
  });
  const toggle = useMutation({
    mutationFn: (menu: ManagedMenu) =>
      updateMenu(menu.id, { ...menu, enabled: !menu.enabled }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: menuKeys.root });
    },
    onError: (e) =>
      setError(e instanceof ApiClientError ? e.message : t("statusError")),
  });
  const edit = useMutation({
    mutationFn: ({ id, value }: { id: string; value: ManagedMenu }) =>
      updateMenu(id, value),
    onSuccess: async () => {
      setError(null);
      await queryClient.invalidateQueries({ queryKey: menuKeys.root });
    },
    onError: (e) =>
      setError(e instanceof ApiClientError ? e.message : t("saveError")),
  });
  const reorder = useMutation({
    mutationFn: reorderMenus,
    onSuccess: async (value) => {
      setDraftMenus(value);
      setDirty(false);
      setError(null);
      await queryClient.invalidateQueries({ queryKey: menuKeys.root });
    },
    onError: (e) => setError(e instanceof ApiClientError ? e.message : t("reorderError")),
  });

  const applyMove = (sourceId: string, targetId: string, placement: DropPlacement) => {
    const next = moveMenu(draftMenus, sourceId, targetId, placement);
    if (!next) {
      setError(t("depthError"));
      return;
    }
    setDraftMenus(next);
    setDirty(true);
    setError(null);
  };

  const siblingsOf = (menu: ManagedMenu) => draftMenus
    .filter((candidate) => candidate.parentId === menu.parentId)
    .sort((a, b) => a.order - b.order);

  const renderNode = (node: MenuTreeNode, depth: number): ReactNode => {
    const menu = node.menu;
    const siblings = siblingsOf(menu);
    const index = siblings.findIndex((candidate) => candidate.id === menu.id);
    const parent = menu.parentId ? draftMenus.find((candidate) => candidate.id === menu.parentId) : undefined;
    return (
      <div className="flex flex-col gap-2" key={menu.id}>
        <SortableMenuNode
          actions={<>
            <Button aria-label={t("moveUp", { name: menu.label })} disabled={index <= 0} onClick={() => applyMove(menu.id, siblings[index - 1]!.id, "before")} size="icon-sm" variant="ghost"><ArrowUp /></Button>
            <Button aria-label={t("moveDown", { name: menu.label })} disabled={index >= siblings.length - 1} onClick={() => applyMove(menu.id, siblings[index + 1]!.id, "after")} size="icon-sm" variant="ghost"><ArrowDown /></Button>
            <Button aria-label={t("indent", { name: menu.label })} disabled={index <= 0 || depth >= 3} onClick={() => applyMove(menu.id, siblings[index - 1]!.id, "inside")} size="icon-sm" variant="ghost"><ArrowRight /></Button>
            <Button aria-label={t("outdent", { name: menu.label })} disabled={!parent} onClick={() => parent && applyMove(menu.id, parent.id, "after")} size="icon-sm" variant="ghost"><ArrowLeft /></Button>
            <MenuDialog menu={menu} onSave={(value) => edit.mutateAsync({ id: menu.id, value })} pending={edit.isPending} />
            <Button disabled={toggle.isPending} onClick={() => toggle.mutate(menu)} size="sm" variant="outline">{menu.enabled ? common("disable") : common("enable")}</Button>
          </>}
          activeId={draggedId}
          depth={depth}
          node={node}
        />
        {node.children.map((child) => renderNode(child, depth + 1))}
      </div>
    );
  };
  const depthOf = (id: string) => {
    let depth = 1;
    let parentId = draftMenus.find((menu) => menu.id === id)?.parentId;
    while (parentId) {
      depth += 1;
      parentId = draftMenus.find((menu) => menu.id === parentId)?.parentId;
    }
    return depth;
  };
  const onDragStart = (event: DragStartEvent) => {
    const id = String(event.active.id);
    setDraggedId(id);
    setProjectedDepth(depthOf(id));
  };
  const onDragMove = (event: DragMoveEvent) => {
    if (!event.over) return;
    const projection = projectMenuMove(
      draftMenus,
      String(event.active.id),
      String(event.over.id),
      event.delta.x,
    );
    setProjectedDepth(projection?.depth ?? depthOf(String(event.active.id)));
  };
  const onDragEnd = (event: DragEndEvent) => {
    setDraggedId(null);
    setProjectedDepth(null);
    if (!event.over) return;
    const sourceId = String(event.active.id);
    const targetId = String(event.over.id);
    const projection = projectMenuMove(draftMenus, sourceId, targetId, event.delta.x);
    if (!projection) return;
    setDraftMenus(projection.menus);
    setDirty(true);
    setError(null);
  };
  return (
    <>
      <PageHeader
        actions={
          <MenuDialog
            onSave={(value) => save.mutateAsync(value)}
            pending={save.isPending}
          />
        }
        description={t("description")}
        eyebrow={t("eyebrow")}
        title={t("title")}
      />
      <Card>
        <CardHeader>
          <CardTitle>{t("allTitle")}</CardTitle>
        </CardHeader>
        <CardContent>
          {error && (
            <Alert className="mb-4" variant="destructive">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
          {menus.isPending && (
            <LoadingPanel label={t("loading")} />
          )}
          {menus.isError && (
            <Alert variant="destructive" role="alert">
              <AlertDescription>{t("loadError")}</AlertDescription>
            </Alert>
          )}
          {menus.data?.length === 0 && <StatusPanel>{t("empty")}</StatusPanel>}
          {menus.data && (
            <div className="flex flex-col gap-4">
              <div className="flex items-center justify-between gap-4">
                <p className="text-muted-foreground text-sm">{t("dragDescription")}</p>
                <Button disabled={!dirty || reorder.isPending} onClick={() => reorder.mutate(draftMenus.map(({ id, parentId, order }) => ({ id, parentId, order })))}>
                  <Save data-icon="inline-start" />{t("saveOrder")}
                </Button>
              </div>
              <DndContext collisionDetection={closestCenter} onDragCancel={() => { setDraggedId(null); setProjectedDepth(null); }} onDragEnd={onDragEnd} onDragMove={onDragMove} onDragStart={onDragStart} sensors={sensors}>
                <SortableContext items={draftMenus.map((menu) => menu.id)} strategy={verticalListSortingStrategy}>
                  <ItemGroup>{buildMenuTree(draftMenus).map((node) => renderNode(node, 1))}</ItemGroup>
                </SortableContext>
                <DragOverlay>{draggedId ? <div className="flex items-center gap-3 rounded-md border bg-background px-4 py-3 shadow-lg"><span>{draftMenus.find((menu) => menu.id === draggedId)?.label}</span><span className="text-muted-foreground text-xs">{t("depthLabel", { depth: projectedDepth ?? depthOf(draggedId) })}</span></div> : null}</DragOverlay>
              </DndContext>
            </div>
          )}
        </CardContent>
      </Card>
    </>
  );
}
