// shared-states.jsx — the reusable state set and the remote-config degraded
// states. Documented once here rather than invented per screen.

function MiniFrame({ children, label, h = 330 }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 8, flex: 1, minWidth: 0 }}>
      <div className="rs" style={{
        boxSizing: "border-box", height: h, borderRadius: 20, overflow: "hidden",
        background: "var(--bg)", border: "1px solid var(--line-2)", display: "flex", flexDirection: "column",
      }}>{children}</div>
      <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".1em", color: "var(--ink-3)" }}>{label}</div>
    </div>
  );
}

function MiniBar({ title = "Rides to Bambalapitiya" }) {
  return (
    <div style={{ padding: "11px 13px", borderBottom: "1px solid var(--line)", background: "var(--surface)", display: "flex", alignItems: "center", gap: 9, flexShrink: 0 }}>
      <Icon name="back" size={16} color="var(--ink-2)"/>
      <div style={{ fontSize: 12, fontWeight: 700, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{title}</div>
    </div>
  );
}

// ═══════════ X01 · the five states every list and detail screen must define ═══════════
const STATE_SPEC = [
  {
    key: "loading", label: "LOADING", when: "Request in flight and nothing cached.",
    rule: "Skeletons mirror the real row: avatar, two text lines, a price. Never a spinner on a list — a spinner hides how much is coming. Announce “Loading rides” to screen readers.",
    render: (
      <>
        <MiniBar/>
        <div style={{ padding: 11, display: "flex", flexDirection: "column", gap: 8 }}>
          <Skel w="52%" h={11}/>
          <SkelRow/><SkelRow/><SkelRow/>
        </div>
      </>
    ),
  },
  {
    key: "empty", label: "EMPTY", when: "Request succeeded, zero results.",
    rule: "Say why it's empty in the user's terms, then give the one action most likely to fix it. Never “No data”. For results, the fix is widening time or accepting a lower match.",
    render: (
      <>
        <MiniBar/>
        <EmptyState icon="route" title="No routes match yet"
          body="Nobody has published this stretch for 8 AM. Widen the time and we'll look again."
          cta="Search 7–10 AM"/>
      </>
    ),
  },
  {
    key: "error", label: "ERROR + RETRY", when: "5xx, timeout, or an unparseable response.",
    rule: "Blame the system, not the user. One primary Retry, and a way out to support if it repeats. Never show a status code without plain-language copy beside it.",
    render: (
      <>
        <MiniBar/>
        <EmptyState kind="error" icon="alert" title="That didn't load"
          body="Something went wrong on our side. Your search is saved — try again."
          cta="Try again"/>
      </>
    ),
  },
  {
    key: "offline", label: "OFFLINE", when: "No connectivity, detected before the request.",
    rule: "Persistent banner, not a modal — the user may still want to read cached content. Auto-retry on reconnect and replace the banner with a brief “Back online”. Cached rows stay visible and are marked stale.",
    render: (
      <>
        <MiniBar/>
        <div style={{ padding: 11, display: "flex", flexDirection: "column", gap: 9 }}>
          <Banner kind="warn" icon="alert" title="You're offline" body="Showing rides from 9:41 AM. We'll refresh when you're back." action="Retry"/>
          <div style={{ opacity: .55 }}><SkelRow/></div>
        </div>
      </>
    ),
  },
  {
    key: "limited", label: "RATE LIMITED", when: "429 — too many attempts, usually OTP or booking.",
    rule: "Give the exact wait as a countdown, never “try again later”. Disable the action rather than letting it fail. Offer the alternative channel if one exists.",
    render: (
      <>
        <MiniBar title="Verify code"/>
        <div style={{ padding: 13, display: "flex", flexDirection: "column", gap: 11 }}>
          <div style={{ display: "flex", gap: 6 }}>
            {[0, 1, 2, 3, 4, 5].map(i => <div key={i} style={{ flex: 1, height: 40, borderRadius: 12, background: "var(--surface)", border: "1.5px solid var(--line)" }}/>)}
          </div>
          <Banner kind="bad" icon="lock" title="Too many attempts"
            body="For your security, wait 4:32 before requesting another code. Need help now? Message support."/>
          <div className="rs-btn accent full" style={{ opacity: .45, height: 44 }}>Resend in 4:32</div>
        </div>
      </>
    ),
  },
];

function StateSetBoard() {
  return (
    <BBoard>
      <BTitle sub="Every list and detail screen in ComiGo defines these five. They are one component set with one set of copy rules — not re-invented per screen. Permission-denied states (location, notifications, camera) follow the same shape with the OS-settings deep link as the primary action.">Reusable state set</BTitle>
      <div style={{ display: "flex", gap: 16 }}>
        {STATE_SPEC.map(s => (
          <div key={s.key} style={{ flex: 1, minWidth: 0, display: "flex", flexDirection: "column", gap: 12 }}>
            <MiniFrame label={s.label}>{s.render}</MiniFrame>
            <div style={{ fontSize: 11.5, lineHeight: 1.5 }}>
              <div style={{ fontWeight: 700, color: "var(--ink)" }}>{s.when}</div>
              <div style={{ color: "var(--ink-3)", marginTop: 5 }}>{s.rule}</div>
            </div>
          </div>
        ))}
      </div>
    </BBoard>
  );
}

// ═══════════ X02 · remote-config degraded states ═══════════
function DegradedBoard() {
  return (
    <BBoard>
      <BTitle sub="Driven by the operator's remote app config, so the app must degrade without an update. In each case the feature disappears cleanly and the user is told what they can do instead — never a dead control or a silent failure.">Feature-gated &amp; degraded states</BTitle>
      <div style={{ display: "flex", gap: 16 }}>
        <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: 12 }}>
          <MiniFrame label="CARD PAYMENTS DISABLED → CASH ONLY" h={330}>
            <MiniBar title="Payment"/>
            <div style={{ padding: 11, display: "flex", flexDirection: "column", gap: 9 }}>
              <Banner kind="warn" icon="card" title="Card payments are paused" body="Our payment provider is having trouble. Cash is available as normal."/>
              <div style={{ padding: 12, borderRadius: 14, background: "var(--surface)", border: "1.5px solid var(--ink)", display: "flex", alignItems: "center", gap: 10 }}>
                <div style={{ width: 32, height: 32, borderRadius: 10, background: "var(--status-approved-soft)", display: "flex", alignItems: "center", justifyContent: "center" }}><Icon name="cash" size={16} color="var(--status-approved-ink)"/></div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 12.5, fontWeight: 700 }}>Cash</div>
                  <div style={{ fontSize: 10.5, color: "var(--ink-3)" }}>Pay Saman directly · LKR 310</div>
                </div>
                <Icon name="check" size={16} color="var(--status-approved-ink)" strokeWidth={2.6}/>
              </div>
              <div style={{ padding: 12, borderRadius: 14, background: "var(--bg-soft)", border: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 10, opacity: .6 }}>
                <div style={{ width: 32, height: 32, borderRadius: 10, background: "var(--surface)", display: "flex", alignItems: "center", justifyContent: "center" }}><Icon name="card" size={16} color="var(--ink-3)"/></div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 12.5, fontWeight: 700 }}>Visa ···4429</div>
                  <div style={{ fontSize: 10.5, color: "var(--ink-3)" }}>Unavailable right now</div>
                </div>
              </div>
            </div>
          </MiniFrame>
          <div style={{ fontSize: 11.5, color: "var(--ink-3)", lineHeight: 1.5 }}>The disabled method stays visible but inert, so the user understands it's temporary rather than gone. Checkout never blocks — cash always exists.</div>
        </div>

        <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: 12 }}>
          <MiniFrame label="MAPS UNAVAILABLE → ACCESSIBLE LIST" h={330}>
            <MiniBar title="Your ride"/>
            <div style={{ padding: 11, display: "flex", flexDirection: "column", gap: 9 }}>
              <Banner kind="info" icon="pin" title="Map can't load" body="Here's the same trip as a list."/>
              {[["Picked up", "Narahenpita · 8:04 AM", "var(--teal)", true],
                ["Now", "Nawala Road, 2.1 km to go", "var(--accent-ink)", false],
                ["Drop off", "Bambalapitiya · 8:42 AM", "var(--ink-3)", false]].map(([t, s, c, done]) => (
                <div key={t} style={{ display: "flex", gap: 10, alignItems: "flex-start" }}>
                  <div style={{ width: 12, height: 12, borderRadius: 6, background: c, marginTop: 3, flexShrink: 0 }}/>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 12, fontWeight: 700 }}>{t}</div>
                    <div style={{ fontSize: 10.5, color: "var(--ink-3)", marginTop: 1 }}>{s}</div>
                  </div>
                  {done && <Icon name="check" size={14} color="var(--teal)" strokeWidth={2.6}/>}
                </div>
              ))}
            </div>
          </MiniFrame>
          <div style={{ fontSize: 11.5, color: "var(--ink-3)", lineHeight: 1.5 }}>This list is not a fallback bolted on — it is the accessible equivalent every map screen ships with, reachable any time via “View as list”. Screen-reader users get it by default.</div>
        </div>

        <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: 12 }}>
          <MiniFrame label="NOT AVAILABLE IN THIS AREA" h={330}>
            <MiniBar title="Publish a trip"/>
            <EmptyState icon="pin" title="Not in Kandy yet"
              body="ComiGo runs in Colombo and Gampaha for now. We'll email you the day it opens where you are."
              cta="Tell me when it opens"/>
          </MiniFrame>
          <div style={{ fontSize: 11.5, color: "var(--ink-3)", lineHeight: 1.5 }}>Region gating names the region the user is actually in, lists where the service does run, and captures intent. Never “feature unavailable”.</div>
        </div>
      </div>
    </BBoard>
  );
}

Object.assign(window, { MiniFrame, MiniBar, StateSetBoard, DegradedBoard, STATE_SPEC });
