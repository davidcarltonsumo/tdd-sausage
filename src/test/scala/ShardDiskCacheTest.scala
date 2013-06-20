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
    "download and install a shard" in {
      val shard = Shard("name", "path")
      val downloader = mock[Downloader]
      val sut = new ShardDiskCache(downloader)
      when(downloader.download("name", "path")).thenReturn(new File("installed/name"))

      sut.install(shard) should equal (new File("installed/name"))

      verify(downloader).download("name", "path")
    }

    // Not re-install a shard that is already installed.
    // Install a bunch of shards.
    // Install multiple shards in parallel.
    // Not download a shard that's in the process of being downloaded.
    // Bound the number of simultaneous downloads.
    // Evict old shards if the disk is full.
    // Initialize the cache from disk on startup.
    // Not get confused if the process gets terminated while we're downloading.
    // Provide a prefetch mechanism where we say that we'll want something in the future?
  }
}
