// Generated lightweight API contract inventory for app client wiring readiness.
// Source of truth remains docs/api/*.openapi.json.

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
export type ApiEndpoint = { method: HttpMethod; path: string };

export const passengerApiEndpoints = [
  {
    "method": "GET",
    "path": "/api/v1/app/config"
  },
  {
    "method": "GET",
    "path": "/api/v1/auth/me"
  },
  {
    "method": "GET",
    "path": "/api/v1/passenger/bookings"
  },
  {
    "method": "POST",
    "path": "/api/v1/passenger/bookings"
  },
  {
    "method": "GET",
    "path": "/api/v1/passenger/bookings/{bookingId}"
  },
  {
    "method": "POST",
    "path": "/api/v1/passenger/bookings/{bookingId}/cancel"
  },
  {
    "method": "POST",
    "path": "/api/v1/passenger/bookings/{bookingId}/early-drop-off"
  },
  {
    "method": "POST",
    "path": "/api/v1/passenger/bookings/{bookingId}/rating"
  },
  {
    "method": "GET",
    "path": "/api/v1/passenger/bookings/{bookingId}/receipt"
  },
  {
    "method": "POST",
    "path": "/api/v1/passenger/bookings/{bookingId}/share"
  },
  {
    "method": "POST",
    "path": "/api/v1/passenger/bookings/{bookingId}/share-link"
  },
  {
    "method": "GET",
    "path": "/api/v1/passenger/notification-preferences"
  },
  {
    "method": "PUT",
    "path": "/api/v1/passenger/notification-preferences"
  },
  {
    "method": "GET",
    "path": "/api/v1/passenger/notifications"
  },
  {
    "method": "POST",
    "path": "/api/v1/passenger/notifications/{notificationId}/read"
  },
  {
    "method": "GET",
    "path": "/api/v1/passenger/payment-methods"
  },
  {
    "method": "POST",
    "path": "/api/v1/passenger/payment-methods"
  },
  {
    "method": "DELETE",
    "path": "/api/v1/passenger/payment-methods/{paymentMethodId}"
  },
  {
    "method": "POST",
    "path": "/api/v1/passenger/payment-methods/{paymentMethodId}/default"
  },
  {
    "method": "POST",
    "path": "/api/v1/passenger/payments/intents"
  },
  {
    "method": "GET",
    "path": "/api/v1/passenger/profile"
  },
  {
    "method": "PUT",
    "path": "/api/v1/passenger/profile"
  },
  {
    "method": "POST",
    "path": "/api/v1/passenger/profile/avatar-upload"
  },
  {
    "method": "POST",
    "path": "/api/v1/passenger/push-registrations"
  },
  {
    "method": "POST",
    "path": "/api/v1/passenger/ride-searches"
  },
  {
    "method": "GET",
    "path": "/api/v1/passenger/ride-searches/{searchId}/results"
  },
  {
    "method": "GET",
    "path": "/api/v1/passenger/ride-searches/{searchId}/results/{resultId}"
  },
  {
    "method": "GET",
    "path": "/api/v1/passenger/saved-places"
  },
  {
    "method": "POST",
    "path": "/api/v1/passenger/saved-places"
  },
  {
    "method": "DELETE",
    "path": "/api/v1/passenger/saved-places/{savedPlaceId}"
  },
  {
    "method": "PUT",
    "path": "/api/v1/passenger/saved-places/{savedPlaceId}"
  },
  {
    "method": "POST",
    "path": "/api/v1/passenger/sos-events"
  },
  {
    "method": "GET",
    "path": "/api/v1/passenger/support/tickets"
  },
  {
    "method": "POST",
    "path": "/api/v1/passenger/support/tickets"
  },
  {
    "method": "GET",
    "path": "/api/v1/passenger/support/tickets/{ticketId}"
  },
  {
    "method": "POST",
    "path": "/api/v1/passenger/support/tickets/{ticketId}/messages"
  },
  {
    "method": "GET",
    "path": "/api/v1/passenger/trips/current"
  },
  {
    "method": "GET",
    "path": "/api/v1/passenger/trips/history"
  },
  {
    "method": "GET",
    "path": "/api/v1/passenger/trips/{tripId}/live-state"
  },
  {
    "method": "GET",
    "path": "/api/v1/passenger/trusted-contacts"
  },
  {
    "method": "POST",
    "path": "/api/v1/passenger/trusted-contacts"
  },
  {
    "method": "DELETE",
    "path": "/api/v1/passenger/trusted-contacts/{contactId}"
  },
  {
    "method": "PUT",
    "path": "/api/v1/passenger/trusted-contacts/{contactId}"
  },
  {
    "method": "POST",
    "path": "/api/v1/passenger/verification/documents"
  },
  {
    "method": "GET",
    "path": "/api/v1/passenger/verification/status"
  }
] as const satisfies readonly ApiEndpoint[];

