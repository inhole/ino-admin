import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import {
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";
import {
  getRoleCatalog,
  getUser,
  getUsers,
  updateUserProfile,
  updateUserStatus,
  type UserSummary,
} from "@/features/users/api/usersApi";
import { ApiClientError } from "@/api/client";
import {
  DEFAULT_USER_LIST_QUERY,
  parseUserListQuery,
  toUserListSearchParams,
  type UserListQuery,
} from "@/features/users/hook/userListQuery";
import { userKeys } from "@/features/users/hook/userKeys";
import { PageHeader, StatusPanel } from "@/components/layout/Page";
import { EmptyState, ErrorState } from "@/components/states/PageStates";
import { Alert, AlertDescription } from "@/components/ui/alert";
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
import { useAuth } from "@/features/auth/hook/useAuth";
import { CreateUserDialog } from "@/features/users/component/CreateUserDialog";
import { UserList } from "@/features/users/component/UserList";
import { UserListPagination } from "@/features/users/component/UserListPagination";
import { UserListToolbar } from "@/features/users/component/UserListToolbar";

export function UsersPage() {
  const { t } = useTranslation("users");
  const { t: common } = useTranslation("common");
  const [searchParams, setSearchParams] = useSearchParams();
  const query = parseUserListQuery(searchParams);
  const users = useQuery({
    queryKey: userKeys.list(query),
    queryFn: () => getUsers(query),
    placeholderData: keepPreviousData,
  });
  const roles = useQuery({
    queryKey: userKeys.roles,
    queryFn: getRoleCatalog,
  });
  const { user: currentUser } = useAuth();
  const queryClient = useQueryClient();
  const [statusError, setStatusError] = useState<string | null>(null);
  const [editing, setEditing] = useState<UserSummary | null>(null);
  const correctedPageRef = useRef<string | null>(null);
  const activeRoleOptions =
    roles.data?.map((role) => ({
        value: role.role,
        label: role.displayName || role.role,
      })) ?? [];
  const roleOptions = activeRoleOptions.filter(
    (role) => role.value !== "SUPER_ADMIN",
  );
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
      await queryClient.invalidateQueries({ queryKey: userKeys.all });
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
      await queryClient.invalidateQueries({ queryKey: userKeys.all });
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
  const statusAction = (user: UserSummary) => ({
    id: user.id,
    status:
      user.status === "ACTIVE" ? ("DISABLED" as const) : ("ACTIVE" as const),
  });
  const updateQuery = useCallback(
    (next: UserListQuery, options?: { replace?: boolean }) => {
      setSearchParams(toUserListSearchParams(next), options);
    },
    [setSearchParams],
  );
  const resetQuery = useCallback(() => {
    updateQuery(DEFAULT_USER_LIST_QUERY);
  }, [updateQuery]);

  const rawSearch = searchParams.toString();
  const canonicalSearch = toUserListSearchParams(query).toString();
  useEffect(() => {
    if (rawSearch !== canonicalSearch) {
      setSearchParams(canonicalSearch, { replace: true });
    }
  }, [canonicalSearch, rawSearch, setSearchParams]);

  useEffect(() => {
    const data = users.data;
    const correctedPage = data ? data.totalPages - 1 : -1;
    if (
      data?.content.length === 0 &&
      query.page > 0 &&
      data.totalPages > 0 &&
      query.page !== correctedPage
    ) {
      const correctionKey = `${toUserListSearchParams(query)}:${correctedPage}`;
      if (correctedPageRef.current === correctionKey) return;
      correctedPageRef.current = correctionKey;
      updateQuery(
        { ...query, page: correctedPage },
        { replace: true },
      );
      return;
    }
    correctedPageRef.current = null;
  }, [query, updateQuery, users.data]);

  const hasFilters =
    query.query !== "" || query.role !== "" || query.status !== "";
  const isRefreshing = users.isFetching && !users.isPending;
  const canCreateUsers = currentUser?.permissions.includes("user:create") ?? false;
  return (
    <>
      <PageHeader
        description={t("description")}
        eyebrow={t("eyebrow")}
        title={t("title")}
      />
      {canCreateUsers && (
        <>
          {roles.isPending && (
            <div
              aria-label={t("creationRolesLoading")}
              className="mb-4 flex items-center gap-2 text-sm text-muted-foreground"
              role="status"
            >
              <Spinner />
              {t("creationRolesLoading")}
            </div>
          )}
          {roles.isError && (
            <Alert className="mb-4" variant="destructive">
              <AlertDescription className="flex items-center justify-between gap-3">
                <span>{t("creationRolesError")}</span>
                <Button onClick={() => roles.refetch()} type="button" variant="outline">
                  {t("retry")}
                </Button>
              </AlertDescription>
            </Alert>
          )}
          {roles.isSuccess && roleOptions.length === 0 && (
            <StatusPanel className="mb-4">{t("creationRolesEmpty")}</StatusPanel>
          )}
          {roles.isSuccess && roleOptions.length > 0 && (
            <CreateUserDialog roles={roleOptions} />
          )}
        </>
      )}
      <Card>
        <CardHeader>
          <CardTitle>{t("listTitle")}</CardTitle>
          <CardDescription>{t("listDescription")}</CardDescription>
        </CardHeader>
        <CardContent>
          <UserListToolbar
            onChange={updateQuery}
            onReset={resetQuery}
            roles={activeRoleOptions}
            value={query}
          />
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
            <ErrorState
              description={
                users.error instanceof ApiClientError && users.error.status === 400
                  ? t("invalidQuery")
                  : undefined
              }
              error={users.error}
              forbiddenDescription={t("forbidden")}
              onRetry={() => users.refetch()}
              title={t("listError")}
            />
          )}
          {!users.isError && isRefreshing && (
            <p
              aria-label={t("refreshing")}
              aria-live="polite"
              className="mb-3 text-sm text-muted-foreground"
              role="status"
            >
              {t("refreshing")}
            </p>
          )}
          {!users.isError &&
            !isRefreshing &&
            users.data?.content.length === 0 &&
            !hasFilters && (
            <StatusPanel>{t("empty")}</StatusPanel>
          )}
          {!users.isError &&
            !isRefreshing &&
            users.data?.content.length === 0 &&
            hasFilters && (
              <EmptyState
                action={
                  <Button onClick={resetQuery} type="button" variant="outline">
                    {t("query.reset")}
                  </Button>
                }
                title={t("filteredEmpty")}
              />
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
                  {common("save")}
                </Button>
                <Button
                  onClick={() => setEditing(null)}
                  type="button"
                  variant="outline"
                >
                  {common("cancel")}
                </Button>
              </div>
              </FieldGroup>
            </form>
          )}
          {!users.isError && users.data && users.data.content.length > 0 && (
            <>
              <UserList
                canUpdate={
                  currentUser?.permissions.includes("user:update") ?? false
                }
                currentUserId={currentUser?.id}
                isStatusPending={changeStatus.isPending}
                onEdit={startEditing}
                onStatusChange={(user) => changeStatus.mutate(statusAction(user))}
                users={users.data.content}
              />
              <UserListPagination
                count={users.data.totalElements}
                onPageChange={(page) => updateQuery({ ...query, page })}
                page={query.page}
                totalPages={users.data.totalPages}
              />
            </>
          )}
        </CardContent>
      </Card>
    </>
  );
}
