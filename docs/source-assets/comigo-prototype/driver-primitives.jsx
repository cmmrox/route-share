// driver-primitives.jsx — driver-specific helpers (status pill, earnings tile, passenger row)

function StatusPill({ online }) {
  return (
    <div style={{
      display: "inline-flex", alignItems: "center", gap: 6,
      height: 30, padding: "0 12px", borderRadius: 999,
      background: online ? "var(--success-soft)" : "var(--bg-soft)",
      color: online ? "var(--match-full)" : "var(--ink-3)",
      fontSize: 12, fontWeight: 700, letterSpacing: ".04em",
    }}>
      <div style={{
        width: 8, height: 8, borderRadius: 4,
        background: online ? "var(--success)" : "var(--ink-4)",
        animation: online ? "pulse 1.8s infinite" : "none",
      }}/>
      {online ? "ONLINE" : "OFFLINE"}
    </div>
  );
}

function EarningsTile({ label, value, sub, big, dark }) {
  return (
    <div style={{
      flex: 1, padding: big ? 16 : 12, borderRadius: 16,
      background: dark ? "rgba(255,255,255,.12)" : "var(--surface)",
      border: dark ? "none" : "1px solid var(--line)",
      color: dark ? "#faf7f2" : "var(--ink)",
    }}>
      <div style={{ fontSize: 10, letterSpacing: ".12em", fontWeight: 700, opacity: .7 }}>{label}</div>
      <div className="rs-display tab" style={{ fontSize: big ? 28 : 20, lineHeight: 1, marginTop: 4, fontWeight: 600 }}>{value}</div>
      {sub && <div style={{ fontSize: 11, opacity: .65, marginTop: 4 }}>{sub}</div>}
    </div>
  );
}

// Passenger row used on trip detail, live trip, booking requests
function PaxRow({ name, pickup, drop, fare, dist, status, action }) {
  const statusMeta = {
    pending: { tint: "var(--accent-soft)", fg: "var(--accent-ink)", label: "REQUEST" },
    confirmed: { tint: "var(--teal-soft)", fg: "var(--teal)", label: "CONFIRMED" },
    boarded: { tint: "var(--success-soft)", fg: "var(--match-full)", label: "ON BOARD" },
    dropped: { tint: "var(--bg-soft)", fg: "var(--ink-3)", label: "DROPPED" },
    noshow: { tint: "var(--danger-soft)", fg: "var(--danger)", label: "NO-SHOW" },
  };
  const s = statusMeta[status] || statusMeta.confirmed;
  return (
    <div style={{ padding: 14, background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 16 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
        <Avatar name={name} size={42}/>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontWeight: 700, fontSize: 14, display: "flex", alignItems: "center", gap: 6 }}>
            {name}
            <span style={{ height: 18, padding: "0 7px", borderRadius: 999, background: s.tint, color: s.fg, fontSize: 9, fontWeight: 700, letterSpacing: ".04em", display: "inline-flex", alignItems: "center" }}>{s.label}</span>
          </div>
          <div style={{ fontSize: 11, color: "var(--ink-3)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{dist} · LKR {fare}</div>
        </div>
        {action}
      </div>
      <div style={{ marginTop: 10, paddingLeft: 8, position: "relative" }}>
        <div style={{ position: "absolute", left: 13, top: 8, bottom: 8, width: 2, background: "var(--line-2)", backgroundImage: "linear-gradient(var(--line-2) 60%, transparent 60%)", backgroundSize: "2px 4px" }}/>
        <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 6 }}>
          <div style={{ width: 10, height: 10, borderRadius: 5, background: "var(--teal)" }}/>
          <div style={{ fontSize: 12, color: "var(--ink-2)", flex: 1, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{pickup}</div>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <div style={{ width: 10, height: 10, background: "var(--accent)" }}/>
          <div style={{ fontSize: 12, color: "var(--ink-2)", flex: 1, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{drop}</div>
        </div>
      </div>
    </div>
  );
}

// Mini stepper for KYC/create-trip flows
function Stepper({ step = 1, total = 3, labels }) {
  return (
    <div style={{ padding: "0 4px" }}>
      <div style={{ display: "flex", gap: 6 }}>
        {Array.from({ length: total }, (_, i) => (
          <div key={i} style={{
            flex: 1, height: 4, borderRadius: 2,
            background: i < step ? "var(--accent)" : "var(--bg-soft)",
            transition: "background .2s",
          }}/>
        ))}
      </div>
      {labels && (
        <div style={{ display: "flex", justifyContent: "space-between", marginTop: 8, fontSize: 10, fontWeight: 700, letterSpacing: ".08em" }}>
          {labels.map((l, i) => (
            <div key={i} style={{ color: i < step ? "var(--accent-ink)" : "var(--ink-3)" }}>{l.toUpperCase()}</div>
          ))}
        </div>
      )}
    </div>
  );
}

// Calendar weekday picker
function DayPicker({ days = [], onChange }) {
  const D = ["M", "T", "W", "T", "F", "S", "S"];
  const N = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
  return (
    <div style={{ display: "flex", gap: 8 }}>
      {D.map((d, i) => {
        const on = days.includes(N[i]);
        return (
          <div key={i} style={{
            flex: 1, height: 44, borderRadius: 12,
            background: on ? "var(--accent)" : "var(--surface)",
            color: on ? "#fff" : "var(--ink)",
            border: on ? "none" : "1.5px solid var(--line)",
            display: "flex", alignItems: "center", justifyContent: "center",
            fontWeight: 700, fontSize: 14,
          }} onClick={() => onChange && onChange(on ? days.filter(x => x !== N[i]) : [...days, N[i]])}>{d}</div>
        );
      })}
    </div>
  );
}

// Document upload card
function DocUpload({ icon, label, status, hint, image }) {
  // status: 'empty' | 'uploaded' | 'verified' | 'rejected'
  const meta = {
    empty: { border: "1.5px dashed var(--line-2)", bg: "var(--bg-soft)", fg: "var(--ink-3)" },
    uploaded: { border: "1.5px solid var(--teal)", bg: "var(--teal-soft)", fg: "var(--teal)" },
    verified: { border: "1.5px solid var(--success)", bg: "var(--success-soft)", fg: "var(--match-full)" },
    rejected: { border: "1.5px solid var(--danger)", bg: "var(--danger-soft)", fg: "var(--danger)" },
  };
  const s = meta[status] || meta.empty;
  return (
    <div style={{ padding: 16, borderRadius: 16, background: s.bg, border: s.border, display: "flex", alignItems: "center", gap: 14 }}>
      <div style={{ width: 52, height: 52, borderRadius: 14, background: "rgba(255,255,255,.7)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
        <Icon name={icon} size={24} color={s.fg}/>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 700 }}>{label}</div>
        <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 2 }}>{hint}</div>
      </div>
      {status === "empty" && <button className="rs-btn ghost" style={{ height: 36, padding: "0 14px", fontSize: 12 }}>Upload</button>}
      {status === "uploaded" && <div style={{ fontSize: 11, fontWeight: 700, color: s.fg, letterSpacing: ".08em" }}>IN REVIEW</div>}
      {status === "verified" && <Icon name="check" size={22} color={s.fg} strokeWidth={2.6}/>}
      {status === "rejected" && <button style={{ fontSize: 12, fontWeight: 700, color: s.fg }}>Retry</button>}
    </div>
  );
}

Object.assign(window, { StatusPill, EarningsTile, PaxRow, Stepper, DayPicker, DocUpload });
