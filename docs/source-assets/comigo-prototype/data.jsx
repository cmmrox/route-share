// data.jsx — one fixture per CONTEXT, each internally consistent.
// Sharing a single ride object between "a search result" and "the trip I have
// booked" is how a 6:15 PM departure ends up inside an 8:00 AM search.

// ratePerKm here is the PLATFORM REFERENCE rate only — the midpoint an admin
// starts from. The rate a rider actually pays comes from the trip's vehicle
// (`ratePerKm` on the ride), because admin sets a min–max band per vehicle and
// the driver picks inside it. Never quote FARE_POLICY.ratePerKm on a trip.
const FARE_POLICY = {
  currency: "LKR",
  ratePerKm: 50,
  commissionPct: 10,
  discountLabel: "Route-match discount",
};

// ── POLICY ────────────────────────────────────────────────────────────────
// The rules that decide when money moves and what a broken promise costs. Every
// screen that states a rule reads it from here, so a number can only be wrong
// in one place.
const POLICY = {
  // A card is AUTHORISED at booking and CAPTURED when the driver starts the
  // trip. Nothing moves in between — not on booking, not on acceptance.
  chargeAt: "TRIP_START",
  // Exception: a trip already on the road. The service begins the instant the
  // driver accepts, so that booking captures immediately.
  chargeAtWhenEnRoute: "DRIVER_ACCEPT",
  // Early drop-off: the fare is recalculated on distance actually travelled,
  // twice a calendar month. Beyond that the passenger may still get off — the
  // fare simply stands.
  earlyDropAdjustedPerMonth: 2,
  // Cancelling a published trip: free outside this window, penalised inside it.
  driverCancelFreeHours: 12,
  lateCancelPenaltyPct: 20,
  // Passenger-side consequences. A cancellation after the trip has started costs
  // less than not turning up at all — telling us early is always cheaper.
  paxCancelAfterStartPct: 20,
  noShowPenaltyPct: 25,
  // A driver who is late to a pickup, or who never starts, costs the passenger
  // her morning. She may cancel free of charge after this many minutes, and he
  // carries the same penalty as a late cancellation.
  driverLateGraceMin: 10,
  driverLatePenaltyPct: 20,
  // EVERY penalty is now SPLIT: half to the person who was let down, half to the
  // platform. So a penalty can appear in a driver ledger as a negative line (he
  // caused it) or a positive one (he was the victim).
  penaltyRecipient: "SPLIT",
  penaltyVictimPct: 50,
  penaltyPlatformPct: 50,
  // Waiting at a pickup: the clock starts on arrival and can be extended once.
  pickupWaitMin: 5,
  pickupWaitExtendMin: 5,
  pickupWaitExtendLimit: 1,
  // Missing your own departure: start inside the buffer, or take the one
  // extension, or the trip auto-cancels. The extension protects the driver from
  // the auto-cancel; it does NOT oblige a passenger to keep waiting.
  startBufferMin: 10,
  startExtendMin: 10,
  startExtendLimit: 1,
  missedStartLimit: 3,        // 3rd missed start in a month deactivates the profile
  // A trip is editable until the first seat is booked, then frozen. Price is
  // never edited directly — it is derived from route distance.
  editLocksOnFirstBooking: true,
  // Reviews are mutual and named, with one reply each.
  reviewsNamed: true,
  reviewReplyLimit: 1,
  // Payouts are processed by ComiGo weekly and only above the floor.
  payoutCadence: "WEEKLY",
  payoutDay: "Friday",
  payoutMinimum: 1000,
  // Search only returns drivers whose trip STARTS within this radius of the
  // rider's pickup point. Filtering happens server-side; the client must never
  // receive a trip it would have to hide.
  searchRadiusKm: 20,
  searchRadiusOptions: [5, 10, 20],
  // Referral. Paid out of ComiGo's commission, never out of a driver's earnings.
  // The rate depends on what the REFEREE does, not on what the referrer is.
  referralPaxPct: 1,          // 1% of a referred passenger's fare
  referralDriverPct: 2,       // 2% of a referred driver's NET trip earnings
  referralWindowMonths: 12,
  referralMaxTrips: 50,       // whichever ends first
  referralPaidFrom: "COMMISSION",
  refereeFirstRideDiscount: 150,
  // The rewards balance both roles share. A passenger spends it as ride credit
  // with no floor; a bank transfer needs the floor and rides the Friday batch.
  rewardsBankMinimum: 1000,
  // Identity verification is never a gate on booking — it is a ranking and
  // eligibility signal. Every capture is taken with the in-app camera; a photo
  // from the gallery is not accepted on either side.
  verifyCameraOnly: true,
  verifyExpires: false,
};

// The route-match discount, banded to match the tiers riders already see. A rider
// who overlaps most of a driver's route is cheap for him to carry, so she pays
// less per km than one he detours for. Stated once here; RIDES carry the figures
// the fare service returned for their own matches.
const matchDiscountPct = (m) => (m >= 95 ? 10 : m >= 75 ? 8 : m >= 45 ? 5 : 2.5);

// Penalties are percentages of the fare, never flat amounts — a flat fee is
// trivial on a long trip and punitive on a short one.
const noShowPenalty = (fare) => Math.round(fare * POLICY.noShowPenaltyPct / 100);
const paxCancelPenalty = (fare) => Math.round(fare * POLICY.paxCancelAfterStartPct / 100);
const driverLatePenalty = (net) => Math.round(net * POLICY.driverLatePenaltyPct / 100);
// Splitting a penalty. The victim's half is rounded and the platform takes the
// remainder, so the two halves always add back to the whole fee exactly.
const victimShare = (fee) => Math.round(fee * POLICY.penaltyVictimPct / 100);
const platformShare = (fee) => fee - victimShare(fee);

