// passenger-status.jsx — P38/P38b/P39: the two passenger screens that mirror
// driver surfaces which had no counterpart. P38 is the rider's side of D19 (the
// wait at a pickup, before it becomes a no-show), P39 is the rider's side of D28
// (her own record, judged by the same rules that judge him).

// ═══════════ P38 · YOUR DRIVER IS WAITING ═══════════
// D19 counts the same clock from the driver's seat. This is the only screen that
// can stop a no-show, so it does one job: say how long is left, and what it
// costs if it runs out. The fee is never a surprise on P27.
function PxDriverWaitingScreen({ extended = false }) {
  const r = MY_TRIP;
  const first = r.driver.split(" ")[0];
  const total = POLICY.pickupWaitMin + (extended ? POLICY.pickupWaitExtendMin : 0);
  const fee = noShowPenalty(r.price);
  return (
    <Phone label={`${extended ? "P38b" : "P38"} Driver waiting · ${extended ? "extended once" : `${POLICY.pickupWaitMin} min`}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "20px 20px 22px", background: extended ? "var(--danger)" : "var(--status-pending)", color: "var(--on-bright-fill)", flexShrink: 0 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <Icon name="clock" size={20} color="var(--on-bright-fill)" strokeWidth={2.4}/>
            <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em" }}>
              {extended ? `HE EXTENDED THE WAIT BY ${POLICY.pickupWaitExtendMin} MINUTES` : `${first.toUpperCase()} IS AT ${r.from.toUpperCase()}`}
            </div>
          </div>
          <div className="rs-display tab" style={{ fontSize: 46, lineHeight: 1.05, marginTop: 10 }}>{extended ? "1:48" : "3:12"}</div>
          <div style={{ fontSize: 13, marginTop: 4, opacity: .92, lineHeight: 1.5 }}>
            {extended
              ? `of the extra ${POLICY.pickupWaitExtendMin} minutes. There is no second extension — when this runs out he releases your seat and drives on.`
              : `before he can release your seat. He waits ${POLICY.pickupWaitMin} minutes as standard and can add ${POLICY.pickupWaitExtendMin} more, once.`}
          </div>
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          <div className="rs-card" style={{ padding: 14, display: "flex", alignItems: "center", gap: 12 }}>
            <Avatar name={r.driver} size={44}/>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 700 }}>{r.driver}</div>
              <div className="tab" style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>{r.car} · {r.plate}</div>
            </div>
            <button data-row="chat with driver" style={{ width: 44, height: 44, borderRadius: 22, background: "var(--accent-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label="Chat with driver"><Icon name="chat" size={18} color="var(--accent-ink)"/></button>
          </div>

          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 11 }}>IF THE CLOCK RUNS OUT</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              <RuleRow icon="card" tint="var(--status-rejected-ink)" title={`You are charged ${POLICY.noShowPenaltyPct}% · ${FARE_POLICY.currency} ${money(fee)}`}
                body={`The rest of the ${FARE_POLICY.currency} ${money(r.price)} comes back to your Visa. The fee is split: ${FARE_POLICY.currency} ${money(victimShare(fee))} to ${first} for the seat nobody used, ${FARE_POLICY.currency} ${money(platformShare(fee))} to ComiGo.`}/>
              <RuleRow icon="users" title="Your seat goes back on sale"
                body={`Riders searching the rest of his route can book it while he drives, so the seat is not wasted — but it is no longer yours.`}/>
              <RuleRow icon="star" title="Your rating isn't touched"
                body="A fee is not a review. What it does affect is your no-show count."/>
              <RuleRow icon="clock" tint="var(--status-pending-ink)" title={`${PAX_RELIABILITY.prepayThreshold} no-shows in a month and we ask you to prepay`}
                body={`You have ${PAX_RELIABILITY.noShows} this ${PAX_RELIABILITY.monthLabel}. The count clears at the end of the month.`}/>
            </div>
          </div>

          <Banner kind="info" icon="pin" title={extended ? "Tell him where you are, now" : "Running two minutes behind?"}
            body={extended
              ? `He has already given you everything he can. A message in the chat is the only thing that will keep him there — he cannot extend again.`
              : `Say so in the chat. Most drivers will wait a little longer for someone who tells them, and it costs you nothing to ask.`}/>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)", display: "flex", flexDirection: "column", gap: 9 }}>
          <button className="rs-btn accent full">I'm here — where is the car?</button>
          <button className="rs-btn soft full">Message {first}</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ P39 · YOUR RECORD AS A RIDER ═══════════
// The mirror of D28. A passenger is judged on completion, no-shows and being at
// the kerb on time, and she can see all three — the same rules, stated the same
// way, so neither side can claim the other is graded more gently.
function PxRiderRatingScreen() {
  const t = TRUST.passenger;
  const dist = t.stars;
  const total = dist.reduce((a, [, n]) => a + n, 0);
  return (
    <Phone label="P39 Your rating & reliability">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Your rating" sub={`As a rider · since ${t.since}`}/>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          <div className="rs-card" style={{ padding: 16 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
              <div>
                <div className="rs-display tab" style={{ fontSize: 44, lineHeight: 1, color: "var(--accent-ink)" }}>{t.rating}</div>
                <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 5 }}>from {t.ratings} drivers</div>
              </div>
              <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: 5 }}>
                {dist.map(([star, n]) => (
                  <div key={star} style={{ display: "flex", alignItems: "center", gap: 8 }}>
                    <div className="tab" style={{ fontSize: 11, color: "var(--ink-3)", width: 8 }}>{star}</div>
                    <div style={{ flex: 1, height: 6, borderRadius: 3, background: "var(--bg-soft)", overflow: "hidden" }}>
                      <div style={{ width: `${total ? (n / total) * 100 : 0}%`, height: "100%", background: "var(--accent-ink)" }}/>
                    </div>
                    <div className="tab" style={{ fontSize: 11, color: "var(--ink-3)", width: 18, textAlign: "right" }}>{n}</div>
                  </div>
                ))}
              </div>
            </div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 13, lineHeight: 1.5, paddingTop: 12, borderTop: "1px solid var(--line)" }}>
              Drivers rate you after the trip, at the same moment you rate them, and both publish together — neither of you can answer the other's score.
            </div>
          </div>

          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 12 }}>WHAT DRIVERS ARE JUDGED ON, YOU ARE TOO</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
              {[
                ["check", `${PAX_RELIABILITY.completionPct}%`, "Trips completed", `${t.trips} booked, ${Math.round(t.trips * PAX_RELIABILITY.completionPct / 100)} ridden`, "var(--status-approved-ink)"],
                ["clock", `${PAX_RELIABILITY.onTimeAtPickupPct}%`, "At the kerb on time", "Measured from your promised pickup time", "var(--status-approved-ink)"],
                ["users", `${PAX_RELIABILITY.noShows}`, "No-shows", `None this ${PAX_RELIABILITY.monthLabel}. ${PAX_RELIABILITY.prepayThreshold} in a month and we ask you to prepay.`, "var(--ink-2)"],
                ["close", `${PAX_RELIABILITY.lateCancels}`, "Cancelled after the trip started", `Costs ${POLICY.paxCancelAfterStartPct}% of the fare, half of it to the driver`, "var(--ink-2)"],
              ].map(([ic, v, l, sub, c]) => (
                <div key={l} style={{ display: "flex", alignItems: "flex-start", gap: 12 }}>
                  <div style={{ width: 34, height: 34, borderRadius: 12, background: "var(--bg-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                    <Icon name={ic} size={16} color="var(--ink-2)"/>
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13.5, fontWeight: 700 }}>{l}</div>
                    <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.45 }}>{sub}</div>
                  </div>
                  <div className="tab" style={{ fontSize: 17, fontWeight: 800, color: c, flexShrink: 0 }}>{v}</div>
                </div>
              ))}
            </div>
          </div>

          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 4 }}>WHAT DRIVERS WROTE</div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginBottom: 13 }}>Named, both ways, with one reply each — the same rule that applies to what you write about them.</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
              {REVIEWS.asRider.map(rv => (
                <div key={rv.who}>
                  <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    <Avatar name={rv.who} size={34}/>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 13, fontWeight: 700 }}>{rv.who}</div>
                      <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 1 }}>{rv.when}</div>
                    </div>
                    <div style={{ display: "flex", gap: 2 }}>
                      {Array.from({ length: 5 }, (_, i) => <Icon key={i} name="star" size={12} color={i < rv.stars ? "var(--accent-ink)" : "var(--line-2)"}/>)}
                    </div>
                  </div>
                  <div style={{ fontSize: 12.5, color: "var(--ink-2)", marginTop: 8, lineHeight: 1.55, textWrap: "pretty" }}>{rv.body}</div>
                  {rv.tags && (
                    <div style={{ display: "flex", gap: 6, marginTop: 9, flexWrap: "wrap" }}>
                      {rv.tags.map(tg => <span key={tg} className="rs-chip" style={{ height: 26, fontSize: 11 }}>{tg}</span>)}
                    </div>
                  )}
                  {rv.reply && (
                    <div style={{ marginTop: 9, padding: 11, borderRadius: 13, background: "var(--bg-soft)" }}>
                      <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".08em", color: "var(--ink-3)" }}>YOUR REPLY</div>
                      <div style={{ fontSize: 12, color: "var(--ink-2)", marginTop: 4, lineHeight: 1.5 }}>{rv.reply}</div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>

          <Banner kind="info" icon="shield" title="A score on its own says very little"
            body={`Yours rests on ${t.ratings} ratings across ${t.trips} trips since ${t.since}. Drivers see that pair, not a bare star — the same way you see theirs.`}/>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, { PxDriverWaitingScreen, PxRiderRatingScreen });
