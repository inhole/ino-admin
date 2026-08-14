import { RiComputerLine, RiMoonLine, RiSunLine } from "@remixicon/react";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useTheme } from "../hook/useTheme";
import type { Theme } from "../provider/themeContext";

const options = [
  { value: "light", icon: RiSunLine, label: "themeLight" },
  { value: "dark", icon: RiMoonLine, label: "themeDark" },
  { value: "system", icon: RiComputerLine, label: "themeSystem" },
] as const;

export function ThemeMenu() {
  const { t } = useTranslation("common");
  const { resolvedTheme, setTheme, theme } = useTheme();
  const CurrentIcon = resolvedTheme === "dark" ? RiMoonLine : RiSunLine;

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        render={
          <Button
            aria-label={t("theme")}
            size="icon"
            title={t("theme")}
            variant="ghost"
          />
        }
      >
        <CurrentIcon aria-hidden="true" />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuRadioGroup
          onValueChange={(value) => setTheme(value as Theme)}
          value={theme}
        >
          <DropdownMenuLabel>{t("theme")}</DropdownMenuLabel>
          {options.map(({ value, icon: Icon, label }) => (
            <DropdownMenuRadioItem key={value} value={value}>
              <Icon aria-hidden="true" />
              {t(label)}
            </DropdownMenuRadioItem>
          ))}
        </DropdownMenuRadioGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
