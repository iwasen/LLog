package jp.co.troot.llog

import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.webkit.WebView
import java.util.*

class PhotoZoomActivity : MyActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 画面設定
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_photo_zoom)

        // ビューの設定
        val webView = findViewById<WebView>(R.id.webView)
        val ws = webView.settings
        ws.builtInZoomControls = true
        ws.setSupportZoom(true)
        ws.displayZoomControls = false

        // サーバから取得した写真をビューに表示
        val db = Database()
        val seqNo = intent.getSerializableExtra("PhotoSeqNo") as Int
        val sql = String.format("SELECT ph_date,ph_folder_name,ph_file_name FROM t_photo WHERE ph_seq_no=%d", seqNo)
        val rs = db.query(sql)
        if (rs.next()) {
            val date: Date = rs.getDate("ph_date")
            val folderName = rs.getString("ph_folder_name")
            val fileName = rs.getString("ph_file_name")
            val c = Calendar.getInstance()
            c.time = date
            val year = c[Calendar.YEAR]
            val url = String.format("https://inet.troot.co.jp/photo_org/%d/%s/%s", year, folderName, fileName)
            webView.loadUrl(url)
        }
    }
}