// Nimali's early drop-off usage this calendar month. The next one is her 2nd,
// so it is still adjusted; a 3rd would not be.
const EARLY_DROP = { month: "July", used: 1, allowance: POLICY.earlyDropAdjustedPerMonth };
const earlyDropLeft = () => Math.max(0, EARLY_DROP.allowance - EARLY_DROP.used);

// Both sides see the same shape of trust: score, how many scores it rests on,
// completed trips, and how long they have been here.
//
// The star BREAKDOWN is the source and the score is read off it, for both roles.
// A headline average beside a histogram that cannot produce it is arithmetic a
// reader can check on screen — so neither number is typed.
const ratingDist = (d) => d.reduce((a, [, n]) => a + n, 0);
const ratingFromDist = (d) =>
  Math.round(d.reduce((a, [stars, n]) => a + stars * n, 0) / ratingDist(d) * 100) / 100;

const DRIVER_STARS = [[5, 295], [4, 12], [3, 3], [2, 1], [1, 1]];
const PAX_STARS = [[5, 20], [4, 2], [3, 0], [2, 0], [1, 0]];

const TRUST = {
  driver: { stars: DRIVER_STARS, rating: ratingFromDist(DRIVER_STARS), ratings: ratingDist(DRIVER_STARS), trips: ratingDist(DRIVER_STARS), since: "Mar 2025", completed: 98, cancelled: 2, replyMin: 4 },
  passenger: { stars: PAX_STARS, rating: ratingFromDist(PAX_STARS), ratings: ratingDist(PAX_STARS), trips: 24, since: "Nov 2025", completed: 96, noShows: 0 },
};

// CONTEXT 1 — results for a morning Narahenpita → Bambalapitiya search.
// price, gross and discount all arrive from the fare service; the UI never does
// money arithmetic, because a derived discount is how you print "−LKR 0".
// `ratePerKm` differs per driver: admin sets a band per vehicle and the driver
// picks inside it, so two cars on the same road are not the same price.
// `startsKmAway` is how far the driver's trip STARTS from the rider's pickup —
// the radius filter is applied on this, server-side.
const RIDES = [
  { id: "r1", driver: "Saman W", rating: 4.9, trips: 312, match: 100, dist: 6.2, ratePerKm: 52, startsKmAway: 1.2, gross: 322, discount: 32, price: 290, seats: 2, depart: "8:04 AM", car: "Toyota Aqua · Silver", vClass: "Car", plate: "CAR-2211", from: "Narahenpita", to: "Bambalapitiya", overlap: "Narahenpita → Bambalapitiya, all the way", verifiedOnly: true },
  { id: "r2", driver: "Kasun D", rating: 4.8, trips: 128, match: 92, dist: 5.8, ratePerKm: 50, startsKmAway: 2.8, gross: 290, discount: 23, price: 267, seats: 2, depart: "8:12 AM", car: "Suzuki Alto · Blue", vClass: "Car", plate: "WP KB-8842", from: "Narahenpita", to: "Bambalapitiya", overlap: "Drops you 200 m from Bambalapitiya station" },
  { id: "r3", driver: "Priya J", rating: 5.0, trips: 84, match: 74, dist: 4.5, ratePerKm: 46, startsKmAway: 6.4, gross: 207, discount: 10, price: 197, seats: 1, depart: "8:30 AM", car: "Honda Fit · Pearl", vClass: "Car", plate: "CBC-4401", from: "Narahenpita", to: "Thunmulla", overlap: "You walk 450 m at the end", womenOnly: true },
  { id: "r4", driver: "Imran F", rating: 4.7, trips: 201, match: 58, dist: 3.6, ratePerKm: 44, startsKmAway: 14.5, gross: 158, discount: 4, price: 154, seats: 3, depart: "8:45 AM", car: "Perodua Viva · White", vClass: "Car", plate: "KI-1198", from: "Narahenpita", to: "Thunmulla", overlap: "Drops at Thunmulla, you continue on your own" },
];
// How many trips the 20 km filter removed. Search states it rather than silently
// shrinking the list — an unexplained short result reads as "no drivers".
const RADIUS_FILTERED_OUT = 6;

// CONTEXT 2 — the evening journey Nimali has actually booked. Used by checkout,
// the three booking outcomes, live trip, early drop-off and the receipt.
const MY_TRIP = {
  bookingRef: "#8042",
  driver: "Kasun D", rating: 4.8, trips: 128,
  car: "Suzuki Alto · Blue", plate: "WP KB-8842",
  from: "Rajagiriya junction", to: "Nugegoda",
  depart: "6:15 PM", arrive: "6:38 PM",
  match: 92, dist: 5.8, ratePerKm: 50, gross: 290, discount: 23, price: 267, seats: 2,
  // He is late to HER pickup. The grace clock runs from her promised pickup
  // time, not from the trip's departure time — they are different moments.
  pickupAt: "6:15 PM", lateByMin: 11, etaMin: 6,
  // early drop-off: travelled 4.1 km of 5.8, so 1.7 km is refunded
  droppedAt: "Kirulapone", actualDist: 4.1, refund: 85, paid: 182,
  // when the card was actually captured — the moment Kasun started the trip
  chargedAt: "6:15 PM",
  // how long before departure the driver cancelled — 3 h is inside the 12 h
  // window, so he carries a penalty and Nimali was never charged
  cancelledBeforeHrs: 3, autoCancelAfterMin: POLICY.startBufferMin,
  alternatives: [
    { driver: "Dilshan R", depart: "6:25 PM", match: 88, price: 255 },
    { driver: "Fathima N", depart: "6:40 PM", match: 71, price: 198 },
    { driver: "Ruwan P", depart: "6:55 PM", match: 54, price: 162 },
  ],
};

