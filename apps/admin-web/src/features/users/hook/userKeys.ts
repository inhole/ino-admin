import type { UserListQuery } from "@/features/users/hook/userListQuery";

export const userKeys = {
  all: ["users"] as const,
  list: (query: UserListQuery) => [...userKeys.all, "list", query] as const,
};