export const driverApiEndpoints = [
  {
    "method": "GET",
    "path": "/api/v1/app/config"
  },
  {
    "method": "GET",
    "path": "/api/v1/auth/me"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/application"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/bookings/{bookingId}/approve"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/bookings/{bookingId}/cash-collected"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/bookings/{bookingId}/decline"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/bookings/{bookingId}/fare-adjustment-request"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/documents"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/documents"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/documents/{documentId}/submit"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/earnings/summary"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/earnings/transactions"
  },
  {
    "method": "PUT",
    "path": "/api/v1/driver/kyc/identity"
  },
  {
    "method": "PUT",
    "path": "/api/v1/driver/kyc/licence"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/notification-preferences"
  },
  {
    "method": "PUT",
    "path": "/api/v1/driver/notification-preferences"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/notifications"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/notifications/{notificationId}/read"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/payout-profile"
  },
  {
    "method": "PUT",
    "path": "/api/v1/driver/payout-profile"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/profile"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/push-registrations"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/ratings"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/recurring-routes"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/recurring-routes"
  },
  {
    "method": "DELETE",
    "path": "/api/v1/driver/recurring-routes/{routeId}"
  },
  {
    "method": "PUT",
    "path": "/api/v1/driver/recurring-routes/{routeId}"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/recurring-routes/{routeId}/generate-occurrences"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/routes"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/routes"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/routes/{routeId}"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/routes/{routeId}/cancel"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/routes/{routeId}/publish"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/routes/{routeId}/share-link"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/sos-events"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/support/tickets"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/support/tickets"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/support/tickets/{ticketId}"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/support/tickets/{ticketId}/messages"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/trips"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/trips/{tripId}"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/trips/{tripId}/arrived-pickup"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/trips/{tripId}/booking-requests"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/trips/{tripId}/complete"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/trips/{tripId}/passengers/{bookingId}/board"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/trips/{tripId}/passengers/{bookingId}/drop-off"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/trips/{tripId}/passengers/{bookingId}/no-show"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/trips/{tripId}/pre-trip-checklist"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/trips/{tripId}/start"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/vehicles"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/vehicles"
  },
  {
    "method": "DELETE",
    "path": "/api/v1/driver/vehicles/{vehicleId}"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/vehicles/{vehicleId}"
  },
  {
    "method": "PUT",
    "path": "/api/v1/driver/vehicles/{vehicleId}"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/vehicles/{vehicleId}/documents"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/vehicles/{vehicleId}/documents"
  },
  {
    "method": "POST",
    "path": "/api/v1/driver/vehicles/{vehicleId}/documents/{documentId}/submit"
  },
  {
    "method": "GET",
    "path": "/api/v1/driver/verification-status"
  },
  {
    "method": "POST",
    "path": "/api/v1/location/updates"
  }
] as const satisfies readonly ApiEndpoint[];

export const adminApiEndpoints = [
  {
    "method": "GET",
    "path": "/api/v1/admin/audit/actions"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/bookings"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/bookings/{bookingId}"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/bookings/{bookingId}/status-history"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/cash-collections"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/commission-rules"
  },
  {
    "method": "POST",
    "path": "/api/v1/admin/commission-rules"
  },
  {
    "method": "PUT",
    "path": "/api/v1/admin/commission-rules/{ruleId}"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/dashboard"
  },
  {
    "method": "POST",
    "path": "/api/v1/admin/documents/{documentId}/download-url"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/driver-applications"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/driver-applications/{driverId}"
  },
  {
    "method": "POST",
    "path": "/api/v1/admin/driver-applications/{driverId}/review"
  },
  {
    "method": "POST",
    "path": "/api/v1/admin/driver-documents/{documentId}/review"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/fare-policies"
  },
  {
    "method": "POST",
    "path": "/api/v1/admin/fare-policies"
  },
  {
    "method": "PUT",
    "path": "/api/v1/admin/fare-policies/{policyId}"
  },
  {
    "method": "POST",
    "path": "/api/v1/admin/finance/adjustments"
  },
  {
    "method": "POST",
    "path": "/api/v1/admin/notifications/broadcasts"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/payments"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/payments/{paymentIntentId}"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/payments/{paymentIntentId}/events"
  },
  {
    "method": "POST",
    "path": "/api/v1/admin/payments/{paymentIntentId}/refund"
  },
  {
    "method": "POST",
    "path": "/api/v1/admin/payments/{paymentIntentId}/void"
  },
  {
    "method": "POST",
    "path": "/api/v1/admin/reports/export"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/reports/summary"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/safety/sos-events"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/safety/sos-events/{sosEventId}"
  },
  {
    "method": "POST",
    "path": "/api/v1/admin/safety/sos-events/{sosEventId}/resolve"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/settlements/driver-balances"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/settlements/payout-batches"
  },
  {
    "method": "POST",
    "path": "/api/v1/admin/settlements/payout-batches"
  },
  {
    "method": "POST",
    "path": "/api/v1/admin/settlements/payout-batches/{batchId}/mark-paid"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/support/tickets"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/support/tickets/{ticketId}"
  },
  {
    "method": "PUT",
    "path": "/api/v1/admin/support/tickets/{ticketId}"
  },
  {
    "method": "POST",
    "path": "/api/v1/admin/support/tickets/{ticketId}/messages"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/trips"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/trips/live"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/trips/{tripId}"
  },
  {
    "method": "POST",
    "path": "/api/v1/admin/trips/{tripId}/cancel"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/trips/{tripId}/location-trail"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/users"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/users/{appUserId}"
  },
  {
    "method": "POST",
    "path": "/api/v1/admin/users/{appUserId}/activate"
  },
  {
    "method": "PUT",
    "path": "/api/v1/admin/users/{appUserId}/roles"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/users/{appUserId}/status-history"
  },
  {
    "method": "POST",
    "path": "/api/v1/admin/users/{appUserId}/suspend"
  },
  {
    "method": "POST",
    "path": "/api/v1/admin/vehicle-documents/{documentId}/review"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/vehicles"
  },
  {
    "method": "GET",
    "path": "/api/v1/admin/vehicles/{vehicleId}"
  },
  {
    "method": "POST",
    "path": "/api/v1/admin/vehicles/{vehicleId}/review"
  },
  {
    "method": "GET",
    "path": "/api/v1/auth/me"
  }
] as const satisfies readonly ApiEndpoint[];

export const apiContractCounts = {
  passenger: passengerApiEndpoints.length,
  driver: driverApiEndpoints.length,
  admin: adminApiEndpoints.length,
} as const;
