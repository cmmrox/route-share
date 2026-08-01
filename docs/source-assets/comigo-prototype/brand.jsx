// brand.jsx — ComiGo brand mark package: 3 concepts, chosen mark, ship assets, type spec

// ── palette resolver for a mark rendered on light / dark / single-colour ──
function markTones(tone) {
  if (tone === "mono") return { ink: "currentColor", accent: "currentColor", teal: "currentColor", faint: "currentColor" };
  if (tone === "onDark") return { ink: "#f4ece0", accent: "#e8834f", teal: "#48a89f", faint: "#a89d8f" };
  return { ink: "var(--ink)", accent: "var(--accent)", teal: "var(--teal)", faint: "var(--ink-3)" };
}

// ═══════════════ CONCEPT A — "Overlap" (two routes sharing a stretch) ═══════════════
function MarkOverlap({ size = 48, tone = "color", weight = 1 }) {
  const c = markTones(tone);
  const w = (n) => n * weight;
  return (
    <svg width={size} height={size} viewBox="0 0 48 48" fill="none" style={{ display: "block", overflow: "visible" }}>
      <path d="M7 36C16 34 18 26 26 22" stroke={c.ink} strokeWidth={w(4.4)} strokeLinecap="round"/>
      <path d="M30 20C34 18 38 16 41 12" stroke={c.ink} strokeWidth={w(4.4)} strokeLinecap="round"/>
      <path d="M13 11C15 18 16 23 18.5 26.5" stroke={c.teal} strokeWidth={w(3.4)} strokeLinecap="round"/>
      <path d="M29.5 20.5C32 26 34 31 36 37" stroke={c.teal} strokeWidth={w(3.4)} strokeLinecap="round"/>
      <path d="M18.5 26.5C22 24.5 24.5 23 29.5 20.5" stroke={c.accent} strokeWidth={w(6.6)} strokeLinecap="round"/>
    </svg>
  );
}

// ═══════════════ CONCEPT B — "Come & Go" (two journeys in exchange) ═══════════════
function MarkComeGo({ size = 48, tone = "color", weight = 1 }) {
  const c = markTones(tone);
  const w = (n) => n * weight;
  return (
    <svg width={size} height={size} viewBox="0 0 48 48" fill="none" style={{ display: "block" }}>
      <path d="M12.7 30.5A13 13 0 1 1 35.3 30.5" stroke={c.accent} strokeWidth={w(5)} strokeLinecap="round"/>
      <path d="M35.3 17.5A13 13 0 1 1 12.7 17.5" stroke={c.teal} strokeWidth={w(5)} strokeLinecap="round"/>
      <circle cx="35.3" cy="30.5" r={w(3.4)} fill={c.accent}/>
      <circle cx="12.7" cy="17.5" r={w(3.4)} fill={c.teal}/>
    </svg>
  );
}

// ═══════════════ CONCEPT C — "Seats on a line" ═══════════════
function MarkSeats({ size = 48, tone = "color", weight = 1 }) {
  const c = markTones(tone);
  const w = (n) => n * weight;
  return (
    <svg width={size} height={size} viewBox="0 0 48 48" fill="none" style={{ display: "block" }}>
      <path d="M8 38C17 37 19 25 28 20C34 16.5 38 13 41 10" stroke={c.ink} strokeWidth={w(4)} strokeLinecap="round"/>
      <circle cx="12.5" cy="36.5" r={w(4)} fill="var(--surface)" stroke={c.ink} strokeWidth={w(2.6)}/>
      <circle cx="21" cy="27" r={w(5.2)} fill={c.accent}/>
      <circle cx="31" cy="17.5" r={w(4)} fill="var(--surface)" stroke={c.ink} strokeWidth={w(2.6)}/>
    </svg>
  );
}

// ═══════════════ WORDMARK ═══════════════
function Wordmark({ size = 34, tone = "color", weight = 600 }) {
  const c = markTones(tone);
  return (
    <span style={{
      fontFamily: '"Fraunces", Georgia, serif', fontWeight: weight, fontSize: size,
      letterSpacing: "-0.025em", lineHeight: 1, color: c.ink,
      fontVariationSettings: `"opsz" ${Math.min(144, Math.max(9, size * 1.4))}`,
      whiteSpace: "nowrap",
    }}>Comi<span style={{ color: tone === "mono" ? "currentColor" : c.accent }}>G</span>o</span>
  );
}

