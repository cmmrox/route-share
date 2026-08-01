// shell.jsx — ComiGo shell: splash, onboarding, auth, mode switcher, tab bars, conflict states

// ── mode identity helper ──
function modeTint(mode) {
  return mode === "drive"
    ? { c: "var(--mode-drive)", ink: "var(--mode-drive-ink)", soft: "var(--mode-drive-soft)", label: "Driving", other: "Riding", otherMode: "ride", icon: "car" }
    : { c: "var(--mode-ride)", ink: "var(--mode-ride-ink)", soft: "var(--mode-ride-soft)", label: "Riding", other: "Driving", otherMode: "drive", icon: "users" };
}

// ═══════════════════════ TAB BAR ═══════════════════════
// Structurally identical in both modes: Home · Trips · [mode action] · Inbox · Account.
// Only the centre action and the tab contents change.
function TabBar({ mode = "ride", active = "home", badges = {} }) {
  const t = modeTint(mode);
  const tabs = [
    { key: "home", icon: "home", label: "Home" },
    { key: "trips", icon: "history", label: "Trips" },
    { key: "action", icon: mode === "drive" ? "plus" : "search", label: mode === "drive" ? "Publish" : "Find" },
    { key: "inbox", icon: "bell", label: "Inbox" },
    { key: "account", icon: "user", label: "Account" },
  ];
  return (
    <div style={{
      // position:relative is a web-only stacking fix: on map screens an absolutely
      // positioned MapBackdrop would otherwise paint over this static bar. RN paints
      // later siblings on top by default, so this has no RN equivalent — ignore it.
      position: "relative", zIndex: 2,
      display: "flex", alignItems: "flex-start", justifyContent: "space-between",
      padding: "8px 10px 10px", background: "var(--surface)",
      borderTop: "1px solid var(--line)", boxShadow: "var(--shadow-tabbar)", flexShrink: 0,
    }}>
      {tabs.map(tab => {
        const isAction = tab.key === "action";
        const on = active === tab.key;
        const badge = badges[tab.key];
        if (isAction) return (
          <div key={tab.key} data-tab={tab.key} data-tabmode={mode} style={{ width: 62, display: "flex", flexDirection: "column", alignItems: "center", gap: 4 }}>
            <div style={{ width: 46, height: 40, borderRadius: 14, background: t.c, display: "flex", alignItems: "center", justifyContent: "center", boxShadow: "var(--shadow-switch)" }}>
              <Icon name={tab.icon} size={21} color="#fff" strokeWidth={2.2}/>
            </div>
            <div style={{ fontSize: 10, fontWeight: 700, color: t.ink, letterSpacing: ".01em" }}>{tab.label}</div>
          </div>
        );
        return (
          <div key={tab.key} data-tab={tab.key} data-tabmode={mode} style={{ width: 62, minHeight: 44, display: "flex", flexDirection: "column", alignItems: "center", gap: 5, paddingTop: 5 }}>
            <div style={{ position: "relative" }}>
              <Icon name={tab.icon} size={22} color={on ? t.c : "var(--ink-3)"} strokeWidth={on ? 2.3 : 1.8}/>
              {badge != null && (
                <div style={{
                  position: "absolute", top: -4, right: -7, minWidth: badge === true ? 8 : 17, height: badge === true ? 8 : 17,
                  borderRadius: 999, background: "var(--danger)", color: "var(--on-bright-fill)",
                  fontSize: 10.5, fontWeight: 800, display: "flex", alignItems: "center", justifyContent: "center",
                  padding: badge === true ? 0 : "0 4px", border: "1.5px solid var(--surface)",
                }}>{badge === true ? "" : badge}</div>
              )}
            </div>
            <div style={{ fontSize: 10, fontWeight: on ? 700 : 600, color: on ? t.ink : "var(--ink-3)" }}>{tab.label}</div>
          </div>
        );
      })}
    </div>
  );
}

