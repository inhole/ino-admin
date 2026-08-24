import { afterEach, expect, test, vi } from "vitest";
import { getUsers } from "@/features/users/api/usersApi";
import { DEFAULT_USER_LIST_QUERY } from "@/features/users/hook/userListQuery";

afterEach(() => {
  vi.restoreAllMocks();
});

test("requests the exact serialized user list query", async () => {
  const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
    new Response(
      JSON.stringify({
        content: [],
        page: 2,
        size: 20,
        totalElements: 0,
        totalPages: 0,
      }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    ),
  );

  await getUsers({
    ...DEFAULT_USER_LIST_QUERY,
    query: "kim",
    role: "ADMIN",
    status: "ACTIVE",
    page: 2,
    sort: "email",
    direction: "asc",
  });

  expect(fetchMock.mock.calls[0]?.[0]).toBe(
    "/api/v1/users?query=kim&role=ADMIN&status=ACTIVE&page=2&sort=email&direction=asc",
  );
});

test("omits the question mark when every filter is defaulted", async () => {
  const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
    new Response(
      JSON.stringify({
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
      }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    ),
  );

  await getUsers(DEFAULT_USER_LIST_QUERY);

  expect(fetchMock.mock.calls[0]?.[0]).toBe("/api/v1/users");
});