// horizontal + stacked lockups, mark-agnostic
function Lockup({ Mark, size = 40, tone = "color", stacked = false, tagline }) {
  const c = markTones(tone);
  if (stacked) return (
    <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: size * 0.3 }}>
      <Mark size={size * 1.35} tone={tone}/>
      <Wordmark size={size * 0.78} tone={tone}/>
      {tagline && <div style={{ fontSize: size * 0.24, fontWeight: 700, letterSpacing: ".16em", color: c.faint, textTransform: "uppercase" }}>{tagline}</div>}
    </div>
  );
  return (
    <div style={{ display: "flex", alignItems: "center", gap: size * 0.28 }}>
      <Mark size={size} tone={tone}/>
      <div style={{ display: "flex", flexDirection: "column", gap: 2 }}>
        <Wordmark size={size * 0.82} tone={tone}/>
        {tagline && <div style={{ fontSize: size * 0.22, fontWeight: 700, letterSpacing: ".16em", color: c.faint, textTransform: "uppercase" }}>{tagline}</div>}
      </div>
    </div>
  );
}

// ── layout helpers for the brand boards (not app UI) ──
function BBoard({ children, pad = 32, dark = false, style }) {
  return (
    <div className={dark ? "dark" : undefined} style={{ boxSizing: "border-box", height: "100%", background: "var(--bg)", padding: pad, fontFamily: "var(--font)", color: "var(--ink)", ...style }}>{children}</div>
  );
}
function BTitle({ children, sub }) {
  return (
    <div style={{ marginBottom: 24 }}>
      <div className="rs-display" style={{ fontSize: 26, letterSpacing: "-0.02em" }}>{children}</div>
      {sub && <div style={{ fontSize: 13.5, color: "var(--ink-3)", marginTop: 6, maxWidth: 760, lineHeight: 1.5 }}>{sub}</div>}
    </div>
  );
}
function BLabel({ children, nowrap }) {
  return <div style={{ fontSize: 10, fontWeight: 700, letterSpacing: ".14em", color: "var(--ink-3)", textTransform: "uppercase", whiteSpace: nowrap ? "nowrap" : undefined, flexShrink: nowrap ? 0 : undefined }}>{children}</div>;
}
function BCard({ children, style }) {
  return <div style={{ background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 20, padding: 24, display: "flex", flexDirection: "column", gap: 18, ...style }}>{children}</div>;
}

// ═══════════════ B01 · Three concepts ═══════════════
const CONCEPTS = [
  {
    key: "A", Mark: MarkOverlap, name: "Overlap",
    idea: "One long stroke is the driver's journey. A second, thinner stroke joins it, travels the shared stretch, and leaves. The overlap segment is the only accent-coloured element.",
    why: "Draws the actual mechanic — partial route overlap — rather than a category cliché. The thick middle bar is the product's whole pricing model in one shape.",
    risk: "Needs the accent segment to carry meaning; in single-colour the overlap reads as a thicker stroke only.",
  },
  {
    key: "B", Mark: MarkComeGo, name: "Come & Go",
    idea: "Two 240° arcs chasing each other, each ending in a node. Reciprocity: the same person comes and goes, rides and drives.",
    why: "Speaks directly to the name and to the one-app-two-modes decision. Reads as a badge, which is strong at 24px and as a monochrome notification glyph.",
    risk: "Circular exchange marks are a crowded space (recycling, sync, refresh). Says 'two-way' but not 'route'.",
  },
  {
    key: "C", Mark: MarkSeats, name: "Seats on a line",
    idea: "A single route stroke with three beads on it — one filled, two open. Seats being taken along a path.",
    why: "The most literal expression of seat inventory, and the beads give a natural loading/occupancy animation.",
    risk: "Three elements plus a stroke is too much at 24px; the beads collapse into the line. Weakest of the three at small sizes.",
  },
];