// ═══════════════════════ MODE CHIP (the switcher) ═══════════════════════
// state: "approved" (one tap swaps) · "pending" · "rejected" · "none" (no chip)
function ModeChip({ mode = "ride", state = "approved", compact = false }) {
  const t = modeTint(mode);
  if (state === "none") return null;
  const gated = state === "pending" || state === "rejected";
  const gc = state === "pending" ? "var(--status-pending)" : "var(--status-rejected)";
  const gink = state === "pending" ? "var(--status-pending-ink)" : "var(--status-rejected-ink)";
  const gsoft = state === "pending" ? "var(--status-pending-soft)" : "var(--status-rejected-soft)";
  return (
    <div data-row={gated ? (state === "pending" ? "mode chip in review" : "mode chip action needed") : `switch to ${t.other}`} style={{
      display: "inline-flex", alignItems: "center", gap: 8, height: 44, padding: "0 6px 0 12px",
      borderRadius: 999, background: gated ? gsoft : t.soft, border: `1px solid ${gated ? gc : "transparent"}`,
    }}>
      <Icon name={gated ? "lock" : t.icon} size={16} color={gated ? gink : t.c} strokeWidth={2.1}/>
      <div style={{ display: "flex", flexDirection: "column", lineHeight: 1.15 }}>
        <div style={{ fontSize: 13, fontWeight: 800, color: gated ? gink : t.ink }}>{gated ? "Driver" : t.label}</div>
        {!compact && <div style={{ fontSize: 10.5, fontWeight: 700, letterSpacing: ".04em", color: gated ? gink : t.ink }}>
          {state === "pending" ? "IN REVIEW" : state === "rejected" ? "ACTION NEEDED" : `TAP FOR ${t.other.toUpperCase()}`}
        </div>}
      </div>
      <div style={{ width: 32, height: 32, borderRadius: 16, background: gated ? "transparent" : "rgba(255,255,255,.75)", display: "flex", alignItems: "center", justifyContent: "center" }}>
        <Icon name={gated ? "chev" : "swap"} size={15} color={gated ? gink : t.c} strokeWidth={2.2}/>
      </div>
    </div>
  );
}

// ── shared header used by both home surfaces ──
function HomeHeader({ mode = "ride", state = "approved", name = "Nimali" }) {
  return (
    <div style={{ padding: "10px 16px 12px", display: "flex", alignItems: "center", gap: 10 }}>
      <ModeChip mode={mode} state={state}/>
      <div style={{ flex: 1 }}/>
      <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", position: "relative" }} aria-label="Notifications">
        <Icon name="bell" size={20}/>
        <div style={{ position: "absolute", top: 10, right: 11, width: 8, height: 8, borderRadius: 4, background: "var(--danger)", border: "1.5px solid var(--bg-soft)" }}/>
      </button>
      <Avatar name={name} size={44}/>
    </div>
  );
}

// ── condensed home bodies, used to evaluate switcher placement in context ──
function RideHomeBody({ promo = false }) {
  return (
    <div style={{ flex: 1, overflow: "hidden", padding: "0 16px 14px", display: "flex", flexDirection: "column", gap: 12 }}>
      <div className="rs-display" style={{ fontSize: 27, lineHeight: 1.15, marginTop: 2 }}>Where are you<br/>heading, Nimali?</div>
      <button style={{ width: "100%", height: 56, padding: "0 16px", display: "flex", alignItems: "center", gap: 12, background: "var(--surface)", border: "1.5px solid var(--line)", borderRadius: 16 }}>
        <Icon name="search" size={18} color="var(--ink-3)"/>
        <span style={{ color: "var(--ink-3)", fontSize: 15 }}>Enter destination</span>
        <span style={{ marginLeft: "auto", display: "inline-flex", alignItems: "center", gap: 5, fontSize: 12, fontWeight: 700, padding: "5px 10px", borderRadius: 999, background: "var(--bg-soft)" }}>
          <Icon name="clock" size={12}/> Now
        </span>
      </button>
      <div style={{ display: "flex", gap: 8 }}>
        <button className="rs-tap"><span className="rs-chip"><Icon name="home" size={13}/> Nugegoda</span></button>
        <button className="rs-tap"><span className="rs-chip"><Icon name="briefcase" size={13}/> Colombo Fort</span></button>
      </div>
      {promo ? <BecomeDriverPromo/> : (
        <div className="rs-card" style={{ padding: 14, display: "flex", alignItems: "center", gap: 12 }}>
          <Avatar name="Saman W" size={42}/>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", color: "var(--mode-ride-ink)" }}>TOMORROW · 8:04 AM</div>
            <div style={{ fontSize: 13.5, fontWeight: 700, marginTop: 2 }}>{RIDES[0].from} → {RIDES[0].to}</div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)" }}>{RIDES[0].driver} · 1 seat · {FARE_POLICY.currency} {money(RIDES[0].price)}</div>
          </div>
          <MatchRing value={100} size={44} strokeWidth={3.5}/>
        </div>
      )}
    </div>
  );
}

