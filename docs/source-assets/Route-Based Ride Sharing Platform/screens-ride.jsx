// screens-ride.jsx — ride detail, seat selection, payment, booking, tracking, drop-off, receipt, rating

// ═══════════════════════════════════════════════════════════════════
// RIDE DETAIL
// ═══════════════════════════════════════════════════════════════════
function RideDetailScreen() {
  const r = MOCK_RIDES[0];
  return (
    <Phone label="10 Ride Detail">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ height: 220, position: "relative", flexShrink: 0 }}>
          <MapBackdrop pickupLabel="Pick up" dropLabel="Drop off"/>
          <button style={{ position: "absolute", top: 12, left: 12, width: 40, height: 40, borderRadius: 20, background: "#fff", display: "inline-flex", alignItems: "center", justifyContent: "center", boxShadow: "var(--shadow-md)" }}>
            <Icon name="back" size={20}/>
          </button>
          <button style={{ position: "absolute", top: 12, right: 12, width: 40, height: 40, borderRadius: 20, background: "#fff", display: "inline-flex", alignItems: "center", justifyContent: "center", boxShadow: "var(--shadow-md)" }}>
            <Icon name="share" size={18}/>
          </button>
        </div>
        <div style={{ flex: 1, overflow: "auto", marginTop: -28, background: "var(--surface)", borderTopLeftRadius: 28, borderTopRightRadius: 28, position: "relative", zIndex: 2 }} className="rs-scroll">
          <div className="rs-sheet-grab"/>
          <div style={{ padding: "4px 20px 16px", display: "flex", alignItems: "center", gap: 14 }}>
            <MatchRing value={r.match} size={64}/>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 12, color: "var(--accent-2)", fontWeight: 700, letterSpacing: ".04em" }}>BEST MATCH · FULL ROUTE</div>
              <div className="rs-display" style={{ fontSize: 20, lineHeight: 1.15 }}>{r.overlap}</div>
            </div>
          </div>

          <div style={{ padding: "0 20px" }}>
            <div className="rs-card" style={{ padding: 14, display: "flex", alignItems: "center", gap: 12, background: "var(--bg-soft)", border: "none" }}>
              <Avatar name={r.driver} size={48}/>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 700, fontSize: 15 }}>{r.driver}</div>
                <div style={{ fontSize: 12, color: "var(--ink-3)", display: "flex", alignItems: "center", gap: 4 }}>
                  <Icon name="star" size={12} color="var(--warn)"/> {r.rating} · {r.trips} trips · <Icon name="shield" size={10} color="var(--teal)"/> Verified
                </div>
              </div>
              <button style={{ width: 40, height: 40, borderRadius: 20, background: "#fff", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="phone" size={18} color="var(--teal)"/>
              </button>
            </div>
          </div>

          <div style={{ padding: "16px 20px" }}>
            <div className="rs-section-label" style={{ marginBottom: 10 }}>TRIP TIMELINE</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 0, position: "relative" }}>
              <Timeline step="Driver departs Rajagiriya" sub="8:00 AM · published daily" color="var(--ink-3)" dotOutline/>
              <Timeline step="Pick you up · Narahenpita Jn" sub="8:04 AM · ETA 3 min" color="var(--teal)"/>
              <Timeline step="Drop you off · Bambalapitiya" sub="8:22 AM · 6.2 km · full overlap" color="var(--accent)"/>
              <Timeline step="Driver continues to Colombo Fort" sub="8:34 AM · off your route" color="var(--ink-3)" dotOutline last/>
            </div>
          </div>

          <div style={{ padding: "4px 20px 16px" }}>
            <div className="rs-section-label" style={{ marginBottom: 10 }}>VEHICLE</div>
            <div className="rs-card" style={{ padding: 14, display: "flex", alignItems: "center", gap: 12 }}>
              <div style={{ width: 56, height: 56, borderRadius: 14, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="car" size={28}/>
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 700, fontSize: 14 }}>{r.car}</div>
                <div style={{ fontSize: 12, color: "var(--ink-3)" }}>{r.plate} · 4-seater · AC</div>
              </div>
              <div className="rs-chip teal"><Icon name="leaf" size={12}/> Hybrid</div>
            </div>
          </div>

          <div style={{ padding: "0 20px 120px" }}>
            <div className="rs-section-label" style={{ marginBottom: 10 }}>FARE ESTIMATE</div>
            <FareRow label="Base fare · 6.2 km × LKR 50" val="310"/>
            <FareRow label="Route match discount" val="−45" pos/>
            <FareRow label="Platform fee" val="27" muted/>
            <div className="rs-divider" style={{ margin: "8px 0" }}/>
            <FareRow label="Estimated total" val="292" strong/>
            <div style={{ fontSize: 11, color: "var(--ink-4)", marginTop: 6 }}>Final fare is billed by actual kilometres. If you exit early, you pay less.</div>
          </div>
        </div>

        <div style={{ padding: "12px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)", display: "flex", gap: 10, alignItems: "center", position: "absolute", left: 0, right: 0, bottom: 0, zIndex: 5 }}>
          <div>
            <div style={{ fontSize: 11, color: "var(--ink-3)", fontWeight: 600 }}>FROM</div>
            <div className="rs-display" style={{ fontSize: 22 }}>LKR 292</div>
          </div>
          <button className="rs-btn accent" style={{ flex: 1, height: 52 }}>Book this ride <Icon name="arrow" size={16} color="#fff"/></button>
        </div>
      </div>
    </Phone>
  );
}

