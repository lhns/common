lazy val scalaVersions = Seq("3.3.4")

ThisBuild / scalaVersion := scalaVersions.head
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / organization := "de.lhns"
name := (app.projectRefs.head / name).value

val V = new {
  val catsEffect = "3.5.7"
  val catsTagless = "0.16.2"
  val dottyCpsAsync = "0.9.23"
  val dumbo = "0.5.3"
  val fs2 = "3.11.0"
  val http4s = "0.23.30"
  val http4sDom = "0.2.11"
  val http4sJdkHttpClient = "0.10.0"
  val http4sOtel4s = "0.10.0"
  val julToSlf4j = "2.0.16"
  val log4Cats = "2.7.0"
  val logbackClassic = "1.5.16"
  val munitCatsEffect = "2.0.0"
  val otel4s = "0.11.2"
  val otelAutoconfigure = "1.46.0"
  val otel4sExperimental = "0.5.0"
  val otelIncubator = "1.45.0-alpha"
  val otelLogback = "2.11.0-alpha"
  val otelOtlp = "1.46.0"
  val otelRuntime = "2.11.0-alpha"
  val proxyVole = "1.1.6"
  val scalaJavaTime = "2.6.0"
  val scalajsJavaSecurerandom = "1.0.0"
  val skunk = "1.1.0-M3"
  val sttpShared = "1.4.2"
  val tapir = "1.11.13"
  val trustmanagerUtils = "1.1.0"
}

lazy val commonSettings: SettingsDefinition = Def.settings(
  version := {
    val Tag = "refs/tags/v?([0-9]+(?:\\.[0-9]+)+(?:[+-].*)?)".r
    sys.env.get("CI_VERSION").collect { case Tag(tag) => tag }
      .getOrElse("0.0.1-SNAPSHOT")
  },

  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0")),

  homepage := scmInfo.value.map(_.browseUrl),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/lhns/scala-common"),
      "scm:git@github.com:lhns/scala-common.git"
    )
  ),
  developers := List(
    Developer(id = "lhns", name = "Pierre Kisters", email = "pierrekisters@gmail.com", url = url("https://github.com/lhns/"))
  ),

  libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % V.logbackClassic % Test,
    "org.typelevel" %%% "munit-cats-effect" % V.munitCatsEffect % Test,
  ),

  testFrameworks += new TestFramework("munit.Framework"),

  scalacOptions ++= Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8", // Specify character encoding used by source files.
    "-explaintypes", // Explain type errors in more detail.
    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros", // Allow macro definition (besides implementation and application)
    "-language:higherKinds", // Allow higher-kinded types
    "-language:implicitConversions", // Allow definition of implicit functions called views
    "-unchecked", // Enable additional warnings where generated code depends on assumptions.
    "-Werror", // Fail the compilation if there are any warnings.
    "-Wconf:msg=Given search preference for:i",
    //"-Wshadow:private-shadow", // A private field (or class parameter) shadows a superclass field.
    //"-Wshadow:type-parameter-shadow", // A local type parameter shadows a type already in scope.
    "-Wnonunit-statement",
    "-Ykind-projector:underscores"
  ),

  Test / scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),

  Compile / doc / sources := Seq.empty,

  publishMavenStyle := true,

  publishTo := sonatypePublishToBundle.value,

  sonatypeCredentialHost := "s01.oss.sonatype.org",

  credentials ++= (for {
    username <- sys.env.get("SONATYPE_USERNAME")
    password <- sys.env.get("SONATYPE_PASSWORD")
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    sonatypeCredentialHost.value,
    username,
    password
  )).toList,
)

lazy val root: Project = project.in(file("."))
  .settings(commonSettings)
  .settings(
    publishArtifact := false,
    publish / skip := true
  )
  .aggregate(core.projectRefs: _*)
  .aggregate(app.projectRefs: _*)
  .aggregate(http.projectRefs: _*)
  .aggregate(httpClient.projectRefs: _*)
  .aggregate(httpServer.projectRefs: _*)
  .aggregate(skunk.projectRefs: _*)

lazy val core = projectMatrix.in(file("modules/core"))
  .settings(commonSettings)
  .settings(
    name := "scala-common-core",
    libraryDependencies ++= Seq(
      "co.fs2" %%% "fs2-io" % V.fs2,
      "com.github.rssh" %%% "cps-async-connect-cats-effect" % V.dottyCpsAsync,
      "org.typelevel" %%% "otel4s-experimental-trace" % V.otel4sExperimental,
      "org.typelevel" %%% "cats-effect" % V.catsEffect,
      "org.typelevel" %%% "cats-tagless-core" % V.catsTagless,
      "org.typelevel" %%% "log4cats-core" % V.log4Cats,
      "org.typelevel" %%% "otel4s-core" % V.otel4s,
    ),
  )
  .jvmPlatform(scalaVersions)
  .jsPlatform(scalaVersions, Seq(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % V.scalaJavaTime,
      "org.scala-js" %%% "scalajs-java-securerandom" % V.scalajsJavaSecurerandom cross CrossVersion.for3Use2_13
    )
  ))

