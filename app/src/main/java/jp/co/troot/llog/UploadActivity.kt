package jp.co.troot.llog

import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Xml
import android.widget.Button
import android.widget.TextView
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream
import java.io.FileReader
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

class UploadActivity : MyActivity() {
    private val mPhotoFiles: MutableList<String> = ArrayList()
    private val mUploadPhotoFiles: MutableList<String> = ArrayList()
    private val mGpsFiles: MutableList<String> = ArrayList()
    private val mUploadGpsFiles: MutableList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 画面設定
        setContentView(R.layout.activity_upload)

        // 本体内の写真取得
        getPhotoFiles()
        findViewById<TextView>(R.id.textViewPhotoFileNum2).text = String.format("：%d 件", mPhotoFiles.size)

        getGpsFiles("/MyTracks/gpx")
        getGpsFiles("/com.kamoland/ytlog")
        findViewById<TextView>(R.id.textViewGpsFileNum2).text = String.format("：%d 件", mGpsFiles.size)

        // サーバ上のファイルを取得（非同期）
        asyncGetUploadFiles()

        // アップロードボタン処理
        findViewById<Button>(R.id.buttonUpload).setOnClickListener {
            findViewById<Button>(R.id.buttonUpload).isEnabled = false
            asyncUploadFiles()
        }
    }

    // 本体内の写真取得
    private fun getPhotoFiles() {
        @Suppress("DEPRECATION")
        val cameraDir = Environment.getExternalStorageDirectory().absolutePath + "/DCIM/Camera"
        val dir = File(cameraDir)
        val filelist = dir.listFiles()
        if (filelist != null) {
            for (file in filelist) {
                if (file.isDirectory) {
                    if (file.name.startsWith("IMG_")) {
                        val filelist2 = File(cameraDir + "/" + file.name).listFiles()
                        if (filelist2 != null) {
                            for (file2 in filelist2) {
                                if (file2.name.endsWith("_COVER.jpg")) {
                                    mPhotoFiles.add(file2.absolutePath)
                                }
                            }
                        }
                    }
                } else if (file.isFile) {
                    if (getSuffix(file.name) == "jpg") {
                        mPhotoFiles.add(file.absolutePath)
                    }
                }
            }
        }
    }

    // 本体内のGPSデータ取得
    private fun getGpsFiles(path: String) {
        @Suppress("DEPRECATION")
        val gpsDir = Environment.getExternalStorageDirectory().absolutePath + path
        val dir = File(gpsDir)
        val filelist = dir.listFiles()
        if (filelist != null) {
            for (file in filelist) {
                if (getSuffix(file.name) == "gpx") {
                    mGpsFiles.add(file.absolutePath)
                }
            }
        }
    }

    // 非同期でサーバから写真とGPSデータを取得
    @Suppress("BlockingMethodInNonBlockingContext")
    private fun asyncGetUploadFiles() = CoroutineScope(Dispatchers.IO).launch {
        // 未アップロードの写真取得
        var postParams = JSONArray()
        for (photo in mPhotoFiles) {
            val exif = ExifInterface(photo)
            var date = exif.getAttribute(ExifInterface.TAG_DATETIME)
            date = if (date != null) {
                date.replaceFirst("([0-9]+):([0-9]+):([0-9]+) ([0-9]+):([0-9]+):([0-9]+)".toRegex(), "$1-$2-$3 $4:$5:$6")
            } else {
                val file = File(photo)
                val date2 = Date(file.lastModified())
                val sdf = SimpleDateFormat("yyyy-MM-dd kk:mm:ss", Locale.US)
                sdf.format(date2)
            }
            postParams.put(date)
        }
        var result = JSONArray(MyUtils.requestJSON("https://llog.troot.co.jp/ph_upload_check.php", postParams.toString()))
        if (mPhotoFiles.size == result.length()) {
            for (i in mPhotoFiles.indices) {
                if (result[i] == "0") {
                    mUploadPhotoFiles.add(mPhotoFiles[i])
                }
            }
        }

        // 未アップロードのGPSデータ取得
        postParams = JSONArray()
        val parser = Xml.newPullParser()
        for (gpsFile in mGpsFiles) {
            val file = File(gpsFile)
            val filereader = FileReader(file)
            parser.setInput(filereader)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "time") {
                    var time = parser.nextText()
                    time = time.substring(0, 19)
                    time = time.replace('T', ' ')
                    postParams.put(time)
                    break
                }
                eventType = parser.next()
            }
        }
        result = JSONArray(MyUtils.requestJSON("https://llog.troot.co.jp/gl_upload_check.php", postParams.toString()))
        if (mGpsFiles.size == result.length()) {
            for (i in mGpsFiles.indices) {
                if (result[i] == "0") {
                    mUploadGpsFiles.add(mGpsFiles[i])
                }
            }
        }

        // UIスレッドで件数を表示する
        runOnUiThread {
            var textView = findViewById<TextView>(R.id.textViewUploadPhotoNum2)
            textView.text = String.format("：%d 件", mUploadPhotoFiles.size)
            textView.setTextColor(if (mUploadPhotoFiles.size == 0) Color.BLACK else Color.RED)

            textView = findViewById(R.id.textViewUploadGpsNum2)
            textView.text = String.format("：%d 件", mUploadGpsFiles.size)
            textView.setTextColor(if (mUploadGpsFiles.size == 0) Color.BLACK else Color.RED)
            if (mUploadPhotoFiles.size + mUploadGpsFiles.size > 0) {
                findViewById<Button>(R.id.buttonUpload).isEnabled = true
            }
        }
    }

    // 非同期で写真とGPSデータをサーバにアップロード
    @Suppress("BlockingMethodInNonBlockingContext")
    private fun asyncUploadFiles() = CoroutineScope(Dispatchers.IO).launch {
        var postParams: JSONObject

        // 写真をアップロード
        for (uploadPhotoFile in mUploadPhotoFiles) {
            postParams = JSONObject()
            val upfile = File(uploadPhotoFile)
            var photoUri = Uri.fromFile(upfile)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                photoUri = MediaStore.setRequireOriginal(photoUri)
            }
            val fis = contentResolver.openInputStream(photoUri)
            val buf = ByteArray(fis!!.available())
            if (fis.read(buf) == buf.size) postParams.put("data", Base64.encodeToString(buf, Base64.DEFAULT))
            MyUtils.requestJSON("https://llog.troot.co.jp/ph_upload2.php", postParams.toString())
        }

        // GPSデータをアップロード
        for (uploadPhotoFile in mUploadGpsFiles) {
            postParams = JSONObject()
            val upfile = File(uploadPhotoFile)
            val buf = ByteArray(upfile.length().toInt())
            val fis = FileInputStream(upfile)
            if (fis.read(buf) == buf.size) postParams.put("data", String(buf, StandardCharsets.UTF_8))
            MyUtils.requestJSON("https://llog.troot.co.jp/gl_upload.php", postParams.toString())
        }

        // 処理が終わったら終了
        runOnUiThread {
            finish()
        }
    }

    companion object {
        private fun getSuffix(fileName: String?): String? {
            if (fileName == null) return null
            val dot = fileName.lastIndexOf(".")
            return if (dot != -1) {
                fileName.substring(dot + 1)
            } else fileName
        }
    }
}