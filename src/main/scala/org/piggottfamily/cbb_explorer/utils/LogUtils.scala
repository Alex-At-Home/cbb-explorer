package org.piggottfamily.cbb_explorer.utils

/** Logging utils */
trait LogUtils {
  /** Log a message with INFO priority */
  def info(s: String): Unit = println(s"[INFO] $s")
  def warn(s: String): Unit = println(s"[WARN] $s")
  def error(s: String): Unit = println(s"[ERROR] $s")
}
object LogUtils extends LogUtils
