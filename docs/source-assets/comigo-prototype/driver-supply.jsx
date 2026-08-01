// driver-supply.jsx — D08…D17: dashboard (two variations), trip list, recurring
// management, the create-trip wizard, published + QR, and booking requests.

// ═══════════ D08 · DASHBOARD · earnings-first ═══════════
// `live` is the mirror of P01b: a trip is on the road, so the dashboard leads
// with getting back into it rather than with the next departure.
function DvHomeScreen({ live = false }) {
  return (
    <Phone label={live ? "D08b Dashboard · trip running" : "D08 Dashboard · earnings-first"}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <HomeHeader mode="drive" state="approved"/>
        <div style={{ flex: 1, overflow: "auto", padding: "0 16px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          <div style={{ padding: 18, borderRadius: 22, background: "var(--ink-fill)", color: "var(--on-ink-fill)" }}>
            <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em", opacity: .65 }}>EARNED TODAY</div>
            <div className="rs-display tab" style={{ fontSize: 40, lineHeight: 1.05, marginTop: 4 }}>{FARE_POLICY.currency} {money(earnedToday())}</div>
            <div style={{ display: "flex", gap: 20, marginTop: 14 }}>
              {[["This week", `${FARE_POLICY.currency} ${money(DRIVER_TODAY.weekTotal)}`], ["Rating", DRIVER_TODAY.rating], ["Accepted", `${DRIVER_TODAY.acceptance}%`]].map(([l, v]) => (
                <div key={l}>
                  <div style={{ fontSize: 10.5, opacity: .6 }}>{l}</div>
                  <div className="tab" style={{ fontSize: 14, fontWeight: 700, marginTop: 2 }}>{v}</div>
                </div>
              ))}
            </div>
          </div>

          {live && (
            <button data-row="resume the trip" style={{ textAlign: "left", padding: 15, borderRadius: 20, background: "var(--mode-drive)", color: "var(--on-bright-fill)", display: "flex", alignItems: "center", gap: 12, minHeight: 44 }}>
              <div style={{ width: 40, height: 40, borderRadius: 13, background: "rgba(255,255,255,.2)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                <Icon name="car" size={19} color="var(--on-bright-fill)"/>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", opacity: .8 }}>ON THE ROAD NOW</div>
                <div style={{ fontSize: 14.5, fontWeight: 800, marginTop: 2 }}>{LIVE_DRIVE.from} → {LIVE_DRIVE.to}</div>
                <div style={{ fontSize: 11.5, opacity: .85, marginTop: 2 }}>{LIVE_DRIVE.onBoard} on board · next drop {LIVE_DRIVE.nextDrop.place}, {LIVE_DRIVE.nextDrop.km} km</div>
              </div>
              <Icon name="chev" size={18} color="var(--on-bright-fill)"/>
            </button>
          )}

          <div className="rs-card" style={{ padding: 15, border: "1.5px solid var(--mode-drive)", opacity: live ? .55 : 1 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 9 }}>
              <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", color: "var(--mode-drive-ink)", flex: 1 }}>{live ? "AFTER THIS ONE" : `NEXT TRIP · ${NEXT_DRIVE.depart} · IN ${NEXT_DRIVE.inMin} MIN`}</div>
              <StatusBadge status="approved" label="PUBLISHED"/>
            </div>
            <div style={{ fontSize: 16, fontWeight: 800, marginTop: 8 }}>{NEXT_DRIVE.from} → {NEXT_DRIVE.to}</div>
            <div style={{ display: "flex", alignItems: "center", gap: 12, marginTop: 11 }}>
              <div style={{ display: "flex", gap: 4 }}>
                {Array.from({ length: NEXT_DRIVE.seatsTotal }, (_, i) => (
                  <div key={i} style={{ width: 18, height: 22, borderRadius: "5px 5px 3px 3px", background: i < NEXT_DRIVE.seatsBooked ? "var(--mode-drive)" : "var(--bg-soft)", border: `1.5px solid ${i < NEXT_DRIVE.seatsBooked ? "var(--mode-drive)" : "var(--line-2)"}` }}/>
                ))}
              </div>
              <div style={{ fontSize: 12, color: "var(--ink-3)", flex: 1 }}>{NEXT_DRIVE.seatsBooked} of {NEXT_DRIVE.seatsTotal} booked</div>
              <div style={{ textAlign: "right" }}>
                <div className="tab" style={{ fontSize: 15, fontWeight: 800 }}>{FARE_POLICY.currency} {money(NEXT_DRIVE.netExpected)}</div>
                <div style={{ fontSize: 10, color: "var(--ink-3)", marginTop: 1 }}>you keep</div>
              </div>
            </div>
            <button className="rs-btn full" style={{ marginTop: 13, background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Start trip</button>
            <div style={{ textAlign: "center", fontSize: 10.5, color: "var(--ink-3)", marginTop: 8, lineHeight: 1.45 }}>
              Starting charges {NEXT_DRIVE.seatsBooked} cards · start within {POLICY.startBufferMin} min of {NEXT_DRIVE.depart}
            </div>
          </div>

          <div style={{ display: "flex", gap: 10 }}>
            <button style={{ flex: 1, padding: 14, borderRadius: 16, background: "var(--surface)", border: "1px solid var(--line)", display: "flex", flexDirection: "column", alignItems: "flex-start", gap: 6, position: "relative" }}>
              <Icon name="users" size={19} color="var(--mode-drive-ink)"/>
              <div style={{ fontSize: 13, fontWeight: 700 }}>Requests</div>
              <div style={{ fontSize: 11, color: "var(--ink-3)" }}>{NEXT_DRIVE.requests} waiting</div>
              <div style={{ position: "absolute", top: 12, right: 12, width: 18, height: 18, borderRadius: 9, background: "var(--danger)", color: "var(--on-bright-fill)", fontSize: 10.5, fontWeight: 800, display: "flex", alignItems: "center", justifyContent: "center" }}>{NEXT_DRIVE.requests}</div>
            </button>
            <button style={{ flex: 1, padding: 14, borderRadius: 16, background: "var(--surface)", border: "1px solid var(--line)", display: "flex", flexDirection: "column", alignItems: "flex-start", gap: 6 }}>
              <Icon name="calendar" size={19} color="var(--ink-2)"/>
              <div style={{ fontSize: 13, fontWeight: 700 }}>Recurring</div>
              <div style={{ fontSize: 11, color: "var(--ink-3)" }}>{NEXT_DRIVE.recurring} active</div>
            </button>
          </div>

          <Banner kind="warn" icon="shield" title={`Insurance expires in ${MY_VEHICLE.insuranceDaysLeft} days`}
            body="Renew before 15 Aug to keep publishing. Trips already booked are unaffected." action="Renew"/>
        </div>
        <TabBar mode="drive" active="home" badges={{ trips: NEXT_DRIVE.requests, inbox: 2 }}/>
      </div>
    </Phone>
  );
}

// ═══════════ D09 · MY TRIPS (published supply — not history) ═══════════
function DvTripListScreen() {
  const trips = [
    { when: `Today · ${NEXT_DRIVE.depart}`, from: NEXT_DRIVE.from, to: NEXT_DRIVE.to, booked: NEXT_DRIVE.seatsBooked, total: NEXT_DRIVE.seatsTotal, amt: NEXT_DRIVE.netExpected, st: "published", req: NEXT_DRIVE.requests },
    { when: "Today · 5:30 PM", from: "Colombo Fort", to: "Nugegoda", booked: 1, total: 3, amt: SEAT_NET, st: "published" },
    { when: "Tomorrow · 7:45 AM", from: "Nugegoda", to: "Colombo Fort", booked: 0, total: 3, amt: 0, st: "published" },
    { when: "Sat 27 Jul · 9:00 AM", from: "Nugegoda", to: "Negombo", booked: 0, total: 4, amt: 0, st: "draft" },
  ];
  return (
    <Phone label="D09 My trips">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Your trips" sub="Published and upcoming"/>
        <div style={{ padding: "12px 16px" }}>
          <Segmented options={["Upcoming", "Recurring", "Past"]} value="Upcoming" tint="var(--mode-drive)" fg="var(--on-bright-fill)"/>
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: "0 16px 16px", display: "flex", flexDirection: "column", gap: 10 }} className="rs-scroll">
          <Banner kind="info" icon="lock" title="A trip freezes when its first seat is booked"
            body={`Until then, edit it freely. After that, cancelling is the only change — free until ${POLICY.driverCancelFreeHours} hours before departure.`}/>
          {trips.map(t => (
            <div key={t.when} className="rs-card" style={{ padding: 14, opacity: t.st === "draft" ? .75 : 1 }}>
              <div style={{ display: "flex", alignItems: "center", gap: 9 }}>
                <div className="tab" style={{ fontSize: 11, fontWeight: 800, color: "var(--ink-3)", flex: 1 }}>{t.when.toUpperCase()}</div>
                {t.st === "draft" ? <StatusBadge status="none" label="DRAFT"/> : <StatusBadge status="approved" label="PUBLISHED"/>}
              </div>
              <div style={{ fontSize: 14.5, fontWeight: 700, marginTop: 7 }}>{t.from} → {t.to}</div>
              <div style={{ display: "flex", alignItems: "center", gap: 10, marginTop: 10 }}>
                <div style={{ display: "flex", gap: 3 }}>
                  {Array.from({ length: t.total }, (_, i) => (
                    <div key={i} style={{ width: 15, height: 19, borderRadius: "4px 4px 2px 2px", background: i < t.booked ? "var(--mode-drive)" : "var(--bg-soft)", border: `1.5px solid ${i < t.booked ? "var(--mode-drive)" : "var(--line-2)"}` }}/>
                  ))}
                </div>
                <div style={{ fontSize: 11.5, color: "var(--ink-3)", flex: 1 }}>{t.booked} of {t.total} booked</div>
                {t.amt > 0 && <div style={{ textAlign: "right" }}><div className="tab" style={{ fontSize: 13.5, fontWeight: 800 }}>{FARE_POLICY.currency} {money(t.amt)}</div><div style={{ fontSize: 10, color: "var(--ink-3)" }}>you keep</div></div>}
              </div>
              {t.st === "published" && t.booked === 0 && (
                <div style={{ marginTop: 11, paddingTop: 11, borderTop: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 9 }}>
                  <Icon name="settings" size={15} color="var(--ink-3)"/>
                  <div style={{ fontSize: 11.5, color: "var(--ink-3)", flex: 1, lineHeight: 1.4 }}>Nobody has booked yet — still editable</div>
                  <button className="rs-btn soft" style={{ padding: "0 16px", height: 40, fontSize: 12, flexShrink: 0 }}>Edit</button>
                </div>
              )}
              {t.req && (
                <div style={{ marginTop: 11, paddingTop: 11, borderTop: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 9 }}>
                  <div style={{ width: 8, height: 8, borderRadius: 4, background: "var(--danger)", flexShrink: 0 }}/>
                  <div style={{ fontSize: 12, fontWeight: 700, color: "var(--status-rejected-ink)", flex: 1 }}>{t.req} seat request waiting</div>
                  <div className="rs-btn" style={{ padding: "0 16px", fontSize: 12, background: "var(--mode-drive)", color: "var(--on-bright-fill)", flexShrink: 0 }}>Review</div>
                </div>
              )}
            </div>
          ))}
        </div>
        <TabBar mode="drive" active="trips" badges={{ trips: NEXT_DRIVE.requests, inbox: 2 }}/>
      </div>
    </Phone>
  );
}

// ═══════════ D10 · RECURRING MANAGEMENT ═══════════
function DvRecurringScreen() {
  const routes = [
    { name: `${NEXT_DRIVE.from} → ${NEXT_DRIVE.to}`, days: "Mon–Fri", time: "7:45 AM", seats: 3, st: "active", next: "12 occurrences generated to 8 Aug" },
    { name: "Colombo Fort → Nugegoda", days: "Mon–Fri", time: "5:30 PM", seats: 3, st: "active", next: "12 occurrences generated to 8 Aug" },
    { name: "Nugegoda → Negombo", days: "Sat", time: "9:00 AM", seats: 4, st: "paused", next: "Paused since 19 Jul" },
  ];
  return (
    <Phone label="D10 Recurring routes">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Recurring routes"/>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 11 }} className="rs-scroll">
          <Banner kind="info" icon="calendar" title="Templates, not bookings"
            body="A recurring route generates individual trips ahead of time. Pausing stops new ones — it never cancels a trip someone has already booked."/>
          {routes.map(r => (
            <div key={r.name} className="rs-card" style={{ padding: 15, opacity: r.st === "paused" ? .78 : 1 }}>
              <div style={{ display: "flex", alignItems: "center", gap: 9 }}>
                <div style={{ fontSize: 14.5, fontWeight: 700, flex: 1, minWidth: 0 }}>{r.name}</div>
                {r.st === "paused" ? <StatusBadge status="none" label="PAUSED"/> : <StatusBadge status="approved" label="ACTIVE"/>}
              </div>
              <div style={{ display: "flex", gap: 7, marginTop: 10, flexWrap: "wrap" }}>
                <span className="rs-chip" style={{ height: 26 }}><Icon name="calendar" size={12}/> {r.days}</span>
                <span className="rs-chip" style={{ height: 26 }}><Icon name="clock" size={12}/> {r.time}</span>
                <span className="rs-chip" style={{ height: 26 }}><Icon name="users" size={12}/> {r.seats} seats</span>
              </div>
              <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 10, lineHeight: 1.45 }}>{r.next}</div>
              <div style={{ height: 1, background: "var(--line)", margin: "13px 0" }}/>
              <div style={{ display: "flex", gap: 9 }}>
                {r.st === "active" ? (
                  <>
                    <button className="rs-btn soft" style={{ flex: 1, height: 44, fontSize: 12.5 }}>Pause</button>
                    <button className="rs-btn soft" style={{ flex: 1.3, height: 44, fontSize: 12.5 }}>Generate more</button>
                    <button className="rs-btn ghost" style={{ width: 52, height: 44 }} aria-label={`Cancel ${r.name}`}><Icon name="close" size={17} color="var(--status-rejected-ink)"/></button>
                  </>
                ) : (
                  <>
                    <button className="rs-btn" style={{ flex: 1, height: 44, fontSize: 12.5, background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Resume</button>
                    <button className="rs-btn ghost" style={{ flex: 1, height: 44, fontSize: 12.5, color: "var(--status-rejected-ink)" }}>Cancel route</button>
                  </>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D11–D13 · CREATE TRIP WIZARD ═══════════
function DvCreateRouteScreen() {
  return (
    <Phone label="D11 Create · route">
      <div style={{ height: "100%", position: "relative", display: "flex", flexDirection: "column" }}>
        <MapBackdrop pickupLabel={NEXT_DRIVE.from} dropLabel={NEXT_DRIVE.to}/>
        <div style={{ position: "relative", padding: "10px 16px 0", display: "flex", alignItems: "center", gap: 10 }}>
          <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--surface)", boxShadow: "var(--shadow-md)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label="Back"><Icon name="back" size={20}/></button>
          <div style={{ flex: 1, padding: "8px 13px", borderRadius: 14, background: "var(--surface)", boxShadow: "var(--shadow-md)", fontSize: 12.5, fontWeight: 700 }}>Step 1 of 3 · Your route</div>
        </div>
        <div style={{ flex: 1 }}/>
        <div className="rs-sheet" style={{ position: "relative", padding: "6px 16px 14px" }}>
          <div className="rs-sheet-grab"/>
          <div style={{ display: "flex", gap: 11, marginTop: 8 }}>
            <div style={{ width: 14, display: "flex", flexDirection: "column", alignItems: "center", paddingTop: 18 }}>
              <div style={{ width: 11, height: 11, borderRadius: 6, background: "var(--teal)" }}/>
              <div style={{ flex: 1, width: 2, background: "var(--line-2)", margin: "4px 0" }}/>
              <div style={{ width: 11, height: 11, background: "var(--accent-ink)" }}/>
            </div>
            <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: 9 }}>
              <div style={{ height: 50, padding: "0 14px", borderRadius: 14, background: "var(--bg-soft)", border: "1.5px solid var(--line)", display: "flex", alignItems: "center", fontSize: 14.5, fontWeight: 600 }}>{NEXT_DRIVE.from}</div>
              <div style={{ height: 50, padding: "0 14px", borderRadius: 14, background: "var(--bg-soft)", border: "1.5px solid var(--line)", display: "flex", alignItems: "center", fontSize: 14.5, fontWeight: 600 }}>{NEXT_DRIVE.to}</div>
            </div>
          </div>
          <div style={{ marginTop: 12, padding: 13, borderRadius: 16, background: "var(--bg-soft)", border: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 12 }}>
            <Icon name="route" size={19} color="var(--mode-drive-ink)"/>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 13, fontWeight: 700 }}>{NEXT_DRIVE_KM} km · about 32 min</div>
              <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 1 }}>Via Nawala Road and Baseline Road</div>
            </div>
            <button style={{ minHeight: 44, minWidth: 44, padding: "0 8px", fontSize: 12, fontWeight: 800, color: "var(--accent-ink)" }}>Edit</button>
          </div>
          <button className="rs-btn full" style={{ marginTop: 12, background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Continue to timing</button>
        </div>
      </div>
    </Phone>
  );
}

function DvCreateScheduleScreen() {
  return (
    <Phone label="D12 Create · schedule">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="When are you going?" sub="Step 2 of 3"/>
        <div style={{ flex: 1, overflow: "auto", padding: "16px 16px 16px", display: "flex", flexDirection: "column", gap: 16 }} className="rs-scroll">
          <div>
            <div className="rs-section-label" style={{ marginBottom: 9 }}>DEPARTURE</div>
            <div style={{ display: "flex", gap: 10 }}>
              <div style={{ flex: 1, minHeight: 56, padding: "0 15px", borderRadius: 16, background: "var(--surface)", border: "1.5px solid var(--line)", display: "flex", alignItems: "center", gap: 9 }}>
                <Icon name="calendar" size={17} color="var(--ink-3)"/>
                <span style={{ fontSize: 14.5, fontWeight: 600 }}>Today</span>
              </div>
              <div style={{ flex: 1, minHeight: 56, padding: "0 15px", borderRadius: 16, background: "var(--surface)", border: "1.5px solid var(--ink)", display: "flex", alignItems: "center", gap: 9 }}>
                <Icon name="clock" size={17} color="var(--ink-3)"/>
                <span className="tab" style={{ fontSize: 14.5, fontWeight: 700 }}>{NEXT_DRIVE.depart}</span>
              </div>
            </div>
          </div>
          <div>
            <div className="rs-section-label" style={{ marginBottom: 9 }}>REPEAT</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 9 }}>
              {[["Just this once", false], ["Every weekday", true], ["Custom days", false]].map(([l, on]) => (
                <div key={l} style={{
                  minHeight: 52, padding: "0 15px", borderRadius: 15, display: "flex", alignItems: "center", gap: 11,
                  background: on ? "var(--mode-drive-soft)" : "var(--surface)",
                  border: `1.5px solid ${on ? "var(--mode-drive)" : "var(--line)"}`,
                }}>
                  <div style={{ width: 20, height: 20, borderRadius: 10, border: `2px solid ${on ? "var(--mode-drive)" : "var(--line-2)"}`, background: on ? "var(--mode-drive)" : "transparent", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                    {on && <Icon name="check" size={11} color="var(--on-bright-fill)" strokeWidth={3}/>}
                  </div>
                  <div style={{ fontSize: 14, fontWeight: on ? 700 : 600 }}>{l}</div>
                </div>
              ))}
            </div>
          </div>
          <div>
            <div className="rs-section-label" style={{ marginBottom: 9 }}>DAYS</div>
            <DayPicker active={[1, 2, 3, 4, 5]}/>
          </div>
          <div>
            <div className="rs-section-label" style={{ marginBottom: 9 }}>GENERATE TRIPS UNTIL</div>
            <div style={{ minHeight: 52, padding: "0 15px", borderRadius: 15, background: "var(--surface)", border: "1.5px solid var(--line)", display: "flex", alignItems: "center", gap: 9 }}>
              <Icon name="calendar" size={17} color="var(--ink-3)"/>
              <span style={{ fontSize: 14, fontWeight: 600, flex: 1 }}>8 August 2026</span>
              <span style={{ fontSize: 11.5, color: "var(--ink-3)" }}>12 trips</span>
            </div>
          </div>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn full" style={{ background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Continue to seats</button>
        </div>
      </div>
    </Phone>
  );
}

function DvCreatePriceScreen() {
  const perSeat = SEAT_FARE;
  const net = SEAT_NET;
  return (
    <Phone label="D13 Create · seats & price">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Seats and price" sub="Step 3 of 3"/>
        <div style={{ flex: 1, overflow: "auto", padding: "16px 16px 16px", display: "flex", flexDirection: "column", gap: 16 }} className="rs-scroll">
          <div>
            <div className="rs-section-label" style={{ marginBottom: 9 }}>SEATS YOU'RE OFFERING</div>
            <div style={{ display: "flex", gap: 9 }}>
              {[1, 2, 3, 4].map(n => (
                <button key={n} style={{
                  flex: 1, minHeight: 56, borderRadius: 16, fontSize: 17, fontWeight: 800,
                  background: n === NEXT_DRIVE.seatsTotal ? "var(--mode-drive)" : "var(--surface)",
                  color: n === NEXT_DRIVE.seatsTotal ? "var(--on-bright-fill)" : "var(--ink)",
                  border: `1.5px solid ${n === NEXT_DRIVE.seatsTotal ? "var(--mode-drive)" : "var(--line)"}`,
                }}>{n}</button>
              ))}
            </div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 8 }}>Your {MY_VEHICLE.make} seats {MY_VEHICLE.seats} passengers.</div>
          </div>

          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 11 }}>FARE FOR THE FULL ROUTE</div>
            <div style={{ display: "flex", alignItems: "baseline", gap: 9 }}>
              <div className="rs-display tab" style={{ fontSize: 30, lineHeight: 1 }}>{FARE_POLICY.currency} {money(perSeat)}</div>
              <div style={{ fontSize: 12, color: "var(--ink-3)" }}>per seat</div>
            </div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 8, lineHeight: 1.5 }}>
              {NEXT_DRIVE_KM} km at {FARE_POLICY.currency} {money(RATE_BAND.chosen)} per km — the rate you picked inside the {FARE_POLICY.currency} {RATE_BAND.min}–{RATE_BAND.max} band ComiGo set for this car. Riders who match only part of your route pay proportionally less.
            </div>
            <div style={{ height: 1, background: "var(--line)", margin: "13px 0" }}/>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 12.5, fontWeight: 700 }}>You keep per seat</div>
                <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 2 }}>After the {FARE_POLICY.commissionPct}% ComiGo fee</div>
              </div>
              <div className="rs-display tab" style={{ fontSize: 22, fontWeight: 600, color: "var(--status-approved-ink)" }}>{FARE_POLICY.currency} {money(net)}</div>
            </div>
          </div>

          <div>
            <div className="rs-section-label" style={{ marginBottom: 9 }}>WHO CAN BOOK</div>
            <div className="rs-card" style={{ padding: "4px 14px" }}>
              <MenuRow icon="check" label="Book instantly" sub="Any verified rider takes a free seat" right={<Toggle on={false} tint="var(--mode-drive)"/>} chev={false}/>
              <div className="rs-divider"/>
              <MenuRow icon="users" label="I approve each request" sub="You get 30 minutes to accept or decline" right={<Toggle on tint="var(--mode-drive)"/>} chev={false}/>
            </div>
          </div>

          {/* Gender preference. Open to everyone by default; women-only is offered
              to verified female drivers, because it is a safety choice, not a filter. */}
          <div>
            <div className="rs-section-label" style={{ marginBottom: 9 }}>WHO CAN RIDE WITH YOU</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 9 }}>
              {[["users", "Anyone", "Women and men can book a seat", true], ["user", "Women only", `Available because your NIC verifies you as female`, false]].map(([ic, l, s, on]) => (
                <button key={l} data-row={`pref ${l}`} style={{
                  textAlign: "left", minHeight: 60, padding: "12px 15px", borderRadius: 15, display: "flex", alignItems: "center", gap: 12,
                  background: on ? "var(--mode-drive-soft)" : "var(--surface)",
                  border: `1.5px solid ${on ? "var(--mode-drive)" : "var(--line)"}`,
                }}>
                  <div style={{ width: 20, height: 20, borderRadius: 10, border: `2px solid ${on ? "var(--mode-drive)" : "var(--line-2)"}`, background: on ? "var(--mode-drive)" : "transparent", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                    {on && <Icon name="check" size={11} color="var(--on-bright-fill)" strokeWidth={3}/>}
                  </div>
                  <Icon name={ic} size={18} color="var(--ink-2)"/>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13.5, fontWeight: on ? 700 : 600 }}>{l}</div>
                    <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>{s}</div>
                  </div>
                </button>
              ))}
            </div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 8, lineHeight: 1.45 }}>
              Riders see this on your trip before they book, so nobody wastes a request. You can change it per trip.
            </div>
          </div>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn full" style={{ background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Publish trip</button>
          <div style={{ textAlign: "center", fontSize: 11, color: "var(--ink-3)", marginTop: 9, lineHeight: 1.5 }}>Editable until the first seat is booked. After that it can only be cancelled — and cancelling under {POLICY.driverCancelFreeHours} hours before departure carries a penalty.</div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D13b · CREATE · form-first variation ═══════════
