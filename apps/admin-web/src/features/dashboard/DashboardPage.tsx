import { useQuery } from "@tanstack/react-query";
import { CheckCircle2, Server } from "lucide-react";
import { useTranslation } from "react-i18next";
import { getSamples } from "@/features/dashboard/api/dashboardApi";
import { dashboardKeys } from "@/features/dashboard/hook/dashboardKeys";
import { LoadingPanel, PageHeader, StatusPanel } from "@/components/layout/Page";
import { ErrorState } from "@/components/states/PageStates";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Item,
  ItemActions,
  ItemContent,
  ItemGroup,
  ItemTitle,
} from "@/components/ui/item";

export function DashboardPage() {
  const { t } = useTranslation("dashboard");
  const { t: common } = useTranslation("common");
  const samples = useQuery({ queryKey: dashboardKeys.samples, queryFn: getSamples });
  return (
    <>
      <PageHeader
        description={t("description")}
        eyebrow={t("eyebrow")}
        title={t("title")}
      />
      <Card aria-labelledby="connection-title">
        <CardHeader>
          <CardTitle className="flex items-center gap-2" id="connection-title">
            <Server aria-hidden="true" />
            {t("connectionTitle")}
          </CardTitle>
          <CardDescription>{t("connectionDescription")}</CardDescription>
        </CardHeader>
        <CardContent>
          {samples.isPending && (
            <LoadingPanel label={t("loading")} />
          )}
          {samples.isError && (
            <ErrorState
              error={samples.error}
              forbiddenDescription={t("forbidden")}
              onRetry={() => samples.refetch()}
              title={t("errorTitle")}
            />
          )}
          {samples.data?.content.length === 0 && (
            <StatusPanel>{common("empty")}</StatusPanel>
          )}
          {samples.data && samples.data.content.length > 0 && (
            <ItemGroup>
              {samples.data.content.map((sample) => (
                <Item key={sample.id} variant="outline">
                  <ItemContent>
                    <ItemTitle>{sample.name}</ItemTitle>
                  </ItemContent>
                  <ItemActions>
                  <Badge variant="secondary">
                    <CheckCircle2 aria-hidden="true" />
                    {common("statusNormal")}
                  </Badge>
                  </ItemActions>
                </Item>
              ))}
            </ItemGroup>
          )}
        </CardContent>
      </Card>
    </>
  );
}
