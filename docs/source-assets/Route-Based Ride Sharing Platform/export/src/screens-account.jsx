// screens-account.jsx — trip history, saved places, profile, SOS, share trip, notifications, support

// ═══════════════════════════════════════════════════════════════════
// TRIP HISTORY
// ═══════════════════════════════════════════════════════════════════
function TripHistoryScreen() {
  const trips = [
    { date: "Today · 8:04 AM", from: "Narahenpita", to: "Kollupitiya", price: 186, match: 100, driver: "Saman W", dist: "3.8 km" },
    { date: "Yesterday · 5:42 PM", from: "WTC", to: "Rajagiriya", price: 240, match: 88, driver: "Kasun D", dist: "4.9 km" },
    { date: "Yesterday · 8:12 AM", from: "Rajagiriya", to: "WTC", price: 310, match: 100, driver: "Saman W", dist: "6.2 km" },
    { date: "22 Apr · 7:50 AM", from: "Rajagiriya", to: "WTC", price: 310, match: 100, driver: "Saman W", dist: "6.2 km" },
    { date: "21 Apr · 6:02 PM", from: "WTC", to: "Bambalapitiya", price: 145, match: 74, driver: "Priya J", dist: "2.9 km" },
    { date: "19 Apr · 9:15 AM", from: "Nugegoda", to: "Fort", price: 420, match: 92, driver: "Imran F", dist: "8.4 km" },
  ];
  return (
    <Phone label="18 Trip History">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "14px 20px", display: "flex", alignItems: "center", gap: 12, background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="back" size={20}/>
          </button>
          <div style={{ flex: 1, fontSize: 18, fontWeight: 700 }}>Your trips</div>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="filter" size={18}/>
          </button>
        </div>

        <div style={{ padding: "16px 20px 10px", display: "flex", gap: 8 }}>
          <div className="rs-chip accent" style={{ height: 30 }}>All</div>
          <div className="rs-chip" style={{ height: 30 }}>This week</div>
          <div className="rs-chip" style={{ height: 30 }}>Last month</div>
        </div>

        <div style={{ padding: "10px 20px 14px" }}>
          <div className="rs-card" style={{ padding: 14, background: "var(--teal-soft)", border: "none" }}>
            <div style={{ display: "flex", justifyContent: "space-between" }}>
              <div>
                <div style={{ fontSize: 11, color: "var(--teal)", fontWeight: 700, letterSpacing: ".08em" }}>THIS MONTH</div>
                <div className="rs-display" style={{ fontSize: 24, color: "var(--teal)" }}>LKR 2,810</div>
                <div style={{ fontSize: 11, color: "var(--teal)", opacity: .75 }}>14 trips · 68 km</div>
              </div>
              <div style={{ textAlign: "right" }}>
                <div style={{ fontSize: 11, color: "var(--teal)", fontWeight: 700, letterSpacing: ".08em" }}>SAVED VS TAXI</div>
                <div className="rs-display" style={{ fontSize: 24, color: "var(--match-full)" }}>LKR 3,440</div>
                <div style={{ fontSize: 11, color: "var(--teal)", opacity: .75 }}>55% off</div>
              </div>
            </div>
          </div>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "0 20px 20px" }} className="rs-scroll">
          {trips.map((t, i) => (
            <div key={i}>
              <div style={{ padding: "12px 0", display: "flex", gap: 12, alignItems: "center" }}>
                <div style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0, position: "relative" }}>
                  <Icon name="route" size={20} color="var(--ink-2)"/>
                  <div style={{ position: "absolute", bottom: -2, right: -2, padding: "1px 5px", background: "var(--surface)", border: "1.5px solid var(--line)", borderRadius: 8, fontSize: 9, fontWeight: 700, color: "var(--match-full)" }}>{t.match}%</div>
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 14, fontWeight: 700, display: "flex", alignItems: "center", gap: 6 }}>
                    {t.from} <Icon name="arrow" size={11} color="var(--ink-3)"/> {t.to}
                  </div>
                  <div style={{ fontSize: 11, color: "var(--ink-3)" }}>{t.date} · {t.dist} · {t.driver}</div>
                </div>
                <div className="tab" style={{ fontWeight: 700, fontSize: 14 }}>LKR {t.price}</div>
              </div>
              {i < trips.length - 1 && <div className="rs-divider"/>}
            </div>
          ))}
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// SAVED PLACES
// ═══════════════════════════════════════════════════════════════════
function SavedPlacesScreen() {
  const places = [
    { icon: "home", name: "Home", addr: "34/A Baseline Road, Rajagiriya", tint: "var(--teal-soft)", tintFg: "var(--teal)" },
    { icon: "briefcase", name: "Office", addr: "World Trade Center, Colombo 1", tint: "var(--accent-soft)", tintFg: "var(--accent-2)" },
    { icon: "star", name: "Gym", addr: "Body Bar, Thunmulla", tint: "var(--bg-soft)", tintFg: "var(--ink-2)" },
    { icon: "pin", name: "Mum's place", addr: "Ward Pl, Colombo 7", tint: "var(--bg-soft)", tintFg: "var(--ink-2)" },
  ];
  return (
    <Phone label="19 Saved Places">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "14px 20px", display: "flex", alignItems: "center", gap: 12, background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="back" size={20}/>
          </button>
          <div style={{ flex: 1, fontSize: 18, fontWeight: 700 }}>Saved places</div>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "16px 20px" }} className="rs-scroll">
          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            {places.map((p) => (
              <div key={p.name} className="rs-card" style={{ padding: 14, display: "flex", alignItems: "center", gap: 12 }}>
                <div style={{ width: 44, height: 44, borderRadius: 14, background: p.tint, display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                  <Icon name={p.icon} size={20} color={p.tintFg}/>
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 14, fontWeight: 700 }}>{p.name}</div>
                  <div style={{ fontSize: 12, color: "var(--ink-3)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{p.addr}</div>
                </div>
                <button style={{ width: 32, height: 32, borderRadius: 16, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                  <Icon name="ellipsis" size={16}/>
                </button>
              </div>
            ))}
          </div>

          <button style={{ marginTop: 16, width: "100%", padding: 18, background: "transparent", border: "1.5px dashed var(--line-2)", borderRadius: 14, display: "flex", alignItems: "center", justifyContent: "center", gap: 10, fontWeight: 700, color: "var(--ink-2)" }}>
            <Icon name="plus" size={16}/> Add new place
          </button>

          <div className="rs-section-label" style={{ marginTop: 28, marginBottom: 10 }}>RECENT SEARCHES</div>
          <div style={{ display: "flex", flexDirection: "column" }}>
            {["Colombo Fort Station", "Majestic City", "University of Colombo", "Galle Face Green"].map((n) => (
              <div key={n} style={{ padding: "12px 0", display: "flex", gap: 12, alignItems: "center", borderBottom: "1px solid var(--line)" }}>
                <Icon name="history" size={16} color="var(--ink-3)"/>
                <div style={{ flex: 1, fontSize: 14 }}>{n}</div>
                <Icon name="plus" size={14} color="var(--ink-4)"/>
              </div>
            ))}
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// PROFILE / ACCOUNT
// ═══════════════════════════════════════════════════════════════════
function AccountScreen() {
  return (
    <Phone label="20 Account">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)", overflow: "auto" }} className="rs-scroll">
        <div style={{ padding: "20px 20px 24px", background: "var(--accent)", color: "#fff", borderBottomLeftRadius: 28, borderBottomRightRadius: 28 }}>
          <div style={{ display: "flex", justifyContent: "space-between" }}>
            <button style={{ width: 40, height: 40, borderRadius: 20, background: "rgba(255,255,255,.2)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="back" size={20} color="#fff"/>
            </button>
            <button style={{ width: 40, height: 40, borderRadius: 20, background: "rgba(255,255,255,.2)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="settings" size={18} color="#fff"/>
            </button>
          </div>
          <div style={{ marginTop: 18, display: "flex", alignItems: "center", gap: 14 }}>
            <Avatar name="Nimali P" size={64} style={{ border: "3px solid #fff" }}/>
            <div style={{ flex: 1 }}>
              <div className="rs-display" style={{ fontSize: 22 }}>Nimali Perera</div>
              <div style={{ fontSize: 12, opacity: .85, display: "flex", alignItems: "center", gap: 6 }}>
                <Icon name="star" size={12} color="#fff"/> 4.92 · 38 trips
              </div>
            </div>
          </div>
          <div style={{ marginTop: 18, display: "flex", gap: 8 }}>
            <div style={{ flex: 1, padding: "10px 12px", background: "rgba(255,255,255,.18)", borderRadius: 14 }}>
              <div style={{ fontSize: 10, letterSpacing: ".1em", opacity: .85 }}>WALLET</div>
              <div style={{ fontWeight: 700, fontSize: 16 }}>LKR 1,250</div>
            </div>
            <div style={{ flex: 1, padding: "10px 12px", background: "rgba(255,255,255,.18)", borderRadius: 14 }}>
              <div style={{ fontSize: 10, letterSpacing: ".1em", opacity: .85 }}>SAVED</div>
              <div style={{ fontWeight: 700, fontSize: 16 }}>LKR 3,440</div>
            </div>
          </div>
        </div>

        <div style={{ padding: "18px 20px 20px", display: "flex", flexDirection: "column", gap: 6 }}>
          <MenuRow icon="pin" label="Saved places" sub="Home, Office + 2"/>
          <MenuRow icon="card" label="Payment methods" sub="Visa •••• 4429"/>
          <MenuRow icon="history" label="Trip history" sub="14 this month"/>
          <MenuRow icon="receipt" label="Receipts & invoices"/>
          <MenuRow icon="bell" label="Notifications" sub="SMS, Push"/>

          <div className="rs-section-label" style={{ marginTop: 14, padding: "0 4px" }}>SAFETY</div>
          <MenuRow icon="shield" label="Trusted contacts" sub="3 added"/>
          <MenuRow icon="users" label="Ride preferences" sub="Female drivers preferred"/>
          <MenuRow icon="lock" label="Privacy & data"/>

          <div className="rs-section-label" style={{ marginTop: 14, padding: "0 4px" }}>SUPPORT</div>
          <MenuRow icon="help" label="Help center"/>
          <MenuRow icon="alert" label="Report an issue"/>
          <MenuRow icon="thumb" label="Rate the app"/>

          <button style={{ marginTop: 18, padding: "14px", textAlign: "center", color: "var(--danger)", fontWeight: 700, fontSize: 14 }}>Sign out</button>
          <div style={{ textAlign: "center", fontSize: 11, color: "var(--ink-4)" }}>v1.0.0 · RouteShare · Colombo</div>
        </div>
      </div>
    </Phone>
  );
}

function MenuRow({ icon, label, sub }) {
  return (
    <div style={{ padding: "12px 4px", display: "flex", alignItems: "center", gap: 14 }}>
      <div style={{ width: 36, height: 36, borderRadius: 12, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
        <Icon name={icon} size={18}/>
      </div>
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 14, fontWeight: 600 }}>{label}</div>
        {sub && <div style={{ fontSize: 11, color: "var(--ink-3)" }}>{sub}</div>}
      </div>
      <Icon name="chev" size={16} color="var(--ink-4)"/>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════
// SOS / EMERGENCY
// ═══════════════════════════════════════════════════════════════════
function SosScreen() {
  return (
    <Phone label="21 SOS">
      <div style={{ height: "100%", background: "linear-gradient(180deg, #c0392b 0%, #8a1f14 100%)", color: "#fff", display: "flex", flexDirection: "column", padding: "14px 20px 20px" }}>
        <div style={{ display: "flex", justifyContent: "space-between" }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "rgba(255,255,255,.18)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="close" size={20} color="#fff"/>
          </button>
          <div style={{ height: 40, padding: "0 12px", background: "rgba(255,255,255,.18)", borderRadius: 20, display: "flex", alignItems: "center", gap: 6, fontSize: 11, fontWeight: 700 }}>
            <div style={{ width: 8, height: 8, borderRadius: 4, background: "#fff", animation: "pulse 1s infinite" }}/> LOCATION SHARING ON
          </div>
        </div>

        <div style={{ textAlign: "center", marginTop: 20 }}>
          <div className="rs-display" style={{ fontSize: 32, letterSpacing: "-0.02em" }}>Need help?</div>
          <div style={{ marginTop: 6, fontSize: 14, opacity: .9, maxWidth: 280, margin: "6px auto 0" }}>
            Hold the button for 3 seconds to connect to emergency services and alert your trusted contacts.
          </div>
        </div>

        <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center", position: "relative" }}>
          <div style={{ position: "absolute", width: 230, height: 230, borderRadius: "50%", background: "rgba(255,255,255,.08)" }}/>
          <div style={{ position: "absolute", width: 180, height: 180, borderRadius: "50%", background: "rgba(255,255,255,.12)" }}/>
          <button style={{ width: 140, height: 140, borderRadius: "50%", background: "#fff", color: "#c0392b", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", boxShadow: "0 10px 40px rgba(0,0,0,.3)" }}>
            <Icon name="alert" size={40} color="#c0392b" strokeWidth={2.5}/>
            <div className="rs-display" style={{ fontSize: 26, marginTop: 4, fontWeight: 600 }}>SOS</div>
            <div style={{ fontSize: 10, fontWeight: 700, opacity: .6, letterSpacing: ".1em" }}>HOLD 3s</div>
          </button>
        </div>

        <div style={{ padding: 14, background: "rgba(0,0,0,.22)", borderRadius: 18, marginTop: 16 }}>
          <div style={{ fontSize: 11, opacity: .85, fontWeight: 700, letterSpacing: ".08em", marginBottom: 10 }}>QUICK ACTIONS</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            <div style={{ padding: "12px 14px", background: "rgba(255,255,255,.12)", borderRadius: 12, display: "flex", alignItems: "center", gap: 12 }}>
              <Icon name="phone" size={18} color="#fff"/>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 13, fontWeight: 700 }}>Call Police (119)</div>
                <div style={{ fontSize: 11, opacity: .75 }}>Sri Lanka Police emergency line</div>
              </div>
              <Icon name="chev" size={14} color="#fff"/>
            </div>
            <div style={{ padding: "12px 14px", background: "rgba(255,255,255,.12)", borderRadius: 12, display: "flex", alignItems: "center", gap: 12 }}>
              <Icon name="share" size={18} color="#fff"/>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 13, fontWeight: 700 }}>Alert trusted contacts</div>
                <div style={{ fontSize: 11, opacity: .75 }}>Amma, Dilruk, Maya</div>
              </div>
              <Icon name="chev" size={14} color="#fff"/>
            </div>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// SHARE TRIP
// ═══════════════════════════════════════════════════════════════════
function ShareTripScreen() {
  return (
    <Phone label="22 Share Trip">
      <div style={{ height: "100%", position: "relative", background: "rgba(20,10,5,.35)" }}>
        <MapBackdrop/>
        <div style={{ position: "absolute", inset: 0, background: "rgba(20,10,5,.45)" }}/>
        <div style={{ position: "absolute", left: 0, right: 0, bottom: 0 }} className="rs-sheet">
          <div className="rs-sheet-grab"/>
          <div style={{ padding: "4px 22px 20px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <div>
                <div className="rs-display" style={{ fontSize: 22 }}>Share this trip</div>
                <div style={{ fontSize: 12, color: "var(--ink-3)" }}>Trusted contacts see live location until drop-off.</div>
              </div>
              <button style={{ width: 32, height: 32, borderRadius: 16, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="close" size={16}/>
              </button>
            </div>

            <div className="rs-card" style={{ padding: 14, marginTop: 14, display: "flex", gap: 10, background: "var(--teal-soft)", border: "none" }}>
              <Icon name="shield" size={18} color="var(--teal)"/>
              <div style={{ fontSize: 12, color: "var(--teal)", lineHeight: 1.4 }}>
                <b>Trip ID:</b> RS-4429 · 6.2 km<br/>
                Driver: Saman W · Toyota Aqua CAR-2211 · ETA 8:22 AM
              </div>
            </div>

            <div className="rs-section-label" style={{ margin: "18px 0 10px" }}>TRUSTED CONTACTS</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              {[
                { name: "Amma", ph: "+94 77 222 4401", on: true },
                { name: "Dilruk", ph: "+94 77 555 2230", on: true },
                { name: "Maya", ph: "+94 77 889 1102", on: false },
              ].map(c => (
                <div key={c.name} style={{ padding: 12, background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 14, display: "flex", alignItems: "center", gap: 12 }}>
                  <Avatar name={c.name} size={36}/>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontSize: 14, fontWeight: 700 }}>{c.name}</div>
                    <div style={{ fontSize: 11, color: "var(--ink-3)" }}>{c.ph}</div>
                  </div>
                  <div style={{ width: 36, height: 20, borderRadius: 10, background: c.on ? "var(--success)" : "var(--line-2)", padding: 2, display: "flex", justifyContent: c.on ? "flex-end" : "flex-start" }}>
                    <div style={{ width: 16, height: 16, borderRadius: 8, background: "#fff" }}/>
                  </div>
                </div>
              ))}
            </div>

            <div style={{ display: "flex", gap: 10, marginTop: 18 }}>
              <button className="rs-btn soft" style={{ flex: 1 }}><Icon name="share" size={16}/> Copy link</button>
              <button className="rs-btn accent" style={{ flex: 1.2 }}>Share now</button>
            </div>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// NOTIFICATIONS
// ═══════════════════════════════════════════════════════════════════
function NotificationsScreen() {
  const items = [
    { t: "now", icon: "car", tint: "var(--accent-soft)", fg: "var(--accent-2)", title: "Saman is 3 min away", body: "Heading to Narahenpita Junction in a silver Toyota Aqua.", unread: true },
    { t: "2 min ago", icon: "check", tint: "var(--success-soft)", fg: "var(--match-full)", title: "Booking confirmed", body: "Seat 2 for the 8:04 AM ride to Bambalapitiya.", unread: true },
    { t: "Yesterday", icon: "receipt", tint: "var(--teal-soft)", fg: "var(--teal)", title: "Receipt · LKR 240", body: "Trip from WTC to Rajagiriya. Tap to download." },
    { t: "Yesterday", icon: "star", tint: "var(--bg-soft)", fg: "var(--warn)", title: "How was your ride with Kasun?", body: "Leave a quick rating." },
    { t: "22 Apr", icon: "leaf", tint: "var(--teal-soft)", fg: "var(--teal)", title: "You're a commuter!", body: "You've saved 14 kg of CO₂ this month by sharing rides." },
    { t: "20 Apr", icon: "bell", tint: "var(--bg-soft)", fg: "var(--ink-3)", title: "Weekly summary is ready", body: "LKR 1,180 spent on 6 trips — see breakdown." },
  ];
  return (
    <Phone label="23 Notifications">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "14px 20px", display: "flex", alignItems: "center", gap: 12, background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="back" size={20}/>
          </button>
          <div style={{ flex: 1, fontSize: 18, fontWeight: 700 }}>Notifications</div>
          <button style={{ fontSize: 13, color: "var(--accent-2)", fontWeight: 700 }}>Mark read</button>
        </div>
        <div style={{ flex: 1, overflow: "auto" }} className="rs-scroll">
          {items.map((n, i) => (
            <div key={i} style={{ padding: "14px 20px", display: "flex", gap: 12, borderBottom: "1px solid var(--line)", background: n.unread ? "var(--accent-soft)" : "transparent" }}>
              <div style={{ width: 40, height: 40, borderRadius: 20, background: n.tint, display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                <Icon name={n.icon} size={18} color={n.fg}/>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
                  <div style={{ fontSize: 14, fontWeight: 700 }}>{n.title}</div>
                  <div style={{ fontSize: 11, color: "var(--ink-4)", flexShrink: 0 }}>{n.t}</div>
                </div>
                <div style={{ fontSize: 12, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>{n.body}</div>
              </div>
              {n.unread && <div style={{ width: 8, height: 8, borderRadius: 4, background: "var(--accent)", marginTop: 6, flexShrink: 0 }}/>}
            </div>
          ))}
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// SUPPORT / HELP
// ═══════════════════════════════════════════════════════════════════
function SupportScreen() {
  return (
    <Phone label="24 Support">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "14px 20px", display: "flex", alignItems: "center", gap: 12, background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="back" size={20}/>
          </button>
          <div style={{ flex: 1, fontSize: 18, fontWeight: 700 }}>Help center</div>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "16px 20px" }} className="rs-scroll">
          <div style={{ padding: 14, background: "var(--surface)", border: "1.5px solid var(--line)", borderRadius: 14, display: "flex", alignItems: "center", gap: 10 }}>
            <Icon name="search" size={18} color="var(--ink-3)"/>
            <div style={{ flex: 1, color: "var(--ink-4)", fontSize: 14 }}>Search "refund", "cancel"…</div>
          </div>

          <div style={{ marginTop: 14, padding: 16, background: "var(--accent-soft)", borderRadius: 16, display: "flex", gap: 12, alignItems: "center" }}>
            <div style={{ width: 44, height: 44, borderRadius: 14, background: "#fff", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="receipt" size={20} color="var(--accent-2)"/>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 13, fontWeight: 700 }}>Your last trip · LKR 186</div>
              <div style={{ fontSize: 11, color: "var(--ink-3)" }}>Narahenpita → Kollupitiya · 8:04 AM</div>
            </div>
            <Icon name="chev" size={16} color="var(--ink-3)"/>
          </div>

          <div className="rs-section-label" style={{ marginTop: 18, marginBottom: 10 }}>TOP TOPICS</div>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
            <TopicCard icon="receipt" label="Fare & receipts"/>
            <TopicCard icon="users" label="Bookings & seats"/>
            <TopicCard icon="card" label="Payments"/>
            <TopicCard icon="shield" label="Safety"/>
            <TopicCard icon="route" label="Route matching"/>
            <TopicCard icon="user" label="Account"/>
          </div>

          <div className="rs-section-label" style={{ marginTop: 18, marginBottom: 8 }}>CONTACT US</div>
          <div style={{ display: "flex", flexDirection: "column" }}>
            {[
              { icon: "phone", label: "Call support", sub: "24/7 · +94 11 777 0000", fg: "var(--teal)" },
              { icon: "mail", label: "Email us", sub: "help@routeshare.lk · reply in 4h", fg: "var(--ink-2)" },
              { icon: "alert", label: "Report an incident", sub: "Safety issues get priority", fg: "var(--danger)" },
            ].map((c, i) => (
              <div key={i} style={{ padding: "14px 0", display: "flex", alignItems: "center", gap: 12, borderBottom: "1px solid var(--line)" }}>
                <Icon name={c.icon} size={20} color={c.fg}/>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 14, fontWeight: 700 }}>{c.label}</div>
                  <div style={{ fontSize: 11, color: "var(--ink-3)" }}>{c.sub}</div>
                </div>
                <Icon name="chev" size={14} color="var(--ink-4)"/>
              </div>
            ))}
          </div>
        </div>
      </div>
    </Phone>
  );
}

function TopicCard({ icon, label }) {
  return (
    <div className="rs-card" style={{ padding: 14, display: "flex", flexDirection: "column", gap: 10, minHeight: 92 }}>
      <div style={{ width: 36, height: 36, borderRadius: 12, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
        <Icon name={icon} size={18}/>
      </div>
      <div style={{ fontSize: 13, fontWeight: 700, lineHeight: 1.2 }}>{label}</div>
    </div>
  );
}

Object.assign(window, { TripHistoryScreen, SavedPlacesScreen, AccountScreen, SosScreen, ShareTripScreen, NotificationsScreen, SupportScreen });
