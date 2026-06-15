// screens-onboarding.jsx — onboarding, auth, profile, home, search, results

// ═══════════════════════════════════════════════════════════════════
// SPLASH / ONBOARDING
// ═══════════════════════════════════════════════════════════════════
function SplashScreen() {
  return (
    <Phone label="01 Splash">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 18, background: "var(--accent)", color: "#fff" }}>
        <div style={{ width: 84, height: 84, borderRadius: 26, background: "#fff", display: "flex", alignItems: "center", justifyContent: "center" }}>
          <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="6" cy="6" r="2.5" fill="var(--accent)"/>
            <circle cx="18" cy="18" r="2.5" fill="var(--accent)"/>
            <path d="M8 6h6a4 4 0 0 1 0 8h-4a4 4 0 0 0 0 8h6"/>
          </svg>
        </div>
        <div style={{ textAlign: "center" }}>
          <div className="rs-display" style={{ fontSize: 38, lineHeight: 1 }}>RouteShare</div>
          <div style={{ marginTop: 8, opacity: .85, fontSize: 14 }}>Share the ride. Share the cost.</div>
        </div>
        <div style={{ position: "absolute", bottom: 40, fontSize: 11, opacity: .7, letterSpacing: ".14em" }}>COLOMBO · SRI LANKA</div>
      </div>
    </Phone>
  );
}

function OnboardingScreen({ step = 0 }) {
  const slides = [
    { title: "Rides, already heading your way", body: "Drivers publish trips they're already making. You just hop on.", illo: "route" },
    { title: "Pay only for your stretch", body: "Fare is calculated on the actual kilometres you travel — not the whole route.", illo: "meter" },
    { title: "Track every turn", body: "Live GPS, seat count, driver rating and an SOS button. Always.", illo: "track" },
  ];
  const s = slides[step];
  return (
    <Phone label={`02 Onboarding ${step + 1}`}>
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "14px 22px", display: "flex", justifyContent: "flex-end" }}>
          <button style={{ fontSize: 14, color: "var(--ink-3)", fontWeight: 600 }}>Skip</button>
        </div>
        <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", padding: "12px 28px", gap: 24 }}>
          <div style={{ width: "100%", aspectRatio: "1.1", borderRadius: 24, background: "var(--accent-soft)", position: "relative", overflow: "hidden" }}>
            <OnboardIllo kind={s.illo}/>
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
          <button className="rs-btn accent full">{step === 2 ? "Get started" : "Next"}</button>
        </div>
      </div>
    </Phone>
  );
}

function OnboardIllo({ kind }) {
  if (kind === "route") {
    return (
      <svg viewBox="0 0 280 250" style={{ width: "100%", height: "100%" }}>
        <defs>
          <pattern id="grid-1" width="18" height="18" patternUnits="userSpaceOnUse">
            <path d="M18 0H0V18" stroke="rgba(214,106,59,.15)" fill="none"/>
          </pattern>
        </defs>
        <rect width="280" height="250" fill="url(#grid-1)"/>
        <path d="M30 50 C 70 80, 110 70, 140 120 C 170 170, 210 180, 250 200" stroke="var(--accent)" strokeWidth="5" fill="none" strokeLinecap="round"/>
        <circle cx="30" cy="50" r="9" fill="#fff" stroke="var(--teal)" strokeWidth="4"/>
        <circle cx="250" cy="200" r="9" fill="#fff" stroke="var(--accent)" strokeWidth="4"/>
        <g transform="translate(140 120)">
          <rect x="-22" y="-14" width="44" height="28" rx="6" fill="var(--ink)"/>
          <rect x="-16" y="-10" width="32" height="12" rx="2" fill="#fff" opacity=".3"/>
          <circle cx="-12" cy="14" r="4" fill="var(--ink)"/>
          <circle cx="12" cy="14" r="4" fill="var(--ink)"/>
        </g>
      </svg>
    );
  }
  if (kind === "meter") {
    return (
      <svg viewBox="0 0 280 250" style={{ width: "100%", height: "100%" }}>
        <path d="M40 180 L240 180" stroke="var(--ink-3)" strokeWidth="2" strokeDasharray="3 5"/>
        <circle cx="40" cy="180" r="9" fill="var(--teal)" stroke="#fff" strokeWidth="3"/>
        <circle cx="160" cy="180" r="9" fill="var(--accent)" stroke="#fff" strokeWidth="3"/>
        <circle cx="240" cy="180" r="7" fill="#fff" stroke="var(--ink-4)" strokeWidth="2"/>
        <path d="M40 180 L160 180" stroke="var(--accent)" strokeWidth="5" strokeLinecap="round"/>
        <g transform="translate(100 90)">
          <rect x="-60" y="-30" width="120" height="60" rx="14" fill="#fff" stroke="var(--line)" strokeWidth="1"/>
          <text x="0" y="-5" textAnchor="middle" fontFamily="Fraunces, serif" fontSize="22" fontWeight="600" fill="var(--ink)">LKR 600</text>
          <text x="0" y="14" textAnchor="middle" fontSize="10" fontWeight="600" fill="var(--ink-3)">12 KM · YOUR STRETCH</text>
        </g>
      </svg>
    );
  }
  if (kind === "track") {
    return (
      <svg viewBox="0 0 280 250" style={{ width: "100%", height: "100%" }}>
        <path d="M20 40 C 80 60, 80 140, 140 150 C 200 160, 220 200, 260 220" stroke="var(--ink)" strokeWidth="5" fill="none" strokeLinecap="round" strokeDasharray="8 6"/>
        <g transform="translate(140 150)">
          <circle r="30" fill="var(--accent)" opacity=".15"/>
          <circle r="18" fill="var(--accent)" opacity=".25"/>
          <circle r="10" fill="var(--accent)" stroke="#fff" strokeWidth="3"/>
        </g>
        <g transform="translate(210 60)">
          <rect x="-40" y="-18" width="80" height="36" rx="10" fill="#fff"/>
          <circle cx="-24" cy="0" r="8" fill="var(--teal)"/>
          <text x="-24" y="3" textAnchor="middle" fontSize="9" fontWeight="700" fill="#fff">SA</text>
          <text x="-10" y="-2" fontSize="10" fontWeight="700" fill="var(--ink)">Saman</text>
          <text x="-10" y="9" fontSize="8" fill="var(--ink-3)">4.9 ★ · 3 min</text>
        </g>
      </svg>
    );
  }
  return null;
}