// What Nimali owes ComiGo from earlier trips. It exists because a CASH passenger
// has no card to take a penalty from, so the amount rides along to the next
// booking. Card passengers never accumulate this — theirs is netted on the spot.
const PAX_DUES = {
  items: [
    { what: "No-show fee", why: `${POLICY.noShowPenaltyPct}% of the fare`, when: "21 Jul", trip: "Narahenpita → Thunmulla · Priya J", amount: noShowPenalty(RIDES[2].price), method: "cash" },
  ],
};
const duesTotal = () => PAX_DUES.items.reduce((a, x) => a + x.amount, 0);

// ── IDENTITY VERIFICATION (passenger) ──────────────────────────────────
// Four captures, all taken with the in-app camera — nothing from the gallery.
// Verification is not a gate on booking: it is a ranking signal, a badge, and
// the key to trips a driver has restricted to verified riders.
const VERIFY_STEPS = [
  { key: "front", n: 1, label: "NIC · front", hint: "All four corners inside the frame, no glare on the hologram.", guide: "card" },
  { key: "back", n: 2, label: "NIC · back", hint: "Turn the card over. The address block has to be readable.", guide: "card" },
  { key: "selfie-nic", n: 3, label: "Selfie holding your NIC", hint: "Hold the front of the card beside your face. Both must be sharp.", guide: "both" },
  { key: "photo", n: 4, label: "Profile photo", hint: "A plain selfie. This is the one other people may see — or may not.", guide: "face" },
];
const PAX_VERIFY = {
  level: "VERIFIED",              // NONE | PENDING | VERIFIED | REJECTED
  verifiedOn: "23 Jul",
  nic: "199834502187",
  // What being verified actually buys, in the order a rider cares about.
  benefits: [
    { icon: "search", title: "You appear higher in a driver's request list", body: "Approve-each-request drivers see verified riders first, so fewer of your requests time out." },
    { icon: "lock", title: "Verified-only trips open up", body: "Some drivers accept verified riders only. Those trips are hidden from everyone else." },
    { icon: "shield", title: "A badge on your profile", body: "Drivers can see a real person booked the seat, which is most of why they accept." },
  ],
  // The profile photo is separate from the identity captures and has its own
  // visibility switch. Hidden means hidden in search and in requests — but the
  // driver who is actually picking you up sees it, because he has to find you.
  photoVisibility: "MATCHED",     // PUBLIC | MATCHED | HIDDEN
  photoOptions: [
    { key: "PUBLIC", label: "Show to everyone", body: "Any driver browsing your request sees your photo." },
    { key: "MATCHED", label: "Only my confirmed driver", body: "Hidden until a booking is confirmed. He needs it to find you at the kerb." },
    { key: "HIDDEN", label: "Hide it completely", body: "Nobody sees it — not even your driver. Your initials show instead." },
  ],
};
const verifiedRidesShare = 34;   // % more requests accepted, stated once, in one place

// ── REFERRAL AND THE SHARED REWARDS BALANCE ────────────────────────────
// The commission you earn depends on what the person you invited DOES, not on
// what you are: they ride, you get 1% of the fare; they drive, you get 2% of
// what they keep. It comes out of ComiGo's cut, so nobody's earnings shrink.
const REFERRAL = {
  code: "NIMALI4C",
  link: "comigo.lk/j/NIMALI4C",
  invited: 7, joined: 4,
  rows: [
    { who: "Ishara P", role: "driver", joined: "12 Jun", trips: 41, earned: 980 },
    { who: "Chamodi R", role: "passenger", joined: "4 Jun", trips: 22, earned: 610 },
    { who: "Malith S", role: "passenger", joined: "2 Jul", trips: 9, earned: 250 },
    { who: "Hasini W", role: "passenger", joined: "21 Jul", trips: 0, earned: 0 },
  ],
};
const referralEarned = () => REFERRAL.rows.reduce((a, r) => a + r.earned, 0);
const referralTripsLeft = (r) => Math.max(0, POLICY.referralMaxTrips - r.trips);

// One balance, both roles. A passenger spends it on rides with no floor; a
// driver moves it to a bank account once it clears the floor, in the Friday
// batch that already exists. A passenger's share of a penalty lands here too.
const REWARDS_ROWS = [
  { t: "24 Jul", label: "Referral · Ishara P completed a drive", sub: `${POLICY.referralDriverPct}% of LKR 1,240 kept`, v: 25, kind: "referral" },
  { t: "23 Jul", label: "Driver cancelled after starting", sub: `Your ${POLICY.penaltyVictimPct}% share of his penalty`, v: victimShare(paxCancelPenalty(267)), kind: "comp" },
  { t: "22 Jul", label: "Referral · Chamodi R completed a ride", sub: `${POLICY.referralPaxPct}% of LKR 290`, v: 3, kind: "referral" },
  { t: "18 Jul", label: "Credit used on a booking", sub: "Rajagiriya → Nugegoda", v: -640, kind: "spend" },
];
const rewardsBalance = () => REWARDS_ROWS.reduce((a, r) => a + r.v, 0) + 1385;
const rewardsWithdrawable = () => rewardsBalance() >= POLICY.rewardsBankMinimum;

