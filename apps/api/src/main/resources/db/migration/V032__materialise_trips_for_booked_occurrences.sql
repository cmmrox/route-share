-- A trip is materialised when an occurrence takes its first confirmed booking, which is the moment
-- the start-buffer clock first has stakes. Before V032 nothing in the application created a
-- trip.trip row at all: publication produced route_plan and route_occurrence rows and stopped, so
-- every downstream path keyed on a trip (capture at start, the start window, location samples) was
-- unreachable outside hand-seeded data.
--
-- One trip per occurrence, enforced here rather than in Java: two passengers booking the last two
-- seats at the same instant are two transactions racing on the same occurrence, and an application
-- check cannot make that safe. The insert relies on this index to arbitrate.
--
-- Partial, because route_occurrence_id stays nullable: ad-hoc trips with no occurrence behind them
-- predate the occurrence model and must not collapse onto a single NULL key.
CREATE UNIQUE INDEX trip_route_occurrence_uk
    ON trip.trip (route_occurrence_id)
    WHERE route_occurrence_id IS NOT NULL;

DROP INDEX IF EXISTS trip.trip_route_occurrence_idx;
