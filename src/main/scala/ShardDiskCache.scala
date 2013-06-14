import java.io.File
import scala.collection.mutable

class ShardDiskCache(downloader: Downloader, maxSimultaneousDownloads: Int = 5) {
  private val installedShards = mutable.Map[Shard, File]()
  private val shardsInProgress = mutable.Set[Shard]()
  private val lock = new Object

  def get(shard: Shard): File = {
    checkForShardInCache(shard) match {
      case Some(file) => file
      case None => download(shard)
    }
  }

  private def checkForShardInCache(shard: Shard): Option[File] = {
    lock.synchronized {
      waitForDownloadToComplete(shard)

      if (installedShards.contains(shard)) {
        return Some(installedShards(shard))
      }

      shardsInProgress += shard
      None
    }
  }

  private def waitForDownloadToComplete(shard: Shard) {
    while (shardsInProgress.contains(shard) || shardsInProgress.size >= maxSimultaneousDownloads) {
      lock.wait()
    }
  }

  private def download(shard: Shard): File = {
    try {
      val shardFile = downloader.download(shard.path)
      lock.synchronized {
        installedShards(shard) = shardFile
        shardFile
      }
    } finally {
      lock.synchronized {
        shardsInProgress -= shard
        lock.notifyAll()
      }
    }
  }
}
