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
      cache.get(shard(1)) should equal (file(1))
    }

    "not re-install a shard that is already installed" in {
      cache.get(shard(1))
      cache.get(shard(1)) should equal (file(1))

      downloader.downloadCount(1) should equal (1)
    }

    "install multiple shards" in {
      cache.get(shard(1)) should equal (file(1))
      cache.get(shard(2)) should equal (file(2))
      cache.get(shard(3)) should equal (file(3))
    }

    "install multiple shards in parallel" in {
      downloader.delay(1, 2, 3)

      val requester1 = backgroundRequester(1)
      val requester2 = backgroundRequester(2)
      val requester3 = backgroundRequester(3)

      eventually { downloader.requestCount(1) should be (1) }
      eventually { downloader.requestCount(2) should be (1) }
      eventually { downloader.requestCount(3) should be (1) }

      downloader.resume(1, 2, 3)

      eventually { requester1 should be ('done) }
      eventually { requester2 should be ('done) }
      eventually { requester3 should be ('done) }

      requester1.result should equal (file(1))
      requester2.result should equal (file(2))
      requester3.result should equal (file(3))
    }

    "not download a shard that's in the process of being downloaded" in {
      downloader.delay(1)

      val requester1 = backgroundRequester(1)
      val requester2 = backgroundRequester(1)

      downloader.resume(1)

      eventually { requester1 should be ('done) }
      eventually { requester2 should be ('done) }

      requester1.result should equal (file(1))
      requester2.result should equal (file(1))

      downloader.downloadCount(1) should equal (1)
    }

    "bound the number of simultaneous downloads" in {
      downloader.delay(1, 2, 3, 4, 5, 6)

      val requester1 = backgroundRequester(1)
      val requester2 = backgroundRequester(2)
      backgroundRequester(3)
      backgroundRequester(4)
      backgroundRequester(5)

      eventually { downloader.requestCount should be (5) }

      val requester6 = backgroundRequester(6)
      Thread.sleep(100)
      downloader.requestCount(6) should be (0)

      downloader.resume(1)

      eventually { downloader.requestCount(6) should be (1) }
      requester1 should be ('done)

      downloader.resume(2, 3, 4, 5, 6)

      eventually { requester2 should be ('done) }
      eventually { requester6 should be ('done) }
    }

    "propagate errors when trying to download shards" in {
      downloader.setError(1)

      intercept[RuntimeException] { cache.get(shard(1)) }
    }

    "try a second time if downloading the first time leads to an error" in {
      downloader.setError(1)

      intercept[RuntimeException] { cache.get(shard(1)) }
      cache.get(shard(1)) should equal (file(1))

      downloader.downloadCount(1) should equal (2)
    }

    "not have errors count against the maximum number of simultaneous downloads" in {
      downloader.setError(1)

      intercept[RuntimeException] { cache.get(shard(1)) }

      downloader.delay(2, 3, 4, 5, 6)

      backgroundRequester(2)
      backgroundRequester(3)
      backgroundRequester(4)
      backgroundRequester(5)
      val requester6 = backgroundRequester(6)

      eventually { downloader.requestCount should be (6) }

      downloader.resume(2, 3, 4, 5, 6)

      eventually { requester6 should be ('done) }
    }

    // Evict old shards if the disk is full.
    // Initialize the cache from disk on startup.
    // Not get confused if the process gets terminated while we're downloading.
    // Provide a prefetch mechanism where we say that we'll want something in the future?
    // Questions:
    // * Should Shard store the name?
    // * Should Downloader take the name as well as the path?
    // * Pass in the bound in the number of downloads as a property?
  }

  class StubDownloader extends Downloader {
    private val requestCounts = mutable.Map[String, Int]()
    private val downloadCounts = mutable.Map[String, Int]()
    private val delayedDownloads = mutable.Set[String]()
    private val errorRequested = mutable.Set[String]()

    def download(path: String): File = {
      requestCounts(path) = requestCounts.getOrElse(path, 0) + 1
      waitForPermissionToDownload(path)
      downloadCounts(path) = downloadCounts.getOrElse(path, 0) + 1

      if (errorRequested.contains(path)) {
        errorRequested -= path
        throw new RuntimeException(f"Error while downloading $path")
      } else {
        new File(f"installed-$path")
      }
    }

    def waitForPermissionToDownload(path: String) {
      while (delayedDownloads.contains(path)) {
        Thread.sleep(10)
      }
    }

    def delay(is: Int*) {
      is.foreach { delayedDownloads += path(_) }
    }

    def resume(is: Int*) {
      is.foreach { delayedDownloads -= path(_) }
    }

    def setError(i: Int) {
      errorRequested += path(i)
    }

    def downloadCount(i: Int): Int = {
      downloadCounts(path(i))
    }

    def requestCount(i: Int): Int = {
      requestCounts.getOrElse(path(i), 0)
    }

    def requestCount: Int = {
      requestCounts.values.sum
    }
  }

  class BackgroundRequester(shard: Shard) {
    private var started = false
    private var resultOption: Option[File] = None

    def start() {
      new Thread(f"BackgroundRequester-$shard") {
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
    val requester = new BackgroundRequester(shard(i))
    requester.start()
    eventually { requester should be ('started) }
    requester
  }

  def shard(i: Int): Shard = {
    Shard(f"name-$i", path(i))
  }

  def path(i: Int): String = {
    f"path-$i"
  }

  def file(i: Int): File = {
    new File(f"installed-path-$i")
  }

  override protected def beforeEach() {
    downloader = new StubDownloader
    cache = new ShardDiskCache(downloader)
  }
}
