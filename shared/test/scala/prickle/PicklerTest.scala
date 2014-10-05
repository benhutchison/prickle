package prickle

import java.util.Date

import utest._
import scala.collection.immutable.SortedMap
import scala.util.Success


import microjson._


object PickleTests extends TestSuite {
  val testData = new TestData()
  import testData._

  def tests = TestSuite {
    "with cyclic config"- {
      implicit val config = JsConfig()
      import config._

      "caseclass" - {
        "encoding" - {
          val actual = Pickle(benDetails)
          val expected = expectedBenDetailsPickle
          assert(expected == actual)
        }
        "unpickling" - {
          val actual = Unpickle[PersonalDetails].from(expectedBenDetailsPickle)
          assert(Success(benDetails) == actual)
        }
        "toleratesextradata" - {
          val extra: JsValue = new JsObject(
            expectedBenDetailsPickle.asInstanceOf[JsObject].value + ("foo" -> config.makeString("bar")))

          val actual = Unpickle[PersonalDetails].from(extra).get
          assert(benDetails == actual)
        }
      }
      "generic" - {
        val initial: (Person, Person) = (ben, parent)
        val p = Pickle(initial)
        val unpickled = Unpickle[(Person, Person)].from(p).get
        assert(initial == unpickled)
      }
      "compositepicklers" - {
        "apple" - {
          "pickle" - {
            val applePickle = Pickle(apple)
            assert(expectedApplePickle == applePickle)
            val plantPickle = Pickle(apple: EdiblePlant)
            assert(plantPickle == applePickle)
          }
          "unpickle" - {
            val unpickleFruit = Unpickle[Fruit].from(expectedApplePickle).get
            assert(apple == unpickleFruit)
            val unpicklePlant = Unpickle[EdiblePlant].from(expectedApplePickle).get
            assert(apple == unpicklePlant)
          }
        }
        "carrot" - {
          "pickle" - {
            val carrotPickle = Pickle(carrot)
            assert(carrotPickle == expectedCarrotPickle)
          }
          "unpickle" - {
            val unpickle = Unpickle[EdiblePlant].from(expectedCarrotPickle).get
            assert(carrot == unpickle)
          }
        }
        "null" - {
          val pickle: JsValue = config.makeNull

          "pickle" - {
            assert(Pickle(null: EdiblePlant) == pickle)
          }
          "unpickle" - {
            val unpickle = Unpickle[EdiblePlant].from(pickle).get
            assert(null == unpickle)
          }
        }
      }
      "maps" - {
        val favoriteFoods = Map(ben -> apple, parent -> carrot)

        "pickle" - {
          val favoritePickles = Pickle(favoriteFoods)
          assert(favoritePickles == expectedMapPickle)
        }

        "unpickle" - {
          val unpickle = Unpickle[Map[Person, EdiblePlant]].from(expectedMapPickle).get
          assert(unpickle == favoriteFoods)
        }
      }
      "sorted maps" - {
        val map = SortedMap[Int, String](1 -> "Sydney", 2 -> "Inverloch")
        assert(Unpickle[SortedMap[Int, String]].fromString(Pickle.intoString(map)).get == map)
      }
      "dates" - {
        val date = new Date()
        assert(Unpickle[Date].fromString(Pickle.intoString(date)).get == date)
      }
      "seqs" - {
        val seq = Seq("One", "Two")
        val expectedPickle = makeArray(makeString("One"), makeString("Two"))

        "pickle" - {
          val p = Pickle(seq)
          assert(p == expectedPickle)
        }
        "unpickle" - {
          val unpickle = Unpickle[Seq[String]].from(expectedPickle).get
          assert(unpickle == seq)
        }
      }
      "sets" - {
        val set = Set("One", "Two")
        val expectedPickle = makeArray(makeString("One"), makeString("Two"))

        "pickle" - {
          val p = Pickle(set)
          assert(p == expectedPickle)
        }
        "unpickle" - {
          val unpickle = Unpickle[Set[String]].from(expectedPickle).get
          assert(unpickle == set)
        }
      }
      "cycle handling" - {
        assert(brothers._1.parent eq brothers._2.parent)

        val pickle = Pickle(brothers)
        val afterPickling = Unpickle[(PersonalDetails, PersonalDetails)].from(pickle).get

        val p1 = afterPickling._1.parent
        val p2 = afterPickling._2.parent
        assert(p1 eq p2)
      }
      "case_class_with_doubly_nested_parametric_field"-{
        Unpickle[DoublyNested].from(Pickle(DoublyNested(None)))
      }
      "intoString"-{
        val s = Pickle.intoString(benDetails)
        assert(expectedBenDetailsString == s)
      }
      "fromString"-{
        val actual = Unpickle[PersonalDetails].fromString(expectedBenDetailsString)
        assert(Success(benDetails) == actual)
      }
  }
    "with acyclic config"-{
      implicit val acyclicConfig = JsConfig(isCyclesSupported = false)

      "over cyclic structure"-{

        assert(brothers._1.parent eq brothers._2.parent)

        val pickleState = PickleState()
        val pickle = Pickle(brothers, pickleState)

        val unpickleState = collection.mutable.Map.empty[String, Any]
        val afterPickling = Unpickle[(PersonalDetails, PersonalDetails)].from(pickle, unpickleState).get

        val p1 = afterPickling._1.parent
        val p2 = afterPickling._2.parent

        "shared structures are duplicated"-{assert(!(p1 eq p2))}

        "data is preserved"-{
          assert(afterPickling._1 == benDetails)
          assert(afterPickling._2 == brotherDetails)
        }

        "no id tags added to pickle"-{assert(pickle == stripIdTags(pickle))}

        "no state during pickle"-{assert(pickleState.refs.isEmpty)}
        "no state during unpickle"-{assert(unpickleState.isEmpty)}
      }
    }
  }
}

case class DoublyNested(field:  Option[(Fruit, Int)])