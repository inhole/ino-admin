import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";

interface UserListPaginationProps {
  count: number;
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export function UserListPagination({
  count,
  page,
  totalPages,
  onPageChange,
}: UserListPaginationProps) {
  const { t } = useTranslation("users");
  const currentPage = totalPages === 0 ? 0 : page + 1;

  return (
    <nav
      aria-label={t("pagination.label")}
      className="mt-5 flex flex-col gap-3 border-t pt-4 sm:flex-row sm:items-center sm:justify-between"
    >
      <p className="text-sm text-muted-foreground">
        {t("pagination.total", { count })}
      </p>
      <div className="flex items-center justify-between gap-3 sm:justify-end">
        <p aria-live="polite" className="text-sm font-medium">
          {t("pagination.page", { current: currentPage, total: totalPages })}
        </p>
        <div className="flex gap-2">
          <Button
            disabled={page <= 0}
            onClick={() => onPageChange(Math.max(0, page - 1))}
            size="sm"
            type="button"
            variant="outline"
          >
            {t("pagination.previous")}
          </Button>
          <Button
            disabled={totalPages === 0 || page >= totalPages - 1}
            onClick={() => onPageChange(Math.min(totalPages - 1, page + 1))}
            size="sm"
            type="button"
            variant="outline"
          >
            {t("pagination.next")}
          </Button>
        </div>
      </div>
    </nav>
  );
}
