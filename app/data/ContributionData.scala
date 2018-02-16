package data

import java.sql.Connection

import anorm._
import cats.data.EitherT
import cats.syntax.either._
import data.AnormMappings._
import models.{ContributionMetaData, Contributor, PaymentHook}
import monitoring.LoggingTags
import monitoring.TagAwareLogger
import play.api.db.Database

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait ContributionData {
  def insertPaymentHook(paymentHook: PaymentHook)(implicit tags: LoggingTags): EitherT[Future, String, PaymentHook]
  def insertPaymentMetaData(pmd: ContributionMetaData)(implicit tags: LoggingTags): EitherT[Future, String, ContributionMetaData]
  def saveContributor(contributor: Contributor)(implicit tags: LoggingTags): EitherT[Future, String, Contributor]
}

class CurrentAndNewContributionData(currentDB: ContributionDataInstance, newDB: ContributionDataInstance) extends ContributionData {

  override def insertPaymentHook(paymentHook: PaymentHook)(implicit tags: LoggingTags): EitherT[Future, String, PaymentHook] = {
    newDB.insertPaymentHook(paymentHook)
    currentDB.insertPaymentHook(paymentHook)
  }

  override def insertPaymentMetaData(pmd: ContributionMetaData)(implicit tags: LoggingTags): EitherT[Future, String, ContributionMetaData] = {
    newDB.insertPaymentMetaData(pmd)
    currentDB.insertPaymentMetaData(pmd)
  }

  override def saveContributor(contributor: Contributor)(implicit tags: LoggingTags): EitherT[Future, String, Contributor] = {
    newDB.saveContributor(contributor)
    currentDB.saveContributor(contributor)
  }
}

class ContributionDataInstance(db: Database, paymentProviderType: String, paymentStatusType: String)(implicit ec: ExecutionContext) extends ContributionData with TagAwareLogger {

  def withAsyncConnection[A](autocommit: Boolean = false)(block: Connection => A)(implicit tags: LoggingTags): EitherT[Future, String, A] = EitherT(Future {
    val result = Try(db.withConnection(autocommit)(block))
    Either.fromTry(result).leftMap { exception =>
      val errorMessage = s"Error encountered during the execution of the sql query on the ${db.name} database"
      error(errorMessage, exception)
      errorMessage
    }
  })

  def insertPaymentHook(paymentHook: PaymentHook)(implicit tags: LoggingTags): EitherT[Future, String, PaymentHook] = {
    withAsyncConnection(autocommit = true) { implicit conn =>
      info(s"Inserting into ${db.name}.payment_hook table")
      val request = SQL"""
        INSERT INTO payment_hooks(
          contributionid,
          paymentid,
          provider,
          created,
          currency,
          amount,
          convertedamount,
          status,
          email
        ) VALUES (
          ${paymentHook.contributionId.id}::uuid,
          ${paymentHook.paymentId},
          ${paymentHook.provider}::#$paymentProviderType,
          ${paymentHook.created},
          ${paymentHook.currency},
          ${paymentHook.amount},
          ${paymentHook.convertedAmount},
          ${paymentHook.status}::#$paymentStatusType,
          ${paymentHook.email}
        ) ON CONFLICT(contributionId) DO
        UPDATE SET
          contributionid = excluded.contributionid,
          paymentid = excluded.paymentid,
          provider = excluded.provider,
          created = excluded.created,
          currency = excluded.currency,
          amount = excluded.amount,
          convertedamount = excluded.convertedamount,
          status = excluded.status,
          email = excluded.email"""
      request.execute()
      paymentHook
    }
  }

  def insertPaymentMetaData(pmd: ContributionMetaData)(implicit tags: LoggingTags): EitherT[Future, String, ContributionMetaData] = {
    withAsyncConnection(autocommit = true) { implicit conn =>
      info(s"Inserting into ${db.name}.contribution_metadata table")
      val request = SQL"""
        INSERT INTO contribution_metadata(
          contributionid,
          created,
          email,
          country,
          ophan_pageview_id,
          ophan_browser_id,
          abtests,
          cmp,
          intcmp,
          referer_url,
          referer_pageview_id,
          platform
        ) VALUES (
          ${pmd.contributionId.id}::uuid,
          ${pmd.created},
          ${pmd.email},
          ${pmd.country},
          ${pmd.ophanPageviewId},
          ${pmd.ophanBrowserId},
          ${pmd.abTestAsJson},
          ${pmd.cmp},
          ${pmd.intCmp},
          ${pmd.refererUrl},
          ${pmd.refererPageviewId},
          ${pmd.platform}
        ) ON CONFLICT(contributionId) DO
        UPDATE SET
          contributionid = excluded.contributionid,
          created = excluded.created,
          email = excluded.email,
          country = excluded.country,
          ophan_pageview_id = excluded.ophan_pageview_id,
          ophan_browser_id = excluded.ophan_browser_id,
          abtests = excluded.abtests,
          cmp = excluded.cmp,
          intcmp = excluded.intcmp,
          referer_url = excluded.referer_url,
          referer_pageview_id = excluded.referer_pageview_id,
          platform = excluded.platform"""
      request.execute()
      pmd
    }
  }

  def saveContributor(contributor: Contributor)(implicit tags: LoggingTags): EitherT[Future, String, Contributor] = {
    withAsyncConnection(autocommit = true) { implicit conn =>
      // Note that the contributor ID will only set on insert. it's not touched on update.
      info(s"Inserting into ${db.name}.live_contributors table")
      val request = SQL"""
        INSERT INTO live_contributors(
          receipt_email,
          name,
          firstname,
          lastname,
          iduser,
          postcode,
          contributor_id,
          updated
        ) VALUES (
          ${contributor.email},
          ${contributor.name},
          ${contributor.firstName},
          ${contributor.lastName},
          ${contributor.idUser.map(_.id)},
          ${contributor.postCode},
          ${contributor.contributorId.map(_.id)}::uuid,
          now()
        ) ON CONFLICT(receipt_email) DO
        UPDATE SET
          receipt_email = excluded.receipt_email,
          name = COALESCE(excluded.name, live_contributors.name),
          firstname = COALESCE(excluded.firstname, live_contributors.firstname),
          lastname = COALESCE(excluded.lastname, live_contributors.lastname),
          iduser = COALESCE(excluded.iduser, live_contributors.iduser),
          postcode = COALESCE(excluded.postcode, live_contributors.postcode),
          updated = now()"""
      request.execute()
      contributor
    }
  }
}
