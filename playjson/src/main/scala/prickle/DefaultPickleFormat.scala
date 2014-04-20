package prickle

import play.api.libs.json._
import scala.util.{Failure, Success, Try}

trait DefaultPickleFormat {

  implicit object DefaultConfig extends CyclicPConfig[JsValue] with HashCharPrefix[JsValue]
    with PlayJsonPBuilder with PlayJsonPReader

  object AcyclicConfig extends AcyclicPConfig[JsValue] with HashCharPrefix[JsValue]
    with PlayJsonPBuilder with PlayJsonPReader

}

trait PlayJsonPBuilder extends PBuilder[JsValue] {
  def makeNull(): JsValue = JsNull
  def makeBoolean(b: Boolean): JsValue = JsBoolean(b)
  def makeNumber(x: Double): JsValue = JsNumber(x)
  def makeString(s: String): JsValue = JsString(s)
  def makeArray(elems: JsValue*): JsValue = JsArray(elems)
  def makeObject(fields: Seq[(String, JsValue)]): JsValue = JsObject(fields)
}

trait PlayJsonPReader extends PReader[JsValue] {
  def isNull(x: JsValue): Boolean = x == JsNull
  def readBoolean(x: JsValue): Try[Boolean] = x match {
    case JsBoolean(b) => Success(b)
    case other => Failure(new RuntimeException(s"Expected: boolean  Actual: ${context(other)}"))
  }
  def readNumber(x: JsValue): Try[Double] = x match {
    case JsNumber(bigDecimal) => Success(bigDecimal.toDouble)
    case other => Failure(new RuntimeException(s"Expected: Double  Actual: ${context(other)}"))
  }
  def readString(x: JsValue): Try[String] = x match {
    case JsString(s) => Success(s)
    case other => Failure(new RuntimeException(s"Expected: String  Actual: ${context(other)}"))
  }
  def readArrayLength(x: JsValue): Try[Int] = x match {
    case JsArray(elems) => Success(elems.size)
    case other => Failure(new RuntimeException(s"Expected: array length  Actual: ${context(other)}"))
  }
  def readArrayElem(x: JsValue, index: Int): Try[JsValue] = x match {
    case JsArray(elems) if index < elems.size => Success(elems(index))
    case other => Failure(new RuntimeException(s"Expected: array($index)  Actual: ${context(other)}"))
  }
  def readObjectField(x: JsValue, field: String): Try[JsValue] = x match {
    case o: JsObject => Try(o.value(field))
    case other => Failure(new RuntimeException(s"Expected field '$field'  Actual: ${context(other)}"))
  }

  /** Should provide some diagnostic string about the pickle 'x', eg its content.
    * Used to build error messages. */
  def context(x: JsValue): String = x.toString()
}
