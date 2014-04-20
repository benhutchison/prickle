package prickle

import scala.util.Try
import collection.mutable

object PConfig extends DefaultPickleFormat {

//  object AcyclicConfig extends AcyclicPConfig[PFormat] with HashCharPrefix[PFormat]
//    with SimplePBuilder with SimplePReader
}

trait PConfig[P] extends PReader[P] with PBuilder[P] {

  def prefix: String

  def isCyclesSupported: Boolean

}
trait HashCharPrefix[P] extends PConfig[P] {

  def prefix = "#"
}
trait CyclicPConfig[P] extends PConfig[P] {
  def isCyclesSupported = true
}
trait AcyclicPConfig[P] extends PConfig[P] {
  def isCyclesSupported = false
}



trait PBuilder[P] {

  def makeNull(): P
  def makeBoolean(b: Boolean): P
  def makeNumber(x: Double): P
  def makeString(s: String): P
  def makeArray(elems: P*): P
  def makeObject(fields: Seq[(String, P)]): P

  def makeObject(k: String, v: P): P = makeObject(Seq((k, v)))
  def makeObjectFrom(fields: (String, P)*): P = makeObject(fields)
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