// ═══════════════════════════════════════════════════════════════════
// LOGIN — phone + OTP
// ═══════════════════════════════════════════════════════════════════
function LoginScreen() {
  return (
    <Phone label="03 Login">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", padding: "8px 24px 24px", background: "var(--bg)" }}>
        <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
          <Icon name="back" size={20}/>
        </button>
        <div style={{ marginTop: 32 }}>
          <div className="rs-display" style={{ fontSize: 30, color: "var(--ink)" }}>Welcome back</div>
          <div style={{ color: "var(--ink-3)", marginTop: 6, fontSize: 14 }}>Enter your mobile number to continue</div>
        </div>
        <div style={{ marginTop: 32 }}>
          <div className="rs-section-label" style={{ marginBottom: 8 }}>MOBILE NUMBER</div>
          <div style={{ display: "flex", gap: 10 }}>
            <div style={{ height: 56, padding: "0 14px", display: "flex", alignItems: "center", gap: 8, background: "var(--surface)", border: "1.5px solid var(--line)", borderRadius: 14, fontWeight: 600 }}>
              🇱🇰 +94
            </div>
            <div style={{ flex: 1, height: 56, padding: "0 16px", display: "flex", alignItems: "center", background: "var(--surface)", border: "1.5px solid var(--accent)", borderRadius: 14, fontWeight: 600, fontSize: 17, letterSpacing: 1 }}>
              77 123 4567
              <span style={{ marginLeft: "auto", width: 2, height: 22, background: "var(--accent)", animation: "blink 1s infinite" }}/>
            </div>
          </div>
          <div style={{ color: "var(--ink-4)", fontSize: 12, marginTop: 10 }}>We'll text you a 6-digit verification code.</div>
        </div>

        <div style={{ marginTop: 24, display: "flex", alignItems: "center", gap: 12 }}>
          <div style={{ flex: 1, height: 1, background: "var(--line)" }}/>
          <div style={{ fontSize: 11, color: "var(--ink-4)", fontWeight: 600, letterSpacing: ".14em" }}>OR</div>
          <div style={{ flex: 1, height: 1, background: "var(--line)" }}/>
        </div>

        <div style={{ marginTop: 20, display: "flex", flexDirection: "column", gap: 10 }}>
          <button className="rs-btn ghost full" style={{ gap: 10 }}>
            <Icon name="google" size={18}/> Continue with Google
          </button>
          <button className="rs-btn ghost full" style={{ gap: 10 }}>
            <Icon name="mail" size={18}/> Continue with email
          </button>
        </div>

        <div style={{ flex: 1 }}/>
        <button className="rs-btn accent full">Send code</button>
        <div style={{ textAlign: "center", marginTop: 14, fontSize: 12, color: "var(--ink-4)" }}>
          By continuing you agree to our <span style={{ color: "var(--ink-2)", fontWeight: 600 }}>Terms</span> and <span style={{ color: "var(--ink-2)", fontWeight: 600 }}>Privacy Policy</span>.
        </div>
      </div>
    </Phone>
  );
}

