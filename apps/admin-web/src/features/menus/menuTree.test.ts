import { describe, expect, it } from "vitest";
import type { ManagedMenu } from "./api/menusApi";
import { buildMenuTree, moveMenu, projectMenuMove } from "./menuTree";

const menu = (id: string, parentId: string | null, order: number): ManagedMenu => ({
  id, parentId, order, label: id, route: `/${id}`, icon: "menu", requiredPermission: null, enabled: true,
});

describe("menuTree", () => {
  it("builds a sorted three-level tree", () => {
    const tree = buildMenuTree([menu("leaf", "child", 10), menu("root", null, 10), menu("child", "root", 10)]);
    expect(tree[0]?.children[0]?.children[0]?.menu.id).toBe("leaf");
  });

  it("moves a menu inside another menu and normalizes sibling orders", () => {
    const result = moveMenu([menu("a", null, 10), menu("b", null, 20), menu("c", null, 30)], "c", "a", "inside");
    expect(result?.find((item) => item.id === "c")).toMatchObject({ parentId: "a", order: 10 });
    expect(result?.find((item) => item.id === "b")).toMatchObject({ parentId: null, order: 20 });
  });

  it("rejects moves that create a fourth level or a cycle", () => {
    const menus = [menu("one", null, 10), menu("two", "one", 10), menu("three", "two", 10), menu("other", null, 20)];
    expect(moveMenu(menus, "other", "three", "inside")).toBeNull();
    expect(moveMenu(menus, "one", "three", "inside")).toBeNull();
  });

  it("projects depth from horizontal movement in stable 24px steps", () => {
    const menus = [menu("a", null, 10), menu("b", null, 20), menu("c", null, 30)];
    const nested = projectMenuMove(menus, "c", "b", 26);
    expect(nested?.menus.find((item) => item.id === "c")?.parentId).not.toBeNull();
    expect(nested?.depth).toBe(2);
  });

  it("outdents while preserving the vertical drop position and clamps to three levels", () => {
    const menus = [menu("a", null, 10), menu("b", "a", 10), menu("c", "b", 10), menu("d", null, 20)];
    const outdented = projectMenuMove(menus, "c", "d", -28);
    expect(outdented?.menus.find((item) => item.id === "c")?.parentId).toBe("d");
    expect(outdented?.depth).toBe(2);
    expect(projectMenuMove(menus, "d", "c", 72)?.depth).toBeLessThanOrEqual(3);
  });

  it("keeps depth when moving only vertically across another depth", () => {
    const menus = [menu("a", null, 10), menu("b", "a", 10), menu("c", null, 20)];
    const moved = projectMenuMove(menus, "c", "b", 0);
    expect(moved?.menus.find((item) => item.id === "c")?.parentId).toBeNull();
    expect(moved?.menus.find((item) => item.id === "b")?.parentId).toBe("a");
    expect(moved?.depth).toBe(1);
  });

  it("uses horizontal ranges only to change depth", () => {
    const menus = [menu("a", null, 10), menu("b", "a", 10), menu("c", "b", 10), menu("d", null, 20)];
    expect(projectMenuMove(menus, "d", "c", 23)?.depth).toBe(2);
    expect(projectMenuMove(menus, "d", "c", 49)?.depth).toBe(3);
    expect(projectMenuMove(menus, "c", "d", -23)?.depth).toBe(2);
    expect(projectMenuMove(menus, "c", "d", -49)?.depth).toBe(1);
  });
});
