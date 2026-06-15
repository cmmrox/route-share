// primitives.jsx — shared UI primitives for RouteShare

// Minimal, geometric icon set drawn as SVG strokes
const Icon = ({ name, size = 22, color = "currentColor", strokeWidth = 1.8 }) => {
  const common = {
    width: size, height: size, viewBox: "0 0 24 24",
    fill: "none", stroke: color, strokeWidth, strokeLinecap: "round", strokeLinejoin: "round",
  };
  const paths = {
    search: <><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></>,
    back: <path d="M15 6l-6 6 6 6"/>,
    close: <><path d="M6 6l12 12"/><path d="M18 6L6 18"/></>,
    chev: <path d="M9 6l6 6-6 6"/>,
    menu: <><path d="M4 7h16"/><path d="M4 12h16"/><path d="M4 17h16"/></>,
    pin: <><path d="M12 21s7-7.5 7-12a7 7 0 1 0-14 0c0 4.5 7 12 7 12z"/><circle cx="12" cy="9" r="2.5"/></>,
    dot: <circle cx="12" cy="12" r="3.5" fill={color} stroke="none"/>,
    home: <><path d="M4 11l8-7 8 7"/><path d="M6 10v10h12V10"/></>,
    briefcase: <><rect x="3" y="7" width="18" height="13" rx="2"/><path d="M9 7V5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2"/></>,
    star: <path d="M12 3l2.6 5.7 6.2.6-4.7 4.3 1.4 6.1L12 16.8 6.5 19.7l1.4-6.1L3.2 9.3l6.2-.6L12 3z"/>,
    clock: <><circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/></>,
    calendar: <><rect x="4" y="5" width="16" height="16" rx="2"/><path d="M4 10h16"/><path d="M9 3v4"/><path d="M15 3v4"/></>,
    users: <><circle cx="9" cy="9" r="3.5"/><path d="M2 20c0-3.5 3-6 7-6s7 2.5 7 6"/><circle cx="17" cy="7" r="2.5"/><path d="M22 16c0-2.2-2-4-4.5-4"/></>,
    user: <><circle cx="12" cy="8" r="3.5"/><path d="M4 20c0-4 4-6 8-6s8 2 8 6"/></>,
    card: <><rect x="3" y="6" width="18" height="13" rx="2"/><path d="M3 10h18"/></>,
    cash: <><rect x="3" y="7" width="18" height="11" rx="2"/><circle cx="12" cy="12.5" r="2.5"/></>,
    wallet: <><rect x="3" y="6" width="18" height="13" rx="2.5"/><path d="M16 13h3"/></>,
    phone: <path d="M6 4h4l1.5 4.5L9 10a10 10 0 0 0 5 5l1.5-2.5L20 14v4a2 2 0 0 1-2 2A14 14 0 0 1 4 6a2 2 0 0 1 2-2z"/>,
    shield: <><path d="M12 3l8 3v6c0 4.5-3.5 8-8 9-4.5-1-8-4.5-8-9V6l8-3z"/></>,
    alert: <><circle cx="12" cy="12" r="8.5"/><path d="M12 8v5"/><circle cx="12" cy="16" r=".6" fill={color} stroke="none"/></>,
    share: <><circle cx="6" cy="12" r="2.5"/><circle cx="18" cy="6" r="2.5"/><circle cx="18" cy="18" r="2.5"/><path d="M8 11l8-4"/><path d="M8 13l8 4"/></>,
    bell: <><path d="M6 16V11a6 6 0 0 1 12 0v5l1.5 2H4.5L6 16z"/><path d="M10 20a2 2 0 0 0 4 0"/></>,
    settings: <><circle cx="12" cy="12" r="3"/><path d="M12 2v3M12 19v3M22 12h-3M5 12H2M19 5l-2 2M7 17l-2 2M19 19l-2-2M7 7L5 5"/></>,
    help: <><circle cx="12" cy="12" r="8.5"/><path d="M9.5 9a2.5 2.5 0 0 1 5 .5c0 1.5-2 2-2.5 3.5"/><circle cx="12" cy="17" r=".6" fill={color} stroke="none"/></>,
    check: <path d="M5 12l4 4 10-10"/>,
    plus: <><path d="M12 5v14"/><path d="M5 12h14"/></>,
    minus: <path d="M5 12h14"/>,
    arrow: <><path d="M5 12h14"/><path d="M13 6l6 6-6 6"/></>,
    swap: <><path d="M7 7h13"/><path d="M17 4l3 3-3 3"/><path d="M17 17H4"/><path d="M7 14l-3 3 3 3"/></>,
    receipt: <><path d="M6 3h12v18l-3-2-3 2-3-2-3 2V3z"/><path d="M9 8h6M9 12h6M9 16h4"/></>,
    filter: <><path d="M3 5h18"/><path d="M6 12h12"/><path d="M10 19h4"/></>,
    history: <><path d="M3 12a9 9 0 1 0 3-6.7L3 8"/><path d="M3 3v5h5"/><path d="M12 8v4l3 2"/></>,
    lock: <><rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/></>,
    sos: <path d="M9 8H5a2 2 0 0 0 0 4h2a2 2 0 0 1 0 4H3M13 8v8M13 12h4M21 8v8"/>,
    mail: <><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 7l9 6 9-6"/></>,
    google: <path d="M21 12c0 5-4 8.5-9 8.5S3 17 3 12s4-8.5 9-8.5c2.5 0 4.5 1 6 2.5l-2.5 2.5c-1-1-2-1.5-3.5-1.5-3 0-5.5 2.5-5.5 5s2.5 5 5.5 5c3 0 4.5-2 5-4H12v-3h9"/>,
    car: <><path d="M5 17h14v-5l-2-5H7l-2 5v5z"/><circle cx="8" cy="17" r="1.5"/><circle cx="16" cy="17" r="1.5"/></>,
    route: <><circle cx="6" cy="6" r="2"/><circle cx="18" cy="18" r="2"/><path d="M8 6h6a4 4 0 0 1 0 8h-4a4 4 0 0 0 0 8h6"/></>,
    leaf: <path d="M4 20c0-9 6-16 16-16 0 10-6 16-16 16zM8 16c3-3 6-6 10-10"/>,
    thumb: <path d="M7 11l4-7c1 0 2 1 2 2v4h5a2 2 0 0 1 2 2l-1 6a2 2 0 0 1-2 2H7M7 11v9M3 11h4v9H3z"/>,
    ellipsis: <><circle cx="6" cy="12" r="1.4" fill={color} stroke="none"/><circle cx="12" cy="12" r="1.4" fill={color} stroke="none"/><circle cx="18" cy="12" r="1.4" fill={color} stroke="none"/></>,
    target: <><circle cx="12" cy="12" r="8.5"/><circle cx="12" cy="12" r="3.5"/><path d="M12 2v3M12 19v3M22 12h-3M5 12H2"/></>,
    compass: <><circle cx="12" cy="12" r="8.5"/><path d="M15 9l-2 5-5 2 2-5 5-2z" fill={color} stroke="none" opacity=".35"/><path d="M15 9l-2 5-5 2 2-5 5-2z"/></>,
  };
  return <svg {...common}>{paths[name]}</svg>;
};

