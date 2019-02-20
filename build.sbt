import sbtassembly.AssemblyPlugin.defaultShellScript

name := "hl7-downstream"
organization in ThisBuild := "com.eztier"
scalaVersion in ThisBuild := "2.12.4"

lazy val compilerOptions = Seq(
  "-unchecked",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-deprecation",
  "-encoding",
  "utf8",
  "-Ylog-classpath",
  "-Ypartial-unification"
)

lazy val global = project
  .in(file("."))
  .settings(settings)
  .aggregate(
    common,
    datasource
  )
  
lazy val commonSettings = Seq(
  version := "0.1.24",
  organization := "com.eztier",
  scalaVersion := "2.12.4",
  scalacOptions ++= compilerOptions,
  resolvers ++= Seq(
    Resolver.sonatypeRepo("public"),
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots"),
    Resolver.bintrayRepo("hseeberger", "maven")
  )
)

lazy val settings = commonSettings

lazy val common = project
  .settings(
    name := "common",
    settings,
    libraryDependencies ++= Seq(
      scalaTest,
      logback,
      jodaTime,
      akkaStream,
      akkaSlf4j,
      akkaStreamTestkit
    )
  )

// Common
val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5" % Test
val jodaTime = "joda-time" % "joda-time" % "2.9.9"

// scalaxb
val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
val scalaParser = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6"
val dispatchV = "0.12.3"
val dispatch = "net.databinder.dispatch" %% "dispatch-core" % dispatchV

// scalikejdbc
val scalikejdbc = "org.scalikejdbc" %% "scalikejdbc" % "3.2.2"
val scalikeStreams = "org.scalikejdbc" %% "scalikejdbc-streams" % "3.2.2"
val scalikeConfig = "org.scalikejdbc" %% "scalikejdbc-config"  % "3.2.2"
val scalikeMacro = "org.scalikejdbc" %% "scalikejdbc-syntax-support-macro" % "3.2.2"

// sqljdbc
val sqljdbc = "com.microsoft.sqlserver" % "mssql-jdbc" % "7.2.0.jre8"

// akka
val akka = "com.typesafe.akka"
val akkaHttpV = "10.1.5"

val akkaStream = akka %% "akka-stream" % "2.5.18"
val akkaSlf4j = akka %% "akka-slf4j" % "2.5.18"
val akkaStreamTestkit = akka %% "akka-stream-testkit" % "2.5.18" % Test

// HTTP server
val akkaHttp = akka %% "akka-http" % akkaHttpV
val akkaHttpCore = akka %% "akka-http-core" % akkaHttpV
val akkaHttpSprayJson = akka %% "akka-http-spray-json" % akkaHttpV
val akkaHttpTestkit = akka %% "akka-http-testkit" % akkaHttpV % Test

// akka-http circe
val akkaHttpCirce = "de.heikoseeberger" %% "akka-http-circe" % "1.22.0"

// Support of CORS requests, version depends on akka-http
// val akkaHttpCors = "ch.megard" %% "akka-http-cors" % "0.3.0"

// PostgreSQL
val doobie = "org.tpolecat" %% "doobie-core"      % "0.6.0"
val doobieH2 = "org.tpolecat" %% "doobie-h2"        % "0.6.0"          // H2 driver 1.4.197 + type mappings.
val doobieHikari = "org.tpolecat" %% "doobie-hikari"    % "0.6.0"          // HikariCP transactor.
val doobiePostgres = "org.tpolecat" %% "doobie-postgres"  % "0.6.0"          // Postgres driver 42.2.5 + type mappings.
val doobiePostgresCirce = "org.tpolecat" %% "doobie-postgres-circe"  % "0.6.0"          // Postgres driver 42.2.5 + type mappings.
val doobieSpecs2 = "org.tpolecat" %% "doobie-specs2"    % "0.6.0" % "test" // Specs2 support for typechecking statements.
val doobieScalaTest = "org.tpolecat" %% "doobie-scalatest" % "0.6.0" % "test"  // ScalaTest support for typechecking statements.

// circe
val circeGenericExtras = "io.circe" %% "circe-generic-extras" % "0.10.0"
val circeJava8 = "io.circe" %% "circe-java8" % "0.11.1"

// Cassandra
val cassandraUdt = "com.eztier" %% "cassandra-udt-codec-helper-scala" % "0.2.20"
val hl7cassandraAdapter =  "com.eztier" %% "hl7-cassandra-adapter-scala" % "0.3.11"

// MongoDB
val alpakkaMongoDB = "com.lightbend.akka" %% "akka-stream-alpakka-mongodb" % "1.0-M2"

lazy val datasource = project.
  settings(
    name := "datasource",
    settings,
    assemblySettings,
    libraryDependencies ++= Seq(
      doobiePostgres,
      doobiePostgresCirce,
      doobieScalaTest,
      circeGenericExtras,
      circeJava8,
      cassandraUdt,
      hl7cassandraAdapter,
      sqljdbc,
      alpakkaMongoDB
    )
  ).dependsOn(
    common
  )

// Custom app
lazy val app = project.
  settings(
    name := "app",
    settings,
    assemblySettings,
    /*
    Seq(
      assemblyJarName in assembly := s"${name.value}-${version.value}",
      assemblyOption in assembly := (assemblyOption in assembly).value.copy(prependShellScript = Some(defaultShellScript)),
    ),
    */
    Seq(
      javaOptions ++= Seq(
        // "-Dlogback.configurationFile=./logback.xml",
        "-Xmx1G"
      )
    ),
    libraryDependencies ++= Seq(
      scalaTest
    )
  ).dependsOn(
    common,
    datasource,
    http
  )

lazy val http = project.
  settings(
    name := "http",
    settings,
    assemblySettings,
    Seq(
      javaOptions ++= Seq(
        // "-Dlogback.configurationFile=./logback.xml",
        "-Xms1G",
        "-Xmx3G"
      )
    ),
    libraryDependencies ++= Seq(
      akkaHttp,
      akkaHttpCore,
      akkaHttpCirce,
      akkaHttpSprayJson,
      akkaHttpTestkit
    )
  ).dependsOn(
    common,
    datasource
  )

lazy val integration = project.
  settings(
    name := "integration",
    settings,
    assemblySettings,
    Seq(
      javaOptions ++= Seq(
        // "-Dlogback.configurationFile=./logback.xml",
        "-Xms1G",
        "-Xmx3G"
      )
    ),
    libraryDependencies ++= Seq(
      scalaTest
    )
  ).dependsOn(
    common,
    datasource
  )

// Skip tests for assembly  
lazy val assemblySettings = Seq(
  assemblyJarName in assembly := s"${name.value}-${version.value}.jar",
  
  assemblyMergeStrategy in assembly := {
    case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
    case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.first
    case "application.conf"                            => MergeStrategy.concat
    case "logback.xml"                            => MergeStrategy.first
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  test in assembly := {}
)
