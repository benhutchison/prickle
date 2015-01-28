package prickle

import microjson._

import scala.concurrent.duration._

case class Person(name: String)
case class PersonalDetails(person: Person, starsign: String, age: Int, isFunny: Boolean, height: Double, parent: Person, ref: AnObject.type, favoriteLawnmower: ModelNumber)
case class ModelNumber(series: Char, model: Short, variant: Byte, barcode: Long, fuelConsumption: Float, engineDuration: Duration)
case object AnObject

trait EdiblePlant
abstract class Fruit extends EdiblePlant
trait Vegetable extends EdiblePlant
case object Carrot extends Vegetable
case class Apple(wormCount: Int) extends Fruit
case class Lime(isSour: Boolean) extends Fruit

class TestData() {

  import PConfig.Default._

  val apple: Fruit = Apple(2)
  val carrot: EdiblePlant = Carrot

  implicit val ap = Unpickler.materializeUnpickler[Tuple2[Person, Person]]

  implicit val fruitPickler = CompositePickler[Fruit].concreteType[Apple].concreteType[Lime]
  implicit val plantPickler = CompositePickler[EdiblePlant].concreteType[Carrot.type].concreteType[Apple].concreteType[Lime]


  val lawnmowerModel = ModelNumber('V', 2000, 42, 1234567890l, -0.125f, 365.days)
  val parent = Person("Keith")
  val ben = Person("Ben")
  val benDetails = PersonalDetails(ben, null, 40, false, 175.6667, parent, AnObject, lawnmowerModel)


  val expectedBenDetailsPickle: JsValue = makeObjectFrom(
    "#id" -> makeString("4"),
    "person" -> makeObjectFrom(
      "#id" -> makeString("1"),
      "name" -> makeString("Ben")
    ),
    "starsign" -> makeNull,
    "age" -> makeNumber(40.0),
    "isFunny" -> makeBoolean(false),
    "height" -> makeNumber(175.6667),
    "ref" -> makeObjectFrom("#scalaObj" -> makeString("prickle.AnObject")),
    "parent" -> makeObjectFrom(
      "#id" -> makeString("2"),
      "name" -> makeString("Keith")
    ),
    "favoriteLawnmower" -> makeObjectFrom(
      "#id" -> makeString("3"),
      "series" -> makeString("V"),
      "barcode" -> makeObjectFrom("l" -> makeNumber(1442514), "m" -> makeNumber(294), "h" -> makeNumber(0)),
      "model" -> makeNumber(2000),
      "variant" -> makeNumber(42),
      "fuelConsumption" -> makeNumber(-0.125),
      "engineDuration" -> makeObjectFrom("l" -> makeNumber(2293760), "m" -> makeNumber(2575542), "h" -> makeNumber(1792)))
  )

  val expectedBenDetailsString =
    """{"favoriteLawnmower": {"series": "V", "barcode": {"l": 1442514, "m": 294, "h": 0}, "#id": "3", "model": 2000, "engineDuration": {"l": 2293760, "m": 2575542, "h": 1792}, "variant": 42, "fuelConsumption": -0.125}, "parent": {"#id": "2", "name": "Keith"}, "#id": "4", "isFunny": false, "height": 175.6667, "ref": {"#scalaObj": "prickle.AnObject"}, "age": 40, "starsign": null, "person": {"#id": "1", "name": "Ben"}}"""

  val appleCls: JsValue = makeString("prickle.Apple")
  val carrotCls: JsValue = makeString("prickle.Carrot$")
  val carrotObj: JsValue = makeString("prickle.Carrot")

  val expectedApplePickle: JsValue = makeObjectFrom("#cls" -> appleCls, "#val" -> makeObjectFrom(
    "#id" -> makeString("1"), "wormCount" -> makeNumber(2)))
  val expectedCarrotPickle: JsValue = makeObjectFrom("#cls" -> carrotCls, "#val" -> makeObjectFrom("#scalaObj" -> carrotObj))

  val expectedMapPickle: JsValue = makeArray(
    makeArray(
      makeObjectFrom("#id" -> makeString("1"), "name" -> makeString("Ben")),
      makeObjectFrom("#cls" -> appleCls, "#val" -> makeObjectFrom("#id" -> makeString("2"), "wormCount" -> makeNumber(2)))),

    makeArray(
      makeObjectFrom("#id" -> makeString("3"), "name" -> makeString("Keith")),
      makeObjectFrom("#cls" -> carrotCls, "#val" -> makeObjectFrom("#scalaObj" -> carrotObj)))
  )


  def expectedBenDetailsAcyclicPickle: JsValue = stripIdTags(expectedBenDetailsPickle)

  def stripIdTags(pickle: JsValue): JsValue = pickle match {
    case JsObject(fields) => JsObject(fields - "#id")
    case JsArray(elems) => JsArray(elems.map(stripIdTags))
    case _ => pickle
  }

  val brotherDetails = PersonalDetails(Person("Oliver"), null, 37, false, 175.6667, parent, AnObject, lawnmowerModel)
  val brothers = (benDetails, brotherDetails)
}