// Reviews are mutual, named and answerable once. Both sides read the same shape.
const REVIEWS = {
  received: [
    { who: "Dinuka S", stars: 5, when: "Yesterday", body: "Left exactly on time and knew a way around the Baseline Road traffic. Very comfortable ride.", tags: ["Punctual", "Safe driving"] },
    { who: "Sanduni K", stars: 4, when: "22 Jul", body: "Good trip. The AC took a while to cool down but the driving was careful.", reply: "Thanks Sanduni — the compressor was serviced on Tuesday, it cools much faster now." },
  ],
};
// What drivers wrote about her, same shape and same one-reply rule as REVIEWS.
REVIEWS.asRider = [
  { who: "Kasun D", stars: 5, when: "Yesterday", body: "Waiting at the halt before I got there, and told me in the chat exactly where she'd be. Easy trip.", tags: ["On time", "Clear directions"] },
  { who: "Saman W", stars: 5, when: "22 Jul", body: "No fuss about the route change, and paid the adjusted fare without a word.", reply: "Thanks Saman — the Baseline Road detour was quicker in the end." },
];

// A fare adjustment as the PASSENGER experiences it: her driver asked, ComiGo
// approved, and it lands on the receipt she has already been charged against.
const FARE_ADJUST = {
  reason: "Detour on Baseline Road", extraKm: 2.4, amount: 120,
  requestedBy: MY_TRIP.driver, decidedBy: "ComiGo", approvedOn: "23 Jul",
  disputeHours: 48,
};

const ME = { name: "Nimali Perera", firstName: "Nimali", gender: "female" };

// CONTEXT 2b — a trip ALREADY ON THE ROAD that Nimali joins mid-route. A seat
// came free when someone got off early, so this booking captures on acceptance.
const ENROUTE_RIDE = {
  driver: "Ranjith B", rating: 4.9, trips: 402,
  car: "Toyota Premio · Silver", plate: "WP CAF-2290",
  from: "Kirulapone", to: "Colombo Fort",
  depart: "Boarding now", etaMin: 3, arrive: "6:44 PM",
  match: 88, dist: 4.2, ratePerKm: 50, gross: 210, discount: 17, price: 193, seats: 1,
  freedBy: "a passenger who got off at Kirulapone",
};

// ── THE SECOND KIND OF REQUEST: JOINING A MOVING TRIP ──────────────────────
// ComiGo has two request types and they are not variants of each other:
//   SCHEDULED — the trip is published, hasn't left, the driver approves at his
//               leisure and the card is charged when he starts.
//   LIVE      — the trip is already running with a free seat. The driver is at
//               the wheel, so the decision has to fit in one glance and a few
//               seconds, and the card is charged the moment he accepts (there
//               is no later "start" to charge at).
// A live trip is only offerable if the driver has NOT yet passed the rider's
// pickup point. That is a hard server-side filter, never a warning in the UI.
const LIVE_TRIPS = [
  { id: "L1", driver: "Ranjith B", rating: 4.9, trips: 402, car: "Toyota Premio · Silver", plate: "WP CAF-2290",
    now: "Kirulapone", to: "Colombo Fort", pickupIn: 3, pickupEta: "6:31 PM", arrive: "6:44 PM",
    seats: 1, ratePerKm: 50, dist: 4.2, gross: 210, discount: 17, price: 193, match: 88,
    aheadKm: 1.4, freedBy: "someone got off at Kirulapone", verified: true },
  { id: "L2", driver: "Tharindu M", rating: 4.7, trips: 96, car: "Suzuki Every · White", plate: "WP PB-3310",
    now: "Thimbirigasyaya", to: "Pettah", pickupIn: 7, pickupEta: "6:35 PM", arrive: "6:58 PM",
    seats: 2, ratePerKm: 44, dist: 5.1, gross: 224, discount: 12, price: 212, match: 71,
    aheadKm: 3.2, freedBy: "two seats never sold", verified: false },
];
// The one that can't be shown, and why. Kept as a fixture so the empty state
// can say "3 more drivers are on your route but already past you" honestly.
const LIVE_PASSED_COUNT = 3;

// The same moment from the driver's seat: a request landing while he drives.
// `passedPickup` is what the server checked before it ever reached him.
const LIVE_REQUEST = {
  passenger: "Nimali P", verified: true, rating: TRUST.passenger.rating, rides: 63,
  from: "Kirulapone", to: "Colombo Fort", seats: 1, seat: "Back seat",
  fare: 193, net: 174, addedKm: 0.4, addedMin: 2,
  aheadKm: 1.4, aheadMin: 3, passedPickup: false,
  expiresSec: 45,
  onBoard: 3, seatsFreeAfter: 0,
};

// Chat is scoped to one booking: it opens when the booking is confirmed and
// closes 24 hours after the trip ends. No profile-to-profile messaging exists.
const CHAT = {
  bookingRef: "#8042", closesIn: "24 hours after drop-off",
  msgs: [
    { who: "driver", t: "6:02 PM", body: "Hi Nimali — I'll be at the Rajagiriya junction bus halt, not the roundabout. Silver Alto." },
    { who: "me", t: "6:03 PM", body: "Perfect, I'm two minutes from there." },
    { who: "driver", t: "6:09 PM", body: "Traffic at Welikada, running about 4 minutes behind. Sorry!" },
    { who: "me", t: "6:09 PM", body: "No problem, I'll wait at the halt." },
  ],
};

// CONTEXT 3 — the saved weekday commute the dashboard card promotes. Its own
// route, so it can never quote another route's fare.
// price is derived at the bottom of this file, once the route distance and the
// rate bands exist: a fare no legal band can produce is not a fare.
const USUAL_COMMUTE = {
  from: "Nugegoda", to: "Colombo Fort", time: "8:00 AM", matchCount: 3,
  best: { driver: "Ranjith B", match: 96, ratePerKm: 50, depart: "7:52 AM", dist: 0, gross: 0, discount: 0, price: 0 },
};

// CONTEXT 4 — a booking arriving at Nimali AS A DRIVER. The passenger pays the
// fare; the driver nets it minus the platform commission.
const INBOUND_BOOKING = {
  passenger: "Dinuka S", from: "Narahenpita", to: "Bambalapitiya",
  seats: 1, fare: 279, net: 251, tripTime: "8:00 AM",
};

