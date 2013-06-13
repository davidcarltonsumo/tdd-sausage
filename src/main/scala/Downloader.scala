import java.io.File

trait Downloader {
  def download(path: String): File
}
