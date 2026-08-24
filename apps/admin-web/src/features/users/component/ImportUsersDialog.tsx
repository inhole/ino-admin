import { RiDownloadLine, RiFileExcel2Line, RiUpload2Line } from "@remixicon/react";
import { useState, type FormEvent } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { ApiClientError } from "@/api/client";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button, buttonVariants } from "@/components/ui/button";
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Spinner } from "@/components/ui/spinner";
import { downloadUserImportTemplate, importUsersExcel } from "@/features/users/api/usersApi";
import { userKeys } from "@/features/users/hook/userKeys";

export function ImportUsersDialog() {
  const { t } = useTranslation("users");
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<number | null>(null);
  const [file, setFile] = useState<File | null>(null);

  const downloadTemplate = async () => {
    try {
      const blob = await downloadUserImportTemplate();
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url; anchor.download = "users-import-template.xlsx"; anchor.click(); URL.revokeObjectURL(url);
    } catch (caught) {
      setError(caught instanceof ApiClientError ? caught.message : t("importError"));
    }
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!file) { setError(t("importFileRequired")); return; }
    setPending(true); setError(null); setResult(null);
    try {
      const imported = await importUsersExcel(file);
      setResult(imported.createdCount);
      await queryClient.invalidateQueries({ queryKey: userKeys.all });
    } catch (caught) {
      setError(caught instanceof ApiClientError ? caught.message : t("importError"));
    } finally {
      setPending(false);
    }
  };

  return (
    <Dialog onOpenChange={(next) => { setOpen(next); if (next) { setError(null); setResult(null); setFile(null); } }} open={open}>
      <DialogTrigger render={<button className={buttonVariants({ variant: "outline" })} type="button" />}>
        <RiUpload2Line data-icon="inline-start" />{t("importExcel")}
      </DialogTrigger>
      <DialogContent showCloseButton={false}>
        <DialogHeader><DialogTitle>{t("importTitle")}</DialogTitle><DialogDescription>{t("importDescription")}</DialogDescription></DialogHeader>
        <form aria-label={t("importTitle")} onSubmit={submit}>
          <FieldGroup>
            <Button onClick={downloadTemplate} type="button" variant="secondary"><RiDownloadLine data-icon="inline-start" />{t("downloadTemplate")}</Button>
            <Field>
              <FieldLabel htmlFor="users-excel-file">{t("importFile")}</FieldLabel>
              <Input accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" id="users-excel-file" name="file" onChange={(event) => setFile(event.currentTarget.files?.[0] ?? null)} type="file" />
            </Field>
            {error && <Alert variant="destructive"><RiFileExcel2Line /><AlertDescription>{error}</AlertDescription></Alert>}
            {result !== null && <Alert><AlertDescription>{t("imported", { count: result })}</AlertDescription></Alert>}
            <DialogFooter>
              <DialogClose disabled={pending} render={<button className={buttonVariants({ variant: "outline" })} type="button" />}>{t("cancel")}</DialogClose>
              <Button disabled={pending} type="submit">{pending && <Spinner data-icon="inline-start" />}{pending ? t("importing") : t("importSubmit")}</Button>
            </DialogFooter>
          </FieldGroup>
        </form>
      </DialogContent>
    </Dialog>
  );
}
