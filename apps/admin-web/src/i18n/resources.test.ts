import { expect, test } from "vitest";
import { en, ko } from "./resources";

function leafKeys(value: Record<string, unknown>, prefix = ""): string[] {
  return Object.entries(value).flatMap(([key, child]) => {
    const path = prefix ? `${prefix}.${key}` : key;
    return typeof child === "object" && child !== null
      ? leafKeys(child as Record<string, unknown>, path)
      : [path];
  });
}

test("Korean and English resources contain the same translation keys", () => {
  expect(leafKeys(en).sort()).toEqual(leafKeys(ko).sort());
});

test("monitoring resources include the localized live-status and uptime labels", () => {
  expect(ko.dashboard).toHaveProperty("monitoringLoading");
  expect(ko.dashboard).toHaveProperty("staleStatus");
  expect(ko.dashboard).toHaveProperty("monitoringErrorStatus");
  expect(ko.dashboard).toHaveProperty("uptimeValue");
  expect(leafKeys(en.dashboard).sort()).toEqual(leafKeys(ko.dashboard).sort());
});
