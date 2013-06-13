import java.io.File
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.WordSpec

@RunWith(classOf[JUnitRunner])
class ShardDiskCacheTest extends WordSpec with ShouldMatchers with MockitoSugar {
  "A ShardDiskCache" should {
    "install a shard and return the resulting File" in {
      val downloader = mock[Downloader]
      when(downloader.download("path")).thenReturn(new File("installed-path"))
      val cache = new ShardDiskCache(downloader)
      val shard = Shard("name", "path")

      cache.get(shard) should equal (new File("installed-path"))
    }

    "not re-install a shard that is already installed" in {
      val downloader = mock[Downloader]
      when(downloader.download("path")).thenReturn(new File("installed-path"))
      val cache = new ShardDiskCache(downloader)
      val shard = Shard("name", "path")

      cache.get(shard)
      cache.get(shard) should equal (new File("installed-path"))

      verify(downloader, times(1)).download("path")
    }

    // Install a bunch of shards.
    // Install multiple shards in parallel.
    // Error cases when installing shards?
    // Not download a shard that's in the process of being downloaded.
    // Bound the number of simultaneous downloads.
    // Evict old shards if the disk is full.
    // Initialize the cache from disk on startup.
    // Not get confused if the process gets terminated while we're downloading.
  }
}