function DvCreateFormScreen() {
  return (
    <Phone label="D13b Create · one form">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Publish a trip"/>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          {[
            ["FROM", NEXT_DRIVE.from, "pin"], ["TO", NEXT_DRIVE.to, "pin"],
            ["DATE", "Today, 26 July", "calendar"], ["TIME", NEXT_DRIVE.depart, "clock"],
            ["SEATS", `${NEXT_DRIVE.seatsTotal} of ${MY_VEHICLE.seats}`, "users"],
            ["WHO CAN RIDE", "Anyone", "user"],
            ["VEHICLE", `${MY_VEHICLE.make} · ${MY_VEHICLE.plate}`, "car"],
          ].map(([l, v, ic]) => (
            <div key={l}>
              <div className="rs-section-label" style={{ marginBottom: 6 }}>{l}</div>
              <div style={{ minHeight: 52, padding: "0 14px", borderRadius: 15, background: "var(--surface)", border: "1.5px solid var(--line)", display: "flex", alignItems: "center", gap: 10 }}>
                <Icon name={ic} size={17} color="var(--ink-3)"/>
                <span style={{ fontSize: 14, fontWeight: 600, flex: 1 }}>{v}</span>
                <Icon name="chev" size={16} color="var(--ink-3)"/>
              </div>
            </div>
          ))}
          <div style={{ padding: 14, borderRadius: 16, background: "var(--mode-drive-soft)", border: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 11 }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 12.5, fontWeight: 700, color: "var(--mode-drive-ink)" }}>Estimated per seat</div>
              <div style={{ fontSize: 11, color: "var(--ink-2)", marginTop: 2 }}>{NEXT_DRIVE_KM} km at {FARE_POLICY.currency} {money(RATE_BAND.chosen)}/km · your band</div>
            </div>
            <div className="rs-display tab" style={{ fontSize: 22, fontWeight: 600 }}>{FARE_POLICY.currency} {money(SEAT_FARE)}</div>
          </div>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn full" style={{ background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Publish trip</button>
          <div style={{ textAlign: "center", fontSize: 11, color: "var(--ink-3)", marginTop: 9, lineHeight: 1.5 }}>Editable until someone books a seat.</div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D14 · PUBLISHED + QR ═══════════
