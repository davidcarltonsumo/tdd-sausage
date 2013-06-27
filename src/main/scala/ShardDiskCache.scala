import java.io.File
import scala.collection.mutable

class ShardDiskCache(downloader: Downloader) {
  private val cache = mutable.Map[Shard, File]()

  def install(shard: Shard): File = {
    synchronized {
      if (cache.contains(shard)) {
        return cache(shard)
      }
    }

    val downloadedShard = downloader.download(shard.name, shard.path)

    synchronized {
      cache(shard) = downloadedShard
      cache(shard)
    }
  }
}
