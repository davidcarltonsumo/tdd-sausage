import java.io.File
import java.util.concurrent.atomic.AtomicInteger
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
      sut.install(shard(1)) should equal (installedFile(1))

      downloader.actions should equal ("d(name1,path1);")
    }

    "Not re-download a shard that is already installed" in {
      sut.install(shard(1)) should equal (installedFile(1))
      sut.install(shard(1)) should equal (installedFile(1))

      downloader.actions should equal ("d(name1,path1);")
    }

    "Install a bunch of shards" in {
      sut.install(shard(1)) should equal (installedFile(1))
      sut.install(shard(2)) should equal (installedFile(2))
      sut.install(shard(3)) should equal (installedFile(3))
    }

    "Install multiple shards in parallel" in {
      downloader.block()

      val installer1 = new BackgroundInstaller(shard(1))
      val installer2 = new BackgroundInstaller(shard(2))
      val installer3 = new BackgroundInstaller(shard(3))

      installer1.start()
      installer2.start()
      installer3.start()

      eventually { downloader.downloadCount should equal (3) }

      downloader.unblock()

      eventually { installer1 should be ('done) }
      eventually { installer2 should be ('done) }
      eventually { installer3 should be ('done) }

      installer1.result should equal (installedFile(1))
      installer2.result should equal (installedFile(2))
      installer3.result should equal (installedFile(3))
    }

    // Not download a shard that's in the process of being downloaded.
    // Bound the number of simultaneous downloads.
    // Evict old shards if the disk is full.
    // Initialize the cache from disk on startup.
    // Not get confused if the process gets terminated while we're downloading.
    // Provide a prefetch mechanism where we say that we'll want something in the future?

    // Problems:
    // * ShardDiskCache isn't thread safe
    // * Shard installation uses the path to fetch but stores it only at the name; is that
    //   a problem?
  }


  def shard(i: Int): Shard = {
    Shard(f"name$i", f"path$i")
  }

  def installedFile(i: Int): File = {
    new File(f"installed/name$i")
  }

  override protected def beforeEach() {
    downloader = new StubDownloader
    sut = new ShardDiskCache(downloader)
  }

  class BackgroundInstaller(shard: Shard) {
    var resultOption: Option[File] = None

    def start() {
      new Thread() {
        override def run() {
          resultOption = Some(sut.install(shard))
        }
      }.start()
    }

    def isDone: Boolean = resultOption.isDefined
    def result: File = resultOption.get
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
