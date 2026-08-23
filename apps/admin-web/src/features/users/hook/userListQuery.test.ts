import { expect, test } from "vitest";
import { userKeys } from "@/features/users/hook/userKeys";
import {
  DEFAULT_USER_LIST_QUERY,
  parseUserListQuery,
  toUserListSearchParams,
} from "@/features/users/hook/userListQuery";

test("normalizes supported URL values and omits defaults", () => {
  const query = parseUserListQuery(
    new URLSearchParams(
      "query=kim&role=ADMIN&status=ACTIVE&page=2&sort=email&direction=asc",
    ),
  );

  expect(query).toEqual({
    query: "kim",
    role: "ADMIN",
    status: "ACTIVE",
    page: 2,
    size: 20,
    sort: "email",
    direction: "asc",
  });
  expect(
    toUserListSearchParams({
      ...query,
      page: 0,
      sort: "createdAt",
      direction: "desc",
    }).toString(),
  ).toBe("query=kim&role=ADMIN&status=ACTIVE");
});

test("normalizes invalid URL values back to defaults", () => {
  expect(
    parseUserListQuery(
      new URLSearchParams(
        "query=%20%20&role=%20%20&status=UNKNOWN&page=-3&size=101&sort=passwordHash&direction=sideways",
      ),
    ),
  ).toEqual(DEFAULT_USER_LIST_QUERY);
});

test("normalizes non-numeric page and oversized values back to defaults", () => {
  expect(
    parseUserListQuery(
      new URLSearchParams("page=abc&size=0&sort=status&direction=asc"),
    ),
  ).toEqual({
    ...DEFAULT_USER_LIST_QUERY,
    sort: "status",
    direction: "asc",
  });
});

test("serializes query parameters in the required stable order", () => {
  expect(
    toUserListSearchParams({
      query: "kim",
      role: "ADMIN",
      status: "ACTIVE",
      page: 2,
      size: 50,
      sort: "email",
      direction: "asc",
    }).toString(),
  ).toBe(
    "query=kim&role=ADMIN&status=ACTIVE&page=2&size=50&sort=email&direction=asc",
  );
});

test("builds a query key scoped by the normalized list query", () => {
  expect(
    userKeys.list({
      ...DEFAULT_USER_LIST_QUERY,
      query: "kim",
    }),
  ).toEqual([
    "users",
    "list",
    {
      ...DEFAULT_USER_LIST_QUERY,
      query: "kim",
    },
  ]);
});
