package prickle

import scala.scalajs.js

trait DefaultPickleFormat {

  /*
  object JSPBuilder extends PBuilder[js.Any] {
  def makeNull(): js.Any = null
  def makeBoolean(b: Boolean): js.Any = b
  def makeNumber(x: Double): js.Any = x
  def makeString(s: String): js.Any = s
  def makeArray(elems: js.Any*): js.Any = js.Array(elems: _*)
  def makeObject(fields: (String, js.Any)*): js.Any = {
    val result = js.Dictionary.empty[js.Any]
    for ((prop, value) <- fields)
      result(prop) = value
    result
  }
}

object JSPReader extends PReader[js.Any] {
  def isUndefined(x: js.Any): Boolean = x.isInstanceOf[js.Undefined]
  def isNull(x: js.Any): Boolean = x eq null
  def readBoolean(x: js.Any): Boolean = x.asInstanceOf[js.Boolean]
  def readNumber(x: js.Any): Double = x.asInstanceOf[js.Number]
  def readString(x: js.Any): String = x.asInstanceOf[js.String]
  def readArrayLength(x: js.Any): Int = x.asInstanceOf[js.Array[_]].length.toInt
  def readArrayElem(x: js.Any, index: Int): js.Any =
    x.asInstanceOf[js.Array[js.Any]].apply(index)
  def readObjectField(x: js.Any, field: String): js.Any =
    x.asInstanceOf[js.Dictionary[js.Any]].apply(field)
}
   */

  implicit object JsPBuilder extends PBuilder[js.Any] {
    def makeNull(): js.Any = null
    def makeBoolean(b: Boolean): js.Any = b
    def makeNumber(x: Double): js.Any = x
    def makeString(s: String): js.Any = s
    def makeArray(elems: js.Any*): js.Any = js.Array(elems: _*)
    def makeObject(fields: (String, js.Any)*): js.Any = {
      val result = js.Dictionary.empty[js.Any]
      for ((prop, value) <- fields)
        result(prop) = value
      result
    }
  }

  implicit object JsPReader extends PReader[js.Any] {
    def isNull(x: js.Any): Boolean = x eq null
    def readBoolean(x: js.Any): Option[Boolean] = x match {
      case b: js.Boolean => Some(b)
      case _ => None
    }
    def readNumber(x: js.Any): Option[Double] = x match {
      case x: js.Number => Some(x)
      case _ => None
    }
    def readString(x: js.Any): Option[String] = x match {
      case s: js.String => Some(s)
      case _ => None
    }
    def readArrayLength(x: js.Any): Option[Int] = x match {
      case x: js.Array[_] => Some(x.length.toInt)
      case _ => None
    }
    def readArrayElem(x: js.Any, index: Int): Option[js.Any] = x match {
      case x: js.Array[_] if index < x.length.toInt =>
        Some(x.asInstanceOf[js.Array[js.Any]](index))
      case _ => None
    }
    def readObjectField(x: js.Any, field: String): Option[js.Any] = {
      x.asInstanceOf[js.Dictionary[js.Any]].apply(field) match {
        case _: js.Undefined => None
        case f => Some(f.asInstanceOf[js.Any])
      }
    }

    /** Should provide some diagnostic string about the pickle 'x', eg its content.
      * Used to build error messages. */
    def context(x: js.Any): String = x.toString()
  }
}
