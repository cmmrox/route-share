// driver-live.jsx — D17…D24: pre-trip checklist, live trip (two variations),
// arrived, boarding, no-show, drop-off, cash collected, trip complete.

const PAX = [
  { name: "Dinuka S", from: "Narahenpita", to: "Bambalapitiya", fare: 279, net: 251, pay: "card", st: "waiting" },
  { name: "Tharindu M", from: "Nugegoda", to: "Thunmulla", fare: 198, net: 178, pay: "cash", st: "boarded" },
  { name: "Sanduni K", from: "Rajagiriya", to: "Bambalapitiya", fare: 240, net: 216, pay: "card", st: "boarded" },
];

// ═══════════ D17 · PRE-TRIP CHECKLIST ═══════════
function DvChecklistScreen() {
  return (
    <Phone label="D17 Pre-trip checklist">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Before you go" sub={`${NEXT_DRIVE.depart} · ${NEXT_DRIVE.from} → ${NEXT_DRIVE.to}`}/>
        <div style={{ flex: 1, overflow: "auto", padding: "16px 16px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 12 }}>PICKING UP {PAX.length} PEOPLE</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
              {PAX.map(p => (
                <div key={p.name} style={{ display: "flex", alignItems: "center", gap: 11 }}>
                  <Avatar name={p.name} size={38}/>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13.5, fontWeight: 700 }}>{p.name}</div>
                    <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 1 }}>{p.from} → {p.to}</div>
                  </div>
                  {p.pay === "cash"
                    ? <span className="rs-chip" style={{ height: 24, background: "var(--status-approved-soft)", color: "var(--status-approved-ink)", borderColor: "transparent" }}><Icon name="cash" size={11}/> Cash</span>
                    : <span className="rs-chip" style={{ height: 24 }}><Icon name="card" size={11}/> Card · on start</span>}
                </div>
              ))}
            </div>
          </div>
          <div className="rs-card" style={{ padding: "4px 14px" }}>
            {[
              ["car", "Vehicle is the one on your profile", `${MY_VEHICLE.make} · ${MY_VEHICLE.plate}`, true],
              ["users", "Seats are clear and belts work", "3 passengers today", true],
              ["phone", "Phone mounted and charged", "You'll need navigation for 32 minutes", false],
            ].map(([ic, l, s, done], i) => (
              <div key={l}>
                {i > 0 && <div className="rs-divider"/>}
                <div style={{ padding: "13px 0", display: "flex", alignItems: "center", gap: 12 }}>
                  <div style={{ width: 24, height: 24, borderRadius: 12, border: `2px solid ${done ? "var(--mode-drive)" : "var(--line-2)"}`, background: done ? "var(--mode-drive)" : "transparent", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                    {done && <Icon name="check" size={13} color="var(--on-bright-fill)" strokeWidth={3}/>}
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13.5, fontWeight: 600 }}>{l}</div>
                    <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>{s}</div>
                  </div>
                </div>
              </div>
            ))}
          </div>
          <Banner kind="info" icon="card" title={`Starting charges ${PAX.length} cards`}
            body={`Nobody has paid yet — every booked card is captured the moment you tap start. Anyone who books after that is charged on acceptance.`}/>
          <Banner kind="warn" icon="clock" title={`Start within ${POLICY.startBufferMin} minutes of ${NEXT_DRIVE.depart}`}
            body="After the buffer ComiGo cancels the trip, nobody is charged, and the miss counts against your reliability."/>
          <Banner kind="info" icon="shield" title="Drive the route you published"
            body="Riders matched on it. Big detours change their fare and can end the trip early."/>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn full" style={{ background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Start the trip</button>
        </div>
      </div>
    </Phone>
  );
}

