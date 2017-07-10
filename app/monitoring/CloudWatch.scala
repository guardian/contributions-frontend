package monitoring

import java.util.concurrent.Future

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.regions.Regions.EU_WEST_1
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient
import com.amazonaws.services.cloudwatch.model.{Dimension, MetricDatum, PutMetricDataRequest, PutMetricDataResult}
import com.gu.aws.CredentialsProvider
import monitoring.CloudWatch.cloudwatch

trait CloudWatch extends TagAwareLogger {
  lazy val stageDimension: Dimension = new Dimension().withName("Stage").withValue(stage)
  val stage: String
  val application: String

  def put(name: String, count: Double, responseMethod: String): Unit = {
    put(name, count, new Dimension().withName("ResponseMethod").withValue(responseMethod))
  }

  def put(name: String, count: Double, extraDimensions: Dimension*): Future[PutMetricDataResult] = {
    val metric =
      new MetricDatum()
        .withValue(count)
        .withMetricName(name)
        .withUnit("Count")
        .withDimensions((mandatoryDimensions ++ extraDimensions): _*)

    val request = new PutMetricDataRequest().
      withNamespace(application).withMetricData(metric)

    cloudwatch.putMetricDataAsync(request, LoggingAsyncHandler)
  }

  def mandatoryDimensions: Seq[Dimension] = Seq(stageDimension)

  trait LoggingAsyncHandler extends AsyncHandler[PutMetricDataRequest, PutMetricDataResult] {
    def onError(exception: Exception): Unit = {
      logger.info(s"CloudWatch PutMetricDataRequest error: ${exception.getMessage}}")
    }

    def onSuccess(request: PutMetricDataRequest, result: PutMetricDataResult): Unit = {
      logger.trace("CloudWatch PutMetricDataRequest - success")
      CloudWatchHealth.hasPushedMetricSuccessfully = true
    }
  }

  object LoggingAsyncHandler extends LoggingAsyncHandler

}

object CloudWatch {

  lazy val cloudwatch = AmazonCloudWatchAsyncClient.asyncBuilder
    .withCredentials(CredentialsProvider)
    .withRegion(EU_WEST_1).build()

}


object CloudWatchHealth {
  var hasPushedMetricSuccessfully = false
}

