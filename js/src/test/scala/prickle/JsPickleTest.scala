package prickle

import scala.scalajs.js
import scala.scalajs.js.JSON

object JsTestData extends TestData(PConfig.DefaultConfig) {

  def stripIdTags(pickle: js.Any): js.Any = pickle match {
    case x: js.Array[_] => {
      x.map((value: Any) => stripIdTags(value.asInstanceOf[js.Any]))
    }
    case x: js.Object => {
      val y = copy(x)
      y.delete("#id")
      y
    }
    case _ => pickle
  }

  def addField(pickle: js.Any, field: (String, js.Any)): js.Any = pickle match {
    case x: js.Object => {
      val y = copy(x)
      y.update(field._1, field._2)
      y
    }
    case _ => pickle
  }

  def areEqual(p1: js.Any, p2: js.Any): Boolean = (p1, p2) match {
    case (p1: js.Object, p2: js.Object) => {
      for (field <- js.Dictionary.propertiesOf(p1))
        if (!areEqual(p1.asInstanceOf[js.Dictionary[js.Any]](field),
          p2.asInstanceOf[js.Dictionary[js.Any]](field)))
          return false
      true
    }
    case _ => p1 == p2
  }

  def copy(obj: js.Object): js.Dictionary[js.Any] = {
    val obj2 = js.Dictionary.empty[js.Any]
    for (field <- js.Dictionary.propertiesOf(obj))
      if (obj.hasOwnProperty(field))
        obj2.update(field, obj.asInstanceOf[js.Dictionary[js.Any]](field))
    obj2
  }
}

object JsPickleTest extends PickleTests[js.Any](
  x => if (x) PConfig.DefaultConfig else PConfig.AcyclicConfig, JsTestData)
