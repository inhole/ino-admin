import {
  RiDeleteBinLine,
  RiDownloadLine,
  RiFileInfoLine,
  RiMore2Line,
} from "@remixicon/react";
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
} from "@/components/ui/alert-dialog";
import { Button, buttonVariants } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Item,
  ItemContent,
  ItemDescription,
  ItemGroup,
  ItemTitle,
} from "@/components/ui/item";
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import type { StoredFileSummary } from "@/features/files/api/filesApi";
import { formatDateTime, formatFileSize } from "@/i18n/format";

interface FileRowActionsProps {
  file: StoredFileSummary;
  deleting: boolean;
  onDelete: (fileId: string) => void;
  onDownload: (file: StoredFileSummary) => void | Promise<void>;
}

export function FileRowActions({
  file,
  deleting,
  onDelete,
  onDownload,
}: FileRowActionsProps) {
  const { t } = useTranslation("files");
  const { t: common } = useTranslation("common");
  const [detailOpen, setDetailOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const details = [
    { label: t("detail.name"), value: file.originalName },
    { label: t("detail.type"), value: file.contentType },
    { label: t("detail.size"), value: formatFileSize(file.size) },
    { label: t("detail.uploadedAt"), value: formatDateTime(file.createdAt) },
    { label: t("detail.id"), value: file.id },
  ];

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger
          aria-label={t("actionsFor", { name: file.originalName })}
          className={buttonVariants({ size: "icon-sm", variant: "outline" })}
          title={t("actionsFor", { name: file.originalName })}
        >
          <RiMore2Line />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuGroup>
            <DropdownMenuLabel>{t("actions")}</DropdownMenuLabel>
            <DropdownMenuItem onClick={() => setDetailOpen(true)}>
              <RiFileInfoLine />
              {t("details")}
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => void onDownload(file)}>
              <RiDownloadLine />
              {common("download")}
            </DropdownMenuItem>
          </DropdownMenuGroup>
          <DropdownMenuSeparator />
          <DropdownMenuGroup>
            <DropdownMenuItem
              disabled={deleting}
              onClick={() => setDeleteOpen(true)}
              variant="destructive"
            >
              <RiDeleteBinLine />
              {common("remove")}
            </DropdownMenuItem>
          </DropdownMenuGroup>
        </DropdownMenuContent>
      </DropdownMenu>

      <Sheet onOpenChange={setDetailOpen} open={detailOpen}>
        <SheetContent className="w-full sm:max-w-md" showCloseButton={false}>
          <SheetHeader>
            <SheetTitle>{t("detail.title")}</SheetTitle>
            <SheetDescription className="break-all">
              {t("detail.description", { name: file.originalName })}
            </SheetDescription>
          </SheetHeader>
          <ItemGroup className="px-6">
            {details.map((detail) => (
              <Item key={detail.label} size="sm" variant="outline">
                <ItemContent className="min-w-0">
                  <ItemDescription>{detail.label}</ItemDescription>
                  <ItemTitle className="break-all">{detail.value}</ItemTitle>
                </ItemContent>
              </Item>
            ))}
          </ItemGroup>
          <SheetFooter className="sm:flex-row sm:justify-end">
            <SheetClose
              className={buttonVariants({ variant: "outline" })}
            >
              {common("close")}
            </SheetClose>
            <Button onClick={() => void onDownload(file)}>
              <RiDownloadLine data-icon="inline-start" />
              {common("download")}
            </Button>
          </SheetFooter>
        </SheetContent>
      </Sheet>

      <AlertDialog onOpenChange={setDeleteOpen} open={deleteOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t("deleteTitle")}</AlertDialogTitle>
            <AlertDialogDescription>
              {t("deleteDescription", { name: file.originalName })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{common("cancel")}</AlertDialogCancel>
            <AlertDialogAction
              disabled={deleting}
              onClick={() => {
                onDelete(file.id);
                setDeleteOpen(false);
              }}
              variant="destructive"
            >
              {common("remove")}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