// Avatar with initials and deterministic color
const AVATAR_COLORS = ["#0f6e66", "#d66a3b", "#8a5a2b", "#5c7c3a", "#8c4a6b", "#4a6c8a"];
function Avatar({ name = "", size = 44, style }) {
  const initials = name.split(" ").map(p => p[0]).slice(0, 2).join("").toUpperCase() || "?";
  const hash = [...name].reduce((a, c) => a + c.charCodeAt(0), 0);
  const bg = AVATAR_COLORS[hash % AVATAR_COLORS.length];
  return (
    <div className="rs-avatar" style={{ width: size, height: size, background: bg, fontSize: size * 0.38, ...style }}>
      {initials}
    </div>
  );
}

// Circular match % ring
function MatchRing({ value = 80, size = 56, strokeWidth = 4 }) {
  const r = (size - strokeWidth) / 2;
  const c = 2 * Math.PI * r;
  const color = value >= 95 ? "var(--match-full)" : value >= 70 ? "var(--match-high)" : value >= 50 ? "var(--match-mid)" : "var(--match-low)";
  return (
    <div className="rs-match-ring" style={{ width: size, height: size }}>
      <svg width={size} height={size}>
        <circle cx={size / 2} cy={size / 2} r={r} stroke="var(--line)" strokeWidth={strokeWidth} fill="none"/>
        <circle cx={size / 2} cy={size / 2} r={r} stroke={color} strokeWidth={strokeWidth} fill="none"
          strokeLinecap="round" strokeDasharray={c} strokeDashoffset={c * (1 - value / 100)}/>
      </svg>
      <span className="pct" style={{ fontSize: size * 0.27 }}>{value}<span style={{ fontSize: size * 0.18, color: "var(--ink-3)" }}>%</span></span>
    </div>
  );
}

