// driver-become.jsx — D01…D07: the become-a-driver entry, the 3-step KYC,
// pending review, vehicles list and add-vehicle.

// ═══════════ D01 · BECOME A DRIVER ═══════════
function DvBecomeScreen() {
  return (
    <Phone label="D01 Become a driver">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Start earning"/>
        <div style={{ flex: 1, overflow: "auto", padding: "18px 20px 16px", display: "flex", flexDirection: "column", gap: 16 }} className="rs-scroll">
          <div>
            <div style={{ width: 56, height: 56, borderRadius: 19, background: "var(--mode-drive)", display: "flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="car" size={26} color="var(--on-bright-fill)"/>
            </div>
            <div className="rs-display" style={{ fontSize: 27, marginTop: 15, lineHeight: 1.18 }}>Your empty seats<br/>can pay for petrol.</div>
            <div style={{ fontSize: 13.5, color: "var(--ink-3)", marginTop: 11, lineHeight: 1.6, textWrap: "pretty" }}>
              Publish the trips you already make. Riders book the seats you weren't using, and you keep {100 - FARE_POLICY.commissionPct}% of every fare. Same account — you can still ride as a passenger whenever you like.
            </div>
          </div>

          <div className="rs-card" style={{ padding: 16 }}>
            <div className="rs-section-label" style={{ marginBottom: 12 }}>WHAT A WEEKDAY COMMUTE EARNS</div>
            <div style={{ display: "flex", alignItems: "baseline", gap: 10 }}>
              <div className="rs-display tab" style={{ fontSize: 34, lineHeight: 1 }}>{FARE_POLICY.currency} {money(8400)}</div>
              <div style={{ fontSize: 12, color: "var(--ink-3)" }}>a month</div>
            </div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 8, lineHeight: 1.5 }}>
              Based on 2 seats, {NEXT_DRIVE.from} → {NEXT_DRIVE.to}, 20 weekdays. Your actual earnings depend on distance, seats and how many riders match your route.
            </div>
          </div>

          <div>
            <div className="rs-section-label" style={{ marginBottom: 11 }}>WHAT YOU'LL NEED</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              {[
                ["user", "Your NIC", "Front and back, or a passport."],
                ["card", "A valid driving licence", "Must not expire in the next 30 days."],
                ["car", "Vehicle papers", "Registration and a current insurance certificate."],
                ["cash", "A payout account", "Bank account or mobile wallet, in your own name."],
              ].map(([ic, t, s]) => (
                <div key={t} style={{ display: "flex", gap: 12 }}>
                  <div style={{ width: 34, height: 34, borderRadius: 11, background: "var(--bg-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                    <Icon name={ic} size={16} color="var(--ink-2)"/>
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13.5, fontWeight: 700 }}>{t}</div>
                    <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>{s}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <Banner kind="info" icon="clock" title="About 10 minutes, then a day's wait"
            body="Most applications are reviewed within one working day. You can keep riding while we check."/>
        </div>
        <div style={{ padding: "12px 20px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn full" style={{ background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Start my application</button>
          <div style={{ textAlign: "center", fontSize: 11, color: "var(--ink-3)", marginTop: 9 }}>Free. No commitment to publish anything.</div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D02–D04 · KYC, three steps ═══════════
const KYC_STEPS = [
  {
    n: 1, title: "Prove who you are", sub: "This is the same check every ComiGo driver goes through.",
    label: "IDENTITY", fields: [["FULL NAME, AS ON YOUR NIC", "Nimali Perera"], ["NIC NUMBER", "199834502187"], ["DATE OF BIRTH", "12 March 1998"]],
    uploads: [["NIC · front", "approved"], ["NIC · back", "none"]],
    note: "Your NIC number is stored encrypted and never shown to riders.",
  },
  {
    n: 2, title: "Your driving licence", sub: "We check the number, the expiry date and that the photo is yours.",
    label: "LICENCE", fields: [["LICENCE NUMBER", "B1948872"], ["EXPIRES", "March 2029"]],
    uploads: [["Licence · front", "approved"], ["Licence · back", "pending"]],
    note: "A licence expiring within 30 days will be rejected — renew first.",
  },
  {
    n: 3, title: "The car you'll drive", sub: "Riders see the make, colour and plate before they book.",
    label: "VEHICLE", fields: [["MAKE AND MODEL", "Suzuki Wagon R"], ["COLOUR", "Pearl white"], ["NUMBER PLATE", "WP CAB-7734"], ["PASSENGER SEATS", "4"]],
    uploads: [["Registration", "approved"], ["Insurance certificate", "none"]],
    note: "Insurance must be current for the whole period you're publishing trips.",
  },
];

function DvKycScreen({ step = 1 }) {
  const s = KYC_STEPS[step - 1];
  return (
    <Phone label={`D0${step + 1} KYC · ${s.label.toLowerCase()}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Driver application" sub={`Step ${s.n} of 3`}/>
        <div style={{ padding: "12px 20px 0" }}>
          <Stepper step={s.n} total={3} labels={["Identity", "Licence", "Vehicle"]}/>
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: "18px 20px 16px", display: "flex", flexDirection: "column", gap: 16 }} className="rs-scroll">
          <div>
            <div className="rs-display" style={{ fontSize: 24, lineHeight: 1.2 }}>{s.title}</div>
            <div style={{ fontSize: 13, color: "var(--ink-3)", marginTop: 8, lineHeight: 1.55 }}>{s.sub}</div>
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: 13 }}>
            {s.fields.map(([l, v]) => (
              <div key={l}>
                <div className="rs-section-label" style={{ marginBottom: 7 }}>{l}</div>
                <div style={{ minHeight: 52, padding: "0 15px", borderRadius: 15, background: "var(--surface)", border: "1.5px solid var(--line)", display: "flex", alignItems: "center", fontSize: 14.5, fontWeight: 600 }}>{v}</div>
              </div>
            ))}
          </div>
          <div>
            <div className="rs-section-label" style={{ marginBottom: 9 }}>PHOTOS</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 9 }}>
              {s.uploads.map(([l, st]) => {
                const m = STATUS_META[st];
                const done = st !== "none";
                return (
                  <div key={l} style={{
                    padding: 13, borderRadius: 15, display: "flex", alignItems: "center", gap: 11,
                    background: "var(--surface)",
                    border: done ? "1px solid var(--line)" : "1.5px dashed var(--line-2)",
                  }}>
                    <div style={{ width: 36, height: 36, borderRadius: 11, background: m.bg, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                      <Icon name={done ? m.icon : "plus"} size={17} color={m.c} strokeWidth={2.2}/>
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 13.5, fontWeight: 700 }}>{l}</div>
                      <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 1 }}>{st === "approved" ? "Uploaded" : st === "pending" ? "Checking…" : "Not added yet"}</div>
                    </div>
                    {done ? <StatusBadge status={st}/> : <div className="rs-btn accent" style={{ height: 44, padding: "0 16px", fontSize: 12.5, flexShrink: 0 }}>Add</div>}
                  </div>
                );
              })}
            </div>
          </div>
          <Banner kind="info" icon="lock" title="Why we ask" body={s.note}/>
        </div>
        <div style={{ padding: "12px 20px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">{s.n === 3 ? "Submit application" : "Continue"}</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D05 · APPLICATION PENDING ═══════════
function DvKycPendingScreen() {
  return (
    <Phone label="D05 Application pending">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Driver application"/>
        <div style={{ flex: 1, overflow: "auto", padding: "22px 20px 16px", display: "flex", flexDirection: "column", gap: 16 }} className="rs-scroll">
          <div style={{ width: 62, height: 62, borderRadius: 20, background: "var(--status-pending-soft)", display: "flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="clock" size={29} color="var(--status-pending-ink)"/>
          </div>
          <div>
            <div className="rs-display" style={{ fontSize: 26, lineHeight: 1.2 }}>We're checking<br/>your documents</div>
            <div style={{ fontSize: 13.5, color: "var(--ink-3)", marginTop: 11, lineHeight: 1.6 }}>
              Submitted yesterday at 4:12 PM. Most applications are decided within one working day — we'll notify you either way, so there's no need to keep checking.
            </div>
          </div>
          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 12 }}>WHAT WE'VE GOT</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              {[["Identity · NIC", "approved"], ["Driving licence", "pending"], [`Vehicle · ${MY_VEHICLE.make}`, "pending"]].map(([l, st]) => (
                <div key={l} style={{ display: "flex", alignItems: "center", gap: 11 }}>
                  <Icon name={STATUS_META[st].icon} size={17} color={STATUS_META[st].c} strokeWidth={2.4}/>
                  <div style={{ fontSize: 13, fontWeight: 600, flex: 1 }}>{l}</div>
                  <StatusBadge status={st}/>
                </div>
              ))}
            </div>
          </div>
          <Banner kind="good" icon="users" title="You can still ride" body="Your passenger side works exactly as before. Nothing about this application affects it."/>
        </div>
        <div style={{ padding: "12px 20px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">Back to riding</button>
          <button className="rs-btn ghost full" style={{ marginTop: 10 }}>Message support</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D06 · VEHICLES ═══════════
function DvVehiclesScreen() {
  return (
    <Phone label="D06 Vehicles">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Your vehicles"/>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 11 }} className="rs-scroll">
          <div className="rs-card" style={{ padding: 15, border: "1.5px solid var(--mode-drive)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <div style={{ width: 44, height: 44, borderRadius: 14, background: "var(--mode-drive-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                <Icon name="car" size={21} color="var(--mode-drive-ink)"/>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 15, fontWeight: 800 }}>{MY_VEHICLE.make}</div>
                <div className="tab" style={{ fontSize: 12, color: "var(--ink-3)", marginTop: 2 }}>{MY_VEHICLE.colour} · {MY_VEHICLE.plate}</div>
              </div>
              <StatusBadge status="approved" label="IN USE"/>
            </div>
            <div style={{ display: "flex", gap: 7, marginTop: 12, flexWrap: "wrap" }}>
              <span className="rs-chip" style={{ height: 26 }}><Icon name="users" size={12}/> {MY_VEHICLE.seats} seats</span>
              <span className="rs-chip" style={{ height: 26 }}><Icon name="check" size={12}/> Registration valid</span>
              <span className="rs-chip" style={{ height: 26, background: "var(--status-expiring-soft)", color: "var(--status-expiring-ink)", borderColor: "transparent" }}>
                <Icon name="alert" size={12}/> Insurance {MY_VEHICLE.insuranceDaysLeft}d
              </span>
            </div>
            <div style={{ height: 1, background: "var(--line)", margin: "13px 0" }}/>
            <div style={{ display: "flex", gap: 9 }}>
              <button className="rs-btn soft" style={{ flex: 1, height: 44, fontSize: 13 }}>Renew insurance</button>
              <button className="rs-btn ghost" style={{ flex: 1, height: 44, fontSize: 13 }}>Edit details</button>
            </div>
          </div>
          <button className="rs-btn ghost full" style={{ height: 48 }}><Icon name="plus" size={17}/> Add another vehicle</button>
          <Banner kind="info" icon="car" title="One vehicle per trip" body="If you drive more than one car, pick which one you're using when you publish a trip."/>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ D07 · ADD VEHICLE ═══════════
function DvAddVehicleScreen() {
  return (
    <Phone label="D07 Add vehicle">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Add a vehicle"/>
        <div style={{ flex: 1, overflow: "auto", padding: "18px 20px 16px", display: "flex", flexDirection: "column", gap: 14 }} className="rs-scroll">
          {[["MAKE AND MODEL", "Toyota Vitz", false], ["COLOUR", "Silver", false], ["NUMBER PLATE", "WP CAF-2290", false], ["YEAR", "2016", false]].map(([l, v]) => (
            <div key={l}>
              <div className="rs-section-label" style={{ marginBottom: 7 }}>{l}</div>
              <div style={{ minHeight: 52, padding: "0 15px", borderRadius: 15, background: "var(--surface)", border: "1.5px solid var(--line)", display: "flex", alignItems: "center", fontSize: 14.5, fontWeight: 600 }}>{v}</div>
            </div>
          ))}
          <div>
            <div className="rs-section-label" style={{ marginBottom: 9 }}>PASSENGER SEATS YOU'LL OFFER</div>
            <div style={{ display: "flex", gap: 9 }}>
              {[1, 2, 3, 4].map(n => (
                <button key={n} style={{
                  flex: 1, minHeight: 52, borderRadius: 15, fontSize: 16, fontWeight: 800,
                  background: n === 4 ? "var(--mode-drive)" : "var(--surface)",
                  color: n === 4 ? "var(--on-bright-fill)" : "var(--ink)",
                  border: `1.5px solid ${n === 4 ? "var(--mode-drive)" : "var(--line)"}`,
                }}>{n}</button>
              ))}
            </div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 8, lineHeight: 1.45 }}>Not counting your own seat. You can offer fewer on any individual trip.</div>
          </div>
          <div>
            <div className="rs-section-label" style={{ marginBottom: 9 }}>PAPERS</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 9 }}>
              {["Registration", "Insurance certificate"].map(l => (
                <div key={l} style={{ padding: 13, borderRadius: 15, background: "var(--surface)", border: "1.5px dashed var(--line-2)", display: "flex", alignItems: "center", gap: 11 }}>
                  <div style={{ width: 36, height: 36, borderRadius: 11, background: "var(--bg-soft)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                    <Icon name="plus" size={17} color="var(--ink-3)"/>
                  </div>
                  <div style={{ fontSize: 13.5, fontWeight: 700, flex: 1 }}>{l}</div>
                  <div className="rs-btn" style={{ height: 44, padding: "0 16px", fontSize: 12.5, flexShrink: 0, background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Add</div>
                </div>
              ))}
            </div>
          </div>
        </div>
        <div style={{ padding: "12px 20px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn full" style={{ background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Submit for review</button>
          <div style={{ textAlign: "center", fontSize: 11, color: "var(--ink-3)", marginTop: 9 }}>You can't publish trips with this car until it's approved.</div>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, { DvBecomeScreen, DvKycScreen, DvKycPendingScreen, DvVehiclesScreen, DvAddVehicleScreen, KYC_STEPS });
