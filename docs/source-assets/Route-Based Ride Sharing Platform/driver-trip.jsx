// driver-trip.jsx — trip detail, booking requests inbox, pre-trip checklist, live trip, boarded/dropped, complete

// ═══════════════════════════════════════════════════════════════════
// TRIP DETAIL — passengers booked, pickup points
// ═══════════════════════════════════════════════════════════════════
function DrTripDetailScreen() {
  return (
    <Phone label="D17 Trip Detail">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ height: 200, position: "relative", flexShrink: 0 }}>
          <MapBackdrop pickupLabel="Start" dropLabel="End"/>
          <button style={{ position: "absolute", top: 12, left: 12, width: 40, height: 40, borderRadius: 20, background: "#fff", display: "inline-flex", alignItems: "center", justifyContent: "center", boxShadow: "var(--shadow-md)" }}>
            <Icon name="back" size={20}/>
          </button>
          <button style={{ position: "absolute", top: 12, right: 12, width: 40, height: 40, borderRadius: 20, background: "#fff", display: "inline-flex", alignItems: "center", justifyContent: "center", boxShadow: "var(--shadow-md)" }}>
            <Icon name="ellipsis" size={18}/>
          </button>
          {/* Pickup markers along the route */}
          <div style={{ position: "absolute", left: "32%", top: "32%" }}>
            <div style={{ background: "#fff", padding: "3px 7px", borderRadius: 999, fontSize: 10, fontWeight: 700, boxShadow: "var(--shadow-md)", border: "2px solid var(--teal)" }}>+1</div>
          </div>
          <div style={{ position: "absolute", left: "55%", top: "55%" }}>
            <div style={{ background: "#fff", padding: "3px 7px", borderRadius: 999, fontSize: 10, fontWeight: 700, boxShadow: "var(--shadow-md)", border: "2px solid var(--teal)" }}>+2</div>
          </div>
        </div>

        <div style={{ flex: 1, overflow: "auto", marginTop: -28, background: "var(--surface)", borderTopLeftRadius: 28, borderTopRightRadius: 28, position: "relative", zIndex: 2 }} className="rs-scroll">
          <div className="rs-sheet-grab"/>
          <div style={{ padding: "4px 20px 14px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
              <div>
                <div style={{ fontSize: 11, color: "var(--ink-3)", fontWeight: 700, letterSpacing: ".1em" }}>WED · 8:00 AM</div>
                <div className="rs-display" style={{ fontSize: 22, lineHeight: 1.15 }}>Rajagiriya → Colombo Fort</div>
                <div style={{ fontSize: 12, color: "var(--ink-3)" }}>11.4 km · ~24 min · 3 seats</div>
              </div>
              <span className="rs-chip accent" style={{ height: 26 }}>SCHEDULED</span>
            </div>
          </div>

          {/* Seat status */}
          <div style={{ padding: "0 20px" }}>
            <div style={{ padding: 14, background: "var(--bg-soft)", borderRadius: 14, display: "flex", alignItems: "center", gap: 14 }}>
              <div style={{ display: "flex", gap: 4 }}>
                {[0,1,2].map(i => (
                  <div key={i} className="seat" style={{ background: i < 2 ? "var(--accent)" : "var(--bg-soft)", borderColor: i < 2 ? "var(--accent)" : "var(--line-2)", width: 26, height: 32 }}/>
                ))}
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 14, fontWeight: 700 }}>2 of 3 seats booked</div>
                <div style={{ fontSize: 11, color: "var(--ink-3)" }}>1 seat still available · LKR 540 more potential</div>
              </div>
            </div>
          </div>

          {/* Passengers */}
          <div style={{ padding: "16px 20px 6px" }} className="rs-section-label">PASSENGERS</div>
          <div style={{ padding: "0 20px", display: "flex", flexDirection: "column", gap: 10 }}>
            <PaxRow name="Nimali P" pickup="Narahenpita Jn" drop="Bambalapitiya" fare={292} dist="6.2 km" status="confirmed" action={
              <button style={{ width: 36, height: 36, borderRadius: 18, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="phone" size={14}/>
              </button>
            }/>
            <PaxRow name="Kasun A" pickup="Thunmulla" drop="Colombo Fort" fare={420} dist="8.4 km" status="confirmed" action={
              <button style={{ width: 36, height: 36, borderRadius: 18, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="phone" size={14}/>
              </button>
            }/>
          </div>

          <div style={{ padding: "16px 20px 6px" }} className="rs-section-label">EARNINGS ESTIMATE</div>
          <div style={{ padding: "0 20px 100px" }}>
            <FareRow label="2 passenger fares" val="712"/>
            <FareRow label="Platform commission · 10%" val="−71" muted/>
            <div className="rs-divider" style={{ margin: "8px 0" }}/>
            <FareRow label="Your earnings" val="641" strong/>
          </div>
        </div>

        <div style={{ padding: "12px 20px 14px", background: "var(--surface)", borderTop: "1px solid var(--line)", display: "flex", gap: 10, position: "absolute", left: 0, right: 0, bottom: 0 }}>
          <button className="rs-btn ghost" style={{ flex: 1 }}>Edit trip</button>
          <button className="rs-btn accent" style={{ flex: 1.4 }}>Start trip</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// BOOKING REQUESTS INBOX (manual approval)
// ═══════════════════════════════════════════════════════════════════
function DrBookingRequestsScreen() {
  return (
    <Phone label="D18 Booking Requests">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "14px 20px", display: "flex", alignItems: "center", gap: 12, background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="back" size={20}/>
          </button>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 17, fontWeight: 700, display: "flex", alignItems: "center", gap: 8 }}>
              Booking requests <span style={{ height: 22, padding: "0 8px", background: "var(--accent)", color: "#fff", fontSize: 11, fontWeight: 700, borderRadius: 999, display: "inline-flex", alignItems: "center" }}>2</span>
            </div>
            <div style={{ fontSize: 11, color: "var(--ink-3)" }}>Pending your approval</div>
          </div>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "16px 20px" }} className="rs-scroll">
          <div className="rs-section-label" style={{ marginBottom: 10 }}>WED 8:00 AM · RAJAGIRIYA → FORT</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            <RequestCard name="Priya J" rating={5.0} trips={84} pickup="Narahenpita Jn" drop="Bambalapitiya" fare={225} match={74} time="2 min ago"/>
            <RequestCard name="Dilshan F" rating={4.6} trips={28} pickup="Castle St" drop="Kollupitiya" fare={310} match={92} time="14 min ago"/>
          </div>

          <div className="rs-section-label" style={{ margin: "20px 0 10px" }}>RECENTLY APPROVED</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            <RecentRequest name="Nimali P" status="confirmed"/>
            <RecentRequest name="Kasun A" status="confirmed"/>
            <RecentRequest name="Maya R" status="declined"/>
          </div>
        </div>
      </div>
    </Phone>
  );
}

