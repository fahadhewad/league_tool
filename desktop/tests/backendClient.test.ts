import { expect, it, vi } from "vitest";
import { BackendClient } from "../src/core/backend/backendClient";

function jsonResponse(body: unknown, ok = true, status = 200): Response {
  return { ok, status, json: async () => body } as unknown as Response;
}

const respond = (body: unknown, ok = true, status = 200) =>
  async (_url: string, _init?: RequestInit): Promise<Response> => jsonResponse(body, ok, status);

it("posts recommend requests and returns the parsed body", async () => {
  const fetchImpl = vi.fn(respond({ role: "UTILITY", recommendations: [] }));
  const client = new BackendClient({ baseUrl: "http://test", fetchImpl });

  const result = await client.recommend({ role: "UTILITY", allies: [], enemies: [], bans: [] });

  const [url, init] = fetchImpl.mock.calls[0];
  expect(url).toBe("http://test/api/v1/draft/recommend");
  expect(init?.method).toBe("POST");
  expect(result.role).toBe("UTILITY");
});

it("url-encodes Riot ID segments in profile lookups", async () => {
  const fetchImpl = vi.fn(respond({}));
  const client = new BackendClient({ baseUrl: "http://test", fetchImpl });

  await client.getProfile("euw1", "Hide on bush", "KR1");

  expect(fetchImpl.mock.calls[0][0]).toBe("http://test/api/v1/profiles/euw1/Hide%20on%20bush/KR1");
});

it("throws on non-ok responses", async () => {
  const fetchImpl = vi.fn(respond({}, false, 404));
  const client = new BackendClient({ baseUrl: "http://test", fetchImpl });

  await expect(client.winProbability({ allies: [], enemies: [] })).rejects.toThrow("404");
});
