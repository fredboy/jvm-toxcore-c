package im.tox.tox4j.core.exceptions

import im.tox.tox4j.core.data.ToxFriendNumber
import im.tox.tox4j.testing.ToxTestMixin
import org.scalatest.funsuite.AnyFunSuite

final class ToxSetTypingExceptionTest extends AnyFunSuite with ToxTestMixin {

  private val friendNumber = ToxFriendNumber.fromInt(0).get
  private val badFriendNumber = ToxFriendNumber.fromInt(1).get

  test("SetTypingToNonExistent") {
    interceptWithTox(ToxSetTypingException.Code.FRIEND_NOT_FOUND)(
      _.setTyping(badFriendNumber, typing = true)
    )
  }

}
