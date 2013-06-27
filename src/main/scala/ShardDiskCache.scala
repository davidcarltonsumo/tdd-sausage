import java.io.File
import scala.collection.mutable

class ShardDiskCache(downloader: Downloader) {
  private val cache = mutable.Map[Shard, File]()
  private val downloadsInProgress = mutable.Set[Shard]()

  def install(shard: Shard): File = {
    lookupInCache(shard).getOrElse(
      insertIntoCache(shard, downloader.download(shard.name, shard.path)
      )
    )
  }

  private def lookupInCache(shard: Shard): Option[File] = {
    synchronized {
      while (downloadsInProgress.contains(shard)) {
        wait()
      }

      if (cache.contains(shard)) {
        Some(cache(shard))
      } else {
        downloadsInProgress += shard
        None
      }
    }
  }

  private def insertIntoCache(shard: Shard, downloadedShard: File): File = {
    synchronized {
      downloadsInProgress -= shard
      notifyAll()
      cache(shard) = downloadedShard
      cache(shard)
    }
  }
}
