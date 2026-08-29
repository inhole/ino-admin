import { useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { RiRefreshLine, RiSearchLine } from "@remixicon/react";
import { Button } from "@/components/ui/button";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { emptyAuditFilters, type AuditFilters } from "@/features/audit/model/auditFilters";

export function AuditLogFilters({ value, onApply }: { value: AuditFilters; onApply: (value: AuditFilters) => void }) {
  const { t } = useTranslation("audit");
  const { t: common } = useTranslation("common");
  const [draft, setDraft] = useState(value);
  const submit = (event: FormEvent) => { event.preventDefault(); onApply(draft); };
  const reset = () => { setDraft(emptyAuditFilters); onApply(emptyAuditFilters); };

  return <form className="flex flex-col gap-4" onSubmit={submit}>
    <FieldGroup className="grid gap-3 md:grid-cols-2">
      <Field><FieldLabel htmlFor="audit-from">{t("createdFrom")}</FieldLabel><Input id="audit-from" type="date" value={draft.createdFrom} onChange={(event) => setDraft({ ...draft, createdFrom: event.target.value })} /></Field>
      <Field><FieldLabel htmlFor="audit-to">{t("createdTo")}</FieldLabel><Input id="audit-to" type="date" value={draft.createdTo} onChange={(event) => setDraft({ ...draft, createdTo: event.target.value })} /></Field>
    </FieldGroup>
    <div className="flex justify-end gap-2"><Button type="button" variant="outline" onClick={reset}><RiRefreshLine data-icon="inline-start" />{common("reset")}</Button><Button type="submit"><RiSearchLine data-icon="inline-start" />{common("search")}</Button></div>
  </form>;
}
