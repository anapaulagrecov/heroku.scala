import java.io.ByteArrayOutputStream
import sbt._
import Keys._
import com.typesafe.sbt.SbtScalariform._

object Build extends Build {

  val withManaged =
    List(
      sourceDirectories in ScalariformKeys.format := (sourceDirectories in ScalariformKeys.format).value ++ Seq(sourceManaged.value)
    )

  //inConfig(Compile)(withManaged)

  //inConfig(Test)(withManaged)

  val buildSettings = Seq(
    organization := "com.heroku.platform.api",
    version := "0.0.5-SNAPSHOT",
    scalaVersion := "2.10.4",
    crossScalaVersions := Seq("2.10.4", "2.11.1"),
    resolvers ++= Seq(
      "TypesafeMaven" at "http://repo.typesafe.com/typesafe/maven-releases",
      "whydoineedthis" at "http://repo.typesafe.com/typesafe/releases",
      "spray repo" at "http://repo.spray.io",
      "spray nightlies" at "http://nightlies.spray.io/",
      "sonatype" at "https://oss.sonatype.org/content/groups/public")
  ) ++
    Defaults.defaultSettings ++
    scalariformSettings ++
    publishSettings ++
    Seq( testOptions in Test += Tests.Argument("-oF"),  testOptions in IntegrationTest += Tests.Argument("-oF")) ++
    Seq( scalacOptions in Compile ++= Seq("-unchecked", "-deprecation", "-feature", "-language:postfixOps"))


  val modelBoilerplateGen = Project(
    id = "model-boilerplate-generator",
    base = file("boilerplate-generator/model"),
    settings = buildSettings ++ Seq(libraryDependencies ++= Seq(treehugger, playJson))
  )

  val api = Project(
    id = "api",
    base = file("api"),
    settings = buildSettings ++ Seq(libraryDependencies ++= apiDeps)
  ).settings(generateModelBoilerplate:_*).settings(inConfig(Compile)(withManaged):_*)


  val jsonBoilerplateGen = Project(
    id = "json-boilerplate-generator",
    base = file("boilerplate-generator/json"),
    settings = buildSettings ++ Seq(libraryDependencies ++= Seq(treehugger, sprayJson))
  ).dependsOn(api)

  val spray_client = Project(
    id = "spray-client",
    base = file("spray-client"),
    dependencies = Seq(api % "it->test;test->test;compile->compile"),
    settings = buildSettings ++ Seq(libraryDependencies ++= sprayDeps) ++ spray
  ).settings(Defaults.itSettings: _*).configs(IntegrationTest).settings(generateJsonBoilerplate:_*)

  val spray_jackson_example = Project(
    id = "spray-jackson-example",
    base = file("examples/spray-jackson"),
    dependencies = Seq(api % "it->test;test->test;compile->compile", spray_client % "it->it;test->test;compile->compile"),
    settings = buildSettings ++ Seq(libraryDependencies ++= sprayJacksonExampleDeps) ++ spray
  ).settings(Defaults.itSettings: _*).configs(IntegrationTest)

  val finagle_spray_example = Project(
    id = "finagle-spray-example",
    base = file("examples/finagle-spray"),
    dependencies = Seq(api % "it->test;test->test;compile->compile", spray_client % "it->it;test->test;compile->compile"),
    settings = buildSettings ++ Seq(libraryDependencies ++= finagleSprayExampleDeps)
  ).settings(Defaults.itSettings: _*).configs(IntegrationTest)


  lazy val jsonBoilerplate = TaskKey[Seq[File]]("json-boilerplate", "Generate Json Boilerplate")

  lazy val generateJsonBoilerplate:Seq[Project.Setting[_]] = Seq(
    sourceGenerators in Compile <+= (jsonBoilerplate in Compile).task,
    sourceManaged in Compile <<= baseDirectory / "src_managed/main/scala",
    jsonBoilerplate in Compile <<= (cacheDirectory, sourceManaged in Compile, dependencyClasspath in Runtime in jsonBoilerplateGen, compile in api in Compile, streams) map {
      (cacheDir, sm, cp, apiComp, st) =>
        val apiClasses = apiComp.relations.allProducts
        val cache =
          FileFunction.cached(cacheDir / "json-boilerplate-generator", inStyle = FilesInfo.hash, outStyle = FilesInfo.hash) {
            in: Set[File] =>
              genJsonBoilerplate(sm / "com/heroku/platform/api/client/spray/SprayJsonBoilerplate.scala", cp.files, "SprayJsonBoilerplateGen", st)/* ++
                genJsonBoilerplate(sm / "com/heroku/platform/api/client/spray/PlayJsonBoilerplate.scala", cp.files, "PlayJsonBoilerplateGen", st)*/
          }

       cache(apiClasses.toSet).toSeq
    }
  )

