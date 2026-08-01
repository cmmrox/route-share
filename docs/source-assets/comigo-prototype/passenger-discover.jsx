// passenger-discover.jsx — P01…P07: home, search, results (list / map / grouped),
// ride detail. Layout and IA preserved from the live RouteShare screens; changes
// are the rebrand, the mode chip, the tab bar and data-driven fares.

// FARE_POLICY and RIDES come from data.jsx — one source of truth for money.

// ── route timeline, shared with the driver screens ──
function RouteTimeline({ stops = [], compact }) {
  return (
    <div style={{ display: "flex", flexDirection: "column" }}>
      {stops.map((s, i) => (
        <div key={i} style={{ display: "flex", gap: 12 }}>
          <div style={{ width: 14, display: "flex", flexDirection: "column", alignItems: "center", flexShrink: 0 }}>
            <div style={{
              width: s.kind === "via" ? 8 : 13, height: s.kind === "via" ? 8 : 13, borderRadius: 999, marginTop: 4,
              background: s.kind === "pickup" ? "var(--teal)" : s.kind === "drop" ? "var(--accent-ink)" : "var(--line-2)",
              border: s.kind === "via" ? "none" : "2.5px solid var(--surface)",
              boxShadow: s.kind === "via" ? "none" : "0 0 0 1.5px currentColor",
              color: s.kind === "pickup" ? "var(--teal)" : "var(--accent-ink)",
              flexShrink: 0,
            }}/>
            {i < stops.length - 1 && <div style={{ flex: 1, width: 2, background: "var(--line-2)", marginTop: 3, minHeight: compact ? 16 : 22 }}/>}
          </div>
          <div style={{ flex: 1, minWidth: 0, paddingBottom: i < stops.length - 1 ? (compact ? 12 : 16) : 0 }}>
            <div style={{ display: "flex", alignItems: "baseline", gap: 8 }}>
              <div style={{ fontSize: s.kind === "via" ? 12.5 : 14, fontWeight: s.kind === "via" ? 600 : 700, flex: 1, minWidth: 0, color: s.kind === "via" ? "var(--ink-3)" : "var(--ink)" }}>{s.place}</div>
              {s.time && <div className="tab" style={{ fontSize: 12, fontWeight: 700, color: "var(--ink-3)", flexShrink: 0 }}>{s.time}</div>}
            </div>
            {s.note && <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>{s.note}</div>}
          </div>
        </div>
      ))}
    </div>
  );
}

// ── match badge: colour + number + words, never colour alone ──
function MatchBadge({ value, size = 46 }) {
  const t = matchTier(value);
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 9 }}>
      <MatchRing value={value} size={size} strokeWidth={3.6}/>
      <div style={{ minWidth: 0 }}>
        <div style={{ fontSize: 11.5, fontWeight: 800, color: t.ink, lineHeight: 1.2 }}>{t.label}</div>
        <div style={{ fontSize: 10.5, color: "var(--ink-3)" }}>of your trip</div>
      </div>
    </div>
  );
}

