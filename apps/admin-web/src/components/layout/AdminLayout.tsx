import { useQuery, useQueryClient } from "@tanstack/react-query";
import { File, KeyRound, LayoutDashboard, LogOut, Menu, Users } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useEffect } from "react";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import { getMyMenus } from "@/api/client";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  Sidebar, SidebarContent, SidebarFooter, SidebarHeader, SidebarInset,
  SidebarMenu, SidebarMenuButton, SidebarMenuItem, SidebarMenuSkeleton,
  SidebarProvider, SidebarRail, SidebarTrigger,
  useSidebar,
} from "@/components/ui/sidebar";
import { useAuth } from "@/features/auth/model/useAuth";
import { ThemeSelector } from "@/features/settings/ThemeSelector";

const iconMap = {
  users: Users,
  "key-round": KeyRound,
  menu: Menu,
  file: File,
  "layout-dashboard": LayoutDashboard,
};

function CloseMobileSidebarOnNavigation() {
  const { pathname } = useLocation();
  const { setOpenMobile } = useSidebar();

  useEffect(() => setOpenMobile(false), [pathname, setOpenMobile]);
  return null;
}

export function AdminLayout() {
  const { t } = useTranslation("common");
  const { logout, user } = useAuth();
  const queryClient = useQueryClient();
  const location = useLocation();
  const menus = useQuery({
    queryKey: ["menus", "me"],
    queryFn: getMyMenus,
    enabled: Boolean(user),
  });

  const signOut = async () => {
    queryClient.clear();
    await logout().catch(() => undefined);
  };

  return (
    <SidebarProvider>
      <CloseMobileSidebarOnNavigation />
      <a className="skip-link" href="#main-content">{t("skipToContent")}</a>
      <Sidebar collapsible="icon">
        <SidebarHeader>
          <span className="h-10 truncate px-2 py-2 font-bold tracking-tight">{t("appName")}</span>
        </SidebarHeader>
        <SidebarContent>
          <SidebarMenu aria-busy={menus.isPending} aria-label={t("navigation")}>
            {menus.isPending && [1, 2, 3].map((row) => (
              <SidebarMenuSkeleton key={row} showIcon />
            ))}
            {menus.data?.map((menu) => {
              const Icon = iconMap[menu.icon as keyof typeof iconMap] ?? LayoutDashboard;
              const active = menu.route === "/"
                ? location.pathname === "/"
                : location.pathname.startsWith(menu.route);
              return (
                <SidebarMenuItem key={menu.id}>
                  <SidebarMenuButton
                    isActive={active}
                    render={<NavLink to={menu.route} />}
                    tooltip={menu.label}
                  >
                    <Icon aria-hidden="true" />
                    <span>{menu.label}</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              );
            })}
          </SidebarMenu>
        </SidebarContent>
        <SidebarFooter>
          <div className="group-data-[collapsible=icon]:hidden"><ThemeSelector /></div>
          <Separator />
          <SidebarMenu>
            <SidebarMenuItem>
              <SidebarMenuButton size="lg" tooltip={user?.displayName}>
                <Avatar><AvatarFallback>{user?.displayName?.slice(0, 1)}</AvatarFallback></Avatar>
                <span className="min-w-0">
                  <span className="block truncate font-semibold">{user?.displayName}</span>
                  <span className="block truncate text-xs text-muted-foreground">{user?.email}</span>
                </span>
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
          <Button onClick={signOut} type="button" variant="outline">
            <LogOut aria-hidden="true" data-icon="inline-start" />
            <span className="group-data-[collapsible=icon]:hidden">{t("logout")}</span>
          </Button>
        </SidebarFooter>
        <SidebarRail />
      </Sidebar>
      <SidebarInset>
        <header className="safe-top sticky top-0 flex min-h-16 items-center gap-3 border-b bg-background/90 px-4 backdrop-blur md:hidden">
          <SidebarTrigger aria-label={t("openMenu")} />
          <span className="font-bold">{t("appName")}</span>
        </header>
        <div
          className="mx-auto w-full max-w-[1440px] px-4 py-6 sm:px-6 md:px-8 md:py-10"
          id="main-content"
          tabIndex={-1}
        >
          <Outlet />
        </div>
      </SidebarInset>
    </SidebarProvider>
  );
}
