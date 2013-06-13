import java.io.File
import scala.collection.mutable

class ShardDiskCache(downloader: Downloader) {
  private val installedShards = mutable.Map[Shard, File]()
  private val shardsInProgress = mutable.Set[Shard]()
  private val lock = new Object

  def get(shard: Shard): File = {
    lock.synchronized {
      waitForPermissionToDownload(shard)

      if (!installedShards.contains(shard)) {
        download(shard)
      }

      installedShards(shard)
    }
  }

  private def waitForPermissionToDownload(shard: Shard) {
    while (shardsInProgress.contains(shard)) {
      lock.wait()
    }
  }

  private def download(shard: Shard) {
    shardsInProgress += shard
    try {
      installedShards(shard) = downloader.download(shard.path)
    } finally {
      shardsInProgress -= shard
      lock.notifyAll()
    }
  }
}