function RideCard({ r, showFare = true }) {
  const t = matchTier(r.match);
  return (
    <div className="rs-card" style={{ padding: 14 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
        <MatchRing value={r.match} size={48} strokeWidth={3.6}/>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
            <div style={{ fontSize: 14, fontWeight: 700, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{r.driver}</div>
            <Icon name="star" size={12} color="var(--status-pending-ink)"/>
            <span className="tab" style={{ fontSize: 12, fontWeight: 600 }}>{r.rating}</span>
            <span style={{ fontSize: 11, color: "var(--ink-3)", flexShrink: 0 }}>· {r.trips} trips</span>
          </div>
          <div style={{ fontSize: 11.5, fontWeight: 700, color: t.ink, marginTop: 3 }}>{t.label}</div>
          <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>{r.overlap}</div>
        </div>
        {showFare && (
          <div style={{ textAlign: "right", flexShrink: 0 }}>
            <div className="rs-display tab" style={{ fontSize: 19, fontWeight: 600 }}>{FARE_POLICY.currency} {money(r.price)}</div>
            <div className="tab" style={{ fontSize: 10.5, color: "var(--ink-3)", marginTop: 2 }}>{r.dist} km on route</div>
            {r.ratePerKm && <div className="tab" style={{ fontSize: 10.5, color: "var(--ink-3)", marginTop: 1 }}>{FARE_POLICY.currency} {money(r.ratePerKm)}/km</div>}
          </div>
        )}
      </div>
      <div style={{ display: "flex", alignItems: "center", gap: 7, rowGap: 7, flexWrap: "wrap", marginTop: 12, paddingTop: 11, borderTop: "1px solid var(--line)" }}>
        <span className="rs-chip" style={{ height: 26 }}><Icon name="clock" size={12}/> {r.depart}</span>
        <span className="rs-chip" style={{ height: 26 }}><Icon name="users" size={12}/> {r.seats} free</span>
        {r.startsKmAway != null && <span className="rs-chip" style={{ height: 26 }}><Icon name="pin" size={12}/> Starts {r.startsKmAway} km away</span>}
        {r.verifiedOnly && <span className="rs-chip" style={{ height: 26, background: "var(--status-approved-soft)", color: "var(--status-approved-ink)", borderColor: "transparent" }}><Icon name="badge" size={12} color="var(--status-approved-ink)"/> Verified only</span>}
        {r.womenOnly
          ? <span className="rs-chip" style={{ height: 26, background: "var(--accent-soft)", color: "var(--accent-ink)", borderColor: "transparent" }}><Icon name="user" size={12} color="var(--accent-ink)"/> Women only</span>
          : <span className="rs-chip" style={{ height: 26, whiteSpace: "nowrap", overflow: "hidden" }}>{r.car.split(" · ")[0]}</span>}
      </div>
    </div>
  );
}

// ═══════════ P01 · HOME · map-first ═══════════
function PxHomeScreen({ resume = false }) {
  return (
    <Phone label={resume ? "P01b Home · resume trip" : "P01 Home · map"}>
      <div style={{ height: "100%", position: "relative", display: "flex", flexDirection: "column" }}>
        <MapBackdrop showRoute={false}/>
        <div style={{ position: "relative", padding: "10px 16px 0" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <ModeChip mode="ride" state="approved"/>
            <div style={{ flex: 1 }}/>
            <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--surface)", boxShadow: "var(--shadow-md)", display: "inline-flex", alignItems: "center", justifyContent: "center" }} aria-label="Notifications"><Icon name="bell" size={19}/></button>
            <Avatar name="Nimali" size={44}/>
          </div>
          {resume && (
            <div style={{ marginTop: 12, padding: 13, borderRadius: 18, background: "var(--ink-fill)", color: "var(--on-ink-fill)", display: "flex", alignItems: "center", gap: 12, boxShadow: "var(--shadow-lg)" }}>
              <div style={{ width: 10, height: 10, borderRadius: 5, background: "#e8834f", animation: "pulse 1.8s infinite", flexShrink: 0 }}/>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", opacity: .7 }}>TRIP IN PROGRESS</div>
                <div style={{ fontSize: 13.5, fontWeight: 700, marginTop: 2 }}>With {MY_TRIP.driver} · 6 min to {MY_TRIP.to}</div>
              </div>
              <div className="rs-btn accent" style={{ height: 44, padding: "0 16px", fontSize: 13, flexShrink: 0 }}>Resume</div>
            </div>
          )}
        </div>
        <div style={{ flex: 1 }}/>
        <div className="rs-sheet" style={{ position: "relative", padding: "6px 16px 14px" }}>
          <div className="rs-sheet-grab"/>
          <div className="rs-display" style={{ fontSize: 24, marginTop: 6 }}>Where to, Nimali?</div>
          <button data-row="where to search" style={{ marginTop: 12, width: "100%", height: 56, padding: "0 16px", display: "flex", alignItems: "center", gap: 12, background: "var(--bg-soft)", border: "1.5px solid var(--line)", borderRadius: 16 }}>
            <Icon name="search" size={18} color="var(--ink-3)"/>
            <span style={{ color: "var(--ink-3)", fontSize: 15 }}>Enter destination</span>
            <span style={{ marginLeft: "auto", display: "inline-flex", alignItems: "center", gap: 5, fontSize: 12, fontWeight: 700, padding: "5px 10px", borderRadius: 999, background: "var(--surface)" }}>
              <Icon name="clock" size={12}/> Now
            </span>
          </button>
          <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
            <button className="rs-tap"><span className="rs-chip"><Icon name="home" size={13}/> Nugegoda</span></button>
            <button className="rs-tap"><span className="rs-chip"><Icon name="briefcase" size={13}/> Colombo Fort</span></button>
          </div>
          {!resume && (
            <div data-row="become a driver promo" style={{ marginTop: 12, padding: 13, borderRadius: 18, background: "var(--mode-drive-soft)", border: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 11 }}>
              <div style={{ width: 38, height: 38, borderRadius: 12, background: "var(--mode-drive)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                <Icon name="car" size={18} color="var(--on-bright-fill)"/>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 13, fontWeight: 800, color: "var(--mode-drive-ink)" }}>Driving to Fort tomorrow?</div>
                <div style={{ fontSize: 11.5, color: "var(--ink-2)", marginTop: 2 }}>Publish it and fill your empty seats.</div>
              </div>
              <Icon name="chev" size={17} color="var(--mode-drive-ink)"/>
            </div>
          )}
        </div>
        <TabBar mode="ride" active="home" badges={{ inbox: 3 }}/>
      </div>
    </Phone>
  );
}

// ═══════════ P02 · HOME · commuter dashboard (variation B) ═══════════
function PxHomeDashScreen() {
  return (
    <Phone label="P02 Home · commuter dashboard">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <HomeHeader mode="ride" state="approved"/>
        <div style={{ flex: 1, overflow: "auto", padding: "0 16px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          <div className="rs-display" style={{ fontSize: 26, lineHeight: 1.15 }}>Good morning,<br/>Nimali.</div>
          <div style={{ padding: 15, borderRadius: 20, background: "var(--accent-soft)", border: "1px solid var(--line)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", color: "var(--accent-ink)", flex: 1 }}>YOUR USUAL {USUAL_COMMUTE.time}</div>
              <span className="rs-chip accent" style={{ height: 24 }}>{USUAL_COMMUTE.matchCount} matches</span>
            </div>
            <div style={{ fontSize: 15, fontWeight: 700, marginTop: 8 }}>{USUAL_COMMUTE.from} → {USUAL_COMMUTE.to}</div>
            <div style={{ fontSize: 12, color: "var(--ink-2)", marginTop: 4 }}>Best today: {USUAL_COMMUTE.best.match}% match, {FARE_POLICY.currency} {money(USUAL_COMMUTE.best.price)}, leaves {USUAL_COMMUTE.best.depart}</div>
            <button className="rs-btn accent full" style={{ marginTop: 12, height: 46 }}>See the {USUAL_COMMUTE.matchCount} matches</button>
          </div>
          <div style={{ display: "flex", gap: 10 }}>
            {[["home", "Home", "Nugegoda"], ["briefcase", "Work", "Colombo Fort"]].map(([ic, l, s]) => (
              <button key={l} style={{ flex: 1, padding: 13, borderRadius: 16, background: "var(--surface)", border: "1px solid var(--line)", display: "flex", flexDirection: "column", gap: 7, alignItems: "flex-start", minHeight: 44 }}>
                <Icon name={ic} size={18} color="var(--ink-2)"/>
                <div style={{ fontSize: 13, fontWeight: 700 }}>{l}</div>
                <div style={{ fontSize: 11, color: "var(--ink-3)" }}>{s}</div>
              </button>
            ))}
          </div>
          <div>
            <div className="rs-section-label" style={{ marginBottom: 8 }}>NEXT BOOKED RIDE</div>
            <div className="rs-card" style={{ padding: 14, display: "flex", alignItems: "center", gap: 12 }}>
              <Avatar name={MY_TRIP.driver} size={44}/>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", color: "var(--mode-ride-ink)" }}>TODAY · {MY_TRIP.depart}</div>
                <div style={{ fontSize: 13.5, fontWeight: 700, marginTop: 2 }}>{MY_TRIP.from} → {MY_TRIP.to}</div>
                <div className="tab" style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>{MY_TRIP.plate} · {FARE_POLICY.currency} {money(MY_TRIP.price)}</div>
              </div>
              <StatusBadge status="approved" label="BOOKED"/>
            </div>
          </div>
          <BecomeDriverPromo/>
        </div>
        <TabBar mode="ride" active="home" badges={{ inbox: 3 }}/>
      </div>
    </Phone>
  );
}

// ═══════════ P03 · SEARCH ═══════════
function PxSearchScreen() {
  return (
    <Phone label="P03 Search">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "10px 16px 13px", background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label="Back"><Icon name="back" size={20}/></button>
            <div style={{ fontSize: 15.5, fontWeight: 700 }}>Plan your trip</div>
          </div>
          <div style={{ display: "flex", gap: 11, marginTop: 13 }}>
            <div style={{ width: 14, display: "flex", flexDirection: "column", alignItems: "center", paddingTop: 18 }}>
              <div style={{ width: 11, height: 11, borderRadius: 6, background: "var(--teal)" }}/>
              <div style={{ flex: 1, width: 2, background: "var(--line-2)", margin: "4px 0" }}/>
              <div style={{ width: 11, height: 11, background: "var(--accent-ink)" }}/>
            </div>
            <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: 9 }}>
              <div style={{ height: 50, padding: "0 14px", borderRadius: 14, background: "var(--bg-soft)", border: "1.5px solid var(--line)", display: "flex", alignItems: "center", fontSize: 14.5, fontWeight: 600 }}>Narahenpita</div>
              <div style={{ height: 50, padding: "0 14px", borderRadius: 14, background: "var(--bg-soft)", border: "1.5px solid var(--ink)", display: "flex", alignItems: "center", fontSize: 14.5 }}>
                <span style={{ fontWeight: 600 }}>Bamba</span>
                <span style={{ width: 2, height: 20, background: "var(--accent-ink)", marginLeft: 2, animation: "blink 1s step-end infinite" }}/>
              </div>
            </div>
            <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", alignSelf: "center", flexShrink: 0 }} aria-label="Swap pickup and destination"><Icon name="swap" size={18}/></button>
          </div>
          <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
            <button className="rs-tap"><span className="rs-chip accent"><Icon name="clock" size={12}/> Leave 8:00 AM</span></button>
            <button className="rs-tap"><span className="rs-chip"><Icon name="users" size={12}/> 1 seat</span></button>
          </div>
          {/* The radius is a pre-filter on where the DRIVER'S TRIP STARTS, measured
              from the pickup point typed above — not from live GPS, because you
              plan tomorrow's commute from your sofa. 20 km is the ceiling. */}
          <div style={{ marginTop: 13, paddingTop: 12, borderTop: "1px solid var(--line)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 9 }}>
              <Icon name="pin" size={14} color="var(--ink-3)"/>
              <div style={{ fontSize: 12, fontWeight: 700, flex: 1 }}>Drivers starting within</div>
            </div>
            <div style={{ display: "flex", gap: 8 }}>
              {POLICY.searchRadiusOptions.map(km => {
                const on = km === POLICY.searchRadiusKm;
                return (
                  <button key={km} data-row={`radius ${km}`} className="rs-tap" style={{ flex: 1 }}>
                    <span className={`rs-chip${on ? " accent" : ""}`} style={{ width: "100%", justifyContent: "center", height: 44, fontSize: 13 }}>{km} km</span>
                  </button>
                );
              })}
            </div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 9, lineHeight: 1.45 }}>
              Measured from Narahenpita. {POLICY.searchRadiusKm} km is as wide as ComiGo goes — further than that and a driver is making a trip for you, not sharing one.
            </div>
          </div>
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: "12px 16px 16px" }} className="rs-scroll">
          <div className="rs-section-label" style={{ marginBottom: 6 }}>MATCHES IN COLOMBO</div>
          {[["pin", "Bambalapitiya", "Galle Road · 6.2 km away"],
            ["pin", "Bambalapitiya Flats", "Marine Drive · 6.6 km away"],
            ["star", "Bambalapitiya Station", "Saved · 6.4 km away"],
            ["pin", "Bambalapitiya Junction", "Galle Rd / Dickman's Rd"]].map(([ic, t, s]) => (
            <div key={t}>
              <MenuRow icon={ic} label={t} sub={s} chev={false}/>
              <div className="rs-divider"/>
            </div>
          ))}
          <div style={{ marginTop: 12 }}>
            <Banner kind="info" icon="pin" title="Turn on location for better matches" body="We'll suggest pickup points you can actually walk to." action="Allow"/>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ P04 · RESULTS · list ═══════════
function ResultsHeader({ count = 4 }) {
  return (
    <div style={{ background: "var(--surface)", borderBottom: "1px solid var(--line)", flexShrink: 0 }}>
      <div style={{ padding: "10px 16px", display: "flex", alignItems: "center", gap: 10 }}>
        <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label="Back"><Icon name="back" size={20}/></button>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 14, fontWeight: 700, display: "flex", alignItems: "center", gap: 6, whiteSpace: "nowrap", overflow: "hidden" }}>
            Narahenpita <Icon name="arrow" size={12} color="var(--ink-3)"/> Bambalapitiya
          </div>
          <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 1 }}>Today, from 8:00 AM · 1 seat · within {POLICY.searchRadiusKm} km</div>
        </div>
        <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label="Edit search"><Icon name="settings" size={18}/></button>
      </div>
      <div style={{ padding: "0 16px 11px", display: "flex", gap: 7, overflow: "hidden" }}>
        <button className="rs-tap" style={{ flexShrink: 0 }}><span className="rs-chip accent"><Icon name="filter" size={12}/> Best match</span></button>
        <button className="rs-tap" style={{ flexShrink: 0 }}><span className="rs-chip">Cheapest</span></button>
        <button className="rs-tap" style={{ flexShrink: 0 }}><span className="rs-chip"><Icon name="pin" size={12}/> {POLICY.searchRadiusKm} km</span></button>
        <button className="rs-tap" style={{ flexShrink: 0 }}><span className="rs-chip">Leaves soonest</span></button>
      </div>
    </div>
  );
}

function PxResultsListScreen() {
  return (
    <Phone label="P04 Results · list">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <ResultsHeader/>
        <div style={{ padding: "11px 16px 8px", display: "flex", alignItems: "center", gap: 8 }}>
          <div style={{ fontSize: 12.5, color: "var(--ink-3)", flex: 1 }}><b style={{ color: "var(--ink)" }}>{RIDES.length} drivers</b> going your way</div>
          <button className="rs-tap"><span className="rs-chip"><Icon name="pin" size={12}/> Map</span></button>
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: "0 16px 16px", display: "flex", flexDirection: "column", gap: 10 }} className="rs-scroll">
          {RIDES.map(r => <RideCard key={r.id} r={r}/>)}
          {/* Say what the radius removed. A silently short list reads as "no
              drivers", which is a different and much worse message. */}
          <div style={{ padding: 14, borderRadius: 16, background: "var(--bg-soft)", border: "1px dashed var(--line-2)", display: "flex", gap: 11 }}>
            <Icon name="pin" size={17} color="var(--ink-3)"/>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 12.5, fontWeight: 700 }}>{RADIUS_FILTERED_OUT} more drivers start further than {POLICY.searchRadiusKm} km away</div>
              <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.45 }}>They'd have to drive to you first, so ComiGo doesn't offer them. Try a different departure time instead.</div>
            </div>
          </div>
        </div>
        <TabBar mode="ride" active="action" badges={{ inbox: 3 }}/>
      </div>
    </Phone>
  );
}

