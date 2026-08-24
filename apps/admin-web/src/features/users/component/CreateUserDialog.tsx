import { RiAddLine } from "@remixicon/react";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { ApiClientError } from "@/api/client";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button, buttonVariants } from "@/components/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Spinner } from "@/components/ui/spinner";
import { toast } from "@/components/ui/toast";
import { createUser } from "@/features/users/api/usersApi";
import { userKeys } from "@/features/users/hook/userKeys";

interface CreateUserDialogProps {
  roles: Array<{ value: string; label: string }>;
}

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const emailErrorId = "create-user-email-error";
const passwordErrorId = "create-user-password-error";

export function CreateUserDialog({ roles }: CreateUserDialogProps) {
  const { t } = useTranslation("users");
  const queryClient = useQueryClient();
  const formRef = useRef<HTMLFormElement>(null);
  const submissionInFlightRef = useRef(false);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [invalidFields, setInvalidFields] = useState({
    email: false,
    password: false,
  });
  const create = useMutation({
    mutationFn: createUser,
    onSuccess: async (created) => {
      await queryClient.invalidateQueries({ queryKey: userKeys.all });
      setOpen(false);
      formRef.current?.reset();
      toast.add({ title: t("created", { name: created.displayName }) });
    },
  });

  useEffect(() => {
    if (!open) return;

    const focusTimer = window.setTimeout(() => {
      document.getElementById("create-user-name")?.focus();
    });

    return () => window.clearTimeout(focusTimer);
  }, [open]);

  const setInvalid = (field: "email" | "password", valid: boolean) => {
    setInvalidFields((current) => ({ ...current, [field]: !valid }));
  };

  const handleOpenChange = (nextOpen: boolean) => {
    if (!nextOpen && create.isPending) return;
    setOpen(nextOpen);
    if (nextOpen) {
      setError(null);
      setInvalidFields({ email: false, password: false });
    }
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (submissionInFlightRef.current || create.isPending) return;

    setError(null);
    const data = new FormData(event.currentTarget);
    const email = String(data.get("email"));
    const password = String(data.get("password"));
    const emailValid = emailPattern.test(email);
    const passwordValid = password.length >= 12;
    setInvalidFields({ email: !emailValid, password: !passwordValid });

    if (!emailValid || !passwordValid) {
      document
        .getElementById(emailValid ? "create-user-password" : "create-user-email")
        ?.focus();
      return;
    }

    submissionInFlightRef.current = true;
    try {
      await create.mutateAsync({
        displayName: String(data.get("displayName")),
        email,
        password,
        role: String(data.get("role")),
      });
    } catch (submitError) {
      setError(
        submitError instanceof ApiClientError
          ? submitError.message
          : t("creationError"),
      );
    } finally {
      submissionInFlightRef.current = false;
    }
  };

  return (
    <Dialog onOpenChange={handleOpenChange} open={open}>
      <DialogTrigger render={<button className={buttonVariants()} type="button" />}>
        <RiAddLine data-icon="inline-start" />
        {t("addUser")}
      </DialogTrigger>
      <DialogContent
        initialFocus={() => document.getElementById("create-user-name")}
        showCloseButton={false}
      >
        <DialogHeader>
          <DialogTitle>{t("creationTitle")}</DialogTitle>
          <DialogDescription>{t("creationDescription")}</DialogDescription>
        </DialogHeader>
        <form aria-label={t("creationTitle")} onSubmit={submit} ref={formRef}>
          <FieldGroup>
            <Field>
              <FieldLabel htmlFor="create-user-name">{t("name")}</FieldLabel>
              <Input
                id="create-user-name"
                name="displayName"
                required
              />
            </Field>
            <Field data-invalid={invalidFields.email || undefined}>
              <FieldLabel htmlFor="create-user-email">{t("email")}</FieldLabel>
              <Input
                aria-describedby={invalidFields.email ? emailErrorId : undefined}
                aria-invalid={invalidFields.email || undefined}
                id="create-user-email"
                name="email"
                onChange={(event) => setInvalid("email", emailPattern.test(event.currentTarget.value))}
                onInvalid={() => setInvalid("email", false)}
                required
                type="email"
              />
              {invalidFields.email && (
                <FieldError id={emailErrorId}>{t("invalidEmail")}</FieldError>
              )}
            </Field>
            <Field data-invalid={invalidFields.password || undefined}>
              <FieldLabel htmlFor="create-user-password">
                {t("initialPassword")}
              </FieldLabel>
              <Input
                aria-describedby={invalidFields.password ? passwordErrorId : undefined}
                aria-invalid={invalidFields.password || undefined}
                id="create-user-password"
                minLength={12}
                name="password"
                onChange={(event) =>
                  setInvalid("password", event.currentTarget.value.length >= 12)
                }
                onInvalid={() => setInvalid("password", false)}
                required
                type="password"
              />
              {invalidFields.password && (
                <FieldError id={passwordErrorId}>{t("passwordPolicy")}</FieldError>
              )}
            </Field>
            <Field>
              <FieldLabel htmlFor="create-user-role">{t("role")}</FieldLabel>
              <Select
                defaultValue={roles.find((role) => role.value === "VIEWER")?.value ?? roles[0]?.value}
                items={roles}
                name="role"
              >
                <SelectTrigger className="w-full" id="create-user-role">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectGroup>
                    {roles.map((role) => (
                      <SelectItem key={role.value} value={role.value}>
                        {role.label}
                      </SelectItem>
                    ))}
                  </SelectGroup>
                </SelectContent>
              </Select>
            </Field>
            {error && (
              <Alert variant="destructive">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            <DialogFooter>
              <DialogClose
                disabled={create.isPending}
                render={
                  <button
                    className={buttonVariants({ variant: "outline" })}
                    type="button"
                  />
                }
              >
                {t("cancel")}
              </DialogClose>
              <Button disabled={create.isPending} type="submit">
                {create.isPending && <Spinner data-icon="inline-start" />}
                {create.isPending ? t("creating") : t("create")}
              </Button>
            </DialogFooter>
          </FieldGroup>
        </form>
      </DialogContent>
    </Dialog>
  );
}