// ── DRIVER SIDE ────────────────────────────────────────────────────────────
// Nimali's OWN vehicle. Deliberately not any driver she rides with: safety copy
// that tells a rider to check the plate cannot share a plate with her own car.
const MY_VEHICLE = {
  make: "Suzuki Wagon R", colour: "Pearl white", plate: "WP CAB-7734",
  vClass: "Car", year: 2019, seats: 3, insuranceExpires: "15 Aug", insuranceDaysLeft: 21,
  insuranceType: "Full", fuel: "Hybrid · 22 km/l",
};

// ── ADMIN-SET PER-KM BAND ───────────────────────────────────────────
// A driver never types a free price. The vehicle CLASS sets a default band, then
// the specific car's age, insurance, fuel figure and condition move it, and the
// result is stored on the vehicle. The driver chooses any rate inside it.
// Seats are capped by the class too — a car is a front seat and two back seats.
const VEHICLE_CLASSES = [
  { key: "car", label: "Car", maxSeats: 3, band: [38, 62] },
  { key: "suv", label: "SUV / Wagon", maxSeats: 4, band: [46, 74] },
  { key: "van", label: "Van", maxSeats: 6, band: [40, 68] },
  { key: "tuk", label: "Three-wheeler", maxSeats: 2, band: [26, 42] },
];
const vehicleClass = (label) => VEHICLE_CLASSES.find(c => c.label === label || c.key === label) || VEHICLE_CLASSES[0];

// The band admin actually approved for THIS car, and the four inputs that moved
// it off the class default. Every factor is signed so the arithmetic is visible.
const RATE_BAND = {
  vehicle: `${MY_VEHICLE.make} · ${MY_VEHICLE.plate}`,
  classLabel: "Car", classBand: [38, 62],
  min: 41, max: 58, chosen: 50, setBy: "ComiGo", setOn: "22 Jul",
  factors: [
    { key: "age", icon: "car", label: "Wear and tyres", detail: `${MY_VEHICLE.year} · market value LKR 3.4 M`, delta: -2 },
    { key: "ins", icon: "shield", label: "Insurance", detail: `${MY_VEHICLE.insuranceType} cover · renews ${MY_VEHICLE.insuranceExpires}`, delta: +3 },
    { key: "fuel", icon: "leaf", label: "Fuel consumption", detail: MY_VEHICLE.fuel, delta: -4 },
    { key: "svc", icon: "settings", label: "Repairs and maintenance", detail: "Serviced 12 Jul · no open defects", delta: +2 },
  ],
};
// A second car, still waiting for an admin to set its band — approved papers are
// not enough to publish, because there would be no legal price to charge.
const PENDING_VEHICLE = { make: "Toyota Vitz", colour: "Silver", plate: "WP CAB-4417", vClass: "Car", year: 2016, seats: 3, submitted: "25 Jul", reviewDays: 2 };
// The band is reviewed, not negotiated: a driver may ask for one re-assessment
// when something about the car changes, and admin answers within this window.
const RATE_REVIEW = { requestable: true, slaDays: 3, lastRequest: null };
// A rate inside the band is a trade, not a free win: charge the top and you earn
// more per km but sit below cheaper cars in results. Stated as a fixture so the
// screen never implies a ranking effect the server doesn't actually apply.
const RATE_POSITIONS = [
  { key: "min", label: "Bottom of your band", rank: "Shown above almost every car on your route", demand: "Highest" },
  { key: "mid", label: "Middle", rank: "Shown in line with similar cars", demand: "Steady" },
  { key: "max", label: "Top of your band", rank: "Shown below cheaper cars on the same road", demand: "Lowest" },
];
// A full-route fare at any rate in the band, for the trip the driver actually
// publishes. Distance is the route's, never a typed figure.
const NEXT_DRIVE_KM = 11.4;
const fareAtRate = (rate, km = NEXT_DRIVE_KM) => Math.round(rate * km);

// Seats are named, never drawn. Where a seat physically sits in the car is not
// something a rider picks — front or back is the only difference that matters.
const seatSlots = (capacity = MY_VEHICLE.seats) =>
  Array.from({ length: capacity }, (_, i) => ({
    id: i + 1,
    label: i === 0 ? "Front seat" : "Back seat",
    sub: i === 0 ? "Beside the driver" : "Rear row",
  }));

const driverNet = (fare) => Math.round(fare * (1 - FARE_POLICY.commissionPct / 100));

// Every driver-facing money figure is NET unless the field says gross. Mixing
// the two is how a dashboard overstates what someone is owed.
const DRIVER_TODAY = { weekTotal: 9160, rating: TRUST.driver.rating, acceptance: 96, trips: TRUST.driver.trips };

// ── DRIVER PREFERENCES ─────────────────────────────────────────────────────
// Account-level defaults applied to every trip published from this account. Two
// of them narrow who may book, and both cost the driver seats — so each one
// states its price in requests, not just its benefit.
const DRIVER_PREFS = {
  gender: "ANYONE",              // ANYONE | WOMEN_ONLY — offered to verified female drivers only
  verifiedOnly: true,            // only NIC-verified riders may book
  verifiedOnlyCost: 3,           // how many of last week's requests it turned away
  approveEachRequest: true,
  midTripBookings: true,
  earlyDropRequests: true,
};
// Share of riders on Nimali's routes who are verified — the number that makes
// "verified riders only" a reasonable setting rather than an empty car.
const VERIFIED_PAX_SHARE = 71;

