// passenger-identity.jsx — P28…P33: identity verification, the profile photo and
// its visibility switch, referral, and the shared rewards balance.
//
// Two rules run through all of it:
//   1. Verification is NEVER a gate on booking. It is a ranking signal, a badge,
//      and the key to trips a driver restricted to verified riders. A screen that
//      blocks an unverified rider from paying would be a different product.
//   2. Every identity image is captured with the in-app camera. There is no
//      "choose from gallery" affordance anywhere, because the whole value of the
//      selfie-with-NIC is that it could not have been assembled beforehand.

// ═══════════ P28 · WHY VERIFY ═══════════
function PxVerifyIntroScreen() {
  return (
    <Phone label="P28 Verify · why bother">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Verify your identity" sub="Four photos · about two minutes"/>
        <div style={{ flex: 1, overflow: "auto", padding: "18px 16px 16px", display: "flex", flexDirection: "column", gap: 14 }} className="rs-scroll">
          <div style={{ padding: 18, borderRadius: 20, background: "var(--accent-soft)", border: "1px solid var(--line)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 11 }}>
              <div style={{ width: 46, height: 46, borderRadius: 15, background: "var(--accent-ink)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                <Icon name="badge" size={23} color="#fff" strokeWidth={2}/>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div className="rs-display" style={{ fontSize: 21, lineHeight: 1.15 }}>Get more seats</div>
                <div style={{ fontSize: 12, color: "var(--ink-2)", marginTop: 3 }}>Verified riders have {verifiedRidesShare}% more requests accepted.</div>
              </div>
            </div>
          </div>

          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 12 }}>WHAT IT CHANGES</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              {PAX_VERIFY.benefits.map(b => <RuleRow key={b.title} icon={b.icon} title={b.title} body={b.body}/>)}
            </div>
          </div>

          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 4 }}>WHAT WE ASK FOR</div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginBottom: 13, lineHeight: 1.45 }}>
              All four are taken here, with the camera. We don't accept a photo from your gallery — a picture of a picture is exactly what we're checking for.
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              {VERIFY_STEPS.map(s => (
                <div key={s.key} style={{ display: "flex", alignItems: "center", gap: 11 }}>
                  <div style={{ width: 28, height: 28, borderRadius: 14, background: "var(--bg-soft)", border: "1px solid var(--line)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                    <span className="tab" style={{ fontSize: 12, fontWeight: 800, color: "var(--ink-2)" }}>{s.n}</span>
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13.5, fontWeight: 700 }}>{s.label}</div>
                    <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>{s.hint}</div>
                  </div>
                  <Icon name="camera" size={17} color="var(--ink-3)"/>
                </div>
              ))}
            </div>
          </div>

          <Banner kind="info" icon="lock" title="Your NIC images are never shown to anyone"
            body="Drivers see a badge, a first name and an initial. The card images go to review and nowhere else."/>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">Open the camera</button>
          <button className="rs-btn ghost full" style={{ marginTop: 10 }}>Not now — keep riding unverified</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ P29 · CAMERA CAPTURE ═══════════
// One component, three captures. The guide shape is the instruction: a card
// outline for the NIC sides, a face oval plus a card for the held-up selfie.
function VerifyGuide({ guide }) {
  if (guide === "face") {
    return <div style={{ width: 190, height: 244, borderRadius: "50% / 42%", border: "3px dashed rgba(255,255,255,.85)" }}/>;
  }
  if (guide === "both") {
    return (
      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 14 }}>
        <div style={{ width: 150, height: 192, borderRadius: "50% / 42%", border: "3px dashed rgba(255,255,255,.85)" }}/>
        <div style={{ width: 148, height: 92, borderRadius: 10, border: "3px dashed rgba(232,131,79,.95)", display: "flex", alignItems: "center", justifyContent: "center" }}>
          <span style={{ fontSize: 10.5, fontWeight: 800, letterSpacing: ".1em", color: "rgba(255,255,255,.8)" }}>NIC FRONT</span>
        </div>
      </div>
    );
  }
  return (
    <div style={{ width: 286, height: 180, borderRadius: 14, border: "3px dashed rgba(255,255,255,.85)", position: "relative" }}>
      {[["left", "top"], ["right", "top"], ["left", "bottom"], ["right", "bottom"]].map(([x, y]) => (
        <div key={x + y} style={{ position: "absolute", [x]: -3, [y]: -3, width: 28, height: 28, [`border${x === "left" ? "Left" : "Right"}`]: "4px solid #e8834f", [`border${y === "top" ? "Top" : "Bottom"}`]: "4px solid #e8834f", borderRadius: y === "top" ? (x === "left" ? "12px 0 0 0" : "0 12px 0 0") : (x === "left" ? "0 0 0 12px" : "0 0 12px 0") }}/>
      ))}
    </div>
  );
}