function DriveHomeBody() {
  return (
    <div style={{ flex: 1, overflow: "hidden", padding: "0 16px 14px", display: "flex", flexDirection: "column", gap: 12 }}>
      <div style={{ padding: 18, borderRadius: 20, background: "var(--ink-fill)", color: "#f4ece0" }}>
        <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em", opacity: .65 }}>EARNED TODAY</div>
        <div className="rs-display tab" style={{ fontSize: 38, lineHeight: 1.05, marginTop: 4 }}>{FARE_POLICY.currency} {money(earnedToday())}</div>
        <div style={{ display: "flex", gap: 18, marginTop: 12, fontSize: 11.5 }}>
          <div><span style={{ opacity: .6 }}>This week </span><span className="tab" style={{ fontWeight: 700 }}>{FARE_POLICY.currency} {money(DRIVER_TODAY.weekTotal)}</span></div>
          <div><span style={{ opacity: .6 }}>Rating </span><span className="tab" style={{ fontWeight: 700 }}>{DRIVER_TODAY.rating}</span></div>
        </div>
      </div>
      <div className="rs-card" style={{ padding: 14, display: "flex", alignItems: "center", gap: 12 }}>
        <div style={{ width: 42, height: 42, borderRadius: 14, background: "var(--mode-drive-soft)", display: "flex", alignItems: "center", justifyContent: "center" }}>
          <Icon name="route" size={20} color="var(--mode-drive)"/>
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", color: "var(--mode-drive-ink)" }}>NEXT TRIP · {NEXT_DRIVE.depart} · IN {NEXT_DRIVE.inMin} MIN</div>
          <div style={{ fontSize: 13.5, fontWeight: 700, marginTop: 2 }}>{NEXT_DRIVE.from} → {NEXT_DRIVE.to}</div>
          <div style={{ fontSize: 11.5, color: "var(--ink-3)" }}>{NEXT_DRIVE.seatsBooked} of {NEXT_DRIVE.seatsTotal} seats booked · {FARE_POLICY.currency} {money(NEXT_DRIVE.netExpected)} to you</div>
        </div>
        <Icon name="chev" size={18} color="var(--ink-3)"/>
      </div>
      <div style={{ display: "flex", gap: 8 }}>
        <button className="rs-tap"><span className="rs-chip teal"><Icon name="users" size={13}/> {NEXT_DRIVE.requests} request</span></button>
        <button className="rs-tap"><span className="rs-chip"><Icon name="calendar" size={13}/> {NEXT_DRIVE.recurring} recurring</span></button>
      </div>
    </div>
  );
}

