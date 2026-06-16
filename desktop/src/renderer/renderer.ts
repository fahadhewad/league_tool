/** Main window renderer: status, a compact live-draft / in-game summary, and profile lookup. */
// window.leaguetool is typed globally via the augmentation in ../main/preload.

const status = document.getElementById("status");
const output = document.getElementById("output");

function setStatus(text: string): void {
  if (status) {
    status.textContent = text;
  }
}

window.leaguetool.onChampSelect((draft) => {
  setStatus("In champ select");
  if (!output) {
    return;
  }
  const lines = [
    draft.role ? `Your role: ${draft.role}` : "Role: (unassigned)",
    draft.winProbabilityPct !== null ? `Win probability: ${draft.winProbabilityPct}%` : "Win probability: n/a",
    `Allies: ${draft.allies.join(", ") || "—"}`,
    `Enemies: ${draft.enemies.join(", ") || "—"}`,
    "",
    "Recommended picks:",
    ...draft.topPicks.map((p) => `  ${p.name} (${p.score}) — ${p.reasons[0] ?? ""}`),
  ];
  output.textContent = lines.join("\n");
});

window.leaguetool.onInGame((live) => {
  setStatus(`In game — ${live.clock}`);
  if (!output) {
    return;
  }
  const o = live.objectives;
  output.textContent = [
    `${live.gameMode} — ${live.clock}`,
    `Objectives: Dragons ${o.dragons}, Barons ${o.barons}, Heralds ${o.heralds}, Towers ${o.towers}`,
    "",
    "Recent events:",
    ...live.recentEvents.map((e) => `  ${e}`),
  ].join("\n");
});

const form = document.getElementById("profile-form") as HTMLFormElement | null;
form?.addEventListener("submit", async (event) => {
  event.preventDefault();
  const data = new FormData(form);
  const profile = await window.leaguetool.getProfile(
    String(data.get("platform")),
    String(data.get("gameName")),
    String(data.get("tagLine")),
  );
  if (output) {
    output.textContent = JSON.stringify(profile, null, 2);
  }
});

export {};
