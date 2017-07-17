package monitoring

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.regions.Regions.EU_WEST_1
import com.amazonaws.services.cloudwatch.model.{PutMetricDataRequest, PutMetricDataResult}
import com.amazonaws.services.cloudwatch.{AmazonCloudWatchAsync, AmazonCloudWatchAsyncClient}
import com.gu.aws.CredentialsProvider
import com.typesafe.scalalogging.LazyLogging


object CloudWatch {
  def build(): AmazonCloudWatchAsync = AmazonCloudWatchAsyncClient.asyncBuilder
    .withCredentials(CredentialsProvider)
    .withRegion(EU_WEST_1).build()

  object LoggingAsyncHandler extends AsyncHandler[PutMetricDataRequest, PutMetricDataResult] with LazyLogging {
    def onError(exception: Exception): Unit = {
      logger.info(s"CloudWatch PutMetricDataRequest error: ${exception.getMessage}}")
    }

    def onSuccess(request: PutMetricDataRequest, result: PutMetricDataResult): Unit = {
      logger.trace("CloudWatch PutMetricDataRequest - success")
    }
  }
}
