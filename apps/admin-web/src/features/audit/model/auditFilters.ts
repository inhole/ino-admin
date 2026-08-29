export interface AuditFilters {
  createdFrom: string;
  createdTo: string;
}

export const emptyAuditFilters: AuditFilters = {
  createdFrom: "",
  createdTo: "",
};
