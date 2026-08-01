// shared-chat.jsx — P23 / D36: in-booking chat.
// One component, two modes. Chat exists only inside a confirmed booking: it
// opens on acceptance and closes 24 hours after drop-off, so there is no
// profile-to-profile messaging to moderate.

// The driver-side thread. Screen copy, not shared data — the passenger thread
// and the driver thread are different conversations, not one flipped.
const DRIVE_CHAT = [
  { who: "them", t: "9:52 AM", body: "Good morning! Are you starting from Nugegoda junction or the Kirula Road side?" },
  { who: "me", t: "9:54 AM", body: "Nugegoda junction, near the clock tower. Silver Wagon R, CAB-7734." },
  { who: "them", t: "9:55 AM", body: "Noted. I'll be at the Narahenpita stop by 10:12." },
];

function ChatScreen({ mode = "ride" }) {
  const ride = mode === "ride";
  const other = ride ? MY_TRIP.driver : NEXT_DRIVE.passengers[0].name;
  const role = ride ? "Your driver" : "Your passenger";
  const tint = ride ? "var(--accent-ink)" : "var(--mode-drive-ink)";
  const bubbleMe = ride ? "var(--accent-ink)" : "var(--mode-drive)";
  const msgs = ride ? CHAT.msgs.map(m => ({ ...m, who: m.who === "driver" ? "them" : "me" })) : DRIVE_CHAT;
  const sub = ride
    ? `${CHAT.bookingRef} · ${MY_TRIP.depart} ${MY_TRIP.from} → ${MY_TRIP.to}`
    : `${NEXT_DRIVE.depart} · ${NEXT_DRIVE.from} → ${NEXT_DRIVE.to}`;
  const quick = ride
    ? ["I'm at the pickup", "Running 5 min late", "Which vehicle?"]
    : ["On my way", "I'm at the pickup point", "Running 5 min late"];

  return (
    <Phone label={`${ride ? "P23" : "D36"} Booking chat · ${ride ? "passenger" : "driver"}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title={other} sub={sub}
          right={<button style={{ width: 44, height: 44, borderRadius: 22, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label={`Call ${other}`}><Icon name="phone" size={19}/></button>}/>
        <div style={{ flex: 1, overflow: "auto", padding: "14px 16px 16px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          <div className="rs-card" style={{ padding: 13, display: "flex", alignItems: "center", gap: 11 }}>
            <Avatar name={other} size={40}/>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", color: tint }}>{role.toUpperCase()}</div>
              <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 3, display: "flex", alignItems: "center", gap: 5 }}>
                <Icon name="star" size={12} color="var(--status-pending-ink)"/>
                <span className="tab">{ride ? TRUST.driver.rating : TRUST.passenger.rating}</span>
                · {ride ? TRUST.driver.trips : TRUST.passenger.trips} trips
              </div>
            </div>
            <button className="rs-tap"><span className="rs-chip">View profile</span></button>
          </div>

          <Banner kind="info" icon="lock" title="This chat belongs to one booking"
            body={`It opened when the booking was confirmed and closes ${CHAT.closesIn}. Numbers stay hidden. If either of you reports a problem, ComiGo support can read the thread.`}/>

          <div style={{ display: "flex", flexDirection: "column", gap: 9 }}>
            <div style={{ textAlign: "center", fontSize: 10.5, fontWeight: 700, letterSpacing: ".08em", color: "var(--ink-3)" }}>TODAY</div>
            {msgs.map((m, i) => {
              const mine = m.who === "me";
              return (
                <div key={i} style={{ display: "flex", flexDirection: "column", alignItems: mine ? "flex-end" : "flex-start", gap: 3 }}>
                  <div style={{
                    maxWidth: "84%", padding: "11px 14px", borderRadius: 18,
                    borderBottomRightRadius: mine ? 6 : 18, borderBottomLeftRadius: mine ? 18 : 6,
                    background: mine ? bubbleMe : "var(--surface)",
                    border: mine ? "none" : "1px solid var(--line)",
                    color: mine ? "var(--on-bright-fill)" : "var(--ink)",
                    fontSize: 13, lineHeight: 1.5, textWrap: "pretty",
                  }}>{m.body}</div>
                  <div className="tab" style={{ fontSize: 10, color: "var(--ink-3)", padding: "0 4px" }}>{m.t}{mine ? " · Read" : ""}</div>
                </div>
              );
            })}
          </div>
        </div>
        <div style={{ background: "var(--surface)", borderTop: "1px solid var(--line)", flexShrink: 0 }}>
          <div className="rs-scroll" style={{ padding: "10px 16px 0", display: "flex", gap: 7, overflowX: "auto", overflowY: "hidden", scrollPaddingInlineEnd: 16 }}>
            {quick.map(q => <button key={q} className="rs-tap" style={{ flexShrink: 0 }}><span className="rs-chip" style={{ height: 32 }}>{q}</span></button>)}
            <div style={{ width: 1, flexShrink: 0 }}/>
          </div>
          <div style={{ padding: "10px 16px 16px", display: "flex", alignItems: "center", gap: 9 }}>
            <div style={{ flex: 1, minHeight: 46, padding: "0 15px", borderRadius: 23, background: "var(--bg-soft)", border: "1px solid var(--line)", display: "flex", alignItems: "center", fontSize: 13.5, color: "var(--ink-3)" }}>Message {other.split(" ")[0]}…</div>
            <button className="rs-btn" style={{ width: 46, height: 46, borderRadius: 23, background: bubbleMe, flexShrink: 0 }} aria-label="Send">
              <Icon name="arrow" size={20} color="var(--on-bright-fill)"/>
            </button>
          </div>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, { ChatScreen });
