// passenger-penalties.jsx — P25…P27: what a passenger owes and why.
// Every penalty is SPLIT 50/50: half to the person who was let down, half to
// ComiGo. So these screens must name the driver as a recipient, not just us.
// A card passenger's penalty is netted on the spot; only a CASH passenger
// accumulates an amount that rides along to the next booking.

// ═══════════ P25 · OUTSTANDING AMOUNTS ═══════════
function PxDuesScreen({ settled = false }) {
  const total = duesTotal();
  return (
    <Phone label={`P25 Outstanding amounts · ${settled ? "clear" : "owed"}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Outstanding amounts" sub="Fees carried from earlier trips"/>
        <div style={{ flex: 1, overflow: "auto", padding: "16px 16px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          {settled ? (
            <EmptyState icon="check" kind="empty" title="Nothing outstanding"
              body="You've no unpaid fees. Anything owed in future appears here and on your next checkout before you pay."/>
          ) : (
            <>
              <div style={{ padding: 18, borderRadius: 20, background: "var(--status-pending-soft)", border: "1px solid var(--status-pending)" }}>
                <div style={{ display: "flex", alignItems: "center", gap: 9 }}>
                  <Icon name="alert" size={18} color="var(--status-pending-ink)" strokeWidth={2.4}/>
                  <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", color: "var(--status-pending-ink)" }}>ADDED TO YOUR NEXT BOOKING</div>
                </div>
                <div className="rs-display tab" style={{ fontSize: 38, lineHeight: 1.05, marginTop: 10 }}>{FARE_POLICY.currency} {money(total)}</div>
                <div style={{ fontSize: 12.5, color: "var(--ink-2)", marginTop: 6, lineHeight: 1.5 }}>
                  You paid that trip in cash, so there was no card to take the fee from. It's added to your next booking's total and has to be paid before you ride.
                </div>
              </div>

              <div className="rs-card" style={{ padding: "2px 14px" }}>
                {PAX_DUES.items.map((x, i) => (
                  <div key={x.when}>
                    {i > 0 && <div className="rs-divider"/>}
                    <div style={{ padding: "14px 0", display: "flex", alignItems: "center", gap: 11 }}>
                      <div style={{ width: 36, height: 36, borderRadius: 12, background: "var(--status-rejected-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                        <Icon name="clock" size={17} color="var(--status-rejected-ink)"/>
                      </div>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontSize: 13.5, fontWeight: 700 }}>{x.what} · {x.why}</div>
                        <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>{x.trip}</div>
                        <div className="tab" style={{ fontSize: 10.5, color: "var(--ink-3)", marginTop: 3 }}>{x.when} · paid in cash</div>
                      </div>
                      <div className="tab" style={{ fontSize: 14, fontWeight: 800, flexShrink: 0 }}>{FARE_POLICY.currency} {money(x.amount)}</div>
                    </div>
                  </div>
                ))}
              </div>

              <div className="rs-card" style={{ padding: 15 }}>
                <div className="rs-section-label" style={{ marginBottom: 12 }}>WHY THIS EXISTS</div>
                <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
                  <RuleRow icon="users" title="A held seat can't be resold in time"
                    body={`When nobody boards, the driver leaves with an empty seat he'd already turned others away for.`}/>
                  <RuleRow icon="card" title="Card passengers never see this screen"
                    body={`Their fee comes straight off the refund. Cash has no card to take it from, so it waits.`}/>
                  <RuleRow icon="receipt" title={`Half goes to the driver you left waiting`}
                    body={`He held a seat and waited ${POLICY.pickupWaitMin} minutes for someone who never came. ${POLICY.penaltyVictimPct}% of the fee is his; the rest is ComiGo's.`}/>
                </div>
              </div>

              <Banner kind="info" icon="help" title="Think this is wrong?"
                body="Tell support before you pay. Fees are reversed if the driver never reached the pickup point." action="Dispute"/>
            </>
          )}
        </div>
        {!settled && (
          <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
            <button className="rs-btn accent full">Pay {FARE_POLICY.currency} {money(total)} now</button>
            <div style={{ textAlign: "center", fontSize: 11, color: "var(--ink-3)", marginTop: 9, lineHeight: 1.5 }}>
              Or leave it — it's added to your next booking automatically.
            </div>
          </div>
        )}
      </div>
    </Phone>
  );
}

