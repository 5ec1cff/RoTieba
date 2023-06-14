package io.github.a13e300.ro_tieba

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.github.panpf.sketch.request.DownloadRequest
import com.github.panpf.sketch.request.DownloadResult
import io.github.a13e300.ro_tieba.models.Post
import io.github.a13e300.ro_tieba.models.TiebaThread
import io.github.a13e300.ro_tieba.ui.photo.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URLConnection

object PhotoUtils {
    private const val MY_PATH = "RoTieba"

    private fun saveImage(fileName: String, context: Context, input: InputStream) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageQ(context, fileName, input)
        } else {
            saveImagePreQ(context, fileName, input)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImageQ(context: Context, filename: String, input: InputStream) {
        var fos: OutputStream?
        val imageUri: Uri
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/${MY_PATH}"
            )
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val contentResolver = context.contentResolver

        contentResolver.also { resolver ->
            imageUri =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)!!
            fos = imageUri.let { resolver.openOutputStream(it) }
            fos?.use { input.copyTo(it) }

            contentValues.clear()
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(imageUri, contentValues, null, null)
        }
    }

    private fun saveImagePreQ(context: Context, fileName: String, input: InputStream) {
        val imagesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val dir = File(imagesDir, MY_PATH)
        if (!dir.exists()) dir.mkdir()
        val image = File(dir, fileName)
        FileOutputStream(image).use { fos ->
            input.copyTo(fos)
        }
        context.sendBroadcast(
            Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(image))
        )
    }

    private fun verifyStoragePermissions(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
            return false
        }
        return true
    }

    suspend fun downloadPhoto(
        activity: Activity,
        photo: Photo,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Throwable) -> Unit)? = null
    ) {
        if (verifyStoragePermissions(activity)) {
            val result = DownloadRequest(
                activity,
                photo.url
            )
                .execute()
            if (result is DownloadResult.Success) {
                kotlin.runCatching {
                    withContext(Dispatchers.IO) {
                        BufferedInputStream(result.data.data.newInputStream()).use { inputStream ->
                            // TODO: maybe tieba data knows the type of the image
                            val ext = inputStream.guessExtension()
                            val key = when (val source = photo.source) {
                                is Post -> "rotieba_t${source.tid}_p${source.postId}_f${source.floor}_c${photo.order}"
                                is TiebaThread -> "rotieba_t${source.tid}_p${source.postId}_f1_c${photo.order}"
                                else -> "rotieba"
                            }
                            saveImage(
                                "${key}_${System.currentTimeMillis()}.$ext",
                                activity,
                                inputStream
                            )
                        }
                    }
                }.onSuccess {
                    onSuccess?.invoke()
                }.onFailure {
                    onFailure?.invoke(it)
                }
            } else if (result is DownloadResult.Error) {
                onFailure?.invoke(result.throwable)
            }
        }
    }

    suspend fun sharePhoto(
        context: Context,
        photo: Photo,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Throwable) -> Unit)? = null
    ) {
        val result = DownloadRequest(
            context,
            photo.url
        )
            .execute()
        if (result is DownloadResult.Success) {
            kotlin.runCatching {
                withContext(Dispatchers.IO) {
                    BufferedInputStream(result.data.data.newInputStream()).use { inputStream ->
                        val mime = URLConnection.guessContentTypeFromStream(inputStream)
                        val file = File(context.filesDir, "share/share.png")
                        file.parentFile?.mkdirs()
                        file.outputStream().use { inputStream.copyTo(it) }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(
                                Intent.EXTRA_STREAM,
                                FileProvider.getUriForFile(
                                    context,
                                    "${BuildConfig.APPLICATION_ID}.fp",
                                    file
                                )
                            )
                            type = mime
                        }.let {
                            Intent.createChooser(
                                it,
                                context.getString(R.string.share_photo_title)
                            )
                        }
                        context.startActivity(intent)
                    }
                }
            }.onSuccess {
                onSuccess?.invoke()
            }.onFailure {
                onFailure?.invoke(it)
            }
        } else if (result is DownloadResult.Error) {
            onFailure?.invoke(result.throwable)
        }
    }
}