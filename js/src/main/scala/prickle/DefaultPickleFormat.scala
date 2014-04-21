package prickle

import scala.scalajs.js
import scala.util.{Failure, Try, Success}

trait DefaultPickleFormat {

  implicit object DefaultConfig extends CyclicPConfig[js.Any]
    with HashCharPrefix[js.Any] with JsPBuilder with JsPReader

  object AcyclicConfig extends AcyclicPConfig[js.Any]
    with HashCharPrefix[js.Any] with JsPBuilder with JsPReader

}

trait JsPBuilder extends PBuilder[js.Any] {
  def makeNull(): js.Any = null
  def makeBoolean(b: Boolean): js.Any = b
  def makeNumber(x: Double): js.Any = x
  def makeString(s: String): js.Any = s
  def makeArray(elems: js.Any*): js.Any = js.Array(elems: _*)
  def makeObject(fields: Seq[(String, js.Any)]): js.Any = {
    val result = js.Dictionary.empty[js.Any]
    for ((prop, value) <- fields)
      result(prop) = value
    result
  }
}

trait JsPReader extends PReader[js.Any] {
  def isNull(x: js.Any): Boolean = x eq null
  def readBoolean(x: js.Any): Try[Boolean] = x match {
    case b: js.Boolean => Success(b)
    case other => Failure(new RuntimeException(s"Expected: boolean  Actual: ${context(other)}"))
  }
  def readNumber(x: js.Any): Try[Double] = x match {
    case x: js.Number => Success(x)
    case other => Failure(new RuntimeException(s"Expected: js.Number  Actual: ${context(other)}"))
  }
  def readString(x: js.Any): Try[String] = x match {
    case s: js.String => Success(s)
    case other => Failure(new RuntimeException(s"Expected: string  Actual: ${context(other)}"))
  }
  def readArrayLength(x: js.Any): Try[Int] = x match {
    case x: js.Array[_] => Success(x.length.toInt)
    case other => Failure(new RuntimeException(s"Expected: array length  Actual: ${context(other)}"))
  }
  def readArrayElem(x: js.Any, index: Int): Try[js.Any] = x match {
    case x: js.Array[_] if index < x.length.toInt =>
      Success(x.asInstanceOf[js.Array[js.Any]](index))
    case other => Failure(new RuntimeException(s"Expected: array($index)  Actual: ${context(other)}"))
  }
  def readObjectField(x: js.Any, field: String): Try[js.Any] = {
    x.asInstanceOf[js.Dictionary[js.Any]].apply(field) match {
      case other: js.Undefined => Failure(new RuntimeException(s"Expected field '$field'  Actual: ${context(x)}"))
      case f => Success(f.asInstanceOf[js.Any])
    }
  }

  /** Should provide some diagnostic string about the pickle 'x', eg its content.
    * Used to build error messages. */
  def context(x: js.Any): String = x match {
    case null => "null"
    case x: js.Undefined => "Undefined"
    case x: js.Array[Any] => x.mkString("js.Array(",",", ")")
    case x: js.Boolean => x.toString
    case x: js.String => x
    case x: js.Number => x.toString
    case x: js.Object => {
      val y = x.asInstanceOf[js.Dictionary[js.Any]]
      val f = js.Any.fromFunction1((k: js.String) => s"$k: ${context(y(k))}".asInstanceOf[js.String])
      val ps = js.Dictionary.propertiesOf(x).map(f)
      ps.mkString("js.Object(", ",", ")")
    }
    case x: Any => x.toString
  }
}