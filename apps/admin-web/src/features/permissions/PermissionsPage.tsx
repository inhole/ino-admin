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
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import { Badge } from "@/components/ui/badge";
import {
  Field,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSet,
} from "@/components/ui/field";
import { Spinner } from "@/components/ui/spinner";
import { LoadingPanel, PageHeader } from "@/components/layout/Page";

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
        description={t("description")}
        eyebrow={t("eyebrow")}
        title={t("title")}
      />
      {user?.permissions.includes("permission:update") && (
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>{t("createTitle")}</CardTitle>
          </CardHeader>
          <CardContent>
            <form
              onSubmit={(event) => {
                event.preventDefault();
                const data = new FormData(event.currentTarget);
                create.mutate({
                  role: String(data.get("role")),
                  displayName: String(data.get("displayName")),
                  permissions: [],
                });
                event.currentTarget.reset();
              }}
            >
              <FieldGroup className="grid gap-3 md:grid-cols-3">
              <Field>
              <FieldLabel htmlFor="role-key">{t("roleKey")}</FieldLabel>
              <Input id="role-key"
                aria-label={t("roleKey")}
                name="role"
                placeholder="CONTENT_EDITOR"
                required
              /></Field>
              <Field>
              <FieldLabel htmlFor="role-name">{t("roleName")}</FieldLabel>
              <Input id="role-name"
                aria-label={t("roleName")}
                name="displayName"
                placeholder={t("roleNamePlaceholder")}
                required
              /></Field>
              <Button className="self-end" disabled={create.isPending} type="submit">
                {create.isPending && <Spinner data-icon="inline-start" />}
                {t("create")}
              </Button>
              </FieldGroup>
            </form>
          </CardContent>
        </Card>
      )}
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
                  <FieldSet>
                    <FieldLegend className="sr-only">{t("permissionLegend", { name: item.displayName || item.role })}</FieldLegend>
                    <FieldGroup className="gap-1">
                    {available.data?.map((permission) => {
                      const checked = item.permissions.includes(permission);
                      return (
                        <Field orientation="horizontal"
                          className="min-h-10 rounded-lg px-2 hover:bg-muted"
                          key={permission}
                        >
                          <Checkbox
                            checked={checked}
                            disabled={
                              item.role === "SUPER_ADMIN" ||
                              !user?.permissions.includes(
                                "permission:update",
                              ) ||
                              update.isPending
                            }
                            id={`${item.role}-${permission}`}
                            onCheckedChange={() =>
                              update.mutate({
                                role: item.role,
                                permissions: checked
                                  ? item.permissions.filter(
                                      (value) => value !== permission,
                                    )
                                  : [...item.permissions, permission],
                              })
                            }
                          />
                          <FieldLabel
                            className="cursor-pointer break-all font-mono text-xs"
                            htmlFor={`${item.role}-${permission}`}
                          >
                            {permission}
                          </FieldLabel>
                        </Field>
                      );
                    })}
                    </FieldGroup>
                  </FieldSet>
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
