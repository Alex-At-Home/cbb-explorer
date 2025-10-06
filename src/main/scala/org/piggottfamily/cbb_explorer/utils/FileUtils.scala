package org.piggottfamily.cbb_explorer.utils

import java.io.{BufferedWriter, FileWriter, IOException}
import java.nio.file.{Files, Path, Paths}
import scala.io.Source
import scala.collection.JavaConverters._
import scala.util.{Try, Success, Failure}
import java.util.stream.Collectors

/** File utils */
trait FileUtils {

  /** List a set of files with a given extension in */
  def list_files(
    root_dir: Path, extension: Option[String], time_filter: Option[Long => Boolean] = None, recursive: Boolean = false
  ): List[Path] = {
    if (!Files.exists(root_dir)) return List.empty
    
    val stream = if (recursive) {
      Files.walk(root_dir)
    } else {
      Files.list(root_dir)
    }
    
    try {
      import scala.collection.JavaConverters._
      stream.collect(Collectors.toList()).asScala.toList
        .filter(Files.isRegularFile(_))
        .filter { path =>
          extension match {
            case Some(ext) => path.toString.endsWith(s".$ext")
            case None => true
          }
        }
        .filter { path =>
          time_filter match {
            case Some(lambda) =>
              Try(Files.getLastModifiedTime(path).toMillis).map(lambda).getOrElse(true)
            case None => true
          }
        }
    } finally {
      stream.close()
    }
  }

  /** Reads a file into a string */
  def read_file(file: Path): String = {
    val source = Source.fromFile(file.toFile)
    try {
      source.mkString
    } finally {
      source.close()
    }
  }

  /** Writes a sequence of lines into the file */
  def write_lines_to_file(file: Path, lines: Traversable[String]): Unit = {
    Files.createDirectories(file.getParent)
    val writer = new BufferedWriter(new FileWriter(file.toFile))
    try {
      lines.foreach { line =>
        writer.write(line)
        writer.newLine()
      }
    } finally {
      writer.close()
    }
  }
  
  def read_lines_from_file(file: Path): Seq[String] = {
    val source = Source.fromFile(file.toFile)
    try {
      source.getLines().toSeq
    } finally {
      source.close()
    }
  }
  
  /** Write string content to a file */
  def write_file(file: Path, content: String): Unit = {
    Files.createDirectories(file.getParent)
    Files.write(file, content.getBytes)
  }
  
  /** List directories in a path */
  def list_dirs(root_dir: Path): List[Path] = {
    if (!Files.exists(root_dir)) return List.empty
    
    val stream = Files.list(root_dir)
    try {
      import scala.collection.JavaConverters._
      stream.collect(Collectors.toList()).asScala.toList
        .filter(Files.isDirectory(_))
    } finally {
      stream.close()
    }
  }
}
object FileUtils extends FileUtils