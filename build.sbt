import scalariform.formatter.preferences._

sbtPlugin := true

name := "sbt-spring-loaded"

version := "0.2.0-SNAPSHOT"

organization := "me.browder"

libraryDependencies += "org.springframework" % "springloaded" % "1.2.0.RELEASE"

crossScalaVersions := Seq("2.10.0", "2.10.1", "2.10.2", "2.10.3", "2.10.4").reverse

// scripted stuff (testing)

scriptedSettings

scriptedLaunchOpts += "-Dplugin.version=" + version.value

scriptedBufferLog := false

// scalariform (code reformatting)

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignParameters, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
  
  
// Publishing

licenses := Seq("GPLv3" -> url("http://www.gnu.org/licenses/gpl-3.0.txt"))

homepage := Some(url("http://github.com/kbrowder/sbt-spring-loaded"))

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <scm>
    <url>git@github.com:kbrowder/sbt-spring-loaded.git</url>
    <connection>scm:git:git@github.com:kbrowder/sbt-spring-loaded.git</connection>
  </scm>
  <developers>
    <developer>
      <id>kbrowder</id>
      <name>Kevin Browder</name>
    </developer>
  </developers>)
