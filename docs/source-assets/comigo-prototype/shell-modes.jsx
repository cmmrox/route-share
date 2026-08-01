// shell-modes.jsx — mode switcher variants, gating states, conflict states, tab bar spec

// ── the switch sheet (used by the recommended pattern for anything that needs explaining) ──
function SwitchSheet({ children, title, sub }) {
  return (
    <div className="rs-sheet" style={{ position: "absolute", left: 0, right: 0, bottom: 0, padding: "8px 22px 22px" }}>
      <div className="rs-sheet-grab"/>
      <div className="rs-display" style={{ fontSize: 23, marginTop: 10, lineHeight: 1.22 }}>{title}</div>
      {sub && <div style={{ fontSize: 13, color: "var(--ink-3)", marginTop: 8, lineHeight: 1.55 }}>{sub}</div>}
      {children}
    </div>
  );
}

function ModeOptionRow({ mode, active, gated, note }) {
  const t = modeTint(mode);
  return (
    <div style={{
      padding: 14, borderRadius: 18, display: "flex", alignItems: "center", gap: 12,
      background: active ? t.soft : "var(--surface)",
      border: `1.5px solid ${active ? t.c : "var(--line)"}`,
      opacity: gated ? .55 : 1,
    }}>
      <div style={{ width: 42, height: 42, borderRadius: 14, background: active ? t.c : "var(--bg-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
        <Icon name={t.icon} size={20} color={active ? "#fff" : "var(--ink-3)"}/>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14.5, fontWeight: 800, color: active ? t.ink : "var(--ink)" }}>{mode === "drive" ? "Driver mode" : "Passenger mode"}</div>
        <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>{note}</div>
      </div>
      {active
        ? <div style={{ width: 24, height: 24, borderRadius: 12, background: t.c, display: "flex", alignItems: "center", justifyContent: "center" }}><Icon name="check" size={14} color="#fff" strokeWidth={2.8}/></div>
        : gated ? <Icon name="lock" size={18} color="var(--ink-3)"/> : <Icon name="chev" size={18} color="var(--ink-3)"/>}
    </div>
  );
}

// ═══════════ STATE · passenger only (no chip, two become-a-driver entries) ═══════════
function ModeStateNoneScreen() {
  return (
    <Phone label="S07 No driver access" note={{ kind: "info", head: "No mode chip.", text: "modes: [\"PASSENGER\"] — so the slot holds the wordmark instead. The promo card is become-a-driver entry point 1; an Account → Start earning row is entry point 2." }}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "10px 16px 12px", display: "flex", alignItems: "center", gap: 10 }}>
          <div className="rs-display" style={{ fontSize: 20, flex: 1 }}>ComiGo</div>
          <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }} aria-label="Notifications"><Icon name="bell" size={20}/></button>
          <Avatar name="Nimali" size={44}/>
        </div>
        <RideHomeBody promo/>
        <TabBar mode="ride" active="home" badges={{ inbox: 2 }}/>
      </div>
    </Phone>
  );
}

// ═══════════ STATE · pending review ═══════════
function ModeStatePendingScreen() {
  return (
    <Phone label="S08 Driver pending">
      <div style={{ height: "100%", position: "relative", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ flex: 1, display: "flex", flexDirection: "column", opacity: .4 }}>
          <div style={{ padding: "10px 16px 12px", display: "flex", alignItems: "center", gap: 10 }}>
            <ModeChip mode="ride" state="pending"/><div style={{ flex: 1 }}/><Avatar name="Nimali" size={44}/>
          </div>
          <RideHomeBody/>
        </div>
        <div style={{ position: "absolute", inset: 0, background: "rgba(20,10,5,.45)" }}/>
        <SwitchSheet title="We're checking your documents" sub="Usually done within one working day. We'll notify you the moment it's decided — you can keep riding meanwhile.">
          <div style={{ marginTop: 18, padding: 16, borderRadius: 18, background: "var(--status-pending-soft)", border: "1px solid var(--status-pending)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <div style={{ width: 34, height: 34, borderRadius: 12, background: "var(--status-pending)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="clock" size={17} color="#fff"/>
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 13.5, fontWeight: 800, color: "var(--status-pending-ink)" }}>In review since yesterday, 4:12 PM</div>
                <div style={{ fontSize: 11.5, color: "var(--ink-2)", marginTop: 2 }}>3 of 3 documents submitted</div>
              </div>
            </div>
          </div>
          <div style={{ marginTop: 14, display: "flex", flexDirection: "column", gap: 10 }}>
            <ModeOptionRow mode="ride" active note="Available now."/>
            <ModeOptionRow mode="drive" gated note="Unlocks when your licence and vehicle are approved."/>
          </div>
          <button className="rs-btn soft full" style={{ marginTop: 16 }}>See my application</button>
          <button className="rs-btn accent full" style={{ marginTop: 10 }}>Keep riding</button>
        </SwitchSheet>
      </div>
    </Phone>
  );
}

