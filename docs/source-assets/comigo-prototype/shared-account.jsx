// shared-account.jsx — one Account, one Trip history, one Settings, one
// verification flow. Mode-aware sections replace the old 20 + D27 pair.

// ═══════════ S15 · ACCOUNT (both roles approved) ═══════════
function AccountScreen({ driver = true, driverStatus = "APPROVED", mode = "ride" }) {
  const t = modeTint(mode);
  return (
    <Phone label={driver ? "S15 Account · dual role" : "S16 Account · rider only"}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ flex: 1, overflow: "auto" }} className="rs-scroll">
          <div style={{ padding: "18px 20px 22px", background: "var(--ink-fill)", color: "var(--on-ink-fill)", borderBottomLeftRadius: 28, borderBottomRightRadius: 28 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
              <Avatar name="Nimali Perera" size={56}/>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 19, fontWeight: 800 }}>Nimali Perera</div>
                <div className="tab" style={{ fontSize: 12.5, opacity: .78, marginTop: 2 }}>+94 77 412 8890</div>
              </div>
              <button style={{ width: 44, height: 44, borderRadius: 22, background: "rgba(255,255,255,.16)", display: "inline-flex", alignItems: "center", justifyContent: "center" }} aria-label="Edit profile">
                <Icon name="settings" size={19} color="var(--on-ink-fill)"/>
              </button>
            </div>
            <div style={{ display: "flex", gap: 8, marginTop: 14, flexWrap: "wrap" }}>
              <div style={{ height: 26, padding: "0 10px", borderRadius: 999, background: "rgba(255,255,255,.16)", fontSize: 11, fontWeight: 700, display: "inline-flex", alignItems: "center", gap: 5 }}>
                <Icon name="check" size={12} color="var(--on-ink-fill)" strokeWidth={2.8}/> Identity verified
              </div>
              <div style={{ height: 26, padding: "0 10px", borderRadius: 999, background: "rgba(255,255,255,.16)", fontSize: 11, fontWeight: 700, display: "inline-flex", alignItems: "center", gap: 5 }}>
                <Icon name="star" size={12} color="var(--on-ink-fill)"/> {TRUST.passenger.rating} rider · {TRUST.driver.rating} driver
              </div>
            </div>
          </div>

          <div style={{ padding: "0 16px 20px" }}>
            {/* current mode — the second placement of the switcher */}
            <GroupLabel>YOU'RE IN</GroupLabel>
            {driver && driverStatus === "APPROVED" ? (
              <div style={{ padding: 14, borderRadius: 18, background: t.soft, border: `1.5px solid ${t.c}`, display: "flex", alignItems: "center", gap: 12 }}>
                <div style={{ width: 40, height: 40, borderRadius: 13, background: t.c, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                  <Icon name={t.icon} size={19} color="var(--on-bright-fill)"/>
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 14.5, fontWeight: 800, color: t.ink }}>{mode === "ride" ? "Passenger mode" : "Driver mode"}</div>
                  <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>Switch any time — nothing in progress is lost.</div>
                </div>
                <div style={{ minHeight: 44, padding: "0 14px", borderRadius: 999, background: "var(--surface)", display: "inline-flex", alignItems: "center", justifyContent: "center", gap: 6, fontSize: 12, fontWeight: 800, color: t.ink, flexShrink: 0 }}>
                  <Icon name="swap" size={14} color={t.ink} strokeWidth={2.2}/>{t.other}
                </div>
              </div>
            ) : (
              <div style={{ padding: 16, borderRadius: 18, background: "var(--mode-drive-soft)", border: "1px solid var(--line)" }}>
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  <div style={{ width: 40, height: 40, borderRadius: 13, background: "var(--mode-drive)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                    <Icon name="car" size={19} color="var(--on-bright-fill)"/>
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 14.5, fontWeight: 800, color: "var(--mode-drive-ink)" }}>Start earning with ComiGo</div>
                    <div style={{ fontSize: 11.5, color: "var(--ink-2)", marginTop: 2, lineHeight: 1.45 }}>Publish the trips you already make. Takes about 10 minutes and one photo of your licence.</div>
                  </div>
                </div>
                <button className="rs-btn full" style={{ marginTop: 12, height: 46, background: "var(--mode-drive)", color: "var(--on-bright-fill)" }}>Become a driver</button>
              </div>
            )}

            <GroupLabel>RIDING</GroupLabel>
            <div className="rs-card" style={{ padding: "2px 12px" }}>
              <MenuRow icon="history" label="Your trips" sub="14 rides · 3 drives"/>
              <div className="rs-divider"/>
              <MenuRow icon="pin" label="Saved places" sub="Home, Work, Amma's"/>
              <div className="rs-divider"/>
              <MenuRow icon="card" label="Payment methods" sub="Visa ···4429 · Cash"/>
              <div className="rs-divider"/>
              <MenuRow icon="receipt" label="Outstanding amounts" sub={`${FARE_POLICY.currency} ${money(duesTotal())} carried from 21 Jul`} badge={<StatusBadge status="pending" label="UNPAID"/>}/>
            </div>

            {driver && (
              <>
                <GroupLabel>DRIVING</GroupLabel>
                <div className="rs-card" style={{ padding: "2px 12px" }}>
                  <MenuRow icon="cash" label="Earnings & payouts" sub={`${FARE_POLICY.currency} ${money(PAYOUT.balance)} in your wallet · paid ${PAYOUT.nextDate}`}/>
                  <div className="rs-divider"/>
                  <MenuRow icon="car" label="Vehicles" sub={`${MY_VEHICLE.make} · ${MY_VEHICLE.plate}`} badge={<StatusBadge status="expiring" label={`INSURANCE ${MY_VEHICLE.insuranceDaysLeft}d`}/>} chev={false}/>
                  <div className="rs-divider"/>
                  <MenuRow icon="calendar" label="Recurring routes" sub={`${NEXT_DRIVE.recurring} active`}/>
                  <div className="rs-divider"/>
                  <MenuRow icon="cash" label="Your per-km rate" sub={`${FARE_POLICY.currency} ${RATE_BAND.chosen} per km · band ${RATE_BAND.min}–${RATE_BAND.max} set by ${RATE_BAND.setBy}`}/>
                  <div className="rs-divider"/>
                  <MenuRow icon="user" label="Driving preferences" sub={`${DRIVER_PREFS.gender === "ANYONE" ? "Anyone can book" : "Women only"} · verified riders only · mid-trip bookings on`}/>
                  <div className="rs-divider"/>
                  <MenuRow icon="star" label="Ratings & reviews" sub={`${DRIVER_TODAY.rating} · ${DRIVER_TODAY.trips} reviews`}/>
                </div>
              </>
            )}

            <GroupLabel>REWARDS</GroupLabel>
            <div className="rs-card" style={{ padding: "2px 12px" }}>
              <MenuRow icon="users" label="Invite friends" sub={`${REFERRAL.joined} joined · ${FARE_POLICY.currency} ${money(referralEarned())} earned so far`}/>
              <div className="rs-divider"/>
              <MenuRow icon="cash" label="Rewards balance" sub={driver
                ? `${FARE_POLICY.currency} ${money(rewardsBalance())} · ${rewardsWithdrawable() ? `withdraw on ${PAYOUT.nextDate}` : `${FARE_POLICY.currency} ${money(POLICY.rewardsBankMinimum)} needed to withdraw`}`
                : `${FARE_POLICY.currency} ${money(rewardsBalance())} · spend it on your next ride`}/>
            </div>

            <GroupLabel>ACCOUNT &amp; SAFETY</GroupLabel>
            <div className="rs-card" style={{ padding: "2px 12px" }}>
              <MenuRow icon="shield" label="Verification" sub="Identity approved · licence approved" badge={<StatusBadge status="approved"/>} chev={false}/>
              <div className="rs-divider"/>
              <MenuRow icon="users" label="Trusted contacts" sub="2 people can follow your trips"/>
              <div className="rs-divider"/>
              <MenuRow icon="bell" label="Notifications" sub="Push, SMS and in-app"/>
              <div className="rs-divider"/>
              <MenuRow icon="settings" label="Settings"/>
              <div className="rs-divider"/>
              <MenuRow icon="help" label="Help &amp; support" sub="1 open ticket" badge={<div style={{ width: 8, height: 8, borderRadius: 4, background: "var(--danger)" }}/>}/>
            </div>

            <div style={{ marginTop: 18, display: "flex", flexDirection: "column", gap: 4 }}>
              <button style={{ minHeight: 48, textAlign: "center", color: "var(--status-rejected-ink)", fontWeight: 700, fontSize: 14 }}>Sign out</button>
              <div style={{ textAlign: "center", fontSize: 11, color: "var(--ink-3)" }}>ComiGo 1.0.0 · Colombo</div>
            </div>
          </div>
        </div>
        <TabBar mode={mode} active="account" badges={{ inbox: 3 }}/>
      </div>
    </Phone>
  );
}

