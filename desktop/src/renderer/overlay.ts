/**
 * Overlay window renderer. Receives the finished view-models from the main process and paints a
 * compact, transparent card: champ-select pick advice + win probability during draft, and timers /
 * objectives / scoreboard during a live game. DOM is built with textContent (no innerHTML) to keep
 * the strict CSP and avoid injection.
 */
// Types only (erased at build); window.leaguetool is typed globally via ../main/preload.
import type { DraftOverlayModel } from "../core/champSelect/draftOverlayModel";
import type { LiveOverlayModel } from "../core/liveclient/liveOverlayModel";

const body = document.getElementById("overlay-body");

function el(tag: string, className?: string, text?: string): HTMLElement {
  const node = document.createElement(tag);
  if (className) {
    node.className = className;
  }
  if (text !== undefined) {
    node.textContent = text;
  }
  return node;
}

function clear(node: HTMLElement): void {
  while (node.firstChild) {
    node.removeChild(node.firstChild);
  }
}

function renderDraft(model: DraftOverlayModel): void {
  if (!body) {
    return;
  }
  clear(body);
  body.appendChild(el("div", "section-title", model.role ? `Draft — your role: ${model.role}` : "Draft"));

  if (model.winProbabilityPct !== null) {
    const wrap = el("div", "winprob");
    wrap.appendChild(el("span", "winprob-label", `Win probability: ${model.winProbabilityPct}%`));
    const bar = el("div", "winprob-bar");
    const fill = el("div", "winprob-fill");
    fill.style.width = `${model.winProbabilityPct}%`;
    bar.appendChild(fill);
    wrap.appendChild(bar);
    if (model.winProbabilitySource) {
      wrap.appendChild(el("span", "muted", model.winProbabilitySource));
    }
    body.appendChild(wrap);
  }

  body.appendChild(el("div", "teams", `Allies: ${model.allies.join(", ") || "—"}`));
  body.appendChild(el("div", "teams", `Enemies: ${model.enemies.join(", ") || "—"}`));

  if (model.topPicks.length > 0) {
    body.appendChild(el("div", "section-title", "Recommended picks"));
    const list = el("ul", "picks");
    for (const pick of model.topPicks) {
      const item = el("li");
      item.appendChild(el("span", "pick-name", `${pick.name} (${pick.score})`));
      if (pick.reasons.length > 0) {
        item.appendChild(el("span", "muted", pick.reasons[0]));
      }
      list.appendChild(item);
    }
    body.appendChild(list);
  }
}

function renderLive(model: LiveOverlayModel): void {
  if (!body) {
    return;
  }
  clear(body);
  body.appendChild(el("div", "section-title", `In game — ${model.clock} (${model.gameMode})`));
  const o = model.objectives;
  body.appendChild(
    el("div", "objectives", `Dragons ${o.dragons} · Barons ${o.barons} · Heralds ${o.heralds} · Towers ${o.towers}`),
  );

  if (model.recentEvents.length > 0) {
    body.appendChild(el("div", "section-title", "Recent"));
    const list = el("ul", "events");
    for (const event of model.recentEvents) {
      list.appendChild(el("li", undefined, event));
    }
    body.appendChild(list);
  }
}

window.leaguetool.onChampSelect(renderDraft);
window.leaguetool.onInGame(renderLive);

export {};
