// driver-earnings.jsx — earnings dashboard, payout setup, ratings

// ═══════════════════════════════════════════════════════════════════
// EARNINGS DASHBOARD
// ═══════════════════════════════════════════════════════════════════
function DrEarningsScreen({ showEarnings = true }) {
  const days = [
    { d: "M", v: 1240 }, { d: "T", v: 1380 }, { d: "W", v: 1620 }, { d: "T", v: 980 }, { d: "F", v: 2200 }, { d: "S", v: 1740 }, { d: "S", v: 0 },
  ];
  const max = 2400;
  const fmt = (n) => showEarnings ? n.toLocaleString() : "•••";
  return (
    <Phone label="D24 Earnings">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)", overflow: "auto" }} className="rs-scroll">
        <div style={{ padding: "16px 20px 24px", background: "var(--ink)", color: "#fff", borderBottomLeftRadius: 28, borderBottomRightRadius: 28, position: "relative", overflow: "hidden" }}>
          <div style={{ position: "absolute", right: -80, top: -80, width: 280, height: 280, borderRadius: 140, background: "var(--accent)", opacity: .35 }}/>
          <div style={{ position: "relative" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <button style={{ width: 40, height: 40, borderRadius: 20, background: "rgba(255,255,255,.18)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="back" size={20} color="#fff"/>
              </button>
              <button style={{ height: 36, padding: "0 14px", borderRadius: 18, background: "rgba(255,255,255,.18)", display: "inline-flex", alignItems: "center", gap: 6, fontSize: 12, fontWeight: 700 }}>
                This week <Icon name="chev" size={14} color="#fff"/>
              </button>
              <button style={{ width: 40, height: 40, borderRadius: 20, background: "rgba(255,255,255,.18)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="receipt" size={18} color="#fff"/>
              </button>
            </div>

            <div style={{ marginTop: 20, textAlign: "center" }}>
              <div style={{ fontSize: 11, opacity: .7, fontWeight: 700, letterSpacing: ".14em" }}>YOU EARNED</div>
              <div className="rs-display tab" style={{ fontSize: 64, fontWeight: 600, marginTop: 6, lineHeight: 1, letterSpacing: "-0.03em" }}>
                LKR {fmt(9160)}
              </div>
              <div style={{ marginTop: 8, fontSize: 13, opacity: .85, display: "inline-flex", alignItems: "center", gap: 6 }}>
                <Icon name="arrow" size={12} color="var(--match-full)"/>
                <span style={{ color: "var(--match-full)", fontWeight: 700 }}>+18%</span> vs last week
              </div>
            </div>

            {/* Bar chart */}
            <div style={{ marginTop: 20, display: "flex", justifyContent: "space-between", alignItems: "flex-end", height: 100, padding: "0 4px" }}>
              {days.map((b, i) => (
                <div key={i} style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", gap: 6 }}>
                  <div style={{
                    width: 24, height: Math.max(8, (b.v / max) * 80),
                    background: i === 4 ? "var(--accent)" : "rgba(255,255,255,.4)",
                    borderRadius: 6,
                    transition: "height .3s",
                  }}/>
                  <div style={{ fontSize: 11, fontWeight: 700, opacity: .7 }}>{b.d}</div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div style={{ padding: "16px 20px" }}>
          {/* Next payout */}
          <div className="rs-card" style={{ padding: 14, background: "var(--accent-soft)", border: "none" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <div>
                <div style={{ fontSize: 11, color: "var(--accent-2)", fontWeight: 700, letterSpacing: ".08em" }}>NEXT PAYOUT · MONDAY 29 APR</div>
                <div className="rs-display tab" style={{ fontSize: 28, color: "var(--accent-2)", marginTop: 4 }}>LKR {fmt(8420)}</div>
                <div style={{ fontSize: 11, color: "var(--accent-2)", opacity: .8 }}>BOC · Account ··· 2204</div>
              </div>
              <button style={{ width: 44, height: 44, borderRadius: 22, background: "#fff", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <Icon name="chev" size={20} color="var(--accent-2)"/>
              </button>
            </div>
          </div>

          {/* Stats grid */}
          <div className="rs-section-label" style={{ margin: "20px 0 10px" }}>THIS WEEK</div>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
            <StatCard label="Trips" value="14" sub="+2 vs last week" pos/>
            <StatCard label="Hours driven" value="11.4" sub="≈1.6 h/day" />
            <StatCard label="km driven" value="159" sub="LKR 57 per km"/>
            <StatCard label="Avg / trip" value={`LKR ${fmt(654)}`} sub="2.3 pax avg"/>
          </div>

          {/* Recent trips */}
          <div className="rs-section-label" style={{ margin: "20px 0 10px" }}>RECENT TRIPS</div>
          <div style={{ display: "flex", flexDirection: "column" }}>
            <EarningsRow date="Wed 24 Apr · 8:00 AM" route="Rajagiriya → Fort" pax={2} val={fmt(699)}/>
            <EarningsRow date="Tue 23 Apr · 5:30 PM" route="Fort → Rajagiriya" pax={3} val={fmt(1620)}/>
            <EarningsRow date="Tue 23 Apr · 8:00 AM" route="Rajagiriya → Fort" pax={2} val={fmt(1080)}/>
            <EarningsRow date="Mon 22 Apr · 5:30 PM" route="Fort → Rajagiriya" pax={1} val={fmt(540)} last/>
          </div>
        </div>
      </div>
    </Phone>
  );
}

function StatCard({ label, value, sub, pos }) {
  return (
    <div className="rs-card" style={{ padding: 12 }}>
      <div style={{ fontSize: 10, color: "var(--ink-3)", fontWeight: 700, letterSpacing: ".08em" }}>{label.toUpperCase()}</div>
      <div className="rs-display tab" style={{ fontSize: 22, marginTop: 4, fontWeight: 600 }}>{value}</div>
      <div style={{ fontSize: 11, color: pos ? "var(--match-full)" : "var(--ink-3)", marginTop: 2 }}>{sub}</div>
    </div>
  );
}

function EarningsRow({ date, route, pax, val, last }) {
  return (
    <div style={{ padding: "12px 0", display: "flex", alignItems: "center", gap: 14, borderBottom: last ? "none" : "1px solid var(--line)" }}>
      <div style={{ width: 36, height: 36, borderRadius: 18, background: "var(--bg-soft)", display: "flex", alignItems: "center", justifyContent: "center" }}>
        <Icon name="route" size={16}/>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 13, fontWeight: 700 }}>{route}</div>
        <div style={{ fontSize: 11, color: "var(--ink-3)" }}>{date} · {pax} pax</div>
      </div>
      <div className="tab" style={{ fontWeight: 700, fontSize: 14 }}>LKR {val}</div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════
// PAYOUT SETUP
// ═══════════════════════════════════════════════════════════════════
function DrPayoutSetupScreen() {
  return (
    <Phone label="D25 Payout Setup">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "14px 20px", display: "flex", alignItems: "center", gap: 12, background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="back" size={20}/>
          </button>
          <div style={{ flex: 1, fontSize: 17, fontWeight: 700 }}>Payout account</div>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "18px 20px" }} className="rs-scroll">
          {/* Existing account */}
          <div style={{ padding: 18, borderRadius: 18, background: "linear-gradient(135deg, var(--ink) 0%, #3a2f29 100%)", color: "#fff", position: "relative", overflow: "hidden" }}>
            <div style={{ position: "absolute", right: -40, top: -40, width: 140, height: 140, borderRadius: 70, background: "var(--accent)", opacity: .25 }}/>
            <div style={{ position: "relative" }}>
              <div style={{ fontSize: 10, opacity: .65, fontWeight: 700, letterSpacing: ".14em" }}>PRIMARY · DEFAULT</div>
              <div style={{ marginTop: 10, fontFamily: "var(--font-mono)", fontSize: 18, letterSpacing: 2, fontWeight: 600 }}>•••• •••• 2204</div>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", marginTop: 24 }}>
                <div>
                  <div style={{ fontSize: 9, opacity: .55, fontWeight: 700, letterSpacing: ".1em" }}>ACCOUNT HOLDER</div>
                  <div style={{ fontSize: 14, fontWeight: 700, marginTop: 2 }}>Saman Wijesinghe</div>
                </div>
                <div>
                  <div style={{ fontSize: 9, opacity: .55, fontWeight: 700, letterSpacing: ".1em" }}>BANK</div>
                  <div style={{ fontSize: 13, fontWeight: 700, marginTop: 2 }}>Bank of Ceylon</div>
                </div>
              </div>
            </div>
          </div>

          <div className="rs-section-label" style={{ margin: "20px 0 10px" }}>ACCOUNT DETAILS</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            <Field label="Bank" value="Bank of Ceylon (BOC)"/>
            <div style={{ display: "flex", gap: 10 }}>
              <div style={{ flex: 2 }}><Field label="Branch" value="Rajagiriya"/></div>
              <div style={{ flex: 1 }}><Field label="Code" value="047"/></div>
            </div>
            <Field label="Account number" value="80022047202204"/>
            <Field label="Account holder (as on bank)" value="Saman Wijesinghe"/>
          </div>

          <div className="rs-section-label" style={{ margin: "20px 0 10px" }}>PAYOUT SCHEDULE</div>
          <div style={{ padding: 14, background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 14, display: "flex", alignItems: "center", gap: 12 }}>
            <div style={{ width: 44, height: 44, borderRadius: 14, background: "var(--accent-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
              <Icon name="calendar" size={20} color="var(--accent-2)"/>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 700 }}>Every Monday</div>
              <div style={{ fontSize: 11, color: "var(--ink-3)" }}>Settled by 11:00 AM · LKR 100 min</div>
            </div>
            <button style={{ fontSize: 12, color: "var(--accent-2)", fontWeight: 700 }}>Change</button>
          </div>

          <div style={{ marginTop: 16, padding: 12, background: "var(--bg-soft)", borderRadius: 12, fontSize: 11, color: "var(--ink-3)", lineHeight: 1.45 }}>
            Bank details are verified by penny-test on first payout. We never share your account number with passengers.
          </div>
        </div>

        <div style={{ padding: "12px 20px 14px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">Save changes</button>
        </div>
      </div>
    </Phone>
  );
}

// ═══════════════════════════════════════════════════════════════════
// RATINGS & REVIEWS
// ═══════════════════════════════════════════════════════════════════
function DrRatingsScreen() {
  const ratings = [5, 5, 4, 5, 5, 5, 5, 4, 5, 5];
  const buckets = [5,4,3,2,1].map(s => ratings.filter(r => r === s).length);
  return (
    <Phone label="D26 Ratings">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "14px 20px", display: "flex", alignItems: "center", gap: 12, background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="back" size={20}/>
          </button>
          <div style={{ flex: 1, fontSize: 17, fontWeight: 700 }}>Ratings & reviews</div>
        </div>

        <div style={{ flex: 1, overflow: "auto" }} className="rs-scroll">
          <div style={{ padding: "20px 20px 16px", textAlign: "center", background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
            <div className="rs-display tab" style={{ fontSize: 64, fontWeight: 600, color: "var(--accent)", lineHeight: 1 }}>4.92</div>
            <div style={{ display: "flex", justifyContent: "center", gap: 2, marginTop: 8 }}>
              {[1,2,3,4,5].map(i => <Icon key={i} name="star" size={20} color="var(--warn)"/>)}
            </div>
            <div style={{ marginTop: 6, fontSize: 13, color: "var(--ink-3)" }}>From 312 completed trips</div>

            {/* Distribution */}
            <div style={{ marginTop: 18, display: "flex", flexDirection: "column", gap: 6, textAlign: "left", maxWidth: 280, margin: "18px auto 0" }}>
              {[5,4,3,2,1].map((s, i) => {
                const pct = s === 5 ? 86 : s === 4 ? 12 : s === 3 ? 2 : 0;
                return (
                  <div key={s} style={{ display: "flex", alignItems: "center", gap: 10, fontSize: 12 }}>
                    <div style={{ width: 14, fontWeight: 700 }}>{s}</div>
                    <Icon name="star" size={12} color="var(--warn)"/>
                    <div style={{ flex: 1, height: 6, background: "var(--bg-soft)", borderRadius: 3 }}>
                      <div style={{ width: `${pct}%`, height: "100%", background: "var(--accent)", borderRadius: 3 }}/>
                    </div>
                    <div className="tab" style={{ width: 30, fontSize: 11, color: "var(--ink-3)", fontWeight: 600, textAlign: "right" }}>{pct}%</div>
                  </div>
                );
              })}
            </div>
          </div>

          <div style={{ padding: "16px 20px 10px", display: "flex", gap: 8 }}>
            <div className="rs-chip accent" style={{ height: 30 }}>All</div>
            <div className="rs-chip" style={{ height: 30 }}>5★ · 92</div>
            <div className="rs-chip" style={{ height: 30 }}>With comments · 24</div>
          </div>

          <div style={{ padding: "0 20px 20px", display: "flex", flexDirection: "column", gap: 12 }}>
            <Review name="Nimali P" date="2 hours ago" rating={5} tags={["Safe driving", "On time"]} text="Saman was great — very smooth driver, knew the back routes around Thunmulla. Saved me 15 minutes."/>
            <Review name="Anonymous" date="Yesterday" rating={5} tags={["Friendly", "Clean car"]} text=""/>
            <Review name="Kasun A" date="2 days ago" rating={4} tags={["On time"]} text="Good trip, car could use a wash but otherwise solid."/>
            <Review name="Priya J" date="3 days ago" rating={5} tags={["Knew route", "Quiet"]} text="My usual driver — I book his recurring trip every weekday now. Reliable and stress-free."/>
          </div>
        </div>
      </div>
    </Phone>
  );
}

function Review({ name, date, rating, tags, text }) {
  return (
    <div className="rs-card" style={{ padding: 14 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
        <Avatar name={name} size={36}/>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 14, fontWeight: 700 }}>{name}</div>
          <div style={{ fontSize: 11, color: "var(--ink-3)" }}>{date}</div>
        </div>
        <div style={{ display: "flex", gap: 2 }}>
          {[...Array(rating)].map((_, i) => <Icon key={i} name="star" size={12} color="var(--warn)"/>)}
        </div>
      </div>
      {text && <div style={{ marginTop: 10, fontSize: 13, color: "var(--ink-2)", lineHeight: 1.5 }}>"{text}"</div>}
      {tags && tags.length > 0 && (
        <div style={{ marginTop: 10, display: "flex", gap: 6, flexWrap: "wrap" }}>
          {tags.map(t => <div key={t} className="rs-chip" style={{ height: 24, fontSize: 11 }}>{t}</div>)}
        </div>
      )}
    </div>
  );
}

Object.assign(window, { DrEarningsScreen, DrPayoutSetupScreen, DrRatingsScreen });