// ═══════════ S17 · TRIP HISTORY (rides + drives in one place) ═══════════
function TripHistoryScreen({ tab = "Rides" }) {
  const rides = [
    { d: "Today · 8:04 AM", from: RIDES[0].from, to: RIDES[0].to, who: RIDES[0].driver, amt: RIDES[0].price, m: RIDES[0].match, st: "Completed" },
    { d: "22 Jul · 6:15 PM", from: MY_TRIP.from, to: MY_TRIP.to, who: MY_TRIP.driver, amt: MY_TRIP.price, m: MY_TRIP.match, st: "Completed" },
    { d: CANCELLED_RIDE.d, from: CANCELLED_RIDE.from, to: CANCELLED_RIDE.to, who: CANCELLED_RIDE.who, amt: 0, m: CANCELLED_RIDE.m, st: CANCELLED_RIDE.reason },
  ];
  const drives = DRIVE_HISTORY.map(t => ({ d: t.d, from: t.from, to: t.to, who: `${t.pax} passengers`, amt: t.amt, m: null, st: "Completed" }));
  const rows = tab === "Rides" ? rides : drives;
  return (
    <Phone label={`S17 Trip history · ${tab}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Your trips"/>
        <div style={{ padding: "12px 16px" }}>
          <Segmented options={["Rides", "Drives"]} value={tab}/>
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: "0 16px 16px", display: "flex", flexDirection: "column", gap: 10 }} className="rs-scroll">
          {rows.map(r => {
            const cancelled = r.st !== "Completed";
            return (
              <div key={r.d} className="rs-card" style={{ padding: 14 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                  <div style={{ fontSize: 10.5, fontWeight: 800, letterSpacing: ".08em", color: "var(--ink-3)", flex: 1 }}>{r.d.toUpperCase()}</div>
                  {cancelled
                    ? <StatusBadge status="rejected" label="CANCELLED"/>
                    : <div className="tab" style={{ fontSize: 15, fontWeight: 800 }}>LKR {money(r.amt)}</div>}
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: 12, marginTop: 11 }}>
                  {r.m != null
                    ? <MatchRing value={r.m} size={40} strokeWidth={3.4}/>
                    : <div style={{ width: 40, height: 40, borderRadius: 13, background: "var(--mode-drive-soft)", display: "flex", alignItems: "center", justifyContent: "center" }}><Icon name="car" size={19} color="var(--mode-drive-ink)"/></div>}
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13.5, fontWeight: 700, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{r.from} → {r.to}</div>
                    <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2 }}>{r.who}{cancelled ? ` · ${r.st}` : ""}</div>
                  </div>
                  <Icon name="chev" size={17} color="var(--ink-3)"/>
                </div>
              </div>
            );
          })}
          <div style={{ marginTop: 4, display: "flex", flexDirection: "column", gap: 10 }}>
            <div className="rs-section-label">LOADING MORE</div>
            <SkelRow/>
          </div>
        </div>
        <TabBar mode={tab === "Rides" ? "ride" : "drive"} active="trips" badges={{ inbox: 3 }}/>
      </div>
    </Phone>
  );
}

// ═══════════ S18 · SETTINGS ═══════════
function SettingsScreen() {
  return (
    <Phone label="S18 Settings">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Settings"/>
        <div style={{ flex: 1, overflow: "auto", padding: "0 16px 20px" }} className="rs-scroll">
          <GroupLabel>APPEARANCE</GroupLabel>
          <div className="rs-card" style={{ padding: "2px 12px" }}>
            <MenuRow icon="leaf" label="Theme" sub="Follow system" chev/>
            <div className="rs-divider"/>
            <MenuRow icon="mail" label="Language" sub="English · සිංහල · தமிழ்"/>
          </div>
          <GroupLabel>PRIVACY</GroupLabel>
          <div className="rs-card" style={{ padding: "2px 12px" }}>
            <MenuRow icon="pin" label="Share live location on trips" sub="Only while a trip is active" right={<Toggle on/>} chev={false}/>
            <div className="rs-divider"/>
            <MenuRow icon="user" label="Show my rating publicly" right={<Toggle on/>} chev={false}/>
            <div className="rs-divider"/>
            <MenuRow icon="lock" label="Download my data"/>
          </div>
          <GroupLabel>PAYMENTS</GroupLabel>
          <div className="rs-card" style={{ padding: "2px 12px" }}>
            <MenuRow icon="card" label="Payment methods" sub="Visa ···4429 · Cash"/>
            <div className="rs-divider"/>
            <MenuRow icon="receipt" label="Receipts by email" sub="nimali.p@comigo.lk" right={<Toggle on/>} chev={false}/>
          </div>
          <GroupLabel>ABOUT</GroupLabel>
          <div className="rs-card" style={{ padding: "2px 12px" }}>
            <MenuRow icon="thumb" label="Rate ComiGo" sub="Opens the Play Store" badge={<NeedsBackend>STORE LINK</NeedsBackend>} chev={false}/>
            <div className="rs-divider"/>
            <MenuRow icon="shield" label="Terms &amp; privacy"/>
            <div className="rs-divider"/>
            <MenuRow icon="alert" label="Community rules"/>
          </div>
          <div style={{ marginTop: 18 }}>
            <button style={{ width: "100%", minHeight: 48, textAlign: "center", color: "var(--status-rejected-ink)", fontWeight: 700, fontSize: 14 }}>Delete my account</button>
            <div style={{ textAlign: "center", fontSize: 11, color: "var(--ink-3)", lineHeight: 1.5, marginTop: 4 }}>Deleting removes your profile and saved places. Trip receipts are kept for 7 years as required by law.</div>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ S19 · VERIFICATION STATUS (shared: rider ID + driver docs) ═══════════
function VerificationScreen() {
  const docs = DRIVER_VERIFICATION.docs;
  const blockers = vBlockers();
  const warnings = vWarnings();
  return (
    <Phone label="S19 Verification">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Verification" sub="Identity and documents"/>
        <div style={{ flex: 1, overflow: "auto", padding: "16px 16px 20px", display: "flex", flexDirection: "column", gap: 12 }} className="rs-scroll">
          <div style={{ padding: 16, borderRadius: 18, background: "var(--status-pending-soft)", border: "1px solid var(--status-pending)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <Icon name="alert" size={18} color="var(--status-pending-ink)"/>
              <div style={{ fontSize: 14, fontWeight: 800, color: "var(--status-pending-ink)", flex: 1 }}>{blockers.length} things need you</div>
            </div>
            <div style={{ fontSize: 12, color: "var(--ink-2)", marginTop: 7, lineHeight: 1.5 }}>{`Your ${blockers.map(b => b.label.toLowerCase()).join(" and ")} ${blockers.length > 1 ? "are" : "is"} outstanding, so publishing is paused. Bookings already taken are unaffected. Your insurance also expires in ${MY_VEHICLE.insuranceDaysLeft} days.`}</div>
          </div>
          {docs.map(d => (
            <div key={d.key} className="rs-card" style={{ padding: 14, display: "flex", alignItems: "center", gap: 12 }}>
              <div style={{ width: 40, height: 40, borderRadius: 13, background: STATUS_META[d.st].bg, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                <Icon name={STATUS_META[d.st].icon} size={19} color={STATUS_META[d.st].c}/>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 13.5, fontWeight: 700 }}>{d.label}</div>
                <div style={{ fontSize: 11.5, color: "var(--ink-3)", marginTop: 2, lineHeight: 1.4 }}>{d.detail}</div>
              </div>
              {d.action
                ? <button className="rs-btn accent" style={{ height: 44, padding: "0 18px", fontSize: 12.5, flexShrink: 0 }}>{d.action}</button>
                : <StatusBadge status="approved"/>}
            </div>
          ))}
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ S20 · DOCUMENT UPLOAD (capture → uploading) ═══════════
function DocUploadScreen() {
  return (
    <Phone label="S20 Document upload" statusDark statusBg="#1b1410">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--ink-fill)" }}>
        <AppBar title="Revenue licence" sub="Step 2 of 2 · capture" onDark/>
        <div style={{ flex: 1, padding: "8px 20px 0", display: "flex", flexDirection: "column" }}>
          {/* camera viewfinder */}
          <div style={{ flex: 1, borderRadius: 20, background: "#2a241e", position: "relative", overflow: "hidden", display: "flex", alignItems: "center", justifyContent: "center" }}>
            <div style={{ width: "82%", height: "56%", borderRadius: 12, border: "2px dashed rgba(244,236,224,.5)", display: "flex", alignItems: "center", justifyContent: "center" }}>
              <div style={{ fontSize: 12, color: "rgba(244,236,224,.72)", fontWeight: 600 }}>Fit the whole card inside</div>
            </div>
            <div style={{ position: "absolute", left: 14, right: 14, bottom: 14, padding: "10px 12px", borderRadius: 12, background: "rgba(27,20,16,.82)", display: "flex", gap: 9, alignItems: "flex-start" }}>
              <Icon name="alert" size={15} color="#e8834f" strokeWidth={2.2}/>
              <div style={{ fontSize: 11, color: "var(--on-ink-fill)", lineHeight: 1.45 }}>Last time the edge was cut off. Lay it flat, avoid flash glare, and keep all four corners visible.</div>
            </div>
          </div>
          {/* upload progress */}
          <div style={{ marginTop: 14, padding: 14, borderRadius: 16, background: "rgba(255,255,255,.08)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <div style={{ width: 34, height: 34, borderRadius: 11, background: "rgba(255,255,255,.12)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="receipt" size={17} color="var(--on-ink-fill)"/>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 12.5, fontWeight: 700, color: "var(--on-ink-fill)" }}>licence-front.jpg</div>
                <div style={{ fontSize: 11, color: "rgba(244,236,224,.66)", marginTop: 1 }}>Uploading · 1.2 MB of 2.1 MB</div>
              </div>
              <button style={{ minHeight: 44, minWidth: 44, padding: "0 8px", fontSize: 12, fontWeight: 700, color: "rgba(244,236,224,.8)" }}>Cancel</button>
            </div>
            <div style={{ height: 5, borderRadius: 3, background: "rgba(255,255,255,.14)", marginTop: 11, overflow: "hidden" }}>
              <div style={{ width: "57%", height: "100%", borderRadius: 3, background: "#e8834f" }}/>
            </div>
          </div>
        </div>
        <div style={{ padding: "14px 20px 18px", display: "flex", gap: 10 }}>
          <button className="rs-btn" style={{ flex: 1, background: "rgba(255,255,255,.12)", color: "var(--on-ink-fill)" }}>
            <Icon name="receipt" size={17} color="var(--on-ink-fill)"/> Choose file
          </button>
          <button className="rs-btn accent" style={{ flex: 1.4 }}>
            <Icon name="target" size={17} color="#fff"/> Capture
          </button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════ S21 · DOCUMENT REVIEW OUTCOMES ═══════════
function DocOutcomeScreen({ outcome = "pending" }) {
  const map = {
    pending: {
      icon: "clock", title: "We're checking it now", body: "Most documents are reviewed within one working day. You'll get a notification either way — no need to keep opening the app.",
      extra: <Banner kind="info" icon="clock" title="Submitted 24 Jul, 11:04 AM" body="Nothing else is needed from you right now."/>,
      cta: "Back to verification", ghost: "Notify me by SMS too",
    },
    approved: {
      icon: "check", title: "Revenue licence approved", body: "Everything on your account is now valid. Publishing is back on and your paused route has been restored.",
      extra: <Banner kind="good" icon="check" title="Publishing re-enabled" body="Nugegoda → Colombo Fort, weekdays 7:45 AM is live again."/>,
      cta: "View my route", ghost: "Back to account",
    },
    rejected: {
      icon: "alert", title: "We couldn't accept this one", body: "Nothing else on your account changed. Retake the photo and we'll look again — usually within a day.",
      extra: (
        <div style={{ padding: 15, borderRadius: 18, background: "var(--status-rejected-soft)", border: "1px solid var(--status-rejected)" }}>
          <div className="rs-section-label" style={{ color: "var(--status-rejected-ink)" }}>REASON FROM THE REVIEWER</div>
          <div style={{ fontSize: 13, color: "var(--ink-2)", marginTop: 8, lineHeight: 1.55 }}>"The right edge of the card is outside the frame, so we can't read the expiry date. Please lay it flat and include all four corners."</div>
          <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 9 }}>24 Jul, 2:40 PM · attempt 2 of 3</div>
        </div>
      ),
      cta: "Retake photo", ghost: "Talk to support",
    },
  }[outcome];
  const st = outcome === "approved" ? "approved" : outcome === "rejected" ? "rejected" : "pending";
  const m = STATUS_META[st];
  return (
    <Phone label={`S21 Document · ${outcome}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <AppBar title="Revenue licence"/>
        <div style={{ flex: 1, padding: "24px 24px 0", display: "flex", flexDirection: "column", gap: 16 }}>
          <div style={{ width: 64, height: 64, borderRadius: 21, background: m.bg, display: "flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name={map.icon} size={30} color={m.c}/>
          </div>
          <div>
            <div className="rs-display" style={{ fontSize: 26, lineHeight: 1.2 }}>{map.title}</div>
            <div style={{ fontSize: 13.5, color: "var(--ink-3)", marginTop: 10, lineHeight: 1.6, textWrap: "pretty" }}>{map.body}</div>
          </div>
          {map.extra}
        </div>
        <div style={{ padding: "14px 24px 18px" }}>
          <button className="rs-btn accent full">{map.cta}</button>
          <button className="rs-btn ghost full" style={{ marginTop: 10 }}>{map.ghost}</button>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, { AccountScreen, TripHistoryScreen, SettingsScreen, VerificationScreen, DocUploadScreen, DocOutcomeScreen });
