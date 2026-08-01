// driver-money.jsx — D25…D29: earnings, the itemised ledger, payout setup,
// ratings, and the fare-adjustment request with its three outcomes.

// ═══════════ D25 · EARNINGS ═══════════
function DvEarningsScreen() {
  const bars = WEEK_DAYS;
  const max = Math.max(...bars.map(b => b.v));
  return (
    <Phone label="D25 Earnings">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "18px 20px 22px", background: "var(--ink-fill)", color: "var(--on-ink-fill)", borderBottomLeftRadius: 28, borderBottomRightRadius: 28, flexShrink: 0 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em", opacity: .65, flex: 1 }}>THIS WEEK · {WEEK_RANGE.toUpperCase()}</div>
            <button style={{ minHeight: 44, minWidth: 44, padding: "0 8px", fontSize: 12, fontWeight: 700, opacity: .85 }}>Change</button>
          </div>
          <div className="rs-display tab" style={{ fontSize: 40, lineHeight: 1.05, marginTop: 4 }}>{FARE_POLICY.currency} {money(DRIVER_TODAY.weekTotal)}</div>
          <div style={{ fontSize: 12.5, opacity: .82, marginTop: 5 }}>Next payout {PAYOUT.nextDate} · {FARE_POLICY.currency} {money(PAYOUT.balance)} in your wallet</div>
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          {/* The wallet, and the floor it has to clear. A driver should never be
              surprised on the 1st — the rule is on the screen he checks daily. */}
          <button data-row="wallet card" style={{ textAlign: "left", padding: 15, borderRadius: 18, background: "var(--surface)", border: "1.5px solid var(--mode-drive)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", color: "var(--mode-drive-ink)", flex: 1 }}>WALLET · PAID OUT {PAYOUT.nextDate.toUpperCase()}</div>
              <StatusBadge status={payoutEligible() ? "approved" : "pending"} label={payoutEligible() ? "ABOVE MINIMUM" : "BELOW MINIMUM"}/>
            </div>
            <div style={{ display: "flex", alignItems: "baseline", gap: 9, marginTop: 7 }}>
              <div className="rs-display tab" style={{ fontSize: 30, lineHeight: 1 }}>{FARE_POLICY.currency} {money(PAYOUT.balance)}</div>
              <div style={{ fontSize: 11.5, color: "var(--ink-3)" }}>of {FARE_POLICY.currency} {money(PAYOUT.minimum)} minimum</div>
            </div>
            <div style={{ height: 8, borderRadius: 4, background: "var(--bg-soft)", border: "1px solid var(--line)", marginTop: 11, overflow: "hidden" }}>
              <div style={{ width: `${Math.min(100, (PAYOUT.balance / PAYOUT.minimum) * 100)}%`, height: "100%", background: "var(--mode-drive)" }}/>
            </div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 9, lineHeight: 1.45 }}>
              ComiGo pays drivers out every {PAYOUT.day}. Anything under {FARE_POLICY.currency} {money(PAYOUT.minimum)} stays in your wallet and goes out with the next week's payout.
            </div>
          </button>
          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 14 }}>BY DAY</div>
            <div style={{ display: "flex", alignItems: "flex-end", gap: 8, height: 108 }}>
              {bars.map(b => (
                <div key={b.d} style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", gap: 7, height: "100%", justifyContent: "flex-end" }}>
                  <div className="tab" style={{ fontSize: 9.5, fontWeight: 700, color: "var(--ink-3)" }}>{b.v ? (b.v / 1000).toFixed(1) + "k" : "—"}</div>
                  <div style={{
                    width: "100%", height: `${Math.max(3, (b.v / max) * 72)}px`, borderRadius: 6,
                    background: b.v ? (b.today ? "var(--mode-drive-ink)" : "var(--mode-drive)") : "var(--bg-soft)",
                    border: b.v ? "none" : "1px solid var(--line)",
                  }}/>
                  <div style={{ fontSize: 10.5, color: b.today ? "var(--mode-drive-ink)" : "var(--ink-3)", fontWeight: b.today ? 800 : 600 }}>{b.today ? "Today" : b.d}</div>
                </div>
              ))}
            </div>
          </div>
          <div style={{ display: "flex", gap: 10 }}>
            {[["Trips", "17"], ["Passengers", "38"], ["Per trip", `${money(539)}`]].map(([l, v]) => (
              <div key={l} style={{ flex: 1, padding: 13, borderRadius: 16, background: "var(--surface)", border: "1px solid var(--line)" }}>
                <div style={{ fontSize: 10.5, color: "var(--ink-3)", fontWeight: 600 }}>{l}</div>
                <div className="tab" style={{ fontSize: 17, fontWeight: 800, marginTop: 3 }}>{v}</div>
              </div>
            ))}
          </div>
          <div className="rs-card" style={{ padding: "2px 12px" }}>
            <MenuRow icon="receipt" label="Transaction ledger" sub="Every fare, fee, penalty and payout"/>
            <div className="rs-divider"/>
            <MenuRow icon="cash" label="Weekly payout" sub={`${PAYOUT.nextDate} · minimum ${FARE_POLICY.currency} ${money(PAYOUT.minimum)}`}/>
            <div className="rs-divider"/>
            <MenuRow icon="card" label="Payout account" sub="BOC ···2204"/>
            <div className="rs-divider"/>
            <MenuRow icon="alert" label="Request a fare adjustment" sub="If a trip was priced wrongly"/>
          </div>
        </div>
        <TabBar mode="drive" active="home" badges={{ inbox: 2 }}/>
      </div>
    </Phone>
  );
}