// ═══════════ STATE · rejected ═══════════
function ModeStateRejectedScreen() {
  return (
    <Phone label="S09 Driver rejected">
      <div style={{ height: "100%", position: "relative", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ flex: 1, display: "flex", flexDirection: "column", opacity: .4 }}>
          <div style={{ padding: "10px 16px 12px", display: "flex", alignItems: "center", gap: 10 }}>
            <ModeChip mode="ride" state="rejected"/><div style={{ flex: 1 }}/><Avatar name="Nimali" size={44}/>
          </div>
          <RideHomeBody/>
        </div>
        <div style={{ position: "absolute", inset: 0, background: "rgba(20,10,5,.45)" }}/>
        <SwitchSheet title="One document needs redoing" sub="Your identity and vehicle checks passed. Only the licence photo was unreadable.">
          <div style={{ marginTop: 18, display: "flex", flexDirection: "column", gap: 10 }}>
            <div style={{ padding: 14, borderRadius: 18, background: "var(--status-rejected-soft)", border: "1px solid var(--status-rejected)" }}>
              <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                <Icon name="alert" size={18} color="var(--status-rejected)"/>
                <div style={{ fontSize: 13.5, fontWeight: 800, color: "var(--status-rejected-ink)", flex: 1 }}>Driving licence · rejected</div>
              </div>
              <div style={{ fontSize: 12, color: "var(--ink-2)", marginTop: 8, lineHeight: 1.5 }}>
                "The expiry date is blurred. Please re-upload with the whole card in frame and no flash glare." — ComiGo verification team, 22 Jul
              </div>
            </div>
            {[["Identity · NIC", "approved"], [`Vehicle · ${MY_VEHICLE.make}`, "approved"]].map(([l, s]) => (
              <div key={l} style={{ padding: "12px 14px", borderRadius: 16, background: "var(--surface)", border: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 10 }}>
                <Icon name="check" size={17} color="var(--status-approved)" strokeWidth={2.6}/>
                <div style={{ fontSize: 13, fontWeight: 700, flex: 1 }}>{l}</div>
                <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".08em", color: "var(--status-approved-ink)" }}>APPROVED</div>
              </div>
            ))}
          </div>
          <button className="rs-btn accent full" style={{ marginTop: 18 }}>Re-upload licence</button>
          <button className="rs-btn soft full" style={{ marginTop: 10 }}>Talk to support</button>
        </SwitchSheet>
      </div>
    </Phone>
  );
}

// ═══════════ CONFLICT 1 · switching while on a trip as a passenger ═══════════
function ConflictActiveTripScreen() {
  return (
    <Phone label="S10 Conflict · riding now">
      <div style={{ height: "100%", position: "relative" }}>
        <MapBackdrop pickupLabel="Picked up" dropLabel="Nugegoda"/>
        <div style={{ position: "absolute", inset: 0, background: "rgba(20,10,5,.5)" }}/>
        <SwitchSheet title="You're on a trip right now" sub={`${MY_TRIP.driver.split(" ")[0]} is 6 minutes from your drop-off. Switching to driver mode would leave this screen — your trip keeps running either way.`}>
          <div style={{ marginTop: 18, padding: 14, borderRadius: 18, background: "var(--mode-ride-soft)", border: "1px solid var(--mode-ride)", display: "flex", alignItems: "center", gap: 12 }}>
            <Avatar name={MY_TRIP.driver} size={42}/>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 13.5, fontWeight: 800 }}>In trip · {MY_TRIP.driver}</div>
              <div style={{ fontSize: 11.5, color: "var(--ink-2)", marginTop: 2 }}>Arriving {MY_TRIP.to} {MY_TRIP.arrive} · {FARE_POLICY.currency} {money(MY_TRIP.price)}</div>
            </div>
            <div style={{ width: 10, height: 10, borderRadius: 5, background: "var(--mode-ride)", animation: "pulse 1.8s infinite" }}/>
          </div>
          <div style={{ marginTop: 14, padding: "12px 14px", borderRadius: 16, background: "var(--status-none-soft)", border: "1px solid var(--line)", display: "flex", gap: 10 }}>
            <Icon name="shield" size={17} color="var(--ink-3)"/>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", lineHeight: 1.45 }}>Nothing is cancelled and you stay signed in. A live-trip bar stays pinned above the tab bar in driver mode so you can jump back.</div>
          </div>
          <button className="rs-btn primary full" style={{ marginTop: 18 }}>Stay in this trip</button>
          <button className="rs-btn ghost full" style={{ marginTop: 10 }}>Switch to driving anyway</button>
        </SwitchSheet>
      </div>
    </Phone>
  );
}

