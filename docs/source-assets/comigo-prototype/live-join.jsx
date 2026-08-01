// live-join.jsx — the SECOND kind of booking request: joining a trip that is
// already on the road. P36/P37 are the rider's half, D16b/D16c the driver's.
//
// Why this is not a variant of the scheduled request:
//   • The driver is at the wheel. His screen has to be readable in one glance
//     and answerable with one thumb — no scrolling, no fare breakdown, no
//     paragraph of policy. Everything he needs is above the fold.
//   • There is no later "start" to charge at, so the card is captured the moment
//     he accepts. The rider must be told that before she asks, not after.
//   • A seat is only offerable while the driver is still BEHIND the rider's
//     pickup point. Once he passes it the request is void — enforced on the
//     server, never shown as a warning the driver has to think about.

// ═══════════ P36 · RIDING NOW · trips already moving ═══════════
function PxLiveResultsScreen({ empty = false }) {
  return (
    <Phone label={empty ? "P36b Riding now · all passed you" : "P36 Riding now · joinable trips"}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ background: "var(--surface)", borderBottom: "1px solid var(--line)", flexShrink: 0 }}>
          <div style={{ padding: "10px 16px", display: "flex", alignItems: "center", gap: 10 }}>
            <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label="Back" data-back="1"><Icon name="back" size={20}/></button>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 700, display: "flex", alignItems: "center", gap: 6, whiteSpace: "nowrap", overflow: "hidden" }}>
                Kirulapone <Icon name="arrow" size={12} color="var(--ink-3)"/> Colombo Fort
              </div>
              <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 1 }}>Leaving now · 1 seat</div>
            </div>
            <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label="Edit search"><Icon name="settings" size={18}/></button>
          </div>
          {/* The two request types are a real fork in the product, so they are a
              real fork in the UI — a tab, not a filter chip buried in a row. */}
          <div style={{ padding: "0 16px 11px", display: "flex", gap: 7 }}>
            <button className="rs-tap" style={{ flex: 1 }}><span className="rs-chip" style={{ width: "100%", justifyContent: "center", height: 44, fontSize: 13 }}><Icon name="clock" size={13}/> Scheduled</span></button>
            <button className="rs-tap" style={{ flex: 1 }}><span className="rs-chip accent" style={{ width: "100%", justifyContent: "center", height: 44, fontSize: 13 }}><span style={{ width: 8, height: 8, borderRadius: 4, background: "var(--accent-ink)", animation: "pulse 1.8s infinite" }}/> On the road now</span></button>
          </div>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "12px 16px 16px", display: "flex", flexDirection: "column", gap: 11 }} className="rs-scroll">
          {!empty && (
            <>
              <div style={{ fontSize: 12.5, color: "var(--ink-3)" }}>
                <b style={{ color: "var(--ink)" }}>{LIVE_TRIPS.length} drivers</b> are mid-trip with a free seat and haven't reached you yet.
              </div>
              {LIVE_TRIPS.map(t => <LiveTripCard key={t.id} t={t}/>)}
            </>
          )}

          {empty && (
            <div style={{ padding: "34px 18px", textAlign: "center" }}>
              <div style={{ width: 60, height: 60, borderRadius: 30, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="car" size={27} color="var(--ink-3)"/>
              </div>
              <div className="rs-display" style={{ fontSize: 21, marginTop: 14, lineHeight: 1.2 }}>Everyone's already past you</div>
              <div style={{ fontSize: 13, color: "var(--ink-3)", marginTop: 9, lineHeight: 1.6, textWrap: "pretty" }}>
                {LIVE_PASSED_COUNT} drivers are on your route right now, but all of them have driven through Kirulapone already. Turning back for you isn't a shared trip — it's a taxi.
              </div>
              <button className="rs-btn accent full" style={{ marginTop: 18 }}>See scheduled trips instead</button>
              <button className="rs-btn ghost full" style={{ marginTop: 10 }}>Alert me when a seat frees up</button>
            </div>
          )}

          {!empty && (
            <>
              <div style={{ padding: 14, borderRadius: 16, background: "var(--bg-soft)", border: "1px dashed var(--line-2)", display: "flex", gap: 11 }}>
                <Icon name="pin" size={17} color="var(--ink-3)"/>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 12.5, fontWeight: 700 }}>{LIVE_PASSED_COUNT} more drivers have already passed Kirulapone</div>
                  <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.45 }}>They'd have to double back, so we don't offer them. Only drivers still behind your pickup show here.</div>
                </div>
              </div>
              <Banner kind="warn" icon="card" title="These are charged the moment he accepts"
                body="A moving trip has no start to wait for. If he takes your request the fare leaves your card straight away — that's the one way this differs from booking ahead."/>
            </>
          )}
        </div>
        <TabBar mode="ride" active="action" badges={{ inbox: 3 }}/>
      </div>
    </Phone>
  );
}

