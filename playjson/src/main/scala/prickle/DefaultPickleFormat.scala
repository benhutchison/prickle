package prickle

import play.api.libs.json._

trait DefaultPickleFormat {

  implicit object PlayJsonPBuilder extends PBuilder[JsValue] {
    def makeNull(): JsValue = JsNull
    def makeBoolean(b: Boolean): JsValue = JsBoolean(b)
    def makeNumber(x: Double): JsValue = JsNumber(x)
    def makeString(s: String): JsValue = JsString(s)
    def makeArray(elems: JsValue*): JsValue = JsArray(elems)
    def makeObject(fields: (String, JsValue)*): JsValue = JsObject(fields)
  }

  implicit object PlayJsonPReader extends PReader[JsValue] {
    def isNull(x: JsValue): Boolean = x == JsNull
    def readBoolean(x: JsValue): Option[Boolean] = x match {
      case JsBoolean(b) => Some(b)
      case _ => None
    }
    def readNumber(x: JsValue): Option[Double] = x match {
      case JsNumber(bigDecimal) => Some(bigDecimal.toDouble)
      case _ => None
    }
    def readString(x: JsValue): Option[String] = x match {
      case JsString(s) => Some(s)
      case _ => None
    }
    def readArrayLength(x: JsValue): Option[Int] = x match {
      case JsArray(elems) => Some(elems.size)
      case _ => None
    }
    def readArrayElem(x: JsValue, index: Int): Option[JsValue] = x match {
      case JsArray(elems) if index < elems.size => Some(elems(index))
      case _ => None
    }
    def readObjectField(x: JsValue, field: String): Option[JsValue] = x match {
      case o: JsObject => o.value.get(field)
      case _ => None
    }

    /** Should provide some diagnostic string about the pickle 'x', eg its content.
      * Used to build error messages. */
    def context(x: JsValue): String = x.toString()
  }
}
