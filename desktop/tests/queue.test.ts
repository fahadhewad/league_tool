import { expect, it } from "vitest";
import { isRankedSoloOrFlex } from "../src/core/compliance/queue";

it("identifies ranked solo and flex queues", () => {
  expect(isRankedSoloOrFlex(420)).toBe(true); // Solo/Duo
  expect(isRankedSoloOrFlex(440)).toBe(true); // Flex
});

it("treats other queues as not ranked", () => {
  expect(isRankedSoloOrFlex(400)).toBe(false); // Normal Draft
  expect(isRankedSoloOrFlex(450)).toBe(false); // ARAM
  expect(isRankedSoloOrFlex(undefined)).toBe(false);
  expect(isRankedSoloOrFlex(null)).toBe(false);
});
