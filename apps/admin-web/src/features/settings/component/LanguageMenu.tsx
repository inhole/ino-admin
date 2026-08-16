import { Languages } from "lucide-react";
import { useTranslation } from "react-i18next";
import { buttonVariants } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

const options = [
  { value: "ko", label: "languageKorean" },
  { value: "en", label: "languageEnglish" },
] as const;

export function LanguageMenu() {
  const { i18n, t } = useTranslation("common");
  const language = i18n.resolvedLanguage === "en" ? "en" : "ko";

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        aria-label={t("language")}
        className={buttonVariants({ size: "icon", variant: "ghost" })}
        title={t("language")}
      >
        <Languages aria-hidden="true" />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuRadioGroup
          onValueChange={(value) => void i18n.changeLanguage(value)}
          value={language}
        >
          <DropdownMenuLabel>{t("language")}</DropdownMenuLabel>
          {options.map(({ value, label }) => (
            <DropdownMenuRadioItem key={value} value={value}>
              {t(label)}
            </DropdownMenuRadioItem>
          ))}
        </DropdownMenuRadioGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