function PxVerifyCaptureScreen({ step = 1 }) {
  const s = VERIFY_STEPS[step - 1];
  return (
    <Phone label={`P29${["a", "b", "c", "d"][step - 1]} Capture · ${s.label}`} statusDark statusBg="#12100f" navBg="#12100f">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "#12100f", color: "#f4ece0" }}>
        <div style={{ padding: "10px 14px 12px", display: "flex", alignItems: "center", gap: 10, flexShrink: 0 }}>
          <button style={{ width: 44, height: 44, borderRadius: 22, background: "rgba(255,255,255,.1)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label="Back" data-back="1">
            <Icon name="back" size={20} color="#f4ece0"/>
          </button>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em", opacity: .6 }}>STEP {s.n} OF {VERIFY_STEPS.length}</div>
            <div style={{ fontSize: 15.5, fontWeight: 800, marginTop: 1 }}>{s.label}</div>
          </div>
          <div style={{ display: "flex", gap: 5, flexShrink: 0 }}>
            {VERIFY_STEPS.map(x => (
              <div key={x.key} style={{ width: 7, height: 7, borderRadius: 4, background: x.n < s.n ? "#48a89f" : x.n === s.n ? "#e8834f" : "rgba(255,255,255,.22)" }}/>
            ))}
          </div>
        </div>

        {/* Viewfinder. A flat neutral field stands in for the camera feed — the
            guide and the hint are the design; the live image is the device's. */}
        <div style={{ flex: 1, position: "relative", overflow: "hidden", background: "linear-gradient(160deg,#2a2622,#1a1715 60%,#221d19)", display: "flex", alignItems: "center", justifyContent: "center" }}>
          <VerifyGuide guide={s.guide}/>
          <div style={{ position: "absolute", left: 18, right: 18, bottom: 16, padding: "12px 14px", borderRadius: 14, background: "rgba(18,16,15,.82)", border: "1px solid rgba(255,255,255,.12)", display: "flex", gap: 10 }}>
            <Icon name="alert" size={16} color="#e8834f"/>
            <div style={{ flex: 1, fontSize: 11.5, lineHeight: 1.45, opacity: .9 }}>{s.hint}</div>
          </div>
        </div>

        <div style={{ padding: "16px 16px 18px", flexShrink: 0, display: "flex", alignItems: "center", gap: 14 }}>
          <div style={{ flex: 1, minWidth: 0, fontSize: 11, opacity: .62, lineHeight: 1.45 }}>
            Camera only. Photos from your gallery aren't accepted.
          </div>
          <button style={{ width: 74, height: 74, borderRadius: 37, background: "#f4ece0", border: "4px solid rgba(255,255,255,.28)", display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }} aria-label={`Capture ${s.label}`}>
            <Icon name="camera" size={28} color="#12100f" strokeWidth={2}/>
          </button>
          <div style={{ flex: 1, display: "flex", justifyContent: "flex-end" }}>
            <button style={{ minHeight: 44, padding: "0 14px", borderRadius: 22, background: "rgba(255,255,255,.1)", color: "#f4ece0", fontSize: 12.5, fontWeight: 700 }}>Retake</button>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ P30 · PROFILE PHOTO + VISIBILITY ═══════════
// The fourth capture is not identity evidence — it is what other people see, so
// it gets its own screen and its own switch. Hiding it never hides you from the
// driver who is coming to collect you: he has to be able to find you.
function PxProfilePhotoScreen({ choice = "MATCHED" }) {
  return (
    <Phone label="P30 Profile photo · who sees it">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Your profile photo" sub="Step 4 of 4"/>
        <div style={{ flex: 1, overflow: "auto", padding: "18px 16px 16px", display: "flex", flexDirection: "column", gap: 15 }} className="rs-scroll">
          <div style={{ display: "flex", alignItems: "center", gap: 15 }}>
            <div style={{ position: "relative", flexShrink: 0 }}>
              <Avatar name={ME.name} size={92}/>
              <button style={{ position: "absolute", right: -4, bottom: -4, width: 44, height: 44, borderRadius: 22, background: "var(--accent-ink)", border: "3px solid var(--bg)", display: "inline-flex", alignItems: "center", justifyContent: "center" }} aria-label="Retake profile photo">
                <Icon name="camera" size={19} color="#fff"/>
              </button>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 16, fontWeight: 800 }}>{ME.name}</div>
              <div style={{ fontSize: 12, color: "var(--ink-3)", marginTop: 3, lineHeight: 1.45 }}>
                Taken with the camera, like the rest. Retake it as often as you like — it isn't part of the identity check.
              </div>
            </div>
          </div>

          <div>
            <div className="rs-section-label" style={{ marginBottom: 9 }}>WHO CAN SEE IT</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 9 }}>
              {PAX_VERIFY.photoOptions.map(o => {
                const on = o.key === choice;
                return (
                  <button key={o.key} data-row={`photo ${o.key}`} style={{
                    textAlign: "left", minHeight: 64, padding: "13px 15px", borderRadius: 16, display: "flex", alignItems: "center", gap: 12, width: "100%",
                    background: on ? "var(--accent-soft)" : "var(--surface)", border: `1.5px solid ${on ? "var(--accent-ink)" : "var(--line)"}`,
                  }}>
                    <div style={{ width: 22, height: 22, borderRadius: 11, flexShrink: 0, border: `2px solid ${on ? "var(--accent-ink)" : "var(--line-2)"}`, background: on ? "var(--accent-ink)" : "transparent", display: "flex", alignItems: "center", justifyContent: "center" }}>
                      {on && <Icon name="check" size={12} color="#fff" strokeWidth={3}/>}
                    </div>
                    <Icon name={o.key === "HIDDEN" ? "eyeoff" : "eye"} size={18} color="var(--ink-2)"/>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 13.5, fontWeight: on ? 800 : 700 }}>{o.label}</div>
                      <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>{o.body}</div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          <Banner kind="warn" icon="shield" title="Hidden still means hidden from strangers only"
            body="On the two open settings the driver you're actually riding with can always see the photo once the booking is confirmed. He has thirty seconds at a kerb to recognise you."/>

          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 12 }}>WHAT A DRIVER SEES EITHER WAY</div>
            <div style={{ display: "flex", gap: 11 }}>
              {[["Shown", true], ["Hidden", false]].map(([l, shown]) => (
                <div key={l} style={{ flex: 1, padding: 12, borderRadius: 14, background: "var(--bg-soft)", border: "1px solid var(--line)", display: "flex", flexDirection: "column", alignItems: "center", gap: 8 }}>
                  {shown ? <Avatar name={ME.name} size={40}/> : (
                    <div style={{ width: 40, height: 40, borderRadius: 20, background: "var(--line)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                      <Icon name="user" size={20} color="var(--ink-3)"/>
                    </div>
                  )}
                  <div style={{ fontSize: 12.5, fontWeight: 700 }}>Nimali P.</div>
                  <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
                    <Icon name="badge" size={12} color="var(--status-approved-ink)"/>
                    <span style={{ fontSize: 10.5, fontWeight: 800, color: "var(--status-approved-ink)" }}>VERIFIED</span>
                  </div>
                  <div style={{ fontSize: 10.5, color: "var(--ink-3)" }}>{l}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">Submit for verification</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ P31 · VERIFICATION OUTCOME ═══════════
function PxVerifyStatusScreen({ state = "verified" }) {
  const cfg = {
    pending: {
      num: "P31", tag: "in review", icon: "clock", tint: "var(--status-pending-soft)", ink: "var(--status-pending-ink)",
      kicker: "IN REVIEW", title: "Four photos received",
      lead: "Most checks finish inside an hour. You can carry on booking exactly as before — verification adds seats, it never takes any away.",
    },
    verified: {
      num: "P31b", tag: "verified", icon: "badge", tint: "var(--status-approved-soft)", ink: "var(--status-approved-ink)",
      kicker: "VERIFIED", title: "You're verified",
      lead: `Your badge is live from today. Drivers who approve each request see you above unverified riders, and verified-only trips now appear in your results.`,
    },
    rejected: {
      num: "P31c", tag: "rejected", icon: "alert", tint: "var(--status-rejected-soft)", ink: "var(--status-rejected-ink)",
      kicker: "COULDN'T READ IT", title: "One photo needs redoing",
      lead: "The back of your NIC came out too dark to read the address block. Nothing else needs redoing — just that one.",
    },
  }[state];
  const done = { pending: 4, verified: 4, rejected: 3 }[state];
  return (
    <Phone label={`${cfg.num} Verification · ${cfg.tag}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Identity verification"/>
        <div style={{ flex: 1, overflow: "auto", padding: "20px 16px 16px" }} className="rs-scroll">
          <div style={{ display: "flex", alignItems: "center", gap: 13 }}>
            <div style={{ width: 56, height: 56, borderRadius: 19, background: cfg.tint, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
              <Icon name={cfg.icon} size={26} color={cfg.ink} strokeWidth={2.2}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em", color: cfg.ink }}>{cfg.kicker}</div>
              <div className="rs-display" style={{ fontSize: 25, marginTop: 3, lineHeight: 1.15 }}>{cfg.title}</div>
            </div>
          </div>
          <div style={{ fontSize: 13.5, color: "var(--ink-3)", marginTop: 12, lineHeight: 1.6, textWrap: "pretty" }}>{cfg.lead}</div>

          <div className="rs-card" style={{ padding: "2px 14px", marginTop: 16 }}>
            {VERIFY_STEPS.map((s, i) => {
              const bad = state === "rejected" && s.key === "back";
              const st = bad ? "rejected" : state === "verified" ? "approved" : i < done ? "approved" : "pending";
              return (
                <div key={s.key}>
                  {i > 0 && <div className="rs-divider"/>}
                  <div style={{ padding: "13px 0", display: "flex", alignItems: "center", gap: 11 }}>
                    <Icon name="camera" size={17} color="var(--ink-3)"/>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 13.5, fontWeight: 700 }}>{s.label}</div>
                      {bad && <div style={{ fontSize: 11.5, color: "var(--status-rejected-ink)", marginTop: 2, lineHeight: 1.4 }}>Too dark — the address block isn't legible</div>}
                    </div>
                    <StatusBadge status={st === "approved" ? "approved" : st === "rejected" ? "rejected" : "pending"} label={st === "approved" ? "OK" : st === "rejected" ? "REDO" : "CHECKING"}/>
                  </div>
                </div>
              );
            })}
          </div>

          {state === "verified" && (
            <div style={{ marginTop: 12 }}>
              <div className="rs-card" style={{ padding: 15 }}>
                <div className="rs-section-label" style={{ marginBottom: 12 }}>WHAT'S OPEN NOW</div>
                <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
                  {PAX_VERIFY.benefits.map(b => <RuleRow key={b.title} icon={b.icon} tint="var(--status-approved-ink)" title={b.title} body={b.body}/>)}
                </div>
              </div>
            </div>
          )}
          {state === "pending" && (
            <div style={{ marginTop: 12 }}>
              <Banner kind="info" icon="search" title="Nothing is blocked while we check"
                body="Book, pay and ride as normal. The badge appears on your profile the moment it clears."/>
            </div>
          )}
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">{state === "rejected" ? "Retake the back of my NIC" : state === "pending" ? "Keep looking for a ride" : "See verified-only trips"}</button>
          {state !== "rejected" && <button className="rs-btn ghost full" style={{ marginTop: 10 }}>Profile photo settings</button>}
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ P32 · REFERRAL ═══════════
// One screen, both modes. The rate depends on what the person you invited does,
// so the two rates are stated side by side rather than buried in small print.
function PxReferralScreen({ mode = "ride" }) {
  const t = mode === "drive" ? "var(--mode-drive)" : "var(--accent-ink)";
  const soft = mode === "drive" ? "var(--mode-drive-soft)" : "var(--accent-soft)";
  return (
    <Phone label={`${mode === "drive" ? "D37" : "P32"} Referral · invite people`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Invite and earn" sub={`${REFERRAL.joined} of ${REFERRAL.invited} invites have joined`}/>
        <div style={{ flex: 1, overflow: "auto", padding: "16px 16px 16px", display: "flex", flexDirection: "column", gap: 13 }} className="rs-scroll">
          <div style={{ padding: 18, borderRadius: 20, background: "var(--ink-fill)", color: "var(--on-ink-fill)" }}>
            <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em", opacity: .65 }}>EARNED FROM REFERRALS</div>
            <div className="rs-display tab" style={{ fontSize: 38, lineHeight: 1.05, marginTop: 6 }}>{FARE_POLICY.currency} {money(referralEarned())}</div>
            <div style={{ fontSize: 12, opacity: .8, marginTop: 5 }}>Across {REFERRAL.joined} people, from every trip they finish.</div>
          </div>

          <div style={{ display: "flex", gap: 10 }}>
            {[[POLICY.referralPaxPct, "they ride", "of the fare they pay"], [POLICY.referralDriverPct, "they drive", "of what they keep"]].map(([pct, when, sub]) => (
              <div key={when} style={{ flex: 1, padding: 14, borderRadius: 16, background: soft, border: "1px solid var(--line)" }}>
                <div className="rs-display tab" style={{ fontSize: 26, lineHeight: 1, color: t }}>{pct}%</div>
                <div style={{ fontSize: 12.5, fontWeight: 800, marginTop: 5 }}>when {when}</div>
                <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>{sub}</div>
              </div>
            ))}
          </div>

          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 10 }}>YOUR LINK</div>
            <div style={{ padding: "13px 14px", borderRadius: 14, background: "var(--bg-soft)", border: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 10 }}>
              <div className="tab" style={{ flex: 1, minWidth: 0, fontSize: 13, fontWeight: 700, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{REFERRAL.link}</div>
              <button className="rs-btn" style={{ height: 44, padding: "0 16px", fontSize: 12.5, flexShrink: 0, background: t, color: "var(--on-bright-fill)" }}>Copy</button>
            </div>
            <div style={{ display: "flex", gap: 9, marginTop: 10 }}>
              <button className="rs-btn soft" style={{ flex: 1, height: 46, fontSize: 12.5 }}><Icon name="share" size={16}/> WhatsApp</button>
              <button className="rs-btn soft" style={{ flex: 1, height: 46, fontSize: 12.5 }}><Icon name="mail" size={16}/> SMS</button>
            </div>
            <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 10, lineHeight: 1.45 }}>
              They get {FARE_POLICY.currency} {money(POLICY.refereeFirstRideDiscount)} off their first ride. You start earning on their first completed trip — a sign-up on its own pays nothing.
            </div>
          </div>

          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 12 }}>PEOPLE YOU INVITED</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
              {REFERRAL.rows.map(r => {
                const left = referralTripsLeft(r);
                const pct = Math.min(100, Math.round(r.trips / POLICY.referralMaxTrips * 100));
                return (
                  <div key={r.who}>
                    <div style={{ display: "flex", alignItems: "center", gap: 11 }}>
                      <Avatar name={r.who} size={38}/>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                          <div style={{ fontSize: 13.5, fontWeight: 700 }}>{r.who}</div>
                          <span className="rs-chip" style={{ height: 22, fontSize: 10, background: r.role === "driver" ? "var(--mode-drive-soft)" : "var(--accent-soft)", color: r.role === "driver" ? "var(--mode-drive-ink)" : "var(--accent-ink)", borderColor: "transparent" }}>
                            {r.role === "driver" ? `${POLICY.referralDriverPct}% · drives` : `${POLICY.referralPaxPct}% · rides`}
                          </span>
                        </div>
                        <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 2 }}>Joined {r.joined} · {r.trips} trips · {left} still earning</div>
                      </div>
                      <div className="tab" style={{ fontSize: 14, fontWeight: 800, flexShrink: 0, color: r.earned ? "var(--status-approved-ink)" : "var(--ink-3)" }}>
                        {r.earned ? `${FARE_POLICY.currency} ${money(r.earned)}` : "—"}
                      </div>
                    </div>
                    <div style={{ height: 5, borderRadius: 3, background: "var(--bg-soft)", marginTop: 8, overflow: "hidden" }}>
                      <div style={{ width: `${pct}%`, height: "100%", background: t, borderRadius: 3 }}/>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="rs-card" style={{ padding: 15 }}>
            <div className="rs-section-label" style={{ marginBottom: 12 }}>THE SMALL PRINT, SAID PLAINLY</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              <RuleRow icon="receipt" title="It comes out of ComiGo's cut"
                body={`Not out of the driver's earnings and not off your friend's fare. The ${FARE_POLICY.commissionPct}% commission is where it's paid from.`}/>
              <RuleRow icon="clock" title={`${POLICY.referralWindowMonths} months, or their first ${POLICY.referralMaxTrips} trips`}
                body="Whichever ends first. After that they're just a regular rider and you stop earning on them."/>
              <RuleRow icon="shield" title="One reward per verified person"
                body="A referral only counts once the invited account is verified, and only on trips that actually complete."/>
            </div>
          </div>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn full" style={{ background: t, color: mode === "drive" ? "var(--on-bright-fill)" : "#fff" }}>Share my link</button>
          <button className="rs-btn ghost full" style={{ marginTop: 10 }}>See my rewards balance</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ P33 / D38 · REWARDS BALANCE ═══════════
// One balance, two exits. A passenger spends it on rides with no floor; a driver
// moves it to a bank account once it clears the floor, in the Friday batch that
// already exists. There is no third, on-demand payout rail.
function PxRewardsScreen({ mode = "ride", held = false }) {
  const bal = held ? 420 : rewardsBalance();
  const drive = mode === "drive";
  const t = drive ? "var(--mode-drive)" : "var(--accent-ink)";
  const eligible = bal >= POLICY.rewardsBankMinimum;
  const toFloor = POLICY.rewardsBankMinimum - bal;
  const META = {
    referral: { icon: "gift", c: "var(--status-approved-ink)", bg: "var(--status-approved-soft)" },
    comp: { icon: "shield", c: "var(--status-approved-ink)", bg: "var(--status-approved-soft)" },
    spend: { icon: "car", c: "var(--ink-2)", bg: "var(--bg-soft)" },
  };
  return (
    <Phone label={`${drive ? "D38" : "P33"} Rewards balance · ${drive ? (held ? "under the floor" : "withdraw to bank") : "ride credit"}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Rewards balance" sub="Referrals and compensation"/>
        <div style={{ flex: 1, overflow: "auto", padding: "16px 16px 16px", display: "flex", flexDirection: "column", gap: 13 }} className="rs-scroll">
          <div style={{ padding: 18, borderRadius: 20, background: "var(--ink-fill)", color: "var(--on-ink-fill)" }}>
            <div style={{ fontSize: 10, fontWeight: 800, letterSpacing: ".12em", opacity: .65 }}>AVAILABLE</div>
            <div className="rs-display tab" style={{ fontSize: 42, lineHeight: 1.05, marginTop: 6 }}>{FARE_POLICY.currency} {money(bal)}</div>
            <div style={{ fontSize: 12.5, opacity: .82, marginTop: 5, lineHeight: 1.5 }}>
              {drive
                ? (eligible
                  ? `Goes to ${PAYOUT.last.to} in the ${PAYOUT.day} batch, alongside your fares.`
                  : `${FARE_POLICY.currency} ${money(toFloor)} short of the ${FARE_POLICY.currency} ${money(POLICY.rewardsBankMinimum)} bank minimum. It's held, not lost.`)
                : "Comes off your next booking automatically. No minimum, no expiry."}
            </div>
            {drive && !eligible && (
              <div style={{ marginTop: 12, height: 7, borderRadius: 4, background: "rgba(255,255,255,.16)", overflow: "hidden" }}>
                <div style={{ width: `${Math.round(bal / POLICY.rewardsBankMinimum * 100)}%`, height: "100%", background: "#48a89f", borderRadius: 4 }}/>
              </div>
            )}
          </div>

          <div style={{ display: "flex", gap: 10 }}>
            <div style={{ flex: 1, padding: 14, borderRadius: 16, background: "var(--surface)", border: "1px solid var(--line)" }}>
              <div className="rs-display tab" style={{ fontSize: 22, lineHeight: 1 }}>{FARE_POLICY.currency} {money(referralEarned())}</div>
              <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 4 }}>Earned from referrals</div>
            </div>
            <div style={{ flex: 1, padding: 14, borderRadius: 16, background: "var(--surface)", border: "1px solid var(--line)" }}>
              <div className="rs-display tab" style={{ fontSize: 22, lineHeight: 1 }}>{REFERRAL.joined}</div>
              <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 4 }}>People still earning</div>
            </div>
          </div>

          <div className="rs-card" style={{ padding: "2px 14px" }}>
            {REWARDS_ROWS.map((r, i) => {
              const m = META[r.kind];
              return (
                <div key={r.t + r.label}>
                  {i > 0 && <div className="rs-divider"/>}
                  <div style={{ padding: "13px 0", display: "flex", alignItems: "center", gap: 11 }}>
                    <div style={{ width: 34, height: 34, borderRadius: 11, background: m.bg, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                      <Icon name={m.icon} size={16} color={m.c}/>
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 13, fontWeight: 700 }}>{r.label}</div>
                      <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>{r.sub}</div>
                      <div className="tab" style={{ fontSize: 10.5, color: "var(--ink-3)", marginTop: 3 }}>{r.t}</div>
                    </div>
                    <div className="tab" style={{ fontSize: 13.5, fontWeight: 800, flexShrink: 0, color: r.v < 0 ? "var(--ink-2)" : "var(--status-approved-ink)" }}>
                      {r.v < 0 ? "−" : "+"}{FARE_POLICY.currency} {money(Math.abs(r.v))}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          <Banner kind="info" icon={drive ? "wallet" : "car"}
            title={drive ? `Bank transfers need ${FARE_POLICY.currency} ${money(POLICY.rewardsBankMinimum)}` : "Ride credit has no minimum"}
            body={drive
              ? `Below the floor the balance is held and goes out with a later ${PAYOUT.day}. You can spend any amount of it on your own rides instead, at any time.`
              : `Even ${FARE_POLICY.currency} 20 comes off your next fare. If you also drive, you can send it to a bank account once it passes ${FARE_POLICY.currency} ${money(POLICY.rewardsBankMinimum)}.`}/>
        </div>
        <div style={{ padding: "12px 16px 16px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          {drive ? (
            <button className="rs-btn full" disabled={!eligible} style={{ background: eligible ? t : "var(--bg-soft)", color: eligible ? "var(--on-bright-fill)" : "var(--ink-3)", border: eligible ? "none" : "1px solid var(--line)" }}>
              {eligible ? `Send ${FARE_POLICY.currency} ${money(bal)} to ${PAYOUT.last.to}` : `${FARE_POLICY.currency} ${money(toFloor)} more to withdraw`}
            </button>
          ) : (
            <button className="rs-btn accent full">Use it on my next ride</button>
          )}
          <button className="rs-btn ghost full" style={{ marginTop: 10 }}>Invite more people</button>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, {
  PxVerifyIntroScreen, PxVerifyCaptureScreen, PxProfilePhotoScreen, PxVerifyStatusScreen,
  PxReferralScreen, PxRewardsScreen, VerifyGuide,
});
