package data

import anorm._
import data.AnormMappings._
import models.PaymentHook
import play.api.db.Database

class ContributionData(db: Database) {
  def insertPaymentHook(paymentHook: PaymentHook): Unit = {
    db.withConnection(autocommit = true) { implicit conn =>
      val request = SQL"""
        INSERT INTO payment_hooks(
          contributionid,
          paymentid,
          provider,
          created,
          currency,
          cardcountry,
          amount,
          convertedamount,
          status,
          email
        ) VALUES (
          ${paymentHook.contributionId}::uuid,
          ${paymentHook.paymentId},
          ${paymentHook.provider}::paymentProvider,
          ${paymentHook.created},
          ${paymentHook.currency},
          ${paymentHook.cardCountry},
          ${paymentHook.amount},
          ${paymentHook.convertedAmount},
          ${paymentHook.status}::paymentStatus,
          ${paymentHook.email}
        ) ON CONFLICT(contributionId) DO
        UPDATE SET
          contributionid = excluded.contributionid,
          paymentid = excluded.paymentid,
          provider = excluded.provider,
          created = excluded.created,
          currency = excluded.currency,
          cardcountry = excluded.cardcountry,
          amount = excluded.amount,
          convertedamount = excluded.convertedamount,
          status = excluded.status,
          email = excluded.email"""
      request.execute()
    }
  }
}
