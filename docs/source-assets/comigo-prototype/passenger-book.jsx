// passenger-book.jsx — P08…P14: seat select, checkout, payment methods, and the
// three booking outcomes the old design never showed (awaiting / approved /
// declined) plus the seat-inventory conflict.

// ═══════════ P08 · SEAT SELECT ═══════════
// Named seats, not a cabin diagram: the rider picks "front" or "back", which is
// the only distinction that changes the ride. Picking the seats IS the count, so
// the old quantity stepper is gone — two controls for one decision is one too many.
function PxSeatSelectScreen({ seatCount = 1 }) {
  const r = RIDES[0];
  const cap = vehicleClass(r.vClass).maxSeats;
  const taken = [1];
  const selected = Array.from({ length: seatCount }, (_, i) => (i === 0 ? 0 : 2));
  return (
    <Phone label={`P08 Seat select · ${seatCount}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Choose your seat" sub={`${r.driver} · ${r.car.split(" · ")[0]} · ${cap} passenger seats`}/>
        <div style={{ flex: 1, overflow: "auto", padding: "16px 16px 16px", display: "flex", flexDirection: "column", gap: 14 }} className="rs-scroll">
          <SeatPlan taken={taken} selected={selected} capacity={cap} price={r.price} currency={FARE_POLICY.currency}/>
          <Banner kind="info" icon="users" title="Every seat is the same price"
            body={`Front or back, you pay ${FARE_POLICY.currency} ${money(r.price)} for your ${r.dist} km. Take more than one if you're travelling together.`}/>
          <div style={{ padding: 15, borderRadius: 18, background: "var(--surface)", border: "1px solid var(--line)" }}>
            <div className="rs-section-label" style={{ marginBottom: 11 }}>WHAT SAMAN CHARGES</div>
            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <div className="rs-display tab" style={{ fontSize: 26, lineHeight: 1 }}>{FARE_POLICY.currency} {money(r.ratePerKm)}</div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 12.5, fontWeight: 700 }}>per kilometre, per seat</div>
                <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>Inside the {FARE_POLICY.currency} {money(vehicleClass(r.vClass).band[0])}–{money(vehicleClass(r.vClass).band[1])} band ComiGo set for a {r.vClass.toLowerCase()} like his.</div>
              </div>
            </div>
          </div>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 13 }}>
          <div>
            <div className="rs-display tab" style={{ fontSize: 22, fontWeight: 600 }}>{FARE_POLICY.currency} {money(r.price * seatCount)}</div>
            <div style={{ fontSize: 11, color: "var(--ink-3)" }}>{seatCount} {seatCount === 1 ? "seat" : "seats"} selected</div>
          </div>
          <button className="rs-btn accent" style={{ flex: 1 }}>Continue to pay</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ P09 · CHECKOUT ═══════════
