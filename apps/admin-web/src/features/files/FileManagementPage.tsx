import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  RiCloseLine,
  RiRefreshLine,
  RiUploadCloud2Line,
} from "@remixicon/react";
import { useMemo, useState, type ChangeEvent, type DragEvent } from "react";
import { useTranslation } from "react-i18next";
import { ApiClientError } from "@/api/client";
import { PageHeader, StatusPanel } from "@/components/layout/Page";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Field, FieldDescription } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  Item,
  ItemActions,
  ItemContent,
  ItemDescription,
  ItemFooter,
  ItemGroup,
  ItemTitle,
} from "@/components/ui/item";
import { Progress, ProgressValue } from "@/components/ui/progress";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { toast } from "@/components/ui/toast";
import {
  deleteFile,
  downloadFile,
  getMyFiles,
  uploadFile,
  type FileListParams,
  type StoredFileSummary,
} from "@/features/files/api/filesApi";
import { FileListFilters } from "@/features/files/component/FileListFilters";
import { FileRowActions } from "@/features/files/component/FileRowActions";
import { fileKeys } from "@/features/files/hook/fileKeys";
import {
  defaultFileListFilters,
  type FileListFiltersValue,
} from "@/features/files/model/fileListFilters";
import { formatDateTime, formatFileSize } from "@/i18n/format";

type UploadStatus = "queued" | "uploading" | "success" | "error";

interface UploadQueueItem {
  id: string;
  file: File;
  status: UploadStatus;
  progress: number;
  error?: string;
}

let uploadSequence = 0;

function createUploadItem(file: File): UploadQueueItem {
  uploadSequence += 1;
  return {
    id: `${file.name}-${file.lastModified}-${uploadSequence}`,
    file,
    status: "queued",
    progress: 0,
  };
}

function localDateBoundary(value: string, nextDay = false) {
  if (!value) return undefined;
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day + (nextDay ? 1 : 0)).toISOString();
}

function toListParams(filters: FileListFiltersValue): FileListParams {
  const [sort, direction] = filters.order.split(",") as [
    FileListParams["sort"],
    FileListParams["direction"],
  ];
  return {
    name: filters.name.trim() || undefined,
    contentType:
      filters.contentType === "all" ? undefined : filters.contentType,
    createdFrom: localDateBoundary(filters.createdFrom),
    createdTo: localDateBoundary(filters.createdTo, true),
    sort,
    direction,
  };
}

