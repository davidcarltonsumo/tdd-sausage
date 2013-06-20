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
  var downloader: FakeDownloader = _
  var sut: ShardDiskCache = _

  "A ShardDiskCache" should {
    "download and install a shard" in {
      sut.install(Shard("name", "path")) should equal (new File("installed/name"))

      downloader.actions should equal ("d(name,path);")
    }

    "Not re-download a shard that is already installed" in {
      sut.install(Shard("name", "path")) should equal (new File("installed/name"))
      sut.install(Shard("name", "path")) should equal (new File("installed/name"))

      downloader.actions should equal ("d(name,path);")
    }

    "Install a bunch of shards" in {
      sut.install(Shard("name1", "path1")) should equal (new File("installed/name1"))
      sut.install(Shard("name2", "path2")) should equal (new File("installed/name2"))
      sut.install(Shard("name3", "path3")) should equal (new File("installed/name3"))
    }

    "Install multiple shards in parallel" in {
      // Create a bunch of threads
      // Tell the downloader to block if asked to download
      // Tell them each to start download
      // Tell the downloader to unblock
      // Wait for the threads to be done
      // Check responses as expected
    }

    // Not download a shard that's in the process of being downloaded.
    // Bound the number of simultaneous downloads.
    // Evict old shards if the disk is full.
    // Initialize the cache from disk on startup.
    // Not get confused if the process gets terminated while we're downloading.
    // Provide a prefetch mechanism where we say that we'll want something in the future?
  }

  override protected def beforeEach() {
    downloader = new FakeDownloader
    sut = new ShardDiskCache(downloader)
  }

  class FakeDownloader extends Downloader {
    var actions = ""

    def download(name: String, path: String): File = {
      actions += f"d($name,$path);"
      new File(f"installed/$name")
    }
  }
}
