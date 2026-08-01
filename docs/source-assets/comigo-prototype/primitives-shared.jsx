// primitives-shared.jsx — components shared by the merged (mode-aware) surfaces

// ── app bar used by every pushed screen ──
function AppBar({ title, sub, right, onDark = false }) {
  return (
    <div style={{
      padding: "10px 16px", display: "flex", alignItems: "center", gap: 10, flexShrink: 0,
      background: onDark ? "transparent" : "var(--surface)",
      borderBottom: onDark ? "none" : "1px solid var(--line)",
    }}>
      <button data-back="1" style={{ width: 44, height: 44, borderRadius: 22, background: onDark ? "rgba(255,255,255,.16)" : "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label="Back">
        <Icon name="back" size={20} color={onDark ? "var(--on-ink-fill)" : "var(--ink)"}/>
      </button>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 15.5, fontWeight: 700, color: onDark ? "var(--on-ink-fill)" : "var(--ink)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{title}</div>
        {sub && <div style={{ fontSize: 11.5, color: onDark ? "rgba(244,236,224,.72)" : "var(--ink-3)", marginTop: 1 }}>{sub}</div>}
      </div>
      {right}
    </div>
  );
}

// ── list row ──
function MenuRow({ icon, label, sub, right, tint, fg, badge, danger, chev = true }) {
  return (
    <div data-row={label} style={{ minHeight: 60, padding: "10px 4px", display: "flex", alignItems: "center", gap: 13 }}>
      <div style={{ width: 38, height: 38, borderRadius: 12, background: tint || "var(--bg-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
        <Icon name={icon} size={18} color={danger ? "var(--status-rejected-ink)" : fg || "var(--ink-2)"}/>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 600, color: danger ? "var(--status-rejected-ink)" : "var(--ink)" }}>{label}</div>
        {sub && <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>{sub}</div>}
      </div>
      {badge}
      {right}
      {chev && !right && <Icon name="chev" size={17} color="var(--ink-3)"/>}
    </div>
  );
}

function GroupLabel({ children, style }) {
  return <div className="rs-section-label" style={{ padding: "16px 4px 6px", ...style }}>{children}</div>;
}

// ── segmented control ──
function Segmented({ options = [], value, tint = "var(--ink-fill)", fg = "var(--on-ink-fill)" }) {
  return (
    <div style={{ display: "flex", padding: 4, gap: 4, background: "var(--bg-soft)", borderRadius: 999, border: "1px solid var(--line)" }}>
      {options.map(o => {
        const on = o === value;
        return (
          <div key={o} data-row={o} style={{
            flex: 1, minHeight: 44, borderRadius: 999, display: "flex", alignItems: "center", justifyContent: "center",
            background: on ? tint : "transparent", color: on ? fg : "var(--ink-3)",
            fontSize: 13, fontWeight: on ? 800 : 600,
          }}>{o}</div>
        );
      })}
    </div>
  );
}

// ── toggle ──
function Toggle({ on, tint = "var(--accent-ink)" }) {
  return (
    <div style={{ width: 46, height: 28, borderRadius: 999, background: on ? tint : "var(--line-2)", padding: 3, display: "flex", justifyContent: on ? "flex-end" : "flex-start", flexShrink: 0 }}>
      <div style={{ width: 22, height: 22, borderRadius: 11, background: "#fff", boxShadow: "var(--shadow-sm)" }}/>
    </div>
  );
}

// ── verification / document status badge ──
const STATUS_META = {
  approved: { label: "APPROVED", c: "var(--status-approved-ink)", bg: "var(--status-approved-soft)", icon: "check" },
  pending: { label: "IN REVIEW", c: "var(--status-pending-ink)", bg: "var(--status-pending-soft)", icon: "clock" },
  rejected: { label: "REJECTED", c: "var(--status-rejected-ink)", bg: "var(--status-rejected-soft)", icon: "alert" },
  expiring: { label: "EXPIRES SOON", c: "var(--status-expiring-ink)", bg: "var(--status-expiring-soft)", icon: "alert" },
  none: { label: "NOT STARTED", c: "var(--ink-3)", bg: "var(--bg-soft)", icon: "plus" },
};
function StatusBadge({ status = "pending", label }) {
  const m = STATUS_META[status] || STATUS_META.pending;
  return (
    <div style={{ height: 22, padding: "0 9px", borderRadius: 999, background: m.bg, color: m.c, fontSize: 10, fontWeight: 800, letterSpacing: ".06em", display: "inline-flex", alignItems: "center", gap: 5, flexShrink: 0 }}>
      <Icon name={m.icon} size={11} color={m.c} strokeWidth={2.6}/>{label || m.label}
    </div>
  );
}

// ── money ──
const money = (n) => n.toLocaleString("en-LK");

// ── data-driven fare breakdown ──
// lines: [{ label, sub, value, kind }] · kind: base | discount | fee | adjust | info
// Nothing here is hard-coded: the per-km rate, commission %, currency and the
// discount label all arrive as data, and the row survives 1–5 lines.
function FareBreakdown({ currency = "LKR", lines = [], total, totalLabel = "You pay", footnote, compact }) {
  const sign = (k) => (k === "discount" ? "−" : k === "adjust" ? "+" : "");
  return (
    <div style={{ background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 18, padding: compact ? 14 : 16 }}>
      {lines.filter(l => l.value !== 0 || l.always).map((l, i) => (
        <div key={l.label} style={{ display: "flex", alignItems: "flex-start", gap: 12, paddingTop: i ? 11 : 0 }}>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 13, fontWeight: 600, color: l.kind === "discount" ? "var(--status-approved-ink)" : "var(--ink-2)" }}>{l.label}</div>
            {l.sub && <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>{l.sub}</div>}
          </div>
          <div className="tab" style={{ fontSize: 13.5, fontWeight: 700, flexShrink: 0, color: l.kind === "discount" ? "var(--status-approved-ink)" : "var(--ink)" }}>
            {sign(l.kind)}{currency} {money(l.value)}
          </div>
        </div>
      ))}
      <div style={{ height: 1, background: "var(--line)", margin: "13px 0" }}/>
      <div style={{ display: "flex", alignItems: "baseline", gap: 12 }}>
        <div style={{ flex: 1, fontSize: 13.5, fontWeight: 800 }}>{totalLabel}</div>
        <div className="rs-display tab" style={{ fontSize: 23, fontWeight: 600, flexShrink: 0 }}>{currency} {money(total)}</div>
      </div>
      {footnote && <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 9, lineHeight: 1.45 }}>{footnote}</div>}
    </div>
  );
}

