package com.routeshare.passenger.service;

import com.routeshare.passenger.dto.request.PhotoVisibilityRequest;
import com.routeshare.passenger.dto.response.PhotoVisibilityResponse;
import java.util.Optional;

/**
 * P30 — who may see whose face, decided on every read.
 *
 * <p>Evaluated server-side and nowhere else. A {@code HIDDEN} photo URL must never be emitted, not
 * even for a client to ignore: a URL in a payload is a URL in a log, a cache and a proxy, and the
 * rider who chose to hide her face did not choose any of those.
 *
 * <p>The asymmetry is deliberate. A rider may hide her photo from everyone including her driver; a
 * driver may not, because she is getting into his car and has to know it is him (D35). So a
 * driver's photo is always returned to a confirmed rider — and never in search, where nobody has
 * committed to anything yet.
 */
public interface PhotoVisibilityService {

  /** The rider's current choice, with P30's three options and their copy. */
  PhotoVisibilityResponse mine();

  PhotoVisibilityResponse update(PhotoVisibilityRequest request);

  /**
   * The photo URL a viewer is allowed to see, or empty.
   *
   * @param viewerAppUserId who is asking
   * @param subjectAppUserId whose photo it is
   * @param context where the photo is being shown, which decides what {@code MATCHED} means
   */
  Optional<String> resolve(long viewerAppUserId, long subjectAppUserId, ViewContext context);

  /**
   * What the viewer and the subject are to each other at the moment of the read.
   *
   * <p>Passed in rather than looked up here, because the caller already knows — and a service that
   * went and asked would have to reach into booking from passenger to do it.
   */
  enum ViewContext {
    /** Nobody has committed to anything. Only PUBLIC survives, and no driver photo is shown. */
    SEARCH,

    /** A request the driver has not answered. Still not a confirmed booking. */
    PENDING_REQUEST,

    /** A confirmed booking, and the subject is the <em>rider</em> on it. MATCHED opens. */
    CONFIRMED_BOOKING,

    /**
     * A confirmed booking, and the subject is the <em>driver</em> of it. Always shown — she is
     * getting into his car and has to know it is him (D35).
     *
     * <p>Distinguished from {@link #CONFIRMED_BOOKING} rather than inferred from whether the
     * subject happens to have a driver profile, because in a unified app most drivers also ride.
     * Inferring it would show a hidden photo to the driver of any rider who also drives.
     */
    CONFIRMED_BOOKING_DRIVER,

    /** The subject is the viewer. Everyone may see their own photo. */
    SELF
  }
}
