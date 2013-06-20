import java.io.File

trait Downloader {
  def download(name: String, path: String): File
}
