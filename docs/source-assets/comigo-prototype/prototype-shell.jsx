// prototype-shell.jsx — chrome + flow wiring for the ComiGo clickable prototype

function useHistoryStack(initial) {
  const [st, setSt] = React.useState({ stack: [initial], i: 0 });
  const go = React.useCallback((id) => setSt(s => {
    if (s.stack[s.i] === id) return s;
    const stack = [...s.stack.slice(0, s.i + 1), id];
    return { stack, i: stack.length - 1 };
  }), []);
  const back = React.useCallback(() => setSt(s => ({ ...s, i: Math.max(0, s.i - 1) })), []);
  const fwd = React.useCallback(() => setSt(s => ({ ...s, i: Math.min(s.stack.length - 1, s.i + 1) })), []);
  const reset = React.useCallback((id) => setSt({ stack: [id], i: 0 }), []);
  return { current: st.stack[st.i], canBack: st.i > 0, canFwd: st.i < st.stack.length - 1, go, back, fwd, reset };
}

// applied on every screen, after the screen's own rules — the mode chip and
// the persistent safety affordance exist on many surfaces.
const GLOBAL_LINKS = [["driving", "D08"], ["riding", "P01"], ["in review", "S08"], ["action needed", "S09"]];

function PrototypeApp() {
  const [dark, setDark] = React.useState(false);
  const [hints, setHints] = React.useState(false);
  const [q, setQ] = React.useState("");
  const start = (typeof localStorage !== "undefined" && localStorage.getItem("comigo.proto.screen")) || "S01";
  const h = useHistoryStack(BY_ID[start] || BOARD_BY_ID[start] ? start : "S01");
  const stageRef = React.useRef(null);

  const id = h.current;
  const screen = BY_ID[id];
  const board = BOARD_BY_ID[id];

  React.useEffect(() => { document.documentElement.classList.toggle("dark", dark); }, [dark]);
  React.useEffect(() => { try { localStorage.setItem("comigo.proto.screen", id); } catch (e) {} }, [id]);

  // ── wire the rendered screen ──
  React.useEffect(() => {
    const root = stageRef.current;
    if (!root || !screen) return;
    const rules = [...(screen.links || []), ...GLOBAL_LINKS];
    const mode = screen.mode;
    const bound = [];
    const CANDIDATES = '[data-tab],[data-back],[data-row],.rs-btn,.rs-chip,.rs-tap,.rs-card,button';
    const els = root.querySelectorAll(CANDIDATES);
    const hasAccent = !!root.querySelector('.rs-btn.accent');
    const hasBtn = !!root.querySelector('.rs-btn');
    const used = new Set();
    els.forEach(el => {
      let target = null, kind = "link";
      if (el.dataset.tab) {
        const m = el.dataset.tabmode || mode || "ride";
        target = (TAB_TARGET[m] || TAB_TARGET.ride)[el.dataset.tab];
      } else if (el.dataset.back != null) {
        kind = "back";
      } else {
        const txt = (el.dataset.row || el.getAttribute("aria-label") || el.textContent || "").trim().toLowerCase();
        // Leaves only. A group card's text is the concatenation of all its rows, so
        // matching a container would bind the whole card to one arbitrary member —
        // clicking its padding then went somewhere different from the row itself.
        const isLeaf = !el.querySelector(CANDIDATES);
        if (isLeaf && txt && txt.length <= 180) {
          // Longest matching fragment wins, not the first declared. A row like
          // "Earnings & payouts" contains both "earnings" and "payout"; with
          // first-wins, declaration order silently decided the destination.
          const hit = rules.filter(([frag]) => txt.includes(frag))
            .sort((a, b) => b[0].length - a[0].length)[0];
          if (hit) { target = hit[1]; used.add(hit[0]); }
        }
        const isCard = el.classList.contains("rs-card");
        if (!target && screen.next && (
          (el.classList.contains("rs-btn") && (el.classList.contains("accent") || !hasAccent)) ||
          (el.tagName === "BUTTON" && !hasBtn) ||
          (isCard && !hasBtn && isLeaf)
        )) target = screen.next;
      }
      if (!target && kind !== "back") return;
      const fn = (ev) => { ev.stopPropagation(); ev.preventDefault(); kind === "back" ? h.back() : h.go(target); };
      el.addEventListener("click", fn);
      el.style.cursor = "pointer";
      el.classList.add("proto-hot");
      bound.push([el, fn]);
    });
    // Last resort: a screen with no bindable control at all (splash, full-bleed
    // states) still has to advance, so the phone surface itself becomes the target.
    if (!bound.length && screen.next) {
      const surface = root.querySelector(".rs") || root.firstElementChild;
      if (surface) {
        const fn = (ev) => { ev.stopPropagation(); h.go(screen.next); };
        surface.addEventListener("click", fn);
        surface.style.cursor = "pointer";
        surface.classList.add("proto-hot");
        bound.push([surface, fn]);
      }
    }
    const dead = (screen.links || []).filter(([frag]) => !used.has(frag)).map(([frag]) => frag);
    if (dead.length) console.warn(`[proto] ${screen.id}: link rules matched nothing →`, dead.join(", "));
    return () => bound.forEach(([el, fn]) => { el.removeEventListener("click", fn); el.classList.remove("proto-hot"); });
  }, [id, screen, h.go, h.back]);

  React.useEffect(() => {
    const k = (e) => {
      if (e.target.tagName === "INPUT") return;
      if (e.key === "ArrowLeft" && h.canBack) h.back();
      if (e.key === "ArrowRight" && screen?.next) h.go(screen.next);
    };
    window.addEventListener("keydown", k);
    return () => window.removeEventListener("keydown", k);
  }, [h, screen]);

  const ql = q.trim().toLowerCase();
  const match = (s) => !ql || s.id.toLowerCase().includes(ql) || s.n.toLowerCase().includes(ql);

  const Item = ({ s }) => (
    <button onClick={() => h.go(s.id)} className={"proto-item" + (s.id === id ? " on" : "")}>
      <span className="proto-id">{s.id}</span><span className="proto-nm">{s.n}</span>
    </button>
  );

  return (
    <div className="proto">
      <aside className="proto-side">
        <div className="proto-brand">
          <Lockup Mark={MarkOverlap} size={26}/>
          <span className="proto-count">{SCREENS.length} screens · {BOARDS.length} spec boards</span>
        </div>
        <input className="proto-search" placeholder="Filter screens…" value={q} onChange={e => setQ(e.target.value)}/>
        <div className="proto-list">
          {FLOW.map(sec => {
            const items = sec.s.filter(match);
            if (!items.length) return null;
            return (
              <div key={sec.g} className="proto-group">
                <div className="proto-ghead">{sec.g}</div>
                {items.map(s => <Item key={s.id} s={{ ...s, group: sec.g }}/>)}
              </div>
            );
          })}
          {BOARDS.filter(match).length > 0 && (
            <div className="proto-group">
              <div className="proto-ghead">Reference boards</div>
              {BOARDS.filter(match).map(b => <Item key={b.id} s={b}/>)}
            </div>
          )}
        </div>
        <div className="proto-foot">
          <label><input type="checkbox" checked={hints} onChange={e => setHints(e.target.checked)}/> <span>Show tap targets</span></label>
          <label><input type="checkbox" checked={dark} onChange={e => setDark(e.target.checked)}/> <span>Dark theme</span></label>
        </div>
      </aside>

      <main className="proto-main">
        <header className="proto-bar">
          <div className="proto-navbtns">
            <button onClick={h.back} disabled={!h.canBack} title="Back (←)">←</button>
            <button onClick={h.fwd} disabled={!h.canFwd} title="Forward">→</button>
            <button onClick={() => h.reset("S01")} title="Restart at splash">⟲</button>
          </div>
          <div className="proto-title">
            <b>{id}</b> <span>{(screen || board)?.n}</span>
          </div>
          <div className="proto-toggles">
            {screen?.mode && <em className={"proto-mode " + screen.mode}>{screen.mode === "drive" ? "Driver" : "Passenger"}</em>}
            {board && <em className="proto-mode spec">Spec board</em>}
            {screen?.next && <button className="proto-next" onClick={() => h.go(screen.next)}>Next step →</button>}
          </div>
        </header>

        <div className={"proto-stage" + (hints ? " hints" : "")} ref={stageRef}>
          {screen ? <div className="proto-phone">{screen.el}</div>
                  : <div className="proto-board" style={{ width: board.w }}>{board.el}</div>}
        </div>
      </main>
    </div>
  );
}

Object.assign(window, { PrototypeApp });
