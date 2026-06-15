// screens-home.jsx — home, search, results, ride detail

// ═══════════════════════════════════════════════════════════════════
// HOME — map backdrop + "Where to?" sheet
// ═══════════════════════════════════════════════════════════════════
function HomeScreen() {
  return (
    <Phone label="06 Home" statusDark={false}>
      <div style={{ height: "100%", position: "relative" }}>
        <MapBackdrop showRoute={false}/>
        {/* Top bar */}
        <div style={{ position: "absolute", top: 14, left: 16, right: 16, display: "flex", justifyContent: "space-between", zIndex: 2 }}>
          <button style={{ width: 44, height: 44, borderRadius: 22, background: "#fff", display: "inline-flex", alignItems: "center", justifyContent: "center", boxShadow: "var(--shadow-md)" }}>
            <Icon name="menu" size={20}/>
          </button>
          <button style={{ height: 44, padding: "0 14px", borderRadius: 22, background: "#fff", display: "inline-flex", alignItems: "center", gap: 8, boxShadow: "var(--shadow-md)", fontSize: 13, fontWeight: 600 }}>
            <Icon name="shield" size={16} color="var(--teal)"/> Safety
          </button>
        </div>
        {/* Current location pin */}
        <div style={{ position: "absolute", left: "50%", top: "38%", transform: "translate(-50%, -50%)" }}>
          <div style={{ width: 24, height: 24, borderRadius: 12, background: "var(--teal)", border: "5px solid #fff", boxShadow: "0 0 0 12px rgba(15,110,102,.12)" }}/>
        </div>

        {/* Bottom sheet */}
        <div style={{ position: "absolute", bottom: 0, left: 0, right: 0, zIndex: 3 }} className="rs-sheet">
          <div className="rs-sheet-grab"/>
          <div style={{ padding: "4px 20px 24px" }}>
            <div className="rs-display" style={{ fontSize: 24, color: "var(--ink)", letterSpacing: "-0.02em" }}>Where to, Nimali?</div>

            <button style={{ marginTop: 14, width: "100%", height: 56, padding: "0 16px", display: "flex", alignItems: "center", gap: 12, background: "var(--bg-soft)", border: "1.5px solid var(--line)", borderRadius: 16 }}>
              <Icon name="search" size={18} color="var(--ink-3)"/>
              <span style={{ color: "var(--ink-3)", fontSize: 15 }}>Enter destination</span>
              <span style={{ marginLeft: "auto", display: "inline-flex", alignItems: "center", gap: 6, fontSize: 12, color: "var(--ink)", fontWeight: 600, padding: "4px 10px", borderRadius: 999, background: "#fff", border: "1px solid var(--line)" }}>
                <Icon name="clock" size={12}/> Now
              </span>
            </button>

            <div style={{ marginTop: 18, display: "flex", gap: 10 }}>
              <QuickChip icon="home" label="Home" sub="2 km away"/>
              <QuickChip icon="briefcase" label="Office" sub="World Trade Center"/>
            </div>

            <div style={{ marginTop: 20, display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
              <div className="rs-section-label">FREQUENT ROUTES</div>
              <span style={{ fontSize: 12, color: "var(--accent-2)", fontWeight: 600 }}>See all</span>
            </div>
            <div style={{ marginTop: 10, display: "flex", flexDirection: "column", gap: 2 }}>
              <RecentRow to="Colombo Fort" via="Rajagiriya → Narahenpita" n={8}/>
              <RecentRow to="University of Colombo" via="Kotte Rd → Thunmulla" n={4}/>
            </div>
          </div>
        </div>
      </div>
    </Phone>
  );
}

function QuickChip({ icon, label, sub }) {
  return (
    <div style={{ flex: 1, padding: "12px 14px", background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 14, display: "flex", gap: 10, alignItems: "center" }}>
      <div style={{ width: 36, height: 36, borderRadius: 18, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
        <Icon name={icon} size={18} color="var(--ink)"/>
      </div>
      <div style={{ minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 700 }}>{label}</div>
        <div style={{ fontSize: 11, color: "var(--ink-3)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{sub}</div>
      </div>
    </div>
  );
}

function RecentRow({ to, via, n }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 14, padding: "10px 0" }}>
      <div style={{ width: 36, height: 36, borderRadius: 18, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
        <Icon name="history" size={18} color="var(--ink-3)"/>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 15, fontWeight: 600 }}>{to}</div>
        <div style={{ fontSize: 12, color: "var(--ink-3)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{via}</div>
      </div>
      <div style={{ fontSize: 11, color: "var(--ink-4)", fontWeight: 600 }}>{n} rides</div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════
// SEARCH — pickup + destination input
// ═══════════════════════════════════════════════════════════════════
function SearchScreen() {
  return (
    <Phone label="07 Search">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "12px 16px 14px", background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="back" size={20}/>
            </button>
            <div style={{ flex: 1, position: "relative" }}>
              <div style={{ position: "absolute", left: 6, top: 16, bottom: 16, width: 20, display: "flex", flexDirection: "column", alignItems: "center" }}>
                <div style={{ width: 10, height: 10, borderRadius: 5, background: "var(--teal)" }}/>
                <div style={{ flex: 1, width: 2, background: "var(--line-2)", backgroundImage: "linear-gradient(var(--line-2) 60%, transparent 60%)", backgroundSize: "2px 5px", marginTop: 2, marginBottom: 2 }}/>
                <div style={{ width: 10, height: 10, background: "var(--accent)" }}/>
              </div>
              <div style={{ paddingLeft: 32, display: "flex", flexDirection: "column", gap: 4 }}>
                <div style={{ height: 44, padding: "0 12px", background: "var(--bg-soft)", borderRadius: 10, display: "flex", alignItems: "center", fontSize: 14, fontWeight: 600 }}>
                  Narahenpita Junction
                </div>
                <div style={{ height: 44, padding: "0 12px", background: "var(--surface)", border: "1.5px solid var(--accent)", borderRadius: 10, display: "flex", alignItems: "center", fontSize: 14, fontWeight: 600 }}>
                  Bambalapitiya<span style={{ marginLeft: 2, width: 2, height: 18, background: "var(--accent)" }}/>
                </div>
              </div>
            </div>
            <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="swap" size={18}/>
            </button>
          </div>

          <div style={{ marginTop: 12, display: "flex", gap: 8 }}>
            <Pill active><Icon name="clock" size={12}/> Now</Pill>
            <Pill><Icon name="calendar" size={12}/> Tomorrow 8 AM</Pill>
            <Pill><Icon name="users" size={12}/> 1 seat</Pill>
          </div>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "8px 0" }} className="rs-scroll">
          <div style={{ padding: "8px 20px 6px" }} className="rs-section-label">SAVED</div>
          <Place icon="home" name="Home" addr="34/A Baseline Rd, Rajagiriya" dist="2 km"/>
          <Place icon="briefcase" name="Office" addr="World Trade Center, Colombo 1" dist="8 km"/>
          <div style={{ padding: "14px 20px 6px" }} className="rs-section-label">SUGGESTIONS</div>
          <Place icon="pin" name="Bambalapitiya Railway Stn" addr="Galle Rd, Colombo 4" dist="6 km" highlight/>
          <Place icon="pin" name="Bambalapitiya Majestic City" addr="Galle Rd, Colombo 4" dist="6 km"/>
          <Place icon="pin" name="Bambalapitiya Temple" addr="Temple Rd, Colombo 4" dist="6 km"/>
          <Place icon="pin" name="Bambalapitiya Flats" addr="Marine Dr, Colombo 4" dist="6 km"/>
        </div>
      </div>
    </Phone>
  );
}

function Pill({ children, active }) {
  return (
    <div style={{ height: 28, padding: "0 10px", borderRadius: 14, display: "inline-flex", alignItems: "center", gap: 5, fontSize: 12, fontWeight: 600, background: active ? "var(--ink)" : "var(--surface)", color: active ? "var(--bg)" : "var(--ink-2)", border: active ? "none" : "1px solid var(--line)" }}>
      {children}
    </div>
  );
}

function Place({ icon, name, addr, dist, highlight }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 14, padding: "12px 20px", background: highlight ? "var(--accent-soft)" : "transparent" }}>
      <div style={{ width: 38, height: 38, borderRadius: 19, background: highlight ? "#fff" : "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
        <Icon name={icon} size={18} color={highlight ? "var(--accent)" : "var(--ink)"}/>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 700 }}>{name}</div>
        <div style={{ fontSize: 12, color: "var(--ink-3)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{addr}</div>
      </div>
      <div style={{ fontSize: 11, color: "var(--ink-4)", fontWeight: 600 }}>{dist}</div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════
// RIDE RESULTS — list view, match % hero
// ═══════════════════════════════════════════════════════════════════
const MOCK_RIDES = [
  { id: "r1", driver: "Saman W", rating: 4.9, trips: 312, match: 100, dist: 6.2, price: 310, seats: 3, depart: "8:04 AM", car: "Toyota Aqua · Silver", plate: "CAR-2211", overlap: "Full route · Narahenpita → Bambalapitiya", eta: "3 min", badge: "Best match" },
  { id: "r2", driver: "Kasun D", rating: 4.8, trips: 128, match: 92, dist: 5.8, price: 290, seats: 2, depart: "8:12 AM", car: "Suzuki Alto · Blue", plate: "WP KB-8842", overlap: "92% · Nugegoda → Kollupitiya", eta: "6 min" },
  { id: "r3", driver: "Priya J", rating: 5.0, trips: 84, match: 74, dist: 4.5, price: 225, seats: 1, depart: "8:30 AM", car: "Honda Fit · Pearl", plate: "CBC-4401", overlap: "74% · you walk 450 m at drop-off", eta: "Scheduled", badge: "Top rated" },
  { id: "r4", driver: "Imran F", rating: 4.7, trips: 201, match: 58, dist: 3.6, price: 180, seats: 3, depart: "8:45 AM", car: "Perodua Viva · White", plate: "KI-1198", overlap: "58% · drops at Thunmulla — 1.2 km onward", eta: "Scheduled" },
];

function ResultsListScreen() {
  return (
    <Phone label="08 Results · List">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <ResultsHeader/>
        <div style={{ padding: "10px 16px 6px", display: "flex", gap: 8, alignItems: "center", background: "var(--surface)", borderBottom: "1px solid var(--line)", whiteSpace: "nowrap", overflow: "hidden" }}>
          <button className="rs-chip accent" style={{ height: 32, whiteSpace: "nowrap", flexShrink: 0 }}><Icon name="filter" size={12}/> 50%+</button>
          <button className="rs-chip" style={{ height: 32, flexShrink: 0 }}>Price</button>
          <button className="rs-chip" style={{ height: 32, flexShrink: 0 }}>Depart</button>
          <button className="rs-chip" style={{ height: 32, flexShrink: 0 }}>Rating</button>
        </div>
        <div style={{ padding: "12px 16px 8px", display: "flex", justifyContent: "space-between", alignItems: "center", gap: 8 }}>
          <div style={{ fontSize: 13, fontWeight: 700, whiteSpace: "nowrap" }}>4 matches</div>
          <div style={{ display: "flex", gap: 4, background: "var(--bg-soft)", padding: 3, borderRadius: 999, fontSize: 12, fontWeight: 600 }}>
            <div style={{ padding: "5px 12px", borderRadius: 999, background: "var(--surface)", boxShadow: "var(--shadow-sm)" }}>List</div>
            <div style={{ padding: "5px 12px", color: "var(--ink-3)" }}>Map</div>
          </div>
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: "0 16px 16px", display: "flex", flexDirection: "column", gap: 10 }} className="rs-scroll">
          {MOCK_RIDES.map(r => <RideCard key={r.id} ride={r}/>)}
        </div>
      </div>
    </Phone>
  );
}

function ResultsHeader() {
  return (
    <div style={{ padding: "12px 16px 12px", background: "var(--surface)", borderBottom: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 10 }}>
      <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
        <Icon name="back" size={20}/>
      </button>
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 15, fontWeight: 700, display: "flex", alignItems: "center", gap: 6 }}>
          Narahenpita <Icon name="arrow" size={12} color="var(--ink-3)"/> Bambalapitiya
        </div>
        <div style={{ fontSize: 11, color: "var(--ink-3)" }}>Today · departing now · 1 seat</div>
      </div>
      <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
        <Icon name="settings" size={18}/>
      </button>
    </div>
  );
}

function RideCard({ ride, compact }) {
  return (
    <div className="rs-card" style={{ padding: 14, display: "flex", flexDirection: "column", gap: 12 }}>
      <div style={{ display: "flex", gap: 12, alignItems: "center" }}>
        <MatchRing value={ride.match} size={58}/>
        <div style={{ flex: 1, minWidth: 0 }}>
          {ride.badge && (
            <div style={{ display: "inline-flex", alignItems: "center", gap: 4, height: 20, padding: "0 8px", background: "var(--accent-soft)", color: "var(--accent-2)", fontSize: 10, fontWeight: 700, borderRadius: 999, letterSpacing: ".04em", marginBottom: 4 }}>
              {ride.badge === "Best match" && <Icon name="check" size={10} color="var(--accent-2)"/>}
              {ride.badge === "Top rated" && <Icon name="star" size={10} color="var(--accent-2)"/>}
              {ride.badge.toUpperCase()}
            </div>
          )}
          <div style={{ fontSize: 14, fontWeight: 700, lineHeight: 1.15 }}>{ride.overlap}</div>
          <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 2 }}>Depart {ride.depart} · {ride.dist} km your stretch</div>
        </div>
        <div style={{ textAlign: "right", flexShrink: 0, whiteSpace: "nowrap" }}>
          <div className="rs-display" style={{ fontSize: 22, fontWeight: 600, whiteSpace: "nowrap" }}>LKR {ride.price}</div>
          <div style={{ fontSize: 10, color: "var(--ink-4)", fontWeight: 600, letterSpacing: ".04em", whiteSpace: "nowrap" }}>EST · CASH OK</div>
        </div>
      </div>

      <div className="rs-divider"/>

      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
        <Avatar name={ride.driver} size={34}/>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 13, fontWeight: 700, display: "flex", alignItems: "center", gap: 6, whiteSpace: "nowrap", overflow: "hidden" }}>
            {ride.driver} <Icon name="star" size={12} color="var(--warn)"/> <span style={{ fontSize: 12 }}>{ride.rating}</span>
            <span style={{ fontSize: 11, color: "var(--ink-4)", whiteSpace: "nowrap" }}>· {ride.trips}</span>
          </div>
          <div style={{ fontSize: 11, color: "var(--ink-3)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", minWidth: 0 }}>{ride.car}</div>
        </div>
        <div style={{ display: "inline-flex", alignItems: "center", gap: 4, fontSize: 11, color: "var(--ink-3)", fontWeight: 600, flexShrink: 0, whiteSpace: "nowrap" }}>
          <Icon name="users" size={12}/> {ride.seats}
        </div>
      </div>
    </div>
  );
}

