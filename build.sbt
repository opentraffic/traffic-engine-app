name := """traffic-engine-simulator"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

resolvers += (
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
  )

resolvers += "Geotools" at "http://download.osgeo.org/webdav/geotools/"

resolvers += "Conveyal" at "http://maven.conveyal.com/"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  javaWs,
  "org.opentripplanner" % "otp" % "0.19.0-SNAPSHOT",
  "com.conveyal" % "traffic-engine" % "0.1-SNAPSHOT",
  "org.geotools" % "gt-geojson" % "12.2",
  "com.amazonaws" % "aws-java-sdk" % "1.9.29"
)
