// screens-variants.jsx — Variation B for hero screens (Home, Results, Ride Detail)
// Variant direction: Card-stack / editorial — less map, more content; warm cream base with layered cards

// ═══════════════════════════════════════════════════════════════════
// HOME · Variant B — "commuter dashboard" (less map, more rituals)
// ═══════════════════════════════════════════════════════════════════
function HomeScreenB() {
  return (
    <Phone label="06b Home · Dashboard">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)", overflow: "auto" }} className="rs-scroll">
        <div style={{ padding: "14px 20px 20px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <Avatar name="Nimali P" size={40}/>
            <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", position: "relative" }}>
              <Icon name="bell" size={18}/>
              <div style={{ position: "absolute", top: 8, right: 10, width: 8, height: 8, borderRadius: 4, background: "var(--accent)", border: "2px solid var(--bg-soft)" }}/>
            </button>
          </div>
          <div className="rs-display" style={{ fontSize: 28, marginTop: 14, lineHeight: 1.1 }}>
            Good morning,<br/><span style={{ color: "var(--accent-2)" }}>Nimali.</span>
          </div>
          <div style={{ fontSize: 13, color: "var(--ink-3)", marginTop: 4 }}>Three drivers are heading your way to the city right now.</div>
        </div>

        <div style={{ padding: "0 20px" }}>
          <div style={{ padding: 18, borderRadius: 20, background: "var(--ink)", color: "var(--bg)", position: "relative", overflow: "hidden" }}>
            <div style={{ position: "absolute", right: -30, top: -30, width: 140, height: 140, borderRadius: 70, background: "var(--accent)", opacity: .6 }}/>
            <div style={{ position: "relative" }}>
              <div style={{ fontSize: 11, letterSpacing: ".12em", fontWeight: 700, opacity: .75 }}>YOUR MORNING COMMUTE</div>
              <div className="rs-display" style={{ fontSize: 22, marginTop: 4, lineHeight: 1.1 }}>Rajagiriya → World Trade Center</div>
              <div style={{ marginTop: 14, display: "flex", gap: 14, alignItems: "center" }}>
                <div><div style={{ fontSize: 10, opacity: .7 }}>NEXT RIDE</div><div style={{ fontWeight: 700 }}>8:04 AM</div></div>
                <div><div style={{ fontSize: 10, opacity: .7 }}>MATCHES</div><div style={{ fontWeight: 700 }}>4 drivers</div></div>
                <div><div style={{ fontSize: 10, opacity: .7 }}>FROM</div><div style={{ fontWeight: 700 }}>LKR 225</div></div>
              </div>
              <button style={{ marginTop: 14, padding: "10px 14px", background: "#fff", color: "var(--ink)", fontSize: 13, fontWeight: 700, borderRadius: 999, display: "inline-flex", alignItems: "center", gap: 6 }}>
                See rides <Icon name="arrow" size={14} color="var(--ink)"/>
              </button>
            </div>
          </div>
        </div>

        <div style={{ padding: "18px 20px 8px", display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
          <div className="rs-section-label">WHERE TO?</div>
          <span style={{ fontSize: 12, color: "var(--accent-2)", fontWeight: 700 }}>Search map ›</span>
        </div>
        <div style={{ padding: "0 20px" }}>
          <button style={{ width: "100%", height: 60, padding: "0 16px", display: "flex", alignItems: "center", gap: 12, background: "var(--surface)", border: "1.5px solid var(--line)", borderRadius: 18 }}>
            <Icon name="search" size={20} color="var(--ink-3)"/>
            <span style={{ color: "var(--ink-3)", fontSize: 15, fontWeight: 500 }}>Where are you going?</span>
          </button>
          <div style={{ marginTop: 10, display: "flex", gap: 10 }}>
            <QuickChip icon="home" label="Home" sub="2 km"/>
            <QuickChip icon="briefcase" label="Office" sub="WTC"/>
            <div style={{ width: 52, display: "flex", alignItems: "center", justifyContent: "center", borderRadius: 14, border: "1.5px dashed var(--line-2)" }}>
              <Icon name="plus" size={18} color="var(--ink-3)"/>
            </div>
          </div>
        </div>

        <div style={{ padding: "20px 20px 6px" }} className="rs-section-label">FRIDAY'S RIDES HEADING YOUR WAY</div>
        <div style={{ overflow: "auto", paddingBottom: 20 }} className="rs-scroll">
          <div style={{ display: "flex", gap: 12, padding: "6px 20px 4px" }}>
            {MOCK_RIDES.slice(0, 3).map(r => (
              <div key={r.id} style={{ width: 220, flexShrink: 0, background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 18, padding: 14 }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                  <MatchRing value={r.match} size={46}/>
                  <div style={{ textAlign: "right" }}>
                    <div className="rs-display tab" style={{ fontSize: 20 }}>LKR {r.price}</div>
                    <div style={{ fontSize: 10, color: "var(--ink-4)", fontWeight: 700 }}>{r.depart}</div>
                  </div>
                </div>
                <div style={{ marginTop: 10, fontSize: 13, fontWeight: 700, lineHeight: 1.2 }}>{r.overlap.split(" · ")[0]}</div>
                <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 2 }}>{r.dist} km · {r.seats} seats</div>
                <div style={{ marginTop: 12, display: "flex", alignItems: "center", gap: 8 }}>
                  <Avatar name={r.driver} size={24}/>
                  <div style={{ fontSize: 12, fontWeight: 600 }}>{r.driver.split(" ")[0]}</div>
                  <div style={{ fontSize: 11, color: "var(--ink-3)", marginLeft: "auto", display: "inline-flex", alignItems: "center", gap: 3 }}>
                    <Icon name="star" size={10} color="var(--warn)"/> {r.rating}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// RESULTS · Variant B — split timeline view (by route overlap tier)
// ═══════════════════════════════════════════════════════════════════
function ResultsListScreenB() {
  return (
    <Phone label="08b Results · Grouped">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "14px 20px 10px", background: "var(--ink)", color: "var(--bg)", borderBottomLeftRadius: 24, borderBottomRightRadius: 24 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <button style={{ width: 36, height: 36, borderRadius: 18, background: "rgba(255,255,255,.1)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="back" size={18} color="#fff"/>
            </button>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 10, letterSpacing: ".12em", opacity: .7, fontWeight: 700 }}>4 RIDES · NOW</div>
              <div style={{ fontSize: 15, fontWeight: 700 }}>Narahenpita → Bambalapitiya</div>
            </div>
            <div style={{ display: "flex", background: "rgba(255,255,255,.12)", padding: 3, borderRadius: 12, fontSize: 11, fontWeight: 700 }}>
              <div style={{ padding: "5px 10px", borderRadius: 10, background: "var(--bg)", color: "var(--ink)" }}>List</div>
              <div style={{ padding: "5px 10px", opacity: .7 }}>Map</div>
            </div>
          </div>
          <div style={{ marginTop: 12, display: "flex", gap: 6 }}>
            <div style={{ padding: "5px 10px", fontSize: 11, fontWeight: 700, borderRadius: 999, background: "var(--accent)" }}>50%+ match</div>
            <div style={{ padding: "5px 10px", fontSize: 11, fontWeight: 700, borderRadius: 999, background: "rgba(255,255,255,.12)" }}>Any time</div>
            <div style={{ padding: "5px 10px", fontSize: 11, fontWeight: 700, borderRadius: 999, background: "rgba(255,255,255,.12)" }}>4.5★+</div>
          </div>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px" }} className="rs-scroll">
          <SectionHead label="Full route" pct="100%" count={1} tint="var(--success-soft)" fg="var(--match-full)"/>
          <RideCompact r={MOCK_RIDES[0]}/>

          <SectionHead label="High overlap" pct="70–99%" count={2} tint="var(--teal-soft)" fg="var(--teal)"/>
          <RideCompact r={MOCK_RIDES[1]}/>
          <RideCompact r={MOCK_RIDES[2]}/>

          <SectionHead label="Partial — cheaper" pct="50–69%" count={1} tint="var(--accent-soft)" fg="var(--accent-2)"/>
          <RideCompact r={MOCK_RIDES[3]}/>
        </div>
      </div>
    </Phone>
  );
}

function SectionHead({ label, pct, count, tint, fg }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 10, margin: "12px 4px 10px", paddingTop: 4 }}>
      <div style={{ padding: "4px 10px", borderRadius: 999, background: tint, color: fg, fontSize: 11, fontWeight: 700 }}>{pct}</div>
      <div style={{ fontSize: 13, fontWeight: 700 }}>{label}</div>
      <div style={{ fontSize: 11, color: "var(--ink-4)" }}>· {count} ride{count > 1 ? "s" : ""}</div>
      <div style={{ flex: 1, height: 1, background: "var(--line)" }}/>
    </div>
  );
}

function RideCompact({ r }) {
  return (
    <div style={{ padding: 12, background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 16, display: "flex", gap: 12, alignItems: "center", marginBottom: 10 }}>
      <Avatar name={r.driver} size={44}/>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 13, fontWeight: 700 }}>{r.driver} <span style={{ fontSize: 11, color: "var(--ink-3)", fontWeight: 500 }}>· {r.depart}</span></div>
        <div style={{ fontSize: 11, color: "var(--ink-3)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{r.overlap}</div>
        <div style={{ marginTop: 6, display: "flex", gap: 8, fontSize: 10, fontWeight: 700, color: "var(--ink-3)" }}>
          <span><Icon name="star" size={10} color="var(--warn)"/> {r.rating}</span>
          <span>·</span>
          <span><Icon name="users" size={10}/> {r.seats}</span>
          <span>·</span>
          <span>{r.dist} km</span>
        </div>
      </div>
      <div style={{ textAlign: "right" }}>
        <div className="rs-display tab" style={{ fontSize: 18 }}>LKR {r.price}</div>
        <div style={{ fontSize: 10, color: "var(--match-full)", fontWeight: 700 }}>{r.match}% match</div>
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════
// RIDE DETAIL · Variant B — Inline route spine, bolder price
// ═══════════════════════════════════════════════════════════════════
function RideDetailScreenB() {
  const r = MOCK_RIDES[0];
  return (
    <Phone label="10b Ride Detail · Editorial">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)", overflow: "auto" }} className="rs-scroll">
        <div style={{ padding: "14px 20px 20px", background: "var(--bg-soft)" }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "#fff", display: "inline-flex", alignItems: "center", justifyContent: "center", boxShadow: "var(--shadow-sm)" }}>
            <Icon name="back" size={20}/>
          </button>

          <div style={{ marginTop: 18, display: "flex", alignItems: "flex-end", justifyContent: "space-between" }}>
            <div>
              <div style={{ fontSize: 11, color: "var(--accent-2)", fontWeight: 700, letterSpacing: ".1em" }}>100% MATCH · BEST TODAY</div>
              <div className="rs-display" style={{ fontSize: 44, lineHeight: 1, letterSpacing: "-0.03em", marginTop: 4 }}>LKR 292</div>
              <div style={{ fontSize: 12, color: "var(--ink-3)", marginTop: 2 }}>6.2 km · full route · 18 min</div>
            </div>
            <MatchRing value={100} size={68}/>
          </div>
        </div>

        <div style={{ padding: "20px 20px 6px" }} className="rs-section-label">YOUR ROUTE INSIDE SAMAN'S TRIP</div>
        <div style={{ padding: "8px 20px 20px" }}>
          <div style={{ padding: 16, background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 18 }}>
            <RoutePoint label="Rajagiriya" sub="Driver starts · 8:00 AM" type="start-driver"/>
            <RoutePoint label="Narahenpita Jn" sub="You board · 8:04 AM" type="pickup" highlight/>
            <RoutePoint label="Thunmulla" sub="8:12 AM"/>
            <RoutePoint label="Bambalapitiya" sub="You exit · 8:22 AM" type="drop" highlight/>
            <RoutePoint label="Colombo Fort" sub="Driver continues · 8:34 AM" type="end-driver" last/>
          </div>
        </div>

        <div style={{ padding: "0 20px" }}>
          <div className="rs-card" style={{ padding: 14, display: "flex", alignItems: "center", gap: 12 }}>
            <Avatar name={r.driver} size={48}/>
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 700, fontSize: 15 }}>{r.driver}</div>
              <div style={{ fontSize: 12, color: "var(--ink-3)" }}>
                <Icon name="star" size={11} color="var(--warn)"/> {r.rating} · {r.trips} trips · Hybrid car
              </div>
            </div>
            <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="chev" size={18}/>
            </button>
          </div>
        </div>

        <div style={{ padding: "20px 20px 6px" }} className="rs-section-label">WHY THIS IS A GOOD MATCH</div>
        <div style={{ padding: "0 20px 20px", display: "flex", flexDirection: "column", gap: 8 }}>
          <Why icon="check" title="Full overlap" sub="Driver's route covers 100% of your trip"/>
          <Why icon="clock" title="No detour for you" sub="Pick up within 50 m of your location"/>
          <Why icon="thumb" title="Safer choice" sub="4.9★ across 312 completed trips"/>
        </div>

        <div style={{ padding: "0 20px 120px" }}>
          <div className="rs-section-label" style={{ marginBottom: 10 }}>FARE</div>
          <FareRow label="Your 6.2 km × LKR 50" val="310"/>
          <FareRow label="Match discount · 100%" val="−45" pos/>
          <FareRow label="Platform fee" val="27" muted/>
          <div className="rs-divider" style={{ margin: "8px 0" }}/>
          <FareRow label="Total" val="292" strong/>
        </div>

        <div style={{ padding: "12px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)", position: "absolute", bottom: 0, left: 0, right: 0 }}>
          <button className="rs-btn accent full">Book 1 seat · LKR 292</button>
        </div>
      </div>
    </Phone>
  );
}

function RoutePoint({ label, sub, type, highlight, last }) {
  const colors = {
    "start-driver": "var(--ink-4)",
    "pickup": "var(--teal)",
    "drop": "var(--accent)",
    "end-driver": "var(--ink-4)",
  };
  const c = colors[type] || "var(--ink-3)";
  const outline = type === "start-driver" || type === "end-driver";
  return (
    <div style={{ display: "flex", gap: 12, alignItems: "flex-start", paddingBottom: last ? 0 : 12, position: "relative" }}>
      <div style={{ width: 14, display: "flex", flexDirection: "column", alignItems: "center", flexShrink: 0 }}>
        <div style={{
          width: highlight ? 14 : 10,
          height: highlight ? 14 : 10,
          borderRadius: "50%",
          background: outline ? "transparent" : c,
          border: `2px solid ${c}`,
          boxShadow: highlight ? `0 0 0 4px ${type === "pickup" ? "var(--teal-soft)" : "var(--accent-soft)"}` : "none",
        }}/>
        {!last && <div style={{ width: 2, flex: 1, minHeight: 18, marginTop: 3, backgroundImage: outline ? "linear-gradient(var(--line-2) 60%, transparent 60%)" : "linear-gradient(var(--line-2) 100%, transparent 0)", backgroundSize: "2px 4px" }}/>}
      </div>
      <div style={{ flex: 1, paddingTop: -2 }}>
        <div style={{ fontSize: 14, fontWeight: highlight ? 700 : 600, color: highlight ? "var(--ink)" : outline ? "var(--ink-3)" : "var(--ink)" }}>{label}</div>
        <div style={{ fontSize: 11, color: "var(--ink-3)" }}>{sub}</div>
      </div>
    </div>
  );
}

function Why({ icon, title, sub }) {
  return (
    <div className="rs-card" style={{ padding: 12, display: "flex", alignItems: "center", gap: 12 }}>
      <div style={{ width: 36, height: 36, borderRadius: 18, background: "var(--success-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
        <Icon name={icon} size={16} color="var(--match-full)" strokeWidth={2.4}/>
      </div>
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 13, fontWeight: 700 }}>{title}</div>
        <div style={{ fontSize: 11, color: "var(--ink-3)" }}>{sub}</div>
      </div>
    </div>
  );
}

Object.assign(window, { HomeScreenB, ResultsListScreenB, RideDetailScreenB });
