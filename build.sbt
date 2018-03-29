// The simplest possible sbt build file is just one line:

scalaVersion := "2.12.3"
// That is, to create a valid sbt build, all you've got to do is define the
// version of Scala you'd like your project to use.

// ============================================================================

// Lines like the above defining `scalaVersion` are called "settings" Settings
// are key/value pairs. In the case of `scalaVersion`, the key is "scalaVersion"
// and the value is "2.12.1"

// It's possible to define many kinds of settings, such as:

name := "cbb-expolorer"
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

lazy val root = (project in file("."))

// To use cats...parMapN with Either:
scalacOptions += "-Ypartial-unification"

val circeVersion = "0.9.1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

val kantanVersion = "0.4.0"

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
libraryDependencies += "com.nrinaudo" %% "kantan.csv-joda-time" % kantanVersion

// Provides instances for refined types.
libraryDependencies += "com.nrinaudo" %% "kantan.csv-refined" % kantanVersion

// Provides instances for enumeratum types.
libraryDependencies += "com.nrinaudo" %% "kantan.csv-enumeratum" % kantanVersion

// Provides instances for libra types.
libraryDependencies += "com.nrinaudo" %% "kantan.csv-libra" % kantanVersion

libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "2.1.0"

val utestVersion = "0.6.3"

libraryDependencies += "com.lihaoyi" %% "utest" % utestVersion % "test"

testFrameworks += new TestFramework("utest.runner.Framework")