function DvPublishedScreen() {
  return (
    <Phone label="D14 Published + QR">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Trip published"/>
        <div style={{ flex: 1, overflow: "auto", padding: "20px 20px 16px", display: "flex", flexDirection: "column", gap: 16, alignItems: "center" }} className="rs-scroll">
          <div style={{ width: 62, height: 62, borderRadius: 20, background: "var(--status-approved-soft)", display: "flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="check" size={30} color="var(--status-approved-ink)" strokeWidth={2.4}/>
          </div>
          <div style={{ textAlign: "center" }}>
            <div className="rs-display" style={{ fontSize: 25, lineHeight: 1.2 }}>You're live</div>
            <div style={{ fontSize: 13, color: "var(--ink-3)", marginTop: 9, lineHeight: 1.55 }}>
              {NEXT_DRIVE.from} → {NEXT_DRIVE.to}, {NEXT_DRIVE.depart}. Riders searching this stretch can see your {NEXT_DRIVE.seatsTotal} seats now.
            </div>
          </div>
          <div style={{ alignSelf: "stretch", display: "flex", flexDirection: "column", gap: 10 }}>
            <Banner kind="info" icon="settings" title="Still editable — nobody has booked yet"
              body={`Change the route, time, seats or who can ride, and the fare recalculates from the distance. The moment someone books a seat this freezes, and cancelling becomes the only change left.`} action="Edit"/>
            <Banner kind="info" icon="clock" title={`Start within ${POLICY.startBufferMin} minutes of ${NEXT_DRIVE.depart}`}
              body={`After that ComiGo cancels the trip for you, nobody is charged, and the miss is recorded against your reliability.`}/>
          </div>
          {/* QR payload for the trip — riders scan to open the booking page */}          <div style={{ padding: 16, borderRadius: 20, background: "var(--surface)", border: "1px solid var(--line)", display: "flex", flexDirection: "column", alignItems: "center", gap: 12 }}>
            <div style={{ width: 150, height: 150, borderRadius: 12, background: "var(--bg-soft)", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 8, border: "1px solid var(--line)" }}>
              <Icon name="target" size={40} color="var(--ink-3)"/>
              <div style={{ fontSize: 10.5, color: "var(--ink-3)", fontWeight: 700, letterSpacing: ".06em" }}>QR CODE</div>
            </div>
            <div className="tab" style={{ fontSize: 12, fontWeight: 600, color: "var(--ink-3)" }}>comigo.lk/r/nug-fort-1005</div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", textAlign: "center", lineHeight: 1.45, maxWidth: 250 }}>
              Show this to a colleague or print it for a noticeboard. Scanning opens the booking page — no app needed to view it.
            </div>
          </div>
          <div style={{ display: "flex", gap: 9, alignSelf: "stretch" }}>
            <button className="rs-btn soft" style={{ flex: 1, height: 48 }}><Icon name="share" size={17}/> Share link</button>
            <button className="rs-btn soft" style={{ flex: 1, height: 48 }}><Icon name="receipt" size={17}/> Save QR</button>
          </div>
        </div>
        <div style={{ padding: "12px 20px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn full" style={{ background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>View the trip</button>
          <button className="rs-btn ghost full" style={{ marginTop: 10 }}>Back to dashboard</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D15 · TRIP DETAIL ═══════════
function DvTripDetailScreen() {
  return (
    <Phone label="D15 Trip detail">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ height: 168, position: "relative", flexShrink: 0 }}>
          <MapBackdrop pickupLabel={NEXT_DRIVE.from} dropLabel={NEXT_DRIVE.to}/>
          <div style={{ position: "absolute", left: 14, top: 12 }}>
            <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--surface)", boxShadow: "var(--shadow-md)", display: "inline-flex", alignItems: "center", justifyContent: "center" }} aria-label="Back"><Icon name="back" size={20}/></button>
          </div>
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 13 }} className="rs-scroll">
          <div>
            <div style={{ display: "flex", alignItems: "center", gap: 9 }}>
              <div className="tab" style={{ fontSize: 11, fontWeight: 800, color: "var(--ink-3)", flex: 1 }}>TODAY · {NEXT_DRIVE.depart}</div>
              <StatusBadge status="approved" label="PUBLISHED"/>
            </div>
            <div className="rs-display" style={{ fontSize: 23, marginTop: 6, lineHeight: 1.2 }}>{NEXT_DRIVE.from} → {NEXT_DRIVE.to}</div>
          </div>
          {NEXT_DRIVE.requests > 0 && (
            <button data-row="seat request" style={{ textAlign: "left", padding: 14, borderRadius: 18, background: "var(--status-pending-soft)", border: "1.5px solid var(--status-pending)", display: "flex", alignItems: "center", gap: 12, minHeight: 44 }}>
              <div style={{ width: 36, height: 36, borderRadius: 12, background: "var(--surface)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                <Icon name="bell" size={17} color="var(--status-pending-ink)" strokeWidth={2.2}/>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 13.5, fontWeight: 700 }}>{NEXT_DRIVE.requests} seat request waiting</div>
                <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>Expires in 26 minutes if you don't answer</div>
              </div>
              <div style={{ fontSize: 12.5, fontWeight: 800, color: "var(--status-pending-ink)", flexShrink: 0 }}>Review</div>
            </button>
          )}
          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 11 }}>PASSENGERS · {NEXT_DRIVE.seatsBooked} OF {NEXT_DRIVE.seatsTotal}</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
              {NEXT_DRIVE.passengers.map((p, i) => (
                <div key={p.name} style={{ display: "flex", alignItems: "center", gap: 11 }}>
                  <Avatar name={p.name} size={38}/>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13.5, fontWeight: 700 }}>{p.name}</div>
                    <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 1, display: "flex", alignItems: "center", gap: 4 }}>
                      <Icon name="star" size={11} color="var(--status-pending-ink)"/>
                      <span className="tab">{i === 0 ? TRUST.passenger.rating : 4.7}</span> · {i === 0 ? TRUST.passenger.trips : 61} trips · {p.from} → {p.to}
                    </div>
                  </div>
                  <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label={`Chat with ${p.name}`}><Icon name="chat" size={18}/></button>
                </div>
              ))}
              <div style={{ display: "flex", alignItems: "center", gap: 11, opacity: .6 }}>
                <div style={{ width: 38, height: 38, borderRadius: 19, background: "var(--bg-soft)", border: "1.5px dashed var(--line-2)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                  <Icon name="plus" size={16} color="var(--ink-3)"/>
                </div>
                <div style={{ fontSize: 13, color: "var(--ink-3)", flex: 1 }}>{NEXT_DRIVE.seatsTotal - NEXT_DRIVE.seatsBooked} seat still free · bookable during the trip too</div>
              </div>
            </div>
            <div style={{ height: 1, background: "var(--line)", margin: "13px 0" }}/>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", lineHeight: 1.45 }}>
              Nobody is charged yet. Every card is captured when you start the trip — and immediately, for anyone who books once you're moving.
            </div>
          </div>
          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 11 }}>YOUR ROUTE</div>
            <RouteTimeline compact stops={[
              { kind: "pickup", place: NEXT_DRIVE.from, time: NEXT_DRIVE.depart },
              { kind: "via", place: "Narahenpita", time: "10:14 AM", note: "Dinuka boards" },
              { kind: "via", place: "Thunmulla", time: "10:22 AM", note: "Tharindu gets off" },
              { kind: "drop", place: NEXT_DRIVE.to, time: "10:37 AM" },
            ]}/>
          </div>
          <Banner kind="info" icon="lock" title={`Frozen — ${NEXT_DRIVE.seatsBooked} people have booked`}
            body={`Riders chose this trip on its route, time and price, so those are fixed now. Cancelling is the only change left — free until ${POLICY.driverCancelFreeHours} hours before departure.`}/>
          <button className="rs-btn ghost full" style={{ height: 48, fontSize: 13, color: "var(--status-rejected-ink)" }}>Cancel trip</button>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn full" style={{ background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Start trip</button>
          <div style={{ textAlign: "center", fontSize: 11, color: "var(--ink-3)", marginTop: 9 }}>Starting the trip charges every booked card.</div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D16 · BOOKING REQUESTS ═══════════
// Two situations, one screen. A request against a scheduled trip captures the
// card at start; a request against a trip already running captures on accept.
// The driver has to know which one he is agreeing to.
function DvRequestsScreen({ enRoute = false }) {
  const b = INBOUND_BOOKING;
  return (
    <Phone label={`${enRoute ? "D16b" : "D16"} Booking requests · ${enRoute ? "mid-trip" : "scheduled"}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Seat requests" sub={enRoute ? `On the road · ${LIVE_DRIVE.from} → ${LIVE_DRIVE.to}` : `${NEXT_DRIVE.depart} · ${NEXT_DRIVE.from} → ${NEXT_DRIVE.to}`}/>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 11 }} className="rs-scroll">
          {enRoute ? (
            <Banner kind="warn" icon="car" title="You're mid-trip — accepting charges him now"
              body={`Seat ${LIVE_DRIVE.earlyDrop.seat} came free when ${LIVE_DRIVE.earlyDrop.name.split(" ")[0]} got off at ${LIVE_DRIVE.earlyDrop.place}. Because the trip is running, ${b.passenger.split(" ")[0]}'s card is captured the moment you accept. Only accept if you can stop safely.`}/>
          ) : (
            <Banner kind="warn" icon="clock" title="Answer within 26 minutes"
              body="Requests expire automatically after 30 minutes. Accepting doesn't take his money — every card is captured when you start the trip."/>
          )}

          <TrustStats name={b.passenger} role="Passenger" tint="var(--mode-drive-ink)"
            rating={TRUST.passenger.rating} ratings={TRUST.passenger.ratings}
            stats={[{ l: "Trips", v: TRUST.passenger.trips }, { l: "Completed", v: `${TRUST.passenger.completed}%` }, { l: "No-shows", v: TRUST.passenger.noShows }]}/>

          <div className="rs-card" style={{ padding: 15 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 9 }}>
              <div className="rs-section-label" style={{ flex: 1 }}>{enRoute ? "WHERE HE'D JOIN" : "HIS JOURNEY"}</div>
              <StatusBadge status="pending" label={enRoute ? `${ENROUTE_RIDE.etaMin} MIN AHEAD` : "26 MIN LEFT"}/>
            </div>
            <div style={{ height: 1, background: "var(--line)", margin: "13px 0" }}/>
            <RouteTimeline compact stops={enRoute ? [
              { kind: "pickup", place: LIVE_DRIVE.earlyDrop.place, time: "Now", note: `${ENROUTE_RIDE.etaMin} minutes ahead · on your route` },
              { kind: "drop", place: b.to, time: "10:31 AM", note: "On your route · no detour" },
            ] : [
              { kind: "pickup", place: b.from, time: "10:14 AM", note: "On your route · no detour" },
              { kind: "drop", place: b.to, time: "10:31 AM", note: "On your route · no detour" },
            ]}/>
            <div style={{ height: 1, background: "var(--line)", margin: "13px 0" }}/>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 12.5, fontWeight: 700 }}>{b.seats} seat · you'd earn</div>
                <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 2 }}>{FARE_POLICY.currency} {money(b.fare)} fare less the {FARE_POLICY.commissionPct}% fee</div>
              </div>
              <div className="rs-display tab" style={{ fontSize: 24, fontWeight: 600, color: "var(--status-approved-ink)" }}>{FARE_POLICY.currency} {money(b.net)}</div>
            </div>
            <div style={{ marginTop: 11, padding: "10px 12px", borderRadius: 12, background: "var(--bg-soft)", border: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 9 }}>
              <Icon name="card" size={15} color="var(--ink-3)"/>
              <div style={{ fontSize: 11.5, color: "var(--ink-2)", flex: 1, lineHeight: 1.4 }}>
                {enRoute ? "Charged the moment you accept" : `Charged when you start the trip at ${NEXT_DRIVE.depart}`}
              </div>
            </div>
            <div style={{ display: "flex", gap: 9, marginTop: 14 }}>
              <button className="rs-btn ghost" style={{ flex: 1, height: 50, fontSize: 13.5 }}>Decline</button>
              <button className="rs-btn" style={{ flex: 1.6, height: 50, background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>{enRoute ? "Accept · charge now" : "Accept seat"}</button>
            </div>
          </div>
          {!enRoute && (
            <div className="rs-card" style={{ padding: 15, opacity: .72 }}>
              <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <Avatar name="Hasitha R" size={40}/>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 13.5, fontWeight: 700 }}>Hasitha R</div>
                  <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 1 }}>Expired — didn't answer in time</div>
                </div>
                <StatusBadge status="none" label="EXPIRED"/>
              </div>
            </div>
          )}
        </div>
        <TabBar mode="drive" active="trips" badges={{ trips: NEXT_DRIVE.requests, inbox: 2 }}/>
      </div>
    </Phone>
  );
}

