import type { FileListParams } from "@/features/files/api/filesApi";

export const fileKeys = {
  all: ["files"] as const,
  list: (params: FileListParams) => [...fileKeys.all, "list", params] as const,
};
