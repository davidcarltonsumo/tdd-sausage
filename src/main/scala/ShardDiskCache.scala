import java.io.File
import scala.collection.mutable

class ShardDiskCache(downloader: Downloader) {
  // All access to cache and downloadsInProgress should be guarded by lock
  private val lock = new Object
  private val cache = mutable.Map[Shard, File]()
  private val downloadsInProgress = mutable.Set[Shard]()

  def install(shard: Shard): File = {
    lookupInCache(shard).getOrElse(
      insertIntoCache(shard, downloader.download(shard.name, shard.path)
      )
    )
  }

  private def lookupInCache(shard: Shard): Option[File] = {
    lock.synchronized {
      while (downloadsInProgress.contains(shard)) {
        lock.wait()
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
    lock.synchronized {
      downloadsInProgress -= shard
      lock.notifyAll()
      cache(shard) = downloadedShard
      cache(shard)
    }
  }
}
