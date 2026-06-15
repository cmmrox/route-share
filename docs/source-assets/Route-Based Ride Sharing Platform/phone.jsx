// phone.jsx — a simple, tight Android-ish phone frame used across screens
// Custom (not the starter) so we can control status bar color per-screen and
// avoid the forced M3 app-bar.

const PHONE_W = 380;
const PHONE_H = 820;

function Phone({ children, statusDark = false, statusBg = "transparent", navBg = "transparent", navDark = false, width = PHONE_W, height = PHONE_H, label }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-start", gap: 10 }}>
      <div className="rs" data-screen-label={label} style={{
        width, height,
        borderRadius: 40,
        overflow: "hidden",
        background: "var(--bg)",
        border: "8px solid #1b1410",
        boxShadow: "0 28px 64px rgba(30,20,10,.18), 0 4px 12px rgba(30,20,10,.12)",
        position: "relative",
      }}>
        {/* Status bar */}
        <div style={{
          height: 36, display: "flex", alignItems: "center", justifyContent: "space-between",
          padding: "0 22px", position: "relative",
          background: statusBg,
          color: statusDark ? "#fff" : "var(--ink)",
          fontSize: 13, fontWeight: 600, letterSpacing: 0.2,
          zIndex: 10,
        }}>
          <span>9:41</span>
          <div style={{ position: "absolute", left: "50%", top: 8, transform: "translateX(-50%)", width: 18, height: 18, background: "#0a0a0a", borderRadius: "50%" }}/>
          <span style={{ display: "inline-flex", gap: 5, alignItems: "center" }}>
            <svg width="14" height="10" viewBox="0 0 14 10" fill="currentColor"><path d="M1 8h2v2H1zM5 5h2v5H5zM9 2h2v8H9z"/></svg>
            <svg width="14" height="10" viewBox="0 0 14 10" fill="currentColor"><path d="M7 2a8 8 0 0 0-6 2.5l1 1a6 6 0 0 1 10 0l1-1A8 8 0 0 0 7 2zm0 3a4 4 0 0 0-3 1.2l1 1a2.5 2.5 0 0 1 4 0l1-1A4 4 0 0 0 7 5zm0 3a1.3 1.3 0 1 0 0 2.6A1.3 1.3 0 0 0 7 8z"/></svg>
            <svg width="18" height="10" viewBox="0 0 18 10" fill="none" stroke="currentColor" strokeWidth="1"><rect x="1" y="2" width="14" height="6" rx="1.5"/><rect x="2.5" y="3.5" width="9" height="3" fill="currentColor"/><rect x="16" y="4" width="1.5" height="2"/></svg>
          </span>
        </div>

        {/* Content */}
        <div style={{ position: "absolute", left: 0, right: 0, top: 36, bottom: 18, overflow: "hidden" }}>
          {children}
        </div>

        {/* Nav pill */}
        <div style={{
          position: "absolute", bottom: 0, left: 0, right: 0, height: 18,
          display: "flex", alignItems: "center", justifyContent: "center",
          background: navBg,
          zIndex: 10,
        }}>
          <div style={{ width: 108, height: 4, borderRadius: 2, background: navDark ? "#fff" : "var(--ink)", opacity: 0.4 }}/>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { Phone, PHONE_W, PHONE_H });