// ═══════════ D26 · LEDGER ═══════════
function DvLedgerScreen() {
  const rows = LEDGER;
  const META = {
    fare: { icon: "car", c: "var(--status-approved-ink)", bg: "var(--status-approved-soft)" },
    fee: { icon: "receipt", c: "var(--ink-3)", bg: "var(--bg-soft)" },
    adjust: { icon: "alert", c: "var(--status-pending-ink)", bg: "var(--status-pending-soft)" },
    penalty: { icon: "alert", c: "var(--status-rejected-ink)", bg: "var(--status-rejected-soft)" },
    // A penalty can be a POSITIVE line — his half of a fee a rider paid. It is
    // compensation for a seat nobody could use, not trip income, so it carries
    // the shield rather than the car.
    comp: { icon: "shield", c: "var(--status-approved-ink)", bg: "var(--status-approved-soft)" },
    payout: { icon: "cash", c: "var(--mode-drive-ink)", bg: "var(--mode-drive-soft)" },
  };
  return (
    <Phone label="D26 Earnings ledger">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Ledger" sub="Every fare, fee, penalty and payout"/>
        <div style={{ padding: "12px 16px 10px", display: "flex", gap: 7 }}>
          {["All", "Fares", "Fees", "Penalties", "Payouts"].map((f, i) => (
            <button key={f} className="rs-tap" style={{ flexShrink: 0 }}><span className={`rs-chip${i === 0 ? " accent" : ""}`} style={{ height: 32 }}>{f}</span></button>
          ))}
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: "0 16px 16px" }} className="rs-scroll">
          <div className="rs-card" style={{ padding: "2px 14px" }}>
            {rows.map((r, i) => {
              const m = META[r.kind];
              // A payout row is the only one that leads anywhere: it opens the
              // account the money went to. Fares and fees have no destination,
              // so they stay inert rather than looking tappable.
              const settled = r.kind === "payout";
              const Row = settled ? "button" : "div";
              return (
                <div key={i}>
                  {i > 0 && <div className="rs-divider"/>}
                  <Row {...(settled ? { "data-row": "payout to boc", className: "rs-tap", style: { padding: "13px 0", display: "flex", alignItems: "center", gap: 11, width: "100%", textAlign: "left", minHeight: 44 } } : { style: { padding: "13px 0", display: "flex", alignItems: "center", gap: 11 } })}>
                    <div style={{ width: 34, height: 34, borderRadius: 11, background: m.bg, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                      <Icon name={m.icon} size={16} color={m.c}/>
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 13, fontWeight: 700 }}>{r.label}</div>
                      <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 2 }}>{r.sub}</div>
                      <div className="tab" style={{ fontSize: 10.5, color: "var(--ink-3)", marginTop: 3 }}>{r.t}</div>
                    </div>
                    <div className="tab" style={{ fontSize: 13.5, fontWeight: 800, flexShrink: 0, color: r.v < 0 ? "var(--ink-3)" : "var(--status-approved-ink)" }}>
                      {r.v < 0 ? "−" : "+"}{FARE_POLICY.currency} {money(Math.abs(r.v))}
                    </div>
                    {/* the trailing slot is reserved on every row, so all eight
                        amounts share one right edge and the chevron appears in
                        an already-empty gutter rather than shifting its figure */}
                    {settled ? <Icon name="chev" size={17} color="var(--ink-3)"/> : <div style={{ width: 17, flexShrink: 0 }}/>}
                  </Row>
                </div>
              );
            })}
          </div>
          <div style={{ marginTop: 12, padding: 14, borderRadius: 16, background: "var(--surface)", border: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 10 }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 12.5, fontWeight: 700 }}>Wallet balance</div>
              <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 2 }}>Paid out {PAYOUT.nextDate} if it clears {FARE_POLICY.currency} {money(PAYOUT.minimum)}</div>
            </div>
            <div className="rs-display tab" style={{ fontSize: 22, fontWeight: 600 }}>{FARE_POLICY.currency} {money(ledgerBalance())}</div>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D27 · PAYOUT SETUP ═══════════
