import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import {
  createMenu,
  getMenus,
  updateMenu,
  type ManagedMenu,
} from "@/features/menus/api/menusApi";
import { ApiClientError } from "@/api/client";
import { menuKeys } from "@/features/menus/hook/menuKeys";
import { FormField, LoadingPanel, PageHeader, StatusPanel } from "@/components/layout/Page";
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
import { FieldGroup } from "@/components/ui/field";
import { Spinner } from "@/components/ui/spinner";
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
  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    save.mutate({
      id: String(data.get("id")),
      parentId: String(data.get("parentId")) || null,
      label: String(data.get("label")),
      route: String(data.get("route")),
      icon: String(data.get("icon")) as ManagedMenu["icon"],
      order: Number(data.get("order")),
      requiredPermission: String(data.get("requiredPermission")) || null,
      enabled: true,
    });
    form.reset();
  };
  return (
    <>
      <PageHeader
        description={t("description")}
        eyebrow={t("eyebrow")}
        title={t("title")}
      />
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>{t("createTitle")}</CardTitle>
          <CardDescription>{t("createDescription")}</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={submit}>
            <FieldGroup className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <FormField htmlFor="menu-id" label={t("id")}>
              <Input id="menu-id" name="id" placeholder="menu-id" required />
            </FormField>
            <FormField htmlFor="menu-name" label={t("name")}>
              <Input id="menu-name" name="label" required />
            </FormField>
            <FormField htmlFor="menu-route" label={t("route")}>
              <Input
                id="menu-route"
                name="route"
                placeholder="/route"
                required
              />
            </FormField>
            <FormField htmlFor="menu-icon" label={t("icon")}>
              <Input id="menu-icon" name="icon" placeholder="menu" required />
            </FormField>
            <FormField htmlFor="menu-parent" label={t("parentId")}>
              <Input id="menu-parent" name="parentId" />
            </FormField>
            <FormField htmlFor="menu-permission" label={t("permission")}>
              <Input
                id="menu-permission"
                name="requiredPermission"
                placeholder="resource:action"
              />
            </FormField>
            <FormField htmlFor="menu-order" label={t("order")}>
              <Input
                id="menu-order"
                min="0"
                name="order"
                placeholder="10"
                required
                type="number"
              />
            </FormField>
            <Button
              className="min-h-11 self-end"
              disabled={save.isPending}
              type="submit"
            >
              {save.isPending && <Spinner data-icon="inline-start" />}
              {t("add")}
            </Button>
            </FieldGroup>
          </form>
        </CardContent>
      </Card>
      <Card>
        <CardHeader>
          <CardTitle>{t("allTitle")}</CardTitle>
        </CardHeader>
        <CardContent>
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
          {menus.data && (
            <ItemGroup>
              {menus.data.map((menu) => (
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
