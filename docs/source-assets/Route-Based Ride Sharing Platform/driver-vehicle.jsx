// driver-vehicle.jsx — add vehicle, vehicle list

function DrAddVehicleScreen() {
  return (
    <Phone label="D08 Add Vehicle">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "14px 20px", display: "flex", alignItems: "center", gap: 12, background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="back" size={20}/>
          </button>
          <div style={{ flex: 1, fontSize: 17, fontWeight: 700 }}>Add a vehicle</div>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "18px 20px" }} className="rs-scroll">
          <div style={{ aspectRatio: "1.4", borderRadius: 20, background: "var(--bg-soft)", border: "1px solid var(--line)", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 10 }}>
            <Icon name="car" size={60} color="var(--ink-3)"/>
            <div style={{ fontSize: 13, color: "var(--ink-3)", fontWeight: 600 }}>Tap to add 3 photos of your car</div>
            <div style={{ fontSize: 11, color: "var(--ink-4)" }}>Front · Driver side · Interior</div>
          </div>

          <div className="rs-section-label" style={{ margin: "20px 0 10px" }}>VEHICLE DETAILS</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            <div style={{ display: "flex", gap: 10 }}>
              <div style={{ flex: 1 }}><Field label="Make" value="Toyota"/></div>
              <div style={{ flex: 1 }}><Field label="Model" value="Aqua"/></div>
            </div>
            <div style={{ display: "flex", gap: 10 }}>
              <div style={{ flex: 1 }}><Field label="Year" value="2018"/></div>
              <div style={{ flex: 1 }}><Field label="Colour" value="Silver"/></div>
            </div>
            <Field label="Registration number" value="CAR-2211"/>
          </div>

          <div className="rs-section-label" style={{ margin: "20px 0 10px" }}>SEATS</div>
          <div style={{ padding: 18, background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 16 }}>
            <div style={{ fontSize: 13, color: "var(--ink-3)", marginBottom: 14, fontWeight: 600 }}>How many passenger seats can you offer?</div>
            <div style={{ display: "flex", gap: 8 }}>
              {[1,2,3,4].map(n => (
                <div key={n} style={{
                  flex: 1, height: 64, borderRadius: 14,
                  background: n === 3 ? "var(--accent)" : "var(--bg-soft)",
                  color: n === 3 ? "#fff" : "var(--ink)",
                  display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 2,
                  border: n === 3 ? "none" : "1.5px solid var(--line)",
                }}>
                  <div className="rs-display" style={{ fontSize: 22, fontWeight: 600 }}>{n}</div>
                  <div style={{ fontSize: 9, fontWeight: 700, letterSpacing: ".08em" }}>SEAT{n > 1 ? "S" : ""}</div>
                </div>
              ))}
            </div>
          </div>

          <div className="rs-section-label" style={{ margin: "20px 0 10px" }}>FEATURES</div>
          <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
            <div className="rs-chip accent" style={{ height: 34 }}><Icon name="check" size={12} strokeWidth={3}/> AC</div>
            <div className="rs-chip accent" style={{ height: 34 }}><Icon name="check" size={12} strokeWidth={3}/> Hybrid</div>
            <div className="rs-chip" style={{ height: 34 }}>Bluetooth</div>
            <div className="rs-chip" style={{ height: 34 }}>Bike rack</div>
            <div className="rs-chip" style={{ height: 34 }}>Pet friendly</div>
            <div className="rs-chip" style={{ height: 34 }}>Luggage space</div>
          </div>
        </div>

        <div style={{ padding: "12px 20px 14px", background: "var(--surface)", borderTop: "1px solid var(--line)" }}>
          <button className="rs-btn accent full">Save vehicle</button>
        </div>
      </div>
    </Phone>
  );
}

function DrVehicleListScreen() {
  const vehicles = [
    { name: "Toyota Aqua", year: 2018, plate: "CAR-2211", colour: "Silver", seats: 3, primary: true, status: "verified" },
    { name: "Suzuki Wagon R", year: 2021, plate: "WP CAH-4488", colour: "White", seats: 4, status: "pending" },
  ];
  return (
    <Phone label="D09 Vehicles">
      <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
        <div style={{ padding: "14px 20px", display: "flex", alignItems: "center", gap: 12, background: "var(--surface)", borderBottom: "1px solid var(--line)" }}>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--bg-soft)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="back" size={20}/>
          </button>
          <div style={{ flex: 1, fontSize: 17, fontWeight: 700 }}>Your vehicles</div>
          <button style={{ width: 40, height: 40, borderRadius: 20, background: "var(--accent)", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="plus" size={20} color="#fff" strokeWidth={2.4}/>
          </button>
        </div>

        <div style={{ flex: 1, overflow: "auto", padding: "16px 20px" }} className="rs-scroll">
          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            {vehicles.map(v => (
              <div key={v.plate} className="rs-card" style={{ padding: 16 }}>
                <div style={{ display: "flex", alignItems: "flex-start", gap: 14 }}>
                  <div style={{ width: 64, height: 64, borderRadius: 16, background: "var(--bg-soft)", display: "flex", alignItems: "center", justifyContent: "center", position: "relative" }}>
                    <Icon name="car" size={32}/>
                    {v.primary && <div style={{ position: "absolute", top: -4, right: -4, padding: "2px 6px", background: "var(--accent)", color: "#fff", fontSize: 9, fontWeight: 700, borderRadius: 999, border: "2px solid var(--surface)" }}>PRIMARY</div>}
                  </div>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontSize: 15, fontWeight: 700 }}>{v.name} <span style={{ color: "var(--ink-3)", fontWeight: 500 }}>· {v.year}</span></div>
                    <div style={{ fontSize: 12, color: "var(--ink-3)", marginTop: 2 }}>{v.plate} · {v.colour} · {v.seats} seats</div>
                    <div style={{ marginTop: 8, display: "flex", alignItems: "center", gap: 8 }}>
                      {v.status === "verified" ? (
                        <span className="rs-chip success" style={{ height: 24 }}><Icon name="check" size={10} strokeWidth={3}/> Verified</span>
                      ) : (
                        <span className="rs-chip accent" style={{ height: 24 }}>Pending review</span>
                      )}
                      <span className="rs-chip teal" style={{ height: 24 }}><Icon name="leaf" size={10}/> Hybrid</span>
                    </div>
                  </div>
                </div>
                <div className="rs-divider" style={{ margin: "14px 0" }}/>
                <div style={{ display: "flex", gap: 8 }}>
                  <button className="rs-btn soft" style={{ flex: 1, height: 40, fontSize: 12 }}>Documents</button>
                  <button className="rs-btn soft" style={{ flex: 1, height: 40, fontSize: 12 }}>Edit</button>
                  {!v.primary && <button className="rs-btn ghost" style={{ flex: 1, height: 40, fontSize: 12 }}>Make primary</button>}
                </div>
              </div>
            ))}
          </div>

          <button style={{ marginTop: 16, width: "100%", padding: 18, background: "transparent", border: "1.5px dashed var(--line-2)", borderRadius: 16, display: "flex", alignItems: "center", justifyContent: "center", gap: 10, fontWeight: 700, color: "var(--ink-2)" }}>
            <Icon name="plus" size={16}/> Add another vehicle
          </button>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, { DrAddVehicleScreen, DrVehicleListScreen });
