package prickle

import scala.util.{Failure, Success, Try}
import collection.mutable

import microjson._

object PConfig {
  implicit val Default = new JsConfig()

}

trait PConfig[P] extends PReader[P] with PBuilder[P] {

  def prefix: String

  def areSharedObjectsSupported: Boolean

  @deprecated("Use areSharedObjectsSupported instead. (Misleadingly name)", "1.1.0")
  def isCyclesSupported: Boolean = areSharedObjectsSupported

}

case class JsConfig(val prefix: String = "#", val areSharedObjectsSupported: Boolean = true)
  extends PConfig[JsValue] with JsBuilder with JsReader {

  def onUnpickle(id: String, value: Any, state: mutable.Map[String, Any]) = {
    state += (id -> value)
  }

}
trait AcyclicPConfig[P] extends PConfig[P] {
  def areSharedObjectsSupported = false
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

  def readObjectFieldStr(x: P, field: String): Try[String] = readObjectField(x, field).flatMap(readString)

  def readObjectFieldNum(x: P, field: String): Try[Double] = readObjectField(x, field).flatMap(readNumber)
}


trait JsBuilder extends PBuilder[JsValue] {
  def makeNull(): JsValue = JsNull
  def makeBoolean(b: Boolean): JsValue = if (b) JsTrue else JsFalse
  def makeNumber(x: Double): JsValue = JsNumber(x.toString)

  def makeString(s: String): JsValue = JsString(s)
  def makeArray(elems: JsValue*): JsValue = JsArray(elems)
  def makeObject(fields: Seq[(String, JsValue)]): JsValue = JsObject(fields.toMap)
}

trait JsReader extends PReader[JsValue] {
  def isNull(x: JsValue): Boolean = x match {
    case JsNull => true
    case _ => false
  }
  def readBoolean(x: JsValue): Try[Boolean] = x match {
    case JsTrue => Success(true)
    case JsFalse => Success(false)
    case other => error("boolean", s"$other")
  }
  def readNumber(x: JsValue): Try[Double] = x match {
    case x: JsNumber => Try(x.value.toDouble)
    case other => error("number", s"$other")
  }
  def readString(x: JsValue): Try[String] = x match {
    case s: JsString => Success(s.value)
    case other => error("string", s"$other")
  }
  def readArrayLength(x: JsValue): Try[Int] = x match {
    case x: JsArray => Success(x.value.length)
    case other => error("array length", s"$other")
  }
  def readArrayElem(x: JsValue, index: Int): Try[JsValue] = x match {
    case x: JsArray if index < x.value.length => Success(x.value(index))
    case other => error(s"array($index)", s"$other")
  }
  def readObjectField(x: JsValue, field: String): Try[JsValue] = x match {
    case x: JsObject => Try(x.value(field)).orElse(fail(
      s"Cannot read field '$field' of '$x', available fields: ${x.value.values.mkString(", ")}"))
    case other =>  error(s"field \'$field\'", s"$other")
  }

  def error(exp: String, actual: String) = Failure(new RuntimeException(s"Expected: $exp  Actual: $actual"))

  def fail(msg: String) = Failure(new RuntimeException(msg))
}
