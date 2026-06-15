// driver-home.jsx — home dashboard, trip list, recurring schedule

// Mock driver trips
const DR_TRIPS = [
  { id: "t1", time: "8:00 AM", date: "Today", from: "Rajagiriya", to: "Colombo Fort", booked: 2, seats: 3, fare: 540, status: "live", pax: 2 },
  { id: "t2", time: "5:30 PM", date: "Today", from: "Colombo Fort", to: "Rajagiriya", booked: 1, seats: 3, fare: 540, status: "scheduled", pax: 1 },
  { id: "t3", time: "8:00 AM", date: "Tomorrow", from: "Rajagiriya", to: "Colombo Fort", booked: 3, seats: 3, fare: 540, status: "published", pax: 3 },
  { id: "t4", time: "5:30 PM", date: "Tomorrow", from: "Colombo Fort", to: "Rajagiriya", booked: 0, seats: 3, fare: 540, status: "published", pax: 0 },
  { id: "t5", time: "8:00 AM", date: "Yesterday", from: "Rajagiriya", to: "Colombo Fort", booked: 2, seats: 3, fare: 540, status: "completed", earned: 1080 },
  { id: "t6", time: "5:30 PM", date: "Yesterday", from: "Colombo Fort", to: "Rajagiriya", booked: 3, seats: 3, fare: 540, status: "completed", earned: 1620 },
];

