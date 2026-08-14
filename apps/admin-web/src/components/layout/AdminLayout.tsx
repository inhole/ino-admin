import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  ChevronLeft,
  ChevronRight,
  File,
  KeyRound,
  LayoutDashboard,
  LogOut,
  Menu,
  PanelLeft,
  Users,
} from "lucide-react";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import { getMyMenus } from "@/api/client";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { useAuth } from "@/features/auth/model/useAuth";
import { ThemeSelector } from "@/features/settings/ThemeSelector";
import { cn } from "@/lib/utils";

const iconMap = {
  users: Users,
  "key-round": KeyRound,
  menu: Menu,
  file: File,
  "layout-dashboard": LayoutDashboard,
};

export function AdminLayout() {
  const { t } = useTranslation("common");
  const { logout, user } = useAuth();
  const queryClient = useQueryClient();
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const menus = useQuery({
    queryKey: ["menus", "me"],
    queryFn: getMyMenus,
    enabled: Boolean(user),
  });
  useEffect(() => {
    setMobileOpen(false);
  }, [location.pathname]);
  const signOut = async () => {
    queryClient.clear();
    await logout().catch(() => undefined);
  };
  const sidebar = (mobile = false) => (
    <div className="flex h-full flex-col">
      <div
        className={cn(
          "flex h-16 items-center border-b border-sidebar-border px-4",
          collapsed && !mobile ? "justify-center" : "justify-between",
        )}
      >
        {(!collapsed || mobile) && (
          <span className="font-bold tracking-tight text-sidebar-foreground">
            {t("appName")}
          </span>
        )}
      </div>
      <nav
        aria-busy={menus.isPending}
        aria-label={t("navigation")}
        className="grid gap-1 p-3"
      >
        {menus.data?.map((menu) => {
          const Icon =
            iconMap[menu.icon as keyof typeof iconMap] ?? LayoutDashboard;
          return (
            <NavLink
              aria-label={collapsed && !mobile ? menu.label : undefined}
              className={({ isActive }) =>
                cn(
                  "flex min-h-11 items-center gap-3 rounded-xl px-3 text-sm font-medium text-sidebar-foreground/75 transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
                  isActive &&
                    "bg-sidebar-primary text-sidebar-primary-foreground",
                  collapsed && !mobile && "justify-center px-0",
                )
              }
              end={menu.route === "/"}
              key={menu.id}
              title={collapsed && !mobile ? menu.label : undefined}
              to={menu.route}
            >
              <Icon aria-hidden="true" />
              {(!collapsed || mobile) && (
                <span className="truncate">{menu.label}</span>
              )}
            </NavLink>
          );
        })}
        {menus.isError && (!collapsed || mobile) && (
          <p className="px-3 py-2 text-sm text-red-300">
            메뉴를 불러올 수 없습니다.
          </p>
        )}
      </nav>
      <div className="mt-auto grid gap-4 p-3 safe-bottom">
        {(!collapsed || mobile) && (
          <div className="rounded-xl bg-sidebar-accent p-3 text-sidebar-accent-foreground">
            <ThemeSelector />
          </div>
        )}
        <Separator className="bg-sidebar-border" />
        <div
          className={cn(
            "flex items-center gap-3",
            collapsed && !mobile && "justify-center",
          )}
        >
          <Avatar>
            <AvatarFallback>{user?.displayName?.slice(0, 1)}</AvatarFallback>
          </Avatar>
          {(!collapsed || mobile) && (
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-sidebar-foreground">
                {user?.displayName}
              </p>
              <p className="truncate text-xs text-sidebar-foreground/60">
                {user?.email}
              </p>
            </div>
          )}
        </div>
        <Button
          aria-label={t("logout")}
          className={cn(
            "min-h-11 justify-start border-sidebar-border bg-transparent text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
            collapsed && !mobile && "justify-center px-0",
          )}
          onClick={signOut}
          title={collapsed && !mobile ? t("logout") : undefined}
          type="button"
          variant="outline"
        >
          <LogOut aria-hidden="true" data-icon="inline-start" />
          {(!collapsed || mobile) && t("logout")}
        </Button>
      </div>
    </div>
  );
  return (
    <div
      className={cn(
        "min-h-svh md:grid",
        collapsed ? "md:grid-cols-[76px_1fr]" : "md:grid-cols-[260px_1fr]",
      )}
    >
      <a className="skip-link" href="#main-content">
        {t("skipToContent")}
      </a>
      <aside className="sticky top-0 hidden h-svh bg-sidebar transition-[width] md:block">
        {sidebar()}
        <Button
          aria-label={collapsed ? "사이드바 펼치기" : "사이드바 접기"}
          className="absolute -right-3 top-20 size-7 rounded-full shadow-sm"
          onClick={() => setCollapsed((value) => !value)}
          size="icon"
          variant="outline"
        >
          {collapsed ? <ChevronRight /> : <ChevronLeft />}
        </Button>
      </aside>
      <div className="min-w-0">
        <header className="safe-top sticky top-0 z-30 flex min-h-16 items-center justify-between border-b bg-background/90 px-4 backdrop-blur md:hidden">
          <Button
            aria-label={t("openMenu")}
            onClick={() => setMobileOpen(true)}
            size="icon"
            variant="ghost"
          >
            <PanelLeft />
          </Button>
          <span className="font-bold">{t("appName")}</span>
          <span aria-hidden="true" className="size-9" />
        </header>
        <main
          className="mx-auto w-full max-w-[1440px] px-4 py-6 sm:px-6 md:px-8 md:py-10"
          id="main-content"
          tabIndex={-1}
        >
          <Outlet />
        </main>
      </div>
      <Sheet onOpenChange={setMobileOpen} open={mobileOpen}>
        <SheetContent
          className="w-[min(86vw,320px)] border-sidebar-border bg-sidebar p-0"
          showCloseButton={false}
          side="left"
        >
          <SheetHeader className="sr-only">
            <SheetTitle>{t("navigation")}</SheetTitle>
            <SheetDescription>{t("appName")}</SheetDescription>
          </SheetHeader>
          <aside className="h-full">{sidebar(true)}</aside>
        </SheetContent>
      </Sheet>
    </div>
  );
}
