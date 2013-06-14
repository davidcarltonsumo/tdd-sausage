import java.io.File
import scala.collection.mutable

class ShardDiskCache(downloader: Downloader, maxSimultaneousDownloads: Int = 5) {
  private val installedShards = mutable.Map[Shard, File]()
  private val shardsInProgress = mutable.Set[Shard]()
  private val lock = new Object

  def get(shard: Shard): File = {
    lock.synchronized {
      while (shardsInProgress.contains(shard)) {
        lock.wait()
      }

      if (installedShards.contains(shard)) {
        return installedShards(shard)
      }

      shardsInProgress += shard
    }

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
