package im.tox.tox4j.core.exceptions

import im.tox.tox4j.core.data.ToxPublicKey
import im.tox.tox4j.testing.ToxTestMixin
import org.scalatest.funsuite.AnyFunSuite

final class ToxFriendByPublicKeyExceptionTest extends AnyFunSuite with ToxTestMixin {

  test("Null") {
    interceptWithTox(ToxFriendByPublicKeyException.Code.NULL)(
      _.friendByPublicKey(ToxPublicKey.unsafeFromValue(null))
    )
  }

  test("NotFound") {
    interceptWithTox(ToxFriendByPublicKeyException.Code.NOT_FOUND) { tox =>
      tox.friendByPublicKey(tox.getPublicKey)
    }
  }

}
