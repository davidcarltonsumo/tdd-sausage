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
  var cache: ShardDiskCache = _

  "A ShardDiskCache" should {
    "install a shard and return the resulting File" in {
      when(downloader.download("path")).thenReturn(new File("installed-path"))
      val shard = Shard("name", "path")

      cache.get(shard) should equal (new File("installed-path"))
    }

    "not re-install a shard that is already installed" in {
      when(downloader.download("path")).thenReturn(new File("installed-path"))
      val shard = Shard("name", "path")

      cache.get(shard)
      cache.get(shard) should equal (new File("installed-path"))

      verify(downloader, times(1)).download("path")
    }

    "install multiple shards" in {
      when(downloader.download("path-1")).thenReturn(new File("installed-path-1"))
      when(downloader.download("path-2")).thenReturn(new File("installed-path-2"))
      when(downloader.download("path-3")).thenReturn(new File("installed-path-3"))

      cache.get(Shard("name-1", "path-1")) should equal (new File("installed-path-1"))
      cache.get(Shard("name-2", "path-2")) should equal (new File("installed-path-2"))
      cache.get(Shard("name-3", "path-3")) should equal (new File("installed-path-3"))
    }

    // Install multiple shards in parallel.
    // Error cases when installing shards?
    // Not download a shard that's in the process of being downloaded.
    // Bound the number of simultaneous downloads.
    // Evict old shards if the disk is full.
    // Initialize the cache from disk on startup.
    // Not get confused if the process gets terminated while we're downloading.
    // Questions:
    // * Should Shard store the name?
    // * Should Downloader take the name as well as the path?
  }

  override protected def beforeEach() {
    downloader = mock[Downloader]
    cache = new ShardDiskCache(downloader)
  }
}