function OtpScreen() {
  return (
    <Phone label="04 OTP">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", padding: "8px 24px 24px", background: "var(--bg)" }}>
        <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
          <Icon name="back" size={20}/>
        </button>
        <div style={{ marginTop: 32 }}>
          <div className="rs-display" style={{ fontSize: 30 }}>Enter the code</div>
          <div style={{ color: "var(--ink-3)", marginTop: 6, fontSize: 14 }}>Sent to <span style={{ color: "var(--ink)", fontWeight: 600 }}>+94 77 123 4567</span></div>
        </div>
        <div style={{ marginTop: 36, display: "flex", gap: 8, justifyContent: "space-between" }}>
          {["4","2","8","1","",""].map((v, i) => (
            <div key={i} style={{
              flex: 1, aspectRatio: "0.85", display: "flex", alignItems: "center", justifyContent: "center",
              background: "var(--surface)",
              border: `1.5px solid ${i === 4 ? "var(--accent)" : "var(--line)"}`,
              borderRadius: 14, fontSize: 26, fontWeight: 700, color: "var(--ink)", fontFamily: "var(--font-display)",
            }}>{v}{i === 4 && <span style={{ width: 2, height: 28, background: "var(--accent)" }}/>}</div>
          ))}
        </div>
        <div style={{ marginTop: 22, textAlign: "center", fontSize: 13, color: "var(--ink-3)" }}>
          Didn't receive it? <span style={{ color: "var(--accent-2)", fontWeight: 700 }}>Resend in 0:24</span>
        </div>
        <div style={{ flex: 1 }}/>
        <button className="rs-btn accent full">Verify</button>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// PROFILE SETUP
// ═══════════════════════════════════════════════════════════════════
function ProfileSetupScreen() {
  return (
    <Phone label="05 Profile Setup">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", padding: "8px 24px 24px", background: "var(--bg)", overflow: "auto" }} className="rs-scroll">
        <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
          <Icon name="back" size={20}/>
        </button>
        <div style={{ marginTop: 24 }}>
          <div className="rs-display" style={{ fontSize: 26 }}>Tell us about you</div>
          <div style={{ color: "var(--ink-3)", marginTop: 4, fontSize: 13 }}>Drivers will see your name and photo.</div>
        </div>
        <div style={{ display: "flex", justifyContent: "center", marginTop: 24 }}>
          <div style={{ position: "relative" }}>
            <Avatar name="Nimali P" size={96}/>
            <button style={{ position: "absolute", right: -4, bottom: -4, width: 32, height: 32, borderRadius: 16, background: "var(--ink)", color: "#fff", display: "inline-flex", alignItems: "center", justifyContent: "center", border: "3px solid var(--bg)" }}>
              <Icon name="plus" size={14} color="#fff"/>
            </button>
          </div>
        </div>

        <div style={{ marginTop: 28, display: "flex", flexDirection: "column", gap: 14 }}>
          <Field label="First name" value="Nimali"/>
          <Field label="Last name" value="Perera"/>
          <Field label="Email" value="nimali.p@gmail.com" icon="mail"/>
          <Field label="Referral code (optional)" value=""/>
        </div>

        <div style={{ marginTop: 18, padding: 14, background: "var(--teal-soft)", borderRadius: 14, display: "flex", gap: 10 }}>
          <Icon name="shield" size={18} color="var(--teal)"/>
          <div style={{ fontSize: 12, color: "var(--teal)", lineHeight: 1.45 }}>
            <b>Verified passenger</b> — we'll ask for a photo ID later to unlock all rides.
          </div>
        </div>

        <div style={{ flex: 1, minHeight: 20 }}/>
        <button className="rs-btn accent full" style={{ marginTop: 18 }}>Continue</button>
      </div>
    </Phone>
  );
}

function Field({ label, value, icon, placeholder }) {
  return (
    <div>
      <div className="rs-section-label" style={{ marginBottom: 6 }}>{label}</div>
      <div style={{ height: 52, padding: "0 14px", display: "flex", alignItems: "center", gap: 10, background: "var(--surface)", border: "1.5px solid var(--line)", borderRadius: 14, fontSize: 15, fontWeight: 500 }}>
        {icon && <Icon name={icon} size={16} color="var(--ink-3)"/>}
        <span style={{ color: value ? "var(--ink)" : "var(--ink-4)" }}>{value || placeholder}</span>
      </div>
    </div>
  );
}

Object.assign(window, { SplashScreen, OnboardingScreen, LoginScreen, OtpScreen, ProfileSetupScreen, Field });