function Timeline({ step, sub, color = "var(--ink)", dotOutline, last }) {
  return (
    <div style={{ display: "flex", gap: 12, alignItems: "flex-start", position: "relative", paddingBottom: last ? 0 : 14 }}>
      <div style={{ width: 20, display: "flex", flexDirection: "column", alignItems: "center", flexShrink: 0 }}>
        <div style={{ width: 12, height: 12, borderRadius: 6, background: dotOutline ? "var(--bg)" : color, border: `2px solid ${color}`, zIndex: 2 }}/>
        {!last && <div style={{ width: 2, flex: 1, background: "var(--line-2)", marginTop: 2, minHeight: 18, backgroundImage: "linear-gradient(var(--line-2) 60%, transparent 60%)", backgroundSize: "2px 4px" }}/>}
      </div>
      <div style={{ flex: 1, paddingTop: -2 }}>
        <div style={{ fontSize: 14, fontWeight: 600, color: "var(--ink)" }}>{step}</div>
        <div style={{ fontSize: 12, color: "var(--ink-3)" }}>{sub}</div>
      </div>
    </div>
  );
}

function FareRow({ label, val, strong, muted, pos }) {
  return (
    <div style={{ display: "flex", justifyContent: "space-between", padding: "5px 0" }}>
      <span style={{ fontSize: strong ? 15 : 13, color: strong ? "var(--ink)" : muted ? "var(--ink-4)" : "var(--ink-2)", fontWeight: strong ? 700 : 500 }}>{label}</span>
      <span className="tab" style={{ fontSize: strong ? 17 : 13, color: pos ? "var(--success)" : strong ? "var(--ink)" : "var(--ink-2)", fontWeight: strong ? 700 : 600, fontFamily: strong ? "var(--font-display)" : "inherit" }}>
        {pos ? "" : "LKR "}{val}
      </span>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════
// SEAT SELECTION
// ═══════════════════════════════════════════════════════════════════
function SeatSelectScreen({ seatCount = 1 }) {
  const selected = Array.from({ length: seatCount }, (_, i) => i);
  return (
    <Phone label="11 Seat Select">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "14px 20px 8px", display: "flex", alignItems: "center", gap: 12 }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="back" size={20}/>
          </button>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 11, color: "var(--ink-3)", fontWeight: 700, letterSpacing: ".12em" }}>STEP 1 OF 3</div>
            <div style={{ fontSize: 16, fontWeight: 700 }}>Choose your seats</div>
          </div>
        </div>
        <div style={{ height: 4, background: "var(--bg-soft)", margin: "0 20px", borderRadius: 2 }}>
          <div style={{ width: "33%", height: "100%", background: "var(--accent)", borderRadius: 2 }}/>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "30px 20px" }} className="rs-scroll">
          <div className="rs-display" style={{ fontSize: 22, textAlign: "center" }}>3 of 4 seats available</div>
          <div style={{ fontSize: 13, color: "var(--ink-3)", textAlign: "center", marginTop: 4 }}>Tap a free seat to pick it</div>

          <div style={{ marginTop: 28, display: "flex", justifyContent: "center" }}>
            <SeatPlan taken={[2]} selected={selected} capacity={4}/>
          </div>

          <div style={{ marginTop: 28, display: "flex", justifyContent: "center", gap: 14 }}>
            <LegendDot color="var(--bg-soft)" label="Free" border="var(--line-2)"/>
            <LegendDot color="var(--accent)" label="You"/>
            <LegendDot color="var(--ink)" label="Taken"/>
          </div>

          <div style={{ marginTop: 28, padding: 14, background: "var(--surface)", borderRadius: 14, border: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 12 }}>
            <div style={{ width: 40, height: 40, borderRadius: 20, background: "var(--accent-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="users" size={18} color="var(--accent-2)"/>
            </div>
            <div style={{ flex: 1, fontSize: 12, color: "var(--ink-2)", lineHeight: 1.4 }}>
              <b>Travelling with someone?</b> Book up to 3 seats on this ride. Each seat costs the same.
            </div>
          </div>
        </div>

        <div style={{ padding: "14px 20px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 10 }}>
            <span style={{ fontSize: 13, color: "var(--ink-3)" }}>{seatCount} seat{seatCount > 1 ? "s" : ""} × LKR 292</span>
            <span className="rs-display tab" style={{ fontSize: 20, fontWeight: 600 }}>LKR {292 * seatCount}</span>
          </div>
          <button className="rs-btn accent full">Continue to payment</button>
        </div>
      </div>
    </Phone>
  );
}
function LegendDot({ color, label, border }) {
  return <div style={{ display: "inline-flex", alignItems: "center", gap: 6, fontSize: 12, color: "var(--ink-3)", fontWeight: 600 }}>
    <div style={{ width: 14, height: 18, borderRadius: "4px 4px 3px 3px", background: color, border: border ? `1.5px solid ${border}` : "none" }}/> {label}
  </div>;
}

// ═══════════════════════════════════════════════════════════════════
// PAYMENT
// ═══════════════════════════════════════════════════════════════════
function PaymentScreen() {
  return (
    <Phone label="12 Payment">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "14px 20px 8px", display: "flex", alignItems: "center", gap: 12 }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="back" size={20}/>
          </button>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 11, color: "var(--ink-3)", fontWeight: 700, letterSpacing: ".12em" }}>STEP 2 OF 3</div>
            <div style={{ fontSize: 16, fontWeight: 700 }}>Choose payment</div>
          </div>
        </div>
        <div style={{ height: 4, background: "var(--bg-soft)", margin: "0 20px", borderRadius: 2 }}>
          <div style={{ width: "66%", height: "100%", background: "var(--accent)", borderRadius: 2 }}/>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "20px 20px 0" }} className="rs-scroll">
          <div className="rs-section-label" style={{ marginBottom: 10 }}>YOUR METHODS</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            <PayMethod icon="card" label="•••• 4429" sub="HSBC Visa · expires 08/28" selected/>
            <PayMethod icon="cash" label="Cash" sub="Pay the driver at drop-off" brand="teal"/>
            <PayMethod icon="wallet" label="RouteShare wallet" sub="Balance: LKR 1,250"/>
          </div>
          <button style={{ marginTop: 12, width: "100%", padding: "14px", background: "transparent", border: "1.5px dashed var(--line-2)", borderRadius: 14, display: "flex", alignItems: "center", justifyContent: "center", gap: 8, fontWeight: 600, color: "var(--ink-2)" }}>
            <Icon name="plus" size={16}/> Add new card
          </button>

          <div className="rs-section-label" style={{ margin: "20px 0 10px" }}>FARE BREAKDOWN</div>
          <div className="rs-card" style={{ padding: 14 }}>
            <FareRow label="Ride · 6.2 km (full route)" val="310"/>
            <FareRow label="Route match discount · 100%" val="−45" pos/>
            <FareRow label="Platform fee · 10%" val="27" muted/>
            <div className="rs-divider" style={{ margin: "8px 0" }}/>
            <FareRow label="Total (pre-authorized)" val="292" strong/>
            <div style={{ marginTop: 10, padding: "10px 12px", background: "var(--teal-soft)", borderRadius: 10, fontSize: 11, color: "var(--teal)", lineHeight: 1.4, display: "flex", gap: 8 }}>
              <Icon name="lock" size={14} color="var(--teal)"/>
              <div>We pre-authorize LKR 292 on your card. You're charged the actual kilometres after drop-off.</div>
            </div>
          </div>
        </div>

        <div style={{ padding: "14px 20px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">Confirm & book · LKR 292</button>
        </div>
      </div>
    </Phone>
  );
}

