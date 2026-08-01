// passenger-trip.jsx — P15…P21: live trip, early exit, receipt, rating, saved
// places, trip sharing and the public recipient view.

// ═══════════ P15 · IN TRIP ═══════════
function PxInTripScreen() {
  const r = MY_TRIP;
  return (
    <Phone label="P15 In trip">
      <div style={{ height: "100%", position: "relative", display: "flex", flexDirection: "column" }}>
        <MapBackdrop pickupLabel="Picked up" dropLabel="Nugegoda"/>
        <div style={{ position: "relative", padding: "10px 14px 0", display: "flex", alignItems: "center", gap: 9 }}>
          <div style={{ flex: 1, padding: "11px 14px", borderRadius: 16, background: "var(--ink-fill)", color: "var(--on-ink-fill)", boxShadow: "var(--shadow-lg)", display: "flex", alignItems: "center", gap: 11 }}>
            <div style={{ width: 9, height: 9, borderRadius: 5, background: "#e8834f", animation: "pulse 1.8s infinite", flexShrink: 0 }}/>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", opacity: .68 }}>ON THE WAY</div>
              <div style={{ fontSize: 14, fontWeight: 700, marginTop: 2 }}>Nugegoda by 6:38 PM</div>
            </div>
            <div className="tab" style={{ fontSize: 12, opacity: .8, flexShrink: 0 }}>2.1 km</div>
          </div>
          <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--surface)", boxShadow: "var(--shadow-md)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label="View trip as a list"><Icon name="menu" size={19}/></button>
        </div>
        <div style={{ flex: 1 }}/>
        <div className="rs-sheet" style={{ position: "relative", padding: "6px 16px 14px" }}>
          <div className="rs-sheet-grab"/>
          <div style={{ display: "flex", alignItems: "center", gap: 12, marginTop: 4 }}>
            <Avatar name={r.driver} size={48}/>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 15, fontWeight: 800 }}>{r.driver}</div>
              <div className="tab" style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>{r.car} · {r.plate}</div>
            </div>
            <button style={{ width: 46, height: 46, borderRadius: 23, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label="Call driver"><Icon name="phone" size={20}/></button>
            <button style={{ width: 46, height: 46, borderRadius: 23, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0, position: "relative" }} aria-label="Chat with driver">
              <Icon name="chat" size={20}/>
              <span style={{ position: "absolute", top: 8, right: 9, width: 8, height: 8, borderRadius: 4, background: "var(--danger)" }}/>
            </button>
          </div>
          <div style={{ marginTop: 13, padding: 13, borderRadius: 16, background: "var(--bg-soft)", border: "1px solid var(--line)" }}>
            <RouteTimeline compact stops={[
              { kind: "pickup", place: r.from, time: r.depart, note: `Boarded · card charged at ${r.chargedAt}` },
              { kind: "drop", place: r.to, time: r.arrive, note: `${FARE_POLICY.currency} ${money(r.price)} · Visa ···4429` },
            ]}/>
          </div>
          <div style={{ marginTop: 10, display: "flex", alignItems: "center", gap: 8 }}>
            <Icon name="pin" size={14} color="var(--ink-3)"/>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", flex: 1, lineHeight: 1.4 }}>
              Need to get out sooner? {earlyDropLeft()} of {EARLY_DROP.allowance} fare-adjusted early drop-offs left this month.
            </div>
          </div>
          <div style={{ display: "flex", gap: 9, marginTop: 12 }}>
            <button className="rs-btn soft" style={{ flex: 1, height: 48 }}><Icon name="share" size={17}/> Share trip</button>
            <button className="rs-btn soft" style={{ flex: 1, height: 48 }}><Icon name="arrow" size={17}/> Get off early</button>
            <button className="rs-btn" style={{ width: 56, height: 48, background: "var(--danger-soft)", flexShrink: 0 }} aria-label="Emergency SOS">
              <Icon name="sos" size={20} color="var(--status-rejected-ink)"/>
            </button>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ P16 · GET OFF EARLY ═══════════
// Three states of one request. The fare is adjusted twice a calendar month;
// beyond that you may still get off, the fare simply stands. Either way the
// driver has to stop and confirm the drop before anything is settled.
function PxExitEarlyScreen({ state = "request" }) {
  const r = MY_TRIP;
  const short = (r.dist - r.actualDist).toFixed(1);
  const nth = EARLY_DROP.used + 1;
  const num = { request: "P16", noadjust: "P16b", waiting: "P16c" }[state];

  const body = {
    request: (
      <>
        <div className="rs-display" style={{ fontSize: 24, marginTop: 10, lineHeight: 1.2 }}>Get off at {r.droppedAt}?</div>
        <div style={{ fontSize: 13, color: "var(--ink-3)", marginTop: 9, lineHeight: 1.6 }}>
          {r.driver.split(" ")[0]} gets a request to pull over. When he confirms you're out, the fare is recalculated on the {r.actualDist} km you actually travelled.
        </div>
        <div style={{ marginTop: 15 }}>
          <FareBreakdown
            currency={FARE_POLICY.currency}
            compact
            totalLabel="New total"
            lines={[
              { label: `Originally booked · ${r.dist} km`, sub: `${r.from} → ${r.to}`, value: r.price, always: true },
              { label: `Refund for ${short} km not travelled`, sub: `At ${FARE_POLICY.currency} ${money(FARE_POLICY.ratePerKm)} per km`, value: r.refund, kind: "discount" },
            ]}
            total={r.paid}
            footnote={`Adjustment ${nth} of ${EARLY_DROP.allowance} this month. The refund reaches your Visa in 3–5 working days.`}/>
        </div>
        <div style={{ marginTop: 13, display: "flex", flexDirection: "column", gap: 10 }}>
          <Banner kind="warn" icon="pin" title={`${r.droppedAt} is ${short} km short of ${r.to}`}
            body="Check you can finish the journey from there before confirming."/>
          <Banner kind="info" icon="users" title="Your seat goes back on sale"
            body={`From ${r.droppedAt} the seat is free again, so someone travelling the rest of the route can book it.`}/>
        </div>
        <button className="rs-btn accent full" style={{ marginTop: 15 }}>Ask {r.driver.split(" ")[0]} to stop at {r.droppedAt}</button>
        <button className="rs-btn ghost full" style={{ marginTop: 10 }}>Stay on to {r.to}</button>
      </>
    ),
    noadjust: (
      <>
        <div style={{ display: "flex", alignItems: "center", gap: 10, marginTop: 10 }}>
          <StatusBadge status="pending" label={`${EARLY_DROP.allowance} OF ${EARLY_DROP.allowance} USED IN ${EARLY_DROP.month.toUpperCase()}`}/>
        </div>
        <div className="rs-display" style={{ fontSize: 24, marginTop: 11, lineHeight: 1.2 }}>You can still get off —<br/>the fare stands</div>
        <div style={{ fontSize: 13, color: "var(--ink-3)", marginTop: 9, lineHeight: 1.6 }}>
          ComiGo adjusts the fare for {EARLY_DROP.allowance} early drop-offs a month. This is your third in {EARLY_DROP.month}, so you'll pay the full fare for {r.from} → {r.to}.
        </div>
        <div style={{ marginTop: 15 }}>
          <FareBreakdown
            currency={FARE_POLICY.currency}
            compact
            totalLabel="You still pay"
            lines={[
              { label: `Booked · ${r.dist} km`, sub: `${r.from} → ${r.to}`, value: r.price, always: true },
              { label: "Distance adjustment", sub: `Not applied — 3rd early drop-off in ${EARLY_DROP.month}`, value: 0, always: true },
            ]}
            total={r.price}
            footnote={`Your allowance resets on 1 August. Nothing is refunded for the ${short} km you don't travel.`}/>
        </div>
        <div style={{ marginTop: 13 }}>
          <Banner kind="info" icon="users" title="The seat is still released"
            body={`Someone else can book ${r.droppedAt} onwards even though your fare isn't adjusted.`}/>
        </div>
        <button className="rs-btn accent full" style={{ marginTop: 15 }}>Get off at {r.droppedAt} anyway</button>
        <button className="rs-btn ghost full" style={{ marginTop: 10 }}>Stay on to {r.to}</button>
      </>
    ),
    waiting: (
      <>
        <div style={{ display: "flex", alignItems: "center", gap: 11, marginTop: 10 }}>
          <div style={{ width: 48, height: 48, borderRadius: 17, background: "var(--status-pending-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
            <Icon name="clock" size={23} color="var(--status-pending-ink)"/>
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em", color: "var(--status-pending-ink)" }}>REQUEST SENT</div>
            <div className="rs-display" style={{ fontSize: 22, marginTop: 3, lineHeight: 1.2 }}>{r.driver.split(" ")[0]} is pulling over</div>
          </div>
        </div>
        <div style={{ fontSize: 13, color: "var(--ink-3)", marginTop: 11, lineHeight: 1.6 }}>
          He's looking for a safe place to stop near {r.droppedAt}. Stay seated until the car has stopped.
        </div>
        <div style={{ marginTop: 15, padding: 15, borderRadius: 18, background: "var(--surface)", border: "1px solid var(--line)" }}>
          <div className="rs-section-label" style={{ marginBottom: 12 }}>WHAT HAPPENS NOW</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 13 }}>
            {[["You asked to get off early", "6:28 PM", "done"], [`${r.driver.split(" ")[0]} stops the car`, "Looking for a safe spot", "now"], ["You get off", `At ${r.droppedAt}`, "next"], ["He confirms the drop-off", `Fare recalculated to ${FARE_POLICY.currency} ${money(r.paid)}`, "next"]].map(([t, s, st]) => (
              <div key={t} style={{ display: "flex", gap: 12, alignItems: "flex-start" }}>
                <div style={{ width: 22, height: 22, borderRadius: 11, flexShrink: 0, marginTop: 1,
                  background: st === "done" ? "var(--status-approved)" : st === "now" ? "var(--status-pending)" : "transparent",
                  border: st === "next" ? "2px solid var(--line-2)" : "none",
                  display: "flex", alignItems: "center", justifyContent: "center" }}>
                  {st === "done" && <Icon name="check" size={13} color="var(--on-bright-fill)" strokeWidth={3}/>}
                  {st === "now" && <div style={{ width: 8, height: 8, borderRadius: 4, background: "var(--on-bright-fill)", animation: "pulse 1.8s infinite" }}/>}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 13, fontWeight: st === "next" ? 600 : 700, color: st === "next" ? "var(--ink-3)" : "var(--ink)" }}>{t}</div>
                  <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>{s}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
        <div style={{ marginTop: 13 }}>
          <Banner kind="info" icon="card" title="The adjustment happens on his confirmation"
            body={`Nothing changes on your card until ${r.driver.split(" ")[0]} confirms you're out — that is what fixes the distance you travelled.`}/>
        </div>
        <button className="rs-btn soft full" style={{ marginTop: 15 }}><Icon name="chat" size={17}/> Message {r.driver.split(" ")[0]}</button>
        <button className="rs-btn ghost full" style={{ marginTop: 10 }}>Cancel the request · stay on</button>
      </>
    ),
  }[state];

  return (
    <Phone label={`${num} Get off early · ${state}`}>
      <div style={{ height: "100%", position: "relative", display: "flex", flexDirection: "column" }}>
        <MapBackdrop pickupLabel="Picked up" dropLabel={state === "waiting" ? r.droppedAt : "Nugegoda"}/>
        <div style={{ position: "absolute", inset: 0, background: "rgba(20,10,5,.5)" }}/>
        <div className="rs-sheet" style={{ position: "absolute", left: 0, right: 0, bottom: 0, top: 40, padding: "8px 22px 22px", overflow: "auto" }}>
          <div className="rs-sheet-grab"/>
          {body}
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ P17 · RECEIPT ═══════════
// Three receipts, because the money arrived three different ways: captured from a
// card, handed over in cash (the mirror of D23), or captured and then adjusted
// after ComiGo approved the driver's request (the mirror of D29b).
function PxReceiptScreen({ variant = "card" }) {
  const r = MY_TRIP;
  const cash = variant === "cash";
  const adjusted = variant === "adjusted";
  const a = FARE_ADJUST;
  const paid = adjusted ? r.paid + a.amount : r.paid;
  return (
    <Phone label={`${adjusted ? "P17c" : cash ? "P17b" : "P17"} Receipt${adjusted ? " · fare adjusted" : cash ? " · paid in cash" : ""}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "18px 20px 22px", background: "var(--ink-fill)", color: "var(--on-ink-fill)", borderBottomLeftRadius: 28, borderBottomRightRadius: 28, flexShrink: 0 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <button style={{ width: 44, height: 44, borderRadius: 22, background: "rgba(255,255,255,.16)", display: "inline-flex", alignItems: "center", justifyContent: "center", marginLeft: -6 }} aria-label="Close"><Icon name="close" size={19} color="var(--on-ink-fill)"/></button>
            <div style={{ flex: 1 }}/>
            <button style={{ width: 44, height: 44, borderRadius: 22, background: "rgba(255,255,255,.16)", display: "inline-flex", alignItems: "center", justifyContent: "center" }} aria-label="Share receipt"><Icon name="share" size={19} color="var(--on-ink-fill)"/></button>
          </div>
          <div style={{ marginTop: 14 }}>
            <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em", opacity: .68 }}>TRIP COMPLETE · 25 JUL, 6:31 PM</div>
            <div className="rs-display tab" style={{ fontSize: 40, lineHeight: 1.05, marginTop: 6 }}>{FARE_POLICY.currency} {money(paid)}</div>
            <div style={{ fontSize: 12.5, opacity: .82, marginTop: 5 }}>
              {cash
                ? `Paid in cash to ${r.driver.split(" ")[0]} at drop-off · nothing was taken from a card`
                : adjusted
                  ? `Charged at ${r.chargedAt}, adjusted ${a.approvedOn} · ${FARE_POLICY.currency} ${money(a.amount)} added`
                  : `Charged at ${r.chargedAt} · ${FARE_POLICY.currency} ${money(r.refund)} refunded on drop-off`}
            </div>
          </div>
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 13 }} className="rs-scroll">
          <div className="rs-card" style={{ padding: 14, display: "flex", alignItems: "center", gap: 12 }}>
            <Avatar name={r.driver} size={44}/>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 700 }}>{r.driver}</div>
              <div className="tab" style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>{r.car} · {r.plate}</div>
            </div>
            <StatusBadge status="approved" label={`${r.actualDist} KM`}/>
          </div>
          <div className="rs-card" style={{ padding: 15 }}>
            <RouteTimeline compact stops={[
              { kind: "pickup", place: r.from, time: r.depart },
              { kind: "drop", place: r.droppedAt, time: "6:31 PM", note: `Early drop-off · confirmed by ${r.driver.split(" ")[0]}` },
            ]}/>
          </div>
          <FareBreakdown
            currency={FARE_POLICY.currency}
            totalLabel="Paid"
            lines={[
              { label: `Booked distance · ${r.dist} km`, sub: `At ${FARE_POLICY.currency} ${money(r.ratePerKm)} per km — ${r.driver.split(" ")[0]}'s rate`, value: r.gross },
              { label: FARE_POLICY.discountLabel, sub: `${r.match}% overlap`, value: r.discount, kind: "discount" },
              { label: "Early drop-off refund", sub: `${(r.dist - r.actualDist).toFixed(1)} km not travelled · adjustment ${EARLY_DROP.used + 1} of ${EARLY_DROP.allowance} in ${EARLY_DROP.month}`, value: r.refund, kind: "discount" },
              ...(adjusted ? [{ label: "Route adjustment", sub: `${a.reason} · ${a.extraKm} km further than booked`, value: a.amount, kind: "adjust", always: true }] : []),
            ]}
            total={paid}
            footnote={cash
              ? `Fare includes a ${FARE_POLICY.commissionPct}% ComiGo fee, which ${r.driver.split(" ")[0]} settles with us — you owed him the fare and nothing else. Booking ${r.bookingRef} · receipt emailed to nimali.p@comigo.lk`
              : adjusted
                ? `${a.requestedBy.split(" ")[0]} asked for the adjustment and ${a.decidedBy} approved it on ${a.approvedOn}; drivers cannot change a fare themselves. Dispute it within ${a.disputeHours} hours if the detour wasn't needed. Booking ${r.bookingRef}`
                : `Fare includes a ${FARE_POLICY.commissionPct}% ComiGo fee, charged at ${r.chargedAt} when the trip started. Booking ${r.bookingRef} · receipt emailed to nimali.p@comigo.lk`}/>
          {adjusted && (
            <Banner kind="info" icon="alert" title={`Why the fare went up by ${FARE_POLICY.currency} ${money(a.amount)}`}
              body={`${a.reason} added ${a.extraKm} km to the route. ${a.requestedBy.split(" ")[0]} submitted evidence, ${a.decidedBy} reviewed it, and only then was your card charged the difference.`} action="Dispute"/>
          )}
          {cash && (
            <Banner kind="info" icon="card" title="A cash trip still has a receipt"
              body={`Nothing was authorised or captured on your Visa. If a fee is ever owed on a cash trip it is carried to your next booking rather than taken from a card.`} action="Outstanding amounts"/>
          )}
          <div style={{ display: "flex", gap: 9 }}>
            <button className="rs-btn soft" style={{ flex: 1, height: 48 }}><Icon name="receipt" size={17}/> Get PDF</button>
            <button className="rs-btn soft" style={{ flex: 1, height: 48 }}><Icon name="alert" size={17}/> Report an issue</button>
          </div>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">Rate {r.driver.split(" ")[0]}</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ P18 · RATE DRIVER ═══════════
function PxRateDriverScreen() {
  const r = MY_TRIP;
  return (
    <Phone label="P18 Rate driver">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Rate your trip" right={<button style={{ minHeight: 44, minWidth: 44, padding: "0 8px", fontSize: 13, fontWeight: 700, color: "var(--ink-3)" }}>Skip</button>}/>
        <div style={{ flex: 1, overflow: "auto", padding: "24px 24px 16px", display: "flex", flexDirection: "column", alignItems: "center", gap: 18 }} className="rs-scroll">
          <Avatar name={r.driver} size={76}/>
          <div style={{ textAlign: "center" }}>
            <div className="rs-display" style={{ fontSize: 25, lineHeight: 1.2 }}>How was the ride<br/>with {r.driver.split(" ")[0]}?</div>
            <div style={{ fontSize: 12.5, color: "var(--ink-3)", marginTop: 8 }}>{r.from.split(" ")[0]} → {r.droppedAt} · 25 Jul</div>
          </div>
          <div style={{ display: "flex", gap: 8 }}>
            {[1, 2, 3, 4, 5].map(i => (
              <button key={i} style={{ width: 50, height: 50, borderRadius: 25, display: "inline-flex", alignItems: "center", justifyContent: "center" }} aria-label={`${i} star${i > 1 ? "s" : ""}`}>
                <Icon name="star" size={34} color={i <= 4 ? "var(--status-pending-ink)" : "var(--line-2)"} strokeWidth={i <= 4 ? 2 : 1.6}/>
              </button>
            ))}
          </div>
          <div style={{ alignSelf: "stretch" }}>
            <div className="rs-section-label" style={{ marginBottom: 9, textAlign: "center" }}>WHAT WENT WELL?</div>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap", justifyContent: "center" }}>
              {["Punctual", "Safe driving", "Friendly", "Clean car", "Good route"].map((c, i) => (
                <button key={c} className="rs-tap"><span className={`rs-chip${i < 2 ? " accent" : ""}`}>{c}</span></button>
              ))}
            </div>
          </div>
          <div style={{ alignSelf: "stretch", minHeight: 96, padding: 14, borderRadius: 16, background: "var(--surface)", border: "1.5px solid var(--line)", fontSize: 13.5, color: "var(--ink-3)" }}>
            Anything you'd like {r.driver.split(" ")[0]} to know? (optional)
          </div>
          <div style={{ alignSelf: "stretch" }}>
            <Banner kind="info" icon="users" title={`${r.driver.split(" ")[0]} is rating you too`}
              body={`Both go live together, so neither of you can answer the other's score. Your comment appears on his profile signed "${ME.firstName} ${ME.name.split(" ")[1][0]}." — and he can reply to it once.`}/>
          </div>
        </div>
        <div style={{ padding: "12px 24px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">Send rating</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ P19 · SAVED PLACES ═══════════
function PxSavedPlacesScreen() {
  const places = [
    { icon: "home", label: "Home", addr: "42 Kirula Road, Nugegoda", note: "Used 38 times" },
    { icon: "briefcase", label: "Work", addr: "Ceylinco House, Colombo Fort", note: "Used 31 times" },
    { icon: "star", label: "Amma's", addr: "Station Road, Rajagiriya", note: "Used 6 times" },
    { icon: "pin", label: "Gym", addr: "Duplication Road, Bambalapitiya", note: "Used 4 times" },
  ];
  return (
    <Phone label="P19 Saved places">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Saved places"/>
        <div style={{ flex: 1, overflow: "auto", padding: "0 16px 16px" }} className="rs-scroll">
          <div style={{ padding: "12px 0" }}>
            <Banner kind="info" icon="clock" title="Saved places make searching faster" body="Two taps from home to results, instead of typing your address every morning."/>
          </div>
          <div className="rs-card" style={{ padding: "2px 12px" }}>
            {places.map((p, i) => (
              <div key={p.label}>
                {i > 0 && <div className="rs-divider"/>}
                <MenuRow icon={p.icon} label={p.label} sub={`${p.addr} · ${p.note}`}
                  right={<button style={{ width: 44, height: 44, borderRadius: 22, display: "inline-flex", alignItems: "center", justifyContent: "center" }} aria-label={`Options for ${p.label}`}><Icon name="ellipsis" size={18} color="var(--ink-3)"/></button>}
                  chev={false}/>
              </div>
            ))}
          </div>
          <button className="rs-btn ghost full" style={{ marginTop: 12, height: 48 }}><Icon name="plus" size={17}/> Add a place</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ P20 · SHARE TRIP ═══════════
function PxShareTripScreen() {
  return (
    <Phone label="P20 Share trip">
      <div style={{ height: "100%", position: "relative", display: "flex", flexDirection: "column" }}>
        <MapBackdrop pickupLabel="Picked up" dropLabel="Nugegoda"/>
        <div style={{ position: "absolute", inset: 0, background: "rgba(20,10,5,.5)" }}/>
        <div className="rs-sheet" style={{ position: "absolute", left: 0, right: 0, bottom: 0, padding: "8px 22px 22px" }}>
          <div className="rs-sheet-grab"/>
          <div className="rs-display" style={{ fontSize: 24, marginTop: 10, lineHeight: 1.2 }}>Share this trip</div>
          <div style={{ fontSize: 13, color: "var(--ink-3)", marginTop: 9, lineHeight: 1.55 }}>
            Anyone with the link sees your live location, the driver's name and the vehicle number. No ComiGo account needed.
          </div>
          <div style={{ marginTop: 15, padding: 13, borderRadius: 16, background: "var(--bg-soft)", border: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 10 }}>
            <Icon name="share" size={17} color="var(--ink-3)"/>
            <div className="tab" style={{ flex: 1, fontSize: 12.5, fontWeight: 600, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>comigo.lk/t/8042-kx9f</div>
            <button className="rs-btn accent" style={{ height: 44, padding: "0 16px", fontSize: 12.5, flexShrink: 0 }}>Copy</button>
          </div>
          <div style={{ marginTop: 16 }}>
            <div className="rs-section-label" style={{ marginBottom: 9 }}>SHARED WITH · 2 PEOPLE</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 9 }}>
              {[["Amma", "+94 71 220 4418", "Opened 6:18 PM"], ["Chathura", "+94 76 881 0092", "Not opened yet"]].map(([n, p, s]) => (
                <div key={n} style={{ padding: 12, borderRadius: 15, background: "var(--surface)", border: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 11 }}>
                  <Avatar name={n} size={38}/>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13, fontWeight: 700 }}>{n}</div>
                    <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 1 }}>{s}</div>
                  </div>
                  <button style={{ minHeight: 44, minWidth: 44, padding: "0 8px", fontSize: 12, fontWeight: 800, color: "var(--status-rejected-ink)", flexShrink: 0 }}>Revoke</button>
                </div>
              ))}
            </div>
          </div>
          <button className="rs-btn accent full" style={{ marginTop: 15 }}>Share with someone else</button>
          <div style={{ textAlign: "center", fontSize: 11, color: "var(--ink-3)", marginTop: 10, lineHeight: 1.5 }}>
            The link stops working by itself when the trip ends.
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ P21 · PUBLIC RECIPIENT VIEW (no app, not signed in) ═══════════
function PxPublicTripScreen() {
  return (
    <Phone label="P21 Public trip link" statusDark statusBg="#1b1410">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        {/* mobile browser chrome, to make clear this is a web page not the app */}
        <div style={{ padding: "8px 12px 10px", background: "var(--ink-fill)", flexShrink: 0 }}>
          <div style={{ height: 36, borderRadius: 18, background: "rgba(255,255,255,.14)", display: "flex", alignItems: "center", gap: 8, padding: "0 12px" }}>
            <Icon name="lock" size={13} color="var(--on-ink-fill)"/>
            <div className="tab" style={{ fontSize: 11.5, color: "var(--on-ink-fill)", opacity: .85 }}>comigo.lk/t/8042-kx9f</div>
          </div>
        </div>
        <div style={{ flex: 1, overflow: "auto", display: "flex", flexDirection: "column" }} className="rs-scroll">
          <div style={{ height: 210, position: "relative", flexShrink: 0 }}>
            <MapBackdrop pickupLabel="Picked up" dropLabel="Nugegoda"/>
          </div>
          <div style={{ padding: "16px 18px 18px", display: "flex", flexDirection: "column", gap: 14 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <Lockup Mark={MarkOverlap} size={24}/>
              <div style={{ flex: 1 }}/>
              <div className="rs-chip success" style={{ height: 26 }}><Icon name="check" size={12}/> Live</div>
            </div>
            <div>
              <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em", color: "var(--ink-3)" }}>NIMALI IS ON A TRIP</div>
              <div className="rs-display" style={{ fontSize: 26, marginTop: 6, lineHeight: 1.2 }}>Arriving Nugegoda<br/>around 6:38 PM</div>
              <div style={{ fontSize: 12.5, color: "var(--ink-3)", marginTop: 8 }}>2.1 km to go · updated 4 seconds ago</div>
            </div>
            <div className="rs-card" style={{ padding: 14 }}>
              <div className="rs-section-label" style={{ marginBottom: 11 }}>THE DRIVER</div>
              <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <Avatar name="Kasun D" size={44}/>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 14, fontWeight: 700 }}>Kasun D · 4.8★</div>
                  <div className="tab" style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>Suzuki Alto · Blue · WP KB-8842</div>
                </div>
                <StatusBadge status="approved" label="VERIFIED"/>
              </div>
            </div>
            <div className="rs-card" style={{ padding: 15 }}>
              <RouteTimeline compact stops={[
                { kind: "pickup", place: "Rajagiriya junction", time: "6:15 PM", note: "Boarded" },
                { kind: "drop", place: "Nugegoda", time: "6:38 PM", note: "Expected arrival" },
              ]}/>
            </div>
            <a className="rs-btn full" style={{ background: "var(--danger)", color: "var(--on-bright-fill)", textDecoration: "none" }} href="tel:119">
              <Icon name="phone" size={17} color="var(--on-bright-fill)"/> Call 119 if something's wrong
            </a>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", lineHeight: 1.55, textAlign: "center" }}>
              Nimali shared this from ComiGo. It stops updating when the trip ends, and she can revoke it any time.
            </div>
            <div style={{ padding: 14, borderRadius: 16, background: "var(--mode-drive-soft)", border: "1px solid var(--line)", textAlign: "center" }}>
              <div style={{ fontSize: 13, fontWeight: 800, color: "var(--mode-drive-ink)" }}>Share rides in Colombo</div>
              <div style={{ fontSize: 11.5, color: "var(--ink-2)", marginTop: 4, lineHeight: 1.45 }}>ComiGo matches you with drivers already going your way.</div>
              <button className="rs-btn full" style={{ marginTop: 11, height: 44, background: "var(--mode-drive)", color: "var(--on-bright-fill)", fontSize: 13 }}>Get the app</button>
            </div>
          </div>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, {
  PxInTripScreen, PxExitEarlyScreen, PxReceiptScreen, PxRateDriverScreen,
  PxSavedPlacesScreen, PxShareTripScreen, PxPublicTripScreen,
});
