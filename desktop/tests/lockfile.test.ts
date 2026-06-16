import { expect, it } from "vitest";
import { lcuAuthHeader, lcuBaseUrl, parseLockfile } from "../src/core/lcu/lockfile";

it("parses a lockfile into its parts", () => {
  const lf = parseLockfile("LeagueClient:1234:50000:secretpw:https");
  expect(lf).toEqual({
    processName: "LeagueClient",
    pid: 1234,
    port: 50000,
    password: "secretpw",
    protocol: "https",
  });
});

it("builds the base URL and basic auth header", () => {
  const lf = parseLockfile("LeagueClient:1:443:pw:https");
  expect(lcuBaseUrl(lf)).toBe("https://127.0.0.1:443");
  expect(lcuAuthHeader(lf)).toBe("Basic " + Buffer.from("riot:pw").toString("base64"));
});

it("rejects malformed lockfiles", () => {
  expect(() => parseLockfile("garbage")).toThrow();
  expect(() => parseLockfile("LeagueClient:1:notaport:pw:https")).toThrow();
});
