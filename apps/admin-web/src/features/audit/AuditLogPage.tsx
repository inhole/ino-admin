import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { PageHeader, StatusPanel } from "@/components/layout/Page";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Item, ItemContent, ItemGroup } from "@/components/ui/item";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { AuditLogFilters } from "@/features/audit/AuditLogFilters";
import { getAccessHistory, type AccessHistoryParams } from "@/features/audit/api/auditApi";
import { emptyAuditFilters, type AuditFilters } from "@/features/audit/model/auditFilters";
import { formatDateTime } from "@/i18n/format";

function boundary(value: string, nextDay = false) {
  if (!value) return undefined;
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day + (nextDay ? 1 : 0)).toISOString();
}

function params(filters: AuditFilters): AccessHistoryParams {
  return { createdFrom: boundary(filters.createdFrom), createdTo: boundary(filters.createdTo, true) };
}

export function AccessHistoryPage() {
  const { t } = useTranslation("audit");
  const [filters, setFilters] = useState(emptyAuditFilters);
  const [page, setPage] = useState(0);
  const queryParams = useMemo(() => ({ ...params(filters), page, size: 20 }), [filters, page]);
  const logs = useQuery({ queryKey: ["access-history", queryParams], queryFn: () => getAccessHistory(queryParams) });
  return <>
    <PageHeader eyebrow={t("eyebrow")} title={t("title")} description={t("description")} />
    <Card><CardHeader><CardTitle>{t("listTitle")}</CardTitle><CardDescription>{t("listDescription")}</CardDescription></CardHeader>
      <CardContent className="flex flex-col gap-5"><AuditLogFilters value={filters} onApply={(next) => { setFilters(next); setPage(0); }} /><Separator />
        {logs.isPending && <div className="grid gap-3" role="status" aria-label={t("loading")}>{[1,2,3].map((row) => <Skeleton className="h-24 w-full rounded-xl" key={row} />)}</div>}
        {logs.isError && <Alert variant="destructive" role="alert"><AlertDescription>{t("loadError")}</AlertDescription></Alert>}
        {logs.data?.content.length === 0 && <StatusPanel>{t("empty")}</StatusPanel>}
        {logs.data && logs.data.content.length > 0 && <ItemGroup>{logs.data.content.map((log) => <Item key={log.id} variant="outline">
          <ItemContent><dl className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div><dt className="text-xs text-muted-foreground">{t("account")}</dt><dd className="break-all font-medium">{log.email}</dd></div>
            <div><dt className="text-xs text-muted-foreground">{t("displayName")}</dt><dd>{log.displayName}</dd></div>
            <div><dt className="text-xs text-muted-foreground">{t("role")}</dt><dd>{log.role}</dd></div>
            <div><dt className="text-xs text-muted-foreground">{t("loginAt")}</dt><dd>{formatDateTime(log.createdAt)}</dd></div>
          </dl></ItemContent>
        </Item>)}</ItemGroup>}
        {logs.data && <nav aria-label={t("pagination.label")} className="flex flex-col gap-3 border-t pt-4 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-sm text-muted-foreground">{t("pagination.total", { count: logs.data.totalElements })}</p>
          <div className="flex items-center justify-between gap-3 sm:justify-end">
            <p aria-live="polite" className="text-sm font-medium">{t("pagination.page", { current: logs.data.totalPages === 0 ? 0 : logs.data.page + 1, total: logs.data.totalPages })}</p>
            <div className="flex gap-2">
              <Button disabled={logs.data.page <= 0} onClick={() => setPage((current) => Math.max(0, current - 1))} size="sm" type="button" variant="outline">{t("pagination.previous")}</Button>
              <Button disabled={logs.data.totalPages === 0 || logs.data.page >= logs.data.totalPages - 1} onClick={() => setPage((current) => Math.min(logs.data.totalPages - 1, current + 1))} size="sm" type="button" variant="outline">{t("pagination.next")}</Button>
            </div>
          </div>
        </nav>}
      </CardContent></Card>
  </>;
}
