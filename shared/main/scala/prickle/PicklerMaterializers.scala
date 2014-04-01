package prickle

import scala.language.experimental.macros

import scala.reflect.macros.Context

object PicklerMaterializersImpl {
  def materializePickler[T: c.WeakTypeTag](c: Context): c.Expr[Pickler[T]] = {
    import c.universe._

    val tpe = weakTypeOf[T]
    val sym = tpe.typeSymbol.asClass

    if (!sym.isCaseClass) {
      c.error(c.enclosingPosition,
        s"Cannot materialize pickler for non-case class: ${sym.fullName}")
      return c.Expr[Pickler[T]](q"null")
    }

    val pickleLogic = if (sym.isModuleClass) {

      q"""builder.makeObject(("#scalaObj", builder.makeString(${sym.fullName})))"""

    } else {
      val accessors = (tpe.declarations collect {
        case acc: MethodSymbol if acc.isCaseAccessor => acc
      }).toList

      val pickleFields = for {
        accessor <- accessors
      } yield {
        val fieldName = accessor.name
        val fieldString = fieldName.toString()

        val fieldPickle = q"prickle.Pickle(value.$fieldName)"

        val nullSafeFieldPickle =
          if (accessor.returnType.typeSymbol.asClass.isPrimitive)
            fieldPickle
          else
            q"if (value.$fieldName == null) builder.makeNull() else $fieldPickle"

        q"""($fieldString, $nullSafeFieldPickle)"""
      }

      q"""builder.makeObject(..$pickleFields)"""
    }
    val name = newTermName(c.fresh("GenPickler"))

    val result = q"""
      implicit object $name extends prickle.Pickler[$tpe] {
        import prickle._
        override def pickle[P](value: $tpe)(
            implicit builder: PBuilder[P]): P = $pickleLogic
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

      q"""
        val objName = reader.readString(reader.readObjectField(pickle, "#scalaObj").get).get
        import scala.reflect.runtime.universe
        val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
        val module = runtimeMirror.staticModule(objName)
        val obj = runtimeMirror.reflectModule(module)
        obj.instance.asInstanceOf[$tpe]
      """

    } else {

      val accessors = (tpe.declarations collect {
        case acc: MethodSymbol if acc.isCaseAccessor => acc
      }).toList

      val unpickledFields = for {
        accessor <- accessors
      } yield {
        val fieldName = accessor.name
        val fieldTpe = accessor.returnType
        q"""
            reader.readObjectField(pickle, ${fieldName.toString}).flatMap(field =>
              prickle.Unpickle[$fieldTpe].from(field)(reader)).get
        """
      }
      q"""new $tpe(..$unpickledFields)"""
    }


    val nullLogic = if (sym.isPrimitive)
      q"""throw new RuntimeException("Cannot unpickle null into Primitive field '" +
        ${tpe.typeSymbol.name.toString} + "'. Context: "  + reader.context(pickle))"""
    else
      q"null"

    val name = newTermName(c.fresh("GenUnpickler"))

    val result = q"""
      implicit object $name extends prickle.Unpickler[$tpe] {
        import prickle._
        import scala.util.Try
        override def unpickle[P](pickle: P)(
          implicit reader: PReader[P]): Try[$tpe] = Try {
            if (reader.isNull(pickle))
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


