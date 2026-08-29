import { afterEach, expect, test, vi } from "vitest";
import { getAccessHistory } from "@/features/audit/api/auditApi";

afterEach(() => vi.restoreAllMocks());

test("serializes access history filters", async () => {
  const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
    new Response(JSON.stringify({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
      { status: 200, headers: { "Content-Type": "application/json" } }),
  );

  await getAccessHistory({ createdFrom: "2026-08-01T00:00:00.000Z",
    createdTo: "2026-09-01T00:00:00.000Z" });

  const search = new URL(fetchMock.mock.calls[0][0] as string, "http://localhost").searchParams;
  expect(search.has("result")).toBe(false);
  expect(fetchMock.mock.calls[0][0]).toContain("/api/v1/access-history");
  expect(search.get("createdFrom")).toBe("2026-08-01T00:00:00.000Z");
  expect(search.get("createdTo")).toBe("2026-09-01T00:00:00.000Z");
});
