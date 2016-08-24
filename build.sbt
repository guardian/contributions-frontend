name := """contributions-frontend"""

version := "1.0-SNAPSHOT"

maintainer := "Membership Contributions <membership.dev@theguardian.com>"

packageSummary := "Contributions Play APP"

packageDescription := """lorem ipsum donor sit amet"""

lazy val root = (project in file(".")).enablePlugins(PlayScala,BuildInfoPlugin, RiffRaffArtifact, JDebPackaging).settings(
    buildInfoKeys := Seq[BuildInfoKey](
        name,
        BuildInfoKey.constant("buildNumber", Option(System.getenv("BUILD_NUMBER")) getOrElse "DEV"),
        BuildInfoKey.constant("buildTime", System.currentTimeMillis),
        BuildInfoKey.constant("gitCommitId", Option(System.getenv("BUILD_VCS_NUMBER")) getOrElse(try {
            "git rev-parse HEAD".!!.trim
        } catch {
            case e: Exception => "unknown"
        }))
    ),
    buildInfoPackage := "app",
    buildInfoOptions += BuildInfoOption.ToMap
)

sources in (Compile,doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

play.sbt.routes.RoutesKeys.routesImport ++= Seq("controllers.Binders._", "com.gu.i18n.CountryGroup", "controllers.PaymentError")
scalaVersion := "2.11.7"
val scalaUri = "com.netaporter" %% "scala-uri" % "0.4.6"
val scalaz = "org.scalaz" %% "scalaz-core" % "7.1.1"
val membershipCommon = "com.gu" %% "membership-common" % "0.225"
val sentryRavenLogback = "com.getsentry.raven" % "raven-logback" % "7.6.0"
val memsubCommonPlayAuth = "com.gu" %% "memsub-common-play-auth" % "1.0"
val paypalSdk = "com.paypal.sdk" % "rest-api-sdk" % "1.9.2" exclude("org.apache.logging.log4j", "log4j-slf4j-impl")
libraryDependencies ++= Seq(
  cache,
  ws,
  filters,
  jdbc,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
    scalaUri,
    scalaz,
    membershipCommon,
    sentryRavenLogback,
    memsubCommonPlayAuth,
    paypalSdk,
    "com.softwaremill.macwire" %% "macros" % "2.2.2" % "provided",
    "com.softwaremill.macwire" %% "util" % "2.2.2",
    "com.softwaremill.macwire" %% "proxy" % "2.2.2",
    "com.typesafe.play" %% "anorm" % "2.5.2",
    "org.postgresql" % "postgresql" % "9.4.1209",
    "com.gu.identity" %% "identity-cookie" % "3.51"
)
dependencyOverrides += "com.typesafe.play" %% "play-json" % "2.4.6"


resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

addCommandAlias("devrun", "run 9111")

import com.typesafe.sbt.packager.archetypes.ServerLoader.Systemd
serverLoading in Debian := Systemd

debianPackageDependencies := Seq("openjdk-8-jre-headless")
maintainer := "Alex Ware <alex.ware@guardian.co.uk>"
packageSummary := "contributions frontend"
packageDescription := """take financial contributions"""

riffRaffPackageType := (packageBin in Debian).value
def env(key: String): Option[String] = Option(System.getenv(key))
riffRaffBuildIdentifier := env("BUILD_NUMBER").getOrElse("DEV")
riffRaffManifestBranch := env("BRANCH_NAME").getOrElse("unknown_branch")
riffRaffManifestVcsUrl  := "git@github.com:guardian/contributions-frontend.git"
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")

javaOptions in Universal ++= Seq(
    "-Dpidfile.path=/dev/null",
    "-J-XX:MaxRAMFraction=2",
    "-J-XX:InitialRAMFraction=2",
    "-J-XX:MaxMetaspaceSize=500m",
    "-J-XX:+PrintGCDetails",
    "-J-XX:+PrintGCDateStamps",
    s"-J-Xloggc:/var/log/${packageName.value}/gc.log"
)
