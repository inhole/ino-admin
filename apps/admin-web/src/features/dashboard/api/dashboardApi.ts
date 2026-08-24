import { request, type PageResponse } from "@/api/client";
import type { MonitoringSnapshot } from "@/features/dashboard/model/monitoringHistory";

export interface Sample {
  id: number;
  name: string;
}

export function getSamples() {
  return request<PageResponse<Sample>>("/api/v1/samples");
}

export function getMonitoringSummary(): Promise<MonitoringSnapshot> {
  return request<MonitoringSnapshot>("/api/v1/monitoring/summary");
}
