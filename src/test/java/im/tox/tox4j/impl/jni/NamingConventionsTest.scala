package im.tox.tox4j.impl.jni

import org.scalatest.funsuite.AnyFunSuite

abstract class NamingConventionsTest(jniClass: Class[_], traitClass: Class[_]) extends AnyFunSuite {

  private val exemptions = Seq("callback", "load", "close", "create", "getFriendNumbers")

  test("Java method names should be derivable from JNI method names") {
    val jniMethods = MethodMap(jniClass)

    traitClass
      .getDeclaredMethods.toSeq
      .map(_.getName)
      .filterNot(exemptions.contains)
      .foreach { name =>
        assert(jniMethods.contains(name))
      }
  }

}