// ═══════════════════════════════════════════════════════════════════
// HOME DASHBOARD
// ═══════════════════════════════════════════════════════════════════
function DrHomeScreen({ online = true, showEarnings = true }) {
  const next = DR_TRIPS[0];
  return (
    <Phone label="D10 Home · Dashboard">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)", overflow: "auto" }} className="rs-scroll">
        {/* Header */}
        <div style={{ padding: "14px 20px 18px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
              <Avatar name="Saman W" size={38}/>
              <div>
                <div style={{ fontSize: 11, color: "var(--ink-3)", fontWeight: 600 }}>Good morning</div>
                <div style={{ fontSize: 15, fontWeight: 700 }}>Saman</div>
              </div>
            </div>
            <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
              <StatusPill online={online}/>
              <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", position: "relative" }}>
                <Icon name="bell" size={18}/>
                <div style={{ position: "absolute", top: 8, right: 10, width: 8, height: 8, borderRadius: 4, background: "var(--accent)", border: "2px solid var(--bg)" }}/>
              </button>
            </div>
          </div>
        </div>

        {/* Earnings hero */}
        <div style={{ padding: "0 20px" }}>
          <div style={{ padding: 18, borderRadius: 20, background: "var(--ink)", color: "#fff", position: "relative", overflow: "hidden" }}>
            <div style={{ position: "absolute", right: -40, top: -60, width: 200, height: 200, borderRadius: 100, background: "var(--accent)", opacity: .35 }}/>
            <div style={{ position: "relative" }}>
              <div style={{ fontSize: 11, letterSpacing: ".12em", fontWeight: 700, opacity: .7 }}>TODAY'S EARNINGS</div>
              <div className="rs-display tab" style={{ fontSize: 44, marginTop: 6, fontWeight: 600, lineHeight: 1 }}>
                {showEarnings ? "LKR 1,080" : "LKR ••••"}
              </div>
              <div style={{ fontSize: 12, opacity: .75, marginTop: 4 }}>From 2 completed trips · LKR 7,420 this week</div>
              <div style={{ marginTop: 16, display: "flex", gap: 10 }}>
                <EarningsTile label="THIS WEEK" value={showEarnings ? "7,420" : "••••"} sub="12 trips" dark/>
                <EarningsTile label="RATING" value="4.92" sub="312 trips" dark/>
                <EarningsTile label="ACCEPT" value="98%" sub="last 30 days" dark/>
              </div>
            </div>
          </div>
        </div>

        {/* Next trip live card */}
        <div style={{ padding: "20px 20px 6px" }} className="rs-section-label">YOUR NEXT TRIP</div>
        <div style={{ padding: "0 20px" }}>
          <div className="rs-card" style={{ padding: 16, border: "2px solid var(--accent)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 12 }}>
              <div style={{ display: "inline-flex", gap: 6, alignItems: "center", height: 24, padding: "0 10px", borderRadius: 999, background: "var(--accent)", color: "#fff", fontSize: 10, fontWeight: 700, letterSpacing: ".08em" }}>
                <div style={{ width: 6, height: 6, borderRadius: 3, background: "#fff", animation: "pulse 1.4s infinite" }}/>
                LIVE NOW
              </div>
              <div style={{ fontSize: 13, fontWeight: 700 }}>{next.time}</div>
              <div style={{ marginLeft: "auto", fontSize: 12, color: "var(--ink-3)" }}>{next.pax} / {next.seats} booked</div>
            </div>
            <div style={{ display: "flex", gap: 12, alignItems: "center" }}>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 15, fontWeight: 700, display: "flex", alignItems: "center", gap: 6 }}>
                  {next.from} <Icon name="arrow" size={12} color="var(--ink-3)"/> {next.to}
                </div>
                <div style={{ fontSize: 12, color: "var(--ink-3)", marginTop: 2 }}>11.4 km · est. LKR {next.fare} total</div>
              </div>
              <button className="rs-btn accent" style={{ height: 44, padding: "0 18px", fontSize: 13 }}>Start trip</button>
            </div>
          </div>
        </div>

        {/* Today timeline */}
        <div style={{ padding: "20px 20px 6px", display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
          <div className="rs-section-label">TODAY · WED 24 APR</div>
          <span style={{ fontSize: 12, color: "var(--accent-2)", fontWeight: 700 }}>See week ›</span>
        </div>
        <div style={{ padding: "0 20px 24px", display: "flex", flexDirection: "column", gap: 10 }}>
          {DR_TRIPS.filter(t => t.date === "Today").map(t => <DrTripRow key={t.id} t={t}/>)}
        </div>

        {/* Quick actions */}
        <div style={{ padding: "0 20px 20px", display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
          <QuickAction icon="plus" label="Publish a trip" sub="One-time or recurring" accent/>
          <QuickAction icon="receipt" label="Earnings" sub="LKR 7,420 this week"/>
          <QuickAction icon="users" label="Booking requests" sub="2 waiting" badge={2}/>
          <QuickAction icon="calendar" label="Schedule" sub="5 trips this week"/>
        </div>
      </div>
    </Phone>
  );
}

function QuickAction({ icon, label, sub, accent, badge }) {
  return (
    <div style={{ padding: 14, borderRadius: 16, background: accent ? "var(--accent)" : "var(--surface)", color: accent ? "#fff" : "var(--ink)", border: accent ? "none" : "1px solid var(--line)", position: "relative" }}>
      <div style={{ width: 36, height: 36, borderRadius: 12, background: accent ? "rgba(255,255,255,.2)" : "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
        <Icon name={icon} size={18} color={accent ? "#fff" : "var(--ink)"} strokeWidth={2}/>
      </div>
      <div style={{ marginTop: 10, fontSize: 14, fontWeight: 700 }}>{label}</div>
      <div style={{ fontSize: 11, opacity: accent ? .85 : .65, marginTop: 1 }}>{sub}</div>
      {badge && <div style={{ position: "absolute", top: 14, right: 14, height: 22, minWidth: 22, padding: "0 6px", borderRadius: 11, background: "var(--accent)", color: "#fff", fontSize: 11, fontWeight: 700, display: "inline-flex", alignItems: "center", justifyContent: "center" }}>{badge}</div>}
    </div>
  );
}

function DrTripRow({ t }) {
  const statusMeta = {
    live: { chip: "LIVE", bg: "var(--accent)", fg: "#fff" },
    scheduled: { chip: "SCHEDULED", bg: "var(--teal-soft)", fg: "var(--teal)" },
    published: { chip: "PUBLISHED", bg: "var(--bg-soft)", fg: "var(--ink-2)" },
    completed: { chip: "DONE", bg: "var(--success-soft)", fg: "var(--match-full)" },
  };
  const s = statusMeta[t.status];
  return (
    <div className="rs-card" style={{ padding: 14, display: "flex", gap: 14, alignItems: "center" }}>
      <div style={{ width: 52, textAlign: "center", flexShrink: 0 }}>
        <div className="rs-display tab" style={{ fontSize: 16, fontWeight: 600 }}>{t.time.split(" ")[0]}</div>
        <div style={{ fontSize: 9, color: "var(--ink-3)", fontWeight: 700, letterSpacing: ".06em" }}>{t.time.split(" ")[1]}</div>
      </div>
      <div style={{ width: 1, alignSelf: "stretch", background: "var(--line)" }}/>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 13, fontWeight: 700, display: "flex", alignItems: "center", gap: 6, whiteSpace: "nowrap", overflow: "hidden" }}>
          {t.from} <Icon name="arrow" size={11} color="var(--ink-3)"/> {t.to}
        </div>
        <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 2, display: "flex", alignItems: "center", gap: 8 }}>
          <span><Icon name="users" size={11}/> {t.pax || 0}/{t.seats}</span>
          {t.status === "completed" && <span style={{ color: "var(--match-full)", fontWeight: 700 }}>· LKR {t.earned}</span>}
        </div>
      </div>
      <div style={{ height: 22, padding: "0 8px", display: "inline-flex", alignItems: "center", borderRadius: 999, background: s.bg, color: s.fg, fontSize: 10, fontWeight: 700, letterSpacing: ".04em", flexShrink: 0 }}>
        {s.chip}
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════
// TRIP LIST
// ═══════════════════════════════════════════════════════════════════
function DrTripListScreen() {
  return (
    <Phone label="D11 Trip List">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "14px 20px", display: "flex", alignItems: "center", gap: 12, background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="back" size={20}/>
          </button>
          <div style={{ flex: 1, fontSize: 17, fontWeight: 700 }}>Your trips</div>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--accent)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="plus" size={20} color="#fff" strokeWidth={2.4}/>
          </button>
        </div>

        <div style={{ padding: "10px 20px 0", display: "flex", gap: 4, background: "var(--bg-soft)", margin: "12px 20px 0", borderRadius: 14, padding: 4 }}>
          <div style={{ flex: 1, height: 36, borderRadius: 11, background: "var(--surface)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 13, fontWeight: 700, boxShadow: "var(--shadow-sm)" }}>
            Upcoming <span style={{ marginLeft: 6, fontSize: 10, padding: "1px 6px", background: "var(--accent)", color: "#fff", borderRadius: 999 }}>4</span>
          </div>
          <div style={{ flex: 1, height: 36, borderRadius: 11, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 13, fontWeight: 600, color: "var(--ink-3)" }}>Live</div>
          <div style={{ flex: 1, height: 36, borderRadius: 11, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 13, fontWeight: 600, color: "var(--ink-3)" }}>Past</div>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "16px 20px" }} className="rs-scroll">
          <div className="rs-section-label" style={{ marginBottom: 10 }}>TODAY · WED 24 APR</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 10, marginBottom: 18 }}>
            {DR_TRIPS.filter(t => t.date === "Today").map(t => <DrTripRow key={t.id} t={t}/>)}
          </div>
          <div className="rs-section-label" style={{ marginBottom: 10 }}>TOMORROW · THU 25 APR</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            {DR_TRIPS.filter(t => t.date === "Tomorrow").map(t => <DrTripRow key={t.id} t={t}/>)}
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// RECURRING SCHEDULE — week calendar
// ═══════════════════════════════════════════════════════════════════
function DrScheduleScreen() {
  const days = ["Mon", "Tue", "Wed", "Thu", "Fri"];
  const dates = ["22", "23", "24", "25", "26"];
  return (
    <Phone label="D12 Recurring Schedule">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "14px 20px", display: "flex", alignItems: "center", gap: 12, background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="back" size={20}/>
          </button>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 11, fontWeight: 700, color: "var(--ink-3)", letterSpacing: ".1em" }}>WEEK OF</div>
            <div style={{ fontSize: 15, fontWeight: 700 }}>22 – 28 April</div>
          </div>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="calendar" size={18}/>
          </button>
        </div>

        {/* Day strip */}
        <div style={{ padding: "14px 20px 10px", background: "var(--surface)", borderBottom: "1px solid var(--line)", display: "flex", gap: 6 }}>
          {["M","T","W","T","F","S","S"].map((d, i) => (
            <div key={i} style={{
              flex: 1, padding: "8px 0", borderRadius: 12, textAlign: "center",
              background: i === 2 ? "var(--ink)" : "transparent",
              color: i === 2 ? "var(--bg)" : "var(--ink-3)",
            }}>
              <div style={{ fontSize: 10, fontWeight: 700, opacity: i === 2 ? .65 : 1 }}>{d}</div>
              <div className="rs-display" style={{ fontSize: 17, fontWeight: 600, marginTop: 1 }}>{22 + i > 28 ? 22 + i - 28 : 22 + i}</div>
            </div>
          ))}
        </div>

        {/* Week earnings summary */}
        <div style={{ padding: "16px 20px 6px" }}>
          <div className="rs-card" style={{ padding: 14, background: "var(--teal-soft)", border: "none", display: "flex", justifyContent: "space-between" }}>
            <div>
              <div style={{ fontSize: 11, color: "var(--teal)", fontWeight: 700, letterSpacing: ".08em" }}>SCHEDULED THIS WEEK</div>
              <div className="rs-display" style={{ fontSize: 24, color: "var(--teal)" }}>10 trips</div>
            </div>
            <div style={{ textAlign: "right" }}>
              <div style={{ fontSize: 11, color: "var(--teal)", fontWeight: 700, letterSpacing: ".08em" }}>POTENTIAL EARNINGS</div>
              <div className="rs-display" style={{ fontSize: 24, color: "var(--teal)" }}>LKR 16,200</div>
            </div>
          </div>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "10px 20px 20px" }} className="rs-scroll">
          {days.map((d, i) => (
            <div key={d} style={{ marginBottom: 16 }}>
              <div style={{ display: "flex", alignItems: "center", gap: 10, margin: "8px 0 10px" }}>
                <div className="rs-display" style={{ fontSize: 16, fontWeight: 600 }}>{d}</div>
                <div style={{ fontSize: 12, color: "var(--ink-3)" }}>{dates[i]} April</div>
                <div style={{ flex: 1, height: 1, background: "var(--line)" }}/>
              </div>
              <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                <CalEvent time="8:00 AM" route="Rajagiriya → Fort" booked={i === 2 ? 2 : i === 3 ? 3 : 1}/>
                <CalEvent time="5:30 PM" route="Fort → Rajagiriya" booked={i === 2 ? 1 : 0}/>
              </div>
            </div>
          ))}
        </div>
      </div>
    </Phone>
  );
}

function CalEvent({ time, route, booked }) {
  const seats = 3;
  return (
    <div style={{ padding: 12, background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 12, display: "flex", alignItems: "center", gap: 12 }}>
      <div style={{ width: 4, alignSelf: "stretch", background: "var(--accent)", borderRadius: 2 }}/>
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 13, fontWeight: 700 }}>{time}</div>
        <div style={{ fontSize: 11, color: "var(--ink-3)" }}>{route}</div>
      </div>
      <div style={{ display: "flex", gap: 2 }}>
        {Array.from({ length: seats }, (_, i) => (
          <div key={i} style={{ width: 8, height: 14, borderRadius: 2, background: i < booked ? "var(--accent)" : "var(--line-2)" }}/>
        ))}
      </div>
      <div style={{ fontSize: 11, fontWeight: 700, color: "var(--ink-3)", minWidth: 30, textAlign: "right" }}>{booked}/{seats}</div>
    </div>
  );
}

Object.assign(window, { DrHomeScreen, DrTripListScreen, DrScheduleScreen, DrTripRow, DR_TRIPS });
