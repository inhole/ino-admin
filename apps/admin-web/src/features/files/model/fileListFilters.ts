export interface FileListFiltersValue {
  name: string;
  contentType: string;
  createdFrom: string;
  createdTo: string;
  order: string;
}

export const defaultFileListFilters: FileListFiltersValue = {
  name: "",
  contentType: "all",
  createdFrom: "",
  createdTo: "",
  order: "createdAt,desc",
};
