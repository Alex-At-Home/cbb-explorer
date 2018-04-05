package org.piggottfamily.cbb_explorer.utils

/** Logging utils */
trait LogUtils {
  /** Log a message with INFO priority */
  def info(s: String): Unit = println(s"[INFO] $s")
}
object LogUtils extends LogUtils
