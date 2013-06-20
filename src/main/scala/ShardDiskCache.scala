import java.io.File
import scala.collection.mutable

class ShardDiskCache(downloader: Downloader) {
  private val cache = mutable.Map[Shard, File]()

  def install(shard: Shard): File = {
    cache.getOrElseUpdate(shard, downloader.download(shard.name, shard.path))
  }
}
