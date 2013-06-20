import java.io.File

class ShardDiskCache(downloader: Downloader) {
  def install(shard: Shard): File = {
    downloader.download(shard.name, shard.path)
  }
}