// ═══════════ CONFLICT 2 · passenger booking notification while driving live ═══════════
function ConflictDrivingNotifScreen() {
  return (
    <Phone label="S11 Conflict · notified while driving">
      <div style={{ height: "100%", position: "relative", display: "flex", flexDirection: "column" }}>
        <MapBackdrop pickupLabel={LIVE_DRIVE.from} dropLabel={LIVE_DRIVE.to}/>
        <div style={{ position: "absolute", left: 0, right: 0, top: 0, padding: "10px 14px" }}>
          <div style={{ padding: "14px 16px", borderRadius: 20, background: "var(--ink-fill)", color: "#f4ece0", display: "flex", alignItems: "center", gap: 12, boxShadow: "var(--shadow-lg)" }}>
            <div style={{ width: 42, height: 42, borderRadius: 14, background: "rgba(255,255,255,.14)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
              <Icon name="arrow" size={22} color="#f4ece0"/>
            </div>
            <div style={{ flex: 1 }}>
              <div className="rs-display" style={{ fontSize: 20 }}>400 m</div>
              <div style={{ fontSize: 12, opacity: .75 }}>Continue on Nawala Road</div>
            </div>
            <div className="rs-chip teal" style={{ height: 26, background: "rgba(72,168,159,.2)", color: "#48a89f", border: "none" }}>{LIVE_DRIVE.onBoard} on board</div>
          </div>
        </div>
        <div style={{ flex: 1 }}/>
        <div style={{ position: "relative", padding: "0 14px 12px", display: "flex", flexDirection: "column", gap: 10 }}>
          <div data-row="queued passenger alert" style={{ padding: "12px 14px", borderRadius: 18, background: "var(--surface)", border: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 12, boxShadow: "var(--shadow-md)" }}>
            <div style={{ width: 38, height: 38, borderRadius: 12, background: "var(--mode-ride-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
              <Icon name="check" size={18} color="var(--mode-ride)" strokeWidth={2.4}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 9.5, fontWeight: 800, letterSpacing: ".1em", color: "var(--ink-3)" }}>AS A PASSENGER · QUEUED UNTIL YOU PARK</div>
              <div style={{ fontSize: 13, fontWeight: 700, marginTop: 2 }}>{MY_TRIP.driver.split(" ")[0]} approved your {MY_TRIP.depart} seat</div>
              <div style={{ fontSize: 11.5, color: "var(--ink-3)" }}>{MY_TRIP.from} → {MY_TRIP.to} · {FARE_POLICY.currency} {money(MY_TRIP.price)}</div>
            </div>
            <button style={{ minHeight: 44, minWidth: 44, padding: "0 8px", fontSize: 12, fontWeight: 700, color: "var(--ink-3)" }}>Later</button>
          </div>
          <div style={{ padding: "10px 14px", borderRadius: 14, background: "var(--status-none-soft)", border: "1px solid var(--line)", fontSize: 11, color: "var(--ink-3)", lineHeight: 1.45 }}>
            While a trip is live, passenger-side alerts never take the screen, never make sound, and never open a modal. They queue as one quiet card and a badge on Inbox. Only SOS and trip-critical driver alerts interrupt.
          </div>
          <div style={{ padding: "12px 14px", borderRadius: 18, background: "var(--ink-fill)", color: "#f4ece0", display: "flex", alignItems: "center", gap: 12 }}>
            <Avatar name={LIVE_DRIVE.nextDrop.name} size={38}/>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 13, fontWeight: 700 }}>Next: drop {LIVE_DRIVE.nextDrop.name}</div>
              <div style={{ fontSize: 11.5, opacity: .7 }}>{LIVE_DRIVE.nextDrop.place} · {LIVE_DRIVE.nextDrop.km} km</div>
            </div>
            <div className="rs-btn accent" style={{ height: 44, padding: "0 16px", fontSize: 13 }}>Drop off</div>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ CONFLICT 3 · publishing gate before KYC is complete ═══════════
function ConflictPublishGateScreen() {
  const items = DRIVER_VERIFICATION.docs;
  const blockers = vBlockers();
  return (
    <Phone label="S12 Publishing gate">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "14px 20px", display: "flex", alignItems: "center", gap: 12, background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", marginLeft: -8 }} aria-label="Close"><Icon name="close" size={19}/></button>
          <div style={{ fontSize: 15.5, fontWeight: 700 }}>Publish a trip</div>
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: "20px 20px 0" }} className="rs-scroll">
          <div style={{ width: 58, height: 58, borderRadius: 20, background: "var(--status-pending-soft)", display: "flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="lock" size={26} color="var(--status-pending)"/>
          </div>
          <div className="rs-display" style={{ fontSize: 25, marginTop: 16, lineHeight: 1.2 }}>{blockers.length} things left before<br/>you can publish</div>
          <div style={{ fontSize: 13.5, color: "var(--ink-3)", marginTop: 10, lineHeight: 1.55 }}>Riders only see verified drivers. Your route draft is saved — finish these and publish it in one tap.</div>
          <div style={{ marginTop: 20, display: "flex", flexDirection: "column", gap: 10 }}>
            {items.map(doc => {
              const m = STATUS_META[doc.st];
              const done = doc.st === "approved";
              return (
                <div key={doc.key} style={{ padding: 14, borderRadius: 16, background: "var(--surface)", border: `1px solid ${done ? "var(--line)" : "var(--line-2)"}`, display: "flex", alignItems: "center", gap: 12 }}>
                  <div style={{ width: 36, height: 36, borderRadius: 12, background: m.bg, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                    <Icon name={m.icon} size={17} color={m.c} strokeWidth={2.2}/>
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13.5, fontWeight: 700, color: done ? "var(--ink-3)" : "var(--ink)", textDecoration: done ? "line-through" : "none" }}>{doc.label}</div>
                    <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>{doc.detail}</div>
                  </div>
                  {doc.action && <div className="rs-btn accent" style={{ height: 44, padding: "0 18px", fontSize: 12.5, flexShrink: 0 }}>{doc.action}</div>}
                </div>
              );
            })}
          </div>
          <div style={{ marginTop: 16, padding: "12px 14px", borderRadius: 16, background: "var(--status-none-soft)", border: "1px solid var(--line)", display: "flex", gap: 10 }}>
            <Icon name="receipt" size={17} color="var(--ink-3)"/>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", lineHeight: 1.45 }}><b style={{ color: "var(--ink-2)" }}>Draft saved.</b> {NEXT_DRIVE.from} → {NEXT_DRIVE.to}, weekdays 7:45 AM, {NEXT_DRIVE.seatsTotal} seats.</div>
          </div>
        </div>
        <div style={{ padding: "12px 20px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">{blockers[0].action} {blockers[0].label.toLowerCase()}</button>
          <button className="rs-btn soft full" style={{ marginTop: 10 }}>Back to riding</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ CONFLICT 4 · account suspended (blocks both modes) ═══════════
function SuspendedScreen() {
  return (
    <Phone label="S13 Account suspended">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)", padding: "20px 24px 22px" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <Lockup Mark={MarkOverlap} size={26}/>
          <button style={{ minHeight: 44, fontSize: 13, fontWeight: 700, color: "var(--ink-3)" }}>Sign out</button>
        </div>
        <div style={{ flex: 1, display: "flex", flexDirection: "column", justifyContent: "center", gap: 18 }}>
          <div style={{ width: 66, height: 66, borderRadius: 22, background: "var(--status-rejected-soft)", display: "flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="lock" size={30} color="var(--status-rejected)"/>
          </div>
          <div>
            <div className="rs-display" style={{ fontSize: 28, lineHeight: 1.18 }}>Your account is<br/>on hold</div>
            <div style={{ fontSize: 14, color: "var(--ink-3)", marginTop: 12, lineHeight: 1.6 }}>You can't book or publish trips while this is open. Any money owed to you is unaffected and will still be paid out.</div>
          </div>
          <div style={{ padding: 16, borderRadius: 18, background: "var(--surface)", border: "1px solid var(--status-rejected)" }}>
            <div className="rs-section-label" style={{ color: "var(--status-rejected-ink)" }}>REASON GIVEN</div>
            <div style={{ fontSize: 13.5, fontWeight: 700, marginTop: 8, lineHeight: 1.45 }}>Two reports of a driver not matching the licence photo</div>
            <div style={{ fontSize: 12, color: "var(--ink-3)", marginTop: 8, lineHeight: 1.5 }}>Suspended 24 Jul, 11:20 AM · Case #SL-40912 · Reviewed by ComiGo operations</div>
          </div>
          <div style={{ padding: "12px 14px", borderRadius: 16, background: "var(--status-none-soft)", border: "1px solid var(--line)", display: "flex", gap: 10 }}>
            <Icon name="mail" size={17} color="var(--ink-3)"/>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", lineHeight: 1.45 }}>Most holds are resolved in 2–3 working days once we've heard from you. Replying opens a support ticket you can follow in the app.</div>
          </div>
        </div>
        <button className="rs-btn accent full">Appeal this — contact support</button>
        <button className="rs-btn ghost full" style={{ marginTop: 10 }}>Read the community rules</button>
      </div>
    </Phone>
  );
}

// ═══════════ TAB BAR SPEC (wide board) ═══════════
function TabBarSpecBoard() {
  const rules = [
    ["Home", "home", "Dot only, when a trip needs attention today (booking declined, driver waiting).", "Never a count."],
    ["Trips", "history", "Passenger: count of bookings awaiting driver approval. Driver: count of pending seat requests.", "Clears on tab open."],
    ["Publish / Find", "action", "Never badged — it is an action, not a destination.", "—"],
    ["Inbox", "bell", "Unread notification count, capped at 9+. Operator broadcasts included.", "Clears per item read."],
    ["Account", "user", "Dot only, for verification action needed or a document expiring within 30 days.", "Never a count."],
  ];
  return (
    <BBoard>
      <BTitle sub="Five slots, identical positions in both modes. Only slot 3 changes meaning, and only the accent colour tells you which mode you're in. Ported to React Navigation as one tab navigator whose screen set is swapped by mode — the navigator itself is never unmounted, so an in-progress trip survives the switch.">Tab bars &amp; badge rules</BTitle>
      <div style={{ display: "flex", gap: 20 }}>
        <BCard style={{ width: 420, gap: 16 }}>
          <BLabel>Passenger mode · accent terracotta</BLabel>
          <div style={{ border: "1px solid var(--line)", borderRadius: 18, overflow: "hidden" }}>
            <TabBar mode="ride" active="home" badges={{ inbox: 3 }}/>
          </div>
          <BLabel>Driver mode · accent teal</BLabel>
          <div style={{ border: "1px solid var(--line)", borderRadius: 18, overflow: "hidden" }}>
            <TabBar mode="drive" active="home" badges={{ trips: 2, inbox: 5 }}/>
          </div>
          <BLabel>Active states · every tab, driver mode</BLabel>
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            {["trips", "inbox", "account"].map(a => (
              <div key={a} style={{ border: "1px solid var(--line)", borderRadius: 18, overflow: "hidden" }}>
                <TabBar mode="drive" active={a} badges={{ account: true }}/>
              </div>
            ))}
          </div>
        </BCard>
        <BCard style={{ flex: 1, gap: 0, padding: 0, overflow: "hidden" }}>
          <div style={{ display: "flex", padding: "14px 20px", background: "var(--bg-soft)", fontSize: 10, fontWeight: 800, letterSpacing: ".12em", color: "var(--ink-3)" }}>
            <div style={{ width: 120 }}>SLOT</div><div style={{ flex: 1 }}>BADGE RULE</div><div style={{ width: 150 }}>CLEARS</div>
          </div>
          {rules.map(([l, ic, rule, clears]) => (
            <div key={l} style={{ display: "flex", padding: "14px 20px", borderTop: "1px solid var(--line)", fontSize: 12.5, alignItems: "flex-start" }}>
              <div style={{ width: 120, display: "flex", alignItems: "center", gap: 8, fontWeight: 700, flexShrink: 0 }}>
                <Icon name={ic === "action" ? "plus" : ic} size={16} color="var(--ink-3)"/>{l}
              </div>
              <div style={{ flex: 1, color: "var(--ink-2)", lineHeight: 1.5, paddingRight: 16 }}>{rule}</div>
              <div style={{ width: 150, color: "var(--ink-3)", lineHeight: 1.5 }}>{clears}</div>
            </div>
          ))}
          <div style={{ borderTop: "1px solid var(--line)", padding: "16px 20px", background: "var(--bg-soft)" }}>
            <BLabel>Mode-switch contract</BLabel>
            <div style={{ fontSize: 12.5, color: "var(--ink-2)", lineHeight: 1.6, marginTop: 8 }}>
              Switching mode swaps the tab set and the home surface. It never signs out, never clears navigation history for the other mode, and never cancels an in-progress trip. A live trip in the mode you left stays pinned as a bar above the tab bar until it ends.
            </div>
          </div>
        </BCard>
      </div>
    </BBoard>
  );
}

Object.assign(window, {
  SwitchSheet, ModeOptionRow,
  ModeStateNoneScreen, ModeStatePendingScreen, ModeStateRejectedScreen,
  ConflictActiveTripScreen, ConflictDrivingNotifScreen, ConflictPublishGateScreen, SuspendedScreen,
  TabBarSpecBoard,
});
