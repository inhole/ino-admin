import { RiRefreshLine, RiSearchLine } from "@remixicon/react";
import { useState, type FormEvent } from "react";
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
  defaultFileListFilters,
  type FileListFiltersValue,
} from "@/features/files/model/fileListFilters";

interface FileListFiltersProps {
  value: FileListFiltersValue;
  onApply: (value: FileListFiltersValue) => void;
}

export function FileListFilters({ value, onApply }: FileListFiltersProps) {
  const { t } = useTranslation("files");
  const { t: common } = useTranslation("common");
  const [draft, setDraft] = useState(value);
  const typeOptions = [
    { value: "all", label: t("filter.types.all") },
    { value: "application/pdf", label: t("filter.types.pdf") },
    { value: "image/png", label: t("filter.types.png") },
    { value: "image/jpeg", label: t("filter.types.jpeg") },
    { value: "text/plain", label: t("filter.types.text") },
  ];
  const orderOptions = [
    { value: "createdAt,desc", label: t("filter.orders.newest") },
    { value: "createdAt,asc", label: t("filter.orders.oldest") },
    { value: "originalName,asc", label: t("filter.orders.nameAsc") },
    { value: "originalName,desc", label: t("filter.orders.nameDesc") },
    { value: "size,desc", label: t("filter.orders.sizeDesc") },
    { value: "size,asc", label: t("filter.orders.sizeAsc") },
  ];

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    onApply(draft);
  };

  const reset = () => {
    setDraft(defaultFileListFilters);
    onApply(defaultFileListFilters);
  };

  return (
    <form className="flex flex-col gap-4" onSubmit={submit}>
      <FieldGroup className="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
        <Field>
          <FieldLabel htmlFor="file-name-filter">
            {t("filter.name")}
          </FieldLabel>
          <Input
            id="file-name-filter"
            maxLength={255}
            onChange={(event) =>
              setDraft((current) => ({
                ...current,
                name: event.target.value,
              }))
            }
            placeholder={t("filter.namePlaceholder")}
            value={draft.name}
          />
        </Field>
        <Field>
          <FieldLabel htmlFor="file-type-filter">
            {t("filter.type")}
          </FieldLabel>
          <Select
            items={typeOptions}
            onValueChange={(contentType) =>
              setDraft((current) => ({
                ...current,
                contentType: contentType ?? "all",
              }))
            }
            value={draft.contentType}
          >
            <SelectTrigger className="w-full" id="file-type-filter">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectGroup>
                {typeOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectGroup>
            </SelectContent>
          </Select>
        </Field>
        <Field>
          <FieldLabel htmlFor="file-created-from">
            {t("filter.createdFrom")}
          </FieldLabel>
          <Input
            id="file-created-from"
            max={draft.createdTo || undefined}
            onChange={(event) =>
              setDraft((current) => ({
                ...current,
                createdFrom: event.target.value,
              }))
            }
            type="date"
            value={draft.createdFrom}
          />
        </Field>
        <Field>
          <FieldLabel htmlFor="file-created-to">
            {t("filter.createdTo")}
          </FieldLabel>
          <Input
            id="file-created-to"
            min={draft.createdFrom || undefined}
            onChange={(event) =>
              setDraft((current) => ({
                ...current,
                createdTo: event.target.value,
              }))
            }
            type="date"
            value={draft.createdTo}
          />
        </Field>
        <Field>
          <FieldLabel htmlFor="file-order-filter">
            {t("filter.order")}
          </FieldLabel>
          <Select
            items={orderOptions}
            onValueChange={(order) =>
              setDraft((current) => ({
                ...current,
                order: order ?? defaultFileListFilters.order,
              }))
            }
            value={draft.order}
          >
            <SelectTrigger className="w-full" id="file-order-filter">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectGroup>
                {orderOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectGroup>
            </SelectContent>
          </Select>
        </Field>
      </FieldGroup>
      <div className="flex flex-wrap justify-end gap-2">
        <Button onClick={reset} type="button" variant="outline">
          <RiRefreshLine data-icon="inline-start" />
          {common("reset")}
        </Button>
        <Button type="submit">
          <RiSearchLine data-icon="inline-start" />
          {common("search")}
        </Button>
      </div>
    </form>
  );
}
