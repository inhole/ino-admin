import { request, type PageResponse } from "@/api/client";

export interface Sample {
  id: number;
  name: string;
}

export function getSamples() {
  return request<PageResponse<Sample>>("/api/v1/samples");
}
