import { expect, it, vi } from "vitest";
import { LiveClient } from "../src/core/liveclient/liveClient";
import { LcuClient } from "../src/core/lcu/lcuClient";
import { parseLockfile } from "../src/core/lcu/lockfile";

function jsonResponse(body: unknown, ok = true, status = 200): Response {
  return { ok, status, json: async () => body } as unknown as Response;
}

const respond = (body: unknown) =>
  async (_url: string, _init?: RequestInit): Promise<Response> => jsonResponse(body);

it("LiveClient unwraps the events payload", async () => {
  const fetchImpl = vi.fn(respond({ Events: [{ EventID: 1, EventName: "GameStart", EventTime: 0 }] }));
  const client = new LiveClient(fetchImpl);

  const events = await client.events();

  expect(fetchImpl.mock.calls[0][0]).toBe("https://127.0.0.1:2999/liveclientdata/eventdata");
  expect(events).toHaveLength(1);
  expect(events[0].EventName).toBe("GameStart");
});

it("LcuClient sends basic auth and reads the champ-select session", async () => {
  const fetchImpl = vi.fn(respond({ localPlayerCellId: 0, myTeam: [], theirTeam: [] }));
  const lockfile = parseLockfile("LeagueClient:1:55000:pw:https");
  const client = new LcuClient(lockfile, fetchImpl);

  const session = await client.getChampSelectSession();

  const [url, init] = fetchImpl.mock.calls[0];
  expect(url).toBe("https://127.0.0.1:55000/lol-champ-select/v1/session");
  expect((init?.headers as Record<string, string>).Authorization).toBe(
    "Basic " + Buffer.from("riot:pw").toString("base64"),
  );
  expect(session.localPlayerCellId).toBe(0);
});
