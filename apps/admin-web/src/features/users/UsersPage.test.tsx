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
  },
  {
    role: "VIEWER",
    displayName: "조회자",
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

const adminUser = {
  ...currentUser,
  role: "ADMIN",
  permissions: ["user:read"],
};

const userCreator = {
  ...currentUser,
  permissions: ["user:read", "user:create"],
};

const userExporter = {
  ...currentUser,
  permissions: ["user:read", "excel:export"],
};

type Page = {
  content: typeof user[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

type UsersResponse = Page | Response;
type RoleCatalogResponse =
  | typeof roles
  | Response
  | Promise<typeof roles | Response>;

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
  authUser = currentUser,
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
        user: authUser,
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
  rolesResponse: RoleCatalogResponse = roles,
) {
  return vi.spyOn(globalThis, "fetch").mockImplementation(async (input) => {
    const url = new URL(String(input), "http://localhost");
    if (url.pathname === "/api/v1/users/roles") {
      const response = await rolesResponse;
      return response instanceof Response ? response : json(response);
    }
    if (url.pathname === "/api/v1/permissions") {
      return json({ code: "FORBIDDEN", message: "Forbidden" }, 403);
    }
    if (url.pathname === "/api/v1/excel/users/export") {
      return new Response(new Blob(["xlsx"]), {
        headers: { "Content-Type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" },
      });
    }
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

describe("사용자 생성 진입점", () => {
  test("user:create 권한이 있으면 사용자 추가 모달 트리거를 표시한다", async () => {
    mockApi();
    renderPage("/users", undefined, userCreator);

    expect(
      await screen.findByRole("button", { name: "사용자 추가" }),
    ).toBeInTheDocument();
  });

  test("user:create 권한이 있으면 기존 인라인 생성 폼을 표시하지 않는다", async () => {
    mockApi();
    renderPage("/users", undefined, userCreator);

    await screen.findByText("사용자 목록");
    expect(screen.queryByLabelText("초기 비밀번호")).not.toBeInTheDocument();
  });

  test("user:create 권한이 없으면 사용자 추가 모달 트리거를 표시하지 않는다", async () => {
    mockApi();
    renderPage("/users", undefined, adminUser);

    await screen.findByText("사용자 목록");
    expect(
      screen.queryByRole("button", { name: "사용자 추가" }),
    ).not.toBeInTheDocument();
  });

  test("역할 catalog를 불러오는 동안 생성 가능한 역할의 진행 상태를 표시한다", async () => {
    const pendingRoles = new Promise<typeof roles>(() => undefined);
    const fetchMock = mockApi(page(), pendingRoles);
    renderPage("/users", undefined, userCreator);

    await waitFor(() =>
      expect(
        fetchMock.mock.calls.some(([input]) =>
          String(input).endsWith("/api/v1/users/roles"),
        ),
      ).toBe(true),
    );
    expect(
      screen.getByRole("status", { name: "생성 가능한 역할을 불러오는 중…" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "사용자 추가" }),
    ).not.toBeInTheDocument();
  });

  test("생성 가능한 역할이 없으면 이유를 설명한다", async () => {
    let resolveRoles!: (value: typeof roles) => void;
    const pendingRoles = new Promise<typeof roles>((resolve) => {
      resolveRoles = resolve;
    });
    const fetchMock = mockApi(page(), pendingRoles);
    renderPage("/users", undefined, userCreator);

    await waitFor(() =>
      expect(
        fetchMock.mock.calls.some(([input]) =>
          String(input).endsWith("/api/v1/users/roles"),
        ),
      ).toBe(true),
    );
    await act(async () => {
      resolveRoles([]);
    });

    expect(
      await screen.findByText("생성 가능한 역할이 없습니다."),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "사용자 추가" }),
    ).not.toBeInTheDocument();
  });

  test("역할 catalog 오류는 설명과 재시도 control을 제공한다", async () => {
    let resolveRoles!: (value: Response) => void;
    const pendingRoles = new Promise<Response>((resolve) => {
      resolveRoles = resolve;
    });
    const fetchMock = mockApi(page(), pendingRoles);
    renderPage("/users", undefined, userCreator);

    await waitFor(() =>
      expect(
        fetchMock.mock.calls.some(([input]) =>
          String(input).endsWith("/api/v1/users/roles"),
        ),
      ).toBe(true),
    );
    await act(async () => {
      resolveRoles(json({ code: "INTERNAL_ERROR", message: "server detail" }, 500));
    });

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("생성 가능한 역할을 불러올 수 없습니다.");
    fireEvent.click(within(alert).getByRole("button", { name: "다시 시도" }));
    await waitFor(() =>
      expect(
        fetchMock.mock.calls.filter(([input]) =>
          String(input).endsWith("/api/v1/users/roles"),
        ),
      ).toHaveLength(2),
    );
  });

  test("user:create 권한이 없으면 생성 역할 상태나 재시도 control을 노출하지 않는다", async () => {
    const pendingRoles = new Promise<typeof roles>(() => undefined);
    mockApi(page(), pendingRoles);
    renderPage("/users", undefined, adminUser);

    await screen.findByText("사용자 목록");
    expect(
      screen.queryByRole("status", { name: "생성 가능한 역할을 불러오는 중…" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText("생성 가능한 역할이 없습니다."),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "다시 시도" }),
    ).not.toBeInTheDocument();
  });
});

describe("사용자 Excel 내보내기", () => {
  test("excel:export 권한이 있으면 사용자 목록을 다운로드한다", async () => {
    const fetchMock = mockApi();
    const createObjectURL = vi.spyOn(URL, "createObjectURL").mockReturnValue("blob:users");
    const revokeObjectURL = vi.spyOn(URL, "revokeObjectURL").mockImplementation(() => undefined);
    const click = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => undefined);
    renderPage("/users", undefined, userExporter);

    fireEvent.click(await screen.findByRole("button", { name: "Excel 내보내기" }));

    await waitFor(() => expect(createObjectURL).toHaveBeenCalled());
    expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/api/v1/excel/users/export"))).toBe(true);
    expect(click).toHaveBeenCalled();
    expect(revokeObjectURL).toHaveBeenCalledWith("blob:users");
  });

  test("excel:export 권한이 없으면 내보내기 버튼을 표시하지 않는다", async () => {
    mockApi();
    renderPage();

    await screen.findByText("사용자 목록");
    expect(screen.queryByRole("button", { name: "Excel 내보내기" })).not.toBeInTheDocument();
  });
});

describe("사용자 조회 URL", () => {
  test("잘못된 값과 기본값이 포함된 최초 URL을 canonical query로 replace한다", async () => {
    mockApi();
    renderPage([
      "/previous",
      "/users?status=UNKNOWN&page=0&size=20&sort=createdAt&direction=desc&query=%20kim%20&unexpected=x",
    ]);

    await waitFor(() =>
      expect(screen.getByTestId("location").textContent).toBe(
        "/users?query=kim",
      ),
    );

    fireEvent.click(screen.getByRole("button", { name: "테스트 뒤로" }));
    await waitFor(() =>
      expect(screen.getByTestId("location").textContent).toBe("/previous"),
    );
  });

  test("permission:read가 없는 ADMIN도 안전한 역할 catalog로 필터를 사용한다", async () => {
    const fetchMock = mockApi();
    renderPage("/users", undefined, adminUser);

    const role = await screen.findByRole("combobox", { name: "역할" });
    fireEvent.click(role);
    const option = await screen.findByRole("option", { name: "조회자" });
    fireEvent.pointerDown(option, { pointerType: "mouse" });
    fireEvent.click(option);

    await waitFor(() =>
      expect(screen.getByTestId("location")).toHaveTextContent("role=VIEWER"),
    );
    expect(
      fetchMock.mock.calls.some(([input]) =>
        String(input).endsWith("/api/v1/users/roles"),
      ),
    ).toBe(true);
    expect(
      fetchMock.mock.calls.some(([input]) =>
        String(input).endsWith("/api/v1/permissions"),
      ),
    ).toBe(false);
  });

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
        new URL(String(input), "http://localhost").pathname === "/api/v1/users",
      ),
    ).toHaveLength(2);
    expect(locations.filter((value) => value === "/users?page=2")).toHaveLength(1);

    fireEvent.click(screen.getByRole("button", { name: "테스트 뒤로" }));
    await waitFor(() =>
      expect(screen.getByTestId("location").textContent).toBe("/previous"),
    );
  });
});
