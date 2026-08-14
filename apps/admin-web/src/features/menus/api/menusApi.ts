import { request } from "@/api/client";

export interface MenuItem {
  id: string;
  label: string;
  route: string;
  icon: "layout-dashboard" | "users" | "key-round" | "menu" | "file";
  order: number;
  children: MenuItem[];
}

export interface ManagedMenu extends Omit<MenuItem, "children"> {
  parentId: string | null;
  requiredPermission: string | null;
  enabled: boolean;
}

export function getMyMenus() {
  return request<MenuItem[]>("/api/v1/menus/me");
}
export function getMenus() {
  return request<ManagedMenu[]>("/api/v1/menus");
}
export function createMenu(input: ManagedMenu) {
  return request<ManagedMenu>("/api/v1/menus", {
    method: "POST",
    body: JSON.stringify(input),
  });
}
export function updateMenu(id: string, input: ManagedMenu) {
  return request<ManagedMenu>(`/api/v1/menus/${id}`, {
    method: "PATCH",
    body: JSON.stringify(input),
  });
}