// Upcoming, live and completed are three DIFFERENT journeys with different
// money — identical figures make them read as one trip in three states.
// Departure is relative to the 9:41 clock on every frame.
// The full-route fare per seat is the route's distance at the rate the driver
// picked inside his admin-set band (D39) — never a typed figure. Change his rate
// or the route and every screen that quotes a seat follows.
const SEAT_FARE = fareAtRate(RATE_BAND.chosen);   // 11.4 km × LKR 50 = 570
const SEAT_NET = driverNet(SEAT_FARE);            // 513

const NEXT_DRIVE = {
  from: "Nugegoda", to: "Colombo Fort", depart: "10:05 AM", inMin: 24,
  seatsBooked: 2, seatsTotal: 3, requests: 1, recurring: 5,
  // Derived below from the booked passengers, because both of them ride only
  // part of the route: two bookings on an 11.4 km trip is not two 11.4 km seats.
  grossExpected: 0, netExpected: 0,
  // The booked riders. D15 lists them, D30 refunds them, D31 reports on them —
  // all three derive from here so a name or fare can only be changed in one place.
  passengers: [
    { name: "Dinuka S", from: "Narahenpita", to: "Bambalapitiya", paid: 251 },
    { name: "Tharindu M", from: "Nugegoda", to: "Thunmulla", paid: 178 },
  ],
};
// What the two bookings are actually worth. `paid` is already net of commission,
// so the gross is back-derived from it and the pair can never drift.
NEXT_DRIVE.netExpected = NEXT_DRIVE.passengers.reduce((a, p) => a + p.paid, 0);
NEXT_DRIVE.grossExpected = Math.round(NEXT_DRIVE.netExpected / (1 - FARE_POLICY.commissionPct / 100));

// Total the riders would have paid — derived, never typed. Under the
// charge-on-start rule nobody is charged before departure, so cancelling a
// scheduled trip refunds nothing; this is the value that never gets collected.
const refundTotal = () => NEXT_DRIVE.passengers.reduce((a, p) => a + p.paid, 0);

// ── THE DRIVER WAS THE ONE WHO LET SOMEONE DOWN ────────────────────────────
// The mirror of P34/P35. She waited past the grace window, cancelled free of
// charge, and he carries the same penalty as a late cancellation — half of it
// reaching her. Derived from the seat she actually booked, never typed.
const DRIVER_LATE = {
  passenger: NEXT_DRIVE.passengers[0].name,
  // Stated, not inferred from the name. Two screens guessed differently about
  // the same fixture person and it read as two different passengers.
  they: "he", them: "him", their: "his", theirs: "his",
  pickup: NEXT_DRIVE.passengers[0].from,
  to: NEXT_DRIVE.passengers[0].to,
  seatNet: NEXT_DRIVE.passengers[0].paid,
  promisedAt: "10:14 AM", cancelledAt: "10:27 AM", lateByMin: 13,
  graceMin: POLICY.driverLateGraceMin,
  // the rest of the trip is unaffected: the other rider is still on board
  stillOnBoard: NEXT_DRIVE.passengers[1].name,
  reason: "Stuck behind an accident on Nawala Road",
};
const driverLateFee = () => driverLatePenalty(DRIVER_LATE.seatNet);

// A scheduled request that went stale: he took too long, and the seat sold to
// someone who instant-booked. The mirror of P14 on the rider's side.
const LAPSED_REQUEST = {
  // Her own person: Dinuka already holds a seat on this trip (INBOUND_BOOKING),
  // so he cannot also be the one whose request expired for want of one.
  passenger: "Ishara M", from: INBOUND_BOOKING.from, to: INBOUND_BOOKING.to,
  they: "she", them: "her", their: "her", theirs: "hers",
  fare: INBOUND_BOOKING.fare, net: INBOUND_BOOKING.net,
  waitedMin: 30, windowMin: 30, takenBy: "Sanduni K", seatsLeft: 0,
};

// ── HER RECORD AS A RIDER ──────────────────────────────────────────────────
// The mirror of D28. Same shape of trust as the driver's, because the rules that
// judge her are the same rules — completion, no-shows, punctuality at the kerb.
const PAX_RELIABILITY = {
  completionPct: TRUST.passenger.completed,
  noShows: TRUST.passenger.noShows,
  lateCancels: 1,
  onTimeAtPickupPct: 92,
  prepayThreshold: 2,          // two no-shows in a month and we ask her to prepay
  monthLabel: EARLY_DROP.month,
};

// Driver reliability, over a rolling calendar month. Late cancellations cost
// money; missed starts cost the profile.
const DRIVER_RELIABILITY = {
  lateCancellations: 1,
  missedStarts: 2,
  missedStartLimit: POLICY.missedStartLimit,
  startExtensionsUsed: 1,
  onTimeStartPct: 94,
  ratingBefore: TRUST.driver.rating, ratingAfter: Math.round((TRUST.driver.rating - 0.06) * 100) / 100,
};
// The penalty for cancelling inside the 12-hour window, taken out of the next
// trip's earnings. Derived from the trip that was cancelled, never typed.
const cancelPenalty = () => Math.round(NEXT_DRIVE.netExpected * POLICY.lateCancelPenaltyPct / 100);
const missedStartsLeft = () => Math.max(0, DRIVER_RELIABILITY.missedStartLimit - DRIVER_RELIABILITY.missedStarts);
// "Dinuka and Tharindu" — first names, list-formatted for prose.
const paxFirstNames = () => {
  const f = NEXT_DRIVE.passengers.map(p => p.name.split(" ")[0]);
  return f.length < 2 ? f.join("") : `${f.slice(0, -1).join(", ")} and ${f[f.length - 1]}`;
};