// ═══════════ D30 · CANCEL A PUBLISHED TRIP ═══════════
// The highest-consequence driver action, and the only way to change a published
// trip. Two windows: outside 12 hours it is free, inside it costs a penalty out
// of the next trip's earnings. Nobody is refunded, because under charge-on-start
// nobody has been charged yet.
function DvCancelTripScreen({ window = "late" }) {
  const pax = NEXT_DRIVE.passengers;
  const n = pax.length;
  const late = window === "late";
  const hrs = late ? MY_TRIP.cancelledBeforeHrs : 26;
  const penalty = cancelPenalty();
  return (
    <Phone label={`${late ? "D30" : "D30b"} Cancel trip · ${late ? `inside ${POLICY.driverCancelFreeHours} h` : "free window"}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Cancel this trip" sub={`${NEXT_DRIVE.depart} · ${NEXT_DRIVE.from} → ${NEXT_DRIVE.to}`}/>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          {late ? (
            <Banner kind="bad" icon="alert" title={`${hrs} hours to departure — this carries a penalty`}
              body={`Inside ${POLICY.driverCancelFreeHours} hours, ${FARE_POLICY.currency} ${money(penalty)} (${POLICY.lateCancelPenaltyPct}% of this trip) is taken out of your next trip's earnings, and ${n} people lose their ride to work.`}/>
          ) : (
            <Banner kind="info" icon="clock" title={`${hrs} hours to departure — no penalty`}
              body={`You're outside the ${POLICY.driverCancelFreeHours}-hour window, so cancelling costs you nothing and doesn't touch your reliability. ${paxFirstNames()} get told straight away, with time to find another seat.`}/>
          )}
          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 11 }}>WHY ARE YOU CANCELLING?</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
              {[["Vehicle problem", "Breakdown, service, no fuel", true], ["I'm unwell", "", false], ["Plans changed", "Not travelling this route today", false], ["Route or timing wrong", "I published the wrong details", false], ["Something else", "Tell us in your own words", false]].map(([l, s, on]) => (
                <button key={l} data-row={`reason ${l}`} style={{ textAlign: "left", padding: 13, borderRadius: 14, minHeight: 44, background: on ? "var(--mode-drive-soft)" : "var(--surface)", border: `1.5px solid ${on ? "var(--mode-drive)" : "var(--line)"}`, display: "flex", alignItems: "center", gap: 11 }}>
                  <div style={{ width: 20, height: 20, borderRadius: 10, flexShrink: 0, border: `2px solid ${on ? "var(--mode-drive)" : "var(--line-2)"}`, background: on ? "var(--mode-drive)" : "transparent", display: "flex", alignItems: "center", justifyContent: "center" }}>
                    {on && <Icon name="check" size={12} color="var(--on-bright-fill)" strokeWidth={3}/>}
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13.5, fontWeight: 700 }}>{l}</div>
                    {s && <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>{s}</div>}
                  </div>
                </button>
              ))}
            </div>
          </div>
          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 11 }}>WHAT HAPPENS NEXT</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              <RuleRow icon="card" title="Nobody is refunded, because nobody was charged"
                body={`Cards are captured when a trip starts. The ${FARE_POLICY.currency} ${money(refundTotal())} in booked fares is simply never collected.`}/>
              <RuleRow icon="bell" title="We tell them immediately"
                body="Push and SMS, with your reason and other drivers on their route."/>
              {late ? (
                <>
                  <RuleRow icon="cash" tint="var(--status-rejected-ink)" title={`${FARE_POLICY.currency} ${money(penalty)} penalty on your next trip`}
                    body={`Deducted from what you earn on your next completed trip, not billed to you. It shows in your ledger as a ${POLICY.lateCancelPenaltyPct}% late-cancellation penalty.`}/>
                  <RuleRow icon="users" tint="var(--status-rejected-ink)" title={`${FARE_POLICY.currency} ${money(victimShare(penalty))} of it goes to ${paxFirstNames()}`}
                    body={`Every penalty is split ${POLICY.penaltyVictimPct}/${POLICY.penaltyPlatformPct}: half to the people who were let down, split between them as ride credit, and ${FARE_POLICY.currency} ${money(platformShare(penalty))} to ComiGo. It works the same way when a rider stands you up.`}/>
                  <RuleRow icon="star" tint="var(--status-rejected-ink)" title="Your reliability score drops"
                    body={`This is late cancellation ${DRIVER_RELIABILITY.lateCancellations + 1} in 30 days. Three pauses publishing for a week.`}/>
                </>
              ) : (
                <RuleRow icon="star" title="No penalty and no score change"
                  body={`Cancelling more than ${POLICY.driverCancelFreeHours} hours out is free and unlimited — it is exactly what we want you to do when plans change.`}/>
              )}
            </div>
            <div style={{ height: 1, background: "var(--line)", margin: "13px 0" }}/>
            <div style={{ display: "flex", flexDirection: "column", gap: 9 }}>
              {pax.map(p => (
                <div key={p.name} style={{ display: "flex", alignItems: "center", gap: 11 }}>
                  <Avatar name={p.name} size={36}/>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13, fontWeight: 700 }}>{p.name}</div>
                    <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 1 }}>{p.from} → {p.to}</div>
                  </div>
                  <StatusBadge status="none" label="NOT CHARGED"/>
                </div>
              ))}
            </div>
          </div>
          <Banner kind="info" icon="lock" title="You can't edit a published trip"
            body={late
              ? `Cancelling and republishing is the only way to change details — and this close to departure it leaves ${n} people stranded. Consider driving it as published.`
              : `Cancelling and republishing is the only way to change details. Doing it now, outside ${POLICY.driverCancelFreeHours} hours, costs you nothing.`}/>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)", display: "flex", flexDirection: "column", gap: 9 }}>
          <button className="rs-btn full" style={{ background: late ? "var(--danger)" : "var(--mode-drive)", color: "var(--on-bright-fill)" }}>
            {late ? `Cancel trip · ${FARE_POLICY.currency} ${money(penalty)} penalty` : "Cancel trip · no penalty"}
          </button>
          <button className="rs-btn soft full">Keep the trip</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D31 · TRIP CANCELLED ═══════════
