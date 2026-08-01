// shared-support.jsx — one inbox, one notification-preferences screen, one help
// centre, support tickets with a real thread, one SOS, one safety centre.

// ═══════════ S22 · NOTIFICATION INBOX (both modes + operator broadcasts) ═══════════
const INBOX = [
  { kind: "broadcast", t: "9:02 AM", title: "Heavy rain across Colombo today", body: "Expect delays on Baseline Road and Duplication Road. Drivers: give yourself an extra 15 minutes. Riders: your driver may arrive late.", unread: true },
  { kind: "ride", icon: "check", t: "8:58 AM", title: "Kasun approved your seat", body: "Rajagiriya junction → Nugegoda, today 6:15 PM · LKR 267.", unread: true },
  { kind: "drive", icon: "users", t: "8:41 AM", title: `New booking · ${FARE_POLICY.currency} ${money(INBOUND_BOOKING.net)} to you`, body: `${INBOUND_BOOKING.passenger} booked ${INBOUND_BOOKING.from} → ${INBOUND_BOOKING.to} on your ${INBOUND_BOOKING.tripTime} trip. Fare ${FARE_POLICY.currency} ${money(INBOUND_BOOKING.fare)} less the ${FARE_POLICY.commissionPct}% fee.`, unread: true },
  { kind: "money", icon: "cash", t: "Yesterday", title: "Payout sent · LKR 8,420", body: "To BOC ···2204. Should reach you by 11:00 AM." },
  { kind: "ride", icon: "star", t: "Yesterday", title: "How was your trip with Saman?", body: "Rate your ride from Narahenpita to Bambalapitiya." },
  { kind: "account", icon: "shield", t: "22 Jul", title: "Insurance expires in 21 days", body: "Renew it to keep publishing trips after 15 August." },
];

function InboxScreen({ notifsOff = false, filter = "All" }) {
  const KIND = {
    broadcast: { tint: "var(--ink-fill)", fg: "var(--on-ink-fill)", icon: "alert" },
    ride: { tint: "var(--mode-ride-soft)", fg: "var(--mode-ride-ink)" },
    drive: { tint: "var(--mode-drive-soft)", fg: "var(--mode-drive-ink)" },
    money: { tint: "var(--status-approved-soft)", fg: "var(--status-approved-ink)" },
    account: { tint: "var(--status-pending-soft)", fg: "var(--status-pending-ink)" },
  };
  return (
    <Phone label={notifsOff ? "S22b Inbox · push off" : "S22 Inbox"}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Inbox" right={<div style={{ display: "flex", alignItems: "center", gap: 4 }}>
          <button style={{ minHeight: 44, padding: "0 4px", fontSize: 12.5, fontWeight: 700, color: "var(--accent-ink)" }}>Mark all read</button>
          <button style={{ width: 44, height: 44, display: "inline-flex", alignItems: "center", justifyContent: "center" }} aria-label="Notification preferences" title="Notification preferences"><Icon name="settings" size={19} color="var(--ink-2)"/></button>
        </div>}/>
        <div style={{ padding: "12px 16px 10px", display: "flex", gap: 7, overflow: "hidden" }}>
          {["All", "Trips", "Money", "Account"].map(f => (
            <button key={f} className="rs-tap" style={{ flexShrink: 0 }}><span className={`rs-chip${f === filter ? " accent" : ""}`} style={{ height: 32 }}>{f}</span></button>
          ))}
        </div>
        {notifsOff && (
          <div style={{ padding: "0 16px 10px" }}>
            <Banner kind="warn" icon="bell" title="Notifications are off"
              body="You'll miss booking approvals and pickup alerts. Turn them back on in your phone's settings."
              action="Open settings"/>
          </div>
        )}
        <div style={{ flex: 1, overflow: "auto", padding: "0 16px 16px", display: "flex", flexDirection: "column", gap: 9 }} className="rs-scroll">
          {INBOX.map(n => {
            const k = KIND[n.kind];
            const isB = n.kind === "broadcast";
            return (
              <div key={n.title} data-row={n.title} style={{
                padding: 14, borderRadius: 18,
                background: isB ? "var(--ink-fill)" : "var(--surface)",
                border: isB ? "none" : `1px solid ${n.unread ? "var(--line-2)" : "var(--line)"}`,
                color: isB ? "var(--on-ink-fill)" : "var(--ink)",
                display: "flex", gap: 12,
              }}>
                <div style={{ width: 36, height: 36, borderRadius: 12, background: isB ? "rgba(255,255,255,.14)" : k.tint, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                  <Icon name={n.icon || k.icon} size={17} color={isB ? "var(--on-ink-fill)" : k.fg}/>
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: "flex", alignItems: "baseline", gap: 8 }}>
                    {isB && <div style={{ height: 17, padding: "0 6px", borderRadius: 999, background: "#e8834f", color: "#1b1410", fontSize: 9, fontWeight: 800, letterSpacing: ".07em", display: "inline-flex", alignItems: "center", flexShrink: 0 }}>COMIGO</div>}
                    <div style={{ fontSize: 13.5, fontWeight: 700, flex: 1, minWidth: 0 }}>{n.title}</div>
                    <div style={{ fontSize: 10.5, color: isB ? "rgba(244,236,224,.7)" : "var(--ink-3)", flexShrink: 0 }}>{n.t}</div>
                  </div>
                  <div style={{ fontSize: 12, color: isB ? "rgba(244,236,224,.82)" : "var(--ink-3)", marginTop: 4, lineHeight: 1.5 }}>{n.body}</div>
                </div>
                {n.unread && !isB && <div style={{ width: 8, height: 8, borderRadius: 4, background: "var(--accent-ink)", flexShrink: 0, marginTop: 5 }}/>}
              </div>
            );
          })}
        </div>
        <TabBar mode="ride" active="inbox" badges={{ inbox: 3 }}/>
      </div>
    </Phone>
  );
}

