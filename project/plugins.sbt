
// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.9")

// web plugins


addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.0")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.2")

addSbtPlugin("com.gu" % "sbt-riffraff-artifact" % "0.9.7")

// dependency tracker plugin - needed for snyk cli integration
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")
