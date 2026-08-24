import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { useEffect } from "react";
import {
  MemoryRouter,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from "react-router-dom";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { AuthContext } from "@/features/auth/provider/authContextValue";
import { UsersPage } from "@/features/users/UsersPage";

const user = {
  id: "user-2",
  email: "kim@example.com",
  displayName: "김관리",
  status: "ACTIVE",
  role: "ADMIN",
  createdAt: "2026-08-14T00:00:00Z",
};

const roles = [
  {
    role: "ADMIN",
    displayName: "관리자",
    systemRole: true,
    enabled: true,
    permissions: ["user:read", "user:update"],
  },
  {
    role: "VIEWER",
    displayName: "조회자",
    systemRole: true,
    enabled: true,
    permissions: ["user:read"],
  },
];

const currentUser = {
  id: "user-1",
  email: "super@example.com",
  displayName: "최고 관리자",
  status: "ACTIVE",
  role: "SUPER_ADMIN",
  permissions: ["user:read", "user:update"],
};

type Page = {
  content: typeof user[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

type UsersResponse = Page | Response;

function json(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function page(overrides: Partial<Page> = {}): Page {
  return {
    content: [user],
    page: 0,
    size: 20,
    totalElements: 41,
    totalPages: 3,
    ...overrides,
  };
}

function LocationProbe({ onChange }: { onChange?: (value: string) => void }) {
  const location = useLocation();
  const value = `${location.pathname}${location.search}`;

  useEffect(() => {
    onChange?.(value);
  }, [onChange, value]);

  return <output data-testid="location">{value}</output>;
}

function HistoryBackButton() {
  const navigate = useNavigate();
  return <button onClick={() => navigate(-1)}>테스트 뒤로</button>;
}

function renderPage(
  initialEntry: string | string[] = "/users",
  onLocationChange?: (value: string) => void,
) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  const initialEntries = Array.isArray(initialEntry)
    ? initialEntry
    : [initialEntry];

  render(
    <AuthContext.Provider
      value={{
        user: currentUser,
        isRestoring: false,
        login: vi.fn(),
        logout: vi.fn(),
      }}
      >
      <QueryClientProvider client={queryClient}>
        <MemoryRouter
          initialEntries={initialEntries}
          initialIndex={initialEntries.length - 1}
        >
          <HistoryBackButton />
          <LocationProbe onChange={onLocationChange} />
          <Routes>
            <Route path="/users" element={<UsersPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    </AuthContext.Provider>,
  );
}

function mockApi(
  usersResponse:
    | UsersResponse
    | ((url: URL) => UsersResponse | Promise<UsersResponse>) = page(),
) {
  return vi.spyOn(globalThis, "fetch").mockImplementation(async (input) => {
    const url = new URL(String(input), "http://localhost");
    if (url.pathname === "/api/v1/permissions") return json(roles);
    if (url.pathname === "/api/v1/users") {
      const response =
        typeof usersResponse === "function"
          ? await usersResponse(url)
          : usersResponse;
      return response instanceof Response ? response : json(response);
    }
    throw new Error(`Unexpected request: ${url.pathname}${url.search}`);
  });
}

beforeEach(() => {
  sessionStorage.clear();
});

afterEach(() => {
  cleanup();
  vi.useRealTimers();
  vi.restoreAllMocks();
});

describe("사용자 조회 URL", () => {
  test("URL 조건을 복원하고 같은 query parameter로 조회한다", async () => {
    const fetchMock = mockApi(
      page({ page: 1, content: [user], totalElements: 41, totalPages: 3 }),
    );

    renderPage("/users?query=kim&role=ADMIN&status=ACTIVE&page=1");

    expect(await screen.findByDisplayValue("kim")).toBeInTheDocument();
    expect(screen.getByRole("combobox", { name: "역할" })).toHaveTextContent(
      "ADMIN",
    );
    expect(screen.getByRole("combobox", { name: "상태" })).toHaveTextContent(
      "활성",
    );
    expect(await screen.findByText("2 / 3 페이지")).toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(([input]) =>
        String(input).endsWith(
          "/api/v1/users?query=kim&role=ADMIN&status=ACTIVE&page=1",
        ),
      ),
    ).toBe(true);
  });

  test("검색은 299ms에는 URL을 유지하고 정확히 300ms에 page 없이 반영한다", async () => {
    mockApi();
    renderPage("/users?page=2");
    const search = await screen.findByRole("textbox", {
      name: "사용자 검색",
    });
    vi.useFakeTimers();

    fireEvent.change(search, { target: { value: "kim" } });
    act(() => vi.advanceTimersByTime(299));
    expect(screen.getByTestId("location")).toHaveTextContent("/users?page=2");

    act(() => vi.advanceTimersByTime(1));
    expect(screen.getByTestId("location")).toHaveTextContent(
      "/users?query=kim",
    );
    expect(screen.getByTestId("location")).not.toHaveTextContent("page=");
  });

  test.each([
    ["역할", "조회자", "role=VIEWER"],
    ["상태", "비활성", "status=DISABLED"],
    ["정렬", "이메일", "sort=email"],
    ["정렬 방향", "오름차순", "direction=asc"],
  ])("%s 변경을 즉시 URL에 반영하고 page를 제거한다", async (label, option, expected) => {
    mockApi();
    renderPage("/users?query=kim&page=2");
    const select = await screen.findByRole("combobox", { name: label });

    fireEvent.click(select);
    const selectedOption = await screen.findByRole("option", { name: option });
    fireEvent.pointerDown(selectedOption, { pointerType: "mouse" });
    fireEvent.click(selectedOption);

    await waitFor(() =>
      expect(screen.getByTestId("location")).toHaveTextContent(expected),
    );
    expect(screen.getByTestId("location")).toHaveTextContent("query=kim");
    expect(screen.getByTestId("location")).not.toHaveTextContent("page=");
  });

  test("조회 조건 초기화는 URL에서 조건을 제거하고 기본값으로 돌아간다", async () => {
    mockApi();
    renderPage(
      "/users?query=kim&role=ADMIN&status=LOCKED&page=2&sort=email&direction=asc",
    );

    fireEvent.click(
      await screen.findByRole("button", { name: "조회 조건 초기화" }),
    );

    expect(screen.getByTestId("location")).toHaveTextContent("/users");
    expect(screen.getByTestId("location").textContent).toBe("/users");
    expect(screen.getByRole("textbox", { name: "사용자 검색" })).toHaveValue(
      "",
    );
  });

  test("검색 debounce 전에 초기화하면 draft와 예약된 URL 변경을 함께 취소한다", async () => {
    mockApi();
    renderPage();
    const search = await screen.findByRole("textbox", {
      name: "사용자 검색",
    });
    vi.useFakeTimers();

    fireEvent.change(search, { target: { value: "kim" } });
    fireEvent.click(
      screen.getByRole("button", { name: "조회 조건 초기화" }),
    );

    expect(search).toHaveValue("");
    expect(screen.getByTestId("location").textContent).toBe("/users");
    act(() => vi.advanceTimersByTime(300));
    expect(search).toHaveValue("");
    expect(screen.getByTestId("location").textContent).toBe("/users");
  });
});

describe("사용자 조회 결과", () => {
  test("이전·다음 페이지를 URL로 이동하고 경계 버튼을 비활성화한다", async () => {
    mockApi((url) => {
      const current = Number(url.searchParams.get("page") ?? "0");
      return page({ page: current, totalPages: 2, totalElements: 21 });
    });
    renderPage();

    const previous = await screen.findByRole("button", { name: "이전 페이지" });
    const next = screen.getByRole("button", { name: "다음 페이지" });
    expect(previous).toBeDisabled();
    expect(next).toBeEnabled();

    fireEvent.click(next);
    await waitFor(() =>
      expect(screen.getByTestId("location")).toHaveTextContent("page=1"),
    );
    expect(await screen.findByText("2 / 2 페이지")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "이전 페이지" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "다음 페이지" })).toBeDisabled();
  });

  test("백그라운드 재조회 중에는 기존 결과와 접근 가능한 진행 상태를 유지한다", async () => {
    let resolveFiltered!: (value: Page) => void;
    const filteredResponse = new Promise<Page>((resolve) => {
      resolveFiltered = resolve;
    });
    mockApi((url) =>
      url.searchParams.get("role") === "VIEWER"
        ? filteredResponse
        : page({ content: [user] }),
    );
    renderPage();
    expect(await screen.findAllByText("kim@example.com")).toHaveLength(2);

    const role = screen.getByRole("combobox", { name: "역할" });
    fireEvent.click(role);
    const option = await screen.findByRole("option", { name: "조회자" });
    fireEvent.pointerDown(option, { pointerType: "mouse" });
    fireEvent.click(option);

    expect(
      await screen.findByRole("status", {
        name: "사용자 목록을 갱신하는 중…",
      }),
    ).toBeInTheDocument();
    expect(screen.getAllByText("kim@example.com")).toHaveLength(2);

    await act(async () => {
      resolveFiltered(page({ content: [], totalElements: 0, totalPages: 0 }));
    });
  });

  test("빈 이전 결과를 유지하는 재조회 중에는 진행 상태만 표시한다", async () => {
    let resolveFiltered!: (value: Page) => void;
    const filteredResponse = new Promise<Page>((resolve) => {
      resolveFiltered = resolve;
    });
    mockApi((url) =>
      url.searchParams.get("role") === "VIEWER"
        ? filteredResponse
        : page({ content: [], totalElements: 0, totalPages: 0 }),
    );
    renderPage();
    expect(
      await screen.findByText("등록된 사용자가 없습니다."),
    ).toBeInTheDocument();

    const role = screen.getByRole("combobox", { name: "역할" });
    fireEvent.click(role);
    const option = await screen.findByRole("option", { name: "조회자" });
    fireEvent.pointerDown(option, { pointerType: "mouse" });
    fireEvent.click(option);

    expect(
      await screen.findByRole("status", {
        name: "사용자 목록을 갱신하는 중…",
      }),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("등록된 사용자가 없습니다."),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText("조건에 맞는 사용자가 없습니다."),
    ).not.toBeInTheDocument();

    await act(async () => {
      resolveFiltered(page({ content: [], totalElements: 0, totalPages: 0 }));
    });
    expect(
      await screen.findByText("조건에 맞는 사용자가 없습니다."),
    ).toBeInTheDocument();
  });

  test("전체 목록이 비었으면 기존 문구를 유지한다", async () => {
    mockApi(page({ content: [], totalElements: 0, totalPages: 0 }));
    renderPage();

    expect(
      await screen.findByText("등록된 사용자가 없습니다."),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("조건에 맞는 사용자가 없습니다."),
    ).not.toBeInTheDocument();
  });

  test("조건 결과가 비었으면 조건 전용 문구와 초기화 action을 제공한다", async () => {
    mockApi(page({ content: [], totalElements: 0, totalPages: 0 }));
    renderPage("/users?query=nobody&role=ADMIN");

    expect(
      await screen.findByText("조건에 맞는 사용자가 없습니다."),
    ).toBeInTheDocument();
    const emptyState = screen
      .getByText("조건에 맞는 사용자가 없습니다.")
      .closest('[data-slot="empty"]');
    expect(emptyState).not.toBeNull();
    fireEvent.click(
      within(emptyState as HTMLElement).getByRole("button", {
        name: "조회 조건 초기화",
      }),
    );
    expect(screen.getByTestId("location").textContent).toBe("/users");
  });

  test.each([
    [400, "조회 조건이 올바르지 않습니다."],
    [403, "사용자 목록을 볼 권한이 없습니다."],
  ])("HTTP %s를 공통 오류 상태로 표시한다", async (status, message) => {
    mockApi(json({ code: "ERROR", message: "server detail" }, status));
    renderPage();

    expect(await screen.findByRole("alert")).toHaveTextContent(message);
  });

  test("일반 오류는 서버 메시지와 trace ID를 표시하고 재시도한다", async () => {
    let attempts = 0;
    mockApi(() => {
      attempts += 1;
      return attempts === 1
        ? json(
            { code: "INTERNAL_ERROR", message: "잠시 후 다시 시도하세요.", traceId: "01KTRACE54" },
            500,
          )
        : page();
    });
    renderPage();

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("잠시 후 다시 시도하세요.");
    expect(alert).toHaveTextContent("문의 코드: 01KTRACE54");
    fireEvent.click(within(alert).getByRole("button", { name: "다시 시도" }));

    expect(await screen.findAllByText("kim@example.com")).toHaveLength(2);
    expect(attempts).toBe(2);
  });

  test("데스크톱 표와 모바일 요약에 같은 핵심 정보와 action을 제공한다", async () => {
    mockApi(page({ content: [user], totalElements: 1, totalPages: 1 }));
    renderPage();

    const desktop = await screen.findByRole("region", {
      name: "데스크톱 사용자 목록",
    });
    const mobile = screen.getByRole("region", { name: "모바일 사용자 목록" });

    for (const region of [desktop, mobile]) {
      expect(within(region).getByText("김관리")).toBeInTheDocument();
      expect(within(region).getByText("kim@example.com")).toBeInTheDocument();
      expect(within(region).getByText("ADMIN")).toBeInTheDocument();
      expect(within(region).getByText("ACTIVE")).toBeInTheDocument();
      expect(region).toHaveTextContent("2026년 8월 14일");
      expect(within(region).getByRole("button", { name: "수정" })).toBeInTheDocument();
      expect(within(region).getByRole("button", { name: "비활성화" })).toBeInTheDocument();
    }
  });

  test("범위 밖 빈 페이지는 마지막 페이지로 한 번만 replace한다", async () => {
    const locations: string[] = [];
    const fetchMock = mockApi((url) => {
      const current = Number(url.searchParams.get("page") ?? "0");
      return current === 9
        ? page({ content: [], page: 9, totalElements: 41, totalPages: 3 })
        : page({ content: [user], page: current, totalElements: 41, totalPages: 3 });
    });
    renderPage(["/previous", "/users?page=9"], (value) => locations.push(value));

    await waitFor(() =>
      expect(screen.getByTestId("location")).toHaveTextContent("page=2"),
    );
    expect(await screen.findByText("3 / 3 페이지")).toBeInTheDocument();
    expect(
      fetchMock.mock.calls.filter(([input]) =>
        String(input).includes("/api/v1/users"),
      ),
    ).toHaveLength(2);
    expect(locations.filter((value) => value === "/users?page=2")).toHaveLength(1);

    fireEvent.click(screen.getByRole("button", { name: "테스트 뒤로" }));
    await waitFor(() =>
      expect(screen.getByTestId("location").textContent).toBe("/previous"),
    );
  });
});
