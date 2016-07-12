name := """contributions-frontend"""

version := "1.0-SNAPSHOT"

maintainer := "Membership Contributions <membership.dev@theguardian.com>"

packageSummary := "Contributions Play APP"

packageDescription := """lorem ipsum donor sit amet"""

lazy val root = (project in file(".")).enablePlugins(PlayScala,BuildInfoPlugin, RiffRaffArtifact, UniversalPlugin).settings(
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
    buildInfoPackage := "app"
)

sources in (Compile,doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

play.sbt.routes.RoutesKeys.routesImport += "controllers.Binders._"
scalaVersion := "2.11.7"
val scalaUri = "com.netaporter" %% "scala-uri" % "0.4.6"
val scalaz = "org.scalaz" %% "scalaz-core" % "7.1.1"
val membershipCommon = "com.gu" %% "membership-common" % "0.225"
val sentryRavenLogback = "com.getsentry.raven" % "raven-logback" % "7.2.3"
val memsubCommonPlayAuth = "com.gu" %% "memsub-common-play-auth" % "0.7"
libraryDependencies ++= Seq(
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
    scalaUri,
    scalaz,
    membershipCommon,
    sentryRavenLogback,
    memsubCommonPlayAuth,
    "com.softwaremill.macwire" %% "macros" % "2.2.2" % "provided",
    "com.softwaremill.macwire" %% "util" % "2.2.2",
    "com.softwaremill.macwire" %% "proxy" % "2.2.2"
)
dependencyOverrides += "com.typesafe.play" %% "play-json" % "2.4.6"


resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

addCommandAlias("devrun", "run -Dconfig.resource=dev.conf 9111")

riffRaffPackageType := (packageZipTarball in Universal).value

def env(key: String): Option[String] = Option(System.getenv(key))
riffRaffBuildIdentifier := env("BUILD_NUMBER").getOrElse("DEV")
riffRaffManifestBranch := env("BRANCH_NAME").getOrElse("unknown_branch")
riffRaffManifestVcsUrl  := "git@github.com:guardian/contributions-frontend.git"
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