function RequestCard({ name, rating, trips, pickup, drop, fare, match, time }) {
  return (
    <div className="rs-card" style={{ padding: 14 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
        <Avatar name={name} size={44}/>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 14, fontWeight: 700 }}>{name}</div>
          <div style={{ fontSize: 12, color: "var(--ink-3)", display: "flex", alignItems: "center", gap: 4 }}>
            <Icon name="star" size={11} color="var(--warn)"/> {rating} · {trips} trips
          </div>
        </div>
        <div style={{ textAlign: "right" }}>
          <MatchRing value={match} size={42}/>
        </div>
      </div>
      <div style={{ marginTop: 12, padding: 10, background: "var(--bg-soft)", borderRadius: 10, position: "relative" }}>
        <div style={{ position: "absolute", left: 16, top: 14, bottom: 14, width: 2, background: "var(--line-2)", backgroundImage: "linear-gradient(var(--line-2) 60%, transparent 60%)", backgroundSize: "2px 4px" }}/>
        <div style={{ display: "flex", alignItems: "center", gap: 10, paddingLeft: 4 }}>
          <div style={{ width: 9, height: 9, borderRadius: 5, background: "var(--teal)", flexShrink: 0 }}/>
          <div style={{ fontSize: 12, fontWeight: 600 }}>{pickup}</div>
        </div>
        <div style={{ height: 8 }}/>
        <div style={{ display: "flex", alignItems: "center", gap: 10, paddingLeft: 4 }}>
          <div style={{ width: 9, height: 9, background: "var(--accent)", flexShrink: 0 }}/>
          <div style={{ fontSize: 12, fontWeight: 600 }}>{drop}</div>
        </div>
      </div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 12 }}>
        <div>
          <div className="rs-display tab" style={{ fontSize: 20, fontWeight: 600 }}>LKR {fare}</div>
          <div style={{ fontSize: 10, color: "var(--ink-4)", fontWeight: 700 }}>YOUR EARNINGS · {time}</div>
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          <button className="rs-btn soft" style={{ height: 42, padding: "0 14px", fontSize: 12 }}>Decline</button>
          <button className="rs-btn accent" style={{ height: 42, padding: "0 16px", fontSize: 12 }}>Approve</button>
        </div>
      </div>
    </div>
  );
}

