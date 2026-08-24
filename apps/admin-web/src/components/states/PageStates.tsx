import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { ApiClientError } from "@/api/client";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyTitle,
} from "@/components/ui/empty";
import { Spinner } from "@/components/ui/spinner";
import { cn } from "@/lib/utils";

export function EmptyState({
  title,
  description,
  action,
  className,
}: {
  title: ReactNode;
  description?: ReactNode;
  action?: ReactNode;
  className?: string;
}) {
  const { t } = useTranslation("common");
  return (
    <Empty className={cn("border", className)}>
      <EmptyHeader>
        <EmptyTitle>{title}</EmptyTitle>
        <EmptyDescription>{description ?? t("emptyDescription")}</EmptyDescription>
      </EmptyHeader>
      {action && <EmptyContent>{action}</EmptyContent>}
    </Empty>
  );
}

export function LoadingState({ label }: { label: string }) {
  return (
    <div className="flex min-h-24 items-center justify-center gap-2" role="status">
      <Spinner />
      <span>{label}</span>
    </div>
  );
}

export function ErrorState({
  title,
  description,
  error,
  forbiddenDescription,
  onRetry,
}: {
  title: string;
  description?: ReactNode;
  error?: unknown;
  forbiddenDescription?: ReactNode;
  onRetry?: () => void;
}) {
  const { t } = useTranslation("common");
  const apiError = error instanceof ApiClientError ? error : undefined;
  const resolvedDescription =
    apiError?.status === 403 && forbiddenDescription
      ? forbiddenDescription
      : description ?? apiError?.message ?? t("connectionError");
  return (
    <Alert role="alert" variant="destructive">
      <AlertTitle>{title}</AlertTitle>
      <AlertDescription>
        <p>{resolvedDescription}</p>
        {apiError?.traceId && (
          <p className="mt-2 text-xs font-mono">
            {t("inquiryCode", { traceId: apiError.traceId })}
          </p>
        )}
      </AlertDescription>
      {onRetry && (
        <Button className="mt-3" onClick={onRetry} variant="outline">
          {t("retry")}
        </Button>
      )}
    </Alert>
  );
}
