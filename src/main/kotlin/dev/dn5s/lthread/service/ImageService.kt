package dev.dn5s.lthread.service

import dev.dn5s.lthread.config.AppConfig
import dev.dn5s.lthread.model.Post
import net.coobird.thumbnailator.Thumbnails
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import javax.imageio.ImageIO

@Service
class ImageService(
    private val appConfig: AppConfig
) {

    companion object {
        private const val THUMBNAIL_WIDTH = 250
        private const val THUMBNAIL_HEIGHT = 250
        private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif")
        private val ALLOWED_MIME_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/gif"
        )
    }

    init {
        // Ensure directories exist
        createDirectories()
    }

    private fun createDirectories() {
        val imagesDir = File(appConfig.storage.imagesPath)
        val thumbnailsDir = File(appConfig.storage.imagesPath, appConfig.storage.thumbnailsSubdir)

        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        if (!thumbnailsDir.exists()) {
            thumbnailsDir.mkdirs()
        }
    }

    /**
     * Validate and process uploaded image
     * @return Pair of (original filename, thumbnail filename)
     */
    fun processImage(file: MultipartFile): Pair<String, String> {
        validateImage(file)

        val extension = getFileExtension(file.originalFilename ?: "")
        val timestamp = System.currentTimeMillis()
        val randomId = UUID.randomUUID().toString().take(8)
        val filename = "${timestamp}_$randomId.$extension"

        val originalPath = saveOriginalImage(file, filename)
        val thumbnailFilename = generateThumbnail(originalPath, filename)

        return Pair(filename, thumbnailFilename)
    }

    /**
     * Validate image file
     */
    private fun validateImage(file: MultipartFile) {
        // Check if the file is empty
        if (file.isEmpty) {
            throw IllegalArgumentException("Image file is required")
        }

        // Check file size (20MB max)
        if (file.size > Post.MAX_FILE_SIZE) {
            throw IllegalArgumentException("File size exceeds maximum of 20MB")
        }

        // Check MIME type
        val contentType = file.contentType?.lowercase()
        if (contentType == null || contentType !in ALLOWED_MIME_TYPES) {
            throw IllegalArgumentException("Invalid file type. Only JPEG, PNG, and GIF are allowed")
        }

        // Check file extension
        val extension = getFileExtension(file.originalFilename ?: "")
        if (extension !in ALLOWED_EXTENSIONS) {
            throw IllegalArgumentException("Invalid file extension")
        }

        // Check image dimensions
        try {
            val image = ImageIO.read(file.inputStream) ?: throw IllegalArgumentException("Invalid image file")
            if (image.width > Post.MAX_IMAGE_DIMENSION || image.height > Post.MAX_IMAGE_DIMENSION) {
                throw IllegalArgumentException("Image dimensions exceed maximum of ${Post.MAX_IMAGE_DIMENSION}x${Post.MAX_IMAGE_DIMENSION}")
            }
        } catch (e: Exception) {
            if (e is IllegalArgumentException) throw e
            throw IllegalArgumentException("Failed to process image file")
        }
    }

    /**
     * Save the original image
     */
    private fun saveOriginalImage(file: MultipartFile, filename: String): Path {
        val baseDir: Path = Paths.get(appConfig.storage.imagesPath).toAbsolutePath().normalize()
        val resolved: Path = baseDir.resolve(filename).normalize()

        if (!resolved.startsWith(baseDir)) {
            throw IllegalArgumentException("Invalid file path")
        }
        Files.createDirectories(baseDir)

        file.inputStream.use { input ->
            Files.copy(input, resolved)
        }
        return resolved
    }

    /**
     * Generate thumbnail
     */
    private fun generateThumbnail(originalPath: Path, originalFilename: String): String {
        val thumbnailFilename = "thumb_$originalFilename"
        val baseDir: Path = Paths.get(appConfig.storage.imagesPath).toAbsolutePath().normalize()
        val thumbsDir: Path = baseDir.resolve(appConfig.storage.thumbnailsSubdir).normalize()

        if (!thumbsDir.startsWith(baseDir)) {
            throw IllegalArgumentException("Invalid thumbnails directory")
        }
        Files.createDirectories(thumbsDir)

        val thumbnailPath: Path = thumbsDir.resolve(thumbnailFilename).normalize()
        if (!thumbnailPath.startsWith(thumbsDir)) {
            throw IllegalArgumentException("Invalid thumbnail file path")
        }

        Thumbnails.of(originalPath.toFile())
            .size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
            .keepAspectRatio(true)
            .toFile(thumbnailPath.toFile())

        return thumbnailFilename
    }

    /**
     * Get file extension
     */
    private fun getFileExtension(filename: String): String {
        val lastDot = filename.lastIndexOf('.')
        if (lastDot == -1) {
            throw IllegalArgumentException("File has no extension")
        }
        return filename.substring(lastDot + 1).lowercase()
    }

    /**
     * Delete image files (original and thumbnail)
     */
    fun deleteImages(filename: String?) {
        if (filename == null) return

        val baseDir: Path = Paths.get(appConfig.storage.imagesPath).toAbsolutePath().normalize()
        val originalPath: Path = baseDir.resolve(filename).normalize()
        if (originalPath.startsWith(baseDir)) {
            Files.deleteIfExists(originalPath)
        }

        val thumbsDir: Path = baseDir.resolve(appConfig.storage.thumbnailsSubdir).normalize()
        val thumbnailFilename = "thumb_$filename"
        val thumbnailPath: Path = thumbsDir.resolve(thumbnailFilename).normalize()
        if (thumbsDir.startsWith(baseDir) && thumbnailPath.startsWith(thumbsDir)) {
            Files.deleteIfExists(thumbnailPath)
        }
    }

    /**
     * Check if the image exists
     */
    fun imageExists(filename: String): Boolean {
        val baseDir: Path = Paths.get(appConfig.storage.imagesPath).toAbsolutePath().normalize()
        val path: Path = baseDir.resolve(filename).normalize()
        return path.startsWith(baseDir) && Files.exists(path)
    }

    /**
     * Check if a thumbnail exists
     */
    fun thumbnailExists(filename: String): Boolean {
        val baseDir: Path = Paths.get(appConfig.storage.imagesPath).toAbsolutePath().normalize()
        val thumbsDir: Path = baseDir.resolve(appConfig.storage.thumbnailsSubdir).normalize()
        val path: Path = thumbsDir.resolve(filename).normalize()
        return thumbsDir.startsWith(baseDir) && path.startsWith(thumbsDir) && Files.exists(path)
    }

    /**
     * Get the full image path
     */
    fun getImagePath(filename: String): Path {
        val baseDir: Path = Paths.get(appConfig.storage.imagesPath).toAbsolutePath().normalize()
        val path: Path = baseDir.resolve(filename).normalize()
        if (!path.startsWith(baseDir)) {
            throw IllegalArgumentException("Invalid image path")
        }
        return path
    }

    /**
     * Get a full thumbnail path
     */
    fun getThumbnailPath(filename: String): Path {
        val baseDir: Path = Paths.get(appConfig.storage.imagesPath).toAbsolutePath().normalize()
        val thumbsDir: Path = baseDir.resolve(appConfig.storage.thumbnailsSubdir).normalize()
        val path: Path = thumbsDir.resolve(filename).normalize()
        if (!thumbsDir.startsWith(baseDir) || !path.startsWith(thumbsDir)) {
            throw IllegalArgumentException("Invalid thumbnail path")
        }
        return path
    }
}