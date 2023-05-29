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
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

// https://developer.android.com/training/data-storage/shared/media
// https://stackoverflow.com/a/66817176
object StorageUtils {
    private const val MY_PATH = "RoTieba"

    fun saveImage(fileName: String, context: Context, input: InputStream) {
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
            fos?.also { input.copyTo(it) }

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
        val fos = FileOutputStream(image)
        input.copyTo(fos)
        context.sendBroadcast(
            Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(image))
        )
    }

    fun verifyStoragePermissions(activity: Activity): Boolean {
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
}