function BecomeDriverPromo() {
  return (
    <div data-row="become a driver promo" style={{ padding: 16, borderRadius: 20, background: "var(--mode-drive-soft)", border: "1px solid var(--line)", display: "flex", gap: 14, alignItems: "center" }}>
      <div style={{ width: 46, height: 46, borderRadius: 14, background: "var(--mode-drive)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
        <Icon name="car" size={22} color="#fff"/>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 800, color: "var(--mode-drive-ink)" }}>Driving this way anyway?</div>
        <div style={{ fontSize: 12, color: "var(--ink-2)", marginTop: 3, lineHeight: 1.45 }}>Publish your commute and let riders book the empty seats. Same account, no second app.</div>
        <div style={{ marginTop: 10, display: "inline-flex", alignItems: "center", gap: 6, height: 36, padding: "0 14px", borderRadius: 999, background: "var(--mode-drive)", color: "var(--on-bright-fill)", fontSize: 12.5, fontWeight: 700 }}>
          Start earning <Icon name="arrow" size={14} color="var(--on-bright-fill)"/>
        </div>
      </div>
    </div>
  );
}

// ═══════════════════════ S01 · SPLASH ═══════════════════════
function CgSplashScreen() {
  return (
    <Phone label="S01 Splash" statusDark statusBg="#1b1410">
      <div style={{ height: "100%", background: "#1b1410", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 22 }}>
        <Lockup Mark={MarkOverlap} size={46} tone="onDark" stacked tagline="Colombo"/>
        <div style={{ width: 48, height: 3, borderRadius: 2, background: "#3a3128", overflow: "hidden", marginTop: 6 }}>
          <div style={{ width: "60%", height: "100%", background: "#e8834f", borderRadius: 2 }}/>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════ S02 · ONBOARDING (one carousel, both sides) ═══════════════════════
const ONB = [
  {
    kicker: "HOW COMIGO WORKS", title: "Two trips,\none stretch of road.",
    body: "Drivers publish journeys they're already making. You book the part of their route that matches yours — and pay only for that stretch.",
    art: "overlap",
  },
  {
    kicker: "AS A RIDER", title: "Ride along\nfor less.",
    body: "Search your route, compare match percentages, pick a seat. A full match takes you door to door; a partial match costs less and drops you close by.",
    art: "ride",
  },
  {
    kicker: "AS A DRIVER", title: "Your empty seats\ncan pay for petrol.",
    body: "Publish your commute once, repeat it every weekday, and let riders book the seats you weren't using. Switch between riding and driving any time.",
    art: "drive",
  },
  {
    kicker: "EITHER WAY", title: "Everyone here\nis verified.",
    body: "Identity and licence checks for every driver. Share your live trip with family, and reach emergency services or our team from any screen.",
    art: "safe",
  },
];

function OnbArt({ kind }) {
  const box = { width: "100%", height: 210, borderRadius: 24, background: "var(--bg-soft)", position: "relative", overflow: "hidden", display: "flex", alignItems: "center", justifyContent: "center" };
  if (kind === "overlap") return (
    <div style={box}>
      <svg width="300" height="170" viewBox="0 0 300 170" fill="none">
        <path d="M18 140C70 132 92 96 140 82" stroke="var(--ink)" strokeWidth="7" strokeLinecap="round"/>
        <path d="M196 66C232 56 258 44 282 30" stroke="var(--ink)" strokeWidth="7" strokeLinecap="round"/>
        <path d="M74 24C96 48 112 66 128 84" stroke="var(--mode-drive)" strokeWidth="5" strokeLinecap="round"/>
        <path d="M194 68C210 96 220 118 228 150" stroke="var(--mode-drive)" strokeWidth="5" strokeLinecap="round"/>
        <path d="M132 84C152 78 172 72 192 67" stroke="var(--accent)" strokeWidth="12" strokeLinecap="round"/>
        <circle cx="132" cy="84" r="5.5" fill="var(--surface)" stroke="var(--accent)" strokeWidth="3"/>
        <circle cx="192" cy="67" r="5.5" fill="var(--surface)" stroke="var(--accent)" strokeWidth="3"/>
      </svg>
      <div style={{ position: "absolute", left: "50%", bottom: 26, transform: "translateX(-50%)", display: "inline-flex", alignItems: "center", gap: 6, height: 30, padding: "0 12px", borderRadius: 999, background: "var(--surface)", boxShadow: "var(--shadow-sm)", fontSize: 12, fontWeight: 800 }}>
        <span style={{ color: "var(--accent-ink)" }}>68%</span>
        <span style={{ color: "var(--ink-3)", fontWeight: 600 }}>shared · you pay for this bit</span>
      </div>
    </div>
  );
  if (kind === "ride") return (
    <div style={box}>
      <div style={{ display: "flex", flexDirection: "column", gap: 10, width: 250 }}>
        {[RIDES[0], RIDES[2]].map(r => ({ m: r.match, n: r.driver, p: money(r.price), s: r.match >= 95 ? "Full route" : "Walk 450 m at the end" })).map(r => (
          <div key={r.n} style={{ background: "var(--surface)", borderRadius: 16, padding: 12, display: "flex", alignItems: "center", gap: 10, boxShadow: "var(--shadow-sm)" }}>
            <MatchRing value={r.m} size={42} strokeWidth={3.5}/>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 12.5, fontWeight: 700 }}>{r.n}</div>
              <div style={{ fontSize: 10.5, color: "var(--ink-3)" }}>{r.s}</div>
            </div>
            <div className="tab" style={{ fontSize: 13, fontWeight: 800 }}>LKR {r.p}</div>
          </div>
        ))}
      </div>
    </div>
  );
  if (kind === "drive") return (
    <div style={box}>
      <div style={{ display: "flex", alignItems: "center", gap: 20 }}>
        <SeatPlan taken={[0]} selected={[]} capacity={4}/>
        <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
          <div style={{ background: "var(--surface)", borderRadius: 14, padding: "10px 14px", boxShadow: "var(--shadow-sm)" }}>
            <div style={{ fontSize: 9.5, fontWeight: 800, letterSpacing: ".1em", color: "var(--ink-3)" }}>THIS WEEK</div>
            <div className="rs-display tab" style={{ fontSize: 22, marginTop: 2 }}>{FARE_POLICY.currency} {money(DRIVER_TODAY.weekTotal)}</div>
          </div>
          <div className="rs-chip teal" style={{ height: 28 }}>{MY_VEHICLE.seats - 1} seats free</div>
        </div>
      </div>
    </div>
  );
  return (
    <div style={box}>
      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 14 }}>
        <div style={{ width: 74, height: 74, borderRadius: 24, background: "var(--surface)", display: "flex", alignItems: "center", justifyContent: "center", boxShadow: "var(--shadow-sm)" }}>
          <Icon name="shield" size={34} color="var(--mode-drive)"/>
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          <div className="rs-chip success" style={{ height: 28 }}><Icon name="check" size={12}/> Licence checked</div>
          <div className="rs-chip" style={{ height: 28 }}><Icon name="share" size={12}/> Share trip</div>
        </div>
        <div className="rs-chip" style={{ height: 28, background: "var(--status-rejected-soft)", color: "var(--status-rejected-ink)", borderColor: "transparent" }}><Icon name="sos" size={12}/> 119 &amp; ComiGo ops</div>
      </div>
    </div>
  );
}

