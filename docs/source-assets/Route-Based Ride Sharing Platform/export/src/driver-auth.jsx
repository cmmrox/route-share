// driver-auth.jsx — splash, onboarding, login, OTP, KYC steps, pending review

// ═══════════════════════════════════════════════════════════════════
// SPLASH (driver)
// ═══════════════════════════════════════════════════════════════════
function DrSplashScreen() {
  return (
    <Phone label="D01 Splash">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 18, background: "var(--ink)", color: "#fff" }}>
        <div style={{ width: 88, height: 88, borderRadius: 28, background: "var(--accent)", display: "flex", alignItems: "center", justifyContent: "center", boxShadow: "0 12px 32px rgba(214,106,59,.4)" }}>
          <Icon name="car" size={44} color="#fff" strokeWidth={2.2}/>
        </div>
        <div style={{ textAlign: "center" }}>
          <div className="rs-display" style={{ fontSize: 38, lineHeight: 1 }}>RouteShare</div>
          <div style={{ marginTop: 8, opacity: .7, fontSize: 13, fontWeight: 600, letterSpacing: ".14em" }}>FOR DRIVERS</div>
        </div>
        <div style={{ position: "absolute", bottom: 40, fontSize: 11, opacity: .55, letterSpacing: ".14em", fontWeight: 600 }}>EARN ON THE TRIPS YOU ALREADY MAKE</div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// ONBOARDING (driver-specific)