// A live card leads with TIME, not match percentage. When a car is already
// moving, "3 minutes away" is the only number that decides anything.
function LiveTripCard({ t }) {
  return (
    <div className="rs-card" style={{ padding: 14, border: "1.5px solid var(--accent-ink)" }}>
      <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
        <div style={{ width: 52, flexShrink: 0, textAlign: "center" }}>
          <div className="rs-display tab" style={{ fontSize: 25, lineHeight: 1, color: "var(--accent-ink)" }}>{t.pickupIn}</div>
          <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".06em", color: "var(--ink-3)", marginTop: 3 }}>MIN AWAY</div>
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
            <div style={{ fontSize: 14, fontWeight: 700, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{t.driver}</div>
            <Icon name="star" size={12} color="var(--status-pending-ink)"/>
            <span className="tab" style={{ fontSize: 12, fontWeight: 600 }}>{t.rating}</span>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: 6, marginTop: 3 }}>
            <span style={{ width: 7, height: 7, borderRadius: 4, background: "var(--teal)", animation: "pulse 1.8s infinite", flexShrink: 0 }}/>
            <div style={{ fontSize: 11.5, color: "var(--ink-2)", fontWeight: 600, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>At {t.now}, {t.aheadKm} km before you</div>
          </div>
          <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>Free seat because {t.freedBy}</div>
        </div>
        <div style={{ textAlign: "right", flexShrink: 0 }}>
          <div className="rs-display tab" style={{ fontSize: 19, fontWeight: 600 }}>{FARE_POLICY.currency} {money(t.price)}</div>
          <div className="tab" style={{ fontSize: 10.5, color: "var(--ink-3)", marginTop: 2 }}>{FARE_POLICY.currency} {money(t.ratePerKm)}/km</div>
        </div>
      </div>
      <div style={{ display: "flex", alignItems: "center", gap: 7, rowGap: 7, flexWrap: "wrap", marginTop: 12, paddingTop: 11, borderTop: "1px solid var(--line)" }}>
        <span className="rs-chip" style={{ height: 26 }}><Icon name="users" size={12}/> {t.seats} free</span>
        <span className="rs-chip" style={{ height: 26 }}><Icon name="clock" size={12}/> {t.to} by {t.arrive}</span>
        <span className="rs-chip" style={{ height: 26, whiteSpace: "nowrap", overflow: "hidden" }}>{t.car.split(" · ")[0]}</span>
      </div>
      <button className="rs-btn accent full" style={{ marginTop: 11 }}>Ask {t.driver.split(" ")[0]} for the seat</button>
    </div>
  );
}

