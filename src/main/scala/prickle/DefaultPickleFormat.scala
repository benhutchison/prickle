package prickle

import scala.util.{Failure, Success, Try}

//sealed trait PFormat
//case object PNull extends PFormat
//case class PBoolean(b: Boolean) extends PFormat
//case class PNumber(x: Double) extends PFormat
//case class PString(s: String) extends PFormat
//case class PArray(elems: Seq[PFormat]) extends PFormat
//case class PObject(fields: Map[String, PFormat]) extends PFormat

trait DefaultPickleFormat

