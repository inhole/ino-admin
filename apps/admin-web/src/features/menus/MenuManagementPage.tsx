import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { forwardRef, useEffect, useState, type ReactNode } from "react";
import { SortableTree, type TreeItemComponentProps, type TreeItems } from "dnd-kit-sortable-tree";
import { GripVertical, Save } from "lucide-react";
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
import { toast } from "@/components/ui/toast";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { MenuDialog } from "@/features/menus/component/MenuDialog";
import { buildMenuTree, type MenuTreeNode } from "@/features/menus/menuTree";
import {
  Item,
  ItemActions,
  ItemContent,
  ItemDescription,
  ItemTitle,
} from "@/components/ui/item";
import { cn } from "@/lib/utils";

type SortableMenuData = { menu: ManagedMenu; actions: ReactNode };

type MenuTreeLike = { children?: MenuTreeLike[] };

function menuSubtreeHeight(item: MenuTreeLike): number {
  return 1 + Math.max(0, ...(item.children ?? []).map((child) => menuSubtreeHeight(child)));
}

const SortableMenuItem = forwardRef<HTMLDivElement, TreeItemComponentProps<SortableMenuData>>((props, ref) => {
  const { t } = useTranslation("menus");
  const placeholder = props.ghost;
  return (
    <li
      className={cn("list-none", !props.clone && "mb-2 last:mb-0")}
      data-depth={props.depth + 1}
      data-dragging={props.ghost ? "true" : undefined}
      data-menu-id={props.item.id}
      ref={props.wrapperRef}
      style={{ ...props.style, paddingInlineStart: props.clone ? 0 : props.depth * props.indentationWidth }}
    >
      <div
        className={cn(
          "rounded-md",
          placeholder && "border border-dashed border-primary/50 bg-primary/5",
          props.clone && "translate-x-2 translate-y-2 shadow-xl",
        )}
        ref={ref}
        style={placeholder ? { minHeight: Math.max(72, (props.childCount ?? 1) * 81) } : undefined}
      >
        {!placeholder && (
          <Item className={cn("bg-background text-foreground", props.isOver && "border-primary ring-2 ring-primary/15")} variant="outline">
            <button
              aria-label={t("dragHandle", { name: props.item.menu.label })}
              className={buttonVariants({ className: "cursor-grab touch-none active:cursor-grabbing", size: "icon-sm", variant: "ghost" })}
              type="button"
              {...props.handleProps}
            >
              <GripVertical />
            </button>
            <ItemContent>
              <ItemTitle>{props.item.menu.label}</ItemTitle>
              <ItemDescription className="break-all">{props.item.menu.route} · {props.item.menu.requiredPermission ?? t("public")}</ItemDescription>
            </ItemContent>
            <div
              aria-hidden={props.clone || undefined}
              className={cn(props.clone && "invisible")}
              onPointerDown={props.clone ? undefined : (event) => event.stopPropagation()}
            >
              <ItemActions>{props.item.actions}</ItemActions>
            </div>
          </Item>
        )}
      </div>
    </li>
  );
});
SortableMenuItem.displayName = "SortableMenuItem";

export function MenuManagementPage() {
  const { t } = useTranslation("menus");
  const { t: common } = useTranslation("common");
  const queryClient = useQueryClient();
  const menus = useQuery({ queryKey: menuKeys.all, queryFn: getMenus });
  const [error, setError] = useState<string | null>(null);
  const [draftMenus, setDraftMenus] = useState<ManagedMenu[]>([]);
  const [dirty, setDirty] = useState(false);
  useEffect(() => {
    if (menus.data && !dirty) setDraftMenus(menus.data);
  }, [dirty, menus.data]);
  const save = useMutation({
    mutationFn: createMenu,
    onSuccess: async () => {
      setError(null);
      toast.add({ title: t("createSuccess"), type: "success" });
      await queryClient.invalidateQueries({ queryKey: menuKeys.root });
    },
    onError: (e) =>
      setError(e instanceof ApiClientError ? e.message : t("saveError")),
  });
  const toggle = useMutation({
    mutationFn: (menu: ManagedMenu) =>
      updateMenu(menu.id, { ...menu, enabled: !menu.enabled }),
    onSuccess: async () => {
      toast.add({ title: t("statusSuccess"), type: "success" });
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
      toast.add({ title: t("updateSuccess"), type: "success" });
      await queryClient.invalidateQueries({ queryKey: menuKeys.root });
    },
    onError: (e) =>
      setError(e instanceof ApiClientError ? e.message : t("saveError")),
  });
  const reorder = useMutation({
    mutationFn: reorderMenus,
    onSuccess: async (value) => {
      queryClient.setQueryData(menuKeys.all, value);
      setDraftMenus(value);
      setDirty(false);
      setError(null);
      toast.add({ title: t("reorderSuccess"), type: "success" });
      await queryClient.invalidateQueries({ queryKey: menuKeys.root });
    },
    onError: (e) => setError(e instanceof ApiClientError ? e.message : t("reorderError")),
  });

  const toSortableItems = (nodes: MenuTreeNode[], depth = 1): TreeItems<SortableMenuData> => nodes.map((node) => ({
    id: node.menu.id,
    menu: node.menu,
    actions: <>
      <MenuDialog menu={node.menu} onSave={(value) => edit.mutateAsync({ id: node.menu.id, value })} pending={edit.isPending} />
      <Button disabled={toggle.isPending} onClick={() => toggle.mutate(node.menu)} size="sm" variant="outline">{node.menu.enabled ? common("disable") : common("enable")}</Button>
    </>,
    canHaveChildren: (dragged) => depth + menuSubtreeHeight(dragged) <= 3,
    children: toSortableItems(node.children, depth + 1),
  }));
  const sortableItems = toSortableItems(buildMenuTree(draftMenus));

  const applyTree = (items: TreeItems<SortableMenuData>) => {
    const flatten = (nodes: TreeItems<SortableMenuData>, parentId: string | null = null): ManagedMenu[] => nodes.flatMap((node, index) => [
      { ...node.menu, parentId, order: (index + 1) * 10 },
      ...flatten(node.children ?? [], String(node.id)),
    ]);
    setDraftMenus(flatten(items));
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
              <div className="w-full rounded-xl border bg-muted/20 p-4">
                <SortableTree
                  TreeItemComponent={SortableMenuItem}
                  canRootHaveChildren={(dragged) => menuSubtreeHeight(dragged) <= 3}
                  dropAnimation={null}
                  indentationWidth={50}
                  items={sortableItems}
                  onItemsChanged={(items, reason) => { if (reason.type === "dropped") applyTree(items); }}
                  pointerSensorOptions={{ activationConstraint: { distance: 3 } }}
                  sortableProps={{
                    animateLayoutChanges: () => false,
                    transition: { duration: 180, easing: "cubic-bezier(0.2, 0, 0, 1)" },
                  }}
                />
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </>
  );
}
