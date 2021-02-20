package jp.co.troot.llog

import android.content.res.Configuration
import android.graphics.Point
import android.os.Bundle
import android.view.WindowManager
import android.widget.*
import jp.co.troot.llog.Database.Companion.sqlString
import kotlinx.coroutines.*
import java.util.*

class PhotoListActivity : MyActivity() {
    private var mPhotoSeqNo = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 画面設定
        val config = resources.configuration
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        setContentView(R.layout.activity_photo_list)

        // seqNo取得
        mPhotoSeqNo = intent.getIntExtra("PhotoSeqNo", 0)

        // 写真リストを表示
        asyncDisplayPhotoList()
    }

    // 縮小写真をビューに表示（非同期処理）
    private fun asyncDisplayPhotoList() = CoroutineScope(Dispatchers.IO).launch {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager

        @Suppress("DEPRECATION")
        val disp = wm.defaultDisplay
        val size = Point()
        @Suppress("DEPRECATION")
        disp.getSize(size)
        findViewById<GridLayout>(R.id.gridLayout).columnCount = (size.x - 10) / (148 + 10)

        val db = Database()
        var sql = String.format("SELECT ph_date FROM t_photo WHERE ph_seq_no=%d", mPhotoSeqNo)
        var rs = db.query(sql)
        if (rs.next()) {
            val date = rs.getString("ph_date")
            sql = String.format("SELECT ph_seq_no FROM t_photo WHERE ph_date=%s ORDER BY ph_datetime", sqlString(date))
            rs = db.query(sql)
            while (rs.next()) {
                if (this@PhotoListActivity.isDestroyed) break
                val seqNo = rs.getInt("ph_seq_no")
                val imageView = ImageView(this@PhotoListActivity)
                imageView.tag = seqNo
                imageView.setOnClickListener { v ->
                    // seqNoを呼び出し元画面に戻して終了
                    setResult((v.tag as Int))
                    finish()
                }
                imageView.setImageBitmap(MyUtils.getBitmapFromServer(seqNo, 148))
                imageView.setPadding(5, 5, 5, 5)

                // UIスレッドで縮小写真を追加
                runOnUiThread {
                    findViewById<GridLayout>(R.id.gridLayout).addView(imageView)
                }
            }
        }
        db.close()
    }
}