// Today's schedule. The dashboard totals are derived from this, never typed.
const TODAY_PLAN = [
  { t: "6:20 AM", from: "Nugegoda", to: "Colombo Fort", sub: "3 passengers", net: SEAT_NET * 3, st: "done" },
  { t: "7:45 AM", from: "Nugegoda", to: "Colombo Fort", sub: "2 passengers", net: SEAT_NET * 2, st: "done" },
  { t: "10:05 AM", from: NEXT_DRIVE.from, to: NEXT_DRIVE.to, sub: `${NEXT_DRIVE.seatsBooked} of ${NEXT_DRIVE.seatsTotal} booked · ${NEXT_DRIVE.requests} request`, net: NEXT_DRIVE.netExpected, st: "next" },
  { t: "5:30 PM", from: "Colombo Fort", to: "Nugegoda", sub: "1 of 3 booked", net: SEAT_NET, st: "later" },
];
const earnedToday = () => TODAY_PLAN.filter(t => t.st === "done").reduce((a, t) => a + t.net, 0);
const expectedToday = () => TODAY_PLAN.reduce((a, t) => a + t.net, 0);
const LIVE_DRIVE = {
  from: "Kotte", to: "Colombo Fort", onBoard: 3, expected: 612,
  nextDrop: { name: "Sanduni K", place: "Bambalapitiya", km: 1.2 },
  // A passenger who has asked to get off before her booked stop. The fare is
  // recalculated on distance actually travelled and her seat goes back on sale
  // for the rest of the route.
  earlyDrop: {
    name: "Sanduni K", seat: 3, place: "Kirulapone", aheadM: 300,
    bookedDist: 4.8, actualDist: 2.9, bookedFare: 240, adjustedFare: 145,
    refund: 95, bookedNet: 216, adjustedNet: 131,
    usedThisMonth: 1, allowance: POLICY.earlyDropAdjustedPerMonth,
    remainingLeg: "Kirulapone → Colombo Fort",
  },
};
const DRIVE_HISTORY = [
  { d: "Today · 7:45 AM", from: "Nugegoda", to: "Colombo Fort", pax: 2, amt: 526 },
  { d: "Today · 6:20 AM", from: "Nugegoda", to: "Colombo Fort", pax: 3, amt: 789 },
  { d: "22 Jul · 5:30 PM", from: "Colombo Fort", to: "Nugegoda", pax: 2, amt: 526 },
];

// One verification state. Both the publishing gate and the verification screen
// derive from this, including their counts and summary sentences — two inline
// arrays is how "insurance in review" and "insurance expiring" coexist.
const DRIVER_VERIFICATION = {
  docs: [
    { key: "nic", label: "Identity · NIC", st: "approved", detail: "Approved 21 Jul · 199834502187" },
    { key: "licence", label: "Driving licence", st: "approved", detail: "Approved 22 Jul · expires Mar 2029" },
    { key: "vreg", label: "Vehicle registration", st: "approved", detail: `${MY_VEHICLE.make} · ${MY_VEHICLE.plate}` },
    { key: "insurance", label: "Insurance certificate", st: "expiring", action: "Renew", detail: `Expires ${MY_VEHICLE.insuranceExpires} — renew to keep publishing` },
    { key: "revenue", label: "Revenue licence", st: "rejected", action: "Redo", detail: "Rejected: photo cut off at the edge" },
    { key: "payout", label: "Payout account", st: "none", action: "Add", detail: "Add a bank or mobile wallet to receive fares" },
  ],
};
// rejected and not-started block publishing; expiring is a warning, not a block
const vBlockers = () => DRIVER_VERIFICATION.docs.filter(x => x.st === "rejected" || x.st === "none");
const vWarnings = () => DRIVER_VERIFICATION.docs.filter(x => x.st === "expiring");

// A ride that was cancelled, for trip history. Priya's own route, not another's.
const CANCELLED_RIDE = { d: "21 Jul · 8:30 AM", from: RIDES[2].from, to: RIDES[2].to, who: RIDES[2].driver, m: RIDES[2].match, reason: "Cancelled by driver" };

// Ledger rows are GROSS fare in, commission out — the same convention on every
// row. Booking net as the fare line and then deducting the fee double-counts.
const LEDGER_T0 = TODAY_PLAN[0];
// Gross is back-derived from the trip's net so the pair can never drift from
// TODAY_PLAN, which is what D08 and D25 total. Gross in, commission out.
const LEDGER_T0_GROSS = Math.round(LEDGER_T0.net / (1 - FARE_POLICY.commissionPct / 100));
const LEDGER_T1 = TODAY_PLAN[1];
const LEDGER_T1_GROSS = Math.round(LEDGER_T1.net / (1 - FARE_POLICY.commissionPct / 100));
const LEDGER = [
  { t: `Today · ${LEDGER_T0.t}`, label: `Trip fare · ${LEDGER_T0.sub}`, sub: `${LEDGER_T0.from} → ${LEDGER_T0.to}`, v: LEDGER_T0_GROSS, kind: "fare" },
  { t: `Today · ${LEDGER_T0.t}`, label: `ComiGo fee · ${FARE_POLICY.commissionPct}%`, sub: `On LKR ${LEDGER_T0_GROSS} collected`, v: -(LEDGER_T0_GROSS - LEDGER_T0.net), kind: "fee" },
  { t: `Today · ${LEDGER_T1.t}`, label: `Trip fare · ${LEDGER_T1.sub}`, sub: `${LEDGER_T1.from} → ${LEDGER_T1.to}`, v: LEDGER_T1_GROSS, kind: "fare" },
  { t: `Today · ${LEDGER_T1.t}`, label: `ComiGo fee · ${FARE_POLICY.commissionPct}%`, sub: `On LKR ${LEDGER_T1_GROSS} collected`, v: -(LEDGER_T1_GROSS - LEDGER_T1.net), kind: "fee" },
  { t: "23 Jul · 6:12 PM", label: "Fare adjustment", sub: "Approved · detour on Baseline Road", v: 120, kind: "adjust" },
  { t: "23 Jul · 8:20 AM", label: "Early drop-off adjustment", sub: `Sanduni K got off ${(LIVE_DRIVE.earlyDrop.bookedDist - LIVE_DRIVE.earlyDrop.actualDist).toFixed(1)} km early`, v: -(LIVE_DRIVE.earlyDrop.bookedNet - LIVE_DRIVE.earlyDrop.adjustedNet), kind: "adjust" },
  { t: "22 Jul · 7:45 AM", label: `Late-cancellation penalty · ${POLICY.lateCancelPenaltyPct}%`, sub: `Cancelled ${MY_TRIP.cancelledBeforeHrs} h before departure · half to the riders`, v: -cancelPenalty(), kind: "penalty" },
  // A penalty can now be a POSITIVE line: when a rider stands you up, half his
  // fee reaches you. It is compensation for a seat nobody could use, not income
  // from the trip — so it is its own kind, never folded into the fare.
  { t: "20 Jul · 8:12 AM", label: `No-show compensation · your ${POLICY.penaltyVictimPct}% share`, sub: "Dinuka S never boarded at Narahenpita", v: victimShare(noShowPenalty(290)), kind: "comp" },
  { t: "1 Jul · 11:02 AM", label: "Weekly payout to BOC ···2204", sub: "Week of 13–19 Jul", v: -8420, kind: "payout" },
];
// The payout row settles an earlier balance, so it is not part of what is owed now.
const ledgerBalance = () => LEDGER.filter(r => r.kind !== "payout").reduce((a, r) => a + r.v, 0);

