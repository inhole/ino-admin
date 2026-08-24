import { afterEach, expect, test, vi } from "vitest";
import { getAuditLogs } from "@/features/audit/api/auditApi";

afterEach(() => vi.restoreAllMocks());

test("serializes audit log filters", async () => {
  const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
    new Response(JSON.stringify({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
      { status: 200, headers: { "Content-Type": "application/json" } }),
  );

  await getAuditLogs({ action: "USER_UPDATE", result: "FAILURE", actorId: "actor-1",
    createdFrom: "2026-08-01T00:00:00.000Z", createdTo: "2026-09-01T00:00:00.000Z" });

  const search = new URL(fetchMock.mock.calls[0][0] as string, "http://localhost").searchParams;
  expect(search.get("action")).toBe("USER_UPDATE");
  expect(search.get("result")).toBe("FAILURE");
  expect(search.get("actorId")).toBe("actor-1");
  expect(search.get("createdFrom")).toBe("2026-08-01T00:00:00.000Z");
  expect(search.get("createdTo")).toBe("2026-09-01T00:00:00.000Z");
});
