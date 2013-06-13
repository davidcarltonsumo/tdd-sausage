import java.io.File
import scala.collection.mutable

class ShardDiskCache(downloader: Downloader) {
  private val installedShards = mutable.Map[Shard, File]()

  def get(shard: Shard): File = {
    if (!installedShards.contains(shard)) {
      installedShards(shard) = downloader.download(shard.path)
    }
    installedShards(shard)
  }
}
