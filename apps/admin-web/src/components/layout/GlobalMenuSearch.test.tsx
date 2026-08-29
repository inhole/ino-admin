import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, expect, test } from "vitest";
import { GlobalMenuSearch } from "@/components/layout/GlobalMenuSearch";

const menus = [
  {
    id: "dashboard",
    label: "대시보드",
    route: "/",
    icon: "layout-dashboard" as const,
    order: 10,
    children: [],
  },
  {
    id: "users",
    label: "사용자",
    route: "/users",
    icon: "users" as const,
    order: 20,
    children: [],
  },
  {
    id: "permissions",
    label: "권한",
    route: "/permissions",
    icon: "key-round" as const,
    order: 30,
    children: [],
  },
];

afterEach(cleanup);

test("노출된 전체 메뉴를 대소문자 구분 없이 검색하고 결과로 이동한다", () => {
  render(
    <MemoryRouter initialEntries={["/"]}>
      <GlobalMenuSearch menus={menus} />
      <Routes>
        <Route element={<p>대시보드 화면</p>} path="/" />
        <Route element={<p>권한 화면</p>} path="/permissions" />
      </Routes>
    </MemoryRouter>,
  );

  const search = screen.getByRole("searchbox", { name: "전체 메뉴 검색" });
  fireEvent.change(search, { target: { value: "권한" } });

  expect(screen.getByRole("link", { name: "권한" })).toBeInTheDocument();
  expect(screen.queryByRole("link", { name: "사용자" })).not.toBeInTheDocument();

  fireEvent.click(screen.getByRole("link", { name: "권한" }));

  expect(screen.getByText("권한 화면")).toBeInTheDocument();
  expect(search).toHaveValue("");
});

test("검색 결과가 없으면 전용 상태를 표시한다", () => {
  render(
    <MemoryRouter>
      <GlobalMenuSearch menus={menus} />
    </MemoryRouter>,
  );

  fireEvent.change(
    screen.getByRole("searchbox", { name: "전체 메뉴 검색" }),
    { target: { value: "없는 메뉴" } },
  );

  expect(screen.getByText("검색 조건에 맞는 메뉴가 없습니다.")).toBeInTheDocument();
});
