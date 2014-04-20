package prickle

import play.api.libs.json._

object PlayTestData extends TestData(PConfig.DefaultConfig) {

  def stripIdTags(pickle: JsValue): JsValue = pickle match {
      case JsObject(fields) => JsObject(fields.filterNot(_._1 == "#id"))
      case JsArray(elems) => JsArray(elems.map(stripIdTags))
      case _ => pickle
  }

  def addField(pickle: JsValue, field: (String, JsValue)): JsValue = pickle match {
    case JsObject(fields) => JsObject(field +: fields)
    case _ => pickle
  }
}

object PlayPickleTest extends PickleTests[JsValue](
  x => if (x) PConfig.DefaultConfig else PConfig.AcyclicConfig, PlayTestData)
