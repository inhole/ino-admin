import { request, type PageResponse } from "@/api/client";

export type AuditAction = string;
export type AuditResult = "SUCCESS" | "FAILURE";

export interface AuditLogEntry {
  id: string;
  actorId: string | null;
  action: AuditAction;
  resource: string;
  result: AuditResult;
  statusCode: number;
  ipAddress: string | null;
  userAgent: string | null;
  traceId: string | null;
  createdAt: string;
}

export interface AuditLogParams {
  actorId?: string;
  action?: AuditAction;
  result?: AuditResult;
  createdFrom?: string;
  createdTo?: string;
  page?: number;
  size?: number;
}

export function getAuditLogs(params: AuditLogParams = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined) query.set(key, String(value));
  });
  return request<PageResponse<AuditLogEntry>>(`/api/v1/audit-logs${query.size ? `?${query}` : ""}`);
}
