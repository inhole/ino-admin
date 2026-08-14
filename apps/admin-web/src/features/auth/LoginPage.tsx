import { Eye, EyeOff, ShieldCheck } from "lucide-react";
import { useRef, useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { ApiClientError } from "@/api/client";
import { FormField } from "@/components/layout/Page";
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
import {
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
  InputGroupInput,
} from "@/components/ui/input-group";
import { Spinner } from "@/components/ui/spinner";
import { useAuth } from "@/features/auth/hook/useAuth";
import { ThemeSelector } from "@/features/settings";

export function LoginPage() {
  const { t } = useTranslation("auth");
  const { t: common } = useTranslation("common");
  const { isRestoring, login, user } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);
  const errorRef = useRef<HTMLDivElement>(null);
  if (!isRestoring && user) return <Navigate replace to="/" />;
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    const data = new FormData(event.currentTarget);
    try {
      await login(String(data.get("email")), String(data.get("password")));
      navigate(
        typeof location.state?.from === "string" ? location.state.from : "/",
        { replace: true },
      );
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : t("connectionError"),
      );
      requestAnimationFrame(() => errorRef.current?.focus());
    } finally {
      setIsSubmitting(false);
    }
  };
  return (
    <main className="relative grid min-h-svh place-items-center overflow-hidden bg-background p-4 sm:p-6">
      <div
        aria-hidden="true"
        className="absolute inset-0 bg-[radial-gradient(circle_at_15%_10%,color-mix(in_oklch,var(--primary)_16%,transparent),transparent_34%),radial-gradient(circle_at_90%_90%,color-mix(in_oklch,var(--accent)_55%,transparent),transparent_32%)]"
      />
      <div className="absolute right-4 top-4 z-10 w-56 rounded-xl border bg-card/90 p-2 shadow-sm backdrop-blur sm:right-6 sm:top-6">
        <ThemeSelector />
      </div>
      <Card className="relative w-full max-w-md border-border/80 shadow-2xl shadow-primary/10">
        <CardHeader className="gap-3">
          <div className="grid size-11 place-items-center rounded-xl bg-primary text-primary-foreground">
            <ShieldCheck />
          </div>
          <p className="text-xs font-bold tracking-[0.18em] text-primary">
            {t("eyebrow")}
          </p>
          <CardTitle className="text-3xl">
            <h1 id="login-title">{t("title")}</h1>
          </CardTitle>
          <CardDescription>{t("description")}</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={submit}>
            <FieldGroup>
            <FormField htmlFor="email" label={t("email")}>
              <Input
                autoCapitalize="none"
                autoComplete="username"
                className="min-h-11"
                id="email"
                inputMode="email"
                name="email"
                required
                type="email"
              />
            </FormField>
            <FormField htmlFor="password" label={t("password")}>
              <InputGroup>
                <InputGroupInput
                  autoComplete="current-password"
                  id="password"
                  minLength={12}
                  name="password"
                  required
                  type={showPassword ? "text" : "password"}
                />
                <InputGroupAddon align="inline-end">
                <InputGroupButton
                  aria-label={
                    showPassword
                      ? common("hidePassword")
                      : common("showPassword")
                  }
                  onClick={() => setShowPassword((value) => !value)}
                  size="icon-sm"
                  type="button"
                >
                  {showPassword ? (
                    <EyeOff data-icon="inline-start" />
                  ) : (
                    <Eye data-icon="inline-start" />
                  )}
                </InputGroupButton>
                </InputGroupAddon>
              </InputGroup>
            </FormField>
            {error && (
              <Alert
                ref={errorRef}
                tabIndex={-1}
                variant="destructive"
                role="alert"
              >
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            <Button
              className="min-h-11 w-full"
              disabled={isSubmitting}
              type="submit"
            >
              {isSubmitting && <Spinner data-icon="inline-start" />}
              {isSubmitting ? t("submitting") : t("submit")}
            </Button>
            </FieldGroup>
          </form>
        </CardContent>
      </Card>
    </main>
  );
}
