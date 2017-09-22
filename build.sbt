name := """contributions-frontend"""

version := "1.0-SNAPSHOT"

maintainer := "Membership Contributions <membership.dev@theguardian.com>"

packageSummary := "Contributions Play APP"

packageDescription := """lorem ipsum donor sit amet"""

def env(key: String, default: String): String = Option(System.getenv(key)).getOrElse(default)

def isAcceptanceTest(name: String): Boolean = name.contains("Acceptance")

lazy val AcceptanceTest = config("acceptance").extend(Test)

lazy val root = (project in file("."))
    .enablePlugins(PlayScala, BuildInfoPlugin, RiffRaffArtifact, JDebPackaging)
    .configs(AcceptanceTest)
    .settings(
        inConfig(AcceptanceTest)(Defaults.testTasks),

        buildInfoKeys := Seq[BuildInfoKey](
            name,
            BuildInfoKey.constant("buildNumber", env("BUILD_NUMBER", "DEV")),
            BuildInfoKey.constant("buildTime", System.currentTimeMillis),
            BuildInfoKey.constant("gitCommitId", Option(System.getenv("BUILD_VCS_NUMBER")) getOrElse(try {
                "git rev-parse HEAD".!!.trim
            } catch {
                case e: Exception => "unknown"
            }))),
        buildInfoPackage := "app",
        buildInfoOptions += BuildInfoOption.ToMap
    )

PlayKeys.playRunHooks += AssetsWatch(baseDirectory.value)

sources in (Compile,doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

play.sbt.routes.RoutesKeys.routesImport ++= Seq(
    "controllers.Binders._",
    "utils.ThriftUtils.Implicits._",
    "com.gu.i18n.CountryGroup",
    "controllers.PaymentError",
    "ophan.thrift.componentEvent.ComponentType",
    "ophan.thrift.event.AbTest",
    "ophan.thrift.event.AcquisitionSource"
)

scalaVersion := "2.11.8"

val scalaUri = "com.netaporter" %% "scala-uri" % "0.4.6"
val cats = "org.typelevel" %% "cats" % "0.8.1"
val membershipCommon = "com.gu" %% "membership-common" % "0.306"
val sentryRavenLogback = "com.getsentry.raven" % "raven-logback" % "8.0.3"
val memsubCommonPlayAuth = "com.gu" %% "memsub-common-play-auth" % "1.2"
val paypalSdk = "com.paypal.sdk" % "rest-api-sdk" % "1.13.0" exclude("org.apache.logging.log4j", "log4j-slf4j-impl")
val macwire = "com.softwaremill.macwire" %% "macros" % "2.2.2" % Provided
val anormLib = "com.typesafe.play" %% "anorm" % "2.5.2"
val postgresql = "org.postgresql" % "postgresql" % "9.4.1209"
val identityCookie =  "com.gu.identity" %% "identity-cookie" % "3.51"
val scalaTest = "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0"
val enumeratum = "com.beachape" %% "enumeratum" % "1.5.12"
val enumeratumJson = "com.beachape" %% "enumeratum-play-json" % "1.5.12"
val sqs = "com.amazonaws" % "aws-java-sdk-sqs" % "1.11.36"
val slugify = "com.github.slugify" % "slugify" % "2.1.7"
val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.13.4" % Test
val guava = "com.google.guava" % "guava" % "21.0"
val mockito = "org.mockito" % "mockito-all" % "1.9.5" % Test
val awsCloudwatch = "com.amazonaws" % "aws-java-sdk-cloudwatch" % "1.11.95"
val selenium = "org.seleniumhq.selenium" % "selenium-java" % "3.0.1" % Test
val seleniumManager = "io.github.bonigarcia" % "webdrivermanager" % "1.7.1" % Test
val seleniumHtmlUnitDriver = "org.seleniumhq.selenium" % "htmlunit-driver" % "2.23" % Test
val acquisitionEventProducer = "com.gu" %% "acquisition-event-producer" % "1.0.1"
val simulacrum = "com.github.mpilquist" %% "simulacrum" % "0.10.0"

// Used by simulacrum
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

libraryDependencies ++= Seq(
    cache,
    ws,
    filters,
    jdbc,
    scalaTest,
    scalaUri,
    cats,
    membershipCommon,
    sentryRavenLogback,
    memsubCommonPlayAuth,
    paypalSdk,
    macwire,
    anormLib,
    postgresql,
    identityCookie,
    enumeratum,
    enumeratumJson,
    sqs,
    slugify,
    scalaCheck,
    guava,
    awsCloudwatch,
    mockito,
    scalaTest,
    selenium,
    seleniumManager,
    seleniumHtmlUnitDriver,
    acquisitionEventProducer,
    simulacrum
)

dependencyOverrides += "com.typesafe.play" %% "play-json" % "2.4.6"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += "old-github-maven-repo" at "http://guardian.github.io/maven/repo-releases/"
resolvers += Resolver.bintrayRepo("guardian", "ophan")

addCommandAlias("devrun", "run 9112")

test in assembly := {} // skip tests during assembly


testOptions := Seq(Tests.Argument("-oD")) // display full stack traces on test error
testOptions in Test := Seq(Tests.Filter(name => !isAcceptanceTest(name)))
testOptions in AcceptanceTest := Seq(Tests.Filter(isAcceptanceTest))

javaOptions in Test += "-Dconfig.file=test/resources/application.conf"
javaOptions in AcceptanceTest += "-Dconfig.file=test/resources/application.conf"

import com.typesafe.sbt.packager.archetypes.ServerLoader.Systemd
serverLoading in Debian := Systemd

debianPackageDependencies := Seq("openjdk-8-jre-headless")
maintainer := "Alex Ware <alex.ware@guardian.co.uk>"
packageSummary := "contributions frontend"
packageDescription := """take financial contributions"""

riffRaffPackageType := (packageBin in Debian).value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cloud-formation/cfn.yaml"), "cfn/cfn.yaml")

javaOptions in Universal ++= Seq(
    "-Dpidfile.path=/dev/null",
    "-J-XX:MaxRAMFraction=2",
    "-J-XX:InitialRAMFraction=2",
    "-J-XX:MaxMetaspaceSize=500m",
    "-J-XX:+PrintGCDetails",
    "-J-XX:+PrintGCDateStamps",
    s"-J-Xloggc:/var/log/${packageName.value}/gc.log"
)