function CgOnboardingScreen({ step = 0 }) {
  const s = ONB[step];
  return (
    <Phone label={`S02${"abcd"[step]} Onboarding · ${step + 1}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)", padding: "8px 20px 20px" }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", height: 44 }}>
          <Lockup Mark={MarkOverlap} size={26}/>
          <button style={{ fontSize: 13.5, fontWeight: 700, color: "var(--ink-3)", minHeight: 44, minWidth: 44, padding: "0 8px" }}>Skip</button>
        </div>
        <div style={{ marginTop: 8 }}><OnbArt kind={s.art}/></div>
        <div style={{ marginTop: 24, flex: 1 }}>
          <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".14em", color: "var(--accent-ink)" }}>{s.kicker}</div>
          <div className="rs-display" style={{ fontSize: 29, lineHeight: 1.16, marginTop: 10, whiteSpace: "pre-line" }}>{s.title}</div>
          <div style={{ fontSize: 14, color: "var(--ink-3)", marginTop: 12, lineHeight: 1.6, textWrap: "pretty" }}>{s.body}</div>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
          <div style={{ display: "flex", gap: 6, flex: 1 }}>
            {ONB.map((_, i) => (
              <div key={i} style={{ width: i === step ? 22 : 7, height: 7, borderRadius: 999, background: i === step ? "var(--accent)" : "var(--line-2)" }}/>
            ))}
          </div>
          <button className="rs-btn accent" aria-label={step === ONB.length - 1 ? undefined : "Next slide"} style={{ width: step === ONB.length - 1 ? "auto" : 56, padding: step === ONB.length - 1 ? "0 22px" : 0 }}>
            {step === ONB.length - 1 ? <>Get started <Icon name="arrow" size={17} color="#fff"/></> : <Icon name="arrow" size={20} color="#fff"/>}
          </button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════ S03 · LOGIN (phone OTP only) ═══════════════════════
function CgLoginScreen() {
  return (
    <Phone label="S03 Sign in">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", padding: "8px 24px 24px", background: "var(--bg)" }}>
        <div style={{ height: 44, display: "flex", alignItems: "center" }}>
          <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", marginLeft: -6 }} aria-label="Back">
            <Icon name="back" size={20}/>
          </button>
        </div>
        <div style={{ marginTop: 26 }}>
          <MarkOverlap size={44}/>
          <div className="rs-display" style={{ fontSize: 30, marginTop: 18, lineHeight: 1.15 }}>Your number,<br/>and you're in.</div>
          <div style={{ color: "var(--ink-3)", marginTop: 10, fontSize: 14, lineHeight: 1.55 }}>One ComiGo account for riding and driving. We'll text you a 6-digit code.</div>
        </div>
        <div style={{ marginTop: 26 }}>
          <div className="rs-section-label" style={{ marginBottom: 8 }}>MOBILE NUMBER</div>
          <div style={{ display: "flex", gap: 10 }}>
            <div style={{ height: 56, minWidth: 84, padding: "0 14px", borderRadius: 16, background: "var(--surface)", border: "1.5px solid var(--line)", display: "flex", alignItems: "center", gap: 8, fontWeight: 700, fontSize: 15 }}>
              <span style={{ fontSize: 17 }}>🇱🇰</span> +94
            </div>
            <div style={{ flex: 1, height: 56, padding: "0 16px", borderRadius: 16, background: "var(--surface)", border: "1.5px solid var(--ink)", display: "flex", alignItems: "center" }}>
              <span className="tab" style={{ fontSize: 17, fontWeight: 700, letterSpacing: ".04em" }}>77 412 8890</span>
              <span style={{ width: 2, height: 22, background: "var(--accent)", marginLeft: 3, animation: "blink 1s step-end infinite" }}/>
            </div>
          </div>
          <div style={{ fontSize: 12, color: "var(--ink-3)", marginTop: 10, lineHeight: 1.5 }}>Standard SMS rates may apply.</div>
        </div>
        <div style={{ flex: 1 }}/>
        <button className="rs-btn accent full">Send code</button>
        <div style={{ textAlign: "center", marginTop: 14, fontSize: 11.5, color: "var(--ink-3)", lineHeight: 1.6 }}>
          By continuing you agree to ComiGo's <span style={{ color: "var(--ink-2)", fontWeight: 700 }}>Terms</span> and <span style={{ color: "var(--ink-2)", fontWeight: 700 }}>Privacy Policy</span>.
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════ S04 · OTP ═══════════════════════
function CgOtpScreen() {
  const digits = ["4", "9", "2", "7", "", ""];
  return (
    <Phone label="S04 Verify code">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", padding: "8px 24px 24px", background: "var(--bg)" }}>
        <div style={{ height: 44, display: "flex", alignItems: "center" }}>
          <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", marginLeft: -6 }} aria-label="Back">
            <Icon name="back" size={20}/>
          </button>
        </div>
        <div style={{ marginTop: 26 }}>
          <div className="rs-display" style={{ fontSize: 30, lineHeight: 1.15 }}>Enter the code</div>
          <div style={{ color: "var(--ink-3)", marginTop: 10, fontSize: 14, lineHeight: 1.55 }}>
            Sent to <span style={{ color: "var(--ink)", fontWeight: 700 }} className="tab">+94 77 412 8890</span>
            <button style={{ color: "var(--accent-ink)", fontWeight: 700, marginLeft: 8, minHeight: 44, minWidth: 44, padding: "0 4px" }}>Change</button>
          </div>
        </div>
        <div style={{ display: "flex", gap: 9, marginTop: 30 }}>
          {digits.map((d, i) => (
            <div key={i} style={{
              flex: 1, height: 60, borderRadius: 16, background: "var(--surface)",
              border: `1.5px solid ${i === 4 ? "var(--ink)" : "var(--line)"}`,
              display: "flex", alignItems: "center", justifyContent: "center",
              fontSize: 24, fontWeight: 700, fontVariantNumeric: "tabular-nums",
            }}>
              {d || (i === 4 ? <span style={{ width: 2, height: 26, background: "var(--accent)", animation: "blink 1s step-end infinite" }}/> : "")}
            </div>
          ))}
        </div>
        <div style={{ marginTop: 22, display: "flex", alignItems: "center", gap: 8, fontSize: 13, color: "var(--ink-3)" }}>
          <Icon name="clock" size={16} color="var(--ink-3)"/>
          Resend code in <span className="tab" style={{ fontWeight: 700, color: "var(--ink-2)" }}>0:24</span>
        </div>
        <div style={{ marginTop: 18, padding: 14, borderRadius: 16, background: "var(--status-none-soft)", border: "1px solid var(--line)", display: "flex", gap: 10 }}>
          <Icon name="shield" size={18} color="var(--ink-3)"/>
          <div style={{ fontSize: 12, color: "var(--ink-3)", lineHeight: 1.5 }}>ComiGo will never ask for this code by phone or message. Nobody from our team needs it.</div>
        </div>
        <div style={{ flex: 1 }}/>
        <button className="rs-btn accent full">Verify</button>
      </div>
    </Phone>
  );
}

// ═══════════════════════ S05 · PROFILE SETUP ═══════════════════════
function CgProfileSetupScreen() {
  return (
    <Phone label="S05 Profile setup">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "12px 24px 0" }}>
          <Stepper step={3} total={3} labels={["Number", "Code", "Profile"]}/>
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: "22px 24px 0" }} className="rs-scroll">
          <div className="rs-display" style={{ fontSize: 28, lineHeight: 1.15 }}>Nice to meet you</div>
          <div style={{ color: "var(--ink-3)", marginTop: 8, fontSize: 13.5, lineHeight: 1.55 }}>Riders and drivers see your name, photo and rating — it's how people know who they're travelling with.</div>
          <div style={{ display: "flex", alignItems: "center", gap: 16, marginTop: 22 }}>
            <div style={{ position: "relative" }}>
              <Avatar name="Nimali Perera" size={76}/>
              <div style={{ position: "absolute", right: -3, bottom: -3, width: 30, height: 30, borderRadius: 15, background: "var(--accent)", border: "2.5px solid var(--bg)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="plus" size={15} color="#fff" strokeWidth={2.6}/>
              </div>
            </div>
            <div style={{ fontSize: 12.5, color: "var(--ink-3)", lineHeight: 1.5, flex: 1 }}>Add a clear photo of your face. It raises the chance a driver accepts your booking.</div>
          </div>
          <div style={{ marginTop: 22, display: "flex", flexDirection: "column", gap: 14 }}>
            {[["FULL NAME", "Nimali Perera"], ["EMAIL (OPTIONAL)", "nimali.p@comigo.lk"]].map(([l, v]) => (
              <div key={l}>
                <div className="rs-section-label" style={{ marginBottom: 7 }}>{l}</div>
                <div style={{ height: 54, padding: "0 16px", borderRadius: 16, background: "var(--surface)", border: "1.5px solid var(--line)", display: "flex", alignItems: "center", fontSize: 15, fontWeight: 600 }}>{v}</div>
              </div>
            ))}
            <div>
              <div className="rs-section-label" style={{ marginBottom: 7, display: "flex", alignItems: "center", gap: 8 }}>
                REFERRAL CODE (OPTIONAL)
                <span style={{ height: 16, padding: "0 6px", borderRadius: 999, background: "var(--status-pending-soft)", color: "var(--status-pending-ink)", fontSize: 9.5, fontWeight: 800, letterSpacing: ".06em", display: "inline-flex", alignItems: "center" }}>NEEDS BACKEND</span>
              </div>
              <div style={{ height: 54, padding: "0 16px", borderRadius: 16, background: "var(--surface)", border: "1.5px dashed var(--line-2)", display: "flex", alignItems: "center", fontSize: 15, color: "var(--ink-3)" }}>Enter a friend's code</div>
            </div>
          </div>
        </div>
        <div style={{ padding: "12px 24px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">Start riding</button>
          <div style={{ textAlign: "center", fontSize: 11.5, color: "var(--ink-3)", marginTop: 10 }}>You can add driving later from your account.</div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════ S06 · PUSH PERMISSION PRIMING ═══════════════════════
function CgPushPrimingScreen() {
  return (
    <Phone label="S06 Push priming">
      <div style={{ height: "100%", position: "relative", display: "flex", flexDirection: "column" }}>
        <div style={{ flex: 1, background: "var(--bg)", padding: "0 16px", display: "flex", flexDirection: "column", opacity: .45 }}>
          <HomeHeader state="none"/>
          <RideHomeBody promo/>
        </div>
        <div style={{ position: "absolute", inset: 0, background: "rgba(20,10,5,.45)" }}/>
        <div className="rs-sheet" style={{ position: "absolute", left: 0, right: 0, bottom: 0, padding: "8px 24px 24px" }}>
          <div className="rs-sheet-grab"/>
          <div style={{ width: 60, height: 60, borderRadius: 20, background: "var(--accent-soft)", display: "flex", alignItems: "center", justifyContent: "center", marginTop: 8 }}>
            <Icon name="bell" size={28} color="var(--accent)"/>
          </div>
          <div className="rs-display" style={{ fontSize: 24, marginTop: 16, lineHeight: 1.2 }}>Can we tell you<br/>when it matters?</div>
          <div style={{ marginTop: 14, display: "flex", flexDirection: "column", gap: 12 }}>
            {[
              ["check", "Your driver approved the booking", "Or declined it, so you can find another seat fast."],
              ["car", "Your ride is 5 minutes away", "So you're at the pickup point, not still inside."],
              ["alert", "Trip changes and cancellations", "The things you'd be upset to miss."],
            ].map(([ic, t, s]) => (
              <div key={t} style={{ display: "flex", gap: 12, alignItems: "flex-start" }}>
                <div style={{ width: 32, height: 32, borderRadius: 10, background: "var(--bg-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                  <Icon name={ic} size={16} color="var(--ink-2)"/>
                </div>
                <div>
                  <div style={{ fontSize: 13.5, fontWeight: 700 }}>{t}</div>
                  <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.45 }}>{s}</div>
                </div>
              </div>
            ))}
          </div>
          <button className="rs-btn accent full" style={{ marginTop: 20 }}>Turn on notifications</button>
          <button className="rs-btn soft full" style={{ marginTop: 10 }}>Not now</button>
          <div style={{ textAlign: "center", fontSize: 11, color: "var(--ink-3)", marginTop: 12 }}>You choose the categories afterwards in Settings.</div>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, {
  modeTint, TabBar, ModeChip, HomeHeader, RideHomeBody, DriveHomeBody, BecomeDriverPromo,
  CgSplashScreen, CgOnboardingScreen, CgLoginScreen, CgOtpScreen, CgProfileSetupScreen, CgPushPrimingScreen,
  ONB, OnbArt,
});