// ═══════════ P37 · ASKED A MOVING DRIVER ═══════════
// A countdown, because he is driving and will answer in seconds or not at all.
// The card-capture rule is restated here, plainly, at the moment of commitment.
function PxLiveRequestScreen({ state = "waiting" }) {
  const t = LIVE_TRIPS[0];
  const cfg = {
    waiting: {
      num: "P37", tag: "waiting on the driver", tint: "var(--status-pending-soft)", ink: "var(--status-pending-ink)",
      kicker: "ASKED · HE'S DRIVING", title: `${t.driver.split(" ")[0]} is deciding`,
      lead: `He gets one prompt at the wheel and about 45 seconds to answer. If he doesn't, the request lapses on its own and you've lost nothing.`,
    },
    accepted: {
      num: "P37b", tag: "accepted and charged", tint: "var(--status-approved-soft)", ink: "var(--status-approved-ink)",
      kicker: "ACCEPTED · SEAT IS YOURS", title: `Be at ${t.now} in ${t.pickupIn} minutes`,
      lead: `${FARE_POLICY.currency} ${money(t.price)} has left your card. There's no trip start to wait for on a moving car, so acceptance is the charge.`,
    },
    lapsed: {
      num: "P37c", tag: "he never answered", tint: "var(--status-rejected-soft)", ink: "var(--status-rejected-ink)",
      kicker: "NO ANSWER", title: "He drove on",
      lead: `${t.driver.split(" ")[0]} didn't answer in time, which usually means traffic rather than a no. Nothing was charged and nothing is recorded against either of you.`,
    },
  }[state];
  return (
    <Phone label={`${cfg.num} Live request · ${cfg.tag}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Seat on a moving trip" sub={`${t.driver} · ${t.now} → ${t.to}`}/>
        <div style={{ flex: 1, overflow: "auto", padding: "20px 16px 16px" }} className="rs-scroll">
          <div style={{ display: "flex", alignItems: "center", gap: 13 }}>
            {state === "waiting" ? (
              <div style={{ width: 56, height: 56, borderRadius: 28, background: cfg.tint, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0, position: "relative" }}>
                <div style={{ position: "absolute", inset: 0, borderRadius: 28, border: "3px solid var(--status-pending)", opacity: .4, animation: "pulse 1.8s infinite" }}/>
                <Icon name="clock" size={25} color={cfg.ink} strokeWidth={2.2}/>
              </div>
            ) : (
              <div style={{ width: 56, height: 56, borderRadius: 19, background: cfg.tint, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                <Icon name={state === "accepted" ? "check" : "close"} size={26} color={cfg.ink} strokeWidth={2.4}/>
              </div>
            )}
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em", color: cfg.ink }}>{cfg.kicker}</div>
              <div className="rs-display" style={{ fontSize: 24, marginTop: 3, lineHeight: 1.15 }}>{cfg.title}</div>
            </div>
          </div>
          <div style={{ fontSize: 13.5, color: "var(--ink-3)", marginTop: 12, lineHeight: 1.6, textWrap: "pretty" }}>{cfg.lead}</div>

          <div style={{ marginTop: 15, padding: 15, borderRadius: 18, background: "var(--surface)", border: "1px solid var(--line)" }}>
            <div className="rs-section-label" style={{ marginBottom: 11 }}>WHERE HE IS</div>
            <RouteTimeline compact stops={[
              { kind: "via", place: `${t.now} — his car, right now`, note: `${t.aheadKm} km before your pickup` },
              { kind: "pickup", place: "Kirulapone junction", time: t.pickupEta, note: state === "accepted" ? "Wait on the Havelock Road side" : "Your pickup, still ahead of him" },
              { kind: "drop", place: t.to, time: t.arrive, note: `${t.dist} km · ${FARE_POLICY.currency} ${money(t.ratePerKm)} per km` },
            ]}/>
          </div>

          {state === "waiting" && (
            <div style={{ marginTop: 12 }}>
              <Banner kind="warn" icon="card" title={`${FARE_POLICY.currency} ${money(t.price)} is charged if he says yes`}
                body="Not held, not authorised — taken. A moving trip has already started, so there is no start event left to charge at. Nothing happens if he declines."/>
            </div>
          )}
          {state === "accepted" && (
            <div style={{ marginTop: 12, display: "flex", flexDirection: "column", gap: 11 }}>
              <div style={{ display: "flex", gap: 9 }}>
                <button className="rs-btn soft" style={{ flex: 1, height: 48, fontSize: 12.5 }}><Icon name="phone" size={16}/> Call</button>
                <button className="rs-btn soft" style={{ flex: 1, height: 48, fontSize: 12.5 }}><Icon name="chat" size={16}/> Chat</button>
                <button className="rs-btn soft" style={{ flex: 1, height: 48, fontSize: 12.5 }}><Icon name="share" size={16}/> Share</button>
              </div>
              <Banner kind="info" icon="clock" title={`He waits ${POLICY.pickupWaitMin} minutes at the kerb`}
                body={`There are ${t.seats === 1 ? "three" : "two"} people already in the car, so this one can't wait long. Miss it and the ${POLICY.noShowPenaltyPct}% no-show fee applies as usual.`}/>
            </div>
          )}
          {state === "lapsed" && (
            <div style={{ marginTop: 12, display: "flex", flexDirection: "column", gap: 11 }}>
              <Banner kind="good" icon="card" title="Nothing was charged" body="We only take the fare on an accept. A lapsed request costs nothing and doesn't count as a cancellation."/>
              <div style={{ padding: 15, borderRadius: 18, background: "var(--surface)", border: "1px solid var(--line)" }}>
                <div className="rs-section-label" style={{ marginBottom: 11 }}>STILL BEHIND YOU</div>
                <LiveTripCard t={LIVE_TRIPS[1]}/>
              </div>
            </div>
          )}
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          {state === "waiting" && <><button className="rs-btn accent full">Keep waiting</button><button className="rs-btn ghost full" style={{ marginTop: 10 }}>Withdraw the request</button></>}
          {state === "accepted" && <button className="rs-btn accent full">Track his car</button>}
          {state === "lapsed" && <button className="rs-btn accent full">Ask {LIVE_TRIPS[1].driver.split(" ")[0]} instead</button>}
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D16b · LIVE REQUEST AT THE WHEEL ═══════════
// Designed for a person who is driving. One screenful, no scroll, two targets
// the size of a palm, and the three facts that decide it: what he keeps, how far
// off his line it takes him, and whether the pickup is still ahead.
function DvLiveRequestScreen() {
  const q = LIVE_REQUEST;
  return (
    <Phone label="D16b Live request · while driving" statusDark statusBg="#12100f" navBg="#12100f">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "#12100f", color: "#f4ece0" }}>
        <div style={{ padding: "12px 16px 0", display: "flex", alignItems: "center", gap: 10, flexShrink: 0 }}>
          <span style={{ width: 9, height: 9, borderRadius: 5, background: "#e8834f", animation: "pulse 1.8s infinite" }}/>
          <div style={{ fontSize: 10.5, fontWeight: 800, letterSpacing: ".12em", opacity: .75, flex: 1 }}>SEAT REQUEST · YOU'RE DRIVING</div>
          <div className="tab" style={{ fontSize: 13, fontWeight: 800, color: "#e8834f" }}>{q.expiresSec}s</div>
        </div>
        <div style={{ height: 4, background: "rgba(255,255,255,.12)", margin: "10px 16px 0", borderRadius: 2, flexShrink: 0 }}>
          <div style={{ width: "62%", height: "100%", background: "#e8834f", borderRadius: 2 }}/>
        </div>

        <div style={{ flex: 1, padding: "16px 16px 0", display: "flex", flexDirection: "column", gap: 13, minHeight: 0 }}>
          {/* What he keeps, at the size a glance can read. */}
          <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div className="rs-display tab" style={{ fontSize: 44, lineHeight: 1, color: "#f4ece0" }}>{FARE_POLICY.currency} {money(q.net)}</div>
              <div style={{ fontSize: 12, opacity: .65, marginTop: 5 }}>you keep · {FARE_POLICY.currency} {money(q.fare)} fare</div>
            </div>
            <div style={{ textAlign: "right", flexShrink: 0 }}>
              <div className="rs-display tab" style={{ fontSize: 22, lineHeight: 1, color: "#48a89f" }}>+{q.addedMin} min</div>
              <div style={{ fontSize: 11, opacity: .6, marginTop: 4 }}>+{q.addedKm} km detour</div>
            </div>
          </div>

          <div style={{ padding: 14, borderRadius: 18, background: "rgba(255,255,255,.07)", border: "1px solid rgba(255,255,255,.1)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 11 }}>
              <Avatar name={q.passenger} size={42}/>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                  <div style={{ fontSize: 15, fontWeight: 800 }}>{q.passenger}</div>
                  {q.verified && <Icon name="badge" size={15} color="#48a89f"/>}
                </div>
                <div style={{ fontSize: 11.5, opacity: .65, marginTop: 2 }}>{q.rating}★ · {q.rides} rides · {q.seats} × {q.seat.toLowerCase()}</div>
              </div>
            </div>
            <div style={{ height: 1, background: "rgba(255,255,255,.1)", margin: "12px 0" }}/>
            <div style={{ display: "flex", gap: 11 }}>
              <div style={{ width: 12, display: "flex", flexDirection: "column", alignItems: "center", paddingTop: 5, flexShrink: 0 }}>
                <div style={{ width: 10, height: 10, borderRadius: 5, background: "#48a89f" }}/>
                <div style={{ flex: 1, width: 2, background: "rgba(255,255,255,.18)", margin: "4px 0" }}/>
                <div style={{ width: 10, height: 10, background: "#e8834f" }}/>
              </div>
              <div style={{ flex: 1, minWidth: 0, display: "flex", flexDirection: "column", gap: 11 }}>
                <div>
                  <div style={{ fontSize: 14, fontWeight: 700 }}>{q.from}</div>
                  <div style={{ fontSize: 11.5, color: "#48a89f", fontWeight: 700, marginTop: 2 }}>{q.aheadKm} km ahead of you · {q.aheadMin} min</div>
                </div>
                <div>
                  <div style={{ fontSize: 14, fontWeight: 700 }}>{q.to}</div>
                  <div style={{ fontSize: 11.5, opacity: .6, marginTop: 2 }}>Where you're already going</div>
                </div>
              </div>
            </div>
          </div>

          <div style={{ display: "flex", gap: 9 }}>
            {[[`${q.onBoard} on board`, "users"], [`${q.seatsFreeAfter} free after`, "car"], ["Verified rider", "badge"]].map(([l, ic]) => (
              <div key={l} style={{ flex: 1, padding: "10px 8px", borderRadius: 13, background: "rgba(255,255,255,.06)", display: "flex", flexDirection: "column", alignItems: "center", gap: 5 }}>
                <Icon name={ic} size={15} color="rgba(244,236,224,.7)"/>
                <div style={{ fontSize: 10.5, fontWeight: 700, opacity: .8, textAlign: "center", lineHeight: 1.25 }}>{l}</div>
              </div>
            ))}
          </div>
          <div style={{ flex: 1 }}/>
        </div>

        <div style={{ padding: "12px 16px 18px", flexShrink: 0, display: "flex", flexDirection: "column", gap: 10 }}>
          <button className="rs-btn full" style={{ height: 64, fontSize: 16, background: "#48a89f", color: "#0c1615" }}>Accept · {FARE_POLICY.currency} {money(q.net)}</button>
          <div style={{ display: "flex", gap: 10 }}>
            <button className="rs-btn full" style={{ flex: 1, height: 52, fontSize: 14, background: "rgba(255,255,255,.1)", color: "#f4ece0" }}>Decline</button>
            <button className="rs-btn full" style={{ flex: 1, height: 52, fontSize: 14, background: "rgba(255,255,255,.1)", color: "#f4ece0" }}>Mute for this trip</button>
          </div>
          <div style={{ fontSize: 10.5, opacity: .5, textAlign: "center", lineHeight: 1.45 }}>
            Declining costs you nothing. Her card is charged only when you accept.
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D16c · REQUEST LAPSED — HE'S PAST THE PICKUP ═══════════
// He sees this AFTER the fact, parked at a light, so it can be a normal screen.
// The point is to explain why the money he half-saw disappeared, so he doesn't
// go looking for a bug or start driving to Kirulapone to chase it.
function DvLiveRequestLapsedScreen() {
  const q = LIVE_REQUEST;
  return (
    <Phone label="D16c Live request · lapsed, pickup passed">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Seat request" sub={`${q.passenger} · ${q.from} → ${q.to}`}/>
        <div style={{ flex: 1, overflow: "auto", padding: "20px 16px 16px" }} className="rs-scroll">
          <div style={{ display: "flex", alignItems: "center", gap: 13 }}>
            <div style={{ width: 56, height: 56, borderRadius: 19, background: "var(--bg-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
              <Icon name="pin" size={26} color="var(--ink-3)" strokeWidth={2.2}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em", color: "var(--ink-3)" }}>EXPIRED</div>
              <div className="rs-display" style={{ fontSize: 24, marginTop: 3, lineHeight: 1.15 }}>You've passed {q.from}</div>
            </div>
          </div>
          <div style={{ fontSize: 13.5, color: "var(--ink-3)", marginTop: 12, lineHeight: 1.6, textWrap: "pretty" }}>
            {q.passenger.split(" ")[0]}'s pickup went behind you while the request was open, so ComiGo withdrew it. Going back for her would be a fare, not a shared seat — and she'd be charged for a detour she didn't ask for.
          </div>

          <div className="rs-card" style={{ padding: 15, marginTop: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 12 }}>HOW LIVE REQUESTS WORK</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              <RuleRow icon="pin" title="Only riders still ahead of you can ask"
                body="ComiGo checks your position against their pickup point before the request ever reaches your screen. You'll never be offered a seat you'd have to turn around for."/>
              <RuleRow icon="clock" title="You get about 45 seconds"
                body="One prompt, one glance. Miss it and it lapses quietly — no penalty, no effect on your rating, nothing recorded."/>
              <RuleRow icon="card" title="Accepting charges her immediately"
                body="There's no trip start left to charge at, so the fare moves on your accept. That's why the prompt shows what you keep, not the fare."/>
              <RuleRow icon="settings" title="You can turn them off"
                body="Live requests are opt-out per trip and in your driving preferences. Some routes aren't worth the interruption."/>
            </div>
          </div>

          <div style={{ marginTop: 12 }}>
            <Banner kind="info" icon="users" title={`${LIVE_REQUEST.seatsFreeAfter === 0 ? "Your last seat is still free" : "Seats are still free"} to the end of this route`}
              body="Anyone still behind you on the way to Colombo Fort can take it. We keep offering it until you arrive."/>
          </div>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn full" style={{ background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Back to the trip</button>
          <button className="rs-btn ghost full" style={{ marginTop: 10 }}>Live request settings</button>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, {
  PxLiveResultsScreen, LiveTripCard, PxLiveRequestScreen,
  DvLiveRequestScreen, DvLiveRequestLapsedScreen,
});