// ═══════════ S23 · NOTIFICATION PREFERENCES (per category × per channel) ═══════════
function NotifPrefsScreen() {
  const groups = [
    { label: "RIDING", rows: [
      ["Booking approved or declined", "push,sms,app", true],
      ["Driver arriving soon", "push,app", true],
      ["Trip changes and cancellations", "push,sms,app", true],
      ["Fees and outstanding amounts", "push,app", true],
      ["Receipts", "app", false],
    ]},
    { label: "DRIVING", rows: [
      ["New booking requests", "push,sms,app", true],
      ["Passenger cancelled or didn't board", "push,app", true],
      ["Payouts and penalties", "push,app", true],
      ["Document expiry reminders", "push,sms,app", true],
    ]},
    { label: "FROM COMIGO", rows: [
      ["Service updates in my area", "push,app", true],
      ["Offers and news", "app", false],
    ]},
  ];
  const CH = [["push", "Push"], ["sms", "SMS"], ["app", "In-app"]];
  return (
    <Phone label="S23 Notification preferences">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Notifications" sub="Choose what reaches you, and how"/>
        <div style={{ flex: 1, overflow: "auto", padding: "0 16px 20px" }} className="rs-scroll">
          <div style={{ padding: "12px 0 0" }}>
            <Banner kind="good" icon="check" title="Push is on for this device" body="Safety and trip-critical alerts always arrive, whatever you choose below."/>
          </div>
          {groups.map(g => (
            <div key={g.label}>
              <GroupLabel>{g.label}</GroupLabel>
              <div className="rs-card" style={{ padding: "4px 14px" }}>
                {g.rows.map(([label, chans, on], i) => (
                  <div key={label}>
                    {i > 0 && <div className="rs-divider"/>}
                    <div style={{ padding: "13px 0" }}>
                      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                        <div style={{ fontSize: 13.5, fontWeight: 600, flex: 1, lineHeight: 1.35 }}>{label}</div>
                        <Toggle on={on}/>
                      </div>
                      <div style={{ display: "flex", gap: 7, marginTop: 9, opacity: on ? 1 : .45 }}>
                        {CH.map(([key, cl]) => {
                          const active = on && chans.includes(key);
                          return (
                            <div key={key} style={{
                              height: 28, padding: "0 11px", borderRadius: 999, display: "inline-flex", alignItems: "center", gap: 5,
                              background: active ? "var(--accent-soft)" : "var(--bg-soft)",
                              border: `1px solid ${active ? "transparent" : "var(--line)"}`,
                              color: active ? "var(--accent-ink)" : "var(--ink-3)", fontSize: 11.5, fontWeight: 700,
                            }}>
                              {active && <Icon name="check" size={11} color="var(--accent-ink)" strokeWidth={3}/>}{cl}
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
          <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 14, lineHeight: 1.5, padding: "0 4px" }}>
            SMS is used only where a missed message would cost you money or a seat. Standard rates apply.
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ S24 · HELP CENTRE (mode-aware topics) ═══════════
function HelpCenterScreen({ driver = true }) {
  return (
    <Phone label="S24 Help centre">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Help &amp; support"/>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 20px" }} className="rs-scroll">
          <div style={{ height: 52, padding: "0 15px", borderRadius: 16, background: "var(--surface)", border: "1.5px solid var(--line)", display: "flex", alignItems: "center", gap: 11 }}>
            <Icon name="search" size={18} color="var(--ink-3)"/>
            <span style={{ fontSize: 14.5, color: "var(--ink-3)" }}>Search help articles</span>
          </div>

          <button data-row="open ticket" style={{ width: "100%", textAlign: "left", marginTop: 12, padding: 15, borderRadius: 18, background: "var(--mode-ride-soft)", border: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 12, minHeight: 44 }}>
            <div style={{ width: 40, height: 40, borderRadius: 13, background: "var(--surface)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
              <Icon name="mail" size={19} color="var(--accent-ink)"/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 13.5, fontWeight: 800, color: "var(--accent-ink)" }}>1 open ticket</div>
              <div style={{ fontSize: 11.5, color: "var(--ink-2)", marginTop: 2 }}>"Charged twice for one trip" · agent replied 40 min ago</div>
            </div>
            <Icon name="chev" size={17} color="var(--accent-ink)"/>
          </button>

          <GroupLabel>ABOUT YOUR TRIPS</GroupLabel>
          <div className="rs-card" style={{ padding: "2px 12px" }}>
            <MenuRow icon="route" label="How route matching works" sub="What a match % means and why fares differ"/>
            <div className="rs-divider"/>
            <MenuRow icon="cash" label="Fares, refunds and early drop-off" sub={`When you're charged, and the ${POLICY.earlyDropAdjustedPerMonth} adjusted drop-offs a month`}/>
            <div className="rs-divider"/>
            <MenuRow icon="close" label="Cancellations and no-shows" sub={`${POLICY.paxCancelAfterStartPct}% after a trip starts, ${POLICY.noShowPenaltyPct}% for a no-show`}/>
            <div className="rs-divider"/>
            <MenuRow icon="receipt" label="Outstanding amounts" sub="Fees carried from a cash trip"/>
            <div className="rs-divider"/>
            <MenuRow icon="card" label="Payment problems"/>
          </div>

          {driver && (
            <>
              <GroupLabel>FOR DRIVERS</GroupLabel>
              <div className="rs-card" style={{ padding: "2px 12px" }}>
                <MenuRow icon="shield" label="Verification and documents"/>
                <div className="rs-divider"/>
                <MenuRow icon="receipt" label="Payouts and commission" sub={`Every ${PAYOUT.day} · minimum ${FARE_POLICY.currency} ${money(PAYOUT.minimum)}`}/>
                <div className="rs-divider"/>
                <MenuRow icon="users" label="Managing passengers and seats"/>
                <div className="rs-divider"/>
                <MenuRow icon="car" label="Roadside assistance" badge={<NeedsBackend/>} chev={false}/>
              </div>
            </>
          )}

          <GroupLabel>TALK TO SOMEONE</GroupLabel>
          <div className="rs-card" style={{ padding: "2px 12px" }}>
            <MenuRow icon="mail" label="Message support" sub="Replies within about 2 hours, 7 AM – 11 PM"/>
            <div className="rs-divider"/>
            <MenuRow icon="phone" label="Call ComiGo" sub="011 274 4400 · driver priority line"/>
            <div className="rs-divider"/>
            <MenuRow icon="sos" label="Emergency &amp; safety" sub="SOS, 119, trusted contacts" tint="var(--status-rejected-soft)" fg="var(--status-rejected-ink)"/>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ S25 · SUPPORT TICKET LIST ═══════════
function TicketListScreen() {
  const tickets = [
    { id: "#48210", title: "Charged twice for one trip", st: "Agent replied", when: "40 min ago", open: true, unread: 1 },
    { id: "#47996", title: "Driver took a longer route than shown", st: "Waiting on you", when: "Yesterday", open: true },
    { id: "#47120", title: "Licence rejected twice — need help", st: "Resolved", when: "18 Jul", open: false },
    { id: "#46884", title: "Payout not received", st: "Resolved", when: "9 Jul", open: false },
  ];
  return (
    <Phone label="S25 Support tickets">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Your tickets"/>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 10 }} className="rs-scroll">
          {tickets.map(t => (
            <div key={t.id} className="rs-card" style={{ padding: 14 }}>
              <div style={{ display: "flex", alignItems: "center", gap: 9 }}>
                <div className="tab" style={{ fontSize: 11, fontWeight: 700, color: "var(--ink-3)", flex: 1 }}>{t.id}</div>
                <StatusBadge status={t.st === "Resolved" ? "approved" : t.st === "Agent replied" ? "pending" : "expiring"} label={t.st.toUpperCase()}/>
              </div>
              <div style={{ fontSize: 14, fontWeight: 700, marginTop: 8, lineHeight: 1.35 }}>{t.title}</div>
              <div style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 7 }}>
                <div style={{ fontSize: 11.5, color: "var(--ink-3)", flex: 1 }}>{t.when}</div>
                {t.unread && <div style={{ height: 19, minWidth: 19, borderRadius: 999, background: "var(--danger)", color: "var(--on-bright-fill)", fontSize: 10.5, fontWeight: 800, display: "flex", alignItems: "center", justifyContent: "center", padding: "0 5px" }}>{t.unread}</div>}
                <Icon name="chev" size={16} color="var(--ink-3)"/>
              </div>
            </div>
          ))}
          <div style={{ fontSize: 11.5, color: "var(--ink-3)", lineHeight: 1.5, padding: "6px 4px 0" }}>
            Resolved tickets reopen automatically if you reply to them.
          </div>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full"><Icon name="plus" size={17} color="#fff"/> New ticket</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ S26 · TICKET THREAD ═══════════
function TicketThreadScreen() {
  const msgs = [
    { me: true, t: "24 Jul, 9:12 AM", body: "I was charged LKR 279 twice for my trip from Narahenpita to Bambalapitiya on 21 July. Only one trip happened.", attach: "receipt-7911.pdf" },
    { me: false, who: "Dilani · ComiGo", t: "24 Jul, 9:48 AM", body: "Thanks Nimali — I can see both charges on booking #7911. The second one is a duplicate our gateway took by mistake. You were only meant to be charged once." },
    { me: false, who: "Dilani · ComiGo", t: "24 Jul, 9:49 AM", body: "I've refunded LKR 279 to your Visa today. It reaches your bank in 3–5 working days. If it hasn't landed by Tuesday, reply here and I'll chase it with your bank." },
    { me: true, t: "24 Jul, 10:05 AM", body: "Thank you. I'll check on Tuesday." },
  ];
  return (
    <Phone label="S26 Ticket thread">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Charged twice for one trip" sub="#48210 · Agent replied"/>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          <div style={{ display: "flex", justifyContent: "center" }}>
            <div className="rs-chip" style={{ height: 26 }}>Opened 24 Jul · booking #7911</div>
          </div>
          {msgs.map((m, i) => (
            <div key={i} style={{ display: "flex", flexDirection: "column", alignItems: m.me ? "flex-end" : "flex-start", gap: 5 }}>
              {!m.me && (
                <div style={{ display: "flex", alignItems: "center", gap: 7 }}>
                  <Avatar name="Dilani C" size={22}/>
                  <div style={{ fontSize: 11, fontWeight: 700, color: "var(--ink-3)" }}>{m.who}</div>
                </div>
              )}
              <div style={{
                maxWidth: "84%", padding: "11px 13px", borderRadius: 16,
                background: m.me ? "var(--ink-fill)" : "var(--surface)",
                color: m.me ? "var(--on-ink-fill)" : "var(--ink)",
                border: m.me ? "none" : "1px solid var(--line)",
                borderBottomRightRadius: m.me ? 5 : 16, borderBottomLeftRadius: m.me ? 16 : 5,
              }}>
                <div style={{ fontSize: 13, lineHeight: 1.55 }}>{m.body}</div>
                {m.attach && (
                  <div style={{ marginTop: 9, padding: "7px 9px", borderRadius: 10, background: "rgba(255,255,255,.14)", display: "flex", alignItems: "center", gap: 7 }}>
                    <Icon name="receipt" size={14} color="var(--on-ink-fill)"/>
                    <div style={{ fontSize: 11, fontWeight: 600 }}>{m.attach}</div>
                  </div>
                )}
              </div>
              <div style={{ fontSize: 10.5, color: "var(--ink-3)" }}>{m.t}</div>
            </div>
          ))}
        </div>
        <div style={{ padding: "10px 14px 14px", background: "var(--surface)", borderTop: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 9 }}>
          <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label="Attach a file">
            <Icon name="plus" size={19}/>
          </button>
          <div style={{ flex: 1, minHeight: 44, padding: "0 14px", borderRadius: 22, background: "var(--bg-soft)", border: "1px solid var(--line)", display: "flex", alignItems: "center", fontSize: 13.5, color: "var(--ink-3)" }}>Write a reply…</div>
          <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--accent-ink)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label="Send">
            <Icon name="arrow" size={19} color="#fff"/>
          </button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ S27 · SOS (one screen, context-aware) ═══════════
function SosScreen({ mode = "ride" }) {
  const ctx = mode === "ride"
    ? { line: `You're a passenger on ${MY_TRIP.driver}'s trip`, sub: `${MY_TRIP.car} · ${MY_TRIP.plate} · heading to ${MY_TRIP.to}`, where: "Nawala Road, near Rajagiriya junction" }
    : { line: `You're driving ${LIVE_DRIVE.onBoard} passengers`, sub: `${LIVE_DRIVE.from} → ${LIVE_DRIVE.to} · ${MY_VEHICLE.make} · ${MY_VEHICLE.plate}`, where: "Kotte Road, near Pita Kotte junction" };
  return (
    <Phone label={`S27 SOS · ${mode}`} statusDark statusBg="#8a1f14">
      <div style={{ height: "100%", background: "#8a1f14", color: "#fff", display: "flex", flexDirection: "column", padding: "10px 20px 18px" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div style={{ fontSize: 11, fontWeight: 800, letterSpacing: ".14em", opacity: .82 }}>EMERGENCY</div>
          <button style={{ width: 44, height: 44, borderRadius: 22, background: "rgba(255,255,255,.18)", display: "inline-flex", alignItems: "center", justifyContent: "center" }} aria-label="Close">
            <Icon name="close" size={20} color="#fff"/>
          </button>
        </div>
        <div style={{ marginTop: 16, padding: 14, borderRadius: 16, background: "rgba(0,0,0,.24)" }}>
          <div style={{ fontSize: 13.5, fontWeight: 800 }}>{ctx.line}</div>
          <div style={{ fontSize: 11.5, opacity: .85, marginTop: 3, lineHeight: 1.45 }}>{ctx.sub}</div>
          <div style={{ fontSize: 11.5, opacity: .85, marginTop: 6, display: "flex", alignItems: "center", gap: 6 }}>
            <Icon name="pin" size={13} color="#fff"/> {ctx.where}
          </div>
        </div>
        <div style={{ flex: 1, display: "flex", flexDirection: "column", justifyContent: "center", gap: 12 }}>
          <button style={{ padding: "18px 20px", borderRadius: 22, background: "#fff", color: "#8a1f14", display: "flex", alignItems: "center", gap: 14, textAlign: "left" }}>
            <div style={{ width: 46, height: 46, borderRadius: 15, background: "#8a1f14", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
              <Icon name="phone" size={22} color="#fff"/>
            </div>
            <div style={{ flex: 1 }}>
              <div className="rs-display" style={{ fontSize: 25, lineHeight: 1 }}>Call 119</div>
              <div style={{ fontSize: 12, fontWeight: 600, marginTop: 4, opacity: .8 }}>Sri Lanka Police emergency</div>
            </div>
          </button>
          <button style={{ padding: "15px 18px", borderRadius: 20, background: "rgba(255,255,255,.16)", color: "#fff", display: "flex", alignItems: "center", gap: 13, textAlign: "left" }}>
            <Icon name="shield" size={22} color="#fff"/>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14.5, fontWeight: 800 }}>ComiGo safety team</div>
              <div style={{ fontSize: 11.5, opacity: .82, marginTop: 2 }}>011 274 4400 · answers in under a minute</div>
            </div>
          </button>
          <button style={{ padding: "15px 18px", borderRadius: 20, background: "rgba(255,255,255,.16)", color: "#fff", display: "flex", alignItems: "center", gap: 13, textAlign: "left" }}>
            <Icon name="share" size={22} color="#fff"/>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14.5, fontWeight: 800 }}>Alert my trusted contacts</div>
              <div style={{ fontSize: 11.5, opacity: .82, marginTop: 2 }}>Sends Amma and Chathura your live location</div>
            </div>
          </button>
        </div>
        <div style={{ padding: 13, borderRadius: 16, background: "rgba(0,0,0,.24)", fontSize: 11.5, lineHeight: 1.5, opacity: .92 }}>
          Calling 119 also sends your trip details, vehicle number and live location to the ComiGo safety team. Nothing here cancels your trip.
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ S28 · SAFETY CENTRE ═══════════
function SafetyCenterScreen() {
  return (
    <Phone label="S28 Safety centre">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Safety"/>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 20px" }} className="rs-scroll">
          <button className="rs-btn full" style={{ height: 60, background: "var(--danger)", color: "var(--on-bright-fill)", fontSize: 16, gap: 11 }}>
            <Icon name="sos" size={22} color="var(--on-bright-fill)"/> Emergency SOS
          </button>
          <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 9, textAlign: "center", lineHeight: 1.5 }}>
            Also reachable from any trip screen — no need to come here first.
          </div>

          <GroupLabel>TRUSTED CONTACTS</GroupLabel>
          <div className="rs-card" style={{ padding: "2px 12px" }}>
            {[["Amma", "+94 71 220 4418", true], ["Chathura", "+94 76 881 0092", false]].map(([n, p, auto], i) => (
              <div key={n}>
                {i > 0 && <div className="rs-divider"/>}
                <MenuRow icon="user" label={n} sub={`${p}${auto ? " · gets every trip automatically" : " · you share manually"}`} right={<Toggle on={auto}/>} chev={false}/>
              </div>
            ))}
          </div>
          <button className="rs-btn ghost full" style={{ marginTop: 10 }}><Icon name="plus" size={17}/> Add a contact</button>

          <GroupLabel>DURING A TRIP</GroupLabel>
          <div className="rs-card" style={{ padding: "2px 12px" }}>
            <MenuRow icon="share" label="Share my live trip" sub="Sends a link anyone can open, no app needed"/>
            <div className="rs-divider"/>
            <MenuRow icon="alert" label="Report a problem" sub="Route, behaviour, vehicle or fare"/>
            <div className="rs-divider"/>
            <MenuRow icon="lock" label="Hide my number" sub="Calls go through ComiGo instead" right={<Toggle on/>} chev={false}/>
          </div>

          <GroupLabel>GOOD TO KNOW</GroupLabel>
          <div className="rs-card" style={{ padding: 15, display: "flex", flexDirection: "column", gap: 11 }}>
            {[
              ["Check the plate before you get in", `It's on your booking screen: ${MY_TRIP.plate}.`],
              ["Every driver is identity-checked", "Licence and vehicle papers are verified before they can publish."],
              ["Sit where you're comfortable", "You're never obliged to take the front seat."],
            ].map(([t, b]) => (
              <div key={t} style={{ display: "flex", gap: 11 }}>
                <Icon name="check" size={16} color="var(--status-approved-ink)" strokeWidth={2.6}/>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 13, fontWeight: 700 }}>{t}</div>
                  <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.45 }}>{b}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, { InboxScreen, NotifPrefsScreen, HelpCenterScreen, TicketListScreen, TicketThreadScreen, SosScreen, SafetyCenterScreen, INBOX });
