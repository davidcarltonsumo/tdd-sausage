import java.io.File
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, WordSpec}
import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class ShardDiskCacheTest extends WordSpec with ShouldMatchers with MockitoSugar
with BeforeAndAfterEach {
  var downloader: StubDownloader = _
  var cache: ShardDiskCache = _

  "A ShardDiskCache" should {
    "install a shard and return the resulting File" in {
      cache.get(Shard("name", "path")) should equal (new File("installed-path"))
    }

    "not re-install a shard that is already installed" in {
      val shard = Shard("name", "path")

      cache.get(shard)
      cache.get(shard) should equal (new File("installed-path"))

      downloader.downloadCounts("path") should equal (1)
    }

    "install multiple shards" in {
      cache.get(Shard("name-1", "path-1")) should equal (new File("installed-path-1"))
      cache.get(Shard("name-2", "path-2")) should equal (new File("installed-path-2"))
      cache.get(Shard("name-3", "path-3")) should equal (new File("installed-path-3"))
    }

    "install multiple shards in parallel" in {
      // Fire off three requests.
      // Tell the downloader not to answer the requests for a bit.
      // Then let it answer the requests.
      // Make sure they show up eventually.
    }

    // Install multiple shards in parallel.
    // Not download a shard that's in the process of being downloaded.
    // Bound the number of simultaneous downloads.
    // Error cases when installing shards?
    // Evict old shards if the disk is full.
    // Initialize the cache from disk on startup.
    // Not get confused if the process gets terminated while we're downloading.
    // Questions:
    // * Should Shard store the name?
    // * Should Downloader take the name as well as the path?
  }

  class StubDownloader extends Downloader {
    val downloadCounts = mutable.Map[String, Int]()

    def download(path: String): File = {
      downloadCounts(path) = downloadCounts.getOrElse(path, 0) + 1
      new File("installed-" + path)
    }
  }

  override protected def beforeEach() {
    downloader = new StubDownloader
    cache = new ShardDiskCache(downloader)
  }
}
