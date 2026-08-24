import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { PageHeader, StatusPanel } from "@/components/layout/Page";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Item, ItemContent, ItemDescription, ItemGroup, ItemTitle } from "@/components/ui/item";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { AuditLogFilters } from "@/features/audit/AuditLogFilters";
import { getAuditLogs, type AuditLogParams } from "@/features/audit/api/auditApi";
import { emptyAuditFilters, type AuditFilters } from "@/features/audit/model/auditFilters";
import { formatDateTime } from "@/i18n/format";

function boundary(value: string, nextDay = false) {
  if (!value) return undefined;
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day + (nextDay ? 1 : 0)).toISOString();
}

function params(filters: AuditFilters): AuditLogParams {
  return { actorId: filters.actorId.trim() || undefined, action: filters.action === "all" ? undefined : filters.action,
    result: filters.result === "all" ? undefined : filters.result, createdFrom: boundary(filters.createdFrom), createdTo: boundary(filters.createdTo, true) };
}

export function AuditLogPage() {
  const { t } = useTranslation("audit");
  const [filters, setFilters] = useState(emptyAuditFilters);
  const queryParams = useMemo(() => params(filters), [filters]);
  const logs = useQuery({ queryKey: ["audit-logs", queryParams], queryFn: () => getAuditLogs(queryParams) });
  return <>
    <PageHeader eyebrow={t("eyebrow")} title={t("title")} description={t("description")} />
    <Card><CardHeader><CardTitle>{t("listTitle")}</CardTitle><CardDescription>{t("listDescription")}</CardDescription></CardHeader>
      <CardContent className="flex flex-col gap-5"><AuditLogFilters value={filters} onApply={setFilters} /><Separator />
        {logs.isPending && <div className="grid gap-3" role="status" aria-label={t("loading")}>{[1,2,3].map((row) => <Skeleton className="h-24 w-full rounded-xl" key={row} />)}</div>}
        {logs.isError && <Alert variant="destructive" role="alert"><AlertDescription>{t("loadError")}</AlertDescription></Alert>}
        {logs.data?.content.length === 0 && <StatusPanel>{t("empty")}</StatusPanel>}
        {logs.data && logs.data.content.length > 0 && <ItemGroup>{logs.data.content.map((log) => <Item key={log.id} variant="outline">
          <ItemContent><ItemTitle className="break-all">{log.resource}</ItemTitle><ItemDescription>{log.action} · {log.actorId ?? t("anonymous")} · {formatDateTime(log.createdAt)}</ItemDescription></ItemContent>
          <Badge variant={log.result === "FAILURE" ? "destructive" : "secondary"}>{t(log.result.toLowerCase())}</Badge>
        </Item>)}</ItemGroup>}
      </CardContent></Card>
  </>;
}