// ═══════════ P26 / P27 · PENALTY OUTCOMES ═══════════
// Two ways a passenger loses money on a trip that already started. Both show
// the arithmetic, because "a small penalty" with no number is what generates
// support tickets.
function PxPenaltyScreen({ kind = "cancel" }) {
  const r = MY_TRIP;
  const cancel = kind === "cancel";
  const pct = cancel ? POLICY.paxCancelAfterStartPct : POLICY.noShowPenaltyPct;
  const fee = cancel ? paxCancelPenalty(r.price) : noShowPenalty(r.price);
  const back = r.price - fee;
  const cfg = cancel ? {
    num: "P26", tag: "cancelled after start",
    kicker: "BOOKING CANCELLED", title: "You cancelled mid-trip",
    lead: `${r.driver.split(" ")[0]} had already started, so ${pct}% of the fare is kept. The rest is on its way back to your Visa.`,
  } : {
    num: "P27", tag: "no-show",
    kicker: `NOT AT ${r.from.toUpperCase()}`, title: "You missed this ride",
    lead: `${r.driver.split(" ")[0]} waited ${POLICY.pickupWaitMin + POLICY.pickupWaitExtendMin} minutes at ${r.from} and had to go. ${pct}% of the fare is kept; the rest is refunded.`,
  };
  return (
    <Phone label={`${cfg.num} Penalty · ${cfg.tag}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Your booking" sub={`${r.bookingRef} · ${r.driver}`}/>
        <div style={{ flex: 1, overflow: "auto", padding: "20px 16px 16px" }} className="rs-scroll">
          <div style={{ display: "flex", alignItems: "center", gap: 13 }}>
            <div style={{ width: 56, height: 56, borderRadius: 19, background: "var(--status-pending-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
              <Icon name={cancel ? "close" : "clock"} size={26} color="var(--status-pending-ink)" strokeWidth={2.3}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em", color: "var(--status-pending-ink)" }}>{cfg.kicker}</div>
              <div className="rs-display" style={{ fontSize: 25, marginTop: 3, lineHeight: 1.15 }}>{cfg.title}</div>
            </div>
          </div>
          <div style={{ fontSize: 13.5, color: "var(--ink-3)", marginTop: 12, lineHeight: 1.6, textWrap: "pretty" }}>{cfg.lead}</div>

          <div style={{ marginTop: 16 }}>
            <FareBreakdown
              currency={FARE_POLICY.currency}
              totalLabel="Back to your Visa"
              lines={[
                { label: `Charged when the trip started`, sub: `${r.from} → ${r.to} · ${r.chargedAt}`, value: r.price, always: true },
                { label: `${cancel ? "Late cancellation" : "No-show"} fee · ${pct}%`, sub: `${FARE_POLICY.currency} ${money(victimShare(fee))} to ${r.driver.split(" ")[0]}, ${FARE_POLICY.currency} ${money(platformShare(fee))} to ComiGo`, value: fee, kind: "fee", always: true },
              ]}
              total={back}
              footnote={`Refunds reach your card in 3–5 working days. Every penalty is split ${POLICY.penaltyVictimPct}/${POLICY.penaltyPlatformPct}: half compensates the person who was let down — here, ${r.driver.split(" ")[0]}, who held a seat nobody used — and half goes to ComiGo. It works the same way when a driver lets you down.`}/>
          </div>

          <div className="rs-card" style={{ padding: 15, marginTop: 12 }}>
            <div className="rs-section-label" style={{ marginBottom: 12 }}>WHAT THIS DOES TO YOUR ACCOUNT</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              <RuleRow icon="star" title="Your rating isn't touched by this"
                body={`${r.driver.split(" ")[0]} rates the trips you actually take. A fee is not a review.`}/>
              <RuleRow icon="users" title="Your seat was released immediately"
                body="It went back on sale for the rest of the route, so someone else could use it."/>
              {!cancel && (
                <RuleRow icon="clock" tint="var(--status-pending-ink)" title={`Two no-shows in a month and we ask you to prepay`}
                  body="This is your first. It clears at the end of the month."/>
              )}
            </div>
          </div>

          <div style={{ marginTop: 12 }}>
            <Banner kind="info" icon="help" title={cancel ? "Was the driver not where he said?" : "Were you there and he wasn't?"}
              body="Tell support within 48 hours. If his GPS never reached the pickup point, the fee is reversed in full." action="Dispute"/>
          </div>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">Find another ride</button>
          <button className="rs-btn ghost full" style={{ marginTop: 10 }}>Contact support</button>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, { PxDuesScreen, PxPenaltyScreen });