function DvTripCancelledScreen({ window = "late" }) {
  const n = NEXT_DRIVE.passengers.length;
  const late = window === "late";
  const penalty = cancelPenalty();
  return (
    <Phone label={`${late ? "D31" : "D31b"} Trip cancelled · ${late ? "penalty" : "free"}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ flex: 1, overflow: "auto", padding: "28px 20px 16px", display: "flex", flexDirection: "column" }} className="rs-scroll">
          <div style={{ width: 60, height: 60, borderRadius: 20, background: "var(--status-rejected-soft)", display: "flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="close" size={28} color="var(--status-rejected-ink)" strokeWidth={2.3}/>
          </div>
          <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em", color: "var(--status-rejected-ink)", marginTop: 16 }}>CANCELLED · {NEXT_DRIVE.depart}</div>
          <div className="rs-display" style={{ fontSize: 26, marginTop: 4, lineHeight: 1.15 }}>{NEXT_DRIVE.from} → {NEXT_DRIVE.to}</div>
          <div style={{ fontSize: 13.5, color: "var(--ink-3)", marginTop: 11, lineHeight: 1.6, textWrap: "pretty" }}>
            {paxFirstNames()} have been told. Nobody was charged, so there is nothing to refund. The trip is off your schedule.
          </div>
          <div className="rs-card" style={{ padding: 15, marginTop: 18 }}>
            <div className="rs-section-label" style={{ marginBottom: 11 }}>DONE FOR YOU</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              {[[`${n} riders notified`, "Push and SMS sent, with your reason"], ["No money moved", `${FARE_POLICY.currency} ${money(refundTotal())} in fares was never collected`], ["Trip removed", "No longer visible in search"]].map(([t, s]) => (
                <div key={t} style={{ display: "flex", gap: 11, alignItems: "center" }}>
                  <div style={{ width: 22, height: 22, borderRadius: 11, background: "var(--status-approved)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                    <Icon name="check" size={13} color="var(--on-bright-fill)" strokeWidth={3}/>
                  </div>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontSize: 13, fontWeight: 700 }}>{t}</div>
                    <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 1 }}>{s}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
          {late ? (
            <>
              <div style={{ marginTop: 12, padding: 15, borderRadius: 18, background: "var(--status-rejected-soft)", border: "1px solid var(--status-rejected)" }}>
                <div className="rs-section-label" style={{ color: "var(--status-rejected-ink)", marginBottom: 10 }}>PENALTY · {POLICY.lateCancelPenaltyPct}%</div>
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontSize: 12.5, fontWeight: 700 }}>Taken from your next trip</div>
                    <div style={{ fontSize: 11.5, color: "var(--ink-2)", marginTop: 2, lineHeight: 1.45 }}>Because you cancelled {MY_TRIP.cancelledBeforeHrs} hours before departure. Nothing is billed to you — it comes out of what you earn next.</div>
                    <div style={{ fontSize: 11.5, color: "var(--ink-2)", marginTop: 6, lineHeight: 1.45 }}>{FARE_POLICY.currency} {money(victimShare(penalty))} of it reaches {paxFirstNames()} as ride credit; {FARE_POLICY.currency} {money(platformShare(penalty))} goes to ComiGo. Penalties are always split {POLICY.penaltyVictimPct}/{POLICY.penaltyPlatformPct}.</div>
                  </div>
                  <div className="rs-display tab" style={{ fontSize: 23, fontWeight: 600, color: "var(--status-rejected-ink)" }}>−{FARE_POLICY.currency} {money(penalty)}</div>
                </div>
              </div>
              <div style={{ marginTop: 12 }}>
                <Banner kind="warn" icon="star" title={`${DRIVER_RELIABILITY.lateCancellations + 1} late cancellations in the last 30 days`}
                  body="One more and publishing pauses for a week. Your score recovers as you complete trips." action="My rating"/>
              </div>
            </>
          ) : (
            <div style={{ marginTop: 12 }}>
              <Banner kind="good" icon="check" title="No penalty, no score change"
                body={`You cancelled more than ${POLICY.driverCancelFreeHours} hours before departure, so nothing is deducted and your reliability is untouched.`}/>
            </div>
          )}
          <div style={{ marginTop: 12 }}>
            <Banner kind="info" icon="calendar" title="This was one of a recurring route"
              body="Only today's trip was cancelled. Tomorrow's 10:05 AM run is still published." action="Manage route"/>
          </div>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn full" style={{ background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Back to my trips</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D32 · THE START BUFFER ═══════════
// A published departure is a promise. Miss it by more than the buffer and the
// system cancels for you — nobody is charged, but the miss is recorded, and the
// third one in a month takes the driver profile offline.
function DvStartBufferScreen({ state = "counting" }) {
  const extended = state === "extended";
  const counting = state === "counting" || extended;
  const pax = NEXT_DRIVE.passengers;
  const already = DRIVER_RELIABILITY.missedStarts - 1;   // before this one
  // The extension is a single, spendable thing. Before it is used the clock runs
  // on the 10-minute buffer; after it, on the 10 minutes it bought — and there is
  // no second one, so the button has to disappear rather than fail on tap.
  const extLeft = POLICY.startExtendLimit - (extended ? 1 : 0);
  const label = extended ? "D32c Start buffer · extension used" : counting ? "D32 Start buffer · counting down" : "D32b Auto-cancelled · missed start";
  return (
    <Phone label={label}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "20px 20px 22px", background: counting ? "var(--status-pending)" : "var(--danger)", color: "var(--on-bright-fill)", flexShrink: 0 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <Icon name={counting ? "clock" : "close"} size={20} color="var(--on-bright-fill)" strokeWidth={2.4}/>
            <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em" }}>
              {extended ? `EXTENDED BY ${POLICY.startExtendMin} MINUTES` : counting ? `DEPARTURE WAS ${NEXT_DRIVE.depart}` : `NO START WITHIN ${POLICY.startBufferMin} MINUTES`}
            </div>
          </div>
          {counting ? (
            <>
              <div className="rs-display tab" style={{ fontSize: 46, lineHeight: 1.05, marginTop: 10 }}>{extended ? "9:31" : "8:14"}</div>
              <div style={{ fontSize: 13, marginTop: 4, opacity: .9, lineHeight: 1.5 }}>
                {extended
                  ? `of your extra ${POLICY.startExtendMin} minutes. This was your only extension — the trip cancels when it runs out.`
                  : "left to start before ComiGo cancels this trip for you"}
              </div>
            </>
          ) : (
            <>
              <div className="rs-display" style={{ fontSize: 27, lineHeight: 1.15, marginTop: 10 }}>Trip auto-cancelled</div>
              <div style={{ fontSize: 13, marginTop: 6, opacity: .9, lineHeight: 1.5 }}>{NEXT_DRIVE.from} → {NEXT_DRIVE.to}, {NEXT_DRIVE.depart}</div>
            </>
          )}
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          {counting ? (
            <>
              <div className="rs-card" style={{ padding: 15 }}>
                <div className="rs-section-label" style={{ marginBottom: 11 }}>IF THE TIMER RUNS OUT</div>
                <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
                  <RuleRow icon="close" tint="var(--status-rejected-ink)" title="The trip is cancelled automatically" body={`${paxFirstNames()} are told and released to find another seat.`}/>
                  <RuleRow icon="card" title="Nobody is charged" body="Cards are only captured at start, so there is no money to unwind — and no earnings for you."/>
                  <RuleRow icon="star" tint="var(--status-rejected-ink)" title="Your rating is degraded" body={`A missed start hits your score harder than a decline. You have ${already} already in ${EARLY_DROP.month}.`}/>
                  <RuleRow icon="shield" tint="var(--status-rejected-ink)" title={`${DRIVER_RELIABILITY.missedStartLimit} in a month deactivates your profile`} body={`This would be ${already + 1} of ${DRIVER_RELIABILITY.missedStartLimit}. After the third you'd have to ask an admin to reinstate driving.`}/>
                </div>
              </div>
              <div className="rs-card" style={{ padding: 15 }}>
                <div className="rs-section-label" style={{ marginBottom: 11 }}>{pax.length} PEOPLE ARE WAITING</div>
                <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
                  {pax.map(p => (
                    <div key={p.name} style={{ display: "flex", alignItems: "center", gap: 11 }}>
                      <Avatar name={p.name} size={36}/>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontSize: 13, fontWeight: 700 }}>{p.name}</div>
                        <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 1 }}>At {p.from} since {NEXT_DRIVE.depart}</div>
                      </div>
                      <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label={`Chat with ${p.name}`}><Icon name="chat" size={18}/></button>
                    </div>
                  ))}
                </div>
              </div>
              {extended ? (
                <Banner kind="warn" icon="clock" title={`Extension used · ${extLeft} left`}
                  body={`You get one ${POLICY.startExtendMin}-minute extension per trip and this trip's is spent. It protects you from the auto-cancel — it does not oblige ${paxFirstNames()} to keep waiting, and either of them may cancel free of charge and leave.`}/>
              ) : (
                <Banner kind="info" icon="clock" title={`You can buy ${POLICY.startExtendMin} more minutes, once`}
                  body={`One extension per trip, no penalty for taking it. It only stops ComiGo cancelling — after ${POLICY.driverLateGraceMin} minutes your riders can still cancel free of charge, so tell them why in the chat.`}/>
              )}
              <Banner kind="info" icon="chat" title="Running late is not the same as not turning up"
                body="Tell them in the booking chat and start the trip — a late start still counts as a start."/>
            </>
          ) : (
            <>
              <div className="rs-card" style={{ padding: 15 }}>
                <div className="rs-section-label" style={{ marginBottom: 11 }}>WHAT WE DID</div>
                <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
                  <RuleRow icon="bell" title={`${pax.length} riders told and released`} body={`${paxFirstNames()} were offered the next drivers on their route.`}/>
                  <RuleRow icon="card" title="No money moved" body={`No card was captured, so nothing to refund — and no earnings from this trip.`}/>
                  <RuleRow icon="star" tint="var(--status-rejected-ink)" title={`Rating degraded · ${DRIVER_RELIABILITY.ratingBefore} → ${DRIVER_RELIABILITY.ratingAfter}`} body="Missed starts weigh more than any other reliability signal."/>
                </div>
              </div>
              <div style={{ padding: 15, borderRadius: 18, background: "var(--status-rejected-soft)", border: "1px solid var(--status-rejected)" }}>
                <div className="rs-section-label" style={{ color: "var(--status-rejected-ink)", marginBottom: 10 }}>MISSED STARTS IN {EARLY_DROP.month.toUpperCase()}</div>
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                  <div style={{ display: "flex", gap: 6, flex: 1 }}>
                    {Array.from({ length: DRIVER_RELIABILITY.missedStartLimit }, (_, i) => (
                      <div key={i} style={{ flex: 1, height: 8, borderRadius: 4, background: i < DRIVER_RELIABILITY.missedStarts ? "var(--status-rejected)" : "var(--surface)", border: "1px solid var(--status-rejected)" }}/>
                    ))}
                  </div>
                  <div className="rs-display tab" style={{ fontSize: 20, fontWeight: 600, color: "var(--status-rejected-ink)" }}>{DRIVER_RELIABILITY.missedStarts} of {DRIVER_RELIABILITY.missedStartLimit}</div>
                </div>
                <div style={{ fontSize: 12, color: "var(--ink-2)", marginTop: 11, lineHeight: 1.5 }}>
                  {missedStartsLeft() === 1
                    ? `One more missed start this month and your driver profile is deactivated. You'd keep riding as a passenger, and an admin would have to approve you again to drive.`
                    : `Your driver profile is deactivated. An admin has to approve you again before you can publish.`}
                </div>
              </div>
              <Banner kind="info" icon="calendar" title="The count resets monthly"
                body={`Missed starts are counted per calendar month. Yours resets on 1 August — completed trips in the meantime pull your rating back up.`}/>
            </>
          )}
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)", display: "flex", flexDirection: "column", gap: 9 }}>
          {counting ? (
            <>
              <button className="rs-btn full" style={{ background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Start trip now</button>
              {extLeft > 0 ? (
                <button className="rs-btn soft full">Give me {POLICY.startExtendMin} more minutes</button>
              ) : (
                <button className="rs-btn soft full" disabled style={{ opacity: .45 }} aria-disabled="true">Extension already used</button>
              )}
              <button className="rs-btn ghost full" style={{ color: "var(--status-rejected-ink)" }}>Cancel the trip instead</button>
            </>
          ) : (
            <>
              <button className="rs-btn full" style={{ background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>My reliability</button>
              <button className="rs-btn soft full">Back to my trips</button>
            </>
          )}
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D35 · DRIVING PREFERENCES ═══════════
// Account-level defaults for every trip published from here. The women-only
// option is offered to verified female drivers only — it is a safety setting,
// not a marketplace filter.
function DvPreferencesScreen() {
  return (
    <Phone label="D35 Driving preferences">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Driving preferences" sub="Defaults for every trip you publish"/>
        <div style={{ flex: 1, overflow: "auto", padding: "0 16px 16px" }} className="rs-scroll">
          <GroupLabel>WHO CAN RIDE WITH YOU</GroupLabel>
          <div style={{ display: "flex", flexDirection: "column", gap: 9 }}>
            {[["users", "Anyone", "Women and men can book a seat", true], ["user", "Women only", "Your NIC verifies you as female, so you can limit seats to women", false]].map(([ic, l, s, on]) => (
              <button key={l} data-row={`pref ${l}`} style={{
                textAlign: "left", minHeight: 64, padding: "13px 15px", borderRadius: 16, display: "flex", alignItems: "center", gap: 12,
                background: on ? "var(--mode-drive-soft)" : "var(--surface)",
                border: `1.5px solid ${on ? "var(--mode-drive)" : "var(--line)"}`,
              }}>
                <div style={{ width: 20, height: 20, borderRadius: 10, border: `2px solid ${on ? "var(--mode-drive)" : "var(--line-2)"}`, background: on ? "var(--mode-drive)" : "transparent", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                  {on && <Icon name="check" size={11} color="var(--on-bright-fill)" strokeWidth={3}/>}
                </div>
                <Icon name={ic} size={18} color="var(--ink-2)"/>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 13.5, fontWeight: on ? 700 : 600 }}>{l}</div>
                  <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>{s}</div>
                </div>
              </button>
            ))}
          </div>
          <div style={{ marginTop: 12 }}>
            <Banner kind="info" icon="shield" title="Riders see this before they request"
              body="A women-only trip is shown as such in search, so a man never wastes a request and you never have to decline one."/>
          </div>

          <GroupLabel>IDENTITY</GroupLabel>
          <div className="rs-card" style={{ padding: "4px 14px" }}>
            <MenuRow icon="shield" label="Verified riders only"
              sub={`Only riders who have passed NIC verification can book. About ${VERIFIED_PAX_SHARE}% of riders on your routes are verified.`}
              right={<Toggle on={DRIVER_PREFS.verifiedOnly} tint="var(--mode-drive)"/>} chev={false}/>
            <div className="rs-divider"/>
            <MenuRow icon="user" label="Your photo is always shown to booked riders"
              sub="A rider can hide her photo from everyone but her driver. You can't — she is getting into your car and has to know it's you. It is never shown in search, only after a booking is confirmed."
              badge={<StatusBadge status="approved" label="ALWAYS ON"/>} chev={false}/>
          </div>
          <div style={{ marginTop: 10 }}>
            <Banner kind="warn" icon="users" title={`It cost you ${DRIVER_PREFS.verifiedOnlyCost} requests last week`}
              body="Unverified riders never see the trip, so they can't request it — which is the point, but it is also fewer seats sold. Turn it off for a quiet route and back on for an evening one."/>
          </div>

          <GroupLabel>SEATS AND BOOKING</GroupLabel>
          <div className="rs-card" style={{ padding: "4px 14px" }}>
            <MenuRow icon="users" label="I approve each request" sub="30 minutes to accept or decline" right={<Toggle on tint="var(--mode-drive)"/>} chev={false}/>
            <div className="rs-divider"/>
            <MenuRow icon="car" label="Let riders book while I'm driving" sub="Fills seats freed by an early drop-off. Those bookings are charged on acceptance." right={<Toggle on tint="var(--mode-drive)"/>} chev={false}/>
            <div className="rs-divider"/>
            <MenuRow icon="pin" label="Accept early drop-off requests" sub="Passengers can ask to get out before their stop" right={<Toggle on tint="var(--mode-drive)"/>} chev={false}/>
          </div>

          <GroupLabel>CONTACT</GroupLabel>
          <div className="rs-card" style={{ padding: "4px 14px" }}>
            <MenuRow icon="chat" label="In-booking chat" sub="Open from acceptance until 24 hours after drop-off" right={<Toggle on tint="var(--mode-drive)"/>} chev={false}/>
            <div className="rs-divider"/>
            <MenuRow icon="phone" label="Hide my number on calls" sub="Calls go through a ComiGo relay" right={<Toggle on tint="var(--mode-drive)"/>} chev={false}/>
          </div>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, {
  DvHomeScreen, DvTripListScreen, DvRecurringScreen,
  DvCreateRouteScreen, DvCreateScheduleScreen, DvCreatePriceScreen, DvCreateFormScreen,
  DvPublishedScreen, DvTripDetailScreen, DvRequestsScreen,
  DvCancelTripScreen, DvTripCancelledScreen, DvStartBufferScreen, DvPreferencesScreen,
});
