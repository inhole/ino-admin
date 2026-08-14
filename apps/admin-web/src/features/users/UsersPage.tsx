import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import {
  createUser,
  getUser,
  getUsers,
  updateUserProfile,
  updateUserStatus,
  type UserSummary,
} from "@/features/users/api/usersApi";
import { ApiClientError } from "@/api/client";
import { getPermissionCatalog } from "@/features/permissions/api/permissionsApi";
import { PageHeader, StatusPanel } from "@/components/layout/Page";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Spinner } from "@/components/ui/spinner";
import { toast } from "@/components/ui/toast";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useAuth } from "@/features/auth/model/useAuth";
import { formatDate } from "@/i18n/format";

export function UsersPage() {
  const { t } = useTranslation("users");
  const users = useQuery({ queryKey: ["users"], queryFn: () => getUsers() });
  const roles = useQuery({
    queryKey: ["permissions"],
    queryFn: getPermissionCatalog,
  });
  const { user: currentUser } = useAuth();
  const queryClient = useQueryClient();
  const [createError, setCreateError] = useState<string | null>(null);
  const [statusError, setStatusError] = useState<string | null>(null);
  const [statusDialogId, setStatusDialogId] = useState<string | null>(null);
  const [editing, setEditing] = useState<UserSummary | null>(null);
  const roleOptions =
    roles.data
      ?.filter((role) => role.role !== "SUPER_ADMIN" && role.enabled)
      .map((role) => ({
        value: role.role,
        label: role.displayName || role.role,
      })) ?? [];
  const create = useMutation({
    mutationFn: createUser,
    onSuccess: async (created) => {
      toast.add({ title: t("created", { name: created.displayName }) });
      await queryClient.invalidateQueries({ queryKey: ["users"] });
    },
  });
  const changeStatus = useMutation({
    mutationFn: ({
      id,
      status,
    }: {
      id: string;
      status: "ACTIVE" | "DISABLED";
    }) => updateUserStatus(id, status),
    onSuccess: async () => {
      setStatusError(null);
      await queryClient.invalidateQueries({ queryKey: ["users"] });
    },
    onError: (error) =>
      setStatusError(
        error instanceof ApiClientError ? error.message : t("statusError"),
      ),
  });
  const update = useMutation({
    mutationFn: ({
      id,
      displayName,
      role,
    }: {
      id: string;
      displayName: string;
      role: string;
    }) => updateUserProfile(id, { displayName, role }),
    onSuccess: async () => {
      setEditing(null);
      setStatusError(null);
      await queryClient.invalidateQueries({ queryKey: ["users"] });
    },
    onError: (error) =>
      setStatusError(
        error instanceof ApiClientError ? error.message : t("updateError"),
      ),
  });
  const startEditing = async (id: string) => {
    try {
      setEditing(await getUser(id));
      setStatusError(null);
    } catch (error) {
      setStatusError(
        error instanceof ApiClientError ? error.message : t("loadOneError"),
      );
    }
  };
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setCreateError(null);
    const form = event.currentTarget;
    const data = new FormData(form);
    try {
      await create.mutateAsync({
        email: String(data.get("email")),
        password: String(data.get("password")),
        displayName: String(data.get("displayName")),
        role: String(data.get("role")),
      });
      form.reset();
    } catch (error) {
      setCreateError(
        error instanceof ApiClientError ? error.message : t("createError"),
      );
    }
  };
  const statusAction = (user: UserSummary) => ({
    id: user.id,
    status:
      user.status === "ACTIVE" ? ("DISABLED" as const) : ("ACTIVE" as const),
  });
  const statusLabel = (user: UserSummary) =>
    user.status === "ACTIVE"
      ? t("deactivate")
      : user.status === "LOCKED"
        ? t("unlock")
        : t("activate");
  return (
    <>
      <PageHeader
        description={t("description")}
        eyebrow={t("eyebrow")}
        title={t("title")}
      />
      {currentUser?.permissions.includes("user:create") && (
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>{t("createTitle")}</CardTitle>
            <CardDescription>{t("createDescription")}</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={submit}>
              <FieldGroup className="grid gap-4 md:grid-cols-2">
              <Field>
                <FieldLabel htmlFor="displayName">{t("name")}</FieldLabel>
                <Input id="displayName" name="displayName" required />
              </Field>
              <Field>
                <FieldLabel htmlFor="new-user-email">{t("email")}</FieldLabel>
                <Input id="new-user-email" name="email" required type="email" />
              </Field>
              <Field>
                <FieldLabel htmlFor="new-user-password">
                  {t("initialPassword")}
                </FieldLabel>
                <Input
                  id="new-user-password"
                  minLength={12}
                  name="password"
                  required
                  type="password"
                />
              </Field>
              <Field>
                <FieldLabel>{t("role")}</FieldLabel>
                <Select defaultValue="VIEWER" items={roleOptions} name="role">
                  <SelectTrigger aria-label={t("role")} className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent><SelectGroup>{roleOptions.map((role) => <SelectItem key={role.value} value={role.value}>{role.label}</SelectItem>)}</SelectGroup></SelectContent>
                </Select>
              </Field>
              {createError && (
                <Alert
                  className="md:col-span-2"
                  variant="destructive"
                  role="alert"
                >
                  <AlertDescription>{createError}</AlertDescription>
                </Alert>
              )}
              <Button
                className="min-h-11 md:col-span-2 md:w-fit"
                disabled={create.isPending}
                type="submit"
              >
                {create.isPending && <Spinner data-icon="inline-start" />}
                {create.isPending ? t("creating") : t("create")}
              </Button>
              </FieldGroup>
            </form>
          </CardContent>
        </Card>
      )}
      <Card>
        <CardHeader>
          <CardTitle>{t("listTitle")}</CardTitle>
          <CardDescription>{t("listDescription")}</CardDescription>
        </CardHeader>
        <CardContent>
          {statusError && (
            <Alert className="mb-4" variant="destructive" role="alert">
              <AlertDescription>{statusError}</AlertDescription>
            </Alert>
          )}
          {users.isPending && (
            <div className="grid gap-3" role="status" aria-label={t("loading")}>
              {[1, 2, 3].map((row) => (
                <Skeleton className="h-14 w-full" key={row} />
              ))}
            </div>
          )}
          {users.isError && (
            <Alert variant="destructive" role="alert">
              <AlertTitle>{t("listError")}</AlertTitle>
              <AlertDescription>
                {users.error instanceof ApiClientError &&
                users.error.status === 403
                  ? t("forbidden")
                  : users.error.message}
              </AlertDescription>
              <Button
                className="mt-3"
                onClick={() => users.refetch()}
                variant="outline"
              >
                다시 시도
              </Button>
            </Alert>
          )}
          {users.data?.content.length === 0 && (
            <StatusPanel>{t("empty")}</StatusPanel>
          )}
          {editing && (
            <form
              className="mb-5 rounded-xl border bg-muted/20 p-4"
              onSubmit={(event) => {
                event.preventDefault();
                const data = new FormData(event.currentTarget);
                update.mutate({
                  id: editing.id,
                  displayName: String(data.get("displayName")),
                  role: String(data.get("role")),
                });
              }}
            >
              <FieldGroup className="grid gap-3 md:grid-cols-3">
              <Field>
              <FieldLabel htmlFor="edit-user-name">{t("editName")}</FieldLabel>
              <Input id="edit-user-name"
                aria-label={t("editName")}
                defaultValue={editing.displayName}
                maxLength={100}
                name="displayName"
                required
              /></Field>
              <Field><FieldLabel>{t("editRole")}</FieldLabel>
              <Select defaultValue={editing.role} items={roleOptions} name="role">
                <SelectTrigger aria-label={t("editRole")} className="w-full"><SelectValue /></SelectTrigger>
                <SelectContent><SelectGroup>{roleOptions.map((role) => <SelectItem key={role.value} value={role.value}>{role.label}</SelectItem>)}</SelectGroup></SelectContent>
              </Select></Field>
              <div className="flex gap-2">
                <Button disabled={update.isPending} type="submit">
                  {update.isPending && <Spinner data-icon="inline-start" />}
                  저장
                </Button>
                <Button
                  onClick={() => setEditing(null)}
                  type="button"
                  variant="outline"
                >
                  취소
                </Button>
              </div>
              </FieldGroup>
            </form>
          )}
          {users.data && users.data.content.length > 0 && (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("name")}</TableHead>
                  <TableHead>{t("email")}</TableHead>
                  <TableHead>{t("role")}</TableHead>
                  <TableHead>{t("status")}</TableHead>
                  <TableHead>{t("createdAt")}</TableHead>
                  <TableHead>{t("actions")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {users.data.content.map((user) => (
                  <TableRow key={user.id}>
                    <TableCell className="font-medium">
                      {user.displayName}
                    </TableCell>
                    <TableCell>{user.email}</TableCell>
                    <TableCell>
                      <Badge variant="secondary">{user.role}</Badge>
                    </TableCell>
                    <TableCell>
                      <Badge
                        variant={
                          user.status === "ACTIVE" ? "secondary" : "outline"
                        }
                      >
                        {user.status}
                      </Badge>
                    </TableCell>
                    <TableCell>{formatDate(user.createdAt)}</TableCell>
                    <TableCell>
                      {currentUser?.permissions.includes("user:update") &&
                        currentUser.id !== user.id && (
                          <div className="flex gap-2">
                            <Button
                              onClick={() => startEditing(user.id)}
                              size="sm"
                              variant="outline"
                            >
                              {t("editName").replace("할 이름", "")}
                            </Button>
                            <AlertDialog
                              onOpenChange={(open) =>
                                setStatusDialogId(open ? user.id : null)
                              }
                              open={statusDialogId === user.id}
                            >
                              <AlertDialogTrigger render={
                                <Button
                                  disabled={changeStatus.isPending}
                                  size="sm"
                                  variant="outline"
                                >
                                  {statusLabel(user)}
                                </Button>
                              } />
                              <AlertDialogContent>
                                <AlertDialogHeader>
                                  <AlertDialogTitle>{statusLabel(user)}할까요?</AlertDialogTitle>
                                  <AlertDialogDescription>{user.displayName} 사용자의 상태를 변경합니다.</AlertDialogDescription>
                                </AlertDialogHeader>
                                <AlertDialogFooter>
                                  <AlertDialogCancel>취소</AlertDialogCancel>
                                  <AlertDialogAction variant={user.status === "ACTIVE" ? "destructive" : "default"} onClick={() => {
                                    changeStatus.mutate(statusAction(user));
                                    setStatusDialogId(null);
                                  }}>{statusLabel(user)}</AlertDialogAction>
                                </AlertDialogFooter>
                              </AlertDialogContent>
                            </AlertDialog>
                          </div>
                        )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </>
  );
}
