import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  ApiClientError,
  createRole,
  getAvailablePermissions,
  getPermissionCatalog,
  updateRolePermissions,
  updateRoleStatus,
} from "@/api/client";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { useAuth } from "@/features/auth/model/useAuth";
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
  const catalog = useQuery({
    queryKey: ["permissions"],
    queryFn: getPermissionCatalog,
  });
  const available = useQuery({
    queryKey: ["permissions", "available"],
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
      client.invalidateQueries({ queryKey: ["permissions"] }),
  });
  const create = useMutation({
    mutationFn: createRole,
    onSuccess: async () =>
      client.invalidateQueries({ queryKey: ["permissions"] }),
  });
  const status = useMutation({
    mutationFn: ({ role, enabled }: { role: string; enabled: boolean }) =>
      updateRoleStatus(role, enabled),
    onSuccess: async () =>
      client.invalidateQueries({ queryKey: ["permissions"] }),
  });
  return (
    <>
      <PageHeader
        description="역할별 서버 접근 권한을 관리합니다."
        eyebrow="IDENTITY"
        title="권한 카탈로그"
      />
      {user?.permissions.includes("permission:update") && (
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>커스텀 역할 생성</CardTitle>
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
              <FieldLabel htmlFor="role-key">역할 키</FieldLabel>
              <Input id="role-key"
                aria-label="역할 키"
                name="role"
                placeholder="CONTENT_EDITOR"
                required
              /></Field>
              <Field>
              <FieldLabel htmlFor="role-name">역할 이름</FieldLabel>
              <Input id="role-name"
                aria-label="역할 이름"
                name="displayName"
                placeholder="콘텐츠 편집자"
                required
              /></Field>
              <Button className="self-end" disabled={create.isPending} type="submit">
                {create.isPending && <Spinner data-icon="inline-start" />}
                역할 생성
              </Button>
              </FieldGroup>
            </form>
          </CardContent>
        </Card>
      )}
      <Card>
        <CardHeader>
          <CardTitle>역할별 권한</CardTitle>
          <CardDescription>
            API 접근에 사용되는 서버 권한 키입니다.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {catalog.isPending && (
            <LoadingPanel label="권한을 불러오는 중…" />
          )}
          {catalog.isError && (
            <Alert variant="destructive" role="alert">
              <AlertTitle>조회 오류</AlertTitle>
              <AlertDescription>
                {catalog.error instanceof ApiClientError
                  ? catalog.error.message
                  : "권한을 불러올 수 없습니다."}
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
                        {item.enabled ? "비활성화" : "활성화"}
                      </Button>
                      </CardAction>
                    )}
                  </CardHeader>
                  <CardContent>
                  <FieldSet>
                    <FieldLegend className="sr-only">{item.displayName || item.role} 권한</FieldLegend>
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
                      부여된 권한 없음
                    </p>
                  )}
                  {update.isError && (
                    <Alert className="mt-3" variant="destructive">
                      <AlertDescription>
                        {update.error instanceof ApiClientError
                          ? update.error.message
                          : "권한을 변경할 수 없습니다."}
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
