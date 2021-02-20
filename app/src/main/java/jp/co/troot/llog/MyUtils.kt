package jp.co.troot.llog

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.DisplayMetrics
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

internal object MyUtils {
    // サーバから写真を取得
    fun getBitmapFromServer(date: Date, folderName: String, fileName: String): Bitmap {
        return getBitmapFromURL(getServerURL(date, folderName, fileName))
    }

    // サーバから写真を取得（縮小）
    fun getBitmapFromServer(date: Date, folderName: String, fileName: String, maxSize: Int): Bitmap {
        val cal = Calendar.getInstance()
        cal.time = date
        val url = String.format("https://llog.troot.co.jp/ph_img.php?year=%d&folder=%s&file=%s&m=%d&c=1", cal[Calendar.YEAR], URLEncoder.encode(folderName, "UTF-8"), URLEncoder.encode(fileName, "UTF-8"), maxSize)
        return getBitmapFromURL(url)
    }

    // サーバから写真を取得（seqNo指定）
    fun getBitmapFromServer(seqNo: Int, maxSize: Int): Bitmap {
        val url = String.format("https://llog.troot.co.jp/ph_img.php?seq_no=%d&m=%d&c=1", seqNo, maxSize)
        return getBitmapFromURL(url)
    }

    // photoStream取得
    fun getPhotoStreamFromServer(date: Date, folderName: String, fileName: String): InputStream {
        return getPhotoStreamFromURL(getServerURL(date, folderName, fileName))
    }

    // サーバから写真を取得（URL指定）
    fun getBitmapFromURL(src: String): Bitmap {
        return BitmapFactory.decodeStream(getPhotoStreamFromURL(src))
    }

    // サーバから写真を取得（URL指定）
    private fun getServerURL(date: Date, folderName: String, fileName: String): String {
        val cal = Calendar.getInstance()
        cal.time = date
        return String.format("https://inet.troot.co.jp/photo/%d/%s/%s", cal[Calendar.YEAR], URLEncoder.encode(folderName, "UTF-8"), URLEncoder.encode(fileName, "UTF-8"))
    }

    // photoStream取得
    private fun getPhotoStreamFromURL(src: String): InputStream {
        val url = URL(src)
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        return connection.inputStream
    }

    // JSONでサーバにリクエスト
    fun requestJSON(url: String, requestData: String): String {
        val `object` = URL(url)
        val con = `object`.openConnection() as HttpURLConnection
        con.doOutput = true
        con.doInput = true
        con.setRequestProperty("Content-Type", "application/json")
        con.setRequestProperty("Accept", "application/json")
        con.requestMethod = "POST"
        val wr = OutputStreamWriter(con.outputStream)
        wr.write(requestData)
        wr.flush()
        val sb = StringBuilder()
        val httpResult = con.responseCode
        if (httpResult == HttpURLConnection.HTTP_OK) {
            val br = BufferedReader(InputStreamReader(con.inputStream, StandardCharsets.UTF_8))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                sb.append(line)
                sb.append("\n")
            }
            br.close()
        }
        return sb.toString()
    }

    // 画面のMetricsを取得
    fun getDisplayMetrics(activity: Activity): DisplayMetrics {
        val displayMetrics = DisplayMetrics()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val display = activity.display
            display?.getRealMetrics(displayMetrics)
        } else {
            @Suppress("DEPRECATION")
            val display = activity.windowManager.defaultDisplay
            @Suppress("DEPRECATION")
            display.getMetrics(displayMetrics)
        }

        return displayMetrics
    }
}