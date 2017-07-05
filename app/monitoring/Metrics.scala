package monitoring

import configuration.Config
import com.amazonaws.regions.{Regions, Region}

  trait Metrics extends CloudWatch {
    val stage: String = Config.stage
    val application = "contributions" // This sets the namespace for Custom Metrics in AWS (see CloudWatch)
  }


trait ApplicationMetrics extends CloudWatch {
  val region: Region = Region.getRegion(Regions.EU_WEST_1)
  val application: String
  val stage: String
}


trait StatusMetrics extends CloudWatch {
  def putResponseCode(status: Int, responseMethod: String) {
    val statusClass = status / 100
    put(s"${statusClass}XX-response-code", 1, responseMethod)
  }
}

/*trait RequestMetrics extends CloudWatch {
  def putRequest {
    put("request-count", 1)
  }
}

trait AuthenticationMetrics extends CloudWatch {
  def putAuthenticationError {
    put("auth-error", 1)
  }
}*/
