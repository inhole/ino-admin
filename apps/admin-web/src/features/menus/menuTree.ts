import type { ManagedMenu } from "./api/menusApi";

export type MenuTreeNode = { menu: ManagedMenu; children: MenuTreeNode[] };
export type DropPlacement = "before" | "inside" | "after";
export type ProjectedMenuMove = { menus: ManagedMenu[]; depth: number };

export function buildMenuTree(menus: ManagedMenu[]): MenuTreeNode[] {
  const nodes = new Map(menus.map((menu) => [menu.id, { menu, children: [] as MenuTreeNode[] }]));
  const roots: MenuTreeNode[] = [];
  [...menus].sort((a, b) => a.order - b.order).forEach((menu) => {
    const node = nodes.get(menu.id)!;
    const parent = menu.parentId ? nodes.get(menu.parentId) : undefined;
    if (parent) parent.children.push(node);
    else roots.push(node);
  });
  return roots;
}

function contains(node: MenuTreeNode, id: string): boolean {
  return node.menu.id === id || node.children.some((child) => contains(child, id));
}

function height(node: MenuTreeNode): number {
  return 1 + Math.max(0, ...node.children.map(height));
}

function flatten(nodes: MenuTreeNode[], parentId: string | null = null): ManagedMenu[] {
  return nodes.flatMap((node, index) => {
    const menu = { ...node.menu, parentId, order: (index + 1) * 10 };
    return [menu, ...flatten(node.children, menu.id)];
  });
}

export function moveMenu(
  menus: ManagedMenu[],
  draggedId: string,
  targetId: string,
  placement: DropPlacement,
): ManagedMenu[] | null {
  if (draggedId === targetId) return null;
  const roots = buildMenuTree(menus);
  let dragged: MenuTreeNode | undefined;
  let target: MenuTreeNode | undefined;
  let targetDepth = 0;

  const locate = (nodes: MenuTreeNode[], depth: number): void => nodes.forEach((node) => {
    if (node.menu.id === draggedId) dragged = node;
    if (node.menu.id === targetId) { target = node; targetDepth = depth; }
    locate(node.children, depth + 1);
  });
  locate(roots, 1);
  if (!dragged || !target || contains(dragged, targetId)) return null;
  const draggedNode = dragged;
  const targetNode = target;

  const detach = (nodes: MenuTreeNode[]): boolean => {
    const index = nodes.findIndex((node) => node.menu.id === draggedId);
    if (index >= 0) { nodes.splice(index, 1); return true; }
    return nodes.some((node) => detach(node.children));
  };
  detach(roots);

  if (placement === "inside") {
    if (targetDepth + height(draggedNode) > 3) return null;
    targetNode.children.push(draggedNode);
  } else {
    const insert = (nodes: MenuTreeNode[], depth: number): boolean => {
      const index = nodes.findIndex((node) => node.menu.id === targetId);
      if (index >= 0) {
        if (depth + height(draggedNode) - 1 > 3) return false;
        nodes.splice(index + (placement === "after" ? 1 : 0), 0, draggedNode);
        return true;
      }
      return nodes.some((node) => insert(node.children, depth + 1));
    };
    if (!insert(roots, 1)) return null;
  }
  return flatten(roots);
}

type FlatMenu = { menu: ManagedMenu; depth: number };

function flattenWithDepth(nodes: MenuTreeNode[], depth = 1): FlatMenu[] {
  return nodes.flatMap((node) => [
    { menu: node.menu, depth },
    ...flattenWithDepth(node.children, depth + 1),
  ]);
}

export function projectMenuMove(
  menus: ManagedMenu[],
  sourceId: string,
  targetId: string,
  horizontalOffset: number,
  indentationWidth = 24,
): ProjectedMenuMove | null {
  const flat = flattenWithDepth(buildMenuTree(menus));
  const sourceIndex = flat.findIndex((item) => item.menu.id === sourceId);
  const targetIndex = flat.findIndex((item) => item.menu.id === targetId);
  if (sourceIndex < 0 || targetIndex < 0 || sourceId === targetId) return null;
  const source = flat[sourceIndex]!;
  let blockEnd = sourceIndex + 1;
  while (blockEnd < flat.length && flat[blockEnd]!.depth > source.depth) blockEnd++;
  const movingBlock = flat.slice(sourceIndex, blockEnd);
  if (movingBlock.some((item) => item.menu.id === targetId)) return null;

  const remaining = [...flat.slice(0, sourceIndex), ...flat.slice(blockEnd)];
  const remainingTargetIndex = remaining.findIndex((item) => item.menu.id === targetId);
  if (remainingTargetIndex < 0) return null;

  let insertionIndex = remainingTargetIndex;
  if (sourceIndex < targetIndex) {
    insertionIndex++;
    const targetDepth = remaining[remainingTargetIndex]!.depth;
    while (insertionIndex < remaining.length && remaining[insertionIndex]!.depth > targetDepth) insertionIndex++;
  }

  const subtreeHeight = Math.max(...movingBlock.map((item) => item.depth - source.depth + 1));
  const requestedDepth = source.depth + Math.round(horizontalOffset / indentationWidth);
  const boundedRequestedDepth = Math.max(1, Math.min(4 - subtreeHeight, requestedDepth));
  while (
    insertionIndex > 0
    && insertionIndex < remaining.length
    && remaining[insertionIndex]!.depth > boundedRequestedDepth
  ) insertionIndex--;
  const previousDepth = insertionIndex > 0 ? remaining[insertionIndex - 1]!.depth : 0;
  const projectedDepth = Math.min(previousDepth + 1, boundedRequestedDepth);
  const depthDelta = projectedDepth - source.depth;
  const projectedBlock = movingBlock.map((item) => ({ ...item, depth: item.depth + depthDelta }));
  const projected = [
    ...remaining.slice(0, insertionIndex),
    ...projectedBlock,
    ...remaining.slice(insertionIndex),
  ];

  const ancestors: string[] = [];
  const siblingOrders = new Map<string, number>();
  const moved = projected.map(({ menu, depth }) => {
    const parentId = depth === 1 ? null : ancestors[depth - 2] ?? null;
    const siblingKey = parentId ?? "__root__";
    const order = (siblingOrders.get(siblingKey) ?? 0) + 10;
    siblingOrders.set(siblingKey, order);
    ancestors[depth - 1] = menu.id;
    ancestors.length = depth;
    return { ...menu, parentId, order };
  });
  return { menus: moved, depth: projectedDepth };
}