// Payouts are processed by ComiGo monthly and only clear the floor. Below it the
// balance rolls into next month — it is never lost, only held.
const PAYOUT = {
  minimum: POLICY.payoutMinimum, day: POLICY.payoutDay, nextDate: "Friday 31 Jul",
  balance: ledgerBalance(),
  heldBalance: 740,             // the same screen, for a driver under the floor
  last: { amount: 8420, when: "Fri 24 Jul", to: "BOC ···2204" },
};
const payoutEligible = (b = PAYOUT.balance) => b >= PAYOUT.minimum;

// Week runs Mon–Sun and today is Sunday 26 July 2026, so today IS the last bar.
const WEEK_RANGE = "20–26 Jul";
const WEEK_DAYS = [
  { d: "Mon", v: 1840 }, { d: "Tue", v: 2210 }, { d: "Wed", v: 1315 },
  { d: "Thu", v: 2480 }, { d: "Fri", v: 0 }, { d: "Sat", v: 0 },
  { d: "Sun", v: earnedToday(), today: true },
];
const weekTotal = () => WEEK_DAYS.reduce((a, b) => a + b.v, 0);
// Keep the field every screen already reads, but make it derived rather than typed.
DRIVER_TODAY.weekTotal = weekTotal();

// CONTEXT 5 — a past, separate trip the open support ticket is about.
const TICKET_TRIP = { bookingRef: "#7911", driver: "Saman W", from: "Narahenpita", to: "Bambalapitiya", charged: 279 };

// Nimali's saved commute, priced the same way every other seat in the app is:
// the distance she actually overlaps, at the driver's own per-km rate inside his
// band, less the route-match discount her overlap earns. 288 used to be typed
// here and worked out at LKR 25/km — below the floor of every vehicle class.
{
  const u = USUAL_COMMUTE.best;
  u.dist = Math.round(NEXT_DRIVE_KM * u.match / 100 * 10) / 10;
  u.gross = fareAtRate(u.ratePerKm, u.dist);
  u.discount = Math.round(u.gross * matchDiscountPct(u.match) / 100);
  u.price = u.gross - u.discount;
}

const rideById = (id) => RIDES.find(r => r.id === id);
const cheapestFare = () => Math.min(...RIDES.map(r => r.price));

Object.assign(window, {
  FARE_POLICY, POLICY, EARLY_DROP, earlyDropLeft, TRUST, CHAT, ENROUTE_RIDE,
  ratingFromDist, ratingDist,
  LIVE_TRIPS, LIVE_PASSED_COUNT, LIVE_REQUEST,
  PAX_DUES, duesTotal, REVIEWS, noShowPenalty, paxCancelPenalty, victimShare, platformShare,
  RIDES, RADIUS_FILTERED_OUT, MY_TRIP, USUAL_COMMUTE, INBOUND_BOOKING, TICKET_TRIP, ME,
  VERIFY_STEPS, PAX_VERIFY, verifiedRidesShare,
  REFERRAL, referralEarned, referralTripsLeft, REWARDS_ROWS, rewardsBalance, rewardsWithdrawable,
  MY_VEHICLE, VEHICLE_CLASSES, vehicleClass, RATE_BAND, PENDING_VEHICLE, seatSlots,
  RATE_REVIEW, RATE_POSITIONS, NEXT_DRIVE_KM, fareAtRate, DRIVER_PREFS, VERIFIED_PAX_SHARE,
  DRIVER_TODAY, NEXT_DRIVE, LIVE_DRIVE, DRIVE_HISTORY,
  SEAT_FARE, SEAT_NET, driverNet, TODAY_PLAN, earnedToday, expectedToday,
  LEDGER, ledgerBalance, PAYOUT, payoutEligible, WEEK_DAYS, WEEK_RANGE, weekTotal,
  DRIVER_VERIFICATION, vBlockers, vWarnings, CANCELLED_RIDE,
  DRIVER_RELIABILITY, cancelPenalty, missedStartsLeft, refundTotal, paxFirstNames,
  DRIVER_LATE, driverLateFee, driverLatePenalty, LAPSED_REQUEST, PAX_RELIABILITY, FARE_ADJUST,
  rideById, cheapestFare, matchDiscountPct,
});
