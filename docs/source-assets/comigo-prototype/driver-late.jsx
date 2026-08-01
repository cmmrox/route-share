// driver-late.jsx — D41/D16e: the two driver surfaces that had no counterpart on
// a passenger flow. D41 is his side of P34/P35 (he was the one who was late), and
// D16e is his side of P14 (the seat sold while he was deciding).

// ═══════════ D41 · YOU WERE LATE, SHE CANCELLED ═══════════
// The mirror of P34. She waited past the grace window and cancelled free of
// charge; he carries the same penalty as a late cancellation and half of it
// reaches her. Every consequence is stated in money before he taps away, because
// finding a deduction in the ledger later is what generates support tickets.
function DvLatePenaltyScreen() {
  const d = DRIVER_LATE;
  const fee = driverLateFee();
  const first = d.passenger.split(" ")[0];
  const { they, them, their } = d;
  return (
    <Phone label="D41 You were late · rider cancelled free">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "20px 20px 22px", background: "var(--danger)", color: "var(--on-bright-fill)", flexShrink: 0 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <Icon name="clock" size={20} color="var(--on-bright-fill)" strokeWidth={2.4}/>
            <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em" }}>{d.lateByMin} MINUTES PAST {their.toUpperCase()} PICKUP TIME</div>
          </div>
          <div className="rs-display" style={{ fontSize: 26, lineHeight: 1.15, marginTop: 10 }}>{first} cancelled, free of charge</div>
          <div style={{ fontSize: 13, marginTop: 6, opacity: .92, lineHeight: 1.5 }}>
            {d.pickup} → {d.to} · {they} was due at {d.promisedAt} and cancelled at {d.cancelledAt}
          </div>
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          <Banner kind="bad" icon="alert" title={`After ${d.graceMin} minutes a rider can always cancel free`}
            body={`The clock runs from the pickup time you promised ${them}, not from the trip's departure time — they are different moments. Once it passes, the cancellation is ${d.theirs} to make and costs ${them} nothing.`}/>

          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 11 }}>WHAT THIS COSTS YOU</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              <RuleRow icon="cash" tint="var(--status-rejected-ink)" title={`${FARE_POLICY.currency} ${money(fee)} penalty · ${POLICY.driverLatePenaltyPct}% of ${their} seat`}
                body={`The same rate as cancelling late. It comes out of what you earn on your next completed trip — nothing is billed to you and it is never a debt.`}/>
              <RuleRow icon="users" tint="var(--status-rejected-ink)" title={`${FARE_POLICY.currency} ${money(victimShare(fee))} of it goes to ${first}`}
                body={`As ride credit, for the morning ${they} lost. ${FARE_POLICY.currency} ${money(platformShare(fee))} goes to ComiGo. Every penalty splits ${POLICY.penaltyVictimPct}/${POLICY.penaltyPlatformPct} — it is the identical rule that pays you when a rider stands you up.`}/>
              <RuleRow icon="card" title={`${first}'s fare was never collected`}
                body={`Cards are captured when a trip starts. ${they[0].toUpperCase() + they.slice(1)} was not charged, so there is nothing to refund and nothing of ${d.theirs} to argue about.`}/>
              <RuleRow icon="star" tint="var(--status-rejected-ink)" title="It counts as a late cancellation"
                body={`This is number ${DRIVER_RELIABILITY.lateCancellations + 1} in 30 days. Three pauses publishing for a week. Missed starts are counted separately.`}/>
            </div>
            <div style={{ height: 1, background: "var(--line)", margin: "13px 0" }}/>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 12.5, fontWeight: 800 }}>Deducted from your next trip</div>
                <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>Shows in your ledger as a late penalty</div>
              </div>
              <div className="rs-display tab" style={{ fontSize: 23, fontWeight: 600, color: "var(--status-rejected-ink)" }}>−{FARE_POLICY.currency} {money(fee)}</div>
            </div>
          </div>

          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 11 }}>THE TRIP IS STILL RUNNING</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              <RuleRow icon="check" tint="var(--status-approved-ink)" title={`${d.stillOnBoard.split(" ")[0]} is on board and unaffected`}
                body="One cancellation does not end the trip. Carry on to the next pickup — the rest of your earnings are intact."/>
              <RuleRow icon="users" title={`${first}'s seat is back on sale`}
                body={`Riders searching the rest of your route can book it while you drive, so the seat can still earn.`}/>
            </div>
          </div>

          <Banner kind="info" icon="help" title="Was the delay not your fault?"
            body={`Tell support within 48 hours — "${d.reason}" with your GPS trail behind it is exactly the kind of case that gets a penalty reversed in full.`} action="Dispute"/>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)", display: "flex", flexDirection: "column", gap: 9 }}>
          <button className="rs-btn full" style={{ background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Continue the trip</button>
          <button className="rs-btn soft full">My reliability</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D16e · THE SEAT WENT WHILE HE WAS DECIDING ═══════════
// The mirror of P14. Seat inventory is transactional and his approval window is
// finite, so a request can simply expire. The screen explains it without blaming
// him and points at the setting that prevents it.
function DvRequestLapsedScreen() {
  const q = LAPSED_REQUEST;
  const first = q.passenger.split(" ")[0];
  const { they, their } = q;
  return (
    <Phone label="D16e Request lapsed · seat already sold">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Booking request" sub={`${q.from} → ${q.to}`}/>
        <div style={{ flex: 1, overflow: "auto", padding: "20px 16px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          <div style={{ display: "flex", alignItems: "center", gap: 13 }}>
            <div style={{ width: 56, height: 56, borderRadius: 19, background: "var(--bg-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
              <Icon name="clock" size={26} color="var(--ink-3)" strokeWidth={2.3}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em", color: "var(--ink-3)" }}>EXPIRED AFTER {q.windowMin} MINUTES</div>
              <div className="rs-display" style={{ fontSize: 24, marginTop: 3, lineHeight: 1.18 }}>This request has gone</div>
            </div>
          </div>
          <div style={{ fontSize: 13.5, color: "var(--ink-3)", lineHeight: 1.6, textWrap: "pretty" }}>
            {first} asked for a seat and waited {q.waitedMin} minutes. While the request sat here, {q.takenBy.split(" ")[0]} booked the last seat outright — so there is nothing left to approve.
          </div>

          <div className="rs-card" style={{ padding: 14, display: "flex", alignItems: "center", gap: 12, opacity: .6 }}>
            <Avatar name={q.passenger} size={44}/>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 700 }}>{q.passenger}</div>
              <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>{q.from} → {q.to} · {FARE_POLICY.currency} {money(q.net)} to you</div>
            </div>
            <StatusBadge status="none" label="LAPSED"/>
          </div>

          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 11 }}>WHERE THINGS STAND</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              <RuleRow icon="card" title="Nobody was charged either way"
                body={`${their[0].toUpperCase() + their.slice(1)} card was authorised, never captured, and the authorisation is released. ${they[0].toUpperCase() + they.slice(1)} has already been told and offered the next drivers on ${their} route.`}/>
              <RuleRow icon="users" title={`${q.seatsLeft} seats left on this trip`}
                body="The trip is full. Nothing about this changes what you earn from the riders who did book."/>
              <RuleRow icon="star" tint="var(--status-approved-ink)" title="Your acceptance rate is untouched"
                body="A request that expires because the seat sold is not a decline. It does not count against you."/>
            </div>
          </div>

          <Banner kind="info" icon="settings" title="Tired of racing your own trips?"
            body={`Turn off "I approve each request" and seats sell instantly to anyone who qualifies — no ${q.windowMin}-minute window, no expired requests. You keep the verified-riders-only filter either way.`} action="Preferences"/>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)", display: "flex", flexDirection: "column", gap: 9 }}>
          <button className="rs-btn full" style={{ background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Back to the trip</button>
          <button className="rs-btn soft full">Driving preferences</button>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, { DvLatePenaltyScreen, DvRequestLapsedScreen });
