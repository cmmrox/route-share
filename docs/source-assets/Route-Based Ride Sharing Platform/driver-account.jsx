// driver-account.jsx — account, notifications, SOS, help, leaderboard

// ═══════════════════════════════════════════════════════════════════
// ACCOUNT
// ═══════════════════════════════════════════════════════════════════
function DrAccountScreen({ verified = true }) {
  return (
    <Phone label="D27 Account">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)", overflow: "auto" }} className="rs-scroll">
        <div style={{ padding: "20px 20px 24px", background: "var(--ink)", color: "#fff", borderBottomLeftRadius: 28, borderBottomRightRadius: 28, position: "relative", overflow: "hidden" }}>
          <div style={{ position: "absolute", right: -50, top: -50, width: 200, height: 200, borderRadius: 100, background: "var(--accent)", opacity: .35 }}/>
          <div style={{ position: "relative" }}>
            <div style={{ display: "flex", justifyContent: "space-between" }}>
              <button style={{ width: 40, height: 40, borderRadius: 20, background: "rgba(255,255,255,.18)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="back" size={20} color="#fff"/>
              </button>
              <button style={{ width: 40, height: 40, borderRadius: 20, background: "rgba(255,255,255,.18)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="settings" size={18} color="#fff"/>
              </button>
            </div>
            <div style={{ marginTop: 18, display: "flex", alignItems: "center", gap: 14 }}>
              <div style={{ position: "relative" }}>
                <Avatar name="Saman W" size={70} style={{ border: "3px solid #fff" }}/>
                {verified && <div style={{ position: "absolute", right: -2, bottom: -2, width: 24, height: 24, borderRadius: 12, background: "var(--success)", display: "flex", alignItems: "center", justifyContent: "center", border: "2px solid var(--ink)" }}>
                  <Icon name="check" size={12} color="#fff" strokeWidth={3.4}/>
                </div>}
              </div>
              <div style={{ flex: 1 }}>
                <div className="rs-display" style={{ fontSize: 22 }}>Saman Wijesinghe</div>
                <div style={{ fontSize: 12, opacity: .85, display: "flex", alignItems: "center", gap: 6 }}>
                  <Icon name="star" size={12} color="#fff"/> 4.92 · 312 trips · Member since 2024
                </div>
                {verified ? (
                  <div style={{ marginTop: 8, display: "inline-flex", alignItems: "center", gap: 5, padding: "3px 10px", borderRadius: 999, background: "rgba(58,138,90,.4)", fontSize: 11, fontWeight: 700, letterSpacing: ".04em" }}>
                    <Icon name="shield" size={11} color="#fff" strokeWidth={2.4}/> VERIFIED DRIVER
                  </div>
                ) : (
                  <div style={{ marginTop: 8, display: "inline-flex", alignItems: "center", gap: 5, padding: "3px 10px", borderRadius: 999, background: "rgba(214,106,59,.5)", fontSize: 11, fontWeight: 700, letterSpacing: ".04em" }}>
                    <Icon name="clock" size={11} color="#fff"/> KYC IN REVIEW
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        <div style={{ padding: "18px 20px 20px", display: "flex", flexDirection: "column", gap: 4 }}>
          <MenuRow icon="car" label="Your vehicles" sub="Toyota Aqua + 1"/>
          <MenuRow icon="card" label="Payout account" sub="BOC ··· 2204"/>
          <MenuRow icon="receipt" label="Earnings & history"/>
          <MenuRow icon="star" label="Ratings & reviews" sub="4.92 · 312 reviews"/>
          <MenuRow icon="calendar" label="Recurring trips" sub="5 active"/>

          <div className="rs-section-label" style={{ marginTop: 14, padding: "0 4px" }}>VERIFICATION</div>
          <MenuRow icon="shield" label="Identity & licence" sub="Verified · expires 2029"/>
          <MenuRow icon="receipt" label="Vehicle documents"/>
          <MenuRow icon="user" label="Tax info (TIN)"/>

          <div className="rs-section-label" style={{ marginTop: 14, padding: "0 4px" }}>SETTINGS</div>
          <MenuRow icon="bell" label="Notifications"/>
          <MenuRow icon="lock" label="Privacy & data"/>
          <MenuRow icon="thumb" label="Rate the app"/>

          <div className="rs-section-label" style={{ marginTop: 14, padding: "0 4px" }}>SUPPORT</div>
          <MenuRow icon="help" label="Help center"/>
          <MenuRow icon="alert" label="Report an issue"/>

          <button style={{ marginTop: 14, padding: "14px", textAlign: "center", color: "var(--danger)", fontWeight: 700, fontSize: 14 }}>Sign out</button>
          <div style={{ textAlign: "center", fontSize: 11, color: "var(--ink-4)" }}>RouteShare Driver v1.0.0 · Colombo</div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// NOTIFICATIONS (driver-specific)
// ═══════════════════════════════════════════════════════════════════
function DrNotificationsScreen() {
  const items = [
    { t: "now", icon: "users", tint: "var(--accent-soft)", fg: "var(--accent-2)", title: "New booking · LKR 292", body: "Nimali P booked Narahenpita → Bambalapitiya for your 8:00 AM trip.", unread: true },
    { t: "5 min ago", icon: "alert", tint: "var(--accent-soft)", fg: "var(--accent-2)", title: "Booking request waiting", body: "Priya J wants to join your morning trip — needs your approval.", unread: true },
    { t: "1 h ago", icon: "card", tint: "var(--success-soft)", fg: "var(--match-full)", title: "Payout sent · LKR 8,420", body: "Settled to BOC ··· 2204. Will reach by 11:00 AM." },
    { t: "Yesterday", icon: "star", tint: "var(--bg-soft)", fg: "var(--warn)", title: "You got a 5-star rating", body: "Kasun A said: \"My usual driver — reliable and stress-free.\"" },
    { t: "2 days ago", icon: "thumb", tint: "var(--teal-soft)", fg: "var(--teal)", title: "You're a top earner this week!", body: "LKR 9,160 earned — top 8% in Colombo. Keep it up." },
    { t: "3 days ago", icon: "shield", tint: "var(--bg-soft)", fg: "var(--ink-3)", title: "Insurance expires in 30 days", body: "Renew your policy to keep publishing trips." },
  ];
  return (
    <Phone label="D28 Notifications">
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
// SOS / EMERGENCY
// ═══════════════════════════════════════════════════════════════════
function DrSosScreen() {
  return (
    <Phone label="D29 SOS">
      <div style={{ height: "100%", background: "linear-gradient(180deg, #c0392b 0%, #8a1f14 100%)", color: "#fff", display: "flex", flexDirection: "column", padding: "14px 20px 20px" }}>
        <div style={{ display: "flex", justifyContent: "space-between" }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "rgba(255,255,255,.18)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="close" size={20} color="#fff"/>
          </button>
          <div style={{ height: 40, padding: "0 12px", background: "rgba(255,255,255,.18)", borderRadius: 20, display: "flex", alignItems: "center", gap: 6, fontSize: 11, fontWeight: 700 }}>
            <div style={{ width: 8, height: 8, borderRadius: 4, background: "#fff", animation: "pulse 1s infinite" }}/> TRIP IN PROGRESS
          </div>
        </div>

        <div style={{ textAlign: "center", marginTop: 20 }}>
          <div className="rs-display" style={{ fontSize: 32, letterSpacing: "-0.02em" }}>Need help?</div>
          <div style={{ marginTop: 6, fontSize: 14, opacity: .9, maxWidth: 280, margin: "6px auto 0" }}>
            Hold the button for 3 seconds to alert RouteShare safety team and emergency services. Your live location and passenger details are shared.
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
            <SosRow icon="phone" label="Call Police (119)" sub="Sri Lanka Police emergency line"/>
            <SosRow icon="car" label="Roadside assistance" sub="Tow, breakdown, mechanical issue"/>
            <SosRow icon="alert" label="Report unsafe passenger" sub="End trip, flag for review"/>
          </div>
        </div>
      </div>
    </Phone>
  );
}

function SosRow({ icon, label, sub }) {
  return (
    <div style={{ padding: "12px 14px", background: "rgba(255,255,255,.12)", borderRadius: 12, display: "flex", alignItems: "center", gap: 12 }}>
      <Icon name={icon} size={18} color="#fff"/>
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 13, fontWeight: 700 }}>{label}</div>
        <div style={{ fontSize: 11, opacity: .75 }}>{sub}</div>
      </div>
      <Icon name="chev" size={14} color="#fff"/>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════
// HELP / SUPPORT
// ═══════════════════════════════════════════════════════════════════
function DrSupportScreen() {
  return (
    <Phone label="D30 Help Center">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "14px 20px", display: "flex", alignItems: "center", gap: 12, background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="back" size={20}/>
          </button>
          <div style={{ flex: 1, fontSize: 17, fontWeight: 700 }}>Driver help</div>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "16px 20px" }} className="rs-scroll">
          <div style={{ padding: 14, background: "var(--surface)", border: "1.5px solid var(--line)", borderRadius: 14, display: "flex", alignItems: "center", gap: 10 }}>
            <Icon name="search" size={18} color="var(--ink-3)"/>
            <div style={{ flex: 1, color: "var(--ink-4)", fontSize: 14 }}>Search "payout", "no-show"…</div>
          </div>

          <div style={{ marginTop: 14, padding: 16, background: "var(--accent-soft)", borderRadius: 16, display: "flex", gap: 12, alignItems: "center" }}>
            <div style={{ width: 44, height: 44, borderRadius: 14, background: "#fff", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="route" size={20} color="var(--accent-2)"/>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 13, fontWeight: 700 }}>Issue with last trip · LKR 699</div>
              <div style={{ fontSize: 11, color: "var(--ink-3)" }}>Rajagiriya → Fort · 8:00 AM</div>
            </div>
            <Icon name="chev" size={16} color="var(--ink-3)"/>
          </div>

          <div className="rs-section-label" style={{ marginTop: 18, marginBottom: 10 }}>TOP TOPICS</div>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
            <TopicCard icon="card" label="Payouts & taxes"/>
            <TopicCard icon="users" label="Difficult passengers"/>
            <TopicCard icon="route" label="Route matching"/>
            <TopicCard icon="car" label="Vehicle issues"/>
            <TopicCard icon="shield" label="Verification"/>
            <TopicCard icon="calendar" label="Recurring trips"/>
          </div>

          <div className="rs-section-label" style={{ marginTop: 18, marginBottom: 8 }}>CONTACT</div>
          <div style={{ display: "flex", flexDirection: "column" }}>
            {[
              { icon: "phone", label: "Call driver support", sub: "Priority line · +94 11 777 0001", fg: "var(--teal)" },
              { icon: "mail", label: "Email", sub: "drivers@routeshare.lk · reply in 2h", fg: "var(--ink-2)" },
              { icon: "users", label: "Driver community forum", sub: "Tips from 1,200+ drivers", fg: "var(--accent-2)" },
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

// ═══════════════════════════════════════════════════════════════════
// DRIVER LEADERBOARD / REWARDS
// ═══════════════════════════════════════════════════════════════════
function DrLeaderboardScreen() {
  return (
    <Phone label="D31 Leaderboard">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "14px 20px", display: "flex", alignItems: "center", gap: 12, background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="back" size={20}/>
          </button>
          <div style={{ flex: 1, fontSize: 17, fontWeight: 700 }}>Weekly leaderboard</div>
        </div>

        <div style={{ flex: 1, overflow: "auto" }} className="rs-scroll">
          {/* Your rank hero */}
          <div style={{ padding: "20px 20px 24px", background: "var(--accent)", color: "#fff", textAlign: "center", borderBottomLeftRadius: 24, borderBottomRightRadius: 24, position: "relative", overflow: "hidden" }}>
            <div style={{ position: "absolute", left: -40, bottom: -60, width: 180, height: 180, borderRadius: 90, background: "rgba(255,255,255,.15)" }}/>
            <div style={{ position: "relative" }}>
              <div style={{ fontSize: 11, fontWeight: 700, letterSpacing: ".14em", opacity: .85 }}>YOUR RANK · WEEK 17</div>
              <div className="rs-display tab" style={{ fontSize: 80, fontWeight: 600, lineHeight: 1, marginTop: 4 }}>
                #14<span style={{ fontSize: 28, opacity: .65 }}>/1,240</span>
              </div>
              <div style={{ marginTop: 8, fontSize: 13, opacity: .9, display: "inline-flex", alignItems: "center", gap: 6 }}>
                Top 1.1% in Colombo · <Icon name="arrow" size={12} color="#fff"/> up 6 from last week
              </div>
            </div>
          </div>

          {/* Reward tier */}
          <div style={{ padding: "16px 20px 6px" }}>
            <div className="rs-card" style={{ padding: 14, display: "flex", alignItems: "center", gap: 12 }}>
              <div style={{ width: 48, height: 48, borderRadius: 14, background: "var(--accent-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="star" size={24} color="var(--accent-2)"/>
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 14, fontWeight: 700 }}>Gold tier · 312 trips</div>
                <div style={{ fontSize: 11, color: "var(--ink-3)" }}>88 more trips to Platinum (lower commission)</div>
              </div>
            </div>
          </div>

          {/* Top drivers */}
          <div className="rs-section-label" style={{ padding: "18px 20px 8px" }}>TOP THIS WEEK</div>
          <div style={{ padding: "0 20px 20px", display: "flex", flexDirection: "column", gap: 8 }}>
            {[
              { rank: 1, name: "Imran F", trips: 38, earn: 22400, badge: "🥇" },
              { rank: 2, name: "Saman P", trips: 32, earn: 19800, badge: "🥈" },
              { rank: 3, name: "Anjali R", trips: 30, earn: 18200, badge: "🥉" },
              { rank: 4, name: "Lasith K", trips: 28, earn: 16100 },
              { rank: 5, name: "Maya S", trips: 27, earn: 15400 },
              { rank: 14, name: "Saman W (you)", trips: 14, earn: 9160, you: true },
            ].map(r => (
              <div key={r.rank} style={{
                padding: 12, borderRadius: 14,
                background: r.you ? "var(--accent-soft)" : "var(--surface)",
                border: r.you ? "1.5px solid var(--accent)" : "1px solid var(--line)",
                display: "flex", alignItems: "center", gap: 12,
              }}>
                <div className="rs-display tab" style={{ width: 32, fontSize: 20, fontWeight: 600, color: r.you ? "var(--accent-2)" : "var(--ink-3)", textAlign: "center" }}>{r.badge || r.rank}</div>
                <Avatar name={r.name.replace(" (you)", "")} size={36}/>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 13, fontWeight: 700 }}>{r.name}</div>
                  <div style={{ fontSize: 11, color: "var(--ink-3)" }}>{r.trips} trips</div>
                </div>
                <div className="tab" style={{ fontWeight: 700, fontSize: 14 }}>LKR {r.earn.toLocaleString()}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, { DrAccountScreen, DrNotificationsScreen, DrSosScreen, DrSupportScreen, DrLeaderboardScreen });
