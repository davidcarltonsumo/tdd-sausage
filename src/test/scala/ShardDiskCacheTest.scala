import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec

@RunWith(classOf[JUnitRunner])
class ShardDiskCacheTest extends WordSpec with ShouldMatchers {
  "A ShardDiskCache" should {
    "do something" in {
      1 should equal (3)
    }
  }
}