  lazy val modelBoilerplate = TaskKey[Seq[File]]("model-boilerplate", "Generate Model Boilerplate")

  lazy val generateModelBoilerplate:Seq[Project.Setting[_]] = Seq(
    sourceGenerators in Compile <+= (modelBoilerplate in Compile).task,
    sourceManaged in Compile <<= baseDirectory / "src_managed/main/scala",
    modelBoilerplate in Compile <<= (cacheDirectory, sourceManaged in Compile, dependencyClasspath in Runtime in modelBoilerplateGen, baseDirectory, streams) map {
      (cacheDir, sm, cp, base, st) =>
       val schema = Set(base / "src/main/resources/schema.json")
       val cache =
         FileFunction.cached(cacheDir / "model-boilerplate-generator", inStyle = FilesInfo.hash, outStyle = FilesInfo.hash) {
           in: Set[File] =>
              genModelboilerplate(sm / "com/heroku/platform/api", cp.files, "ModelBoilerplateGen", st)
         }

        cache(schema).toSeq
    }
  )


  def genModelboilerplate(source: File, cp: Seq[File], mainClass:String, streams:Types.Id[Keys.TaskStreams]): Set[File] = {
    streams.log.info("Generating:%s".format(source))
    val baos = new ByteArrayOutputStream()
    val i = new Fork.ForkScala(mainClass).fork(None, Nil, cp, Seq(source.getAbsolutePath), None, false, CustomOutput(baos)).exitValue()
    if (i != 0) {
      streams.log.error("Trouble with code generator")
    }
    val files = new String(baos.toByteArray).split('\n').map(new File(_))
    Set(files:_*)
  }

  def genJsonBoilerplate(source: File, cp: Seq[File], mainClass:String, streams:Types.Id[Keys.TaskStreams]): Set[File] = {
    streams.log.info("Generating:%s".format(source))
    val baos = new ByteArrayOutputStream()
    val i = new Fork.ForkScala(mainClass).fork(None, Nil, cp, Nil, None, false, CustomOutput(baos)).exitValue()
    if (i != 0) {
      streams.log.error("Trouble with code generator")
    }
    val code = new String(baos.toByteArray)
    IO delete source
    IO write(source, code)
    Set(source)
  }


  val root = Project(id = "heroku-scala-project", base = file("."), settings = buildSettings).aggregate(modelBoilerplateGen, api, jsonBoilerplateGen, spray_client, finagle_spray_example, spray_jackson_example)

  def apiDeps = Seq(scalaTest)

  def sprayDeps = Seq(sprayJson % "provided", akka, scalaTest, playJson % "provided")

  def sprayJacksonExampleDeps = Seq(jacksonScala, sprayJson)

  def finagleSprayExampleDeps = Seq(finagle, sprayJson)

  //val spray = "io.spray" % "spray-client" % "1.3.1" % "compile"

  def spray:Seq[Setting[Seq[ModuleID]]] = Seq(libraryDependencies <+= scalaVersion(sprayDependency(_)))

  def sprayDependency(scalaVersion: String) = scalaVersion match {
    case "2.10.4" => "io.spray" % "spray-client" % "1.3.1" % "compile"
    case "2.11.1" => "io.spray" % "spray-client_2.11" % "1.3.1-20140423" % "compile"
  }

  val sprayJson = "io.spray" %% "spray-json" % "1.2.6"
  val akka = "com.typesafe.akka" %% "akka-actor" % "2.3.4" % "compile"
  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.0" % "test"
  val treehugger = "com.eed3si9n" %% "treehugger" % "0.3.0"
  val playJson = "com.typesafe.play" %% "play-json" % "2.3.2"

  //deps for examples
  val jacksonScala = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.4.1"
  val finagle = "com.twitter" % "finagle-http_2.10" % "6.8.1"

  def publishSettings: Seq[Setting[_]] = Seq(
    // If we want on maven central, we need to be in maven style.
    publishMavenStyle := true,
    publishArtifact in Test := false,
    // The Nexus repo we're publishing to.
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
      else                             Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    // Maven central cannot allow other repos.  We're ok here because the artifacts we
    // we use externally are *optional* dependencies.
    pomIncludeRepository := { x => false },
    // Maven central wants some extra metadata to keep things 'clean'.
    pomExtra := (

      <url>http://github.com/heroku/heroku.scala</url>
        <licenses>
          <license>
            <name>The Apache Software License, Version 2.0</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <scm>
          <url>git@github.com:heroku/heroku.scala.git</url>
          <connection>scm:git:git@github.com:heroku/heroku.scala.git</connection>
        </scm>
        <developers>
          <developer>
            <id>sclasen</id>
            <name>Scott Clasen</name>
            <url>http://github.com/sclasen</url>
          </developer>
        </developers>)
  )



}