// ═══════════════════════════════════════════════════════════════════
function DrOnboardingScreen({ step = 0 }) {
  const slides = [
    { title: "Earn from empty seats", body: "Heading to work anyway? Publish your route and let passengers pay you to ride along.", illo: "earn" },
    { title: "Drive your own schedule", body: "Set one-time or recurring trips. We only show you passengers who match your route.", illo: "schedule" },
    { title: "Get paid weekly", body: "Cash collected on the spot, card payments settled Mondays. We handle the rest.", illo: "payout" },
  ];
  const s = slides[step];
  return (
    <Phone label={`D02 Onboarding ${step + 1}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "14px 22px", display: "flex", justifyContent: "flex-end" }}>
          <button style={{ fontSize: 14, color: "var(--ink-3)", fontWeight: 600 }}>Skip</button>
        </div>
        <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", padding: "12px 28px", gap: 24 }}>
          <div style={{ width: "100%", aspectRatio: "1.1", borderRadius: 24, background: "var(--ink)", position: "relative", overflow: "hidden", color: "#fff" }}>
            <DrOnboardIllo kind={s.illo}/>
          </div>
          <div style={{ textAlign: "center" }}>
            <div className="rs-display" style={{ fontSize: 28, lineHeight: 1.1, color: "var(--ink)" }}>{s.title}</div>
            <div style={{ marginTop: 12, fontSize: 15, color: "var(--ink-3)", lineHeight: 1.45 }}>{s.body}</div>
          </div>
        </div>
        <div style={{ padding: "0 24px 28px" }}>
          <div style={{ display: "flex", justifyContent: "center", gap: 6, marginBottom: 20 }}>
            {[0,1,2].map(i => (
              <div key={i} style={{ height: 6, width: i === step ? 22 : 6, borderRadius: 3, background: i === step ? "var(--accent)" : "var(--line-2)", transition: "width .2s" }}/>
            ))}
          </div>
          <button className="rs-btn accent full">{step === 2 ? "Become a driver" : "Next"}</button>
        </div>
      </div>
    </Phone>
  );
}

function DrOnboardIllo({ kind }) {
  if (kind === "earn") {
    return (
      <svg viewBox="0 0 280 250" style={{ width: "100%", height: "100%" }}>
        <g transform="translate(140 130)">
          <rect x="-60" y="-30" width="120" height="60" rx="14" fill="var(--accent)"/>
          <rect x="-50" y="-20" width="100" height="14" rx="3" fill="rgba(255,255,255,.3)"/>
          <circle cx="-30" cy="35" r="10" fill="var(--accent)" stroke="#fff" strokeWidth="3"/>
          <circle cx="30" cy="35" r="10" fill="var(--accent)" stroke="#fff" strokeWidth="3"/>
          {/* Empty seats */}
          <rect x="-44" y="-14" width="22" height="22" rx="4" fill="rgba(255,255,255,.25)"/>
          <rect x="-12" y="-14" width="22" height="22" rx="4" fill="rgba(255,255,255,.25)"/>
          <rect x="22" y="-14" width="22" height="22" rx="4" fill="rgba(255,255,255,.25)"/>
        </g>
        {/* Money signs floating up */}
        <text x="60" y="80" fontFamily="Fraunces, serif" fontSize="40" fill="#fff" opacity=".7">+</text>
        <text x="200" y="100" fontFamily="Fraunces, serif" fontSize="32" fill="#fff" opacity=".5">+</text>
        <text x="100" y="60" fontFamily="Fraunces, serif" fontSize="24" fill="#fff" opacity=".4">+</text>
        <g transform="translate(190 200)">
          <rect x="-40" y="-18" width="80" height="36" rx="10" fill="#fff"/>
          <text x="0" y="6" textAnchor="middle" fontFamily="Fraunces, serif" fontSize="20" fontWeight="600" fill="var(--accent)">LKR 540</text>
        </g>
      </svg>
    );
  }
  if (kind === "schedule") {
    return (
      <svg viewBox="0 0 280 250" style={{ width: "100%", height: "100%" }}>
        {[0,1,2,3,4,5,6].map(i => (
          <g key={i} transform={`translate(${24 + i * 36} 70)`}>
            <rect width="28" height="28" rx="6" fill={i < 5 ? "var(--accent)" : "rgba(255,255,255,.18)"}/>
            <text x="14" y="19" textAnchor="middle" fontSize="14" fontWeight="700" fill="#fff">{["M","T","W","T","F","S","S"][i]}</text>
          </g>
        ))}
        <g transform="translate(40 130)">
          <rect width="200" height="80" rx="14" fill="rgba(255,255,255,.1)"/>
          <text x="16" y="28" fontSize="10" fontWeight="700" fill="#fff" opacity=".6">RECURRING TRIP</text>
          <text x="16" y="54" fontFamily="Fraunces, serif" fontSize="22" fontWeight="500" fill="#fff">8:00 AM weekdays</text>
          <text x="16" y="72" fontSize="11" fill="#fff" opacity=".7">Rajagiriya → Colombo Fort</text>
        </g>
      </svg>
    );
  }
  if (kind === "payout") {
    return (
      <svg viewBox="0 0 280 250" style={{ width: "100%", height: "100%" }}>
        <g transform="translate(50 60)">
          <rect width="180" height="50" rx="10" fill="rgba(255,255,255,.12)"/>
          <text x="14" y="20" fontSize="9" fontWeight="700" fill="#fff" opacity=".6">THIS WEEK</text>
          <text x="14" y="42" fontFamily="Fraunces, serif" fontSize="22" fontWeight="500" fill="#fff">LKR 12,840</text>
          <text x="170" y="32" textAnchor="end" fontSize="14" fill="var(--accent)" fontWeight="700">+18%</text>
        </g>
        <g transform="translate(50 130)">
          <rect width="180" height="80" rx="10" fill="var(--accent)"/>
          <text x="14" y="22" fontSize="9" fontWeight="700" fill="#fff" opacity=".75">NEXT PAYOUT · MONDAY</text>
          <text x="14" y="50" fontFamily="Fraunces, serif" fontSize="32" fontWeight="500" fill="#fff">LKR 8,420</text>
          <text x="14" y="68" fontSize="10" fill="#fff" opacity=".75">to BOC ··· 2204</text>
        </g>
        <g transform="translate(170 30)" opacity=".3">
          <circle r="18" fill="#fff"/>
          <text y="6" textAnchor="middle" fontSize="22" fill="var(--ink)">$</text>
        </g>
      </svg>
    );
  }
  return null;
}

// ═══════════════════════════════════════════════════════════════════
// LOGIN — same as passenger but framed as "Driver sign in"
// ═══════════════════════════════════════════════════════════════════
function DrLoginScreen() {
  return (
    <Phone label="D03 Login">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", padding: "8px 24px 24px", background: "var(--bg)" }}>
        <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
          <Icon name="back" size={20}/>
        </button>
        <div style={{ marginTop: 24 }}>
          <div className="rs-chip teal" style={{ marginBottom: 14 }}><Icon name="car" size={12}/> Driver app</div>
          <div className="rs-display" style={{ fontSize: 30 }}>Get on the road</div>
          <div style={{ color: "var(--ink-3)", marginTop: 6, fontSize: 14 }}>Sign in or apply to become a RouteShare driver</div>
        </div>
        <div style={{ marginTop: 28 }}>
          <div className="rs-section-label" style={{ marginBottom: 8 }}>MOBILE NUMBER</div>
          <div style={{ display: "flex", gap: 10 }}>
            <div style={{ height: 56, padding: "0 14px", display: "flex", alignItems: "center", gap: 8, background: "var(--surface)", border: "1.5px solid var(--line)", borderRadius: 14, fontWeight: 600 }}>
              🇱🇰 +94
            </div>
            <div style={{ flex: 1, height: 56, padding: "0 16px", display: "flex", alignItems: "center", background: "var(--surface)", border: "1.5px solid var(--accent)", borderRadius: 14, fontWeight: 600, fontSize: 17, letterSpacing: 1 }}>
              77 555 2230
              <span style={{ marginLeft: "auto", width: 2, height: 22, background: "var(--accent)", animation: "blink 1s infinite" }}/>
            </div>
          </div>
        </div>

        <div style={{ marginTop: 22, padding: 16, background: "var(--teal-soft)", borderRadius: 16, display: "flex", gap: 12 }}>
          <Icon name="shield" size={20} color="var(--teal)"/>
          <div style={{ fontSize: 12, color: "var(--teal)", lineHeight: 1.45 }}>
            <b>New driver?</b> You'll need a valid Sri Lankan driving licence, vehicle registration, and insurance. Verification takes 24–48 hours.
          </div>
        </div>

        <div style={{ flex: 1 }}/>
        <button className="rs-btn accent full">Continue</button>
        <div style={{ textAlign: "center", marginTop: 12, fontSize: 13, color: "var(--ink-3)" }}>
          Passenger instead? <span style={{ color: "var(--ink)", fontWeight: 700 }}>Get the rider app</span>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// KYC step 1 — personal details + ID upload
// ═══════════════════════════════════════════════════════════════════
function DrKyc1Screen() {
  return (
    <Phone label="D04 KYC · Identity">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "12px 20px 14px", background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 14 }}>
            <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="back" size={20}/>
            </button>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 11, fontWeight: 700, color: "var(--ink-3)", letterSpacing: ".12em" }}>STEP 1 OF 3</div>
              <div style={{ fontSize: 17, fontWeight: 700 }}>Identity & address</div>
            </div>
          </div>
          <Stepper step={1} total={3} labels={["Identity", "Licence", "Vehicle"]}/>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "18px 20px" }} className="rs-scroll">
          <div className="rs-section-label" style={{ marginBottom: 10 }}>PERSONAL DETAILS</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            <Field label="Full name (as on NIC)" value="Saman Wijesinghe"/>
            <div style={{ display: "flex", gap: 10 }}>
              <div style={{ flex: 1 }}><Field label="Date of birth" value="14 Aug 1988"/></div>
              <div style={{ flex: 1 }}><Field label="Gender" value="Male"/></div>
            </div>
            <Field label="Home address" value="42 Pereira Mw, Rajagiriya" icon="home"/>
          </div>

          <div className="rs-section-label" style={{ margin: "20px 0 10px" }}>NIC / PASSPORT</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            <DocUpload icon="card" label="NIC · Front" hint="JPG or PDF · clear, all corners visible" status="verified"/>
            <DocUpload icon="card" label="NIC · Back" hint="JPG or PDF" status="uploaded"/>
            <DocUpload icon="user" label="Selfie holding your NIC" hint="Used to confirm it's you" status="empty"/>
          </div>

          <div style={{ marginTop: 16, padding: 12, background: "var(--bg-soft)", borderRadius: 12, fontSize: 11, color: "var(--ink-3)", lineHeight: 1.4 }}>
            We use Sri Lanka KYC standards. Documents are encrypted and only reviewed by our verification team.
          </div>
        </div>

        <div style={{ padding: "12px 20px 14px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">Continue to driving licence</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// KYC step 2 — driving licence
// ═══════════════════════════════════════════════════════════════════
function DrKyc2Screen() {
  return (
    <Phone label="D05 KYC · Licence">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "12px 20px 14px", background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 14 }}>
            <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="back" size={20}/>
            </button>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 11, fontWeight: 700, color: "var(--ink-3)", letterSpacing: ".12em" }}>STEP 2 OF 3</div>
              <div style={{ fontSize: 17, fontWeight: 700 }}>Driving licence</div>
            </div>
          </div>
          <Stepper step={2} total={3} labels={["Identity", "Licence", "Vehicle"]}/>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "18px 20px" }} className="rs-scroll">
          {/* Licence preview */}
          <div style={{ aspectRatio: "1.6", borderRadius: 18, background: "linear-gradient(135deg, var(--ink) 0%, #3a2f29 100%)", color: "#fff", padding: 18, position: "relative", overflow: "hidden" }}>
            <div style={{ position: "absolute", right: -40, top: -40, width: 140, height: 140, borderRadius: 70, background: "var(--accent)", opacity: .35 }}/>
            <div style={{ position: "relative" }}>
              <div style={{ fontSize: 10, opacity: .65, fontWeight: 700, letterSpacing: ".12em" }}>SRI LANKA · DRIVER LICENCE</div>
              <div className="rs-display" style={{ fontSize: 22, marginTop: 8 }}>Saman Wijesinghe</div>
              <div style={{ fontSize: 11, opacity: .8, marginTop: 4 }}>B1 · LIGHT MOTOR VEHICLE</div>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", marginTop: 20 }}>
                <div>
                  <div style={{ fontSize: 9, opacity: .55, fontWeight: 700, letterSpacing: ".1em" }}>LICENCE No.</div>
                  <div style={{ fontFamily: "var(--font-mono)", fontSize: 14, marginTop: 2 }}>B1 88824712</div>
                </div>
                <div>
                  <div style={{ fontSize: 9, opacity: .55, fontWeight: 700, letterSpacing: ".1em" }}>EXPIRES</div>
                  <div style={{ fontSize: 13, fontWeight: 700, marginTop: 2 }}>14 / 08 / 2029</div>
                </div>
              </div>
            </div>
          </div>

          <div className="rs-section-label" style={{ margin: "18px 0 10px" }}>UPLOADED DOCUMENTS</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            <DocUpload icon="card" label="Licence · Front" hint="Make sure all text is readable" status="verified"/>
            <DocUpload icon="card" label="Licence · Back" hint="Both sides required" status="verified"/>
          </div>

          <div className="rs-section-label" style={{ margin: "18px 0 10px" }}>LICENCE DETAILS</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            <Field label="Licence number" value="B1 88824712"/>
            <div style={{ display: "flex", gap: 10 }}>
              <div style={{ flex: 1 }}><Field label="Issued" value="14 Aug 2019"/></div>
              <div style={{ flex: 1 }}><Field label="Expires" value="14 Aug 2029"/></div>
            </div>
            <Field label="Class" value="B1 — Light Motor Vehicle"/>
          </div>
        </div>

        <div style={{ padding: "12px 20px 14px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">Continue to vehicle</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// KYC step 3 — vehicle docs
// ═══════════════════════════════════════════════════════════════════
function DrKyc3Screen() {
  return (
    <Phone label="D06 KYC · Vehicle">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "12px 20px 14px", background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 14 }}>
            <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="back" size={20}/>
            </button>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 11, fontWeight: 700, color: "var(--ink-3)", letterSpacing: ".12em" }}>STEP 3 OF 3</div>
              <div style={{ fontSize: 17, fontWeight: 700 }}>Vehicle documents</div>
            </div>
          </div>
          <Stepper step={3} total={3} labels={["Identity", "Licence", "Vehicle"]}/>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "18px 20px" }} className="rs-scroll">
          <div className="rs-section-label" style={{ marginBottom: 10 }}>YOUR VEHICLE</div>
          <div style={{ padding: 14, background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 16 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <div style={{ width: 60, height: 60, borderRadius: 14, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="car" size={30}/>
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 14, fontWeight: 700 }}>Toyota Aqua 2018</div>
                <div style={{ fontSize: 12, color: "var(--ink-3)" }}>CAR-2211 · Silver · 4 seats</div>
              </div>
              <button style={{ fontSize: 12, color: "var(--accent-2)", fontWeight: 700 }}>Edit</button>
            </div>
          </div>

          <div className="rs-section-label" style={{ margin: "18px 0 10px" }}>DOCUMENTS</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            <DocUpload icon="receipt" label="Vehicle registration (CR)" hint="Issued by Dept. of Motor Traffic" status="verified"/>
            <DocUpload icon="shield" label="Revenue licence" hint="Current and valid" status="verified"/>
            <DocUpload icon="shield" label="Insurance certificate" hint="Third-party minimum · must cover passenger liability" status="uploaded"/>
            <DocUpload icon="car" label="Vehicle photo (front + sides)" hint="3 photos for the passenger app" status="empty"/>
          </div>

          <div style={{ marginTop: 18, padding: 12, background: "var(--accent-soft)", borderRadius: 12, display: "flex", gap: 10 }}>
            <Icon name="alert" size={16} color="var(--accent-2)"/>
            <div style={{ fontSize: 12, color: "var(--accent-2)", lineHeight: 1.4 }}>
              Your insurance must include <b>shared-ride passenger liability</b>. Most Sri Lankan policies need an endorsement — talk to your insurer.
            </div>
          </div>
        </div>

        <div style={{ padding: "12px 20px 14px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">Submit for review</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// KYC pending review
// ═══════════════════════════════════════════════════════════════════
function DrKycPendingScreen() {
  return (
    <Phone label="D07 KYC · Pending">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)", padding: "12px 24px 24px" }}>
        <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
          <Icon name="close" size={20}/>
        </button>

        <div style={{ marginTop: 28, textAlign: "center" }}>
          <div style={{ width: 96, height: 96, borderRadius: 48, background: "var(--teal-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center", position: "relative" }}>
            <Icon name="shield" size={42} color="var(--teal)" strokeWidth={1.8}/>
            <div style={{ position: "absolute", inset: -6, borderRadius: 52, border: "2px dashed var(--teal)", opacity: .4, animation: "spin 12s linear infinite" }}/>
          </div>
          <div className="rs-display" style={{ fontSize: 28, marginTop: 18, lineHeight: 1.1 }}>You're nearly there,<br/>Saman.</div>
          <div style={{ fontSize: 14, color: "var(--ink-3)", marginTop: 8, lineHeight: 1.5 }}>
            Our team is reviewing your documents.<br/>You'll get a notification within <b style={{ color: "var(--ink)" }}>24–48 hours</b>.
          </div>
        </div>

        <div style={{ marginTop: 28, padding: 18, background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 18 }}>
          <div className="rs-section-label" style={{ marginBottom: 12 }}>REVIEW STATUS</div>
          <ReviewItem label="Identity & address" status="done"/>
          <ReviewItem label="Driving licence" status="done"/>
          <ReviewItem label="Vehicle registration" status="done"/>
          <ReviewItem label="Insurance certificate" status="in-progress"/>
          <ReviewItem label="Background check" status="queued" last/>
        </div>

        <div style={{ flex: 1 }}/>
        <button className="rs-btn ghost full">Contact support</button>
        <div style={{ textAlign: "center", marginTop: 14, fontSize: 12, color: "var(--ink-4)" }}>
          You can still browse the app, but won't be able to publish trips until you're verified.
        </div>
      </div>
    </Phone>
  );
}

function ReviewItem({ label, status, last }) {
  const meta = {
    "done": { icon: "check", color: "var(--match-full)", bg: "var(--success-soft)", text: "Verified" },
    "in-progress": { icon: "clock", color: "var(--accent-2)", bg: "var(--accent-soft)", text: "Reviewing now" },
    "queued": { icon: "clock", color: "var(--ink-4)", bg: "var(--bg-soft)", text: "In queue" },
  };
  const s = meta[status];
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 12, padding: "10px 0", borderBottom: last ? "none" : "1px solid var(--line)" }}>
      <div style={{ width: 28, height: 28, borderRadius: 14, background: s.bg, display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
        <Icon name={s.icon} size={14} color={s.color} strokeWidth={status === "done" ? 3 : 2}/>
      </div>
      <div style={{ flex: 1, fontSize: 13, fontWeight: 600 }}>{label}</div>
      <div style={{ fontSize: 11, color: s.color, fontWeight: 700 }}>{s.text}</div>
    </div>
  );
}

Object.assign(window, { DrSplashScreen, DrOnboardingScreen, DrLoginScreen, DrKyc1Screen, DrKyc2Screen, DrKyc3Screen, DrKycPendingScreen });