// ── skeleton + empty + inline error, the reusable state set ──
function Skel({ w = "100%", h = 12, r = 8, style }) {
  return <div className="rs-skel" style={{ width: w, height: h, borderRadius: r, ...style }}/>;
}
function SkelRow() {
  return (
    <div style={{ padding: 14, background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 16, display: "flex", alignItems: "center", gap: 12 }}>
      <Skel w={44} h={44} r={22}/>
      <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: 7 }}>
        <Skel w="62%" h={12}/><Skel w="42%" h={10}/>
      </div>
      <Skel w={54} h={16}/>
    </div>
  );
}
function EmptyState({ icon = "search", title, body, cta, kind = "empty" }) {
  const tint = kind === "error" ? "var(--status-rejected-soft)" : kind === "offline" ? "var(--status-pending-soft)" : "var(--bg-soft)";
  const fg = kind === "error" ? "var(--status-rejected-ink)" : kind === "offline" ? "var(--status-pending-ink)" : "var(--ink-3)";
  return (
    <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", textAlign: "center", padding: "0 32px", gap: 14 }}>
      <div style={{ width: 62, height: 62, borderRadius: 20, background: tint, display: "flex", alignItems: "center", justifyContent: "center" }}>
        <Icon name={icon} size={28} color={fg}/>
      </div>
      <div>
        <div style={{ fontSize: 17, fontWeight: 800 }}>{title}</div>
        <div style={{ fontSize: 13, color: "var(--ink-3)", marginTop: 7, lineHeight: 1.55, textWrap: "pretty" }}>{body}</div>
      </div>
      {cta && <button className="rs-btn accent" style={{ height: 46, padding: "0 22px", marginTop: 2 }}>{cta}</button>}
    </div>
  );
}

