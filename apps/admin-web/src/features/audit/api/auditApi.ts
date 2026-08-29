import { request, type PageResponse } from "@/api/client";

export interface AccessHistoryEntry {
  id: string;
  email: string;
  createdAt: string;
}

export interface AccessHistoryParams {
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
