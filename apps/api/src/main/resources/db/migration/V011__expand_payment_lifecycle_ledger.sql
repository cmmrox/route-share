ALTER TABLE payment.fare_ledger_entry
  DROP CONSTRAINT IF EXISTS fare_ledger_entry_entry_type_check;

ALTER TABLE payment.fare_ledger_entry
  DROP CONSTRAINT IF EXISTS fare_ledger_entry_amount_check;

ALTER TABLE payment.fare_ledger_entry
  ADD CONSTRAINT fare_ledger_entry_entry_type_check
    CHECK (entry_type IN (
      'BOOKING_FARE_ESTIMATE',
      'PAYMENT_CAPTURED',
      'PAYMENT_VOIDED',
      'PAYMENT_REFUNDED',
      'CASH_COLLECTED',
      'DRIVER_EARNING',
      'PLATFORM_COMMISSION',
      'SETTLEMENT_ADJUSTMENT'
    ));

ALTER TABLE payment.fare_ledger_entry
  ADD CONSTRAINT fare_ledger_entry_amount_check CHECK (amount <> 0);
