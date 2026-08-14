import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, expect, test } from "vitest";
import { ThemeProvider } from "../provider/ThemeProvider";
import { ThemeMenu } from "./ThemeMenu";

beforeEach(() => localStorage.clear());
afterEach(cleanup);

test("opens the theme menu and persists a dark theme selection", async () => {
  render(
    <ThemeProvider>
      <ThemeMenu />
    </ThemeProvider>,
  );

  fireEvent.click(screen.getByRole("button", { name: "테마" }));
  fireEvent.click(await screen.findByRole("menuitemradio", { name: "다크" }));

  expect(document.documentElement).toHaveClass("dark");
  expect(localStorage.getItem("ino-admin.theme")).toBe("dark");
});
