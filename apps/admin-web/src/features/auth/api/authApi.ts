import {
  createSession,
  destroySession,
  request,
} from "@/api/client";

export interface CurrentUser {
  id: string;
  email: string;
  displayName: string;
  status: string;
  role: string;
  permissions: string[];
}

export const login = createSession;
export const logout = destroySession;
export function getCurrentUser() {
  return request<CurrentUser>("/api/v1/auth/me");
}