export function FileManagementPage() {
  const { t } = useTranslation("files");
  const { t: common } = useTranslation("common");
  const queryClient = useQueryClient();
  const [listFilters, setListFilters] = useState(defaultFileListFilters);
  const listParams = useMemo(() => toListParams(listFilters), [listFilters]);
  const files = useQuery({
    queryKey: fileKeys.list(listParams),
    queryFn: () => getMyFiles(listParams),
  });
  const [error, setError] = useState<string | null>(null);
  const [uploadItems, setUploadItems] = useState<UploadQueueItem[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const isUploading = uploadItems.some((item) => item.status === "uploading");
  const hasListFilters = Boolean(
    listParams.name ||
      listParams.contentType ||
      listParams.createdFrom ||
      listParams.createdTo,
  );

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

  const addFiles = (selectedFiles: File[]) => {
    if (selectedFiles.length === 0) return;
    setUploadItems((current) => [
      ...current,
      ...selectedFiles.map(createUploadItem),
    ]);
  };

  const select = (event: ChangeEvent<HTMLInputElement>) => {
    addFiles(Array.from(event.target.files ?? []));
    event.target.value = "";
  };

  const drop = (event: DragEvent<HTMLElement>) => {
    event.preventDefault();
    setIsDragging(false);
    addFiles(Array.from(event.dataTransfer.files));
  };

  const updateUploadItem = (
    id: string,
    update: Partial<Omit<UploadQueueItem, "id" | "file">>,
  ) => {
    setUploadItems((current) =>
      current.map((item) => (item.id === id ? { ...item, ...update } : item)),
    );
  };

  const uploadTargets = async (targets: UploadQueueItem[]) => {
    if (targets.length === 0 || isUploading) return;
    targets.forEach((item) =>
      updateUploadItem(item.id, {
        status: "uploading",
        progress: 0,
        error: undefined,
      }),
    );

    const outcomes = await Promise.all(
      targets.map(async (item) => {
        try {
          await uploadFile(item.file, (progress) =>
            updateUploadItem(item.id, { progress }),
          );
          updateUploadItem(item.id, { status: "success", progress: 100 });
          return true;
        } catch (caught) {
          updateUploadItem(item.id, {
            status: "error",
            error:
              caught instanceof ApiClientError
                ? caught.message
                : t("uploadError"),
          });
          return false;
        }
      }),
    );

    const successCount = outcomes.filter(Boolean).length;
    const failureCount = outcomes.length - successCount;
    if (successCount > 0) {
      await queryClient.invalidateQueries({ queryKey: fileKeys.all });
    }
    toast.add({
      title: t("uploadSummary", { successCount, failureCount }),
    });
  };

  const uploadQueued = () => {
    void uploadTargets(
      uploadItems.filter(
        (item) => item.status === "queued" || item.status === "error",
      ),
    );
  };

  const removeUploadItem = (id: string) => {
    setUploadItems((current) => current.filter((item) => item.id !== id));
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

  const statusBadge = (item: UploadQueueItem) => {
    const variant =
      item.status === "error"
        ? "destructive"
        : item.status === "queued"
          ? "outline"
          : "secondary";
    return <Badge variant={variant}>{t(`status.${item.status}`)}</Badge>;
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
        <CardContent className="flex flex-col gap-5">
          <Field>
            <Button
              className="min-h-32 w-full flex-col"
              disabled={isUploading}
              nativeButton={false}
              onDragEnter={() => setIsDragging(true)}
              onDragLeave={() => setIsDragging(false)}
              onDragOver={(event) => event.preventDefault()}
              onDrop={drop}
              render={<label htmlFor="file-upload" />}
              variant="outline"
            >
              <RiUploadCloud2Line aria-hidden="true" data-icon="inline-start" />
              {isDragging ? t("dropActive") : t("input")}
              <Input
                accept=".pdf,.png,.jpg,.jpeg,.txt"
                aria-label={t("input")}
                className="sr-only"
                disabled={isUploading}
                id="file-upload"
                multiple
                onChange={select}
                type="file"
              />
            </Button>
            <FieldDescription>{t("dropHint")}</FieldDescription>
          </Field>

          {uploadItems.length > 0 && (
            <>
              <Separator />
              <div className="flex flex-wrap items-center justify-between gap-3">
                <p className="text-sm font-medium">
                  {t("queueTitle", { count: uploadItems.length })}
                </p>
                <Button
                  disabled={
                    isUploading ||
                    !uploadItems.some(
                      (item) =>
                        item.status === "queued" || item.status === "error",
                    )
                  }
                  onClick={uploadQueued}
                >
                  <RiUploadCloud2Line data-icon="inline-start" />
                  {isUploading ? t("uploading") : t("uploadAll")}
                </Button>
              </div>
              <ItemGroup aria-live="polite">
                {uploadItems.map((item) => (
                  <Item key={item.id} variant="outline">
                    <ItemContent className="min-w-0">
                      <ItemTitle className="break-all">
                        {item.file.name}
                      </ItemTitle>
                      <ItemDescription>
                        {formatFileSize(item.file.size)} ·{" "}
                        {item.file.type || t("unknownType")}
                      </ItemDescription>
                    </ItemContent>
                    {statusBadge(item)}
                    <ItemActions>
                      {item.status === "error" && (
                        <Button
                          aria-label={t("retryFile", { name: item.file.name })}
                          disabled={isUploading}
                          onClick={() => void uploadTargets([item])}
                          size="icon-sm"
                          title={common("retry")}
                          variant="outline"
                        >
                          <RiRefreshLine />
                        </Button>
                      )}
                      {item.status !== "uploading" && (
                        <Button
                          aria-label={t("removeFile", { name: item.file.name })}
                          disabled={isUploading}
                          onClick={() => removeUploadItem(item.id)}
                          size="icon-sm"
                          title={t("removeFromQueue")}
                          variant="ghost"
                        >
                          <RiCloseLine />
                        </Button>
                      )}
                    </ItemActions>
                    {item.status === "uploading" && (
                      <ItemFooter className="w-full">
                        <Progress
                          aria-label={t("fileProgress", {
                            name: item.file.name,
                          })}
                          className="w-full"
                          value={item.progress}
                        >
                          <ProgressValue />
                        </Progress>
                      </ItemFooter>
                    )}
                    {item.status === "error" && item.error && (
                      <ItemFooter className="w-full">
                        <p className="text-sm text-destructive">{item.error}</p>
                      </ItemFooter>
                    )}
                  </Item>
                ))}
              </ItemGroup>
            </>
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
        <CardContent className="flex flex-col gap-5">
          <FileListFilters onApply={setListFilters} value={listFilters} />
          <Separator />
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
            <StatusPanel>
              {hasListFilters ? t("filter.empty") : t("empty")}
            </StatusPanel>
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
                  <ItemActions>
                    <FileRowActions
                      deleting={remove.isPending}
                      file={file}
                      onDelete={(fileId) => remove.mutate(fileId)}
                      onDownload={download}
                    />
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
