// driver-rates.jsx — D39/D40 · the per-km rate a driver may charge.
// A driver never types a price. ComiGo sets a MIN–MAX band per vehicle from four
// assessed factors, and the driver picks a rate inside it. These two screens are
// the only place that band is explained, so both show the arithmetic that
// produced it rather than announcing a number.

// ═══════════ D39 · YOUR PER-KM RATE ═══════════
// Read-only where it matters (the band) and editable where it doesn't (the rate
// inside it). The four factors are shown signed and totalled, because a driver
// who cannot see why his ceiling is LKR 58 assumes it was arbitrary.
function DvRateBandScreen({ picked = "mid" }) {
  const b = RATE_BAND;
  const span = b.max - b.min;
  const chosen = picked === "min" ? b.min : picked === "max" ? b.max : b.chosen;
  const pos = ((chosen - b.min) / span) * 100;
  const net = (r) => driverNet(fareAtRate(r));
  const stance = RATE_POSITIONS.find(p => p.key === picked) || RATE_POSITIONS[1];
  const totalDelta = b.factors.reduce((a, f) => a + f.delta, 0);
  return (
    <Phone label={`D39 Your per-km rate${picked === "mid" ? "" : ` · ${picked === "min" ? "bottom" : "top"} of band`}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Your per-km rate" sub={b.vehicle}/>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          <div className="rs-card" style={{ padding: 16 }}>
            <div className="rs-section-label" style={{ marginBottom: 4 }}>YOU CHARGE</div>
            <div style={{ display: "flex", alignItems: "baseline", gap: 7 }}>
              <div className="rs-display tab" style={{ fontSize: 40, lineHeight: 1.05, color: "var(--mode-drive-ink)" }}>{FARE_POLICY.currency} {chosen}</div>
              <div style={{ fontSize: 14, fontWeight: 700, color: "var(--ink-3)" }}>per km</div>
            </div>
            <div style={{ marginTop: 18, position: "relative", height: 10, borderRadius: 5, background: "var(--mode-drive-soft)", border: "1px solid var(--mode-drive)" }}>
              <div style={{ position: "absolute", top: -7, left: `${pos}%`, transform: "translateX(-50%)", width: 24, height: 24, borderRadius: 12, background: "var(--mode-drive)", border: "3px solid var(--surface)", boxShadow: "0 1px 4px rgba(0,0,0,.2)" }}/>
            </div>
            <div style={{ display: "flex", justifyContent: "space-between", marginTop: 12 }}>
              <div>
                <div className="tab" style={{ fontSize: 13, fontWeight: 800 }}>{b.min}</div>
                <div style={{ fontSize: 10.5, color: "var(--ink-3)", marginTop: 1 }}>Your floor</div>
              </div>
              <div style={{ textAlign: "right" }}>
                <div className="tab" style={{ fontSize: 13, fontWeight: 800 }}>{b.max}</div>
                <div style={{ fontSize: 10.5, color: "var(--ink-3)", marginTop: 1 }}>Your ceiling</div>
              </div>
            </div>
            <div style={{ height: 1, background: "var(--line)", margin: "14px 0 13px" }}/>
            <div style={{ fontSize: 12.5, color: "var(--ink-2)", lineHeight: 1.55, textWrap: "pretty" }}>
              Anywhere between {FARE_POLICY.currency} {b.min} and {b.max} is yours to choose, and you can change it between trips. The band itself is set by {b.setBy} — you can ask for it to be re-assessed, but you can't type a number outside it.
            </div>
          </div>

          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 4 }}>WHAT {FARE_POLICY.currency} {chosen} EARNS YOU</div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginBottom: 12 }}>A full-route seat on {NEXT_DRIVE.from} → {NEXT_DRIVE.to} · {NEXT_DRIVE_KM} km. This is the figure your trip publishes at.</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 9 }}>
              {[["Rider pays", fareAtRate(chosen), false], [`ComiGo fee · ${FARE_POLICY.commissionPct}%`, -(fareAtRate(chosen) - net(chosen)), true], ["You keep, per seat", net(chosen), false]].map(([l, v, fee], i) => (
                <div key={l} style={{ display: "flex", alignItems: "center", gap: 10, paddingTop: i === 2 ? 9 : 0, borderTop: i === 2 ? "1px solid var(--line)" : "none" }}>
                  <div style={{ flex: 1, fontSize: 13, fontWeight: i === 2 ? 800 : 600, color: fee ? "var(--ink-3)" : "var(--ink)" }}>{l}</div>
                  <div className="tab" style={{ fontSize: i === 2 ? 17 : 14, fontWeight: 800, color: fee ? "var(--status-rejected-ink)" : i === 2 ? "var(--mode-drive-ink)" : "var(--ink)" }}>
                    {v < 0 ? "−" : ""}{FARE_POLICY.currency} {money(Math.abs(v))}
                  </div>
                </div>
              ))}
            </div>
            <div style={{ marginTop: 13, padding: 12, borderRadius: 14, background: "var(--bg-soft)", display: "flex", flexDirection: "column", gap: 7 }}>
              {RATE_POSITIONS.map(p => {
                const r = p.key === "min" ? b.min : p.key === "max" ? b.max : b.chosen;
                const on = p.key === picked;
                return (
                  <div key={p.key} data-row={`rate ${p.key}`} style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    <div style={{ width: 16, height: 16, borderRadius: 8, flexShrink: 0, border: `2px solid ${on ? "var(--mode-drive)" : "var(--line-2)"}`, background: on ? "var(--mode-drive)" : "transparent" }}/>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 12.5, fontWeight: on ? 800 : 600 }}>{FARE_POLICY.currency} {r} · {p.label}</div>
                      <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 1, lineHeight: 1.4 }}>{p.rank}</div>
                    </div>
                    <div className="tab" style={{ fontSize: 12.5, fontWeight: 700, color: "var(--ink-2)", flexShrink: 0 }}>{FARE_POLICY.currency} {money(net(r))}</div>
                  </div>
                );
              })}
            </div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 11, lineHeight: 1.5 }}>
              A rider who only overlaps part of the route pays for the part she rides, so most seats sell for less than a full one. {stance.rank}. Demand at this rate: {stance.demand.toLowerCase()}. Results are ordered by route overlap first and price second — a higher rate never hides you, it just puts cheaper cars above you.
            </div>
          </div>

          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 4 }}>HOW YOUR BAND WAS SET</div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginBottom: 12 }}>{b.classLabel} class starts at {FARE_POLICY.currency} {b.classBand[0]}–{b.classBand[1]}. Four things about your car moved it.</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              {b.factors.map(f => (
                <div key={f.key} style={{ display: "flex", alignItems: "flex-start", gap: 11 }}>
                  <div style={{ width: 30, height: 30, borderRadius: 10, background: "var(--bg-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                    <Icon name={f.icon} size={15} color="var(--ink-2)"/>
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13, fontWeight: 700 }}>{f.label}</div>
                    <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.45 }}>{f.detail}</div>
                  </div>
                  <div className="tab" style={{ fontSize: 13, fontWeight: 800, flexShrink: 0, color: f.delta < 0 ? "var(--status-rejected-ink)" : "var(--status-approved-ink)" }}>
                    {f.delta > 0 ? "+" : "−"}{Math.abs(f.delta)}
                  </div>
                </div>
              ))}
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 10, marginTop: 13, paddingTop: 12, borderTop: "1px solid var(--line)" }}>
              <div style={{ flex: 1, fontSize: 12.5, fontWeight: 800 }}>Net effect on your ceiling</div>
              <div className="tab" style={{ fontSize: 14, fontWeight: 800, color: totalDelta < 0 ? "var(--status-rejected-ink)" : "var(--status-approved-ink)" }}>{totalDelta > 0 ? "+" : "−"}{Math.abs(totalDelta)}</div>
            </div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 9, lineHeight: 1.5 }}>Assessed by {b.setBy} on {b.setOn}. Renewing your insurance or servicing the car are the two factors you can actually move.</div>
          </div>

          <Banner kind="info" icon="help" title="Think your band is too low?"
            body={`Ask for one re-assessment when something about the car changes — a service, new tyres, a better policy. ${b.setBy} answers within ${RATE_REVIEW.slaDays} working days and the current band stays live meanwhile.`} action="Request review"/>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)", display: "flex", flexDirection: "column", gap: 9 }}>
          <button className="rs-btn full" style={{ background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Save {FARE_POLICY.currency} {chosen} per km</button>
          <button className="rs-btn soft full">All my vehicles</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D40 · A CAR WITH NO BAND YET ═══════════
// Papers approved is not the same as publishable: with no band there is no legal
// price to charge, so the trip cannot exist. The screen's whole job is to stop
// the driver believing this is a bug and to say when it clears.
function DvRatePendingScreen() {
  const v = PENDING_VEHICLE;
  const cls = vehicleClass(v.vClass);
  return (
    <Phone label="D40 Rate band · being set">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Per-km rate" sub={`${v.make} · ${v.plate}`}/>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          <div style={{ padding: 16, borderRadius: 18, background: "var(--status-pending-soft)", border: "1px solid var(--status-pending)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 11 }}>
              <div style={{ width: 44, height: 44, borderRadius: 15, background: "var(--status-pending)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                <Icon name="clock" size={21} color="var(--on-bright-fill)" strokeWidth={2.3}/>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em", color: "var(--status-pending-ink)" }}>SUBMITTED {v.submitted.toUpperCase()}</div>
                <div className="rs-display" style={{ fontSize: 21, marginTop: 3, lineHeight: 1.2 }}>ComiGo is setting your rate</div>
              </div>
            </div>
            <div style={{ fontSize: 12.5, color: "var(--ink-2)", marginTop: 12, lineHeight: 1.55, textWrap: "pretty" }}>
              Your papers for the {v.make} are approved. What's missing is the rate band — the floor and ceiling you may charge per km. Usually {v.reviewDays} working days; we'll notify you.
            </div>
          </div>

          <Banner kind="warn" icon="lock" title="This car can't publish yet"
            body={`Without a band there is no legal price to put on a seat, so the trip can't be created. Your ${MY_VEHICLE.make} is unaffected — keep publishing on that one.`} action="Switch car"/>

          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 4 }}>WHAT TO EXPECT</div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginBottom: 12 }}>Every {cls.label.toLowerCase()} starts from the same class band, then the four factors move it.</div>
            <div style={{ position: "relative", height: 10, borderRadius: 5, background: "var(--bg-soft)", border: "1px dashed var(--line-2)" }}/>
            <div style={{ display: "flex", justifyContent: "space-between", marginTop: 10 }}>
              <div className="tab" style={{ fontSize: 13, fontWeight: 800, color: "var(--ink-3)" }}>{FARE_POLICY.currency} {cls.band[0]}</div>
              <div style={{ fontSize: 11, fontWeight: 700, color: "var(--ink-3)" }}>{cls.label} class range</div>
              <div className="tab" style={{ fontSize: 13, fontWeight: 800, color: "var(--ink-3)" }}>{FARE_POLICY.currency} {cls.band[1]}</div>
            </div>
            <div style={{ height: 1, background: "var(--line)", margin: "14px 0 13px" }}/>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              {RATE_BAND.factors.map(f => (
                <div key={f.key} style={{ display: "flex", alignItems: "center", gap: 11 }}>
                  <div style={{ width: 30, height: 30, borderRadius: 10, background: "var(--bg-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                    <Icon name={f.icon} size={15} color="var(--ink-3)"/>
                  </div>
                  <div style={{ flex: 1, minWidth: 0, fontSize: 13, fontWeight: 700, color: "var(--ink-2)" }}>{f.label}</div>
                  <StatusBadge status="pending" label="ASSESSING"/>
                </div>
              ))}
            </div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 12, lineHeight: 1.5 }}>
              A {v.year} car with the papers you've given us usually lands mid-range. Nothing is decided until all four are assessed.
            </div>
          </div>

          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 11 }}>YOUR OTHER CAR IS READY</div>
            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <div style={{ width: 40, height: 40, borderRadius: 13, background: "var(--mode-drive-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                <Icon name="car" size={19} color="var(--mode-drive-ink)"/>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 13.5, fontWeight: 700 }}>{MY_VEHICLE.make}</div>
                <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>{FARE_POLICY.currency} {RATE_BAND.min}–{RATE_BAND.max} per km · charging {RATE_BAND.chosen}</div>
              </div>
              <StatusBadge status="approved" label="ACTIVE"/>
            </div>
          </div>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)", display: "flex", flexDirection: "column", gap: 9 }}>
          <button className="rs-btn full" style={{ background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Notify me when it's set</button>
          <button className="rs-btn soft full">Message support</button>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, { DvRateBandScreen, DvRatePendingScreen });
