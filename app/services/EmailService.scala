package services

import cats.data.{Xor, XorT}
import com.amazonaws.auth._
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.typesafe.scalalogging.LazyLogging
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.model._
import configuration.Config
import models.ContributorRow
import play.api.Logger
import play.api.libs.json._
import utils.AwsAsyncHandler

import scala.concurrent.{ExecutionContext, Future}

class EmailService(implicit ec: ExecutionContext) extends LazyLogging {

  val credentialsProviderChain: AWSCredentialsProviderChain = new AWSCredentialsProviderChain(
    new EnvironmentVariableCredentialsProvider,
    new SystemPropertiesCredentialsProvider,
    new ProfileCredentialsProvider("membership"),
    new InstanceProfileCredentialsProvider
  )

  private val sqsClient = new AmazonSQSAsyncClient(credentialsProviderChain)
  sqsClient.setRegion(Region.getRegion(Regions.EU_WEST_1))

  val thankYouQueueUrl = sqsClient.getQueueUrl(Config.thankYouEmailQueue).getQueueUrl

  def thank(row: ContributorRow): XorT[Future, Throwable, SendMessageResult] = {
    sendEmailToQueue(thankYouQueueUrl, row)
  }

  def sendEmailToQueue(queueUrl: String, row: ContributorRow): XorT[Future, Throwable, SendMessageResult] = {
    val payload = Json.stringify(Json.toJson(row))

    val handler = new AwsAsyncHandler[SendMessageRequest, SendMessageResult]
    sqsClient.sendMessageAsync(queueUrl, payload, handler)

    XorT(handler.future.map { result =>
      Xor.Right(result)
    } recover {
      case t: Throwable =>
        Logger.error(s"Unable to send message to the SQS queue $queueUrl", t)
        Xor.Left(t)
    })
  }
}