// ── shared live-trip navigation header ──
function NavHeader({ dist = "400 m", instr = "Continue on Nawala Road", onBoard = 3 }) {
  return (
    <div style={{ padding: "14px 16px", borderRadius: 20, background: "var(--ink-fill)", color: "var(--on-ink-fill)", display: "flex", alignItems: "center", gap: 12, boxShadow: "var(--shadow-lg)" }}>
      <div style={{ width: 42, height: 42, borderRadius: 14, background: "rgba(255,255,255,.14)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
        <Icon name="arrow" size={22} color="var(--on-ink-fill)"/>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div className="rs-display" style={{ fontSize: 20 }}>{dist}</div>
        <div style={{ fontSize: 12, opacity: .75 }}>{instr}</div>
      </div>
      <div className="rs-chip teal" style={{ height: 26, background: "rgba(72,168,159,.2)", color: "#48a89f", border: "none", flexShrink: 0 }}>{onBoard} on board</div>
    </div>
  );
}

// ═══════════ D18 · LIVE TRIP · map-dominant ═══════════
// Two states: the ordinary next stop, and a passenger asking to get out early.
// The request has to reach the driver as a thing to DO, not a notification.
function DvLiveTripScreen({ earlyRequest = false }) {
  const e = LIVE_DRIVE.earlyDrop;
  return (
    <Phone label={`${earlyRequest ? "D18c" : "D18"} Live trip · ${earlyRequest ? "early drop request" : "map"}`}>
      <div style={{ height: "100%", position: "relative", display: "flex", flexDirection: "column" }}>
        <MapBackdrop pickupLabel={LIVE_DRIVE.from} dropLabel={LIVE_DRIVE.to}/>
        <div style={{ position: "relative", padding: "10px 14px 0" }}>
          <NavHeader onBoard={LIVE_DRIVE.onBoard}/>
        </div>
        <div style={{ flex: 1 }}/>
        <div style={{ position: "relative", padding: "0 14px 12px", display: "flex", flexDirection: "column", gap: 10 }}>
          <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
            <button className="rs-tap"><span className="rs-chip" style={{ background: "var(--surface)", boxShadow: "var(--shadow-md)", border: "none" }}><Icon name="menu" size={12}/> List view</span></button>
            <button className="rs-btn" style={{ width: 52, background: "var(--danger-soft)", boxShadow: "var(--shadow-md)" }} aria-label="Emergency SOS">
              <Icon name="sos" size={19} color="var(--status-rejected-ink)"/>
            </button>
          </div>
          {earlyRequest ? (
            <div style={{ padding: 15, borderRadius: 20, background: "var(--surface)", boxShadow: "var(--shadow-lg)", border: "1.5px solid var(--status-pending)" }}>
              <div style={{ display: "flex", alignItems: "center", gap: 9, marginBottom: 12 }}>
                <div style={{ width: 8, height: 8, borderRadius: 4, background: "var(--status-pending)", animation: "pulse 1.8s infinite", flexShrink: 0 }}/>
                <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", color: "var(--status-pending-ink)", flex: 1 }}>EARLY DROP-OFF REQUESTED</div>
                <div className="tab" style={{ fontSize: 11, color: "var(--ink-3)" }}>{e.aheadM} m ahead</div>
              </div>
              <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <Avatar name={e.name} size={44}/>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 14.5, fontWeight: 800 }}>{e.name} wants out at {e.place}</div>
                  <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>Seat {e.seat} · {(e.bookedDist - e.actualDist).toFixed(1)} km before her booked stop</div>
                </div>
                <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label={`Chat with ${e.name}`}>
                  <Icon name="chat" size={19}/>
                </button>
              </div>
              <div style={{ marginTop: 12, padding: "10px 12px", borderRadius: 12, background: "var(--bg-soft)", border: "1px solid var(--line)", display: "flex", gap: 9 }}>
                <Icon name="shield" size={15} color="var(--ink-3)"/>
                <div style={{ fontSize: 11.5, color: "var(--ink-2)", flex: 1, lineHeight: 1.45 }}>
                  Stop only where it's safe. Confirm the drop after she's out — that's what sets her fare.
                </div>
              </div>
              <button className="rs-btn full" style={{ marginTop: 12, background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Pulling over at {e.place}</button>
              <button className="rs-btn ghost full" style={{ marginTop: 9, height: 44, fontSize: 12.5 }}>Can't stop here — next safe spot</button>
            </div>
          ) : (
            <div style={{ padding: 15, borderRadius: 20, background: "var(--surface)", boxShadow: "var(--shadow-lg)" }}>
              <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <Avatar name={LIVE_DRIVE.nextDrop.name} size={44}/>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", color: "var(--mode-drive-ink)" }}>NEXT STOP · {LIVE_DRIVE.nextDrop.km} KM</div>
                  <div style={{ fontSize: 14.5, fontWeight: 800, marginTop: 2 }}>Drop {LIVE_DRIVE.nextDrop.name}</div>
                  <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 1 }}>{LIVE_DRIVE.nextDrop.place}</div>
                </div>
                <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label={`Call ${LIVE_DRIVE.nextDrop.name}`}>
                  <Icon name="phone" size={19}/>
                </button>
              </div>
              <button className="rs-btn accent full" style={{ marginTop: 13 }}>Drop off here</button>
            </div>
          )}
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D18b · LIVE TRIP · list-dominant ═══════════
function DvLiveListScreen() {
  return (
    <Phone label="D18b Live trip · list">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "10px 14px 12px", background: "var(--bg)", flexShrink: 0 }}>
          <NavHeader onBoard={LIVE_DRIVE.onBoard}/>
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: "0 16px 16px" }} className="rs-scroll">
          <div className="rs-section-label" style={{ marginBottom: 10 }}>STOPS AHEAD</div>
          <RouteTimeline stops={[
            { kind: "via", place: "Rajagiriya junction", time: "Done", note: "Sanduni K boarded" },
            { kind: "drop", place: LIVE_DRIVE.nextDrop.place, time: "10:31 AM", note: `Drop ${LIVE_DRIVE.nextDrop.name} · ${LIVE_DRIVE.nextDrop.km} km away` },
            { kind: "via", place: "Thunmulla", time: "10:36 AM", note: "Drop Tharindu M · collect cash" },
            { kind: "drop", place: LIVE_DRIVE.to, time: "10:48 AM", note: "Trip ends" },
          ]}/>
          <div className="rs-section-label" style={{ margin: "18px 0 10px" }}>ON BOARD · {LIVE_DRIVE.onBoard}</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 9 }}>
            {PAX.map(p => (
              <div key={p.name} className="rs-card" style={{ padding: 13, display: "flex", alignItems: "center", gap: 11 }}>
                <Avatar name={p.name} size={38}/>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 13.5, fontWeight: 700 }}>{p.name}</div>
                  <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 1 }}>Off at {p.to}</div>
                </div>
                {p.pay === "cash" && <span className="rs-chip" style={{ height: 24, background: "var(--status-approved-soft)", color: "var(--status-approved-ink)", borderColor: "transparent" }}>{FARE_POLICY.currency} {money(p.fare)}</span>}
                <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label={`Call ${p.name}`}>
                  <Icon name="phone" size={17}/>
                </button>
              </div>
            ))}
          </div>
          <div className="rs-card" style={{ padding: 13, marginTop: 10, display: "flex", alignItems: "center", gap: 11, border: "1.5px solid var(--mode-drive)" }}>
            <div style={{ width: 38, height: 38, borderRadius: 12, background: "var(--mode-drive-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
              <Icon name="users" size={18} color="var(--mode-drive-ink)"/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 13, fontWeight: 700 }}>Seat {LIVE_DRIVE.earlyDrop.seat} is free from {LIVE_DRIVE.earlyDrop.place}</div>
              <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>On sale for {LIVE_DRIVE.earlyDrop.remainingLeg}. Mid-trip bookings are charged on acceptance.</div>
            </div>
          </div>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)", display: "flex", gap: 9 }}>
          <button className="rs-btn" style={{ width: 56, background: "var(--danger-soft)" }} aria-label="Emergency SOS">
            <Icon name="sos" size={19} color="var(--status-rejected-ink)"/>
          </button>
          <button className="rs-btn accent" style={{ flex: 1 }}>Drop {LIVE_DRIVE.nextDrop.name}</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D19 · ARRIVED AT PICKUP ═══════════
