import { Moon, Monitor, Sun } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useTheme } from "../hook/useTheme";
import type { Theme } from "../provider/themeContext";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { FieldLegend, FieldSet } from "@/components/ui/field";

const options: Array<{
  value: Theme;
  icon: typeof Sun;
  label: "themeLight" | "themeDark" | "themeSystem";
}> = [
  { value: "light", icon: Sun, label: "themeLight" },
  { value: "dark", icon: Moon, label: "themeDark" },
  { value: "system", icon: Monitor, label: "themeSystem" },
];
export function ThemeSelector() {
  const { t } = useTranslation("common");
  const { theme, setTheme } = useTheme();
  return (
    <FieldSet>
      <FieldLegend className="mb-2 text-xs font-semibold text-muted-foreground">
        {t("theme")}
      </FieldLegend>
      <ToggleGroup
        aria-label={t("theme")}
        className="grid-cols-3"
        onValueChange={(values) => {
          const next = values.at(-1) as Theme | undefined;
          if (next) setTheme(next);
        }}
        value={[theme]}
      >
        {options.map(({ value, icon: Icon, label }) => (
          <ToggleGroupItem aria-label={t(label)} key={value} value={value}>
            <Icon aria-hidden="true" />
            <span>{t(label)}</span>
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
    </FieldSet>
  );
}
