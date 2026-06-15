// driver-variants.jsx — Variant B for hero screens (Home, Create-trip route, Live trip)

// ═══════════════════════════════════════════════════════════════════
// HOME · Variant B — "Status board" with go-online focus
// ═══════════════════════════════════════════════════════════════════
function DrHomeScreenB({ online = true, showEarnings = true }) {
  return (
    <Phone label="D10b Home · Status board">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: online ? "var(--bg)" : "var(--bg-soft)", overflow: "auto" }} className="rs-scroll">
        <div style={{ padding: "14px 20px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
              <Avatar name="Saman W" size={36}/>
              <div style={{ fontSize: 14, fontWeight: 700 }}>Saman</div>
            </div>
            <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--surface)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="menu" size={18}/>
            </button>
          </div>
        </div>

        {/* Big online toggle */}
        <div style={{ padding: "0 20px 20px" }}>
          <div style={{
            padding: 22, borderRadius: 24,
            background: online ? "var(--ink)" : "var(--surface)",
            color: online ? "#fff" : "var(--ink)",
            border: online ? "none" : "1.5px solid var(--line)",
            position: "relative", overflow: "hidden",
          }}>
            {online && <div style={{ position: "absolute", right: -50, top: -50, width: 220, height: 220, borderRadius: 110, background: "var(--accent)", opacity: .35 }}/>}
            <div style={{ position: "relative" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <div>
                  <div style={{ fontSize: 11, opacity: .7, fontWeight: 700, letterSpacing: ".14em" }}>YOU'RE</div>
                  <div className="rs-display" style={{ fontSize: 36, lineHeight: 1, marginTop: 4, color: online ? "var(--accent)" : "var(--ink-3)" }}>{online ? "ONLINE" : "OFFLINE"}</div>
                </div>
                {/* Toggle pill */}
                <div style={{ width: 64, height: 36, borderRadius: 18, background: online ? "var(--accent)" : "var(--line-2)", padding: 4, display: "flex", justifyContent: online ? "flex-end" : "flex-start" }}>
                  <div style={{ width: 28, height: 28, borderRadius: 14, background: "#fff", boxShadow: "0 2px 4px rgba(0,0,0,.2)" }}/>
                </div>
              </div>
              <div style={{ marginTop: 14, fontSize: 13, opacity: .85, lineHeight: 1.4 }}>
                {online ? "Showing your published trips to matching passengers." : "Tap the toggle to start accepting bookings."}
              </div>
            </div>
          </div>
        </div>

        {online && (
          <>
            {/* Today snapshot */}
            <div style={{ padding: "0 20px 16px", display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 8 }}>
              <SnapTile label="TODAY" value={showEarnings ? "LKR 1,080" : "LKR ••••"} sub="2 trips done"/>
              <SnapTile label="UPCOMING" value="4" sub="trips today"/>
              <SnapTile label="REQUESTS" value="2" sub="awaiting" badge accent/>
            </div>

            {/* Action card */}
            <div style={{ padding: "0 20px 16px" }}>
              <div className="rs-card" style={{ padding: 16, display: "flex", alignItems: "center", gap: 12, background: "var(--accent)", color: "#fff", border: "none" }}>
                <div style={{ width: 48, height: 48, borderRadius: 14, background: "rgba(255,255,255,.22)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                  <Icon name="plus" size={26} color="#fff" strokeWidth={2.4}/>
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 15, fontWeight: 700 }}>Publish a trip</div>
                  <div style={{ fontSize: 12, opacity: .85 }}>Set your route, schedule, and seats</div>
                </div>
                <Icon name="chev" size={20} color="#fff"/>
              </div>
            </div>

            <div className="rs-section-label" style={{ padding: "8px 20px" }}>UP NEXT</div>
            <div style={{ padding: "0 20px 16px", display: "flex", flexDirection: "column", gap: 10 }}>
              {DR_TRIPS.slice(0, 3).map(t => <DrTripRow key={t.id} t={t}/>)}
            </div>

            <div className="rs-section-label" style={{ padding: "8px 20px" }}>QUICK STATS</div>
            <div style={{ padding: "0 20px 20px" }}>
              <div className="rs-card" style={{ padding: 14, display: "flex", justifyContent: "space-between", textAlign: "center" }}>
                <Stat label="Rating" value="4.92" icon="star"/>
                <Stat label="Accept" value="98%" icon="thumb"/>
                <Stat label="Match" value="84%" icon="route"/>
              </div>
            </div>
          </>
        )}
      </div>
    </Phone>
  );
}

function SnapTile({ label, value, sub, badge, accent }) {
  return (
    <div style={{ padding: 14, borderRadius: 16, background: "var(--surface)", border: "1px solid var(--line)", position: "relative" }}>
      <div style={{ fontSize: 9, color: "var(--ink-3)", fontWeight: 700, letterSpacing: ".08em" }}>{label}</div>
      <div className="rs-display tab" style={{ fontSize: 18, fontWeight: 600, marginTop: 2, color: accent ? "var(--accent-2)" : "var(--ink)", lineHeight: 1.1 }}>{value}</div>
      <div style={{ fontSize: 10, color: "var(--ink-3)", marginTop: 1 }}>{sub}</div>
      {badge && <div style={{ position: "absolute", top: 10, right: 10, width: 8, height: 8, borderRadius: 4, background: "var(--accent)" }}/>}
    </div>
  );
}

function Stat({ label, value, icon }) {
  return (
    <div style={{ flex: 1 }}>
      <div style={{ width: 32, height: 32, borderRadius: 16, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", margin: "0 auto" }}>
        <Icon name={icon} size={16}/>
      </div>
      <div className="rs-display" style={{ fontSize: 18, fontWeight: 600, marginTop: 6 }}>{value}</div>
      <div style={{ fontSize: 10, color: "var(--ink-3)", fontWeight: 700, letterSpacing: ".06em" }}>{label.toUpperCase()}</div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════
// CREATE TRIP STEP 1 · Variant B — Form-style (no map dominant)
// ═══════════════════════════════════════════════════════════════════
function DrCreate1ScreenB() {
  return (
    <Phone label="D13b Create · Form-first">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "12px 20px 14px", background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 14 }}>
            <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="close" size={20}/>
            </button>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 11, fontWeight: 700, color: "var(--ink-3)", letterSpacing: ".12em" }}>STEP 1 OF 3</div>
              <div style={{ fontSize: 17, fontWeight: 700 }}>Your route</div>
            </div>
          </div>
          <Stepper step={1} total={3}/>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "18px 20px" }} className="rs-scroll">
          <div className="rs-section-label" style={{ marginBottom: 10 }}>WHERE FROM, WHERE TO</div>
          <div style={{ padding: 14, background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 16, position: "relative" }}>
            <div style={{ position: "absolute", left: 26, top: 30, bottom: 30, width: 2, background: "var(--line-2)", backgroundImage: "linear-gradient(var(--line-2) 60%, transparent 60%)", backgroundSize: "2px 4px" }}/>
            {/* FROM */}
            <div style={{ padding: "10px 0 10px 28px", position: "relative", borderBottom: "1px solid var(--line)" }}>
              <div style={{ position: "absolute", left: 20, top: "50%", transform: "translateY(-50%)", width: 14, height: 14, borderRadius: 7, background: "var(--teal)", border: "2px solid #fff", boxShadow: "0 1px 4px rgba(0,0,0,.15)" }}/>
              <div style={{ fontSize: 10, color: "var(--ink-3)", fontWeight: 700, letterSpacing: ".08em" }}>FROM</div>
              <div style={{ fontSize: 15, fontWeight: 700, marginTop: 2 }}>Rajagiriya · Home</div>
            </div>
            {/* TO */}
            <div style={{ padding: "10px 0 10px 28px", position: "relative" }}>
              <div style={{ position: "absolute", left: 20, top: "50%", transform: "translateY(-50%)", width: 14, height: 14, background: "var(--accent)", border: "2px solid #fff", boxShadow: "0 1px 4px rgba(0,0,0,.15)" }}/>
              <div style={{ fontSize: 10, color: "var(--ink-3)", fontWeight: 700, letterSpacing: ".08em" }}>TO</div>
              <div style={{ fontSize: 15, fontWeight: 700, marginTop: 2 }}>Colombo Fort · World Trade Center</div>
            </div>
          </div>

          {/* Mini map preview */}
          <div style={{ marginTop: 14, height: 140, borderRadius: 16, overflow: "hidden", position: "relative" }}>
            <MapBackdrop/>
            <div style={{ position: "absolute", right: 10, top: 10, padding: "5px 10px", background: "rgba(0,0,0,.6)", color: "#fff", fontSize: 10, fontWeight: 700, borderRadius: 999, letterSpacing: ".06em" }}>11.4 km · 24 min</div>
            <button style={{ position: "absolute", bottom: 10, right: 10, height: 32, padding: "0 12px", borderRadius: 16, background: "#fff", fontSize: 11, fontWeight: 700, display: "inline-flex", alignItems: "center", gap: 5 }}>
              <Icon name="route" size={12}/> Edit on map
            </button>
          </div>

          <div className="rs-section-label" style={{ margin: "20px 0 10px" }}>WAYPOINTS · OPTIONAL</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            <div style={{ padding: "12px 14px", background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 12, display: "flex", alignItems: "center", gap: 10 }}>
              <div style={{ width: 10, height: 10, borderRadius: 5, background: "var(--ink-3)" }}/>
              <div style={{ flex: 1, fontSize: 13 }}>Narahenpita Jn</div>
              <Icon name="close" size={14} color="var(--ink-3)"/>
            </div>
            <button style={{ padding: "10px 14px", border: "1.5px dashed var(--line-2)", borderRadius: 12, display: "flex", alignItems: "center", gap: 8, fontSize: 13, fontWeight: 600, color: "var(--ink-2)", background: "transparent" }}>
              <Icon name="plus" size={14}/> Add stop along the way
            </button>
          </div>

          <div style={{ marginTop: 14, padding: 12, background: "var(--accent-soft)", borderRadius: 12, fontSize: 12, color: "var(--accent-2)", lineHeight: 1.4 }}>
            <b>Tip:</b> Adding common waypoints (junctions, stations) helps passengers find your trip in search.
          </div>
        </div>

        <div style={{ padding: "12px 20px 14px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">Continue to schedule</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// LIVE TRIP · Variant B — Passenger list dominant
// ═══════════════════════════════════════════════════════════════════
function DrLiveTripScreenB() {
  return (
    <Phone label="D20b Live · List view">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        {/* Map strip */}
        <div style={{ height: 180, position: "relative", flexShrink: 0 }}>
          <MapBackdrop/>
          <div style={{ position: "absolute", left: "30%", top: "40%" }}>
            <div style={{ width: 28, height: 28, borderRadius: 14, background: "var(--accent)", border: "3px solid #fff", display: "flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="car" size={14} color="#fff"/>
            </div>
          </div>
          {/* Top toolbar */}
          <div style={{ position: "absolute", top: 12, left: 12, right: 12, display: "flex", gap: 8 }}>
            <div style={{ height: 36, padding: "0 12px", background: "#fff", borderRadius: 18, display: "inline-flex", alignItems: "center", gap: 6, fontSize: 12, fontWeight: 700, boxShadow: "var(--shadow-md)" }}>
              <div style={{ width: 8, height: 8, borderRadius: 4, background: "var(--success)", animation: "pulse 1.4s infinite" }}/>
              ON TRIP · 11.4 km
            </div>
            <div style={{ flex: 1 }}/>
            <button style={{ width: 36, height: 36, borderRadius: 18, background: "var(--danger)", color: "#fff", display: "inline-flex", alignItems: "center", justifyContent: "center", boxShadow: "var(--shadow-md)" }}>
              <Icon name="alert" size={16} color="#fff"/>
            </button>
          </div>
          {/* Distance bottom-left */}
          <div style={{ position: "absolute", bottom: 12, left: 12, padding: "6px 12px", background: "rgba(0,0,0,.7)", color: "#fff", fontSize: 12, fontWeight: 700, borderRadius: 999 }}>
            Next pickup · 600 m
          </div>
        </div>

        {/* Trip progress */}
        <div style={{ padding: "16px 20px 10px", background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
            <div>
              <div style={{ fontSize: 10, color: "var(--ink-3)", fontWeight: 700, letterSpacing: ".08em" }}>EARNINGS SO FAR</div>
              <div className="rs-display tab" style={{ fontSize: 22, fontWeight: 600 }}>LKR 0</div>
            </div>
            <div style={{ textAlign: "right" }}>
              <div style={{ fontSize: 10, color: "var(--ink-3)", fontWeight: 700, letterSpacing: ".08em" }}>ETA TO FINAL</div>
              <div className="rs-display" style={{ fontSize: 18 }}>8:34 AM</div>
            </div>
          </div>
          <div style={{ marginTop: 10, height: 6, background: "var(--bg-soft)", borderRadius: 3, position: "relative" }}>
            <div style={{ position: "absolute", inset: 0, width: "8%", background: "var(--accent)", borderRadius: 3 }}/>
          </div>
        </div>

        {/* Passenger list */}
        <div style={{ flex: 1, overflow: "auto", padding: "14px 20px" }} className="rs-scroll">
          <div className="rs-section-label" style={{ marginBottom: 10 }}>PASSENGERS · 2 OF 3</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            <PaxRow
              name="Nimali P"
              pickup="Narahenpita Jn (next · 2 min)"
              drop="Bambalapitiya"
              fare={292}
              dist="6.2 km"
              status="pending"
              action={<button className="rs-btn accent" style={{ height: 36, padding: "0 12px", fontSize: 11 }}>Boarding</button>}
            />
            <PaxRow
              name="Kasun A"
              pickup="Thunmulla"
              drop="Colombo Fort"
              fare={420}
              dist="8.4 km"
              status="confirmed"
              action={<button style={{ width: 36, height: 36, borderRadius: 18, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}><Icon name="phone" size={14}/></button>}
            />
            <div style={{ padding: 14, border: "1.5px dashed var(--line-2)", borderRadius: 14, background: "var(--bg-soft)", display: "flex", alignItems: "center", gap: 10 }}>
              <div style={{ width: 42, height: 42, borderRadius: 21, background: "var(--surface)", display: "inline-flex", alignItems: "center", justifyContent: "center", border: "1px dashed var(--line-2)" }}>
                <Icon name="plus" size={16} color="var(--ink-3)"/>
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 13, fontWeight: 700, color: "var(--ink-3)" }}>1 seat free</div>
                <div style={{ fontSize: 11, color: "var(--ink-4)" }}>Passengers can still join until 8:00 AM</div>
              </div>
            </div>
          </div>
        </div>

        <div style={{ padding: "12px 20px 14px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full" style={{ height: 52 }}>
            <Icon name="check" size={18} color="#fff" strokeWidth={3}/> I've arrived at Narahenpita
          </button>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, { DrHomeScreenB, DrCreate1ScreenB, DrLiveTripScreenB });
