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
import { dashboardKeys } from "@/features/dashboard/hook/dashboardKeys";
import {
  appendMonitoringPoint,
  classifyDerivedMetric,
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

function formatUptime(
  value: number | null,
  localize: (hours: number, minutes: number) => string,
) {
  if (value === null) return null;
  const hours = Math.floor(value / 3_600);
  const minutes = Math.floor((value % 3_600) / 60);
  return localize(hours, minutes);
}

function MetricSkeleton({ label }: { label: string }) {
  return (
    <div aria-label={label} className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4" role="status">
      {Array.from({ length: 4 }, (_, index) => (
        <Skeleton className="h-28" key={index} />
      ))}
    </div>
  );
}

function derivedMetricDisplay(
  status: "available" | "collecting" | "unavailable",
  value: number | null,
  format: (value: number) => string,
  unavailable: string,
  collecting: string,
) {
  if (status === "unavailable") return unavailable;
  return status === "collecting" || value === null ? collecting : format(value);
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
  const previous = history.at(-2);
  const display = (value: string | null) => value ?? t("unavailable");
  const collecting = t("collecting");
  const unavailable = t("unavailable");
  const tpsStatus = latest
    ? classifyDerivedMetric(latest, previous, ["httpRequestCount"], latest.tps)
    : "collecting";
  const latencyStatus = latest
    ? classifyDerivedMetric(latest, previous, ["httpRequestCount", "httpRequestDurationSeconds"], latest.averageResponseMs)
    : "collecting";
  const errorRateStatus = latest
    ? classifyDerivedMetric(latest, previous, ["httpRequestCount", "httpServerErrorCount"], latest.serverErrorRate)
    : "collecting";
  const liveStatus = monitoring.isError
    ? history.length > 0
      ? t("staleStatus")
      : t("monitoringErrorStatus")
    : monitoring.isFetching
      ? t("refreshing")
      : latest
        ? t("updated")
        : collecting;

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
            {liveStatus}
          </span>
        </div>

        {monitoring.isPending && <MetricSkeleton label={t("monitoringLoading")} />}
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
            <MetricCard description={t("uptimeDescription")} icon={RiTimeLine} label={t("uptime")} value={display(formatUptime(latest.processUptimeSeconds, (hours, minutes) => t("uptimeValue", { hours, minutes })))} />
            <MetricCard description={t("peakThreads", { value: display(latest.peakThreads?.toLocaleString() ?? null) })} icon={RiServerLine} label={t("threads")} value={display(latest.liveThreads?.toLocaleString() ?? null)} />
            <MetricCard description={tpsStatus === "unavailable" ? unavailable : history.length < 2 ? collecting : t("tpsDescription")} icon={RiFlashlightLine} label={t("tps")} value={derivedMetricDisplay(tpsStatus, latest.tps, (value) => `${value.toFixed(2)} TPS`, unavailable, collecting)} />
            <MetricCard description={latencyStatus === "unavailable" ? unavailable : history.length < 2 ? collecting : t("latencyDescription")} icon={RiTimeLine} label={t("latency")} value={derivedMetricDisplay(latencyStatus, latest.averageResponseMs, (value) => `${value.toFixed(1)} ms`, unavailable, collecting)} />
            <MetricCard description={errorRateStatus === "unavailable" ? unavailable : history.length < 2 ? collecting : t("errorDescription")} icon={RiErrorWarningLine} label={t("errorRate")} value={derivedMetricDisplay(errorRateStatus, latest.serverErrorRate, (value) => `${value.toFixed(1)}%`, unavailable, collecting)} />
          </div>
        )}
      </section>
    </>
  );
}
