import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  RiCpuLine,
  RiDashboardLine,
  RiErrorWarningLine,
  RiFlashlightLine,
  RiRefreshLine,
  RiServerLine,
  RiTimeLine,
} from "@remixicon/react";
import { useTranslation } from "react-i18next";
import { getMonitoringSummary } from "@/features/dashboard/api/dashboardApi";
import { MetricCard } from "@/features/dashboard/component/MetricCard";
import { MonitoringCharts } from "@/features/dashboard/component/MonitoringCharts";
import { dashboardKeys } from "@/features/dashboard/hook/dashboardKeys";
import {
  appendMonitoringPoint,
  type MonitoringPoint,
} from "@/features/dashboard/model/monitoringHistory";
import { PageHeader } from "@/components/layout/Page";
import { ErrorState } from "@/components/states/PageStates";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

function formatPercent(value: number | null) {
  return value === null ? null : `${(value * 100).toFixed(1)}%`;
}

function formatMegabytes(value: number | null) {
  return value === null ? null : `${(value / 1024 / 1024).toFixed(0)} MB`;
}

function formatUptime(value: number | null) {
  if (value === null) return null;
  const hours = Math.floor(value / 3_600);
  const minutes = Math.floor((value % 3_600) / 60);
  return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`;
}

function MetricSkeleton() {
  return (
    <div aria-label="관제 정보를 불러오는 중…" className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4" role="status">
      {Array.from({ length: 4 }, (_, index) => (
        <Skeleton className="h-28" key={index} />
      ))}
    </div>
  );
}

export function DashboardPage() {
  const { t } = useTranslation("dashboard");
  const { t: common } = useTranslation("common");
  const [history, setHistory] = useState<MonitoringPoint[]>([]);
  const monitoring = useQuery({
    queryKey: dashboardKeys.monitoring,
    queryFn: getMonitoringSummary,
    refetchInterval: 5_000,
    refetchIntervalInBackground: false,
  });

  useEffect(() => {
    if (!monitoring.data) return;
    setHistory((current) => appendMonitoringPoint(current, monitoring.data));
  }, [monitoring.data]);

  const latest = history.at(-1);
  const display = (value: string | null) => value ?? t("unavailable");
  const collecting = t("collecting");

  return (
    <>
      <PageHeader
        actions={
          <Badge variant="secondary">
            <RiDashboardLine aria-hidden />
            {t("liveWindow")}
          </Badge>
        }
        description={t("monitoringDescription")}
        eyebrow={t("monitoringEyebrow")}
        title={t("monitoringTitle")}
      />
      <section aria-labelledby="monitoring-summary-title" className="flex flex-col gap-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-col gap-1">
            <h2 className="text-lg font-semibold" id="monitoring-summary-title">
              {t("summaryTitle")}
            </h2>
            <p className="text-sm text-muted-foreground">{t("summaryDescription")}</p>
          </div>
          <span aria-live="polite" className="text-xs text-muted-foreground">
            {monitoring.isFetching ? t("refreshing") : t("updated")}
          </span>
        </div>

        {monitoring.isPending && <MetricSkeleton />}
        {monitoring.isError && history.length === 0 && (
          <ErrorState
            error={monitoring.error}
            forbiddenDescription={t("forbidden")}
            onRetry={() => monitoring.refetch()}
            title={t("monitoringErrorTitle")}
          />
        )}
        {monitoring.isError && history.length > 0 && (
          <Alert>
            <RiErrorWarningLine aria-hidden />
            <AlertTitle>{t("staleTitle")}</AlertTitle>
            <AlertDescription>{t("staleDescription")}</AlertDescription>
            <Button onClick={() => monitoring.refetch()} size="sm" variant="outline">
              <RiRefreshLine data-icon="inline-start" />
              {common("retry")}
            </Button>
          </Alert>
        )}
        {latest && (
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <MetricCard description={t("systemCpu")} icon={RiCpuLine} label={t("cpu")} value={display(formatPercent(latest.systemCpuUsage))} />
            <MetricCard description={t("heapMax", { value: display(formatMegabytes(latest.heapMaxBytes)) })} icon={RiServerLine} label={t("heap")} value={display(formatMegabytes(latest.heapUsedBytes))} />
            <MetricCard description={t("uptimeDescription")} icon={RiTimeLine} label={t("uptime")} value={display(formatUptime(latest.processUptimeSeconds))} />
            <MetricCard description={t("peakThreads", { value: display(latest.peakThreads?.toLocaleString() ?? null) })} icon={RiServerLine} label={t("threads")} value={display(latest.liveThreads?.toLocaleString() ?? null)} />
            <MetricCard description={history.length < 2 ? collecting : t("tpsDescription")} icon={RiFlashlightLine} label={t("tps")} value={latest.tps === null ? collecting : `${latest.tps.toFixed(2)} TPS`} />
            <MetricCard description={history.length < 2 ? collecting : t("latencyDescription")} icon={RiTimeLine} label={t("latency")} value={latest.averageResponseMs === null ? collecting : `${latest.averageResponseMs.toFixed(1)} ms`} />
            <MetricCard description={history.length < 2 ? collecting : t("errorDescription")} icon={RiErrorWarningLine} label={t("errorRate")} value={latest.serverErrorRate === null ? collecting : `${latest.serverErrorRate.toFixed(1)}%`} />
          </div>
        )}
      </section>
      <MonitoringCharts history={history} />
    </>
  );
}