// The wait clock starts on GPS arrival, not when the driver feels like it, so a
// no-show can't be manufactured two streets away. One extension, then the seat
// is released and the driver goes.
function DvArrivedScreen({ extended = false }) {
  const total = POLICY.pickupWaitMin + (extended ? POLICY.pickupWaitExtendMin : 0);
  return (
    <Phone label={`${extended ? "D19b" : "D19"} Arrived · waiting ${extended ? "extended" : ""}`.trim()}>
      <div style={{ height: "100%", position: "relative", display: "flex", flexDirection: "column" }}>
        <MapBackdrop pickupLabel="You are here" dropLabel={LIVE_DRIVE.to}/>
        <div style={{ position: "relative", padding: "10px 14px 0" }}>
          <div style={{ padding: "14px 16px", borderRadius: 20, background: "var(--mode-drive)", color: "var(--on-bright-fill)", display: "flex", alignItems: "center", gap: 12, boxShadow: "var(--shadow-lg)" }}>
            <Icon name="pin" size={22} color="var(--on-bright-fill)"/>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 15, fontWeight: 800 }}>Waiting at the pickup</div>
              <div style={{ fontSize: 12, opacity: .85, marginTop: 1 }}>Narahenpita junction · started automatically on arrival</div>
            </div>
          </div>
        </div>
        <div style={{ flex: 1 }}/>
        <div className="rs-sheet" style={{ position: "relative", padding: "6px 16px 14px" }}>
          <div className="rs-sheet-grab"/>
          <div style={{ display: "flex", alignItems: "center", gap: 12, marginTop: 4 }}>
            <Avatar name="Dinuka S" size={48}/>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 15, fontWeight: 800 }}>Dinuka S</div>
              <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>Notified you're here · 2 min ago</div>
            </div>
            <button style={{ width: 46, height: 46, borderRadius: 23, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label="Call Dinuka S"><Icon name="phone" size={20}/></button>
            <button style={{ width: 46, height: 46, borderRadius: 23, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label="Chat with Dinuka S"><Icon name="chat" size={20}/></button>
          </div>
          <div style={{ marginTop: 13, padding: 14, borderRadius: 16, background: extended ? "var(--status-rejected-soft)" : "var(--status-pending-soft)", border: `1px solid ${extended ? "var(--status-rejected)" : "var(--status-pending)"}` }}>
            <div style={{ display: "flex", alignItems: "center", gap: 11 }}>
              <Icon name="clock" size={18} color={extended ? "var(--status-rejected-ink)" : "var(--status-pending-ink)"}/>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 12.5, fontWeight: 700, color: extended ? "var(--status-rejected-ink)" : "var(--status-pending-ink)" }}>
                  {extended ? `Final ${POLICY.pickupWaitExtendMin} minutes — no more extensions` : `${POLICY.pickupWaitMin}-minute wait running`}
                </div>
                <div className="tab" style={{ fontSize: 11.5, color: "var(--ink-2)", marginTop: 2 }}>{extended ? "4:38" : "3:12"} remaining of {total} min</div>
              </div>
            </div>
            <div style={{ display: "flex", gap: 5, marginTop: 11 }}>
              {Array.from({ length: extended ? 2 : 1 }, (_, i) => (
                <div key={i} style={{ flex: 1, height: 6, borderRadius: 3, background: extended && i === 0 ? "var(--status-rejected)" : "var(--surface)", border: `1px solid ${extended ? "var(--status-rejected)" : "var(--status-pending)"}` }}/>
              ))}
              {!extended && <div style={{ flex: 1, height: 6, borderRadius: 3, background: "var(--surface)", border: "1px dashed var(--line-2)" }}/>}
            </div>
          </div>
          <div style={{ marginTop: 11, fontSize: 11.5, color: "var(--ink-3)", lineHeight: 1.45 }}>
            {extended
              ? `When this runs out his seat is released, he's charged ${POLICY.noShowPenaltyPct}% of his fare, and ${POLICY.penaltyVictimPct}% of that fee is credited to you for the wait.`
              : `You can add ${POLICY.pickupWaitExtendMin} more minutes once. After that his seat is released and you carry on.`}
          </div>
          <div style={{ display: "flex", gap: 9, marginTop: 12 }}>
            {extended
              ? <button className="rs-btn ghost" style={{ flex: 1, height: 50, fontSize: 13, color: "var(--status-rejected-ink)" }}>Release his seat</button>
              : <button className="rs-btn soft" style={{ flex: 1, height: 50, fontSize: 13 }}>+{POLICY.pickupWaitExtendMin} min</button>}
            <button className="rs-btn" style={{ flex: 1.6, height: 50, background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Dinuka is in</button>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D20 · BOARDING ═══════════
function DvBoardingScreen() {
  return (
    <Phone label="D20 Boarding">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Who's on board?" sub={`${LIVE_DRIVE.from} → ${LIVE_DRIVE.to}`}/>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 10 }} className="rs-scroll">
          {PAX.map(p => {
            const boarded = p.st === "boarded";
            return (
              <div key={p.name} className="rs-card" style={{ padding: 14, border: `1.5px solid ${boarded ? "var(--mode-drive)" : "var(--line)"}`, background: boarded ? "var(--mode-drive-soft)" : "var(--surface)" }}>
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  <Avatar name={p.name} size={42}/>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 14, fontWeight: 700 }}>{p.name}</div>
                    <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>{p.from} → {p.to}</div>
                  </div>
                  {boarded
                    ? <div style={{ width: 28, height: 28, borderRadius: 14, background: "var(--mode-drive)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}><Icon name="check" size={15} color="var(--on-bright-fill)" strokeWidth={3}/></div>
                    : <StatusBadge status="pending" label="WAITING"/>}
                </div>
                {!boarded && (
                  <div style={{ display: "flex", gap: 9, marginTop: 12 }}>
                    <button className="rs-btn ghost" style={{ flex: 1, height: 44, fontSize: 12.5, color: "var(--status-rejected-ink)" }}>Release seat</button>
                    <button className="rs-btn" style={{ flex: 1.5, height: 44, fontSize: 12.5, background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Mark boarded</button>
                  </div>
                )}
              </div>
            );
          })}
          <Banner kind="info" icon="users" title="Only mark people who are actually in the car"
            body={`Their cards were charged when you started the trip. If someone hasn't turned up, release the seat instead — it goes back on sale and they're charged ${POLICY.noShowPenaltyPct}% of their fare.`}/>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn full" style={{ background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Continue the trip</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D21 · RELEASE A NO-SHOW'S SEAT ═══════════
// The driver gains nothing from this: the fee is ComiGo's, and his own fare for
// that seat goes back to the passenger. So it is framed as what it actually is —
// releasing a seat and moving on — never as protecting his earnings.
function DvNoShowScreen() {
  const fee = noShowPenalty(PAX[0].fare);
  return (
    <Phone label="D21 Release a no-show's seat">
      <div style={{ height: "100%", position: "relative", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ flex: 1, opacity: .38, display: "flex", flexDirection: "column" }}>
          <AppBar title="Who's on board?"/>
          <div style={{ padding: 16, display: "flex", flexDirection: "column", gap: 10 }}><SkelRow/><SkelRow/></div>
        </div>
        <div style={{ position: "absolute", inset: 0, background: "rgba(20,10,5,.5)" }}/>
        <div className="rs-sheet" style={{ position: "absolute", left: 0, right: 0, bottom: 0, padding: "8px 22px 22px" }}>
          <div className="rs-sheet-grab"/>
          <div style={{ width: 56, height: 56, borderRadius: 19, background: "var(--status-rejected-soft)", display: "flex", alignItems: "center", justifyContent: "center", marginTop: 10 }}>
            <Icon name="users" size={26} color="var(--status-rejected-ink)" strokeWidth={2.3}/>
          </div>
          <div className="rs-display" style={{ fontSize: 24, marginTop: 15, lineHeight: 1.2 }}>Release Dinuka's<br/>seat and go?</div>
          <div style={{ fontSize: 13, color: "var(--ink-3)", marginTop: 10, lineHeight: 1.6 }}>
            You waited the full {POLICY.pickupWaitMin + POLICY.pickupWaitExtendMin} minutes. His seat goes straight back on sale for the rest of the route — that, not the fee, is what you get back.
          </div>
          <div style={{ marginTop: 15, padding: 15, borderRadius: 16, background: "var(--surface)", border: "1px solid var(--line)", display: "flex", flexDirection: "column", gap: 11 }}>
            <RuleRow icon="users" tint="var(--status-approved-ink)" title={`Seat back on sale from ${PAX[0].from}`}
              body="Riders searching the rest of your route can book it while you drive."/>
            <RuleRow icon="card" title={`He's charged ${POLICY.noShowPenaltyPct}% · ${FARE_POLICY.currency} ${money(fee)}`}
              body={`The rest of his fare is refunded. The fee is split: ${FARE_POLICY.currency} ${money(victimShare(fee))} to you, ${FARE_POLICY.currency} ${money(platformShare(fee))} to ComiGo.`}/>
            <RuleRow icon="cash" tint="var(--status-approved-ink)" title={`${FARE_POLICY.currency} ${money(victimShare(fee))} compensation on your next payout`}
              body={`Your ${POLICY.penaltyVictimPct}% share of his fee, credited because the timer ran out with your car at the pickup point. It shows in your ledger as compensation, not as trip earnings.`}/>
            <RuleRow icon="star" tint="var(--status-approved-ink)" title="Your reliability is untouched"
              body="A no-show is his record, not yours. It doesn't count as a cancellation."/>
          </div>
          <button className="rs-btn full" style={{ marginTop: 16, background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Release the seat and continue</button>
          <button className="rs-btn soft full" style={{ marginTop: 10 }}>Keep waiting</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D22 · DROP OFF ═══════════
// Planned drop, and the early one. Confirming an early drop is what fixes the
// distance travelled, recalculates the fare, and puts the seat back on sale.
function DvDropOffScreen({ kind = "planned" }) {
  const early = kind === "early";
  const e = LIVE_DRIVE.earlyDrop;
  return (
    <Phone label={`${early ? "D22b" : "D22"} Drop off · ${early ? "early" : "planned"}`}>
      <div style={{ height: "100%", position: "relative", display: "flex", flexDirection: "column" }}>
        <MapBackdrop pickupLabel="You are here" dropLabel={LIVE_DRIVE.to}/>
        <div style={{ position: "absolute", inset: 0, background: "rgba(20,10,5,.5)" }}/>
        <div className="rs-sheet" style={{ position: "absolute", left: 0, right: 0, bottom: 0, top: early ? 30 : "auto", padding: "8px 22px 22px", overflow: early ? "auto" : "visible" }}>
          <div className="rs-sheet-grab"/>
          {early ? (
            <>
              <div className="rs-display" style={{ fontSize: 24, marginTop: 10, lineHeight: 1.2 }}>Confirm {e.name.split(" ")[0]}'s<br/>early drop-off</div>
              <div style={{ fontSize: 13, color: "var(--ink-3)", marginTop: 9, lineHeight: 1.55 }}>
                She asked to get out at {e.place}, {(e.bookedDist - e.actualDist).toFixed(1)} km before her booked stop. Confirm once she's out of the car — that fixes the distance and recalculates the fare.
              </div>
              <div style={{ marginTop: 15 }}>
                <FareBreakdown
                  currency={FARE_POLICY.currency} compact totalLabel="Her new fare"
                  lines={[
                    { label: `Booked · ${e.bookedDist} km`, sub: `Rajagiriya → ${LIVE_DRIVE.nextDrop.place}`, value: e.bookedFare, always: true },
                    { label: `Refund for ${(e.bookedDist - e.actualDist).toFixed(1)} km not travelled`, sub: `At ${FARE_POLICY.currency} ${money(FARE_POLICY.ratePerKm)} per km`, value: e.refund, kind: "discount" },
                  ]}
                  total={e.adjustedFare}
                  footnote={`You keep ${FARE_POLICY.currency} ${money(e.adjustedNet)} instead of ${FARE_POLICY.currency} ${money(e.bookedNet)} — you are not driving the last ${(e.bookedDist - e.actualDist).toFixed(1)} km for her. Adjustment ${e.usedThisMonth + 1} of ${e.allowance} on her account this month.`}/>
              </div>
              <div style={{ marginTop: 13, display: "flex", flexDirection: "column", gap: 10 }}>
                <Banner kind="good" icon="users" title={`Seat ${e.seat} goes back on sale`}
                  body={`Free for ${e.remainingLeg}. Riders searching that stretch can book it while you drive — and those bookings are charged the moment you accept.`}/>
                <Banner kind="warn" icon="pin" title="Confirm only once she's out"
                  body="The adjustment is timestamped where you confirm, so confirming early under-charges her and costs you."/>
              </div>
              <button className="rs-btn full" style={{ marginTop: 15, background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Confirm drop-off at {e.place}</button>
              <button className="rs-btn soft full" style={{ marginTop: 10 }}>She stayed on</button>
            </>
          ) : (
            <>
              <div className="rs-display" style={{ fontSize: 24, marginTop: 10, lineHeight: 1.2 }}>Dropping {LIVE_DRIVE.nextDrop.name}</div>
              <div style={{ fontSize: 13, color: "var(--ink-3)", marginTop: 9, lineHeight: 1.55 }}>
                At {LIVE_DRIVE.nextDrop.place}, as booked. Her card was charged when you started the trip — confirming closes the fare at the full booked distance.
              </div>
              <div style={{ marginTop: 15, padding: 14, borderRadius: 16, background: "var(--surface)", border: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 12 }}>
                <Avatar name={LIVE_DRIVE.nextDrop.name} size={44}/>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 14, fontWeight: 700 }}>{LIVE_DRIVE.nextDrop.name}</div>
                  <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>Rajagiriya → {LIVE_DRIVE.nextDrop.place} · paid by card</div>
                </div>
                <div style={{ textAlign: "right", flexShrink: 0 }}>
                  <div className="rs-display tab" style={{ fontSize: 20, fontWeight: 600, color: "var(--status-approved-ink)" }}>{FARE_POLICY.currency} {money(PAX[2].net)}</div>
                  <div style={{ fontSize: 10, color: "var(--ink-3)" }}>you keep</div>
                </div>
              </div>
              <div style={{ marginTop: 12 }}>
                <Banner kind="info" icon="pin" title="Getting out here instead?"
                  body="An early drop-off recalculates her fare on actual distance and frees the seat for the rest of the route." action="Early drop"/>
              </div>
              <button className="rs-btn full" style={{ marginTop: 15, background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Confirm drop-off</button>
              <button className="rs-btn soft full" style={{ marginTop: 10 }}>Not yet</button>
            </>
          )}
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D23 · CASH COLLECTED ═══════════
function DvCashScreen() {
  const p = PAX[1];
  return (
    <Phone label="D23 Cash collected">
      <div style={{ height: "100%", position: "relative", display: "flex", flexDirection: "column" }}>
        <MapBackdrop pickupLabel="You are here" dropLabel={LIVE_DRIVE.to}/>
        <div style={{ position: "absolute", inset: 0, background: "rgba(20,10,5,.5)" }}/>
        <div className="rs-sheet" style={{ position: "absolute", left: 0, right: 0, bottom: 0, padding: "8px 22px 22px" }}>
          <div className="rs-sheet-grab"/>
          <div style={{ width: 56, height: 56, borderRadius: 19, background: "var(--status-approved-soft)", display: "flex", alignItems: "center", justifyContent: "center", marginTop: 10 }}>
            <Icon name="cash" size={26} color="var(--status-approved-ink)"/>
          </div>
          <div className="rs-display" style={{ fontSize: 24, marginTop: 15, lineHeight: 1.2 }}>Collect cash from<br/>{p.name}</div>
          <div style={{ fontSize: 13, color: "var(--ink-3)", marginTop: 10, lineHeight: 1.6 }}>
            Take the full fare in cash. ComiGo's {FARE_POLICY.commissionPct}% fee is deducted from your next payout, not from this trip.
          </div>
          <div style={{ marginTop: 16, padding: 16, borderRadius: 18, background: "var(--status-approved-soft)", border: "1px solid var(--status-approved)" }}>
            <div style={{ fontSize: 11.5, fontWeight: 700, color: "var(--status-approved-ink)" }}>ASK FOR</div>
            <div className="rs-display tab" style={{ fontSize: 38, lineHeight: 1.05, marginTop: 5 }}>{FARE_POLICY.currency} {money(p.fare)}</div>
            <div style={{ height: 1, background: "var(--status-approved)", opacity: .3, margin: "13px 0" }}/>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <div style={{ fontSize: 12, color: "var(--ink-2)", flex: 1 }}>You keep after the fee</div>
              <div className="tab" style={{ fontSize: 15, fontWeight: 800 }}>{FARE_POLICY.currency} {money(p.net)}</div>
            </div>
          </div>
          <button className="rs-btn full" style={{ marginTop: 16, background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Cash received</button>
          <button className="rs-btn ghost full" style={{ marginTop: 10, color: "var(--status-rejected-ink)" }}>They couldn't pay</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D24 · TRIP COMPLETE ═══════════
// Also where a late-cancellation penalty is actually collected: out of what the
// driver earns on the next completed trip, never billed separately.
function DvTripCompleteScreen() {
  const gross = PAX.reduce((a, p) => a + p.net, 0);
  const penalty = cancelPenalty();
  const total = gross - penalty;
  return (
    <Phone label="D24 Trip complete">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "20px 20px 24px", background: "var(--ink-fill)", color: "var(--on-ink-fill)", borderBottomLeftRadius: 28, borderBottomRightRadius: 28, flexShrink: 0 }}>
          <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em", opacity: .65 }}>TRIP COMPLETE · {LIVE_DRIVE.from} → {LIVE_DRIVE.to}</div>
          <div className="rs-display tab" style={{ fontSize: 42, lineHeight: 1.05, marginTop: 6 }}>{FARE_POLICY.currency} {money(total)}</div>
          <div style={{ fontSize: 12.5, opacity: .82, marginTop: 5 }}>{PAX.length} passengers · after the {FARE_POLICY.commissionPct}% fee and a {FARE_POLICY.currency} {money(penalty)} penalty</div>
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 12 }}>WHO PAID WHAT</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
              {PAX.map(p => (
                <div key={p.name} style={{ display: "flex", alignItems: "center", gap: 11 }}>
                  <Avatar name={p.name} size={36}/>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13, fontWeight: 700 }}>{p.name}</div>
                    <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 1 }}>{p.pay === "cash" ? "Cash collected" : "Card charged at trip start"}</div>
                  </div>
                  <div className="tab" style={{ fontSize: 13.5, fontWeight: 800 }}>{FARE_POLICY.currency} {money(p.net)}</div>
                </div>
              ))}
              <div style={{ height: 1, background: "var(--line)" }}/>
              <div style={{ display: "flex", alignItems: "center", gap: 11 }}>
                <div style={{ width: 36, height: 36, borderRadius: 12, background: "var(--status-rejected-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                  <Icon name="alert" size={17} color="var(--status-rejected-ink)"/>
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 13, fontWeight: 700 }}>Late-cancellation penalty</div>
                  <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 1 }}>{POLICY.lateCancelPenaltyPct}% of the trip you cancelled {MY_TRIP.cancelledBeforeHrs} h before departure</div>
                </div>
                <div className="tab" style={{ fontSize: 13.5, fontWeight: 800, color: "var(--status-rejected-ink)" }}>−{FARE_POLICY.currency} {money(penalty)}</div>
              </div>
            </div>
          </div>
          <Banner kind="good" icon="cash" title={`Card fares reach you on ${PAYOUT.day}`}
            body={`Cash you already have. ComiGo pays out weekly, and only above ${FARE_POLICY.currency} ${money(PAYOUT.minimum)} — anything below that rolls into next week.`}/>
          {/* Rating is mutual: they rate you at the same time, and neither score
              is published until both are in or the window closes. */}
          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 4 }}>RATE YOUR PASSENGERS</div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginBottom: 13, lineHeight: 1.45 }}>
              They're rating you too. Both go live together, both are signed with a first name and initial, and each of you can reply once.
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: 13 }}>
              {PAX.map((p, i) => (
                <div key={p.name} style={{ display: "flex", alignItems: "center", gap: 11 }}>
                  <Avatar name={p.name} size={38}/>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13, fontWeight: 700 }}>{p.name}</div>
                    <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 1 }}>{i === 0 ? "Rated you 5★" : "Hasn't rated yet"}</div>
                  </div>
                  <div style={{ display: "flex", gap: 3, flexShrink: 0 }}>
                    {[1, 2, 3, 4, 5].map(s => (
                      <button key={s} style={{ width: 30, height: 44, display: "inline-flex", alignItems: "center", justifyContent: "center" }} aria-label={`Rate ${p.name} ${s}`}>
                        <Icon name="star" size={19} color={i === 0 && s <= 5 ? "var(--status-pending-ink)" : "var(--line-2)"}/>
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn full" style={{ background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Send ratings</button>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, {
  PAX, NavHeader, DvChecklistScreen, DvLiveTripScreen, DvLiveListScreen, DvArrivedScreen,
  DvBoardingScreen, DvNoShowScreen, DvDropOffScreen, DvCashScreen, DvTripCompleteScreen,
});
