import { useState } from "react";
import { useTranslation } from "react-i18next";
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
  Item,
  ItemActions,
  ItemContent,
  ItemDescription,
  ItemGroup,
  ItemHeader,
  ItemTitle,
} from "@/components/ui/item";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { UserSummary } from "@/features/users/api/usersApi";
import { formatDate } from "@/i18n/format";

interface UserListProps {
  users: UserSummary[];
  canUpdate: boolean;
  currentUserId?: string;
  isStatusPending: boolean;
  onEdit: (id: string) => void;
  onStatusChange: (user: UserSummary) => void;
}

function statusLabelKey(user: UserSummary) {
  if (user.status === "ACTIVE") return "deactivate";
  if (user.status === "LOCKED") return "unlock";
  return "activate";
}

function UserActions({
  user,
  isStatusPending,
  onEdit,
  onStatusChange,
}: {
  user: UserSummary;
  isStatusPending: boolean;
  onEdit: (id: string) => void;
  onStatusChange: (user: UserSummary) => void;
}) {
  const { t } = useTranslation("users");
  const { t: common } = useTranslation("common");
  const statusLabel = t(statusLabelKey(user));
  const [statusDialogOpen, setStatusDialogOpen] = useState(false);

  return (
    <div className="flex flex-wrap gap-2">
      <Button onClick={() => onEdit(user.id)} size="sm" variant="outline">
        {common("edit")}
      </Button>
      <AlertDialog onOpenChange={setStatusDialogOpen} open={statusDialogOpen}>
        <AlertDialogTrigger
          render={
            <Button disabled={isStatusPending} size="sm" variant="outline">
              {statusLabel}
            </Button>
          }
        />
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {t("statusConfirmTitle", { action: statusLabel })}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {t("statusConfirmDescription", { name: user.displayName })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{common("cancel")}</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                onStatusChange(user);
                setStatusDialogOpen(false);
              }}
              variant={user.status === "ACTIVE" ? "destructive" : "default"}
            >
              {statusLabel}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}

export function UserList({
  users,
  canUpdate,
  currentUserId,
  isStatusPending,
  onEdit,
  onStatusChange,
}: UserListProps) {
  const { t } = useTranslation("users");
  const canManage = (user: UserSummary) => canUpdate && currentUserId !== user.id;

  return (
    <>
      <section
        aria-label={t("desktopList")}
        className="hidden md:block"
      >
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
            {users.map((user) => (
              <TableRow key={user.id}>
                <TableCell className="font-medium">{user.displayName}</TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>
                  <Badge variant="secondary">{user.role}</Badge>
                </TableCell>
                <TableCell>
                  <Badge
                    variant={user.status === "ACTIVE" ? "secondary" : "outline"}
                  >
                    {user.status}
                  </Badge>
                </TableCell>
                <TableCell>{formatDate(user.createdAt)}</TableCell>
                <TableCell>
                  {canManage(user) && (
                    <UserActions
                      isStatusPending={isStatusPending}
                      onEdit={onEdit}
                      onStatusChange={onStatusChange}
                      user={user}
                    />
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </section>
      <section aria-label={t("mobileList")} className="md:hidden">
        <ItemGroup>
          {users.map((user) => (
            <Item key={user.id} variant="outline">
              <ItemHeader>
                <ItemContent>
                  <ItemTitle>{user.displayName}</ItemTitle>
                  <ItemDescription>{user.email}</ItemDescription>
                </ItemContent>
                <div className="flex gap-2">
                  <Badge variant="secondary">{user.role}</Badge>
                  <Badge
                    variant={user.status === "ACTIVE" ? "secondary" : "outline"}
                  >
                    {user.status}
                  </Badge>
                </div>
              </ItemHeader>
              <ItemContent>
                <ItemDescription>
                  {t("createdAt")}: {formatDate(user.createdAt)}
                </ItemDescription>
              </ItemContent>
              {canManage(user) && (
                <ItemActions className="basis-full justify-end">
                  <UserActions
                    isStatusPending={isStatusPending}
                    onEdit={onEdit}
                    onStatusChange={onStatusChange}
                    user={user}
                  />
                </ItemActions>
              )}
            </Item>
          ))}
        </ItemGroup>
      </section>
    </>
  );
}
