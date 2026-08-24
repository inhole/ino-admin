import { RiAddLine, RiEditLine } from "@remixicon/react";
import { useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { FormField } from "@/components/layout/Page";
import { Button, buttonVariants } from "@/components/ui/button";
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { FieldGroup } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Spinner } from "@/components/ui/spinner";
import type { ManagedMenu } from "@/features/menus/api/menusApi";

type MenuInput = Omit<ManagedMenu, "enabled"> & { enabled: boolean };

export function MenuDialog({ menu, onSave, pending }: { menu?: ManagedMenu; onSave: (value: MenuInput) => Promise<unknown>; pending: boolean }) {
  const { t } = useTranslation("menus");
  const { t: common } = useTranslation("common");
  const [open, setOpen] = useState(false);
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    await onSave({
      id: menu?.id ?? String(data.get("id")), parentId: String(data.get("parentId")) || null,
      label: String(data.get("label")), route: String(data.get("route")), icon: String(data.get("icon")) as ManagedMenu["icon"],
      order: Number(data.get("order")), requiredPermission: String(data.get("requiredPermission")) || null, enabled: menu?.enabled ?? true,
    });
    setOpen(false);
  };
  return <Dialog onOpenChange={(next) => { if (!pending) setOpen(next); }} open={open}>
    <DialogTrigger render={<button className={buttonVariants({ variant: menu ? "outline" : "default", size: menu ? "sm" : "default" })} type="button" />}>
      {menu ? <RiEditLine data-icon="inline-start" /> : <RiAddLine data-icon="inline-start" />}{menu ? common("edit") : t("add")}
    </DialogTrigger>
    <DialogContent showCloseButton={false}>
      <DialogHeader><DialogTitle>{menu ? t("editTitle") : t("createTitle")}</DialogTitle><DialogDescription>{menu ? t("editDescription") : t("createDescription")}</DialogDescription></DialogHeader>
      <form onSubmit={submit}><FieldGroup>
        {!menu && <FormField htmlFor="dialog-menu-id" label={t("id")}><Input id="dialog-menu-id" name="id" placeholder="menu-id" required /></FormField>}
        <FormField htmlFor={`dialog-menu-name-${menu?.id ?? "new"}`} label={t("name")}><Input defaultValue={menu?.label} id={`dialog-menu-name-${menu?.id ?? "new"}`} name="label" required /></FormField>
        <FormField htmlFor={`dialog-menu-route-${menu?.id ?? "new"}`} label={t("route")}><Input defaultValue={menu?.route} id={`dialog-menu-route-${menu?.id ?? "new"}`} name="route" placeholder="/route" required /></FormField>
        <FormField htmlFor={`dialog-menu-icon-${menu?.id ?? "new"}`} label={t("icon")}><Input defaultValue={menu?.icon} id={`dialog-menu-icon-${menu?.id ?? "new"}`} name="icon" required /></FormField>
        <FormField htmlFor={`dialog-menu-parent-${menu?.id ?? "new"}`} label={t("parentId")}><Input defaultValue={menu?.parentId ?? ""} id={`dialog-menu-parent-${menu?.id ?? "new"}`} name="parentId" /></FormField>
        <FormField htmlFor={`dialog-menu-permission-${menu?.id ?? "new"}`} label={t("permission")}><Input defaultValue={menu?.requiredPermission ?? ""} id={`dialog-menu-permission-${menu?.id ?? "new"}`} name="requiredPermission" /></FormField>
        <FormField htmlFor={`dialog-menu-order-${menu?.id ?? "new"}`} label={t("order")}><Input defaultValue={menu?.order ?? 10} id={`dialog-menu-order-${menu?.id ?? "new"}`} min="0" name="order" required type="number" /></FormField>
        <DialogFooter><DialogClose disabled={pending} render={<button className={buttonVariants({ variant: "outline" })} type="button" />}>{common("cancel")}</DialogClose><Button disabled={pending} type="submit">{pending && <Spinner data-icon="inline-start" />}{common("save")}</Button></DialogFooter>
      </FieldGroup></form>
    </DialogContent>
  </Dialog>;
}
