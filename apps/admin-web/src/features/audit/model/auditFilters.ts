import type { AuditAction, AuditResult } from "@/features/audit/api/auditApi";

export interface AuditFilters {
  actorId: string;
  action: AuditAction | "all";
  result: AuditResult | "all";
  createdFrom: string;
  createdTo: string;
}

export const emptyAuditFilters: AuditFilters = {
  actorId: "",
  action: "all",
  result: "all",
  createdFrom: "",
  createdTo: "",
};
