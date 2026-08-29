import { request, type PageResponse } from "@/api/client";

export type AuditResult = "SUCCESS" | "FAILURE";

export interface AccessHistoryEntry {
  id: string;
  result: AuditResult;
  statusCode: number;
  ipAddress: string | null;
  userAgent: string | null;
  traceId: string | null;
  createdAt: string;
}

export interface AccessHistoryParams {
  result?: AuditResult;
  createdFrom?: string;
  createdTo?: string;
  page?: number;
  size?: number;
}

export function getAccessHistory(params: AccessHistoryParams = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined) query.set(key, String(value));
  });
  return request<PageResponse<AccessHistoryEntry>>(`/api/v1/access-history${query.size ? `?${query}` : ""}`);
}