// Variant B — horizontally scrolling "hero" match-cards over the map
function ResultsMapScreen() {
  return (
    <Phone label="09 Results · Map">
      <div style={{ height: "100%", position: "relative" }}>
        <MapBackdrop pickupLabel="Narahenpita" dropLabel="Bambalapitiya"/>
        {/* other driver pins */}
        <div style={{ position: "absolute", left: "32%", top: "28%" }}>
          <SmallCarPin color="var(--ink)" pct={100}/>
        </div>
        <div style={{ position: "absolute", left: "55%", top: "48%" }}>
          <SmallCarPin color="var(--ink-3)" pct={92}/>
        </div>
        <div style={{ position: "absolute", left: "40%", top: "62%" }}>
          <SmallCarPin color="var(--ink-3)" pct={74}/>
        </div>
        {/* Top bar */}
        <div style={{ position: "absolute", top: 12, left: 12, right: 12, zIndex: 5, display: "flex", gap: 8 }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "#fff", display: "inline-flex", alignItems: "center", justifyContent: "center", boxShadow: "var(--shadow-md)" }}>
            <Icon name="back" size={20}/>
          </button>
          <div style={{ flex: 1, height: 40, padding: "0 14px", background: "#fff", borderRadius: 20, display: "flex", alignItems: "center", gap: 8, fontSize: 13, fontWeight: 600, boxShadow: "var(--shadow-md)" }}>
            <div style={{ width: 8, height: 8, background: "var(--teal)", borderRadius: 4 }}/> Narahenpita
            <Icon name="arrow" size={12} color="var(--ink-3)"/>
            <div style={{ width: 8, height: 8, background: "var(--accent)" }}/> Bambalapitiya
          </div>
        </div>
        {/* View toggle */}
        <div style={{ position: "absolute", right: 12, top: 64, zIndex: 5 }}>
          <div style={{ display: "flex", flexDirection: "column", gap: 2, background: "#fff", padding: 4, borderRadius: 14, fontSize: 11, fontWeight: 700, boxShadow: "var(--shadow-md)" }}>
            <div style={{ padding: "6px 10px", color: "var(--ink-3)" }}>List</div>
            <div style={{ padding: "6px 10px", borderRadius: 10, background: "var(--ink)", color: "var(--bg)" }}>Map</div>
          </div>
        </div>
        {/* Swipeable card stack */}
        <div style={{ position: "absolute", bottom: 18, left: 0, right: 0, zIndex: 5 }}>
          <div style={{ padding: "0 12px 6px", display: "flex", justifyContent: "space-between", color: "#fff", fontSize: 12, fontWeight: 700 }}>
            <span style={{ background: "rgba(0,0,0,.5)", padding: "3px 10px", borderRadius: 999 }}>1 of 4</span>
            <span style={{ background: "rgba(0,0,0,.5)", padding: "3px 10px", borderRadius: 999 }}>Swipe ›</span>
          </div>
          <div style={{ padding: "0 12px", display: "flex", gap: 10, overflow: "hidden" }}>
            <div style={{ width: 320, flexShrink: 0 }}>
              <RideCard ride={MOCK_RIDES[0]}/>
            </div>
            <div style={{ width: 50, flexShrink: 0 }}>
              <div style={{ height: 200, background: "#fff", borderRadius: 16, opacity: .6 }}/>
            </div>
          </div>
        </div>
      </div>
    </Phone>
  );
}

function SmallCarPin({ color, pct }) {
  return (
    <div style={{ position: "relative" }}>
      <div style={{ width: 40, height: 40, borderRadius: 20, background: color, border: "3px solid #fff", boxShadow: "0 3px 10px rgba(0,0,0,.2)", display: "flex", alignItems: "center", justifyContent: "center" }}>
        <Icon name="car" size={18} color="#fff"/>
      </div>
      <div style={{ position: "absolute", top: -8, right: -14, background: "var(--accent)", color: "#fff", fontSize: 10, fontWeight: 700, padding: "2px 6px", borderRadius: 999, border: "2px solid #fff" }}>{pct}%</div>
    </div>
  );
}

Object.assign(window, { HomeScreen, SearchScreen, ResultsListScreen, ResultsMapScreen, RideCard, MOCK_RIDES });
