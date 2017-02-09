package prickle

import scala.language.experimental.macros

import scala.reflect.macros.Context


object PicklerMaterializersImpl {
  def materializePickler[T: c.WeakTypeTag](c: Context): c.Expr[Pickler[T]] = {
    import c.universe._

    val tpe = weakTypeOf[T]

    if (!tpe.typeSymbol.isClass)
      throw new RuntimeException("Enclosure: " + c.enclosingPosition.toString)

    val sym = tpe.typeSymbol.asClass

    if (!sym.isCaseClass) {
      c.error(c.enclosingPosition,
        s"Cannot materialize pickler for non-case class: ${sym.fullName}")
      return c.Expr[Pickler[T]](q"null")
    }

    val pickleLogic = if (sym.isModuleClass) {

      q"""config.makeObject(config.prefix + "scalaObj", config.makeString(${sym.fullName}))"""

    } else {
      val accessors = (tpe.declarations collect {
        case acc: MethodSymbol if acc.isCaseAccessor => acc
      }).toList

      val pickleFields = for {
        accessor <- accessors
      } yield {
        val fieldName = accessor.name.asInstanceOf[TermName]
        val fieldString = fieldName.toString()

        val fieldPickle = q"prickle.Pickle.withConfig(value.$fieldName, state, config)"

        val nullSafeFieldPickle = {
          val symbol = accessor.typeSignatureIn(tpe).typeSymbol
          if (symbol.isClass && symbol.asClass.isPrimitive)
            fieldPickle
          else
            q"""if (value.$fieldName == null) {
              config.makeNull()
            } else
              prickle.Pickle.withConfig(value.$fieldName, state, config)"""
        }

        q"""($fieldString, $nullSafeFieldPickle)"""
      }

      q"""
        def fieldPickles = Seq(..$pickleFields)
        Pickler.resolvingSharing[P](value, fieldPickles, state, config)
      """
    }
    val name = newTermName(c.fresh("GenPickler"))

    val result = q"""
      implicit object $name extends prickle.Pickler[$tpe] {
        import prickle._
        override def pickle[P](value: $tpe, state: PickleState)(
            implicit config: PConfig[P]): P = $pickleLogic
      }
      $name
    """

    c.Expr[Pickler[T]](result)
  }

  def materializeUnpickler[T: c.WeakTypeTag](c: Context): c.Expr[Unpickler[T]] = {
    import c.universe._

    val tpe = weakTypeOf[T]
    val sym = tpe.typeSymbol.asClass

    if (!sym.isCaseClass) {
      c.error(c.enclosingPosition,
        s"Cannot materialize pickler for non-case class: ${sym.fullName}")
      return c.Expr[Unpickler[T]](q"null")
    }

    val unpickleLogic = if (sym.isModuleClass) {
      c.parse(sym.fullName)
    } else {
      val unpickleBody = {
        val accessors = (tpe.declarations.collect {
          case acc: MethodSymbol if acc.isCaseAccessor => acc
        }).toList

        val unpickledFields = for {
          accessor <- accessors
        } yield {
          val fieldName = accessor.name
          val fieldTpe = accessor.typeSignatureIn(tpe)
          q"""
              config.readObjectField(pickle, ${fieldName.toString}).flatMap(field =>
                prickle.Unpickle[$fieldTpe].from(field, state)(config)).get
          """
        }
        q"""
          val result = new $tpe(..$unpickledFields)
          Unpickler.resolvingSharing[P](result, pickle, state, config)
          scala.util.Success(result)
        """
      }
      val unpickleRef = q"""(p: P) => config.readString(p).flatMap(ref => Try{state(ref).asInstanceOf[$tpe]})"""

      q"""
      config.readObjectField(pickle, config.prefix + "ref").transform({$unpickleRef}, _ => {$unpickleBody}).get
      """
    }


    val nullLogic = if (sym.isPrimitive)
      q"""throw new RuntimeException("Cannot unpickle null into Primitive field '" +
        ${tpe.typeSymbol.name.toString} + "'. Context: "  + config.context(pickle))"""
    else
      q"null"

    val name = newTermName(c.fresh("GenUnpickler"))

    val result = q"""
      implicit object $name extends prickle.Unpickler[$tpe] {
        import prickle._
        import scala.util.Try
        override def unpickle[P](pickle: P, state: collection.mutable.Map[String, Any])(
          implicit config: PConfig[P]): Try[$tpe] = Try {
            if (config.isNull(pickle))
              $nullLogic
            else
              $unpickleLogic
          }
      }
      $name
    """

    c.Expr[Unpickler[T]](result)
  }
}