function RecentRequest({ name, status }) {
  const meta = status === "confirmed"
    ? { tint: "var(--success-soft)", fg: "var(--match-full)", label: "Confirmed" }
    : { tint: "var(--danger-soft)", fg: "var(--danger)", label: "Declined" };
  return (
    <div style={{ padding: "10px 12px", background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 12, display: "flex", alignItems: "center", gap: 12 }}>
      <Avatar name={name} size={32}/>
      <div style={{ flex: 1, fontSize: 13, fontWeight: 600 }}>{name}</div>
      <div style={{ height: 22, padding: "0 8px", borderRadius: 999, background: meta.tint, color: meta.fg, fontSize: 11, fontWeight: 700, display: "inline-flex", alignItems: "center" }}>{meta.label}</div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════
// PRE-TRIP CHECKLIST
// ═══════════════════════════════════════════════════════════════════
function DrPreTripScreen() {
  return (
    <Phone label="D19 Pre-trip Checklist">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)", padding: "12px 20px 20px" }}>
        <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
          <Icon name="close" size={20}/>
        </button>

        <div style={{ marginTop: 20 }}>
          <div className="rs-display" style={{ fontSize: 28, lineHeight: 1.15 }}>Ready to roll?</div>
          <div style={{ fontSize: 13, color: "var(--ink-3)", marginTop: 4 }}>Quick safety check before starting the trip.</div>
        </div>

        <div style={{ marginTop: 24, display: "flex", flexDirection: "column", gap: 10 }}>
          <CheckItem icon="user" label="I'm well-rested and alert" done/>
          <CheckItem icon="car" label="Vehicle is in good condition" done/>
          <CheckItem icon="shield" label="Seatbelts working for all seats" done/>
          <CheckItem icon="phone" label="Phone mounted, charged, on do-not-disturb" done={false}/>
        </div>

        <div style={{ marginTop: 18, padding: 14, background: "var(--bg-soft)", borderRadius: 14, display: "flex", gap: 10 }}>
          <Icon name="users" size={18}/>
          <div style={{ flex: 1, fontSize: 12, lineHeight: 1.4 }}>
            <b>2 passengers</b> are waiting along your route. Nimali boards first at Narahenpita.
          </div>
        </div>

        <div style={{ marginTop: 14, padding: 14, background: "var(--accent-soft)", borderRadius: 14, display: "flex", gap: 10 }}>
          <Icon name="alert" size={18} color="var(--accent-2)"/>
          <div style={{ flex: 1, fontSize: 12, color: "var(--accent-2)", lineHeight: 1.4 }}>
            <b>Drive safe.</b> No phone use while driving — use voice navigation only.
          </div>
        </div>

        <div style={{ flex: 1 }}/>
        <button className="rs-btn accent full" style={{ height: 56 }}>
          <Icon name="car" size={18} color="#fff"/> Start the trip
        </button>
        <button style={{ marginTop: 8, padding: 14, fontSize: 13, fontWeight: 600, color: "var(--ink-3)" }}>Cancel this trip</button>
      </div>
    </Phone>
  );
}

function CheckItem({ icon, label, done }) {
  return (
    <div style={{ padding: 14, background: "var(--surface)", border: `1.5px solid ${done ? "var(--success)" : "var(--line)"}`, borderRadius: 14, display: "flex", alignItems: "center", gap: 12 }}>
      <div style={{ width: 36, height: 36, borderRadius: 12, background: done ? "var(--success-soft)" : "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
        <Icon name={icon} size={18} color={done ? "var(--match-full)" : "var(--ink-2)"}/>
      </div>
      <div style={{ flex: 1, fontSize: 14, fontWeight: 600 }}>{label}</div>
      <div style={{ width: 24, height: 24, borderRadius: 12, border: `2px solid ${done ? "var(--success)" : "var(--line-2)"}`, background: done ? "var(--success)" : "transparent", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
        {done && <Icon name="check" size={14} color="#fff" strokeWidth={3}/>}
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════
// LIVE TRIP — map-dominant with passenger pickup strip
// ═══════════════════════════════════════════════════════════════════
function DrLiveTripScreen() {
  return (
    <Phone label="D20 Live Trip">
      <div style={{ height: "100%", position: "relative" }}>
        <MapBackdrop pickupLabel="Next pickup" dropLabel="Final stop"/>
        {/* Driver position */}
        <div style={{ position: "absolute", left: "30%", top: "32%" }}>
          <div style={{ width: 30, height: 30, borderRadius: 15, background: "var(--accent)", border: "4px solid #fff", boxShadow: "0 0 0 8px rgba(214,106,59,.18)", display: "flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="car" size={14} color="#fff"/>
          </div>
        </div>

        {/* Top instruction card */}
        <div style={{ position: "absolute", top: 12, left: 12, right: 12, padding: 14, background: "var(--ink)", color: "#fff", borderRadius: 18, display: "flex", alignItems: "center", gap: 12, zIndex: 4, boxShadow: "var(--shadow-lg)" }}>
          <div style={{ width: 56, height: 56, borderRadius: 14, background: "var(--accent)", display: "flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="arrow" size={28} color="#fff" strokeWidth={2.4}/>
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 11, opacity: .7, fontWeight: 700, letterSpacing: ".1em" }}>IN 600 M</div>
            <div style={{ fontSize: 16, fontWeight: 700, marginTop: 1 }}>Turn right onto Baseline Rd</div>
            <div style={{ fontSize: 11, opacity: .7, marginTop: 1 }}>Then pickup Nimali at Narahenpita Jn</div>
          </div>
        </div>

        {/* Right side action stack */}
        <div style={{ position: "absolute", right: 12, top: 100, display: "flex", flexDirection: "column", gap: 8, zIndex: 4 }}>
          <button style={{ width: 44, height: 44, borderRadius: 22, background: "#fff", display: "flex", alignItems: "center", justifyContent: "center", boxShadow: "var(--shadow-md)" }}>
            <Icon name="target" size={20}/>
          </button>
          <button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--danger)", display: "flex", alignItems: "center", justifyContent: "center", boxShadow: "var(--shadow-md)" }}>
            <Icon name="alert" size={20} color="#fff"/>
          </button>
        </div>

        {/* Bottom: passenger strip + control */}
        <div style={{ position: "absolute", left: 0, right: 0, bottom: 0, zIndex: 4 }} className="rs-sheet">
          <div className="rs-sheet-grab"/>
          <div style={{ padding: "4px 16px 14px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", padding: "0 4px" }}>
              <div>
                <div style={{ fontSize: 11, color: "var(--accent-2)", fontWeight: 700, letterSpacing: ".08em" }}>NEXT PICKUP IN</div>
                <div className="rs-display" style={{ fontSize: 22, lineHeight: 1 }}>2 min · 600 m</div>
              </div>
              <div style={{ textAlign: "right" }}>
                <div style={{ fontSize: 11, color: "var(--ink-3)", fontWeight: 700, letterSpacing: ".08em" }}>EARNING SO FAR</div>
                <div className="rs-display" style={{ fontSize: 18 }}>LKR 0</div>
              </div>
            </div>

            <div style={{ marginTop: 12, display: "flex", gap: 8, overflow: "hidden" }}>
              <PaxStrip name="Nimali P" status="next" stop="Narahenpita"/>
              <PaxStrip name="Kasun A" status="waiting" stop="Thunmulla"/>
              <PaxStrip name="(seat free)" status="empty" stop=""/>
            </div>

            <button className="rs-btn accent full" style={{ marginTop: 12, height: 52 }}>
              <Icon name="check" size={18} color="#fff" strokeWidth={3}/> I've arrived at Narahenpita
            </button>
          </div>
        </div>
      </div>
    </Phone>
  );
}

function PaxStrip({ name, status, stop }) {
  const meta = {
    next: { bg: "var(--accent)", fg: "#fff", border: "var(--accent)" },
    waiting: { bg: "var(--surface)", fg: "var(--ink)", border: "var(--line)" },
    boarded: { bg: "var(--success-soft)", fg: "var(--match-full)", border: "var(--success)" },
    empty: { bg: "var(--bg-soft)", fg: "var(--ink-4)", border: "var(--line-2)", dashed: true },
  };
  const s = meta[status];
  return (
    <div style={{ flex: 1, padding: "8px 10px", background: s.bg, color: s.fg, border: `1.5px ${s.dashed ? "dashed" : "solid"} ${s.border}`, borderRadius: 12, minWidth: 0 }}>
      <div style={{ fontSize: 12, fontWeight: 700, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{name}</div>
      <div style={{ fontSize: 10, opacity: status === "next" ? .9 : .6, fontWeight: 600, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{stop || "—"}</div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════
// MARK PASSENGER BOARDED
// ═══════════════════════════════════════════════════════════════════
function DrBoardingScreen() {
  return (
    <Phone label="D21 Boarding">
      <div style={{ height: "100%", position: "relative" }}>
        <MapBackdrop pickupLabel="Pickup" dropLabel="Drop"/>
        <div style={{ position: "absolute", inset: 0, background: "rgba(20,10,5,.35)" }}/>

        <div style={{ position: "absolute", left: 0, right: 0, bottom: 0, zIndex: 5 }} className="rs-sheet">
          <div className="rs-sheet-grab"/>
          <div style={{ padding: "4px 22px 22px" }}>
            <div style={{ fontSize: 11, color: "var(--accent-2)", fontWeight: 700, letterSpacing: ".1em" }}>YOU'VE ARRIVED</div>
            <div className="rs-display" style={{ fontSize: 24, marginTop: 2 }}>Pick up Nimali here</div>

            <div style={{ marginTop: 16, padding: 14, background: "var(--bg-soft)", borderRadius: 14, display: "flex", alignItems: "center", gap: 12 }}>
              <Avatar name="Nimali P" size={48}/>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 700, fontSize: 15 }}>Nimali Perera</div>
                <div style={{ fontSize: 12, color: "var(--ink-3)", display: "flex", alignItems: "center", gap: 4 }}>
                  <Icon name="star" size={11} color="var(--warn)"/> 4.92 · 38 trips
                </div>
              </div>
              <button style={{ width: 42, height: 42, borderRadius: 21, background: "#fff", display: "inline-flex", alignItems: "center", justifyContent: "center", border: "1px solid var(--line)" }}>
                <Icon name="phone" size={18} color="var(--teal)"/>
              </button>
              <button style={{ width: 42, height: 42, borderRadius: 21, background: "#fff", display: "inline-flex", alignItems: "center", justifyContent: "center", border: "1px solid var(--line)" }}>
                <Icon name="mail" size={18}/>
              </button>
            </div>

            <div style={{ marginTop: 14, padding: 14, background: "var(--accent-soft)", borderRadius: 14, fontSize: 12, color: "var(--accent-2)", display: "flex", gap: 10 }}>
              <Icon name="shield" size={16} color="var(--accent-2)"/>
              <div>Confirm Nimali's identity before letting her board — check the passenger app shows your car's plate.</div>
            </div>

            <div style={{ marginTop: 18, display: "flex", gap: 10 }}>
              <button className="rs-btn soft" style={{ flex: 1, color: "var(--danger)" }}>Mark no-show</button>
              <button className="rs-btn accent" style={{ flex: 1.6 }}>
                <Icon name="check" size={16} color="#fff" strokeWidth={3}/> Boarded
              </button>
            </div>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// MARK PASSENGER DROPPED
// ═══════════════════════════════════════════════════════════════════
function DrDropOffScreen() {
  return (
    <Phone label="D22 Drop Off">
      <div style={{ height: "100%", position: "relative" }}>
        <MapBackdrop pickupLabel="Pickup" dropLabel="Drop here"/>
        <div style={{ position: "absolute", inset: 0, background: "rgba(20,10,5,.35)" }}/>

        <div style={{ position: "absolute", left: 0, right: 0, bottom: 0, zIndex: 5 }} className="rs-sheet">
          <div className="rs-sheet-grab"/>
          <div style={{ padding: "4px 22px 22px" }}>
            <div style={{ fontSize: 11, color: "var(--teal)", fontWeight: 700, letterSpacing: ".1em" }}>DROP OFF NIMALI</div>
            <div className="rs-display" style={{ fontSize: 22, marginTop: 2 }}>Bambalapitiya · 6.2 km</div>

            <div style={{ marginTop: 16, padding: 14, background: "var(--bg-soft)", borderRadius: 14 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  <Avatar name="Nimali P" size={40}/>
                  <div>
                    <div style={{ fontSize: 14, fontWeight: 700 }}>Nimali Perera</div>
                    <div style={{ fontSize: 11, color: "var(--ink-3)" }}>Boarded at 8:04 AM</div>
                  </div>
                </div>
              </div>
              <div className="rs-divider" style={{ margin: "12px 0" }}/>
              <FareRow label="Distance travelled" val="6.2 km" />
              <FareRow label="Passenger fare" val="310"/>
              <FareRow label="Platform fee" val="−31" muted/>
              <div className="rs-divider" style={{ margin: "8px 0" }}/>
              <FareRow label="Added to your earnings" val="279" strong pos/>
              <div style={{ marginTop: 6, fontSize: 10, color: "var(--ink-4)", fontWeight: 700, letterSpacing: ".06em" }}>PAYMENT: CARD · AUTO-COLLECTED</div>
            </div>

            <div style={{ marginTop: 14, padding: 12, background: "var(--teal-soft)", borderRadius: 12, fontSize: 12, color: "var(--teal)", display: "flex", gap: 10 }}>
              <Icon name="users" size={16} color="var(--teal)"/>
              <div><b>Next stop:</b> Drop Kasun at Colombo Fort · 5.2 km · LKR 420</div>
            </div>

            <div style={{ marginTop: 16, display: "flex", gap: 10 }}>
              <button className="rs-btn soft" style={{ flex: 1 }}>Edit fare</button>
              <button className="rs-btn accent" style={{ flex: 1.6 }}>
                <Icon name="check" size={16} color="#fff" strokeWidth={3}/> Drop off
              </button>
            </div>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// TRIP COMPLETE — earnings summary
// ═══════════════════════════════════════════════════════════════════
function DrTripCompleteScreen() {
  return (
    <Phone label="D23 Trip Complete">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "20px 20px 24px", background: "var(--ink)", color: "#fff", borderBottomLeftRadius: 28, borderBottomRightRadius: 28, position: "relative", overflow: "hidden" }}>
          <div style={{ position: "absolute", right: -50, top: -50, width: 200, height: 200, borderRadius: 100, background: "var(--accent)", opacity: .4 }}/>
          <div style={{ position: "relative" }}>
            <button style={{ width: 40, height: 40, borderRadius: 20, background: "rgba(255,255,255,.2)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="close" size={20} color="#fff"/>
            </button>
            <div style={{ marginTop: 20 }}>
              <div style={{ fontSize: 11, opacity: .7, fontWeight: 700, letterSpacing: ".12em" }}>TRIP COMPLETE</div>
              <div className="rs-display tab" style={{ fontSize: 56, fontWeight: 600, marginTop: 4, lineHeight: 1 }}>LKR 699</div>
              <div style={{ fontSize: 13, opacity: .8, marginTop: 6 }}>Added to your wallet · 2 passengers · 11.4 km</div>
            </div>
          </div>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "18px 20px 20px" }} className="rs-scroll">
          <div className="rs-section-label" style={{ marginBottom: 10 }}>BREAKDOWN</div>
          <div className="rs-card" style={{ padding: 14 }}>
            <FareRow label="Nimali · 6.2 km · card" val="279"/>
            <FareRow label="Kasun · 8.4 km · cash" val="378"/>
            <FareRow label="Recurring bonus · 5th day this week" val="42" pos/>
            <div className="rs-divider" style={{ margin: "8px 0" }}/>
            <FareRow label="Net earnings" val="699" strong/>
          </div>

          <div className="rs-section-label" style={{ margin: "18px 0 10px" }}>PASSENGERS</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            {[
              { name: "Nimali P", fare: 279, rating: null },
              { name: "Kasun A", fare: 378, rating: 5 },
            ].map(p => (
              <div key={p.name} style={{ padding: 12, background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 12, display: "flex", alignItems: "center", gap: 12 }}>
                <Avatar name={p.name} size={36}/>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 14, fontWeight: 700 }}>{p.name}</div>
                  <div style={{ fontSize: 11, color: "var(--ink-3)" }}>LKR {p.fare} added</div>
                </div>
                {p.rating ? (
                  <div style={{ display: "flex", gap: 2 }}>
                    {[...Array(p.rating)].map((_, i) => <Icon key={i} name="star" size={14} color="var(--warn)"/>)}
                  </div>
                ) : (
                  <button className="rs-chip accent" style={{ height: 28 }}>Rate</button>
                )}
              </div>
            ))}
          </div>

          <div style={{ marginTop: 18, padding: 14, background: "var(--success-soft)", borderRadius: 14, display: "flex", gap: 10, alignItems: "center" }}>
            <div style={{ width: 40, height: 40, borderRadius: 20, background: "#fff", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="leaf" size={20} color="var(--match-full)"/>
            </div>
            <div style={{ flex: 1, fontSize: 12, color: "var(--match-full)" }}>
              <div style={{ fontWeight: 700 }}>You saved ~2.8 kg of CO₂ today</div>
              <div style={{ opacity: .8 }}>By sharing seats vs solo travel</div>
            </div>
          </div>
        </div>

        <div style={{ padding: "12px 20px 14px", background: "var(--surface)", borderTop: "1px solid var(--line)", display: "flex", gap: 10 }}>
          <button className="rs-btn ghost" style={{ flex: 1 }}>See earnings</button>
          <button className="rs-btn accent" style={{ flex: 1.4 }}>Back to home</button>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, { DrTripDetailScreen, DrBookingRequestsScreen, DrPreTripScreen, DrLiveTripScreen, DrBoardingScreen, DrDropOffScreen, DrTripCompleteScreen });
