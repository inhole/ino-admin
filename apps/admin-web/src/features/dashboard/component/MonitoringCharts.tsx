import type { ReactNode } from "react";
import {
  Area,
  AreaChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  XAxis,
} from "recharts";
import { useTranslation } from "react-i18next";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  ChartContainer,
  ChartLegendContent,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart";
import type { MonitoringPoint } from "@/features/dashboard/model/monitoringHistory";

function formatPercent(value: number | null) {
  return value === null ? null : `${(value * 100).toFixed(1)}%`;
}

function formatMegabytes(value: number | null) {
  return value === null ? null : `${(value / 1024 / 1024).toFixed(0)} MB`;
}

function formatTps(value: number | null) {
  return value === null ? null : `${value.toFixed(2)} TPS`;
}

function formatMilliseconds(value: number | null) {
  return value === null ? null : `${value.toFixed(1)} ms`;
}

function formatErrorRate(value: number | null) {
  return value === null ? null : `${value.toFixed(1)}%`;
}

function timeLabel(timestamp: string) {
  return new Intl.DateTimeFormat(undefined, {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).format(new Date(timestamp));
}

function ChartPanel({
  id,
  title,
  description,
  latest,
  children,
}: {
  id: string;
  title: string;
  description: string;
  latest: string;
  children: ReactNode;
}) {
  return (
    <section aria-labelledby={id}>
      <Card>
        <CardHeader className="gap-1">
          <CardTitle id={id}>{title}</CardTitle>
          <CardDescription>{description}</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          {children}
          <p className="text-xs text-muted-foreground">{latest}</p>
        </CardContent>
      </Card>
    </section>
  );
}

export function MonitoringCharts({ history }: { history: MonitoringPoint[] }) {
  const { t } = useTranslation("dashboard");
  const latest = history.at(-1);
  if (!latest) return null;

  const cpuChartConfig = {
    systemCpuUsage: { label: t("systemCpu"), color: "var(--chart-2)" },
    processCpuUsage: { label: t("processCpu"), color: "var(--chart-4)" },
  } satisfies ChartConfig;
  const heapChartConfig = {
    heapUsedBytes: { label: t("heapUsed"), color: "var(--chart-2)" },
    heapMaxBytes: { label: t("heapMaxLabel"), color: "var(--chart-4)" },
  } satisfies ChartConfig;
  const tpsChartConfig = {
    value: { label: t("tps"), color: "var(--chart-3)" },
  } satisfies ChartConfig;
  const latencyChartConfig = {
    value: { label: t("latency"), color: "var(--chart-3)" },
  } satisfies ChartConfig;
  const errorChartConfig = {
    value: { label: t("errorRate"), color: "var(--chart-3)" },
  } satisfies ChartConfig;

  return (
    <section aria-labelledby="monitoring-charts-title" className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <h2 className="text-lg font-semibold" id="monitoring-charts-title">
          {t("chartsTitle")}
        </h2>
        <p className="text-sm text-muted-foreground">{t("chartsDescription")}</p>
      </div>
      <div className="grid gap-4 xl:grid-cols-2">
        <ChartPanel
          description={t("chartCpuDescription")}
          id="monitoring-chart-cpu"
          latest={t("latestCpu", {
            system: formatPercent(latest.systemCpuUsage) ?? t("unavailable"),
            process: formatPercent(latest.processCpuUsage) ?? t("unavailable"),
          })}
          title={t("chartCpu")}
        >
          <ChartContainer className="h-56 w-full" config={cpuChartConfig}>
            <LineChart accessibilityLayer data={history}>
              <CartesianGrid vertical={false} />
              <XAxis dataKey="timestamp" tickFormatter={timeLabel} tickLine={false} axisLine={false} />
              <ChartTooltip
                content={<ChartTooltipContent formatter={(value, name) => [formatPercent(Number(value)) ?? t("unavailable"), name]} />}
              />
              <Legend content={<ChartLegendContent />} />
              <Line dataKey="systemCpuUsage" dot={false} name={t("systemCpu")} stroke="var(--color-systemCpuUsage)" type="monotone" />
              <Line dataKey="processCpuUsage" dot={false} name={t("processCpu")} stroke="var(--color-processCpuUsage)" type="monotone" />
            </LineChart>
          </ChartContainer>
        </ChartPanel>
        <ChartPanel
          description={t("chartHeapDescription")}
          id="monitoring-chart-heap"
          latest={t("latestHeap", {
            used: formatMegabytes(latest.heapUsedBytes) ?? t("unavailable"),
            max: formatMegabytes(latest.heapMaxBytes) ?? t("unavailable"),
          })}
          title={t("chartHeap")}
        >
          <ChartContainer className="h-56 w-full" config={heapChartConfig}>
            <AreaChart accessibilityLayer data={history}>
              <CartesianGrid vertical={false} />
              <XAxis dataKey="timestamp" tickFormatter={timeLabel} tickLine={false} axisLine={false} />
              <ChartTooltip
                content={<ChartTooltipContent formatter={(value, name) => [formatMegabytes(Number(value)) ?? t("unavailable"), name]} />}
              />
              <Legend content={<ChartLegendContent />} />
              <Area dataKey="heapUsedBytes" fill="var(--color-heapUsedBytes)" fillOpacity={0.18} name={t("heapUsed")} stroke="var(--color-heapUsedBytes)" type="monotone" />
              <Area dataKey="heapMaxBytes" fill="var(--color-heapMaxBytes)" fillOpacity={0.08} name={t("heapMaxLabel")} stroke="var(--color-heapMaxBytes)" type="monotone" />
            </AreaChart>
          </ChartContainer>
        </ChartPanel>
        <ChartPanel description={t("chartTpsDescription")} id="monitoring-chart-tps" latest={t("latestValue", { value: formatTps(latest.tps) ?? t("collecting") })} title={t("chartTps")}>
          <ChartContainer className="h-56 w-full" config={tpsChartConfig}>
            <LineChart accessibilityLayer data={history}>
              <CartesianGrid vertical={false} />
              <XAxis dataKey="timestamp" tickFormatter={timeLabel} tickLine={false} axisLine={false} />
              <ChartTooltip content={<ChartTooltipContent formatter={(value) => [formatTps(Number(value)) ?? t("collecting"), t("tps")]} />} />
              <Line dataKey="tps" dot={false} name={t("tps")} stroke="var(--color-value)" type="monotone" />
            </LineChart>
          </ChartContainer>
        </ChartPanel>
        <ChartPanel description={t("chartLatencyDescription")} id="monitoring-chart-latency" latest={t("latestValue", { value: formatMilliseconds(latest.averageResponseMs) ?? t("collecting") })} title={t("chartLatency")}>
          <ChartContainer className="h-56 w-full" config={latencyChartConfig}>
            <LineChart accessibilityLayer data={history}>
              <CartesianGrid vertical={false} />
              <XAxis dataKey="timestamp" tickFormatter={timeLabel} tickLine={false} axisLine={false} />
              <ChartTooltip content={<ChartTooltipContent formatter={(value) => [formatMilliseconds(Number(value)) ?? t("collecting"), t("latency")]} />} />
              <Line dataKey="averageResponseMs" dot={false} name={t("latency")} stroke="var(--color-value)" type="monotone" />
            </LineChart>
          </ChartContainer>
        </ChartPanel>
        <ChartPanel description={t("chartErrorDescription")} id="monitoring-chart-error" latest={t("latestValue", { value: formatErrorRate(latest.serverErrorRate) ?? t("collecting") })} title={t("chartError")}>
          <ChartContainer className="h-56 w-full" config={errorChartConfig}>
            <LineChart accessibilityLayer data={history}>
              <CartesianGrid vertical={false} />
              <XAxis dataKey="timestamp" tickFormatter={timeLabel} tickLine={false} axisLine={false} />
              <ChartTooltip content={<ChartTooltipContent formatter={(value) => [formatErrorRate(Number(value)) ?? t("collecting"), t("errorRate")]} />} />
              <Line dataKey="serverErrorRate" dot={false} name={t("errorRate")} stroke="var(--color-value)" type="monotone" />
            </LineChart>
          </ChartContainer>
        </ChartPanel>
      </div>
    </section>
  );
}