function DvPayoutScreen() {
  return (
    <Phone label="D27 Payout account">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Payout account"/>
        <div style={{ flex: 1, overflow: "auto", padding: "0 16px 16px" }} className="rs-scroll">
          <GroupLabel>WHERE YOUR MONEY GOES</GroupLabel>
          <div className="rs-card" style={{ padding: 15, border: "1.5px solid var(--mode-drive)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <div style={{ width: 42, height: 42, borderRadius: 13, background: "var(--mode-drive-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                <Icon name="cash" size={20} color="var(--mode-drive-ink)"/>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 14.5, fontWeight: 800 }}>Bank of Ceylon</div>
                <div className="tab" style={{ fontSize: 12, color: "var(--ink-3)", marginTop: 2 }}>···2204 · Nimali Perera</div>
              </div>
              <StatusBadge status="approved" label="VERIFIED"/>
            </div>
          </div>
          <div style={{ marginTop: 10, display: "flex", flexDirection: "column", gap: 9 }}>
            <div style={{ padding: 13, borderRadius: 15, background: "var(--surface)", border: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 11, opacity: .8 }}>
              <div style={{ width: 38, height: 38, borderRadius: 12, background: "var(--bg-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                <Icon name="phone" size={17} color="var(--ink-2)"/>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 13.5, fontWeight: 700}}>Mobile wallet</div>
                <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 1 }}>eZ Cash or mCash, in your own name</div>
              </div>
              <button className="rs-btn ghost" style={{ padding: "0 16px", fontSize: 12.5, flexShrink: 0 }}>Add</button>
            </div>
          </div>

          <GroupLabel>SCHEDULE</GroupLabel>
          <div className="rs-card" style={{ padding: 15 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 11 }}>
              <div style={{ width: 38, height: 38, borderRadius: 12, background: "var(--mode-drive-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                <Icon name="calendar" size={18} color="var(--mode-drive-ink)"/>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 13.5, fontWeight: 700 }}>Every {PAYOUT.day}</div>
                <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>Processed by ComiGo · not a schedule you set</div>
              </div>
              <StatusBadge status="approved" label="FIXED"/>
            </div>
            <div style={{ height: 1, background: "var(--line)", margin: "13px 0" }}/>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              <RuleRow icon="cash" title={`Minimum ${FARE_POLICY.currency} ${money(PAYOUT.minimum)}`}
                body={`Below that, your balance is held and goes out with the next week's payout. Nothing is lost — it only waits.`}/>
              <RuleRow icon="receipt" title="Card fares only"
                body={`Cash you already hold. The ${FARE_POLICY.commissionPct}% fee on cash trips is netted off this payout.`}/>
              <RuleRow icon="alert" title="Penalties come off first"
                body="Late-cancellation penalties are deducted from earnings before the balance is paid."/>
            </div>
          </div>
          <div style={{ marginTop: 10, padding: 14, borderRadius: 16, background: "var(--surface)", border: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 10 }}>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 12.5, fontWeight: 700 }}>Last payout · {PAYOUT.last.when}</div>
              <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 2 }}>{PAYOUT.last.to}</div>
            </div>
            <div className="tab" style={{ fontSize: 15, fontWeight: 800 }}>{FARE_POLICY.currency} {money(PAYOUT.last.amount)}</div>
          </div>

          <div style={{ marginTop: 14 }}>
            <Banner kind="info" icon="lock" title="Name must match your NIC"
              body="Payouts to an account in someone else's name will be rejected by the bank and delayed by about a week."/>
          </div>

          <GroupLabel>TAX</GroupLabel>
          <div className="rs-card" style={{ padding: "2px 12px" }}>
            <MenuRow icon="receipt" label="Tax identification number" sub="For drivers registered with IRD" badge={<NeedsBackend/>} chev={false}/>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D28 · RATINGS ═══════════
function DvRatingsScreen() {
  // From data.jsx, where the score is derived FROM this breakdown — the two can
  // never disagree on screen.
  const dist = TRUST.driver.stars;
  const total = dist.reduce((a, [, n]) => a + n, 0);
  return (
    <Phone label="D28 Ratings">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Ratings &amp; reviews"/>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          <div className="rs-card" style={{ padding: 16, display: "flex", gap: 18, alignItems: "center" }}>
            <div style={{ textAlign: "center", flexShrink: 0 }}>
              <div className="rs-display tab" style={{ fontSize: 40, lineHeight: 1 }}>{DRIVER_TODAY.rating}</div>
              <div style={{ display: "flex", gap: 1, marginTop: 5, justifyContent: "center" }}>
                {[1, 2, 3, 4, 5].map(i => <Icon key={i} name="star" size={12} color="var(--status-pending-ink)"/>)}
              </div>
              <div style={{ fontSize: 10.5, color: "var(--ink-3)", marginTop: 4 }}>{total} ratings</div>
            </div>
            <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: 5 }}>
              {dist.map(([star, n]) => (
                <div key={star} style={{ display: "flex", alignItems: "center", gap: 7 }}>
                  <div className="tab" style={{ fontSize: 10.5, color: "var(--ink-3)", width: 8 }}>{star}</div>
                  <div style={{ flex: 1, height: 6, borderRadius: 3, background: "var(--bg-soft)", overflow: "hidden" }}>
                    <div style={{ width: `${(n / total) * 100}%`, height: "100%", borderRadius: 3, background: "var(--status-pending-ink)" }}/>
                  </div>
                  <div className="tab" style={{ fontSize: 10, color: "var(--ink-3)", width: 24, textAlign: "right" }}>{n}</div>
                </div>
              ))}
            </div>
          </div>

          <div style={{ display: "flex", gap: 10 }}>
            {[["Acceptance", `${DRIVER_TODAY.acceptance}%`], ["On-time starts", `${DRIVER_RELIABILITY.onTimeStartPct}%`], ["Cancelled", "2%"]].map(([l, v]) => (
              <div key={l} style={{ flex: 1, padding: 13, borderRadius: 16, background: "var(--surface)", border: "1px solid var(--line)" }}>
                <div style={{ fontSize: 10.5, color: "var(--ink-3)", fontWeight: 600 }}>{l}</div>
                <div className="tab" style={{ fontSize: 17, fontWeight: 800, marginTop: 3 }}>{v}</div>
              </div>
            ))}
          </div>

          {/* Reliability is a separate contract from the star rating: stars are
              opinion, these are counts, and only these can take you offline. */}
          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 4 }}>RELIABILITY THIS MONTH</div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginBottom: 13, lineHeight: 1.45 }}>
              Counted per calendar month and reset on the 1st. Riders see your score and trip count, not these counts.
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: 13 }}>
              {[
                { l: "Missed starts", v: DRIVER_RELIABILITY.missedStarts, max: DRIVER_RELIABILITY.missedStartLimit, s: `Didn't start within ${POLICY.startBufferMin} minutes · ${DRIVER_RELIABILITY.missedStartLimit} deactivates driving`, bad: true },
                { l: "Late cancellations", v: DRIVER_RELIABILITY.lateCancellations, max: 3, s: `Inside ${POLICY.driverCancelFreeHours} hours of departure · each carries a ${POLICY.lateCancelPenaltyPct}% penalty`, bad: true },
              ].map(x => (
                <div key={x.l}>
                  <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 13, fontWeight: 700 }}>{x.l}</div>
                      <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>{x.s}</div>
                    </div>
                    <div className="tab" style={{ fontSize: 14, fontWeight: 800, color: x.v ? "var(--status-rejected-ink)" : "var(--ink-3)", flexShrink: 0 }}>{x.v} of {x.max}</div>
                  </div>
                  <div style={{ display: "flex", gap: 5, marginTop: 8 }}>
                    {Array.from({ length: x.max }, (_, i) => (
                      <div key={i} style={{ flex: 1, height: 7, borderRadius: 4, background: i < x.v ? "var(--status-rejected)" : "var(--bg-soft)", border: "1px solid var(--line)" }}/>
                    ))}
                  </div>
                </div>
              ))}
            </div>
            <div style={{ height: 1, background: "var(--line)", margin: "13px 0" }}/>
            <Banner kind="bad" icon="shield" title={`${missedStartsLeft()} missed start from deactivation`}
              body={`One more this month and your driver profile goes offline. You'd keep riding as a passenger, and an admin would have to approve you again before you can publish.`} action="What happens"/>
          </div>

          <div className="rs-section-label" style={{ marginTop: 4 }}>WHAT RIDERS SAY</div>
          <div style={{ display: "flex", gap: 7, flexWrap: "wrap" }}>
            {[["Safe driving", 142], ["Punctual", 118], ["Friendly", 96], ["Clean car", 74]].map(([l, n]) => (
              <span key={l} className="rs-chip" style={{ height: 30 }}>{l} · {n}</span>
            ))}
          </div>

          {/* Reviews are named and answerable once. An unfair review sits on a
              public profile, so the person it is about gets exactly one reply. */}
          <div style={{ display: "flex", flexDirection: "column", gap: 10, marginTop: 4 }}>
            {REVIEWS.received.map((r, i) => (
              <div key={i} className="rs-card" style={{ padding: 14 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                  <Avatar name={r.who} size={32}/>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 12.5, fontWeight: 700 }}>{r.who}.</div>
                    <div style={{ display: "flex", gap: 1, marginTop: 3 }}>
                      {[1, 2, 3, 4, 5].map(s => <Icon key={s} name="star" size={11} color={s <= r.stars ? "var(--status-pending-ink)" : "var(--line-2)"}/>)}
                    </div>
                  </div>
                  <div style={{ fontSize: 11, color: "var(--ink-3)", flexShrink: 0 }}>{r.when}</div>
                </div>
                <div style={{ fontSize: 12.5, color: "var(--ink-2)", marginTop: 9, lineHeight: 1.55 }}>{r.body}</div>
                {r.reply ? (
                  <div style={{ marginTop: 10, paddingLeft: 12, borderLeft: "2px solid var(--line-2)" }}>
                    <div style={{ fontSize: 10.5, fontWeight: 800, letterSpacing: ".06em", color: "var(--ink-3)" }}>YOUR REPLY</div>
                    <div style={{ fontSize: 12, color: "var(--ink-2)", marginTop: 4, lineHeight: 1.5 }}>{r.reply}</div>
                  </div>
                ) : (
                  <button className="rs-btn soft full" style={{ marginTop: 11, height: 44, fontSize: 12.5 }}>Reply once</button>
                )}
              </div>
            ))}
          </div>
          <Banner kind="info" icon="users" title="They see your reviews too"
            body="Rating works both ways and both sides are named. You can answer any review once — after that it stands as written."/>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D29 · FARE ADJUSTMENT ═══════════
