package com.routeshare.booking.repository;

import com.routeshare.booking.entity.BookingSeatEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingSeatRepository extends JpaRepository<BookingSeatEntity, Long> {

  List<BookingSeatEntity> findByBookingId(long bookingId);

  @Query(
      "select s from BookingSeatEntity s where s.bookingId = :bookingId and s.releasedAt is null")
  List<BookingSeatEntity> findLiveHolds(@Param("bookingId") long bookingId);

  /** The named seats a booking holds, for the detail response and the receipt. */
  @Query(
      value =
          """
      SELECT s.route_occurrence_seat_id AS "seatId",
             s.slot_index AS "slotIndex",
             s.label AS "label",
             s.sub_label AS "subLabel"
        FROM booking.booking_seat bs
        JOIN routing.route_occurrence_seat s
          ON s.route_occurrence_seat_id = bs.route_occurrence_seat_id
       WHERE bs.booking_id = :bookingId
         AND bs.released_at IS NULL
       ORDER BY s.slot_index
      """,
      nativeQuery = true)
  List<HeldSeatRow> findHeldSeats(@Param("bookingId") long bookingId);

  interface HeldSeatRow {
    Long getSeatId();

    Integer getSlotIndex();

    String getLabel();

    String getSubLabel();
  }
}