// ═══════════ P05 · RESULTS · grouped by overlap tier ═══════════
function PxResultsGroupedScreen() {
  const groups = [
    { tier: "Full route", body: "Door to door — no walking at either end.", rides: RIDES.filter(r => r.match >= 95) },
    { tier: "Most of your route", body: "A short walk or a slightly different drop-off.", rides: RIDES.filter(r => r.match >= 75 && r.match < 95) },
    { tier: "Part of your route", body: "Cheaper, but you finish the last stretch yourself.", rides: RIDES.filter(r => r.match >= 45 && r.match < 75) },
  ];
  return (
    <Phone label="P05 Results · grouped">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <ResultsHeader/>
        <div style={{ flex: 1, overflow: "auto", padding: "12px 16px 16px", display: "flex", flexDirection: "column", gap: 18 }} className="rs-scroll">
          {groups.map(g => {
            const t = matchTier(g.rides[0]?.match ?? 50);
            return (
              <div key={g.tier}>
                <div style={{ display: "flex", alignItems: "center", gap: 9, marginBottom: 4 }}>
                  <div style={{ width: 9, height: 9, borderRadius: 5, background: t.c, flexShrink: 0 }}/>
                  <div style={{ fontSize: 14, fontWeight: 800, flex: 1 }}>{g.tier}</div>
                  <div style={{ fontSize: 11.5, color: "var(--ink-3)" }}>{g.rides.length} {g.rides.length === 1 ? "driver" : "drivers"}</div>
                </div>
                <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginBottom: 10, lineHeight: 1.45 }}>{g.body}</div>
                <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                  {g.rides.map(r => <RideCard key={r.id} r={r}/>)}
                </div>
              </div>
            );
          })}
        </div>
        <TabBar mode="ride" active="action" badges={{ inbox: 3 }}/>
      </div>
    </Phone>
  );
}