// Cash and tokenised card only. No wallet — there is no balance domain.
// Three variants, and the only real difference between them is WHEN the money
// moves: on start for a scheduled trip, on acceptance for one already running.
function PxCheckoutScreen({ variant = "approval", dues = false, credit = false }) {
  const rewards = Math.min(rewardsBalance(), 200);
  const cfg = {
    approval: {
      num: "P09", tag: "needs approval", r: MY_TRIP,
      banner: { kind: "warn", icon: "clock", title: "Kasun approves each booking",
        body: `Accepting does not charge you. Your card is charged when he starts the trip at ${MY_TRIP.chargedAt} — if he declines, cancels, or never starts, nothing is taken.` },
      cardSub: `Charged at ${MY_TRIP.chargedAt}, when the trip starts`,
      cta: "Request this seat",
      foot: `Free to cancel until 30 minutes before departure. Nothing leaves your card before ${MY_TRIP.chargedAt}.`,
    },
    instant: {
      num: "P09b", tag: "instant", r: RIDES[0],
      banner: { kind: "info", icon: "card", title: "Charged when the trip starts",
        body: `Your seat is confirmed the moment you book. The card is charged at ${RIDES[0].depart}, when Saman starts driving — not now.` },
      cardSub: `Charged at ${RIDES[0].depart}, on start`,
      cta: null,
      foot: "Free to cancel until 30 minutes before departure.",
    },
    enroute: {
      num: "P09c", tag: "trip already running", r: ENROUTE_RIDE,
      banner: { kind: "warn", icon: "car", title: "This trip is already on the road",
        body: `${ENROUTE_RIDE.driver.split(" ")[0]} is ${ENROUTE_RIDE.etaMin} minutes away — a seat came free at ${ENROUTE_RIDE.from}. Because the trip has started, your card is charged the moment he accepts.` },
      cardSub: `Charged as soon as ${ENROUTE_RIDE.driver.split(" ")[0]} accepts`,
      cta: "Request seat · charged on accept",
      foot: "There is no waiting period on a running trip. If he declines, nothing is taken.",
    },
  }[variant];
  const r = cfg.r;
  const first = r.driver.split(" ")[0];
  return (
    <Phone label={`${cfg.num}${dues ? "d" : credit ? "e" : ""} Checkout · ${dues ? "unpaid fee carried over" : credit ? "ride credit applied" : cfg.tag}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Confirm and pay"/>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 14 }} className="rs-scroll">
          <div className="rs-card" style={{ padding: 14, display: "flex", alignItems: "center", gap: 12 }}>
            <Avatar name={r.driver} size={44}/>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 700 }}>{r.driver} · {r.depart}</div>
              <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, display: "flex", alignItems: "center", gap: 5 }}>
                <Icon name="star" size={12} color="var(--status-pending-ink)"/><span className="tab">{r.rating}</span> · {r.trips} trips · {r.plate}
              </div>
            </div>
            <MatchRing value={r.match} size={42} strokeWidth={3.4}/>
          </div>

          <Banner {...cfg.banner}/>

          {dues && (
            <Banner kind="warn" icon="alert" title={`${FARE_POLICY.currency} ${money(duesTotal())} unpaid fee added`}
              body={`A no-show fee from 21 July. You paid that trip in cash, so it couldn't be taken then — it has to be settled before you ride again.`} action="Details"/>
          )}

          {credit && (
            <Banner kind="good" icon="gift" title={`${FARE_POLICY.currency} ${money(rewards)} referral credit applied`}
              body={`You have ${FARE_POLICY.currency} ${money(rewardsBalance())} in rewards. We use it on every booking until it runs out — turn it off here if you'd rather save it.`} action="Don't use"/>
          )}

          <div>
            <div className="rs-section-label" style={{ marginBottom: 9 }}>HOW YOU'LL PAY</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 9 }}>
              {[
                { key: "card", icon: "card", label: "Visa ···4429", sub: cfg.cardSub, on: true },
                { key: "cash", icon: "cash", label: "Cash", sub: `Hand ${FARE_POLICY.currency} ${money(r.price)} to ${first} on board`, on: false },
              ].map(p => (
                <div key={p.key} style={{
                  padding: 13, borderRadius: 16, display: "flex", alignItems: "center", gap: 11,
                  background: p.on ? "var(--accent-soft)" : "var(--surface)",
                  border: `1.5px solid ${p.on ? "var(--accent-ink)" : "var(--line)"}`,
                }}>
                  <div style={{ width: 36, height: 36, borderRadius: 12, background: p.on ? "var(--surface)" : "var(--bg-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                    <Icon name={p.icon} size={17} color={p.on ? "var(--accent-ink)" : "var(--ink-2)"}/>
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13.5, fontWeight: 700 }}>{p.label}</div>
                    <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 1 }}>{p.sub}</div>
                  </div>
                  <div style={{ width: 22, height: 22, borderRadius: 11, border: `2px solid ${p.on ? "var(--accent-ink)" : "var(--line-2)"}`, background: p.on ? "var(--accent-ink)" : "transparent", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                    {p.on && <Icon name="check" size={12} color="#fff" strokeWidth={3}/>}
                  </div>
                </div>
              ))}
              <button className="rs-btn ghost full" style={{ height: 46 }}><Icon name="plus" size={16}/> Add a card</button>
            </div>
          </div>

          <div>
            <div className="rs-section-label" style={{ marginBottom: 9 }}>WHAT YOU PAY</div>
            <FareBreakdown
              currency={FARE_POLICY.currency}
              lines={[
                { label: `On-route distance · ${r.dist} km`, sub: `At ${FARE_POLICY.currency} ${money(r.ratePerKm)} per km — ${first}'s rate`, value: r.gross },
                { label: FARE_POLICY.discountLabel, sub: `${r.match}% overlap`, value: r.discount, kind: "discount" },
                ...(credit ? [{ label: "Referral credit", sub: `From ${REFERRAL.joined} people you invited`, value: rewards, kind: "discount", always: true }] : []),
                ...(dues ? [{ label: `Unpaid no-show fee · ${POLICY.noShowPenaltyPct}%`, sub: "21 Jul · Narahenpita → Thunmulla, paid in cash", value: duesTotal(), kind: "fee", always: true }] : []),
              ]}
              total={r.price + (dues ? duesTotal() : 0) - (credit ? rewards : 0)}
              footnote={`Includes a ${FARE_POLICY.commissionPct}% ComiGo fee. Get off earlier than planned and we recalculate on the distance you actually travelled — twice a month, then the fare stands.`}
            />
          </div>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">{cfg.cta || `Book for ${FARE_POLICY.currency} ${money(r.price)}`}</button>
          <div style={{ textAlign: "center", fontSize: 11, color: "var(--ink-3)", marginTop: 9, lineHeight: 1.5 }}>
            {cfg.foot}
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ P10 · PAYMENT METHODS ═══════════
function PxPaymentMethodsScreen() {
  return (
    <Phone label="P10 Payment methods">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Payment methods"/>
        <div style={{ flex: 1, overflow: "auto", padding: "0 16px 16px" }} className="rs-scroll">
          <GroupLabel>CARDS</GroupLabel>
          <div style={{ display: "flex", flexDirection: "column", gap: 9 }}>
            {[
              { label: "Visa ···4429", sub: "Expires 08/28", def: true },
              { label: "Mastercard ···1180", sub: "Expires 02/27", def: false },
            ].map(c => (
              <div key={c.label} className="rs-card" style={{ padding: 13, display: "flex", alignItems: "center", gap: 11 }}>
                <div style={{ width: 38, height: 38, borderRadius: 12, background: "var(--bg-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                  <Icon name="card" size={18} color="var(--ink-2)"/>
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 7 }}>
                    <div className="tab" style={{ fontSize: 13.5, fontWeight: 700 }}>{c.label}</div>
                    {c.def && <StatusBadge status="approved" label="DEFAULT"/>}
                  </div>
                  <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>{c.sub}</div>
                </div>
                <button style={{ width: 44, height: 44, borderRadius: 22, display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label={`Options for ${c.label}`}>
                  <Icon name="ellipsis" size={19} color="var(--ink-3)"/>
                </button>
              </div>
            ))}
          </div>
          <button className="rs-btn ghost full" style={{ marginTop: 11, height: 48 }}><Icon name="plus" size={17}/> Add a card</button>

          <GroupLabel>CASH</GroupLabel>
          <div className="rs-card" style={{ padding: 13, display: "flex", alignItems: "center", gap: 11 }}>
            <div style={{ width: 38, height: 38, borderRadius: 12, background: "var(--status-approved-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
              <Icon name="cash" size={18} color="var(--status-approved-ink)"/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 13.5, fontWeight: 700 }}>Pay the driver in cash</div>
              <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>Always available. Choose it per trip at checkout.</div>
            </div>
          </div>

          <div style={{ marginTop: 16, padding: 14, borderRadius: 16, background: "var(--bg-soft)", border: "1px solid var(--line)", display: "flex", gap: 10 }}>
            <Icon name="lock" size={17} color="var(--ink-3)"/>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", lineHeight: 1.5 }}>
              ComiGo never stores your card number. Cards are stored as a token by our payment provider, and you can remove one at any time.
            </div>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ P11–P13 · the three booking outcomes ═══════════
function PxBookingStateScreen({ state = "awaiting" }) {
  const r = MY_TRIP;
  // Same list on three screens — declined, cancelled and auto-cancelled all end
  // with "here is another way to get to work", so it is written once.
  const altList = (
    <div style={{ padding: 15, borderRadius: 18, background: "var(--surface)", border: "1px solid var(--line)" }}>
      <div className="rs-section-label" style={{ marginBottom: 10 }}>{r.alternatives.length} OTHER DRIVERS ON YOUR ROUTE</div>
      <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
        {r.alternatives.map(x => {
          const t = matchTier(x.match);
          return (
            <div key={x.driver} style={{ display: "flex", alignItems: "center", gap: 11 }}>
              <MatchRing value={x.match} size={40} strokeWidth={3.4}/>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 13, fontWeight: 700 }}>{x.driver} · {x.depart}</div>
                <div style={{ fontSize: 11, fontWeight: 700, color: t.ink, marginTop: 1 }}>{t.label}</div>
              </div>
              <div className="tab" style={{ fontSize: 13.5, fontWeight: 800 }}>{FARE_POLICY.currency} {money(x.price)}</div>
            </div>
          );
        })}
      </div>
    </div>
  );
  const cfg = {
    awaiting: {
      tint: "var(--status-pending-soft)", ink: "var(--status-pending-ink)", icon: "clock",
      kicker: "WAITING FOR KASUN", title: "Seat requested", lead: `${r.driver.split(" ")[0]} has 30 minutes to accept. We'll notify you the moment he does — you don't need to stay on this screen.`,
      body: (
        <>
          <Banner kind="info" icon="card" title="Nothing charged — and not on acceptance either"
            body={`Your Visa is authorised, not charged. The money moves only when Kasun starts the trip at ${r.chargedAt}. Decline, cancel or a no-start all cost you nothing.`}/>
          <div style={{ marginTop: 11, padding: 14, borderRadius: 16, background: "var(--surface)", border: "1px solid var(--line)" }}>
            <div className="rs-section-label" style={{ marginBottom: 10 }}>MEANWHILE</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 9 }}>
              {[["search", "Keep looking", "Request another seat — you can have two requests open at once."], ["bell", "We'll ping you", "Push and SMS, because a missed approval costs you the seat."]].map(([ic, t, s]) => (
                <div key={t} style={{ display: "flex", gap: 10 }}>
                  <Icon name={ic} size={16} color="var(--ink-3)"/>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontSize: 12.5, fontWeight: 700 }}>{t}</div>
                    <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 1, lineHeight: 1.4 }}>{s}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </>
      ),
      cta: "Find another seat too", ghost: "Cancel the request",
    },
    approved: {
      tint: "var(--status-approved-soft)", ink: "var(--status-approved-ink)", icon: "check",
      kicker: "CONFIRMED", title: "You're booked", lead: `${r.driver.split(" ")[0]} accepted. Be at ${r.from} by ${r.depart} — he waits ${POLICY.pickupWaitMin} minutes.`,
      body: (
        <>
          <div style={{ padding: 15, borderRadius: 18, background: "var(--surface)", border: "1px solid var(--line)" }}>
            <RouteTimeline compact stops={[
              { kind: "pickup", place: r.from, time: r.depart, note: `Look for a ${r.car.split(" · ")[1].toLowerCase()} ${r.car.split(" · ")[0]} · ${r.plate}` },
              { kind: "drop", place: r.to, time: r.arrive, note: "200 m from the station" },
            ]}/>
          </div>
          <div style={{ marginTop: 11 }}>
            <Banner kind="info" icon="card" title={`Your card is charged at ${r.chargedAt}`}
              body={`${FARE_POLICY.currency} ${money(r.price)} is taken when Kasun starts the trip — not now. Cancel before then and you pay nothing.`}/>
          </div>
          <div style={{ marginTop: 11 }}>
            <Banner kind="info" icon="clock" title={`He should reach you at ${r.pickupAt}`}
              body={`We watch it for you. If he is more than ${POLICY.driverLateGraceMin} minutes past that, you can cancel free of charge — and he carries the penalty instead of you.`}/>
          </div>
          <div style={{ marginTop: 11 }}>
            <TrustStats name={r.driver} role="Your driver" rating={TRUST.driver.rating} ratings={TRUST.driver.ratings}
              trips={TRUST.driver.trips} completed={TRUST.driver.completed} since={TRUST.driver.since}/>
          </div>
          <div style={{ marginTop: 11, display: "flex", gap: 9 }}>
            <button className="rs-btn soft" style={{ flex: 1, height: 48, fontSize: 12.5 }}><Icon name="phone" size={16}/> Call</button>
            <button className="rs-btn soft" style={{ flex: 1, height: 48, fontSize: 12.5 }}><Icon name="chat" size={16}/> Chat</button>
            <button className="rs-btn soft" style={{ flex: 1, height: 48, fontSize: 12.5 }}><Icon name="share" size={16}/> Share</button>
          </div>
        </>
      ),
      cta: "Track this trip", ghost: "Cancel booking",
    },
    declined: {
      tint: "var(--status-rejected-soft)", ink: "var(--status-rejected-ink)", icon: "close",
      kicker: "NOT THIS TIME", title: "Kasun declined", lead: "It happens — he may have filled the seat or changed his plan. Your card was never charged.",
      body: (
        <>
          {altList}
          <div style={{ marginTop: 11 }}>
            <Banner kind="info" icon="shield" title="This doesn't affect your rating" body="Drivers decline for all sorts of reasons. Nothing is recorded against you."/>
          </div>
        </>
      ),
      cta: `See all ${r.alternatives.length} drivers`, ghost: "Back to home",
    },
    // Driver cancelled a trip that had not started. Under charge-on-start there
    // is nothing to refund — saying "refunded in full" would invent a charge.
    cancelled: {
      tint: "var(--status-rejected-soft)", ink: "var(--status-rejected-ink)", icon: "alert",
      kicker: "TRIP CANCELLED BY DRIVER", title: "Kasun cancelled",
      lead: `He cancelled ${r.cancelledBeforeHrs} hours before departure. Because the trip never started, your card was never charged — and half of what he pays for that comes to you.`,
      body: (
        <>
          <div style={{ padding: 15, borderRadius: 18, background: "var(--surface)", border: "1px solid var(--line)" }}>
            <div className="rs-section-label" style={{ marginBottom: 11 }}>YOUR CARD</div>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 12.5, fontWeight: 700 }}>Visa ···4429 · never charged</div>
                <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.45 }}>The authorisation is released today. No refund to wait for, no fee.</div>
              </div>
              <div className="rs-display tab" style={{ fontSize: 23, fontWeight: 600, color: "var(--status-approved-ink)" }}>{FARE_POLICY.currency} 0</div>
            </div>
            <div className="rs-divider" style={{ margin: "13px 0" }}/>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 12.5, fontWeight: 700 }}>Your share of his penalty</div>
                <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.45 }}>{POLICY.penaltyVictimPct}% of the {POLICY.lateCancelPenaltyPct}% he pays, as ride credit. He loses it either way — now some of it reaches the person it cost.</div>
              </div>
              <div className="rs-display tab" style={{ fontSize: 23, fontWeight: 600, color: "var(--status-approved-ink)" }}>+{FARE_POLICY.currency} {money(victimShare(paxCancelPenalty(r.price)))}</div>
            </div>
          </div>
          <div style={{ marginTop: 11 }}>{altList}</div>
          <div style={{ marginTop: 11 }}>
            <Banner kind="info" icon="shield" title={`Cancelling inside ${POLICY.driverCancelFreeHours} hours costs him`}
              body={`A penalty comes out of his next trip's earnings and it counts against his reliability. Half of it is credited to you, half to ComiGo. Outside ${POLICY.driverCancelFreeHours} hours there is no penalty — which is why most changes reach you early.`} action="Report"/>
          </div>
        </>
      ),
      cta: `Find another seat`, ghost: "Contact support",
    },
    // The driver never started within the buffer, so the system cancelled for him.
    autocancelled: {
      tint: "var(--status-rejected-soft)", ink: "var(--status-rejected-ink)", icon: "clock",
      kicker: `NO START WITHIN ${POLICY.startBufferMin} MINUTES`, title: "We cancelled this trip",
      lead: `${r.driver.split(" ")[0]} didn't start the trip within ${POLICY.startBufferMin} minutes of ${r.depart}, so ComiGo cancelled it for you. Nothing was charged.`,
      body: (
        <>
          <div style={{ padding: 15, borderRadius: 18, background: "var(--surface)", border: "1px solid var(--line)" }}>
            <div className="rs-section-label" style={{ marginBottom: 11 }}>WHAT WE DID</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              <RuleRow icon="card" title="Your card was never charged" body={`Money only moves at trip start. The ${FARE_POLICY.currency} ${money(r.price)} authorisation is released.`}/>
              <RuleRow icon="star" title="It counts against him, not you" body="His reliability score drops and the miss is recorded. Your account is untouched."/>
              <RuleRow icon="search" title="We already looked again" body={`${r.alternatives.length} drivers are still going your way in the next 40 minutes.`}/>
            </div>
          </div>
          <div style={{ marginTop: 11 }}>{altList}</div>
        </>
      ),
      cta: "Book the next one", ghost: "Contact support",
    },
    // He started, but he is not at HER kerb. A separate clock from the start
    // buffer: this one runs from her promised pickup time, because a trip that
    // left on time can still be twenty minutes from her corner.
    driverlate: {
      tint: "var(--status-pending-soft)", ink: "var(--status-pending-ink)", icon: "clock",
      kicker: `${r.lateByMin} MINUTES LATE`, title: "Kasun hasn't arrived",
      lead: `He was due at ${r.from} at ${r.pickupAt}. Past ${POLICY.driverLateGraceMin} minutes you can walk away at no cost — and he carries the penalty, not you.`,
      body: (
        <>
          <div style={{ padding: 15, borderRadius: 18, background: "var(--surface)", border: "1.5px solid var(--status-pending)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 11 }}>
              <div style={{ width: 8, height: 8, borderRadius: 4, background: "var(--status-pending)", flexShrink: 0 }}/>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 12.5, fontWeight: 800, color: "var(--status-pending-ink)" }}>Live position says {r.etaMin} more minutes</div>
                <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.45 }}>Held up at Welikada. You decide whether that's worth waiting for.</div>
              </div>
            </div>
            <div style={{ display: "flex", gap: 9, marginTop: 13 }}>
              <button className="rs-btn soft" style={{ flex: 1, height: 48, fontSize: 12.5 }}><Icon name="phone" size={16}/> Call</button>
              <button className="rs-btn soft" style={{ flex: 1, height: 48, fontSize: 12.5 }}><Icon name="chat" size={16}/> Chat</button>
            </div>
          </div>
          <div style={{ marginTop: 11, padding: 15, borderRadius: 18, background: "var(--surface)", border: "1px solid var(--line)" }}>
            <div className="rs-section-label" style={{ marginBottom: 12 }}>IF YOU CANCEL NOW</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              <RuleRow icon="card" tint="var(--status-approved-ink)" title="The whole fare goes back"
                body={`All ${FARE_POLICY.currency} ${money(r.price)} of it. No late-cancellation fee applies once he is more than ${POLICY.driverLateGraceMin} minutes past your pickup time.`}/>
              <RuleRow icon="gift" tint="var(--status-approved-ink)" title={`Plus ${FARE_POLICY.currency} ${money(victimShare(paxCancelPenalty(r.price)))} in ride credit`}
                body={`He pays ${POLICY.driverLatePenaltyPct}% for leaving you standing there, and ${POLICY.penaltyVictimPct}% of that reaches you.`}/>
              <RuleRow icon="star" title="Nothing is recorded against you"
                body="This is his lateness. Your rating and your no-show count are untouched."/>
            </div>
          </div>
          <div style={{ marginTop: 11 }}>{altList}</div>
        </>
      ),
      cta: "Cancel — no charge", ghost: `Keep waiting for ${r.driver.split(" ")[0]}`,
    },
    // The buffer has run out and he has taken his one extension. That extension
    // protects HIM from the auto-cancel; it does not oblige her to wait it out.
    notstarted: {
      tint: "var(--status-pending-soft)", ink: "var(--status-pending-ink)", icon: "clock",
      kicker: `${POLICY.startBufferMin} MINUTES PAST DEPARTURE`, title: "Kasun hasn't started",
      lead: `${r.depart} has come and gone. He has taken his one ${POLICY.startExtendMin}-minute extension, so ComiGo won't cancel yet — but you don't have to wait for that.`,
      body: (
        <>
          <div style={{ padding: 15, borderRadius: 18, background: "var(--surface)", border: "1.5px solid var(--status-pending)" }}>
            <div className="rs-section-label" style={{ marginBottom: 11 }}>THE TWO CLOCKS</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              <RuleRow icon="clock" tint="var(--status-pending-ink)" title={`His extension: ${POLICY.startExtendMin} minutes left`}
                body="One extension, once. If he still hasn't started at the end of it, ComiGo cancels the trip and records a missed start against him."/>
              <RuleRow icon="user" tint="var(--status-approved-ink)" title="Yours: already expired"
                body={`You could leave from the ${POLICY.startBufferMin}-minute mark. His extension is his protection, not an obligation on you.`}/>
            </div>
          </div>
          <div style={{ marginTop: 11 }}>
            <Banner kind="good" icon="card" title="Nothing has been charged"
              body="Money only moves when a trip starts. Leaving now costs you nothing at all — there is no fee and no fare to claw back."/>
          </div>
          <div style={{ marginTop: 11 }}>{altList}</div>
        </>
      ),
      cta: "Cancel — no charge", ghost: `Give him the extra ${POLICY.startExtendMin} minutes`,
    },
  }[state];
  const num = { awaiting: "P11", approved: "P12", declined: "P13", cancelled: "P22", autocancelled: "P24", driverlate: "P34", notstarted: "P35" }[state];
  return (
    <Phone label={`${num} Booking · ${state}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Your booking" sub={`${r.bookingRef} · ${r.driver}`}/>
        <div style={{ flex: 1, overflow: "auto", padding: "20px 16px 16px" }} className="rs-scroll">
          <div style={{ display: "flex", alignItems: "center", gap: 13 }}>
            <div style={{ width: 56, height: 56, borderRadius: 19, background: cfg.tint, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
              <Icon name={cfg.icon} size={26} color={cfg.ink} strokeWidth={2.3}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em", color: cfg.ink }}>{cfg.kicker}</div>
              <div className="rs-display" style={{ fontSize: 25, marginTop: 3, lineHeight: 1.15 }}>{cfg.title}</div>
            </div>
          </div>
          <div style={{ fontSize: 13.5, color: "var(--ink-3)", marginTop: 12, lineHeight: 1.6, textWrap: "pretty" }}>{cfg.lead}</div>
          <div style={{ marginTop: 16 }}>{cfg.body}</div>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">{cfg.cta}</button>
          <button className="rs-btn ghost full" style={{ marginTop: 10 }}>{cfg.ghost}</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ P14 · SEATS TAKEN WHILE YOU WERE DECIDING ═══════════
// Seat inventory is transactional, so the booking call can lose the race.
function PxSeatsTakenScreen() {
  const r = RIDES[0];
  return (
    <Phone label="P14 Seats just taken">
      <div style={{ height: "100%", position: "relative", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ flex: 1, display: "flex", flexDirection: "column", opacity: .38 }}>
          <AppBar title="Confirm and pay"/>
          <div style={{ padding: "14px 16px", display: "flex", flexDirection: "column", gap: 12 }}>
            <div className="rs-card" style={{ padding: 14, display: "flex", alignItems: "center", gap: 12 }}>
              <Avatar name={r.driver} size={44}/>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 14, fontWeight: 700 }}>{r.driver} · {r.depart}</div>
                <div style={{ fontSize: 11.5, color: "var(--ink-3)" }}>{r.car}</div>
              </div>
            </div>
            <SkelRow/>
          </div>
        </div>
        <div style={{ position: "absolute", inset: 0, background: "rgba(20,10,5,.5)" }}/>
        <div className="rs-sheet" style={{ position: "absolute", left: 0, right: 0, bottom: 0, padding: "8px 22px 22px" }}>
          <div className="rs-sheet-grab"/>
          <div style={{ width: 56, height: 56, borderRadius: 19, background: "var(--status-pending-soft)", display: "flex", alignItems: "center", justifyContent: "center", marginTop: 10 }}>
            <Icon name="users" size={26} color="var(--status-pending-ink)"/>
          </div>
          <div className="rs-display" style={{ fontSize: 24, marginTop: 15, lineHeight: 1.2 }}>Someone took that<br/>seat first</div>
          <div style={{ fontSize: 13, color: "var(--ink-3)", marginTop: 10, lineHeight: 1.6 }}>
            The last seat on Saman's 8:04 AM went while you were paying. Nothing was charged — your card wasn't touched.
          </div>
          <div style={{ marginTop: 15, padding: 14, borderRadius: 16, background: "var(--surface)", border: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 12 }}>
            <MatchRing value={RIDES[1].match} size={42} strokeWidth={3.4}/>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", color: "var(--ink-3)" }}>CLOSEST ALTERNATIVE</div>
              <div style={{ fontSize: 13.5, fontWeight: 700, marginTop: 2 }}>{RIDES[1].driver} · {RIDES[1].depart}</div>
              <div style={{ fontSize: 11.5, color: "var(--ink-3)" }}>2 seats free · {FARE_POLICY.currency} {money(RIDES[1].price)}</div>
            </div>
          </div>
          <button className="rs-btn accent full" style={{ marginTop: 16 }}>Book Kasun instead</button>
          <button className="rs-btn soft full" style={{ marginTop: 10 }}>See all 3 drivers</button>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, {
  PxSeatSelectScreen, PxCheckoutScreen, PxPaymentMethodsScreen,
  PxBookingStateScreen, PxSeatsTakenScreen,
});