function BrandConceptsBoard() {
  return (
    <BBoard>
      <BTitle sub="Three directions for the ComiGo mark. Each shown as a horizontal lockup, a stacked lockup, and the icon alone at 44px and 24px. Recommendation follows at the bottom.">Mark concepts</BTitle>
      <div style={{ display: "flex", gap: 20 }}>
        {CONCEPTS.map(({ key, Mark, name, idea, why, risk }) => (
          <BCard key={key} style={{ flex: 1, gap: 16 }}>
            <div style={{ display: "flex", alignItems: "baseline", justifyContent: "space-between" }}>
              <BLabel nowrap>Concept {key}</BLabel>
              <div style={{ fontSize: 15, fontWeight: 700 }}>{name}</div>
            </div>
            <div style={{ background: "var(--bg-soft)", borderRadius: 16, padding: "26px 20px", display: "flex", flexDirection: "column", alignItems: "center", gap: 26 }}>
              <Lockup Mark={Mark} size={38}/>
              <div style={{ height: 1, alignSelf: "stretch", background: "var(--line)" }}/>
              <Lockup Mark={Mark} size={34} stacked/>
            </div>
            <div style={{ display: "flex", gap: 12 }}>
              {[
                { s: 44, l: "44 px" }, { s: 24, l: "24 px" },
              ].map(({ s, l }) => (
                <div key={l} style={{ flex: 1, background: "var(--bg-soft)", borderRadius: 14, padding: 14, display: "flex", flexDirection: "column", alignItems: "center", gap: 10 }}>
                  <div style={{ height: 46, display: "flex", alignItems: "center" }}><Mark size={s}/></div>
                  <BLabel>{l}</BLabel>
                </div>
              ))}
              <div style={{ flex: 1, background: "var(--ink-fill)", borderRadius: 14, padding: 14, display: "flex", flexDirection: "column", alignItems: "center", gap: 10 }}>
                <div style={{ height: 46, display: "flex", alignItems: "center", color: "#f4ece0" }}><Mark size={30} tone="mono"/></div>
                <div style={{ fontSize: 10, fontWeight: 700, letterSpacing: ".14em", color: "#a89d8f" }}>1-COLOUR</div>
              </div>
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: 10, fontSize: 12.5, lineHeight: 1.55 }}>
              <div><span style={{ fontWeight: 700 }}>Idea. </span><span style={{ color: "var(--ink-2)" }}>{idea}</span></div>
              <div><span style={{ fontWeight: 700, color: "var(--status-approved-ink)" }}>Strength. </span><span style={{ color: "var(--ink-2)" }}>{why}</span></div>
              <div><span style={{ fontWeight: 700, color: "var(--status-pending-ink)" }}>Risk. </span><span style={{ color: "var(--ink-2)" }}>{risk}</span></div>
            </div>
          </BCard>
        ))}
      </div>
      <div style={{ marginTop: 20, display: "flex", gap: 16, alignItems: "center", background: "var(--accent-soft)", border: "1px solid var(--line)", borderRadius: 20, padding: "20px 24px" }}>
        <MarkOverlap size={52}/>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 14, fontWeight: 800, letterSpacing: ".02em" }}>Recommendation · Concept A, "Overlap"</div>
          <div style={{ fontSize: 13, color: "var(--ink-2)", marginTop: 5, lineHeight: 1.55, maxWidth: 900 }}>
            It is the only one of the three that says <em>route overlap</em> — the concept every screen in the product is built to teach. It survives 24px because it is one continuous gesture, and the accent bar gives the brand a reusable motif: the same shape is the match ring's arc, the fare row's shared-distance bar, and the route timeline's highlighted segment. Concept B's two-way idea is carried forward in the mode switcher, not the logo.
          </div>
        </div>
      </div>
    </BBoard>
  );
}