function DvFareAdjustScreen({ state = "request" }) {
  if (state === "request") return (
    <Phone label="D29 Fare adjustment · request">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Request an adjustment" sub="Kotte → Colombo Fort · today"/>
        <div style={{ flex: 1, overflow: "auto", padding: "16px 16px 16px", display: "flex", flexDirection: "column", gap: 14 }} className="rs-scroll">
          <Banner kind="info" icon="shield" title="Every request is reviewed by a person"
            body="We check it against the route your phone recorded. Requests that don't match are declined, and repeated false ones affect your account."/>
          <div>
            <div className="rs-section-label" style={{ marginBottom: 9 }}>WHAT HAPPENED</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 9 }}>
              {[["Road closed, had to detour", true], ["Passenger asked to go further", false], ["Waited much longer than 5 minutes", false], ["Something else", false]].map(([l, on]) => (
                <div key={l} style={{
                  minHeight: 52, padding: "0 15px", borderRadius: 15, display: "flex", alignItems: "center", gap: 11,
                  background: on ? "var(--mode-drive-soft)" : "var(--surface)",
                  border: `1.5px solid ${on ? "var(--mode-drive)" : "var(--line)"}`,
                }}>
                  <div style={{ width: 20, height: 20, borderRadius: 10, border: `2px solid ${on ? "var(--mode-drive)" : "var(--line-2)"}`, background: on ? "var(--mode-drive)" : "transparent", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                    {on && <Icon name="check" size={11} color="var(--on-bright-fill)" strokeWidth={3}/>}
                  </div>
                  <div style={{ fontSize: 13.5, fontWeight: on ? 700 : 600 }}>{l}</div>
                </div>
              ))}
            </div>
          </div>
          <div>
            <div className="rs-section-label" style={{ marginBottom: 9 }}>HOW MUCH EXTRA</div>
            <div style={{ minHeight: 56, padding: "0 15px", borderRadius: 16, background: "var(--surface)", border: "1.5px solid var(--ink)", display: "flex", alignItems: "center", gap: 9 }}>
              <span style={{ fontSize: 14, color: "var(--ink-3)", fontWeight: 600 }}>{FARE_POLICY.currency}</span>
              <span className="tab" style={{ fontSize: 18, fontWeight: 800 }}>120</span>
            </div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 8, lineHeight: 1.45 }}>2.4 km of extra distance at {FARE_POLICY.currency} {money(FARE_POLICY.ratePerKm)} per km.</div>
          </div>
          <div>
            <div className="rs-section-label" style={{ marginBottom: 9 }}>EXPLAIN BRIEFLY</div>
            <div style={{ minHeight: 92, padding: 14, borderRadius: 16, background: "var(--surface)", border: "1.5px solid var(--line)", fontSize: 13, color: "var(--ink-2)", lineHeight: 1.55 }}>
              Baseline Road was closed at Borella for a procession. Police diverted everyone via Cotta Road, which added about 2.4 km.
            </div>
          </div>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn full" style={{ background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Send for review</button>
          <div style={{ textAlign: "center", fontSize: 11, color: "var(--ink-3)", marginTop: 9 }}>Usually answered within one working day.</div>
        </div>
      </div>
    </Phone>
  );

  const cfg = {
    pending: { st: "pending", title: "With our team", lead: "Submitted today at 11:04 AM. We're comparing your request against the route recorded during the trip.",
      extra: <Banner kind="info" icon="clock" title="Nothing needed from you" body="We'll notify you as soon as it's decided — usually within one working day."/>,
      cta: "Back to earnings", ghost: "Cancel this request" },
    approved: { st: "approved", title: "Adjustment approved", lead: `${FARE_POLICY.currency} ${money(120)} has been added to your balance and appears in your ledger. It'll be included in Friday's payout.`,
      extra: <Banner kind="good" icon="cash" title="Added to your balance" body={`Your ledger now shows ${FARE_POLICY.currency} ${money(ledgerBalance())} owed to you.`}/>,
      cta: "See my ledger", ghost: "Back to earnings" },
    rejected: { st: "rejected", title: "We couldn't approve this", lead: "The recorded route matches the one you published, so we couldn't verify the extra distance. Nothing changes on your earnings for this trip.",
      extra: (
        <div style={{ padding: 15, borderRadius: 18, background: "var(--status-rejected-soft)", border: "1px solid var(--status-rejected)" }}>
          <div className="rs-section-label" style={{ color: "var(--status-rejected-ink)" }}>REASON FROM THE REVIEWER</div>
          <div style={{ fontSize: 13, color: "var(--ink-2)", marginTop: 8, lineHeight: 1.55 }}>
            "GPS shows the trip followed Baseline Road without a diversion. If this is wrong, reply with the time you were diverted and we'll look again."
          </div>
          <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 9 }}>Reviewed today, 3:20 PM · ComiGo operations</div>
        </div>
      ),
      cta: "Reply with more detail", ghost: "Accept the decision" },
  }[state];
  const m = STATUS_META[cfg.st];
  return (
    <Phone label={`D29 Fare adjustment · ${state}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Fare adjustment" sub="Kotte → Colombo Fort · today"/>
        <div style={{ flex: 1, overflow: "auto", padding: "22px 20px 16px", display: "flex", flexDirection: "column", gap: 16 }} className="rs-scroll">
          <div style={{ width: 62, height: 62, borderRadius: 20, background: m.bg, display: "flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name={m.icon} size={29} color={m.c} strokeWidth={2.3}/>
          </div>
          <div>
            <div className="rs-display" style={{ fontSize: 25, lineHeight: 1.2 }}>{cfg.title}</div>
            <div style={{ fontSize: 13.5, color: "var(--ink-3)", marginTop: 11, lineHeight: 1.6, textWrap: "pretty" }}>{cfg.lead}</div>
          </div>
          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 11 }}>YOUR REQUEST</div>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 13, fontWeight: 700 }}>Road closed, had to detour</div>
                <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>2.4 km extra via Cotta Road</div>
              </div>
              <div className="rs-display tab" style={{ fontSize: 22, fontWeight: 600, color: cfg.st === "rejected" ? "var(--ink-3)" : "var(--status-approved-ink)", textDecoration: cfg.st === "rejected" ? "line-through" : "none" }}>
                {FARE_POLICY.currency} {money(120)}
              </div>
            </div>
          </div>
          {cfg.extra}
        </div>
        <div style={{ padding: "12px 20px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">{cfg.cta}</button>
          <button className="rs-btn ghost full" style={{ marginTop: 10 }}>{cfg.ghost}</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D33 · MONTHLY PAYOUT ═══════════
// Payouts are an admin-run monthly batch with a floor. Two states, because the
// held one is the only one a driver ever needs explained.
function DvMonthlyPayoutScreen({ state = "eligible" }) {
  const held = state === "held";
  const bal = held ? PAYOUT.heldBalance : PAYOUT.balance;
  const short = PAYOUT.minimum - PAYOUT.heldBalance;
  return (
    <Phone label={`${held ? "D33b" : "D33"} Monthly payout · ${held ? "held" : "eligible"}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Weekly payout" sub={`Processed by ComiGo every ${PAYOUT.day}`}/>
        <div style={{ flex: 1, overflow: "auto", padding: "16px 16px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          <div style={{ padding: 18, borderRadius: 20, background: held ? "var(--status-pending-soft)" : "var(--status-approved-soft)", border: `1px solid ${held ? "var(--status-pending)" : "var(--status-approved)"}` }}>
            <div style={{ display: "flex", alignItems: "center", gap: 9 }}>
              <Icon name={held ? "clock" : "check"} size={18} color={held ? "var(--status-pending-ink)" : "var(--status-approved-ink)"} strokeWidth={2.4}/>
              <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", color: held ? "var(--status-pending-ink)" : "var(--status-approved-ink)" }}>
                {held ? "HELD FOR NEXT WEEK" : `SCHEDULED FOR ${PAYOUT.nextDate.toUpperCase()}`}
              </div>
            </div>
            <div className="rs-display tab" style={{ fontSize: 38, lineHeight: 1.05, marginTop: 10 }}>{FARE_POLICY.currency} {money(bal)}</div>
            <div style={{ fontSize: 12.5, color: "var(--ink-2)", marginTop: 6, lineHeight: 1.5 }}>
              {held
                ? `${FARE_POLICY.currency} ${money(short)} short of the ${FARE_POLICY.currency} ${money(PAYOUT.minimum)} minimum. This balance carries into next week's payout — nothing expires.`
                : `Above the ${FARE_POLICY.currency} ${money(PAYOUT.minimum)} minimum, so it goes to ${PAYOUT.last.to} this ${PAYOUT.day}.`}
            </div>
            <div style={{ height: 9, borderRadius: 5, background: "var(--surface)", border: "1px solid var(--line)", marginTop: 14, overflow: "hidden" }}>
              <div style={{ width: `${Math.min(100, (bal / PAYOUT.minimum) * 100)}%`, height: "100%", background: held ? "var(--status-pending)" : "var(--status-approved)" }}/>
            </div>
            <div style={{ display: "flex", marginTop: 7 }}>
              <div style={{ fontSize: 10.5, color: "var(--ink-3)", flex: 1 }}>{FARE_POLICY.currency} 0</div>
              <div className="tab" style={{ fontSize: 10.5, color: "var(--ink-3)", fontWeight: 700 }}>minimum {FARE_POLICY.currency} {money(PAYOUT.minimum)}</div>
            </div>
          </div>

          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 12 }}>HOW PAYOUTS WORK</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              <RuleRow icon="calendar" title={`Every ${PAYOUT.day}`} body="A ComiGo admin runs the batch for every driver. There is no on-demand withdrawal."/>
              <RuleRow icon="cash" title={`Only above ${FARE_POLICY.currency} ${money(PAYOUT.minimum)}`} body="Under the floor, transfer fees eat the payment. Your balance is held, not lost, and goes out the following month."/>
              <RuleRow icon="alert" title="Penalties and fees come off first" body={`The ${FARE_POLICY.commissionPct}% ComiGo fee and any late-cancellation penalty are netted before the transfer.`}/>
              <RuleRow icon="receipt" title="You get a statement" body="Every payout lists the trips behind it, in your ledger and by email."/>
            </div>
          </div>

          {held && (
            <Banner kind="info" icon="car" title="Two more trips would clear it"
              body={`At about ${FARE_POLICY.currency} ${money(SEAT_NET)} a seat, two full-route seats put you over the minimum before ${PAYOUT.day}.`} action="Publish"/>
          )}

          <div className="rs-card" style={{ padding: "2px 12px" }}>
            <MenuRow icon="cash" label="Payout account" sub={PAYOUT.last.to}/>
            <div className="rs-divider"/>
            <MenuRow icon="receipt" label={`Last payout · ${PAYOUT.last.when}`} sub={`${FARE_POLICY.currency} ${money(PAYOUT.last.amount)} · week of 13–19 Jul`}/>
            <div className="rs-divider"/>
            <MenuRow icon="history" label="Ledger" sub="Every fare, fee, penalty and payout"/>
          </div>
        </div>
        <TabBar mode="drive" active="home" badges={{ inbox: 2 }}/>
      </div>
    </Phone>
  );
}

