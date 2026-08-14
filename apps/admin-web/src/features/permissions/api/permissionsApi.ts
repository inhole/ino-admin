import { request } from "@/api/client";

export interface RolePermissions {
  role: string;
  displayName: string;
  systemRole: boolean;
  enabled: boolean;
  permissions: string[];
}

export function getPermissionCatalog() {
  return request<RolePermissions[]>("/api/v1/permissions");
}
export function getAvailablePermissions() {
  return request<string[]>("/api/v1/permissions/available");
}
export function updateRolePermissions(role: string, permissions: string[]) {
  return request<RolePermissions>(`/api/v1/permissions/roles/${role}`, {
    method: "PATCH",
    body: JSON.stringify({ permissions }),
  });
}
export function createRole(input: {
  role: string;
  displayName: string;
  permissions: string[];
}) {
  return request<RolePermissions>("/api/v1/permissions/roles", {
    method: "POST",
    body: JSON.stringify(input),
  });
}
export function updateRoleStatus(role: string, enabled: boolean) {
  return request<RolePermissions>(
    `/api/v1/permissions/roles/${role}/status`,
    { method: "PATCH", body: JSON.stringify({ enabled }) },
  );
}