// ═══════════════ B02 · Construction & clear space ═══════════════
function BrandConstructionBoard() {
  return (
    <BBoard>
      <BTitle sub="The refined mark. Built on a 48-unit grid with a 4.4-unit base stroke; the shared segment is 6.6 units — always 1.5× the journey stroke, which is what makes the overlap read.">Chosen mark · construction</BTitle>
      <div style={{ display: "flex", gap: 20 }}>
        <BCard style={{ width: 320, alignItems: "center", gap: 14 }}>
          <div style={{ position: "relative", width: 240, height: 240 }}>
            <svg width="240" height="240" viewBox="0 0 48 48" style={{ position: "absolute", inset: 0 }}>
              {[0, 6, 12, 18, 24, 30, 36, 42, 48].map(n => (
                <g key={n}>
                  <line x1={n} y1="0" x2={n} y2="48" stroke="var(--line)" strokeWidth=".25"/>
                  <line x1="0" y1={n} x2="48" y2={n} stroke="var(--line)" strokeWidth=".25"/>
                </g>
              ))}
              <circle cx="24" cy="24" r="21" stroke="var(--line-2)" strokeWidth=".3" fill="none" strokeDasharray="1 1"/>
            </svg>
            <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center" }}>
              <MarkOverlap size={240}/>
            </div>
          </div>
          <BLabel>48-unit grid · 6-unit gutters</BLabel>
        </BCard>
        <BCard style={{ flex: 1 }}>
          <BLabel>Clear space &amp; minimum sizes</BLabel>
          <div style={{ display: "flex", gap: 22, alignItems: "flex-end" }}>
            <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 10 }}>
              <div style={{ padding: 22, border: "1.5px dashed var(--line-2)", borderRadius: 12 }}><MarkOverlap size={88}/></div>
              <div style={{ fontSize: 11.5, color: "var(--ink-3)" }}>Clear space = ½ mark height on all sides</div>
            </div>
            <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 10 }}>
              <div style={{ padding: "16px 22px", border: "1.5px dashed var(--line-2)", borderRadius: 12 }}><Lockup Mark={MarkOverlap} size={44}/></div>
              <div style={{ fontSize: 11.5, color: "var(--ink-3)" }}>Lockup gap = 0.28 × mark height</div>
            </div>
          </div>
          <div style={{ height: 1, background: "var(--line)" }}/>
          <BLabel>Do not</BLabel>
          <div style={{ display: "flex", gap: 14, fontSize: 11.5, color: "var(--ink-2)", lineHeight: 1.5 }}>
            {[
              "Recolour the overlap segment to anything but accent — it is the meaning, not decoration.",
              "Rotate, mirror or reorder the strokes; the passenger tails always enter top-left and exit bottom-right.",
              "Add a container, shadow or outline to the mark inside the app. Containers exist only in the store icons.",
              "Set the wordmark in Plus Jakarta above 16px — Fraunces is the display face.",
            ].map((t, i) => (
              <div key={i} style={{ flex: 1, display: "flex", gap: 8 }}>
                <div style={{ width: 16, height: 16, borderRadius: 8, background: "var(--status-rejected-soft)", color: "var(--status-rejected-ink)", flexShrink: 0, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 11, fontWeight: 800, marginTop: 1 }}>×</div>
                <div>{t}</div>
              </div>
            ))}
          </div>
        </BCard>
      </div>
    </BBoard>
  );
}

