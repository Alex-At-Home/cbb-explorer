package org.piggottfamily.cbb_explorer.utils

import ammonite.ops._

/** File utils */
trait FileUtils {

  /** List a set of files with a given exension in */
  def list_files(root_dir: Path, extension: Option[String]): List[Path] = {
    (extension match {
      case Some(ext) => ls! root_dir |? (_.ext == ext)
      case None => ls! root_dir
    }).toList
  }

  /** Reads a file into a string */
  def read_file(file: Path): String = {
    read! file
  }

  /** Writes a sequence of lines into the file */
  def write_lines_to_file(file: Path, lines: Traversable[String]): Unit = {
    write.over(file, lines.map(_ + "\n"))
  }
  def read_lines_from_file(file: Path): Seq[String] = {
    read.lines(file)
  }
}
object FileUtils extends FileUtils
