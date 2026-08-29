import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  createMenu,
  getMenus,
  updateMenu,
  type ManagedMenu,
} from "@/features/menus/api/menusApi";
import { ApiClientError } from "@/api/client";
import { menuKeys } from "@/features/menus/hook/menuKeys";
import { LoadingPanel, PageHeader, StatusPanel } from "@/components/layout/Page";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { MenuDialog } from "@/features/menus/component/MenuDialog";
import { Field, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  Item,
  ItemActions,
  ItemContent,
  ItemDescription,
  ItemGroup,
  ItemTitle,
} from "@/components/ui/item";

export function MenuManagementPage() {
  const { t } = useTranslation("menus");
  const { t: common } = useTranslation("common");
  const queryClient = useQueryClient();
  const menus = useQuery({ queryKey: menuKeys.all, queryFn: getMenus });
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const filteredMenus = useMemo(() => {
    const normalizedSearch = search.trim().toLocaleLowerCase();
    if (!normalizedSearch) return menus.data ?? [];

    return (menus.data ?? []).filter((menu) =>
      [menu.label, menu.route, menu.requiredPermission]
        .filter((value): value is string => value !== null)
        .some((value) => value.toLocaleLowerCase().includes(normalizedSearch)),
    );
  }, [menus.data, search]);
  const save = useMutation({
    mutationFn: createMenu,
    onSuccess: async () => {
      setError(null);
      await queryClient.invalidateQueries({ queryKey: menuKeys.root });
    },
    onError: (e) =>
      setError(e instanceof ApiClientError ? e.message : t("saveError")),
  });
  const toggle = useMutation({
    mutationFn: (menu: ManagedMenu) =>
      updateMenu(menu.id, { ...menu, enabled: !menu.enabled }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: menuKeys.root });
    },
    onError: (e) =>
      setError(e instanceof ApiClientError ? e.message : t("statusError")),
  });
  const edit = useMutation({
    mutationFn: ({ id, value }: { id: string; value: ManagedMenu }) =>
      updateMenu(id, value),
    onSuccess: async () => {
      setError(null);
      await queryClient.invalidateQueries({ queryKey: menuKeys.root });
    },
    onError: (e) =>
      setError(e instanceof ApiClientError ? e.message : t("saveError")),
  });
  return (
    <>
      <PageHeader
        actions={
          <MenuDialog
            onSave={(value) => save.mutateAsync(value)}
            pending={save.isPending}
          />
        }
        description={t("description")}
        eyebrow={t("eyebrow")}
        title={t("title")}
      />
      <Card>
        <CardHeader>
          <CardTitle>{t("allTitle")}</CardTitle>
        </CardHeader>
        <CardContent>
          <Field className="mb-4 max-w-md">
            <FieldLabel htmlFor="menu-search">{t("search")}</FieldLabel>
            <Input
              id="menu-search"
              onChange={(event) => setSearch(event.target.value)}
              placeholder={t("searchPlaceholder")}
              type="search"
              value={search}
            />
          </Field>
          {error && (
            <Alert className="mb-4" variant="destructive">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
          {menus.isPending && (
            <LoadingPanel label={t("loading")} />
          )}
          {menus.isError && (
            <Alert variant="destructive" role="alert">
              <AlertDescription>{t("loadError")}</AlertDescription>
            </Alert>
          )}
          {menus.data?.length === 0 && <StatusPanel>{t("empty")}</StatusPanel>}
          {menus.data && menus.data.length > 0 && filteredMenus.length === 0 && (
            <StatusPanel>{t("searchEmpty")}</StatusPanel>
          )}
          {menus.data && (
            <ItemGroup>
              {filteredMenus.map((menu) => (
                <Item key={menu.id} variant="outline">
                  <ItemContent>
                    <ItemTitle>{menu.label}</ItemTitle>
                    <ItemDescription className="break-all">
                      {menu.route}<br />
                      {menu.requiredPermission ?? t("public")} ·{" "}
                      {common("order", { value: menu.order })}
                    </ItemDescription>
                  </ItemContent>
                  <ItemActions>
                    <MenuDialog
                      menu={menu}
                      onSave={(value) =>
                        edit.mutateAsync({ id: menu.id, value })
                      }
                      pending={edit.isPending}
                    />
                    <Button
                      className="min-h-11 sm:min-w-24"
                      disabled={toggle.isPending}
                      onClick={() => toggle.mutate(menu)}
                      variant="outline"
                    >
                      {menu.enabled ? common("disable") : common("enable")}
                    </Button>
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