// ═══════════════ B03 · Scale, theme, single-colour ═══════════════
function BrandScaleBoard() {
  const row = (tone, bg, label) => (
    <div className={tone === "onDark" ? "dark" : undefined} style={{ background: bg, borderRadius: 20, border: "1px solid var(--line)", padding: "22px 26px", display: "flex", alignItems: "center", gap: 34 }}>
      <div style={{ width: 116 }}><BLabel>{label}</BLabel></div>
      {[24, 32, 44, 64].map(s => (
        <div key={s} style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 8 }}>
          <div style={{ height: 66, display: "flex", alignItems: "center" }}><MarkOverlap size={s} tone={tone}/></div>
          <div style={{ fontSize: 10, fontWeight: 700, color: "var(--ink-3)" }}>{s}px</div>
        </div>
      ))}
      <div style={{ width: 1, alignSelf: "stretch", background: "var(--line)" }}/>
      <Lockup Mark={MarkOverlap} size={34} tone={tone}/>
      <div style={{ width: 1, alignSelf: "stretch", background: "var(--line)" }}/>
      <Lockup Mark={MarkOverlap} size={22} tone={tone}/>
    </div>
  );
  return (
    <BBoard>
      <BTitle sub="24px is the tab-bar and nav size, 44px the avatar scale, 512px the store icon. The mark is drawn once and scaled; stroke weights are proportional, so no optical size variants are needed above 24px.">Scale, theme &amp; single-colour</BTitle>
      <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
        {row("color", "var(--surface)", "Light theme")}
        {row("onDark", "#161310", "Dark theme")}
        <div style={{ background: "var(--ink-fill)", borderRadius: 20, padding: "22px 26px", display: "flex", alignItems: "center", gap: 34, color: "#f4ece0" }}>
          <div style={{ width: 116, fontSize: 10, fontWeight: 700, letterSpacing: ".14em", color: "#a89d8f" }}>SINGLE COLOUR</div>
          {[24, 32, 44, 64].map(s => (
            <div key={s} style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 8 }}>
              <div style={{ height: 66, display: "flex", alignItems: "center" }}><MarkOverlap size={s} tone="mono"/></div>
              <div style={{ fontSize: 10, fontWeight: 700, color: "#a89d8f" }}>{s}px</div>
            </div>
          ))}
          <div style={{ width: 1, alignSelf: "stretch", background: "#3a3128" }}/>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <MarkOverlap size={34} tone="mono"/>
            <span style={{ fontFamily: '"Fraunces", serif', fontWeight: 600, fontSize: 28, letterSpacing: "-0.025em" }}>ComiGo</span>
          </div>
          <div style={{ fontSize: 11.5, color: "#a89d8f", maxWidth: 190, lineHeight: 1.5 }}>Overlap segment holds its 1.5× weight so the shape still reads without colour.</div>
        </div>
      </div>
    </BBoard>
  );
}

// ═══════════════ B04 · Ship assets ═══════════════
function BrandAssetsBoard() {
  return (
    <BBoard>
      <BTitle sub="Everything engineering needs to build the app shell. The Android background stays #1b1410 to match the existing app config, so the foreground layer is drawn in cream + accent and kept inside the 66% safe circle.">Ship assets</BTitle>
      <div style={{ display: "flex", gap: 18 }}>
        <BCard style={{ alignItems: "center", gap: 12 }}>
          <BLabel>iOS app icon · 512</BLabel>
          <div style={{ width: 168, height: 168, borderRadius: 38, background: "#1b1410", display: "flex", alignItems: "center", justifyContent: "center", boxShadow: "var(--shadow-md)" }}>
            <MarkOverlap size={104} tone="onDark"/>
          </div>
          <div style={{ fontSize: 11.5, color: "var(--ink-3)", textAlign: "center", maxWidth: 168 }}>Mark at 61% of tile. Radius 22.4% (iOS squircle).</div>
        </BCard>
        <BCard style={{ alignItems: "center", gap: 12 }}>
          <BLabel>Android adaptive · foreground</BLabel>
          <div style={{ position: "relative", width: 168, height: 168, borderRadius: 20, background: "#1b1410", display: "flex", alignItems: "center", justifyContent: "center" }}>
            <svg width="168" height="168" viewBox="0 0 108 108" style={{ position: "absolute", inset: 0 }}>
              <circle cx="54" cy="54" r="36" stroke="#48a89f" strokeWidth=".7" fill="none" strokeDasharray="2 2"/>
              <rect x="18" y="18" width="72" height="72" stroke="#a89d8f" strokeWidth=".5" fill="none"/>
            </svg>
            <MarkOverlap size={78} tone="onDark"/>
          </div>
          <div style={{ fontSize: 11.5, color: "var(--ink-3)", textAlign: "center", maxWidth: 172 }}>108dp canvas · mark inside the 66dp safe circle · background layer solid <span className="tab">#1b1410</span>.</div>
        </BCard>
        <BCard style={{ flex: 1, gap: 14 }}>
          <BLabel>Splash</BLabel>
          <div style={{ height: 196, borderRadius: 16, background: "#1b1410", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 18 }}>
            <Lockup Mark={MarkOverlap} size={44} tone="onDark" stacked tagline="Colombo"/>
            <div style={{ width: 44, height: 3, borderRadius: 2, background: "#3a3128", overflow: "hidden" }}>
              <div style={{ width: "60%", height: "100%", background: "#e8834f", borderRadius: 2 }}/>
            </div>
          </div>
          <div style={{ fontSize: 11.5, color: "var(--ink-3)" }}>Stacked lockup, cream on ink. Determinate bar only if boot exceeds 600 ms.</div>
        </BCard>
        <BCard style={{ alignItems: "center", gap: 12 }}>
          <BLabel>Notification glyph</BLabel>
          <div style={{ display: "flex", gap: 12, alignItems: "center" }}>
            <div style={{ width: 62, height: 62, borderRadius: 16, background: "var(--bg-soft)", display: "flex", alignItems: "center", justifyContent: "center", color: "var(--ink)" }}>
              <MarkOverlap size={30} tone="mono"/>
            </div>
            <div style={{ width: 62, height: 62, borderRadius: 16, background: "#f4ece0", display: "flex", alignItems: "center", justifyContent: "center", color: "#1b1410" }}>
              <MarkOverlap size={22} tone="mono" weight={1.15}/>
            </div>
          </div>
          <div style={{ fontSize: 11.5, color: "var(--ink-3)", textAlign: "center", maxWidth: 168 }}>24dp monochrome, alpha-only. Stroke bumped 15% for the small-badge cut-out.</div>
        </BCard>
      </div>
    </BBoard>
  );
}

