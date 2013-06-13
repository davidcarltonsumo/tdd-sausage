import java.io.File
import org.junit.runner.RunWith
import org.scalatest.concurrent.Eventually
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{BeforeAndAfterEach, WordSpec}
import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class ShardDiskCacheTest extends WordSpec with ShouldMatchers
with BeforeAndAfterEach with Eventually {
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

      downloader.downloadCount("path") should equal (1)
    }

    "install multiple shards" in {
      cache.get(Shard("name-1", "path-1")) should equal (new File("installed-path-1"))
      cache.get(Shard("name-2", "path-2")) should equal (new File("installed-path-2"))
      cache.get(Shard("name-3", "path-3")) should equal (new File("installed-path-3"))
    }

    "install multiple shards in parallel" in {
      downloader.delay("path-1")
      downloader.delay("path-2")
      downloader.delay("path-3")

      val requester1 = backgroundRequester(1)
      val requester2 = backgroundRequester(2)
      val requester3 = backgroundRequester(3)

      eventually { requester1 should be ('started) }
      eventually { requester2 should be ('started) }
      eventually { requester3 should be ('started) }

      downloader.resume("path-1")
      downloader.resume("path-2")
      downloader.resume("path-3")

      eventually { requester1 should be ('done) }
      eventually { requester2 should be ('done) }
      eventually { requester3 should be ('done) }

      requester1.result should equal (new File("installed-path-1"))
      requester2.result should equal (new File("installed-path-2"))
      requester3.result should equal (new File("installed-path-3"))
    }

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
    val downloadCount = mutable.Map[String, Int]()
    val delayedDownloads = mutable.Set[String]()

    def download(path: String): File = {
      waitForPermissionToDownload(path)
      downloadCount(path) = downloadCount.getOrElse(path, 0) + 1
      new File("installed-" + path)
    }

    def waitForPermissionToDownload(path: String) {
      while (delayedDownloads.contains(path)) {
        Thread.sleep(10)
      }
    }

    def delay(path: String) {
      delayedDownloads += path
    }

    def resume(path: String) {
      delayedDownloads -= path
    }
  }

  class BackgroundRequester(shard: Shard) {
    private var started = false
    private var resultOption: Option[File] = None

    def start() {
      new Thread {
        override def run() {
          started = true
          resultOption = Some(cache.get(shard))
        }
      }.start()
    }

    def isStarted: Boolean = started
    def isDone: Boolean = resultOption.isDefined
    def result: File = resultOption.get
  }

  def backgroundRequester(i: Int): BackgroundRequester = {
    val requester = new BackgroundRequester(Shard(f"name-$i", f"path-$i"))
    requester.start()
    requester
  }

  override protected def beforeEach() {
    downloader = new StubDownloader
    cache = new ShardDiskCache(downloader)
  }
}
