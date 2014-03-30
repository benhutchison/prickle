package prickle

import scala.util.Try

trait PBuilder[P] {
  def makeNull(): P
  def makeBoolean(b: Boolean): P
  def makeNumber(x: Double): P
  def makeString(s: String): P
  def makeArray(elems: P*): P
  def makeObject(fields: (String, P)*): P
}

trait PReader[P] {
  def isNull(x: P): Boolean
  def readBoolean(x: P): Try[Boolean]
  def readNumber(x: P): Try[Double]
  def readString(x: P): Try[String]
  def readArrayLength(x: P): Try[Int]
  def readArrayElem(x: P, index: Int): Try[P]
  def readObjectField(x: P, field: String): Try[P]

  /** Should provide some diagnostic string about the pickle 'x', eg its content.
    * Used to build error messages. */
  def context(x: P): String

  def readObjectFieldStr(x: P, field: String): Try[String] = readObjectField(x, field).flatMap(readString)

  def readObjectFieldNum(x: P, field: String): Try[Double] = readObjectField(x, field).flatMap(readNumber)
}

object PBuilder extends DefaultPickleFormat
object PReader extends DefaultPickleFormat
