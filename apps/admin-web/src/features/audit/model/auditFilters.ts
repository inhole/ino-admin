import type { AuditResult } from "@/features/audit/api/auditApi";

export interface AuditFilters {
  result: AuditResult | "all";
  createdFrom: string;
  createdTo: string;
}

export const emptyAuditFilters: AuditFilters = {
  result: "all",
  createdFrom: "",
  createdTo: "",
};
