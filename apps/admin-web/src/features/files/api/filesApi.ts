import {
  request,
  requestBlob,
  requestForm,
  type PageResponse,
} from "@/api/client";

export interface StoredFileSummary {
  id: string;
  originalName: string;
  contentType: string;
  size: number;
  createdAt: string;
}

export interface FileListParams {
  page?: number;
  size?: number;
  name?: string;
  contentType?: string;
  createdFrom?: string;
  createdTo?: string;
  sort?: "createdAt" | "originalName" | "size";
  direction?: "asc" | "desc";
}

export function getMyFiles(params: FileListParams = {}) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") search.set(key, String(value));
  });
  const query = search.toString();
  return request<PageResponse<StoredFileSummary>>(
    `/api/v1/files${query ? `?${query}` : ""}`,
  );
}
export function deleteFile(fileId: string) {
  return request<void>(`/api/v1/files/${fileId}`, { method: "DELETE" });
}
export function uploadFile(
  file: File,
  onProgress?: (percentage: number) => void,
) {
  const form = new FormData();
  form.append("file", file);
  return requestForm<StoredFileSummary>("/api/v1/files", form, onProgress);
}
export function downloadFile(fileId: string) {
  return requestBlob(`/api/v1/files/${fileId}/content`);
}
