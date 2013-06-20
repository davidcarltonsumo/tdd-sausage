import java.io.File
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, WordSpec}

@RunWith(classOf[JUnitRunner])
class ShardDiskCacheTest extends WordSpec with ShouldMatchers with MockitoSugar
with BeforeAndAfterEach {
  var downloader: Downloader = _
  var sut: ShardDiskCache = _

  "A ShardDiskCache" should {
    "download and install a shard" in {
      val shard = Shard("name", "path")

      when(downloader.download("name", "path")).thenReturn(new File("installed/name"))

      sut.install(shard) should equal (new File("installed/name"))

      verify(downloader).download("name", "path")
    }

    "Not re-download a shard that is already installed" in {
      val shard = Shard("name", "path")

      when(downloader.download("name", "path")).thenReturn(new File("installed/name"))

      sut.install(shard) should equal (new File("installed/name"))
      sut.install(shard) should equal (new File("installed/name"))

      verify(downloader, times(1)).download("name", "path")
    }

    "Install a bunch of shards" in {
      val shard1 = Shard("name1", "path1")
      val shard2 = Shard("name2", "path2")
      val shard3 = Shard("name3", "path3")

      when(downloader.download("name1", "path1")).thenReturn(new File("installed/name1"))
      when(downloader.download("name2", "path2")).thenReturn(new File("installed/name2"))
      when(downloader.download("name3", "path3")).thenReturn(new File("installed/name3"))

      sut.install(shard1) should equal (new File("installed/name1"))
      sut.install(shard2) should equal (new File("installed/name2"))
      sut.install(shard3) should equal (new File("installed/name3"))
    }

    // Install multiple shards in parallel.
    // Not download a shard that's in the process of being downloaded.
    // Bound the number of simultaneous downloads.
    // Evict old shards if the disk is full.
    // Initialize the cache from disk on startup.
    // Not get confused if the process gets terminated while we're downloading.
    // Provide a prefetch mechanism where we say that we'll want something in the future?
  }

  override protected def beforeEach() {
    downloader = mock[Downloader]
    sut = new ShardDiskCache(downloader)
  }
}
