
// The simplest possible sbt build file is just one line:

scalaVersion := "2.12.20"
// That is, to create a valid sbt build, all you've got to do is define the
// version of Scala you'd like your project to use.

// ============================================================================

// Lines like the above defining `scalaVersion` are called "settings" Settings
// are key/value pairs. In the case of `scalaVersion`, the key is "scalaVersion"
// and the value is "2.12.1"

// It's possible to define many kinds of settings, such as:

name := "cbb-explorer"
organization := "org.piggottfamily"
version := "0.1"

// Note, it's not required for you to define these three settings. These are
// mostly only necessary if you intend to publish your library's binaries on a
// place like Sonatype or Bintray.


// Want to use a published library in your project?
// You can define other libraries as dependencies in your build like this:
//libraryDependencies += "org.typelevel" %% "cats" % "0.9.0"
// Here, `libraryDependencies` is a set of dependencies, and by using `+=`,
// we're adding the cats dependency to the set of dependencies that sbt will go
// and fetch when it starts up.
// Now, in any Scala file, you can import classes, objects, etc, from cats with
// a regular import.

// TIP: To find the "dependency" that you need to add to the
// `libraryDependencies` set, which in the above example looks like this:

// "org.typelevel" %% "cats" % "0.9.0"

// You can use Scaladex, an index of all known published Scala libraries. There,
// after you find the library you want, you can just copy/paste the dependency
// information that you need into your build file. For example, on the
// typelevel/cats Scaladex page,
// https://index.scala-lang.org/typelevel/cats, you can copy/paste the sbt
// dependency from the sbt box on the right-hand side of the screen.

// IMPORTANT NOTE: while build files look _kind of_ like regular Scala, it's
// important to note that syntax in *.sbt files doesn't always behave like
// regular Scala. For example, notice in this build file that it's not required
// to put our settings into an enclosing object or class. Always remember that
// sbt is a bit different, semantically, than vanilla Scala.

// ============================================================================

// Most moderately interesting Scala projects don't make use of the very simple
// build file style (called "bare style") used in this build.sbt file. Most
// intermediate Scala projects make use of so-called "multi-project" builds. A
// multi-project build makes it possible to have different folders which sbt can
// be configured differently for. That is, you may wish to have different
// dependencies or different testing frameworks defined for different parts of
// your codebase. Multi-project builds make this possible.

// Here's a quick glimpse of what a multi-project build looks like for this
// build, with only one "subproject" defined, called `root`:

// lazy val root = (project in file(".")).
//   settings(
//     inThisBuild(List(
//       organization := "ch.epfl.scala",
//       scalaVersion := "2.12.1"
//     )),
//     name := "hello-world"
//   )

// To learn more about multi-project builds, head over to the official sbt
// documentation at http://www.scala-sbt.org/documentation.html

// DEBUG OPTIONS
//scalacOptions += "-verbose"
// scalacOptions += "-Ylog:all"
// scalacOptions += "-Ytyper-debug"

lazy val root = (project in file("."))

// To use cats...parMapN with Either:
scalacOptions += "-Ypartial-unification"

// Fix dependency conflicts
ThisBuild / evictionErrorLevel := Level.Warn
ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

val circeVersion = "0.14.9"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

// Lens support
libraryDependencies += "com.softwaremill.quicklens" %% "quicklens" % "1.6.1"

val kantanVersion = "0.6.2"

// Core library, included automatically if any other module is imported.
libraryDependencies += "com.nrinaudo" %% "kantan.csv" % kantanVersion

// Java 8 date and time instances.
libraryDependencies += "com.nrinaudo" %% "kantan.csv-java8" % kantanVersion

// Provides scalaz type class instances for kantan.csv, and vice versa.
libraryDependencies += "com.nrinaudo" %% "kantan.csv-scalaz" % kantanVersion

// Provides cats type class instances for kantan.csv, and vice versa.
libraryDependencies += "com.nrinaudo" %% "kantan.csv-cats" % kantanVersion

// Automatic type class instances derivation.
libraryDependencies += "com.nrinaudo" %% "kantan.csv-generic" % kantanVersion

// Provides instances for joda time types.
// libraryDependencies += "com.nrinaudo" %% "kantan.csv-joda-time" % kantanVersion

// Provides instances for refined types.
libraryDependencies += "com.nrinaudo" %% "kantan.csv-refined" % kantanVersion

// Provides instances for enumeratum types.
libraryDependencies += "com.nrinaudo" %% "kantan.csv-enumeratum" % kantanVersion

// Provides instances for libra types.
libraryDependencies += "com.nrinaudo" %% "kantan.csv-libra" % kantanVersion

libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "3.1.1"

val utestVersion = "0.8.4"

libraryDependencies += "com.lihaoyi" %% "utest" % utestVersion % "test"

testFrameworks += new TestFramework("utest.runner.Framework")

val ammoniteVersion = "2.5.4"

// libraryDependencies += "com.lihaoyi" %% "ammonite-ops" % ammoniteVersion

// libraryDependencies += "com.lihaoyi" % "ammonite" % ammoniteVersion % "test" cross CrossVersion.full

val nameOfVersion = "4.0.0"

libraryDependencies += "com.github.dwickern" %% "scala-nameof" % nameOfVersion % "provided"

libraryDependencies += "me.xdrop" % "fuzzywuzzy" % "1.4.0"

// Assembly for shell:
// sbt assembly
// java -jar ,/target/scala-2.12/cbb-explorer.jar

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

assembly / assemblyJarName := "cbb-explorer.jar"
assembly / mainClass := Some("org.piggottfamily.cbb_explorer.Main")

// Or for faster code-compile cycles (can't get 'sbt run' to work):
// sbt assemblyPackageDependency
// (note need to restart sbt or refresh if changing this file)
// (edit code as long as no new libs are imported)
// sbt compile package
// java -cp './target/scala-2.12/cbb-explorer-assembly-0.1-deps.jar:target/scala-2.12/cbb-explorer_2.12-0.1.jar' org.piggottfamily.cbb_explorer.Main

libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.32.0"
