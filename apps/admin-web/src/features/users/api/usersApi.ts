import { request, type PageResponse } from "@/api/client";
import {
  toUserListSearchParams,
  type UserListQuery,
} from "@/features/users/hook/userListQuery";

export interface UserSummary {
  id: string;
  email: string;
  displayName: string;
  status: string;
  role: string;
  createdAt: string;
}

export function getUsers(query: UserListQuery) {
  const search = toUserListSearchParams(query).toString();

  return request<PageResponse<UserSummary>>(
    `/api/v1/users${search === "" ? "" : `?${search}`}`,
  );
}
export function getUser(userId: string) {
  return request<UserSummary>(`/api/v1/users/${userId}`);
}
export function createUser(input: {
  email: string;
  password: string;
  displayName: string;
  role: string;
}) {
  return request<UserSummary>("/api/v1/users", {
    method: "POST",
    body: JSON.stringify(input),
  });
}
export function updateUserStatus(
  userId: string,
  status: "ACTIVE" | "DISABLED",
) {
  return request<{ id: string; status: string }>(
    `/api/v1/users/${userId}/status`,
    { method: "PATCH", body: JSON.stringify({ status }) },
  );
}
export function updateUserProfile(
  userId: string,
  input: { displayName: string; role: string },
) {
  return request<{ id: string; displayName: string; role: string }>(
    `/api/v1/users/${userId}`,
    { method: "PATCH", body: JSON.stringify(input) },
  );
}