// ═══════════ P06 · RESULTS · map ═══════════
function PxResultsMapScreen() {
  const r = RIDES[0];
  return (
    <Phone label="P06 Results · map">
      <div style={{ height: "100%", position: "relative", display: "flex", flexDirection: "column" }}>
        <MapBackdrop pickupLabel="Narahenpita" dropLabel="Bambalapitiya"/>
        <div style={{ position: "relative", padding: "10px 16px 0", display: "flex", alignItems: "center", gap: 10 }}>
          <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--surface)", boxShadow: "var(--shadow-md)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label="Back"><Icon name="back" size={20}/></button>
          <div style={{ flex: 1, padding: "9px 13px", borderRadius: 14, background: "var(--surface)", boxShadow: "var(--shadow-md)" }}>
            <div style={{ fontSize: 12.5, fontWeight: 700, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>Narahenpita → Bambalapitiya</div>
            <div style={{ fontSize: 10.5, color: "var(--ink-3)" }}>{RIDES.length} drivers · from {FARE_POLICY.currency} {money(cheapestFare())}</div>
          </div>
        </div>
        <div style={{ position: "relative", padding: "10px 16px 0", display: "flex", justifyContent: "flex-end" }}>
          <button className="rs-tap"><span className="rs-chip" style={{ background: "var(--surface)", boxShadow: "var(--shadow-md)", border: "none" }}><Icon name="menu" size={12}/> View as list</span></button>
        </div>
        <div style={{ flex: 1 }}/>
        <div style={{ position: "relative", padding: "0 16px 12px" }}>
          <div style={{ display: "flex", gap: 7, marginBottom: 10, overflow: "hidden" }}>
            {RIDES.slice(0, 3).map((x, i) => (
              <div key={x.id} style={{
                padding: "7px 11px", borderRadius: 999, flexShrink: 0,
                background: i === 0 ? "var(--ink-fill)" : "var(--surface)",
                color: i === 0 ? "var(--on-ink-fill)" : "var(--ink)",
                boxShadow: "var(--shadow-md)", fontSize: 11.5, fontWeight: 700,
                display: "flex", alignItems: "center", gap: 6, minHeight: 44,
              }}>
                <span style={{ color: i === 0 ? "#e8834f" : matchTier(x.match).ink, fontWeight: 800 }}>{x.match}%</span>
                {FARE_POLICY.currency} {money(x.price)}
              </div>
            ))}
          </div>
          <div style={{ padding: 14, borderRadius: 20, background: "var(--surface)", boxShadow: "var(--shadow-lg)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <Avatar name={r.driver} size={44}/>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 14, fontWeight: 700 }}>{r.driver} · {r.rating}★</div>
                <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>{r.car} · {r.depart}</div>
              </div>
              <MatchBadge value={r.match} size={44}/>
            </div>
            <button className="rs-btn accent full" style={{ marginTop: 12 }}>
              Book for {FARE_POLICY.currency} {money(r.price)}
            </button>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ P07 · RIDE DETAIL ═══════════
function PxRideDetailScreen() {
  const r = RIDES[2];
  const onRoute = r.dist;
  return (
    <Phone label="P07 Ride detail">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ height: 176, position: "relative", flexShrink: 0 }}>
          <MapBackdrop pickupLabel="Pick up" dropLabel="Drop off"/>
          <div style={{ position: "absolute", left: 14, top: 12 }}>
            <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--surface)", boxShadow: "var(--shadow-md)", display: "inline-flex", alignItems: "center", justifyContent: "center" }} aria-label="Back"><Icon name="back" size={20}/></button>
          </div>
          <div style={{ position: "absolute", right: 14, top: 12, display: "flex", gap: 8 }}>
            <button className="rs-tap"><span className="rs-chip" style={{ background: "var(--surface)", boxShadow: "var(--shadow-md)", border: "none" }}><Icon name="menu" size={12}/> List</span></button>
          </div>
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 14 }} className="rs-scroll">
          {/* Trust, shown as a pair: a score is meaningless without its
              denominator. The panel carries identity, so there is no separate
              header row — two avatars for one driver reads as a bug. */}
          <TrustStats name={r.driver} role="Driver" rating={r.rating} ratings={r.trips}
            trips={r.trips} completed={97} since="Jun 2025"
            right={<button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label="Message driver"><Icon name="chat" size={19}/></button>}
            foot={`${r.car} · ${r.plate}`}/>

          {r.womenOnly && (
            <Banner kind="info" icon="user" title={`${r.driver.split(" ")[0]} takes women passengers only`}
              body="She's set this trip for women only, and you match — so you can request a seat. Drivers set this per trip."/>
          )}

          <div style={{ padding: 15, borderRadius: 18, background: "var(--surface)", border: "1px solid var(--line)" }}>
            <MatchBadge value={r.match} size={52}/>
            <div style={{ height: 1, background: "var(--line)", margin: "13px 0" }}/>
            <div style={{ display: "flex", flexDirection: "column", gap: 9 }}>
              {[["check", `${r.driver.split(" ")[0]}'s route covers ${r.dist} km of your 6.1 km trip`],
                ["pin", "Pick up at Narahenpita junction — 120 m from you"],
                ["arrow", "You get off at Thunmulla and walk 450 m to Bambalapitiya"]].map(([ic, t]) => (
                <div key={t} style={{ display: "flex", gap: 10, alignItems: "flex-start" }}>
                  <Icon name={ic} size={15} color="var(--ink-3)" strokeWidth={2.2}/>
                  <div style={{ fontSize: 12.5, color: "var(--ink-2)", lineHeight: 1.45, flex: 1 }}>{t}</div>
                </div>
              ))}
            </div>
          </div>

          <div>
            <div className="rs-section-label" style={{ marginBottom: 10 }}>THE ROUTE</div>
            <RouteTimeline stops={[
              { kind: "pickup", place: "Narahenpita junction", time: r.depart, note: "Your pickup · 120 m walk" },
              { kind: "via", place: "Kirulapone", time: "8:36 AM" },
              { kind: "via", place: "Thimbirigasyaya", time: "8:41 AM" },
              { kind: "drop", place: "Thunmulla", time: "8:47 AM", note: "Your drop-off · 450 m to Bambalapitiya" },
            ]}/>
          </div>

          <div>
            <div className="rs-section-label" style={{ marginBottom: 10 }}>WHAT YOU PAY</div>
            <FareBreakdown
              currency={FARE_POLICY.currency}
              lines={[
                { label: `On-route distance · ${onRoute} km`, sub: `At ${FARE_POLICY.currency} ${money(r.ratePerKm)} per km — ${r.driver.split(" ")[0]}'s rate`, value: r.gross },
                { label: FARE_POLICY.discountLabel, sub: `${r.match}% overlap`, value: r.discount, kind: "discount" },
              ]}
              total={r.price}
              footnote={`Includes a ${FARE_POLICY.commissionPct}% ComiGo fee, charged when ${r.driver.split(" ")[0]} starts the trip — not when she accepts. Get off earlier than planned and we recalculate on the distance you actually travelled, twice a month.`}
            />
          </div>

          {/* Why this driver is cheaper than the last one. Per-vehicle rates are
              new, so the difference has to be explained where it is first felt. */}
          <div style={{ padding: 15, borderRadius: 18, background: "var(--surface)", border: "1px solid var(--line)" }}>
            <div className="rs-section-label" style={{ marginBottom: 11 }}>WHY {r.driver.split(" ")[0].toUpperCase()}'S RATE IS {FARE_POLICY.currency} {money(r.ratePerKm)}</div>
            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: "flex", justifyContent: "space-between", fontSize: 10.5, fontWeight: 700, color: "var(--ink-3)" }}>
                  <span className="tab">{money(vehicleClass(r.vClass).band[0])}</span>
                  <span className="tab">{money(vehicleClass(r.vClass).band[1])}</span>
                </div>
                <div style={{ position: "relative", height: 8, borderRadius: 4, background: "var(--bg-soft)", marginTop: 5 }}>
                  <div style={{ position: "absolute", left: `${(r.ratePerKm - vehicleClass(r.vClass).band[0]) / (vehicleClass(r.vClass).band[1] - vehicleClass(r.vClass).band[0]) * 100}%`, top: -3, width: 14, height: 14, borderRadius: 7, background: "var(--accent-ink)", border: "2px solid var(--surface)", transform: "translateX(-50%)" }}/>
                </div>
                <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 8, lineHeight: 1.45 }}>
                  ComiGo sets a per-kilometre band for every {r.vClass.toLowerCase()} from its age, insurance, fuel figure and service record. {r.driver.split(" ")[0]} chose her rate inside it — she can't charge above the ceiling.
                </div>
              </div>
            </div>
          </div>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 13 }}>
          <div>
            <div className="rs-display tab" style={{ fontSize: 22, fontWeight: 600 }}>{FARE_POLICY.currency} {money(r.price)}</div>
            <div style={{ fontSize: 11, color: "var(--ink-3)" }}>{r.seats} seat left</div>
          </div>
          <button className="rs-btn accent" style={{ flex: 1 }}>Choose seat</button>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, {
  FARE_POLICY, RIDES, RouteTimeline, MatchBadge, RideCard, ResultsHeader,
  PxHomeScreen, PxHomeDashScreen, PxSearchScreen, PxResultsListScreen,
  PxResultsGroupedScreen, PxResultsMapScreen, PxRideDetailScreen,
});
