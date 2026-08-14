import { useQuery } from "@tanstack/react-query";
import { CheckCircle2, Server } from "lucide-react";
import { useTranslation } from "react-i18next";
import { ApiClientError, getSamples } from "@/api/client";
import { PageHeader, StatusPanel } from "@/components/layout/Page";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
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
  const samples = useQuery({ queryKey: ["samples"], queryFn: getSamples });
  return (
    <>
      <PageHeader
        description={t("description")}
        eyebrow={t("eyebrow")}
        title={t("title")}
      />
      <Card aria-labelledby="connection-title">
        <CardHeader className="flex-row items-start gap-4">
          <div className="grid size-10 shrink-0 place-items-center rounded-xl bg-primary/10 text-primary">
            <Server />
          </div>
          <div>
            <CardTitle id="connection-title">{t("connectionTitle")}</CardTitle>
            <CardDescription>{t("connectionDescription")}</CardDescription>
          </div>
        </CardHeader>
        <CardContent>
          {samples.isPending && (
            <StatusPanel>
              <p role="status">{t("loading")}</p>
            </StatusPanel>
          )}
          {samples.isError && (
            <Alert variant="destructive" role="alert">
              <AlertTitle>{t("errorTitle")}</AlertTitle>
              <AlertDescription>
                {samples.error instanceof ApiClientError &&
                samples.error.status === 403
                  ? t("forbidden")
                  : samples.error.message}
              </AlertDescription>
              <Button
                className="mt-3 min-h-10"
                onClick={() => samples.refetch()}
                variant="outline"
              >
                {common("retry")}
              </Button>
            </Alert>
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