lazy val app = projectMatrix.in(file("modules/app"))
  .dependsOn(core % "compile->compile;test->test")
  .settings(commonSettings)
  .settings(
    name := "scala-common-app"
  )
  .jvmPlatform(scalaVersions, Seq(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % V.logbackClassic,
      "de.lhns" %% "scala-trustmanager-utils" % V.trustmanagerUtils,
      "io.opentelemetry" % "opentelemetry-exporter-otlp" % V.otelOtlp,
      "io.opentelemetry" % "opentelemetry-sdk-extension-autoconfigure" % V.otelAutoconfigure,
      "io.opentelemetry" % "opentelemetry-sdk-extension-incubator" % V.otelIncubator,
      "io.opentelemetry.instrumentation" % "opentelemetry-logback-appender-1.0" % V.otelLogback,
      "io.opentelemetry.instrumentation" % "opentelemetry-runtime-telemetry-java17" % V.otelRuntime,
      "org.bidib.com.github.markusbernhardt" % "proxy-vole" % V.proxyVole,
      "org.slf4j" % "jul-to-slf4j" % V.julToSlf4j,
      "org.typelevel" %% "log4cats-slf4j" % V.log4Cats,
      "org.typelevel" %% "otel4s-experimental-metrics" % V.otel4sExperimental,
      "org.typelevel" %% "otel4s-oteljava" % V.otel4s
    )
  ))
  .jsPlatform(scalaVersions, Seq(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time-tzdb" % V.scalaJavaTime,
      "org.typelevel" %%% "log4cats-js-console" % V.log4Cats
    )
  ))

lazy val http = projectMatrix.in(file("modules/http"))
  .dependsOn(core % "compile->compile;test->test")
  .settings(commonSettings)
  .settings(
    name := "scala-common-http",
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-otel4s-middleware-metrics" % V.http4sOtel4s,
      "org.http4s" %%% "http4s-otel4s-middleware-trace-core" % V.http4sOtel4s,
      "com.softwaremill.sttp.tapir" %%% "tapir-core" % V.tapir,
      "com.softwaremill.sttp.shared" %%% "fs2" % V.sttpShared,
    ),
  )
  .jvmPlatform(scalaVersions)
  .jsPlatform(scalaVersions)

lazy val httpClient = projectMatrix.in(file("modules/http-client"))
  .dependsOn(http % "compile->compile;test->test")
  .settings(commonSettings)
  .settings(
    name := "scala-common-http-client",
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-client" % V.http4s,
      "org.http4s" %%% "http4s-otel4s-middleware-trace-client" % V.http4sOtel4s,
      "com.softwaremill.sttp.tapir" %%% "tapir-http4s-client" % V.tapir,
    ),
  )
  .jvmPlatform(scalaVersions, Seq(
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-jdk-http-client" % V.http4sJdkHttpClient,
    )
  ))
/*.jsPlatform(scalaVersions, Seq(
  libraryDependencies ++= Seq(
    "org.http4s" %%% "http4s-dom" % V.http4sDom
  )
))*/

lazy val httpServer = projectMatrix.in(file("modules/http-server"))
  .dependsOn(http % "compile->compile;test->test")
  .settings(commonSettings)
  .settings(
    name := "scala-common-http-server",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-cats" % V.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % V.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % V.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % V.tapir,
      "org.http4s" %% "http4s-ember-server" % V.http4s,
      "org.http4s" %%% "http4s-otel4s-middleware-trace-server" % V.http4sOtel4s,
    ),
  )
  .jvmPlatform(scalaVersions)

lazy val skunk = projectMatrix.in(file("modules/skunk"))
  .dependsOn(core % "compile->compile;test->test")
  .settings(commonSettings)
  .settings(
    name := "scala-common-skunk",
    libraryDependencySchemes += "org.typelevel" %% "otel4s-core-trace" % VersionScheme.Always,
    libraryDependencies ++= Seq(
      "dev.rolang" %% "dumbo" % V.dumbo,
      "org.tpolecat" %% "skunk-core" % V.skunk,
    ),
  )
  .jvmPlatform(scalaVersions)
