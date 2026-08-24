import { RiRefreshLine } from "@remixicon/react";
import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  DEFAULT_USER_LIST_QUERY,
  type SortDirection,
  type UserListQuery,
  type UserSort,
  type UserStatusFilter,
} from "@/features/users/hook/userListQuery";

interface RoleOption {
  value: string;
  label: string;
}

interface UserListToolbarProps {
  value: UserListQuery;
  roles: RoleOption[];
  onChange: (value: UserListQuery) => void;
  onReset: () => void;
}

const allRolesValue = "__all_roles__";
const allStatusesValue = "__all_statuses__";

export function UserListToolbar({
  value,
  roles,
  onChange,
  onReset,
}: UserListToolbarProps) {
  const { t } = useTranslation("users");
  const [searchDraft, setSearchDraft] = useState(value.query);
  const valueRef = useRef(value);
  valueRef.current = value;
  const roleOptions = [
    { value: allRolesValue, label: t("query.allRoles") },
    ...roles,
  ];
  const statusOptions = [
    { value: allStatusesValue, label: t("query.allStatuses") },
    { value: "ACTIVE", label: t("query.statuses.active") },
    { value: "LOCKED", label: t("query.statuses.locked") },
    { value: "DISABLED", label: t("query.statuses.disabled") },
  ];
  const sortOptions = [
    { value: "createdAt", label: t("query.sorts.createdAt") },
    { value: "displayName", label: t("query.sorts.displayName") },
    { value: "email", label: t("query.sorts.email") },
    { value: "role", label: t("query.sorts.role") },
    { value: "status", label: t("query.sorts.status") },
  ];
  const directionOptions = [
    { value: "desc", label: t("query.directions.desc") },
    { value: "asc", label: t("query.directions.asc") },
  ];

  useEffect(() => {
    setSearchDraft(value.query);
  }, [value.query]);

  useEffect(() => {
    if (searchDraft === value.query) return;
    const timeout = window.setTimeout(() => {
      onChange({ ...valueRef.current, query: searchDraft, page: 0 });
    }, 300);
    return () => window.clearTimeout(timeout);
  }, [onChange, searchDraft, value.query]);

  const update = (next: Partial<UserListQuery>) => {
    onChange({ ...value, ...next, page: 0 });
  };

  return (
    <div className="mb-5 border-b pb-5">
      <FieldGroup className="grid gap-3 md:grid-cols-2 xl:grid-cols-[minmax(15rem,2fr)_repeat(4,minmax(8rem,1fr))_auto] xl:items-end">
        <Field>
          <FieldLabel htmlFor="user-list-search">
            {t("query.search")}
          </FieldLabel>
          <Input
            id="user-list-search"
            maxLength={320}
            onChange={(event) => setSearchDraft(event.target.value)}
            placeholder={t("query.searchPlaceholder")}
            value={searchDraft}
          />
        </Field>
        <Field>
          <FieldLabel htmlFor="user-list-role">{t("query.role")}</FieldLabel>
          <Select
            items={roleOptions}
            onValueChange={(role) =>
              update({ role: role === allRolesValue ? "" : (role ?? "") })
            }
            value={value.role || allRolesValue}
          >
            <SelectTrigger className="w-full" id="user-list-role">
              <SelectValue>
                {
                  roleOptions.find(
                    (option) => option.value === (value.role || allRolesValue),
                  )?.label
                }
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectGroup>
                {roleOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectGroup>
            </SelectContent>
          </Select>
        </Field>
        <Field>
          <FieldLabel htmlFor="user-list-status">
            {t("query.status")}
          </FieldLabel>
          <Select
            items={statusOptions}
            onValueChange={(status) =>
              update({
                status:
                  status === allStatusesValue
                    ? ""
                    : ((status ?? "") as UserStatusFilter),
              })
            }
            value={value.status || allStatusesValue}
          >
            <SelectTrigger className="w-full" id="user-list-status">
              <SelectValue>
                {
                  statusOptions.find(
                    (option) =>
                      option.value === (value.status || allStatusesValue),
                  )?.label
                }
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectGroup>
                {statusOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectGroup>
            </SelectContent>
          </Select>
        </Field>
        <Field>
          <FieldLabel htmlFor="user-list-sort">{t("query.sort")}</FieldLabel>
          <Select
            items={sortOptions}
            onValueChange={(sort) =>
              update({
                sort: (sort ?? DEFAULT_USER_LIST_QUERY.sort) as UserSort,
              })
            }
            value={value.sort}
          >
            <SelectTrigger className="w-full" id="user-list-sort">
              <SelectValue>
                {sortOptions.find((option) => option.value === value.sort)?.label}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectGroup>
                {sortOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectGroup>
            </SelectContent>
          </Select>
        </Field>
        <Field>
          <FieldLabel htmlFor="user-list-direction">
            {t("query.direction")}
          </FieldLabel>
          <Select
            items={directionOptions}
            onValueChange={(direction) =>
              update({
                direction: (direction ??
                  DEFAULT_USER_LIST_QUERY.direction) as SortDirection,
              })
            }
            value={value.direction}
          >
            <SelectTrigger className="w-full" id="user-list-direction">
              <SelectValue>
                {
                  directionOptions.find(
                    (option) => option.value === value.direction,
                  )?.label
                }
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectGroup>
                {directionOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectGroup>
            </SelectContent>
          </Select>
        </Field>
        <Button
          className="min-h-9 md:w-fit xl:self-end"
          onClick={onReset}
          type="button"
          variant="outline"
        >
          <RiRefreshLine data-icon="inline-start" />
          {t("query.reset")}
        </Button>
      </FieldGroup>
    </div>
  );
}
