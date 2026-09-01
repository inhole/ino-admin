import { useQuery, useQueryClient } from "@tanstack/react-query";
import { File, History, KeyRound, LayoutDashboard, LogOut, Menu, Users } from "lucide-react";
import { RiArrowDownSLine, RiUserLine } from "@remixicon/react";
import { useTranslation } from "react-i18next";
import { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import { getMyMenus, menuKeys } from "@/features/menus";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { buttonVariants } from "@/components/ui/button";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Separator } from "@/components/ui/separator";
import {
  Sidebar, SidebarContent, SidebarHeader, SidebarInset,
  SidebarMenu, SidebarMenuButton, SidebarMenuItem, SidebarMenuSkeleton,
  SidebarMenuSub, SidebarMenuSubButton, SidebarMenuSubItem,
  SidebarProvider, SidebarRail, SidebarTrigger,
  useSidebar,
} from "@/components/ui/sidebar";
import { useAuth } from "@/features/auth/hook/useAuth";
import { LanguageMenu, ThemeMenu } from "@/features/settings";
import { cn } from "@/lib/utils";

const iconMap = {
  users: Users,
  "key-round": KeyRound,
  menu: Menu,
  file: File,
  history: History,
  "layout-dashboard": LayoutDashboard,
};

const menuLabelKeys = {
  dashboard: "navDashboard",
  "user-management": "navUserManagement",
  users: "navUsers",
  permissions: "navPermissions",
  "menu-management": "navMenuManagement",
  files: "navFiles",
  "access-history": "navAccessHistory",
} as const;

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
  const [openMenuIds, setOpenMenuIds] = useState<Set<string>>(new Set());
  const menus = useQuery({
    queryKey: menuKeys.current,
    queryFn: getMyMenus,
    enabled: Boolean(user),
  });
  const allMenus = menus.data?.flatMap((menu) => [menu, ...menu.children]) ?? [];
  const currentMenu = allMenus.find((menu) =>
    menu.route === "/"
      ? location.pathname === "/"
      : location.pathname.startsWith(menu.route),
  );
  const menuLabel = (menu: { id: string; label: string }) => {
    const key = menuLabelKeys[menu.id as keyof typeof menuLabelKeys];
    return key ? t(key) : menu.label;
  };

  useEffect(() => {
    const activeParent = menus.data?.find((menu) =>
      menu.children.some((child) => location.pathname.startsWith(child.route)),
    );
    if (!activeParent) return;
    setOpenMenuIds((current) => {
      if (current.has(activeParent.id)) return current;
      return new Set(current).add(activeParent.id);
    });
  }, [location.pathname, menus.data]);

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
              const label = menuLabel(menu);
              const active = menu.route === "/"
                ? location.pathname === "/"
                : location.pathname.startsWith(menu.route)
                  || menu.children.some((child) => location.pathname.startsWith(child.route));
              const hasChildren = menu.children.length > 0;
              const isOpen = openMenuIds.has(menu.id);
              return (
                <SidebarMenuItem key={menu.id}>
                  <SidebarMenuButton
                    aria-controls={hasChildren ? `${menu.id}-submenu` : undefined}
                    aria-expanded={hasChildren ? isOpen : undefined}
                    isActive={active}
                    onClick={hasChildren ? () => setOpenMenuIds((current) => {
                      const next = new Set(current);
                      if (next.has(menu.id)) next.delete(menu.id);
                      else next.add(menu.id);
                      return next;
                    }) : undefined}
                    render={hasChildren ? undefined : <NavLink to={menu.route} />}
                    tooltip={label}
                  >
                    <Icon aria-hidden="true" />
                    <span>{label}</span>
                    {hasChildren && (
                      <RiArrowDownSLine
                        aria-hidden="true"
                        className={cn("ml-auto transition-transform", isOpen && "rotate-180")}
                      />
                    )}
                  </SidebarMenuButton>
                  {hasChildren && isOpen && (
                    <SidebarMenuSub id={`${menu.id}-submenu`}>
                      {menu.children.map((child) => (
                        <SidebarMenuSubItem key={child.id}>
                          <SidebarMenuSubButton
                            isActive={location.pathname.startsWith(child.route)}
                            render={<NavLink to={child.route} />}
                          >
                            <span>{menuLabel(child)}</span>
                          </SidebarMenuSubButton>
                        </SidebarMenuSubItem>
                      ))}
                    </SidebarMenuSub>
                  )}
                </SidebarMenuItem>
              );
            })}
          </SidebarMenu>
        </SidebarContent>
        <SidebarRail />
      </Sidebar>
      <SidebarInset>
        <header className="safe-top sticky top-0 z-20 flex min-h-16 items-center gap-3 border-b bg-background/90 px-4 backdrop-blur sm:px-6">
          <SidebarTrigger aria-label={t("openMenu")} />
          <Separator className="h-5" orientation="vertical" />
          <Breadcrumb className="min-w-0 flex-1">
            <BreadcrumbList className="flex-nowrap">
              <BreadcrumbItem className="hidden sm:inline-flex">
                {location.pathname === "/" ? (
                  <BreadcrumbPage>{t("home")}</BreadcrumbPage>
                ) : (
                  <BreadcrumbLink render={<NavLink to="/" />}>
                    {t("home")}
                  </BreadcrumbLink>
                )}
              </BreadcrumbItem>
              {location.pathname !== "/" && (
                <>
                  <BreadcrumbSeparator className="hidden sm:block" />
                  <BreadcrumbItem className="min-w-0">
                    <BreadcrumbPage className="truncate font-semibold">
                      {currentMenu ? menuLabel(currentMenu) : t("appName")}
                    </BreadcrumbPage>
                  </BreadcrumbItem>
                </>
              )}
            </BreadcrumbList>
          </Breadcrumb>
          <LanguageMenu />
          <ThemeMenu />
          <DropdownMenu>
            <DropdownMenuTrigger
              aria-label={t("accountMenu")}
              className={buttonVariants({ className: "gap-2", variant: "ghost" })}
            >
              <Avatar className="size-8">
                <AvatarFallback>{user?.displayName?.slice(0, 1)}</AvatarFallback>
              </Avatar>
              <span className="hidden max-w-32 truncate text-sm font-semibold md:inline">
                {user?.displayName}
              </span>
              <RiArrowDownSLine aria-hidden="true" className="hidden md:block" />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuGroup>
                <DropdownMenuLabel>
                  <span className="block font-semibold text-foreground">{user?.displayName}</span>
                  <span className="block max-w-56 truncate">{user?.email}</span>
                </DropdownMenuLabel>
              </DropdownMenuGroup>
              <DropdownMenuSeparator />
              <DropdownMenuGroup>
                <DropdownMenuItem disabled>
                  <RiUserLine aria-hidden="true" />
                  {t("account")}
                </DropdownMenuItem>
              </DropdownMenuGroup>
              <DropdownMenuSeparator />
              <DropdownMenuGroup>
                <DropdownMenuItem onClick={signOut} variant="destructive">
                  <LogOut aria-hidden="true" />
                  {t("logout")}
                </DropdownMenuItem>
              </DropdownMenuGroup>
            </DropdownMenuContent>
          </DropdownMenu>
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