// Map backdrop — renders a believable map with roads, blocks, pins, route stroke
function MapBackdrop({ showRoute = true, pickupLabel, dropLabel, variant = "default", style }) {
  return (
    <div className="rs-map" style={style}>
      {/* "Roads" — simple tinted strips */}
      <div className="rs-road" style={{ left: "-10%", right: "-10%", top: "22%", height: 14, transform: "rotate(-6deg)", opacity: .85 }}/>
      <div className="rs-road" style={{ left: "-10%", right: "-10%", top: "58%", height: 10, transform: "rotate(4deg)", opacity: .85 }}/>
      <div className="rs-road" style={{ left: "-10%", right: "-10%", top: "80%", height: 12, transform: "rotate(-3deg)", opacity: .7 }}/>
      <div className="rs-road" style={{ left: "18%", top: "-5%", bottom: "-5%", width: 10, transform: "rotate(8deg)", opacity: .8 }}/>
      <div className="rs-road" style={{ left: "68%", top: "-5%", bottom: "-5%", width: 10, transform: "rotate(-4deg)", opacity: .85 }}/>
      <div className="rs-road" style={{ left: "42%", top: "-5%", bottom: "-5%", width: 6, transform: "rotate(2deg)", opacity: .6 }}/>

      {/* Green blocks (parks) */}
      <div style={{ position: "absolute", left: "68%", top: "30%", width: 90, height: 70, background: "rgba(120,170,120,.22)", borderRadius: 18 }}/>
      <div style={{ position: "absolute", left: "8%", top: "65%", width: 60, height: 50, background: "rgba(120,170,120,.2)", borderRadius: 14 }}/>

      {/* Water */}
      <div style={{ position: "absolute", right: "-5%", bottom: "-5%", width: 180, height: 140, background: "rgba(100,140,170,.22)", borderRadius: "50% 0 0 50%" }}/>

      {/* Route stroke (SVG) */}
      {showRoute && (
        <svg viewBox="0 0 400 600" preserveAspectRatio="none" style={{ position: "absolute", inset: 0, width: "100%", height: "100%" }}>
          <path d="M 60 120 C 110 140, 140 220, 180 260 C 220 300, 260 340, 320 420" stroke="rgba(0,0,0,.12)" strokeWidth="9" fill="none" strokeLinecap="round"/>
          <path d="M 60 120 C 110 140, 140 220, 180 260 C 220 300, 260 340, 320 420" stroke="var(--ink)" strokeWidth="5" fill="none" strokeLinecap="round" strokeDasharray={variant === "dashed" ? "8 8" : undefined}/>
        </svg>
      )}

      {/* Pickup / drop pins */}
      {showRoute && (
        <>
          <div style={{ position: "absolute", left: "calc(15% - 10px)", top: "calc(20% - 10px)" }}>
            <PinDot color="var(--teal)" label={pickupLabel}/>
          </div>
          <div style={{ position: "absolute", left: "calc(80% - 10px)", top: "calc(70% - 10px)" }}>
            <PinDot color="var(--accent)" label={dropLabel}/>
          </div>
        </>
      )}
    </div>
  );
}

function PinDot({ color = "var(--ink)", label, ring = true }) {
  return (
    <div style={{ position: "relative", display: "inline-flex", alignItems: "center", gap: 6 }}>
      <div style={{ width: 18, height: 18, borderRadius: "50%", background: color, border: "3px solid #fff", boxShadow: "0 2px 6px rgba(0,0,0,.22)" }}/>
      {label && (
        <div style={{ position: "absolute", left: 24, top: -4, background: "#fff", color: "var(--ink)", fontSize: 11, fontWeight: 600, padding: "3px 8px", borderRadius: 8, whiteSpace: "nowrap", boxShadow: "0 2px 8px rgba(0,0,0,.1)" }}>
          {label}
        </div>
      )}
    </div>
  );
}

// Seat picker pictogram row (car top-down view)
function SeatPlan({ taken = [], selected = [], onToggle, capacity = 4 }) {
  const cells = Array.from({ length: capacity }, (_, i) => i);
  // 1 driver + (capacity-1) passengers in 2-col layout
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 8, alignItems: "center" }}>
      <div style={{
        width: 168, padding: "14px 16px 20px",
        background: "var(--bg-soft)", border: "1.5px solid var(--line-2)",
        borderRadius: "34px 34px 20px 20px", position: "relative",
      }}>
        {/* Windshield */}
        <div style={{ position: "absolute", top: -4, left: 14, right: 14, height: 12, background: "var(--surface)", borderRadius: "14px 14px 4px 4px", border: "1.5px solid var(--line-2)" }}/>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, marginTop: 6 }}>
          <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 4 }}>
            <div className="seat driver"/>
            <div style={{ fontSize: 9, color: "var(--ink-4)", fontWeight: 600 }}>DRIVER</div>
          </div>
          <SeatCell i={0} taken={taken} selected={selected} onToggle={onToggle}/>
          {cells.slice(1, capacity).map(i => <SeatCell key={i} i={i} taken={taken} selected={selected} onToggle={onToggle}/>)}
        </div>
      </div>
    </div>
  );
}
function SeatCell({ i, taken, selected, onToggle }) {
  const isTaken = taken.includes(i);
  const isSel = selected.includes(i);
  return (
    <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 4 }} onClick={() => !isTaken && onToggle && onToggle(i)}>
      <div className={`seat ${isTaken ? "filled" : isSel ? "selected" : ""}`}/>
      <div style={{ fontSize: 9, color: "var(--ink-4)", fontWeight: 600 }}>
        {isTaken ? "TAKEN" : isSel ? "YOU" : "FREE"}
      </div>
    </div>
  );
}

Object.assign(window, { Icon, Avatar, MatchRing, MapBackdrop, PinDot, SeatPlan });
