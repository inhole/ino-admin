import { RiSearchLine } from "@remixicon/react";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { NavLink } from "react-router-dom";
import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
} from "@/components/ui/input-group";
import type { MenuItem } from "@/features/menus/api/menusApi";
import { cn } from "@/lib/utils";

interface GlobalMenuSearchProps {
  className?: string;
  getMenuLabel?: (menu: MenuItem) => string;
  menus: MenuItem[];
}

export function GlobalMenuSearch({
  className,
  getMenuLabel = (menu) => menu.label,
  menus,
}: GlobalMenuSearchProps) {
  const { t } = useTranslation("common");
  const [query, setQuery] = useState("");
  const normalizedQuery = query.trim().toLocaleLowerCase();
  const results = useMemo(
    () =>
      normalizedQuery
        ? menus.filter((menu) =>
            getMenuLabel(menu).toLocaleLowerCase().includes(normalizedQuery),
          )
        : [],
    [getMenuLabel, menus, normalizedQuery],
  );
  const resultsId = "global-menu-search-results";

  return (
    <div className={cn("relative", className)}>
      <InputGroup>
        <InputGroupAddon>
          <RiSearchLine aria-hidden="true" />
        </InputGroupAddon>
        <InputGroupInput
          aria-controls={normalizedQuery ? resultsId : undefined}
          aria-expanded={Boolean(normalizedQuery)}
          aria-label={t("menuSearch")}
          onChange={(event) => setQuery(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Escape") setQuery("");
          }}
          placeholder={t("menuSearchPlaceholder")}
          type="search"
          value={query}
        />
      </InputGroup>
      {normalizedQuery && (
        <div
          aria-label={t("menuSearchResults")}
          className="absolute top-full mt-2 w-full min-w-56 rounded-2xl border bg-popover p-2 text-popover-foreground shadow-lg"
          id={resultsId}
          role="region"
        >
          {results.length > 0 ? (
            <ul className="flex flex-col gap-1">
              {results.map((menu) => (
                <li key={menu.id}>
                  <NavLink
                    className="block rounded-xl px-3 py-2 text-sm font-medium outline-none hover:bg-accent hover:text-accent-foreground focus-visible:bg-accent focus-visible:text-accent-foreground"
                    onClick={() => setQuery("")}
                    to={menu.route}
                  >
                    {getMenuLabel(menu)}
                  </NavLink>
                </li>
              ))}
            </ul>
          ) : (
            <p className="px-3 py-4 text-center text-sm text-muted-foreground">
              {t("menuSearchEmpty")}
            </p>
          )}
        </div>
      )}
    </div>
  );
}
