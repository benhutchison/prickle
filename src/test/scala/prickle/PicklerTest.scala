package prickle

import utest._
import Pickler._
import Unpickler._

case class Person(name: String, age: Int, isFunny: Boolean, height: Double, parent: Person)
//, ref: AnObject.type, favoriteLawnmower: ModelNumber
case class ModelNumber(series: Char, model: Short, variant: Byte, barcode: Long, fuelConsumption: Float)
case object AnObject



object PickleTests extends TestSuite{
  val tests = TestSuite {

    "caseclass"-{

//      implicit object GenPickler extends prickle.Pickler[prickle.Person] {
//        import prickle._;
//
//        override def pickle[P](value: prickle.Person)(implicit builder: PBuilder[P]): P = {
//          builder.makeObject(Tuple2("name", if (value.name.$eq$eq(null))
//            builder.makeNull()
//          else
//            value.name.pickle), Tuple2("age", value.age.pickle), Tuple2("isFunny", value.isFunny.pickle
//          ), Tuple2("height", value.height.pickle), Tuple2("parent", if (value.parent.$eq$eq(null))
//            builder.makeNull()
//          else
//            value.parent.pickle))
//        }
//      }
//      implicit object GenUnpickler extends prickle.Unpickler[prickle.Person] {
//          import prickle._;
//          import scala.util.Try;
//          override def unpickle[P](pickle: P)(implicit reader: PReader[P]): Try[prickle.Person] = {
//            Try(if (reader.isNull(pickle))
//              null
//            else
//              new prickle.Person(reader.readObjectField(pickle, "name").flatMap(((field) =>
//                prickle.Unpickler.to[String].unpickle(field)(reader))).get,
//              reader.readObjectField(pickle, "age").flatMap(((field) =>
//                prickle.Unpickler.to[Int].unpickle(field)(reader))).get, reader.readObjectField(pickle, "isFunny").flatMap(
//              ((field) => prickle.Unpickler.to[Boolean].unpickle(field)(reader))).get, reader.readObjectField(pickle,
//              "height").flatMap(((field) => prickle.Unpickler.to[Double].unpickle(field)(reader))).get,
//              reader.readObjectField(pickle, "parent").flatMap(((field) =>
//                prickle.Unpickler.to[prickle.Person].unpickle(field)(reader))).get))
//            }}
//            GenUnpickler

      val negatron2k = ModelNumber('V', 2000, 42, 1234567890l, -0.04f)
      val father = Person("Keith", 70, true, 176, null)//, AnObject, negatron2k)
      val son = Person("Ben", 40, false, 175.6667, father)//, AnObject, null)

      val expectedEncoding = PObject(Map(
        "name" -> PString("Ben"),
        "isFunny" -> PBoolean(false),
        "height" -> PNumber(175.6667),
        "ref" -> PObject(Map("#scalaObj" -> PString("prickle.AnObject"))),
        "age" -> PNumber(40.0),
        "favoriteLawnmower" -> PNull,
        "parent" -> PObject(Map(
          "name" -> PString("Keith"),
          "isFunny" -> PBoolean(true),
          "height" -> PNumber(176.0),
          "ref" -> PObject(Map("#scalaObj" -> PString("prickle.AnObject"))),
          "age" -> PNumber(70.0),
          "favoriteLawnmower" -> PObject(Map(
            "series" -> PString("V"),
            "barcode" -> PObject(Map("l" -> PNumber(1442514.0), "m" -> PNumber(294.0), "h" -> PNumber(0.0))),
            "model" -> PNumber(2000.0),
            "variant" -> PNumber(42.0),
            "fuelConsumption" -> PNumber(-0.03999999910593033))),
          "parent" -> PNull
        ))
      ))

      "encoding"-{
        val actual: PFormat = RichPicklee(son).pickle

        def assertField(fld: String) = assert(expectedEncoding.asInstanceOf[PObject].fields(fld) == actual.asInstanceOf[PObject].fields(fld))

        assert(expectedEncoding == actual)
      }
      "unpickling"-{
        val actual = Unpickler.to[Person].unpickle(expectedEncoding: PFormat)

        assert(son == actual.get)
      }
      "toleratesextradata"-{
        val extra: PFormat = expectedEncoding.copy(fields = expectedEncoding.fields +
          ("foo" -> PString("bar")) +
          ("ref" -> PObject(Map("#scalaObj" -> PString("prickle.AnObject"), "foo2" -> PString("bar2")))))

        val actual = Unpickler.to[Person].unpickle(extra)
        assert(son == actual.get)
      }


//      val pik = ben.pickle
//      val benTry = to[Person].unpickle(pik)
//
//      assert(ben == benTry.get)
    }
  }
}