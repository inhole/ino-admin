import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import {
  createRole,
  getAvailablePermissions,
  getPermissionCatalog,
  updateRolePermissions,
  updateRoleStatus,
} from "@/features/permissions/api/permissionsApi";
import { ApiClientError } from "@/api/client";
import { permissionKeys } from "@/features/permissions/hook/permissionKeys";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { useAuth } from "@/features/auth/hook/useAuth";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { LoadingPanel, PageHeader } from "@/components/layout/Page";
import { CreateRoleDialog, EditRolePermissionsDialog } from "@/features/permissions/component/RoleDialogs";

export function PermissionsPage() {
  const { t } = useTranslation("permissions");
  const { t: common } = useTranslation("common");
  const catalog = useQuery({
    queryKey: permissionKeys.all,
    queryFn: getPermissionCatalog,
  });
  const available = useQuery({
    queryKey: permissionKeys.available,
    queryFn: getAvailablePermissions,
  });
  const client = useQueryClient();
  const { user } = useAuth();
  const update = useMutation({
    mutationFn: ({
      role,
      permissions,
    }: {
      role: string;
      permissions: string[];
    }) => updateRolePermissions(role, permissions),
    onSuccess: async () =>
      client.invalidateQueries({ queryKey: permissionKeys.all }),
  });
  const create = useMutation({
    mutationFn: createRole,
    onSuccess: async () =>
      client.invalidateQueries({ queryKey: permissionKeys.all }),
  });
  const status = useMutation({
    mutationFn: ({ role, enabled }: { role: string; enabled: boolean }) =>
      updateRoleStatus(role, enabled),
    onSuccess: async () =>
      client.invalidateQueries({ queryKey: permissionKeys.all }),
  });
  return (
    <>
      <PageHeader
        actions={user?.permissions.includes("permission:update") ? (
          <CreateRoleDialog
            onSave={(input) => create.mutateAsync(input)}
            pending={create.isPending}
          />
        ) : undefined}
        description={t("description")}
        eyebrow={t("eyebrow")}
        title={t("title")}
      />
      <Card>
        <CardHeader>
          <CardTitle>{t("listTitle")}</CardTitle>
          <CardDescription>
            {t("listDescription")}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {catalog.isPending && (
            <LoadingPanel label={t("loading")} />
          )}
          {catalog.isError && (
            <Alert variant="destructive" role="alert">
              <AlertTitle>{t("errorTitle")}</AlertTitle>
              <AlertDescription>
                {catalog.error instanceof ApiClientError
                  ? catalog.error.message
                  : t("loadError")}
              </AlertDescription>
            </Alert>
          )}
          {catalog.data && (
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {catalog.data.map((item) => (
                <Card key={item.role} size="sm">
                  <CardHeader>
                    <CardTitle>{item.displayName || item.role}</CardTitle>
                    <CardDescription>
                      <Badge
                        className="font-mono"
                        variant={item.enabled ? "secondary" : "outline"}
                      >
                        {item.role}
                      </Badge>
                    </CardDescription>
                    {!item.systemRole && (
                      <CardAction>
                        <Button
                          onClick={() =>
                            status.mutate({
                              role: item.role,
                              enabled: !item.enabled,
                            })
                          }
                          size="sm"
                          variant="outline"
                        >
                          {item.enabled ? common("disable") : common("enable")}
                        </Button>
                      </CardAction>
                    )}
                  </CardHeader>
                  <CardContent>
                  <div className="flex flex-wrap gap-2">
                    {item.permissions.map((permission) => (
                      <Badge key={permission} title={permission} variant="outline">
                        {t(`labels.${permission.replace(":", "_")}`, { defaultValue: permission })}
                      </Badge>
                    ))}
                  </div>
                  {item.role !== "SUPER_ADMIN" &&
                    user?.permissions.includes("permission:update") &&
                    available.data && (
                      <div className="mt-3">
                        <EditRolePermissionsDialog
                          available={available.data}
                          onSave={(permissions) =>
                            update.mutateAsync({ role: item.role, permissions })
                          }
                          pending={update.isPending}
                          role={item}
                        />
                      </div>
                    )}
                  {item.permissions.length === 0 && (
                    <p className="mt-2 text-sm text-muted-foreground">
                      {t("empty")}
                    </p>
                  )}
                  {update.isError && (
                    <Alert className="mt-3" variant="destructive">
                      <AlertDescription>
                        {update.error instanceof ApiClientError
                          ? update.error.message
                          : t("updateError")}
                      </AlertDescription>
                    </Alert>
                  )}
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </>
  );
}
