package services

import cats.data.{Xor, XorT}
import com.amazonaws.auth._
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.typesafe.scalalogging.LazyLogging
import com.amazonaws.regions.{Region, Regions}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model._
import configuration.Config
import data.ContributionData
import model.exactTarget.ContributorRow
import play.api.libs.json._

import scala.concurrent.Future
import scala.util.Try

object EmailService extends LazyLogging {
  def thank(row: ContributorRow): XorT[Future, Throwable, SendMessageResult] = {
    val queue = Config.emailSQSQueue
    sendEmailToQueue(queue, row) //Todo: log if failed

  }

  val credentialsProviderChain: AWSCredentialsProviderChain =
    new AWSCredentialsProviderChain(
      new EnvironmentVariableCredentialsProvider,
      new SystemPropertiesCredentialsProvider,
      new ProfileCredentialsProvider("membership"),
      new InstanceProfileCredentialsProvider
    )
  private val sqsClient = new AmazonSQSClient(credentialsProviderChain)
  sqsClient.setRegion(Region.getRegion(Regions.EU_WEST_1))


  def sendEmailToQueue(queueName: String, row: ContributorRow) :XorT[Future,Throwable,SendMessageResult] = {
    Future {
      val payload = Json.toJson(row).toString
      def send(msg: String) = {
        val queueUrl = sqsClient.createQueue(new CreateQueueRequest(queueName)).getQueueUrl
        sqsClient.sendMessage(new SendMessageRequest(queueUrl, msg))
      }
      Xor.catchNonFatal(
        send(payload)
      )
    }
  }
}
