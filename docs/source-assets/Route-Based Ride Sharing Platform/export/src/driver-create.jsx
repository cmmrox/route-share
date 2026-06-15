// driver-create.jsx — create trip 3-step flow (map-first), trip published

// ═══════════════════════════════════════════════════════════════════
// STEP 1 — Route on map
// ═══════════════════════════════════════════════════════════════════
function DrCreate1Screen() {
  return (
    <Phone label="D13 Create · Route">
      <div style={{ height: "100%", position: "relative" }}>
        <MapBackdrop pickupLabel="Start" dropLabel="End"/>
        {/* Top bar */}
        <div style={{ position: "absolute", top: 12, left: 12, right: 12, zIndex: 5, display: "flex", gap: 8 }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "#fff", display: "inline-flex", alignItems: "center", justifyContent: "center", boxShadow: "var(--shadow-md)" }}>
            <Icon name="close" size={20}/>
          </button>
          <div style={{ flex: 1, padding: "10px 14px", background: "#fff", borderRadius: 12, boxShadow: "var(--shadow-md)" }}>
            <div style={{ fontSize: 10, color: "var(--ink-3)", fontWeight: 700, letterSpacing: ".1em" }}>STEP 1 OF 3 · DRAW YOUR ROUTE</div>
            <Stepper step={1} total={3}/>
          </div>
        </div>

        {/* Sheet */}
        <div style={{ position: "absolute", left: 0, right: 0, bottom: 0, zIndex: 5 }} className="rs-sheet">
          <div className="rs-sheet-grab"/>
          <div style={{ padding: "4px 20px 16px" }}>
            <div className="rs-display" style={{ fontSize: 20 }}>Where are you driving?</div>

            <div style={{ marginTop: 14, padding: "14px 14px 14px 16px", background: "var(--bg-soft)", borderRadius: 14, position: "relative" }}>
              <div style={{ position: "absolute", left: 22, top: 22, bottom: 22, width: 2, background: "var(--line-2)", backgroundImage: "linear-gradient(var(--line-2) 60%, transparent 60%)", backgroundSize: "2px 4px" }}/>
              <div style={{ display: "flex", alignItems: "center", gap: 12, paddingLeft: 18 }}>
                <div style={{ position: "absolute", left: 16, width: 14, height: 14, borderRadius: 7, background: "var(--teal)", border: "2px solid #fff", boxShadow: "0 1px 4px rgba(0,0,0,.1)" }}/>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 11, color: "var(--ink-3)", fontWeight: 700 }}>FROM</div>
                  <div style={{ fontSize: 14, fontWeight: 700 }}>Rajagiriya — 42 Pereira Mw</div>
                </div>
              </div>
              <div style={{ height: 10 }}/>
              <div style={{ display: "flex", alignItems: "center", gap: 12, paddingLeft: 18 }}>
                <div style={{ position: "absolute", left: 16, width: 14, height: 14, background: "var(--accent)", border: "2px solid #fff", boxShadow: "0 1px 4px rgba(0,0,0,.1)" }}/>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 11, color: "var(--ink-3)", fontWeight: 700 }}>TO</div>
                  <div style={{ fontSize: 14, fontWeight: 700 }}>Colombo Fort — World Trade Center</div>
                </div>
              </div>
            </div>

            <div style={{ marginTop: 12, display: "flex", gap: 8, fontSize: 11, color: "var(--ink-3)", fontWeight: 600 }}>
              <div className="rs-chip teal" style={{ height: 28 }}><Icon name="leaf" size={11}/> 11.4 km</div>
              <div className="rs-chip" style={{ height: 28 }}><Icon name="clock" size={11}/> ~24 min</div>
              <div className="rs-chip" style={{ height: 28 }}><Icon name="route" size={11}/> Via Baseline Rd</div>
            </div>

            <div style={{ marginTop: 14, padding: 12, background: "var(--accent-soft)", borderRadius: 12, fontSize: 12, color: "var(--accent-2)", display: "flex", gap: 8 }}>
              <Icon name="route" size={14} color="var(--accent-2)"/>
              <div style={{ flex: 1 }}>This route passes <b>Narahenpita</b>, <b>Thunmulla</b> & <b>Bambalapitiya</b> — high match potential.</div>
            </div>

            <button className="rs-btn accent full" style={{ marginTop: 16 }}>Continue to schedule</button>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// STEP 2 — Schedule (one-time / recurring)