function PayMethod({ icon, label, sub, selected, brand }) {
  return (
    <div style={{ padding: 14, borderRadius: 14, background: selected ? "var(--accent-soft)" : "var(--surface)", border: `1.5px solid ${selected ? "var(--accent)" : "var(--line)"}`, display: "flex", alignItems: "center", gap: 12 }}>
      <div style={{ width: 44, height: 44, borderRadius: 12, background: brand === "teal" ? "var(--teal-soft)" : selected ? "#fff" : "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
        <Icon name={icon} size={20} color={brand === "teal" ? "var(--teal)" : "var(--ink)"}/>
      </div>
      <div style={{ flex: 1 }}>
        <div style={{ fontWeight: 700, fontSize: 14 }}>{label}</div>
        <div style={{ fontSize: 12, color: "var(--ink-3)" }}>{sub}</div>
      </div>
      <div style={{ width: 22, height: 22, borderRadius: 11, border: `2px solid ${selected ? "var(--accent)" : "var(--line-2)"}`, display: "inline-flex", alignItems: "center", justifyContent: "center", background: selected ? "var(--accent)" : "transparent" }}>
        {selected && <Icon name="check" size={12} color="#fff" strokeWidth={3}/>}
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════
// BOOKING CONFIRMED · waiting
// ═══════════════════════════════════════════════════════════════════
function BookedScreen() {
  const r = MOCK_RIDES[0];
  return (
    <Phone label="13 Booked">
      <div style={{ height: "100%", position: "relative" }}>
        <MapBackdrop pickupLabel="Pick up" dropLabel="Drop off"/>
        {/* Driver moving pin */}
        <div style={{ position: "absolute", left: "28%", top: "35%" }}>
          <SmallCarPin color="var(--ink)" pct={100}/>
        </div>
        {/* Top */}
        <div style={{ position: "absolute", top: 12, left: 12, right: 12, display: "flex", gap: 8, zIndex: 4 }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "#fff", display: "inline-flex", alignItems: "center", justifyContent: "center", boxShadow: "var(--shadow-md)" }}>
            <Icon name="back" size={20}/>
          </button>
          <div style={{ flex: 1 }}/>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "#fff", display: "inline-flex", alignItems: "center", justifyContent: "center", boxShadow: "var(--shadow-md)" }}>
            <Icon name="share" size={18}/>
          </button>
        </div>

        {/* Success toast */}
        <div style={{ position: "absolute", top: 64, left: 16, right: 16, padding: "10px 14px", background: "var(--success)", color: "#fff", borderRadius: 14, display: "flex", alignItems: "center", gap: 10, boxShadow: "var(--shadow-md)", zIndex: 4 }}>
          <Icon name="check" size={18} color="#fff" strokeWidth={3}/>
          <div style={{ flex: 1, fontSize: 13, fontWeight: 600 }}>Seat confirmed. Saman is on the way.</div>
        </div>

        <div style={{ position: "absolute", bottom: 0, left: 0, right: 0, zIndex: 4 }} className="rs-sheet">
          <div className="rs-sheet-grab"/>
          <div style={{ padding: "4px 20px 18px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <div>
                <div style={{ fontSize: 11, color: "var(--accent-2)", fontWeight: 700, letterSpacing: ".08em" }}>ARRIVING IN</div>
                <div className="rs-display" style={{ fontSize: 34, lineHeight: 1 }}>3 min</div>
              </div>
              <div style={{ textAlign: "right" }}>
                <div style={{ fontSize: 11, color: "var(--ink-3)", fontWeight: 700, letterSpacing: ".08em" }}>PICK UP AT</div>
                <div style={{ fontSize: 15, fontWeight: 700 }}>Narahenpita Jn</div>
              </div>
            </div>

            <div className="rs-divider" style={{ margin: "16px 0" }}/>

            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <Avatar name={r.driver} size={48}/>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 15, fontWeight: 700 }}>{r.driver}</div>
                <div style={{ fontSize: 12, color: "var(--ink-3)" }}>{r.car} · <b style={{ color: "var(--ink)" }}>{r.plate}</b></div>
              </div>
              <button style={{ width: 42, height: 42, borderRadius: 21, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="phone" size={18}/>
              </button>
              <button style={{ width: 42, height: 42, borderRadius: 21, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="mail" size={18}/>
              </button>
            </div>

            <div style={{ marginTop: 14, display: "flex", gap: 8 }}>
              <button className="rs-btn soft" style={{ flex: 1, height: 46, fontSize: 13 }}><Icon name="share" size={14}/> Share trip</button>
              <button className="rs-btn soft" style={{ flex: 1, height: 46, fontSize: 13, color: "var(--danger)" }}>Cancel</button>
            </div>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// LIVE TRACKING · in trip
// ═══════════════════════════════════════════════════════════════════
function InTripScreen() {
  const r = MOCK_RIDES[0];
  return (
    <Phone label="14 In Trip">
      <div style={{ height: "100%", position: "relative" }}>
        <MapBackdrop pickupLabel="Picked up" dropLabel="Bambalapitiya"/>
        {/* moving pin */}
        <div style={{ position: "absolute", left: "48%", top: "50%" }}>
          <SmallCarPin color="var(--accent)" pct={100}/>
        </div>
        <div style={{ position: "absolute", top: 12, left: 12, right: 12, display: "flex", justifyContent: "space-between", zIndex: 4 }}>
          <button style={{ height: 40, padding: "0 14px", borderRadius: 20, background: "#fff", display: "inline-flex", alignItems: "center", gap: 6, fontWeight: 700, fontSize: 13, boxShadow: "var(--shadow-md)", whiteSpace: "nowrap" }}>
            <div style={{ width: 8, height: 8, borderRadius: 4, background: "var(--success)", flexShrink: 0, animation: "pulse 1.4s infinite" }}/> IN TRIP
          </button>
          <button style={{ height: 40, padding: "0 14px", borderRadius: 20, background: "var(--danger)", color: "#fff", display: "inline-flex", alignItems: "center", gap: 6, fontWeight: 700, fontSize: 13, boxShadow: "var(--shadow-md)" }}>
            <Icon name="alert" size={14} color="#fff"/> SOS
          </button>
        </div>

        <div style={{ position: "absolute", bottom: 0, left: 0, right: 0, zIndex: 4 }} className="rs-sheet">
          <div className="rs-sheet-grab"/>
          <div style={{ padding: "4px 20px 16px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
              <div>
                <div style={{ fontSize: 11, color: "var(--teal)", fontWeight: 700, letterSpacing: ".08em" }}>ARRIVING</div>
                <div className="rs-display" style={{ fontSize: 30, lineHeight: 1 }}>8:22 AM</div>
                <div style={{ fontSize: 12, color: "var(--ink-3)", whiteSpace: "nowrap" }}>12 min · 4.1 km left</div>
              </div>
              <div style={{ textAlign: "right" }}>
                <div style={{ fontSize: 11, color: "var(--ink-3)", fontWeight: 700, letterSpacing: ".08em" }}>FARE SO FAR</div>
                <div className="rs-display" style={{ fontSize: 22 }}>LKR 195</div>
                <div style={{ fontSize: 11, color: "var(--ink-4)" }}>2.1 km travelled</div>
              </div>
            </div>

            {/* progress */}
            <div style={{ marginTop: 14, height: 8, background: "var(--bg-soft)", borderRadius: 4, position: "relative", overflow: "hidden" }}>
              <div style={{ position: "absolute", inset: 0, width: "34%", background: "var(--accent)", borderRadius: 4 }}/>
            </div>
            <div style={{ display: "flex", justifyContent: "space-between", marginTop: 6, fontSize: 11, color: "var(--ink-3)", fontWeight: 600 }}>
              <span>Narahenpita</span>
              <span>Thunmulla</span>
              <span style={{ color: "var(--accent)" }}>Bambalapitiya</span>
            </div>

            <div className="rs-divider" style={{ margin: "14px 0" }}/>

            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <Avatar name={r.driver} size={42}/>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 14, fontWeight: 700 }}>{r.driver}</div>
                <div style={{ fontSize: 11, color: "var(--ink-3)" }}>{r.plate}</div>
              </div>
              <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="phone" size={16}/>
              </button>
              <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="mail" size={16}/>
              </button>
            </div>

            <button className="rs-btn ghost full" style={{ marginTop: 12, height: 46, fontSize: 13, color: "var(--accent-2)", borderColor: "var(--accent)" }}>
              Get off earlier ›
            </button>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// GET OFF EARLY CONFIRMATION
// ═══════════════════════════════════════════════════════════════════
function ExitEarlyScreen() {
  return (
    <Phone label="15 Exit Early">
      <div style={{ height: "100%", position: "relative" }}>
        <div style={{ position: "absolute", inset: 0, background: "rgba(20,10,5,.4)", zIndex: 2 }}/>
        <MapBackdrop pickupLabel="Pickup" dropLabel="Drop here"/>
        <div style={{ position: "absolute", left: 0, right: 0, bottom: 0, zIndex: 5 }} className="rs-sheet">
          <div className="rs-sheet-grab"/>
          <div style={{ padding: "4px 22px 22px" }}>
            <div className="rs-display" style={{ fontSize: 24, lineHeight: 1.15 }}>Drop off at<br/>Kollupitiya Jn?</div>
            <div style={{ fontSize: 13, color: "var(--ink-3)", marginTop: 6 }}>You'll leave the ride before the planned drop-off. Saman will continue on.</div>

            <div style={{ marginTop: 20, padding: 16, background: "var(--bg-soft)", borderRadius: 14 }}>
              <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 12 }}>
                <span style={{ fontSize: 12, color: "var(--ink-3)", fontWeight: 600 }}>Distance travelled</span>
                <span style={{ fontSize: 13, fontWeight: 700 }}>3.8 km <span style={{ color: "var(--ink-4)", fontWeight: 500 }}>of 6.2</span></span>
              </div>
              <div style={{ height: 6, background: "var(--surface)", borderRadius: 3 }}>
                <div style={{ width: "61%", height: "100%", background: "var(--accent)", borderRadius: 3 }}/>
              </div>
              <div className="rs-divider" style={{ margin: "14px 0" }}/>
              <FareRow label="Original fare (6.2 km)" val="292" muted/>
              <FareRow label="Adjusted fare (3.8 km)" val="186" strong/>
              <FareRow label="You save" val="−106" pos/>
            </div>

            <div style={{ display: "flex", gap: 10, marginTop: 18 }}>
              <button className="rs-btn soft" style={{ flex: 1 }}>Stay on ride</button>
              <button className="rs-btn accent" style={{ flex: 1.2 }}>Drop me here</button>
            </div>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// TRIP COMPLETE · RECEIPT
// ═══════════════════════════════════════════════════════════════════
function ReceiptScreen() {
  return (
    <Phone label="16 Receipt">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "24px 24px 18px", background: "var(--accent)", color: "#fff", borderBottomLeftRadius: 28, borderBottomRightRadius: 28 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <button style={{ width: 40, height: 40, borderRadius: 20, background: "rgba(255,255,255,.2)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="close" size={20} color="#fff"/>
            </button>
            <div style={{ fontSize: 11, fontWeight: 700, letterSpacing: ".12em", opacity: .85 }}>TRIP #RS-4429</div>
            <button style={{ width: 40, height: 40, borderRadius: 20, background: "rgba(255,255,255,.2)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="receipt" size={18} color="#fff"/>
            </button>
          </div>
          <div style={{ marginTop: 16, textAlign: "center" }}>
            <div style={{ fontSize: 12, fontWeight: 700, letterSpacing: ".08em", opacity: .9 }}>TOTAL PAID</div>
            <div className="rs-display" style={{ fontSize: 48, lineHeight: 1, marginTop: 4 }}>LKR 186</div>
            <div style={{ fontSize: 13, opacity: .85, marginTop: 4 }}>24 Apr · 3.8 km actual · Thanks for riding!</div>
          </div>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "18px 20px 20px" }} className="rs-scroll">
          <div className="rs-card" style={{ padding: 14, display: "flex", gap: 12, alignItems: "center" }}>
            <Avatar name="Saman W" size={44}/>
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 700, fontSize: 14 }}>Saman W</div>
              <div style={{ fontSize: 12, color: "var(--ink-3)" }}>Toyota Aqua · CAR-2211</div>
            </div>
            <button className="rs-chip accent" style={{ height: 32 }}><Icon name="star" size={12}/> Rate</button>
          </div>

          <div style={{ marginTop: 16, padding: "14px 0" }}>
            <Timeline step="Pickup · Narahenpita Jn" sub="8:04 AM" color="var(--teal)"/>
            <Timeline step="Drop · Kollupitiya Jn" sub="8:18 AM · you got off early" color="var(--accent)" last/>
          </div>

          <div className="rs-section-label" style={{ marginTop: 6, marginBottom: 10 }}>FARE DETAIL</div>
          <div className="rs-card" style={{ padding: 14 }}>
            <FareRow label="Actual distance · 3.8 km × LKR 50" val="190"/>
            <FareRow label="Route match discount" val="−28" pos/>
            <FareRow label="Platform fee · 10%" val="17" muted/>
            <FareRow label="Card pre-auth" val="292" muted/>
            <FareRow label="Refunded to •••• 4429" val="−106" pos/>
            <div className="rs-divider" style={{ margin: "10px 0" }}/>
            <FareRow label="Net charged" val="186" strong/>
          </div>

          <div style={{ display: "flex", gap: 10, marginTop: 16 }}>
            <button className="rs-btn ghost" style={{ flex: 1 }}><Icon name="receipt" size={16}/> Receipt</button>
            <button className="rs-btn ghost" style={{ flex: 1 }}><Icon name="help" size={16}/> Get help</button>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// RATE DRIVER
// ═══════════════════════════════════════════════════════════════════
function RateDriverScreen() {
  return (
    <Phone label="17 Rate Driver">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)", padding: "8px 24px 24px" }}>
        <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
          <Icon name="close" size={20}/>
        </button>

        <div style={{ textAlign: "center", marginTop: 28 }}>
          <Avatar name="Saman W" size={80} style={{ margin: "0 auto" }}/>
          <div className="rs-display" style={{ fontSize: 26, marginTop: 14 }}>How was your ride<br/>with Saman?</div>
          <div style={{ fontSize: 13, color: "var(--ink-3)", marginTop: 6 }}>Your rating stays anonymous.</div>
        </div>

        <div style={{ display: "flex", justifyContent: "center", gap: 10, marginTop: 24 }}>
          {[1,2,3,4,5].map(i => (
            <div key={i} style={{ width: 44, height: 44, display: "flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="star" size={36} color={i <= 5 ? "var(--warn)" : "var(--line-2)"} strokeWidth={2}/>
            </div>
          ))}
        </div>
        <div style={{ textAlign: "center", marginTop: 6, fontSize: 13, color: "var(--ink-2)", fontWeight: 600 }}>Excellent ride!</div>

        <div style={{ marginTop: 22 }}>
          <div className="rs-section-label" style={{ marginBottom: 10 }}>WHAT STOOD OUT?</div>
          <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
            {["Safe driving", "Friendly", "Clean car", "Knew route", "On time", "Quiet"].map((t, i) => (
              <div key={t} className={i < 3 ? "rs-chip accent" : "rs-chip"} style={{ height: 34 }}>
                {i < 3 && <Icon name="check" size={12} color="var(--accent-2)" strokeWidth={3}/>} {t}
              </div>
            ))}
          </div>
        </div>

        <div style={{ marginTop: 18 }}>
          <div className="rs-section-label" style={{ marginBottom: 8 }}>NOTE FOR SAMAN (OPTIONAL)</div>
          <div style={{ padding: 12, background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 12, fontSize: 13, color: "var(--ink-4)", minHeight: 70 }}>
            Say something nice…
          </div>
        </div>

        <div style={{ flex: 1 }}/>
        <div style={{ display: "flex", gap: 10 }}>
          <button className="rs-btn soft" style={{ flex: 1 }}>Skip</button>
          <button className="rs-btn accent" style={{ flex: 1.4 }}>Submit rating</button>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, { RideDetailScreen, SeatSelectScreen, PaymentScreen, BookedScreen, InTripScreen, ExitEarlyScreen, ReceiptScreen, RateDriverScreen, FareRow, Timeline });
