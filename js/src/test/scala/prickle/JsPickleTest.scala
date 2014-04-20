package prickle

import scala.scalajs.js

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
