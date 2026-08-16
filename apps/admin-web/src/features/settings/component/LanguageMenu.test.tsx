import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, expect, test } from "vitest";
import i18n from "@/i18n";
import { LanguageMenu } from "./LanguageMenu";

afterEach(cleanup);

test("switches to English and persists the locale", async () => {
  render(<LanguageMenu />);

  fireEvent.click(screen.getByRole("button", { name: "언어" }));
  fireEvent.click(await screen.findByRole("menuitemradio", { name: "영어" }));

  expect(i18n.resolvedLanguage).toBe("en");
  expect(document.documentElement).toHaveAttribute("lang", "en");
  expect(localStorage.getItem("ino-admin.locale")).toBe("en");
  expect(screen.getByRole("button", { name: "Language" })).toBeInTheDocument();
});
