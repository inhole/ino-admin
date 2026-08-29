import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, expect, test, vi } from "vitest";
import { MenuManagementPage } from "@/features/menus/MenuManagementPage";

const menuApi = vi.hoisted(() => ({
  createMenu: vi.fn(),
  getMenus: vi.fn(),
  updateMenu: vi.fn(),
}));

vi.mock("@/features/menus/api/menusApi", () => menuApi);

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  render(
    <QueryClientProvider client={queryClient}>
      <MenuManagementPage />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  Object.values(menuApi).forEach((mock) => mock.mockReset());
  menuApi.getMenus.mockResolvedValue([
    {
      id: "users",
      label: "사용자 관리",
      route: "/users",
      icon: "users",
      order: 10,
      parentId: null,
      requiredPermission: "user:read",
      enabled: true,
    },
    {
      id: "audit",
      label: "감사 로그",
      route: "/audit-logs",
      icon: "menu",
      order: 20,
      parentId: null,
      requiredPermission: "AUDIT:READ",
      enabled: true,
    },
    {
      id: "help",
      label: "도움말",
      route: "/help",
      icon: "menu",
      order: 30,
      parentId: null,
      requiredPermission: null,
      enabled: true,
    },
  ]);
});

afterEach(cleanup);

test("메뉴명, 경로, 필요 권한을 대소문자 구분 없이 검색한다", async () => {
  renderPage();
  const search = await screen.findByRole("searchbox", { name: "메뉴 검색" });

  fireEvent.change(search, { target: { value: "  audit:read  " } });

  expect(screen.getByText("감사 로그")).toBeInTheDocument();
  expect(screen.queryByText("사용자 관리")).not.toBeInTheDocument();
  expect(screen.queryByText("도움말")).not.toBeInTheDocument();

  fireEvent.change(search, { target: { value: "/USERS" } });

  expect(screen.getByText("사용자 관리")).toBeInTheDocument();
  expect(screen.queryByText("감사 로그")).not.toBeInTheDocument();
});

test("빈 검색어는 전체 메뉴를 표시하고 결과가 없으면 빈 상태를 표시한다", async () => {
  renderPage();
  const search = await screen.findByRole("searchbox", { name: "메뉴 검색" });

  fireEvent.change(search, { target: { value: "없는 메뉴" } });
  expect(screen.getByText("검색 조건에 맞는 메뉴가 없습니다.")).toBeInTheDocument();

  fireEvent.change(search, { target: { value: "   " } });
  expect(screen.getByText("사용자 관리")).toBeInTheDocument();
  expect(screen.getByText("감사 로그")).toBeInTheDocument();
  expect(screen.getByText("도움말")).toBeInTheDocument();
});
