package com.xiangweixin.myownstudy.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.ByteArrayOutputStream

private const val TAG = "BitmapUtils"

object BitmapUtils {

    fun toByteArray(bitmap: Bitmap): ByteArray {
        val os = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
        return os.toByteArray()
    }

    fun mirror(rawBitmap: Bitmap): Bitmap {
        var matrix = Matrix()
        matrix.postScale(-1f, 1f)
        return Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
    }

    fun savePic(data: ByteArray?, isMirror: Boolean = false, onSuccess: (savedPath: String, time: String) -> Unit, onFailed: (msg: String) -> Unit) {
        Thread(Runnable {
            try {
                val temp = System.currentTimeMillis()
                val picFile = FileUtil.createCameraFile("camera2")
                if (picFile != null && data != null) {

                    val rawBitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                    val resultBitmap = if (isMirror) mirror(rawBitmap) else rawBitmap
//                    picFile.sink().buffer().write(toByteArray(resultBitmap)).close()
                    onSuccess("${picFile.absolutePath}", "${System.currentTimeMillis() - temp}")

                    logi(TAG, "图片已保存! 耗时：${System.currentTimeMillis() - temp}    路径：  ${picFile.absolutePath}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onFailed("${e.message}")
            }
        }).start()
    }

}