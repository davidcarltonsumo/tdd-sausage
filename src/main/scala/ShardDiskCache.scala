import java.io.File

class ShardDiskCache(downloader: Downloader) {
  def get(shard: Shard): File = {
    downloader.download(shard.path)
  }
}