// ═══════════ D34 · DRIVER PROFILE DEACTIVATED ═══════════
// The third missed start in a month. Riding is untouched — only driving stops,
// and only an admin can turn it back on.
function DvDeactivatedScreen() {
  return (
    <Phone label="D34 Driver profile deactivated">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "22px 20px 24px", background: "var(--danger)", color: "var(--on-bright-fill)", flexShrink: 0 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <Icon name="shield" size={20} color="var(--on-bright-fill)" strokeWidth={2.4}/>
            <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em" }}>DRIVING SUSPENDED · CASE #DR-4417</div>
          </div>
          <div className="rs-display" style={{ fontSize: 27, lineHeight: 1.15, marginTop: 11 }}>Your driver profile<br/>is deactivated</div>
          <div style={{ fontSize: 13, marginTop: 8, opacity: .92, lineHeight: 1.55 }}>
            Three missed starts in {EARLY_DROP.month}. You can still ride as a passenger — nothing about your rider account changes.
          </div>
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 12 }}>THE THREE MISSES</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              {[["24 Jul · 7:45 AM", "Nugegoda → Colombo Fort", "2 riders waiting"], ["21 Jul · 5:30 PM", "Colombo Fort → Nugegoda", "1 rider waiting"], [`Today · ${NEXT_DRIVE.depart}`, `${NEXT_DRIVE.from} → ${NEXT_DRIVE.to}`, `${NEXT_DRIVE.passengers.length} riders waiting`]].map(([t, r, s]) => (
                <div key={t} style={{ display: "flex", gap: 11, alignItems: "flex-start" }}>
                  <div style={{ width: 22, height: 22, borderRadius: 11, background: "var(--status-rejected-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0, marginTop: 1 }}>
                    <Icon name="close" size={12} color="var(--status-rejected-ink)" strokeWidth={3}/>
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div className="tab" style={{ fontSize: 11, fontWeight: 700, color: "var(--ink-3)" }}>{t.toUpperCase()}</div>
                    <div style={{ fontSize: 13, fontWeight: 700, marginTop: 2 }}>{r}</div>
                    <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 1 }}>{s} · auto-cancelled after {POLICY.startBufferMin} minutes</div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 12 }}>WHAT THIS DOES AND DOESN'T DO</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              <RuleRow icon="close" tint="var(--status-rejected-ink)" title="You can't publish trips" body="Your published trips were cancelled and nobody was charged."/>
              <RuleRow icon="check" tint="var(--status-approved-ink)" title="Money you've earned is safe" body={`${FARE_POLICY.currency} ${money(PAYOUT.balance)} still pays out on ${PAYOUT.day} as normal.`}/>
              <RuleRow icon="check" tint="var(--status-approved-ink)" title="You can still ride" body="Booking seats as a passenger works exactly as before."/>
              <RuleRow icon="user" title="Your documents stay verified" body="Nothing needs re-uploading. This is a reliability decision, not a KYC one."/>
            </div>
          </div>

          <Banner kind="warn" icon="clock" title="An admin reviews reinstatement requests"
            body="Tell us what went wrong. Requests are usually answered within two working days, and a first reinstatement is normally granted."/>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)", display: "flex", flexDirection: "column", gap: 9 }}>
          <button className="rs-btn accent full">Request reinstatement</button>
          <button className="rs-btn soft full">Keep riding as a passenger</button>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, { DvEarningsScreen, DvLedgerScreen, DvPayoutScreen, DvRatingsScreen, DvFareAdjustScreen, DvMonthlyPayoutScreen, DvDeactivatedScreen });
