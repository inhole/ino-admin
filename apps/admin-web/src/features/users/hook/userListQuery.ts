export type UserStatusFilter = "" | "ACTIVE" | "LOCKED" | "DISABLED";
export type UserSort =
  | "createdAt"
  | "displayName"
  | "email"
  | "role"
  | "status";
export type SortDirection = "asc" | "desc";

export interface UserListQuery {
  query: string;
  role: string;
  status: UserStatusFilter;
  page: number;
  size: number;
  sort: UserSort;
  direction: SortDirection;
}

export const DEFAULT_USER_LIST_QUERY: UserListQuery = {
  query: "",
  role: "",
  status: "",
  page: 0,
  size: 20,
  sort: "createdAt",
  direction: "desc",
};

const allowedStatuses = ["", "ACTIVE", "LOCKED", "DISABLED"] as const;
const allowedSorts = [
  "createdAt",
  "displayName",
  "email",
  "role",
  "status",
] as const;
const allowedDirections = ["asc", "desc"] as const;

function normalizeText(value: string | null) {
  return value?.trim() ?? "";
}

function parseNonNegativeInteger(
  value: string | null,
  fallback: number,
) {
  if (!value || !/^\d+$/.test(value)) {
    return fallback;
  }

  return Number.parseInt(value, 10);
}

function isUserStatusFilter(value: string): value is UserStatusFilter {
  return allowedStatuses.includes(value as (typeof allowedStatuses)[number]);
}

function isUserSort(value: string): value is UserSort {
  return allowedSorts.includes(value as (typeof allowedSorts)[number]);
}

function isSortDirection(value: string): value is SortDirection {
  return allowedDirections.includes(
    value as (typeof allowedDirections)[number],
  );
}

export function parseUserListQuery(searchParams: URLSearchParams): UserListQuery {
  const query = normalizeText(searchParams.get("query"));
  const role = normalizeText(searchParams.get("role"));
  const rawStatus = searchParams.get("status") ?? "";
  const rawSort = searchParams.get("sort") ?? "";
  const rawDirection = searchParams.get("direction") ?? "";
  const size = parseNonNegativeInteger(
    searchParams.get("size"),
    DEFAULT_USER_LIST_QUERY.size,
  );

  return {
    query,
    role,
    status: isUserStatusFilter(rawStatus)
      ? rawStatus
      : DEFAULT_USER_LIST_QUERY.status,
    page: parseNonNegativeInteger(
      searchParams.get("page"),
      DEFAULT_USER_LIST_QUERY.page,
    ),
    size:
      size >= 1 && size <= 100 ? size : DEFAULT_USER_LIST_QUERY.size,
    sort: isUserSort(rawSort) ? rawSort : DEFAULT_USER_LIST_QUERY.sort,
    direction: isSortDirection(rawDirection)
      ? rawDirection
      : DEFAULT_USER_LIST_QUERY.direction,
  };
}

export function toUserListSearchParams(query: UserListQuery) {
  const searchParams = new URLSearchParams();
  const normalizedQuery = normalizeText(query.query);
  const normalizedRole = normalizeText(query.role);

  if (normalizedQuery !== "") {
    searchParams.set("query", normalizedQuery);
  }
  if (normalizedRole !== "") {
    searchParams.set("role", normalizedRole);
  }
  if (query.status !== "") {
    searchParams.set("status", query.status);
  }
  if (query.page !== DEFAULT_USER_LIST_QUERY.page) {
    searchParams.set("page", String(query.page));
  }
  if (query.size !== DEFAULT_USER_LIST_QUERY.size) {
    searchParams.set("size", String(query.size));
  }
  if (query.sort !== DEFAULT_USER_LIST_QUERY.sort) {
    searchParams.set("sort", query.sort);
  }
  if (query.direction !== DEFAULT_USER_LIST_QUERY.direction) {
    searchParams.set("direction", query.direction);
  }

  return searchParams;
}
