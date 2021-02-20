package jp.co.troot.llog

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.drew.imaging.jpeg.JpegMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import jp.co.troot.llog.Database.Companion.sqlDate
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

class PhotoActivity : MyActivity() {
    private val mPhotoList: ArrayList<PhotoData> = ArrayList()
    private lateinit var mViewPager: ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初期化
        window.requestFeature(Window.FEATURE_CUSTOM_TITLE)
        var currentPage = 0
        val db = Database()
        val date: Calendar?
        var sql: String

        // seqNo指定の場合に日付を取得
        val seqNo = intent.getIntExtra("SeqNo", 0)
        if (seqNo != 0) {
            sql = String.format("SELECT ph_date FROM t_photo WHERE ph_seq_no=%d", seqNo)
            val rs = db.query(sql)
            rs.next()
            date = Calendar.getInstance()
            date.time = rs.getDate("ph_date")
        } else date = intent.getSerializableExtra("Date") as Calendar?

        // 指定された日付の写真データを全て取得
        var counter = 0
        sql = String.format("SELECT ph_seq_no,ph_datetime,ph_folder_name,ph_file_name,ph_location IS NOT NULL AS ph_location_flag FROM t_photo WHERE ph_date=%s ORDER BY ph_datetime", sqlDate(date!!.time))
        val rs = db.query(sql)
        while (rs.next()) {
            val photoData = PhotoData()
            photoData.mSeqNo = rs.getInt("ph_seq_no")
            photoData.mDate = rs.getTimestamp("ph_datetime")
            photoData.mFolderName = rs.getString("ph_folder_name")
            photoData.mFileName = rs.getString("ph_file_name")
            photoData.mLocationFlag = rs.getBoolean("ph_location_flag")
            mPhotoList.add(photoData)
            if (seqNo != 0 && photoData.mSeqNo == seqNo) currentPage = counter
            counter++
        }
        db.close()
        val adapter = PhotoAdapter(this, mPhotoList)

        // ViewPager を生成
        mViewPager = ViewPager(this)
        mViewPager.pageMargin = 16
        mViewPager.adapter = adapter
        mViewPager.currentItem = currentPage
        mViewPager.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                setPhotoTitle(position)
            }
        })

        // レイアウトにセット
        val config = resources.configuration
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        setContentView(mViewPager)

        // タイトルにページを表示
        window.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_photo)
        setPhotoTitle(currentPage)

        // Mapボタン処理
        findViewById<Button>(R.id.buttonMapMap).setOnClickListener {
            val intent = Intent(this@PhotoActivity, MapActivity::class.java)
            intent.putExtra("PhotoSeqNo", mPhotoList[mViewPager.currentItem].mSeqNo)
            startActivity(intent)
        }

        // 情報ボタン処理
        findViewById<Button>(R.id.buttonPhotoInfo).setOnClickListener { displayPhotoInfo() }

        // 一覧ボタン処理
        findViewById<Button>(R.id.buttonPhotoList).setOnClickListener {
            val intent = Intent(this@PhotoActivity, PhotoListActivity::class.java)
            intent.putExtra("PhotoSeqNo", mPhotoList[mViewPager.currentItem].mSeqNo)
            startActivityForResult(intent, 1)
        }

        // 拡大ボタン処理
        findViewById<Button>(R.id.buttonPhotoZoom).setOnClickListener {
            val intent = Intent(this@PhotoActivity, PhotoZoomActivity::class.java)
            intent.putExtra("PhotoSeqNo", mPhotoList[mViewPager.currentItem].mSeqNo)
            startActivity(intent)
        }

        // Topボタン処理
        val buttonPhotoTop = findViewById<Button>(R.id.buttonPhotoTop)
        if (intent.getBooleanExtra("EnableTop", false)) {
            buttonPhotoTop.setOnClickListener {
                val intent = Intent(this@PhotoActivity, MainActivity::class.java)
                intent.putExtra("Date", date.time)
                startActivity(intent)
            }
        } else {
            (findViewById<View>(R.id.layoutPhotoTitle) as ViewGroup).removeView(buttonPhotoTop)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        intent.putExtra("SeqNo", mPhotoList[mViewPager.currentItem].mSeqNo)
    }

    // タイトル表示
    private fun setPhotoTitle(position: Int) {
        val photoData = mPhotoList[position]
        val sdf = SimpleDateFormat("yyyy'/'MM'/'dd' 'HH':'mm", Locale.JAPANESE)
        val view = findViewById<TextView>(R.id.txtPhotoDate)
        view.text = String.format("[%d/%d] %s", position + 1, mPhotoList.size, sdf.format(photoData.mDate))
        val buttonPhotoMap = findViewById<Button>(R.id.buttonMapMap)
        buttonPhotoMap.isEnabled = photoData.mLocationFlag
    }

    // シャイsン情報表示
    private fun displayPhotoInfo() {
        val photoData = mPhotoList[mViewPager.currentItem]
        val metadata = JpegMetadataReader.readMetadata(MyUtils.getPhotoStreamFromServer(photoData.mDate, photoData.mFolderName, photoData.mFileName))
        val exifDirectory = metadata.getDirectory(ExifIFD0Directory::class.java)
        val exifSubIFDDirectory = metadata.getDirectory(ExifSubIFDDirectory::class.java)
        val messages = ArrayList<String?>()
        val model = exifDirectory.getString(ExifIFD0Directory.TAG_MODEL)
        if (model != null) messages.add(String.format("機種名　　　　：%s", model))
        var shutterSpeed = exifSubIFDDirectory.getFloatObject(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)
        if (shutterSpeed != null) messages.add(String.format("シャッター速度：1/%.0f 秒", 1 / shutterSpeed)) else {
            shutterSpeed = exifSubIFDDirectory.getFloatObject(ExifSubIFDDirectory.TAG_SHUTTER_SPEED)
            if (shutterSpeed != null) messages.add(String.format("シャッター速度：1/%.0f 秒", 2.0.pow(shutterSpeed.toDouble())))
        }
        var fNumber = exifSubIFDDirectory.getFloatObject(ExifSubIFDDirectory.TAG_APERTURE)
        if (fNumber != null) messages.add(String.format("絞り　　　　　：F%.1f", sqrt(2.0.pow(fNumber.toDouble())))) else {
            fNumber = exifSubIFDDirectory.getFloatObject(ExifSubIFDDirectory.TAG_FNUMBER)
            if (fNumber != null) messages.add(String.format("絞り　　　　　：F%.1f", fNumber))
        }
        val isoEquivalent = exifSubIFDDirectory.getInteger(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)
        if (isoEquivalent != null) messages.add(String.format("ＩＳＯ感度　　：%d", isoEquivalent))
        val exposureBias = exifSubIFDDirectory.getFloatObject(ExifSubIFDDirectory.TAG_EXPOSURE_BIAS)
        if (exposureBias != null) messages.add(String.format("露出補正　　　：%.1f EV", exposureBias))
        val focalLength = exifSubIFDDirectory.getFloatObject(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)
        if (focalLength != null) messages.add(String.format("焦点距離　　　：%.1f mm", focalLength))
        AlertDialog.Builder(this@PhotoActivity)
                .setTitle("写真情報")
                .setMessage(messages.toTypedArray().joinToString("\n"))
                .setPositiveButton("OK", null)
                .show()
    }

    // 一覧で選択された写真を表示
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1) {
            if (resultCode != 0) {
                for (i in mPhotoList.indices) {
                    if (mPhotoList[i].mSeqNo == resultCode) {
                        mViewPager.currentItem = i
                        break
                    }
                }
            }
        }
    }

}