// ── banner (offline, notifications-off, degraded config) ──
function Banner({ kind = "info", icon, title, body, action }) {
  const m = {
    info: { bg: "var(--surface)", bd: "var(--line)", fg: "var(--ink-3)" },
    warn: { bg: "var(--status-pending-soft)", bd: "var(--status-pending)", fg: "var(--status-pending-ink)" },
    bad: { bg: "var(--status-rejected-soft)", bd: "var(--status-rejected)", fg: "var(--status-rejected-ink)" },
    good: { bg: "var(--status-approved-soft)", bd: "var(--status-approved)", fg: "var(--status-approved-ink)" },
  }[kind];
  return (
    <div style={{ padding: "11px 13px", borderRadius: 14, background: m.bg, border: `1px solid ${m.bd}`, display: "flex", gap: 10, alignItems: "flex-start" }}>
      <Icon name={icon || "alert"} size={17} color={m.fg} strokeWidth={2.2}/>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 12.5, fontWeight: 700, color: m.fg }}>{title}</div>
        {body && <div style={{ fontSize: 11.5, color: "var(--ink-2)", marginTop: 3, lineHeight: 1.45 }}>{body}</div>}
      </div>
      {action && <button style={{ fontSize: 12, fontWeight: 800, color: m.fg, minHeight: 44, minWidth: 44, padding: "0 8px", flexShrink: 0 }}>{action}</button>}
    </div>
  );
}

// ── mutual trust panel ──────────────────────────────────────────────────────
// Both sides see the same shape: a score, how many scores it rests on, completed
// trips, and how long the person has been here. Riders see it about drivers,
// drivers see it about riders. Never a bare star with no denominator — "5.0"
// off two ratings is not the same claim as "4.8" off 128.
function TrustStats({ name, role = "Driver", rating, ratings, trips, since, completed, stats, tint = "var(--accent-ink)", right, foot }) {
  const cells = stats || [
    { l: "Trips", v: trips },
    { l: "Completed", v: `${completed}%` },
    { l: "Since", v: since },
  ];
  return (
    <div className="rs-card" style={{ padding: 15 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
        <Avatar name={name} size={44}/>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", color: tint }}>{role.toUpperCase()}</div>
          <div style={{ fontSize: 14.5, fontWeight: 800, marginTop: 2 }}>{name}</div>
        </div>
        <div style={{ textAlign: "right", flexShrink: 0 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 4, justifyContent: "flex-end" }}>
            <Icon name="star" size={14} color="var(--status-pending-ink)"/>
            {/* One decimal always. A bare "5" beside "84 ratings" reads as a truncation bug. */}
            <div className="rs-display tab" style={{ fontSize: 20, fontWeight: 600 }}>{Number(rating).toFixed(1)}</div>
          </div>
          <div style={{ fontSize: 10.5, color: "var(--ink-3)", marginTop: 1 }}>{ratings} ratings</div>
        </div>
        {right}
      </div>
      {foot && <div className="tab" style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 9 }}>{foot}</div>}
      <div style={{ height: 1, background: "var(--line)", margin: "13px 0" }}/>
      <div style={{ display: "flex", gap: 10 }}>
        {cells.map(c => (
          <div key={c.l} style={{ flex: 1 }}>
            <div style={{ fontSize: 10.5, color: "var(--ink-3)", fontWeight: 600 }}>{c.l}</div>
            <div className="tab" style={{ fontSize: 14.5, fontWeight: 800, marginTop: 2 }}>{c.v}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ── one rule, stated in the place it applies ─────────────────────────────────
// Money and consequence rules are repeated across many screens; this keeps the
// wording and the icon identical everywhere so a rider learns it once.
function RuleRow({ icon, title, body, tint = "var(--ink-3)" }) {
  return (
    <div style={{ display: "flex", gap: 11 }}>
      <Icon name={icon} size={17} color={tint} strokeWidth={2.1}/>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 12.5, fontWeight: 700 }}>{title}</div>
        {body && <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.45 }}>{body}</div>}
      </div>
    </div>
  );
}

// "design-only, needs backend" marker — used wherever §5.3 applies
function NeedsBackend({ children = "NEEDS BACKEND" }) {
  return (
    <span style={{ height: 17, padding: "0 6px", borderRadius: 999, background: "var(--status-pending-soft)", color: "var(--status-pending-ink)", fontSize: 9.5, fontWeight: 800, letterSpacing: ".06em", display: "inline-flex", alignItems: "center", flexShrink: 0 }}>{children}</span>
  );
}

Object.assign(window, { AppBar, MenuRow, GroupLabel, Segmented, Toggle, StatusBadge, STATUS_META, money, FareBreakdown, Skel, SkelRow, EmptyState, Banner, TrustStats, RuleRow, NeedsBackend });