// ═══════════════ B05 · Wordmark typography ═══════════════
function BrandTypeBoard() {
  const specs = [
    ["Family", "Fraunces (variable)"],
    ["Weight", "600"],
    ["Optical size", "opsz 1.4 × font-size, clamped 9–144"],
    ["Tracking", "-0.025em"],
    ["Case", "ComiGo — capital C, capital G, one word"],
    ["Accent", "The G takes --accent; all other letters --ink"],
    ["Small-size alternate", "Plus Jakarta Sans 800, -0.02em, below 16px only"],
  ];
  return (
    <BBoard>
      <BTitle sub="Reproducible in code without a designer in the room. The accented G is the only colour rule and it is what ties the wordmark to the mark's accent overlap.">Wordmark typography</BTitle>
      <div style={{ display: "flex", gap: 20 }}>
        <BCard style={{ flex: 1, justifyContent: "center", gap: 26 }}>
          <Wordmark size={72}/>
          <div style={{ display: "flex", alignItems: "baseline", gap: 28, flexWrap: "wrap" }}>
            <Wordmark size={40}/><Wordmark size={28}/><Wordmark size={20}/>
            <span style={{ fontFamily: "var(--font)", fontWeight: 800, fontSize: 15, letterSpacing: "-0.02em" }}>ComiGo</span>
            <span style={{ fontFamily: "var(--font)", fontWeight: 800, fontSize: 13, letterSpacing: "-0.02em" }}>ComiGo</span>
          </div>
          <div style={{ fontSize: 11.5, color: "var(--ink-3)" }}>Fraunces 600 down to 20px, then the Plus Jakarta 800 alternate.</div>
        </BCard>
        <BCard style={{ width: 480, gap: 0, padding: 0, overflow: "hidden" }}>
          {specs.map(([k, v], i) => (
            <div key={k} style={{ display: "flex", gap: 16, padding: "14px 22px", borderTop: i ? "1px solid var(--line)" : "none", fontSize: 13 }}>
              <div style={{ width: 150, color: "var(--ink-3)", flexShrink: 0 }}>{k}</div>
              <div style={{ fontWeight: 600, color: "var(--ink)" }}>{v}</div>
            </div>
          ))}
          <div style={{ borderTop: "1px solid var(--line)", padding: "16px 22px", background: "var(--bg-soft)", fontFamily: "var(--font-mono)", fontSize: 11.5, lineHeight: 1.7, color: "var(--ink-2)" }}>
            fontFamily: "Fraunces_600SemiBold"<br/>
            fontSize: 34, letterSpacing: -0.85,<br/>
            fontVariationSettings: '"opsz" 48'
          </div>
        </BCard>
      </div>
    </BBoard>
  );
}

Object.assign(window, {
  MarkOverlap, MarkComeGo, MarkSeats, Wordmark, Lockup, markTones,
  BBoard, BTitle, BLabel, BCard,
  BrandConceptsBoard, BrandConstructionBoard, BrandScaleBoard, BrandAssetsBoard, BrandTypeBoard,
});
