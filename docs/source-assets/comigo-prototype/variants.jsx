// variants.jsx — the decision record. The rejected directions are no longer
// drawn; this board is why they were cut, and which surviving screens are
// STATES of one canonical screen rather than competing options.

function DecRow({ kicker, title, body, chosen, cut, states }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
      <div>
        <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".14em", color: "var(--accent-ink)" }}>{kicker}</div>
        <div className="rs-display" style={{ fontSize: 23, marginTop: 7, letterSpacing: "-0.015em" }}>{title}</div>
        <div style={{ fontSize: 13, color: "var(--ink-3)", marginTop: 8, lineHeight: 1.55, maxWidth: 900 }}>{body}</div>
      </div>
      <div style={{ display: "flex", gap: 14, alignItems: "stretch", flexWrap: "wrap" }}>
        {chosen && (
          <div style={{ width: 300, padding: "13px 15px", borderRadius: 16, background: "var(--status-approved-soft)", border: "1.5px solid var(--status-approved)", display: "flex", flexDirection: "column", gap: 7 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", color: "var(--ink-3)", flex: 1 }}>{chosen.id}</div>
              <span style={{ height: 20, padding: "0 8px", borderRadius: 999, background: "var(--status-approved-ink)", color: "var(--on-bright-fill)", fontSize: 9.5, fontWeight: 800, letterSpacing: ".06em", display: "inline-flex", alignItems: "center" }}>CANONICAL</span>
            </div>
            <div style={{ fontSize: 14.5, fontWeight: 800 }}>{chosen.name}</div>
            <div style={{ fontSize: 12, color: "var(--ink-2)", lineHeight: 1.5 }}>{chosen.why}</div>
          </div>
        )}
        {(cut || []).map(c => (
          <div key={c.id} style={{ width: 300, padding: "13px 15px", borderRadius: 16, background: "var(--surface)", border: "1.5px dashed var(--line-2)", display: "flex", flexDirection: "column", gap: 7, opacity: .85 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", color: "var(--ink-3)", flex: 1, textDecoration: "line-through" }}>{c.id}</div>
              <span style={{ height: 20, padding: "0 8px", borderRadius: 999, background: "var(--status-rejected-soft)", color: "var(--status-rejected-ink)", fontSize: 9.5, fontWeight: 800, letterSpacing: ".06em", display: "inline-flex", alignItems: "center" }}>CUT</span>
            </div>
            <div style={{ fontSize: 14.5, fontWeight: 800, color: "var(--ink-3)" }}>{c.name}</div>
            <div style={{ fontSize: 12, color: "var(--ink-3)", lineHeight: 1.5 }}>{c.why}</div>
          </div>
        ))}
        {(states || []).map(s => (
          <div key={s.id} style={{ width: 300, padding: "13px 15px", borderRadius: 16, background: "var(--mode-drive-soft)", border: "1.5px solid var(--mode-drive)", display: "flex", flexDirection: "column", gap: 7 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", color: "var(--ink-3)", flex: 1 }}>{s.id}</div>
              <span style={{ height: 20, padding: "0 8px", borderRadius: 999, background: "var(--mode-drive)", color: "var(--on-bright-fill)", fontSize: 9.5, fontWeight: 800, letterSpacing: ".06em", display: "inline-flex", alignItems: "center" }}>STATE</span>
            </div>
            <div style={{ fontSize: 14.5, fontWeight: 800 }}>{s.name}</div>
            <div style={{ fontSize: 12, color: "var(--ink-2)", lineHeight: 1.5 }}><b>Shown when. </b>{s.when}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

const DECISIONS = [
  {
    kicker: "DECIDED", title: "Mode switching",
    body: "Three placements were built and reviewed. Only the chosen one is still drawn; the other two are recorded here and in README §2 so the decision has a trail.",
    chosen: { id: "V2 → shipped on P01 / D08", name: "Mode chip", why: "A pill top-left of home. One tap swaps when both roles are approved; the sheet appears only when something needs explaining. Riders who never drive see no chip at all." },
    cut: [
      { id: "V1", name: "Header segment", why: "One tap, but it occupies the header on every visit and dangles a Drive control at the ~90% who only ride." },
      { id: "V3", name: "Account only", why: "Three taps from home for something drivers do several times a day, and it buried the most important fact about the account." },
    ],
  },
  {
    kicker: "DECIDED", title: "Driver dashboard",
    body: "Two homes were built. The status-board version was a schedule view competing with a tab that already exists.",
    chosen: { id: "D08", name: "Earnings-first", why: "Money is why someone opens the driver side, and it keeps acceptance rate — the KPI that gates the account — in daily view." },
    cut: [
      { id: "D08b", name: "Status board", why: "Its today-timeline duplicated the Trips tab, and it pushed the earnings figure below the fold on a busy day. D09 covers the schedule need." },
    ],
  },
  {
    kicker: "NOT VARIANTS", title: "Screens that look like options but are states",
    body: "These were reviewed as alternatives and kept — not as competing directions, but because each is the same screen under a different condition. Building only one of each pair would leave a real state undrawn.",
    states: [
      { id: "P02", name: "Commuter home", when: "The user has a saved commute with matches today. P01 is the first-run and general case." },
      { id: "P05", name: "Results grouped by tier", when: "Results span three or more overlap tiers, or it's the user's first search. P04 is the default list." },
      { id: "P06", name: "Results on a map", when: "The user taps the map toggle on P04 — and it answers \"where will this leave me\" better than any list." },
      { id: "D13b", name: "One-form publish", when: "Republishing a familiar route — offered as \"publish like last time\" from D09. The wizard runs on first publish." },
      { id: "D18b", name: "Live trip as a list", when: "Stopped, or the map fails. Required regardless: every map surface ships an accessible list equivalent." },
      { id: "P09b", name: "Instant-book checkout", when: "The driver has instant booking on. P09 is the approve-each-request path." },
    ],
  },
];

function DecisionsBoard() {
  return (
    <BBoard pad={36}>
      <BTitle sub="What is canonical, what was cut, and which screens are states rather than options. Five artboards were removed at finalisation — V1, V2a, V2b, V3 and D08b — because a rejected direction left on the canvas reads as a screen someone still has to build.">Decisions</BTitle>
      <div style={{ display: "flex", flexDirection: "column", gap: 30 }}>
        {DECISIONS.map((d, i) => (
          <div key={d.title} style={{ paddingTop: i ? 26 : 0, borderTop: i ? "1px solid var(--line)" : "none" }}>
            <DecRow {...d}/>
          </div>
        ))}
      </div>
    </BBoard>
  );
}

Object.assign(window, { DecisionsBoard, DecRow, DECISIONS });