// ═══════════════════════════════════════════════════════════════════
function DrCreate2Screen() {
  const [recurring, setRecurring] = React.useState(true);
  const [days, setDays] = React.useState(["Mon", "Tue", "Wed", "Thu", "Fri"]);
  return (
    <Phone label="D14 Create · Schedule">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "12px 20px 14px", background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 14 }}>
            <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="back" size={20}/>
            </button>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 11, fontWeight: 700, color: "var(--ink-3)", letterSpacing: ".12em" }}>STEP 2 OF 3</div>
              <div style={{ fontSize: 17, fontWeight: 700 }}>When are you going?</div>
            </div>
          </div>
          <Stepper step={2} total={3}/>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "18px 20px" }} className="rs-scroll">
          {/* Recurring toggle */}
          <div style={{ display: "flex", padding: 4, background: "var(--bg-soft)", borderRadius: 14, marginBottom: 18 }}>
            <div onClick={() => setRecurring(false)} style={{ flex: 1, height: 40, borderRadius: 11, background: !recurring ? "var(--surface)" : "transparent", color: !recurring ? "var(--ink)" : "var(--ink-3)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 13, fontWeight: 700, boxShadow: !recurring ? "var(--shadow-sm)" : "none" }}>
              One-time
            </div>
            <div onClick={() => setRecurring(true)} style={{ flex: 1, height: 40, borderRadius: 11, background: recurring ? "var(--surface)" : "transparent", color: recurring ? "var(--ink)" : "var(--ink-3)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 13, fontWeight: 700, boxShadow: recurring ? "var(--shadow-sm)" : "none" }}>
              Recurring
            </div>
          </div>

          <div className="rs-section-label" style={{ marginBottom: 10 }}>{recurring ? "DAYS OF WEEK" : "DATE"}</div>
          {recurring ? (
            <DayPicker days={days} onChange={setDays}/>
          ) : (
            <Field label="Trip date" value="Wed, 24 Apr 2026" icon="calendar"/>
          )}
          {recurring && (
            <div style={{ marginTop: 8, fontSize: 11, color: "var(--ink-3)" }}>Repeats {days.length} days a week</div>
          )}

          <div className="rs-section-label" style={{ margin: "22px 0 10px" }}>DEPARTURE TIME</div>
          <div style={{ padding: 20, background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 16, textAlign: "center" }}>
            <div className="rs-display tab" style={{ fontSize: 48, fontWeight: 600, color: "var(--accent)", letterSpacing: "-0.02em" }}>08 : 00 <span style={{ fontSize: 22, color: "var(--ink-3)" }}>AM</span></div>
            <div style={{ marginTop: 10, fontSize: 12, color: "var(--ink-3)" }}>Tap to change · Buffer ± 10 min</div>
          </div>

          <div className="rs-section-label" style={{ margin: "22px 0 10px" }}>BOOKING WINDOW</div>
          <div style={{ display: "flex", gap: 10 }}>
            <Field label="Open from" value="Anytime"/>
            <Field label="Close before" value="30 min"/>
          </div>

          {recurring && (
            <div style={{ marginTop: 16, padding: 14, background: "var(--accent-soft)", borderRadius: 14, display: "flex", gap: 10 }}>
              <Icon name="calendar" size={16} color="var(--accent-2)"/>
              <div style={{ fontSize: 12, color: "var(--accent-2)", lineHeight: 1.4 }}>
                Will create <b>5 trips per week</b> until you pause it. Edit individual days from the schedule view.
              </div>
            </div>
          )}
        </div>

        <div style={{ padding: "12px 20px 14px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">Continue to seats & price</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// STEP 3 — Seats, price/km, booking rules
// ═══════════════════════════════════════════════════════════════════
function DrCreate3Screen({ bookingMode = "instant" }) {
  return (
    <Phone label="D15 Create · Seats & price">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "12px 20px 14px", background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 14 }}>
            <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="back" size={20}/>
            </button>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 11, fontWeight: 700, color: "var(--ink-3)", letterSpacing: ".12em" }}>STEP 3 OF 3</div>
              <div style={{ fontSize: 17, fontWeight: 700 }}>Seats & price</div>
            </div>
          </div>
          <Stepper step={3} total={3}/>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "18px 20px" }} className="rs-scroll">
          <div className="rs-section-label" style={{ marginBottom: 10 }}>SEATS TO OFFER</div>
          <div style={{ padding: 18, background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 16, display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <div>
              <div style={{ fontSize: 13, color: "var(--ink-3)", fontWeight: 600 }}>Toyota Aqua · 4 seats total</div>
              <div className="rs-display" style={{ fontSize: 32, fontWeight: 600, marginTop: 4 }}>3 seats <span style={{ fontSize: 16, color: "var(--ink-3)" }}>open</span></div>
            </div>
            <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
              <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="minus" size={18}/>
              </button>
              <div className="rs-display tab" style={{ fontSize: 24, fontWeight: 600, minWidth: 28, textAlign: "center" }}>3</div>
              <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--accent)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="plus" size={18} color="#fff" strokeWidth={2.4}/>
              </button>
            </div>
          </div>

          <div className="rs-section-label" style={{ margin: "22px 0 10px" }}>PRICE PER KILOMETRE</div>
          <div style={{ padding: 18, background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 16 }}>
            <div style={{ display: "flex", alignItems: "baseline", justifyContent: "space-between" }}>
              <div>
                <div className="rs-display tab" style={{ fontSize: 36, fontWeight: 600, color: "var(--accent)" }}>LKR 50</div>
                <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 2 }}>per kilometre per seat</div>
              </div>
              <div className="rs-chip teal" style={{ height: 26 }}>Suggested</div>
            </div>
            <div style={{ marginTop: 14, height: 6, background: "var(--bg-soft)", borderRadius: 3, position: "relative" }}>
              <div style={{ position: "absolute", left: "30%", right: 0, top: 0, bottom: 0, background: "linear-gradient(90deg, var(--accent) 0%, var(--line-2) 100%)", borderRadius: 3 }}/>
              <div style={{ position: "absolute", left: "30%", top: -5, width: 16, height: 16, borderRadius: 8, background: "#fff", border: "3px solid var(--accent)", transform: "translateX(-50%)" }}/>
            </div>
            <div style={{ display: "flex", justifyContent: "space-between", marginTop: 8, fontSize: 11, color: "var(--ink-4)", fontWeight: 600 }}>
              <span>LKR 35</span><span>LKR 80</span>
            </div>
          </div>

          <div style={{ marginTop: 16, padding: 14, background: "var(--teal-soft)", borderRadius: 14 }}>
            <div style={{ fontSize: 11, color: "var(--teal)", fontWeight: 700, letterSpacing: ".08em" }}>YOU COULD EARN PER FULL TRIP</div>
            <div className="rs-display tab" style={{ fontSize: 24, color: "var(--teal)", marginTop: 4 }}>LKR 1,540 <span style={{ fontSize: 14 }}>after fees</span></div>
            <div style={{ fontSize: 11, color: "var(--teal)", opacity: .75 }}>3 seats × 11.4 km × LKR 50 − 10% commission</div>
          </div>

          <div className="rs-section-label" style={{ margin: "22px 0 10px" }}>BOOKING MODE</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            <BookingMode active={bookingMode === "instant"} icon="check" label="Instant booking" sub="Passengers join without your approval — get more bookings"/>
            <BookingMode active={bookingMode === "manual"} icon="thumb" label="I approve each request" sub="You see passenger details before they're confirmed"/>
          </div>
        </div>

        <div style={{ padding: "12px 20px 14px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">Publish trip</button>
        </div>
      </div>
    </Phone>
  );
}

function BookingMode({ active, icon, label, sub }) {
  return (
    <div style={{ padding: 14, background: active ? "var(--accent-soft)" : "var(--surface)", border: `1.5px solid ${active ? "var(--accent)" : "var(--line)"}`, borderRadius: 14, display: "flex", alignItems: "center", gap: 12 }}>
      <div style={{ width: 36, height: 36, borderRadius: 12, background: active ? "#fff" : "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
        <Icon name={icon} size={18} color={active ? "var(--accent-2)" : "var(--ink-2)"} strokeWidth={2.4}/>
      </div>
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 14, fontWeight: 700 }}>{label}</div>
        <div style={{ fontSize: 11, color: "var(--ink-3)" }}>{sub}</div>
      </div>
      <div style={{ width: 22, height: 22, borderRadius: 11, border: `2px solid ${active ? "var(--accent)" : "var(--line-2)"}`, background: active ? "var(--accent)" : "transparent", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
        {active && <Icon name="check" size={12} color="#fff" strokeWidth={3}/>}
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════
// TRIP PUBLISHED — share link, copy, social
// ═══════════════════════════════════════════════════════════════════
function DrPublishedScreen() {
  return (
    <Phone label="D16 Trip Published">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "20px 20px 28px", background: "var(--success)", color: "#fff", borderBottomLeftRadius: 28, borderBottomRightRadius: 28, position: "relative", overflow: "hidden" }}>
          <div style={{ position: "absolute", right: -40, top: -40, width: 160, height: 160, borderRadius: 80, background: "rgba(255,255,255,.18)" }}/>
          <div style={{ position: "absolute", left: -60, bottom: -80, width: 200, height: 200, borderRadius: 100, background: "rgba(255,255,255,.1)" }}/>
          <div style={{ position: "relative" }}>
            <div style={{ width: 64, height: 64, borderRadius: 32, background: "rgba(255,255,255,.22)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="check" size={36} color="#fff" strokeWidth={2.6}/>
            </div>
            <div className="rs-display" style={{ fontSize: 30, marginTop: 16, lineHeight: 1.05 }}>You're live!</div>
            <div style={{ fontSize: 14, opacity: .9, marginTop: 4 }}>Your trip is now visible to passengers along your route.</div>
          </div>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "18px 20px" }} className="rs-scroll">
          <div className="rs-card" style={{ padding: 16 }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
              <div>
                <div style={{ fontSize: 11, color: "var(--ink-3)", fontWeight: 700, letterSpacing: ".08em" }}>DEPARTS</div>
                <div className="rs-display" style={{ fontSize: 22 }}>Wed, 8:00 AM</div>
              </div>
              <span className="rs-chip accent" style={{ height: 24 }}>Recurring · Weekdays</span>
            </div>
            <div className="rs-divider" style={{ margin: "12px 0" }}/>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <div style={{ width: 10, height: 10, borderRadius: 5, background: "var(--teal)" }}/>
              <div style={{ fontSize: 13, fontWeight: 600 }}>Rajagiriya — 42 Pereira Mw</div>
            </div>
            <div style={{ marginLeft: 4, paddingLeft: 10, borderLeft: "2px dashed var(--line-2)", height: 14 }}/>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <div style={{ width: 10, height: 10, background: "var(--accent)" }}/>
              <div style={{ fontSize: 13, fontWeight: 600 }}>Colombo Fort — WTC</div>
            </div>
            <div className="rs-divider" style={{ margin: "12px 0" }}/>
            <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, color: "var(--ink-3)" }}>
              <span>3 seats · LKR 50/km · Instant booking</span>
              <span style={{ color: "var(--match-full)", fontWeight: 700 }}>~LKR 1,540 / trip</span>
            </div>
          </div>

          <div className="rs-section-label" style={{ margin: "22px 0 10px" }}>SHARE THIS TRIP</div>
          <div style={{ padding: 14, background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 14, display: "flex", alignItems: "center", gap: 10 }}>
            <div style={{ flex: 1, fontFamily: "var(--font-mono)", fontSize: 12, color: "var(--ink-2)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>routeshare.lk/r/sa-8am-wkd</div>
            <button style={{ padding: "8px 12px", background: "var(--ink)", color: "var(--bg)", fontSize: 11, fontWeight: 700, borderRadius: 8 }}>Copy</button>
          </div>
          <div style={{ marginTop: 10, display: "flex", gap: 8 }}>
            <button className="rs-btn ghost" style={{ flex: 1, height: 44, fontSize: 12 }}><Icon name="share" size={14}/> WhatsApp</button>
            <button className="rs-btn ghost" style={{ flex: 1, height: 44, fontSize: 12 }}><Icon name="mail" size={14}/> SMS</button>
            <button className="rs-btn ghost" style={{ flex: 1, height: 44, fontSize: 12 }}><Icon name="receipt" size={14}/> QR</button>
          </div>

          <div style={{ marginTop: 18, padding: 14, background: "var(--bg-soft)", borderRadius: 14, fontSize: 11, color: "var(--ink-3)", lineHeight: 1.5 }}>
            Tip: Sharing your trip link with regular commuters and workplaces gets you more recurring riders.
          </div>
        </div>

        <div style={{ padding: "12px 20px 14px", background: "var(--surface)", borderTop: "1px solid var(--line)", display: "flex", gap: 10 }}>
          <button className="rs-btn soft" style={{ flex: 1 }}>Publish another</button>
          <button className="rs-btn accent" style={{ flex: 1.4 }}>Back to home</button>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, { DrCreate1Screen, DrCreate2Screen, DrCreate3Screen, DrPublishedScreen });
