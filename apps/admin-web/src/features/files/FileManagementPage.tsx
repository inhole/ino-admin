import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Download, FileUp, Trash2 } from "lucide-react";
import { useState, type ChangeEvent } from "react";
import { useTranslation } from "react-i18next";
import {
  deleteFile,
  downloadFile,
  getMyFiles,
  uploadFile,
  type StoredFileSummary,
} from "@/features/files/api/filesApi";
import { ApiClientError } from "@/api/client";
import { fileKeys } from "@/features/files/hook/fileKeys";
import { PageHeader, StatusPanel } from "@/components/layout/Page";
import { Alert, AlertDescription } from "@/components/ui/alert";
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
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Field,
  FieldDescription,
} from "@/components/ui/field";
import {
  Item,
  ItemActions,
  ItemContent,
  ItemDescription,
  ItemGroup,
  ItemTitle,
} from "@/components/ui/item";
import { Skeleton } from "@/components/ui/skeleton";
import { toast } from "@/components/ui/toast";
import { formatDateTime, formatFileSize } from "@/i18n/format";

export function FileManagementPage() {
  const { t } = useTranslation("files");
  const { t: common } = useTranslation("common");
  const queryClient = useQueryClient();
  const files = useQuery({ queryKey: fileKeys.all, queryFn: getMyFiles });
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [deleteDialogId, setDeleteDialogId] = useState<string | null>(null);
  const upload = useMutation({
    mutationFn: uploadFile,
    onSuccess: async () => {
      setError(null);
      toast.add({ title: t("uploaded") });
      setSelected(null);
      await queryClient.invalidateQueries({ queryKey: fileKeys.all });
    },
    onError: (caught) =>
      setError(
        caught instanceof ApiClientError ? caught.message : t("uploadError"),
      ),
  });
  const remove = useMutation({
    mutationFn: deleteFile,
    onSuccess: async () => {
      setError(null);
      toast.add({ title: t("deleted") });
      await queryClient.invalidateQueries({ queryKey: fileKeys.all });
    },
    onError: (caught) =>
      setError(
        caught instanceof ApiClientError ? caught.message : t("deleteError"),
      ),
  });
  const select = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    setSelected(file?.name ?? null);
    if (file) upload.mutate(file);
    event.target.value = "";
  };
  const download = async (file: StoredFileSummary) => {
    try {
      const blob = await downloadFile(file.id);
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = file.originalName;
      link.click();
      URL.revokeObjectURL(url);
    } catch (caught) {
      setError(
        caught instanceof ApiClientError ? caught.message : t("downloadError"),
      );
    }
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
          <CardTitle>{t("uploadTitle")}</CardTitle>
          <CardDescription>{t("uploadDescription")}</CardDescription>
        </CardHeader>
        <CardContent>
          <Field>
          <Button
            className="min-h-32 w-full flex-col"
            nativeButton={false}
            render={<label htmlFor="file-upload" />}
            variant="outline"
          >
            <FileUp aria-hidden="true" data-icon="inline-start" />
            {t("input")}
            <Input
              aria-label={t("input")}
              accept=".pdf,.png,.jpg,.jpeg,.txt"
              className="sr-only"
              disabled={upload.isPending}
              id="file-upload"
              onChange={select}
              type="file"
            />
          </Button>
          {selected && (
            <FieldDescription className="break-all">
              {t("selected", { name: selected })}
            </FieldDescription>
          )}
          </Field>
          {upload.isPending && (
            <div
              className="mt-4 grid gap-2"
              role="status"
              aria-label={t("uploading")}
            >
              <Skeleton className="h-2 w-full" />
              <p className="text-sm">{t("uploading")}</p>
            </div>
          )}
        </CardContent>
      </Card>
      {error && (
        <Alert className="mb-4" variant="destructive" role="alert">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      <Card>
        <CardHeader>
          <CardTitle>{t("listTitle")}</CardTitle>
          <CardDescription>{t("listDescription")}</CardDescription>
        </CardHeader>
        <CardContent>
          {files.isPending && (
            <div className="grid gap-3" role="status" aria-label={t("loading")}>
              {[1, 2, 3].map((row) => (
                <Skeleton className="h-20 w-full rounded-xl" key={row} />
              ))}
            </div>
          )}
          {files.isError && (
            <Alert variant="destructive" role="alert">
              <AlertDescription>{t("loadError")}</AlertDescription>
            </Alert>
          )}
          {files.data?.content.length === 0 && (
            <StatusPanel>{t("empty")}</StatusPanel>
          )}
          {files.data && files.data.content.length > 0 && (
            <ItemGroup>
              {files.data.content.map((file) => (
                <Item key={file.id} variant="outline">
                  <ItemContent>
                    <ItemTitle className="break-all">
                      {file.originalName}
                    </ItemTitle>
                    <ItemDescription>
                      {formatFileSize(file.size)} ·{" "}
                      {formatDateTime(file.createdAt)}
                    </ItemDescription>
                  </ItemContent>
                  <ItemActions className="w-full sm:w-auto">
                    <Button
                      className="min-h-11 flex-1 sm:flex-none"
                      onClick={() => download(file)}
                      variant="outline"
                    >
                      <Download data-icon="inline-start" />
                      {common("download")}
                    </Button>
                    <AlertDialog
                      onOpenChange={(open) =>
                        setDeleteDialogId(open ? file.id : null)
                      }
                      open={deleteDialogId === file.id}
                    >
                      <AlertDialogTrigger render={
                        <Button
                          className="min-h-11 flex-1 sm:flex-none"
                          disabled={remove.isPending}
                          variant="destructive"
                        >
                          <Trash2 data-icon="inline-start" />
                          {common("remove")}
                        </Button>
                      } />
                      <AlertDialogContent>
                        <AlertDialogHeader>
                          <AlertDialogTitle>{t("deleteTitle")}</AlertDialogTitle>
                          <AlertDialogDescription>{t("deleteDescription", { name: file.originalName })}</AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                          <AlertDialogCancel>{common("cancel")}</AlertDialogCancel>
                          <AlertDialogAction variant="destructive" onClick={() => {
                            remove.mutate(file.id);
                            setDeleteDialogId(null);
                          }}>{common("remove")}</AlertDialogAction>
                        </AlertDialogFooter>
                      </AlertDialogContent>
                    </AlertDialog>
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
