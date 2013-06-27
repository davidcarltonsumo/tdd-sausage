import java.io.File
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import org.junit.runner.RunWith
import org.scalatest.concurrent.Eventually
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, WordSpec}

@RunWith(classOf[JUnitRunner])
class ShardDiskCacheTest extends WordSpec with ShouldMatchers with MockitoSugar
with BeforeAndAfterEach with Eventually {
  var downloader: StubDownloader = _
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
      downloader.block()

      // Tell them each to start download

      eventually { downloader.downloadCount should equal (5) }

      // Tell the downloader to unblock
      downloader.unblock()

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
    downloader = new StubDownloader
    sut = new ShardDiskCache(downloader)
  }

  class StubDownloader extends Downloader {
    var actions = ""
    var currentDownloadCount = new AtomicInteger(0)
    var shouldBlock = false

    def download(name: String, path: String): File = {
      currentDownloadCount.incrementAndGet()
      waitUntilShouldntBlock()
      actions += f"d($name,$path);"
      currentDownloadCount.decrementAndGet()
      new File(f"installed/$name")
    }

    def waitUntilShouldntBlock() {
      eventually { shouldBlock should be (false) }
    }

    def block() {
      shouldBlock = true
    }

    def unblock() {
      shouldBlock = false
    }

    def downloadCount = currentDownloadCount.get()
  }
}
