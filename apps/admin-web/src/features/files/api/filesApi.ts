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

export function getMyFiles() {
  return request<PageResponse<StoredFileSummary>>("/api/v1/files");
}
export function deleteFile(fileId: string) {
  return request<void>(`/api/v1/files/${fileId}`, { method: "DELETE" });
}
export function uploadFile(file: File) {
  const form = new FormData();
  form.append("file", file);
  return requestForm<StoredFileSummary>("/api/v1/files", form);
}
export function downloadFile(fileId: string) {
  return requestBlob(`/api/v1/files/${fileId}/content`);
}
