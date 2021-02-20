package jp.co.troot.llog

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import jp.co.troot.llog.Database.Companion.sqlDate
import java.sql.ResultSet
import java.util.*

// メインActivity
class MainActivity : MyActivity() {
    private lateinit var mCurrentDate: Calendar
    private var mInDateChangeFlag = false
    private var mGpsLoggerSeqNo = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 権限チェック
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityForResult(intent, REQUEST_CODE_PERMISSION)
                return
            }
            val noPermissions = mutableListOf<String>()
            val requestPermissions = REQUEST_PERMISSIONS.toMutableList()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestPermissions.add(Manifest.permission.ACCESS_MEDIA_LOCATION)
            }
            for (requestPermission in requestPermissions) {
                val permission = ContextCompat.checkSelfPermission(this, requestPermission)
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    noPermissions.add(requestPermission)
                }
            }
            if (noPermissions.size != 0) {
                ActivityCompat.requestPermissions(
                        this,
                        noPermissions.toTypedArray(),
                        REQUEST_PERMISSIONS_CODE
                )
                return
            }
        }

        // 初期化処理
        window.requestFeature(Window.FEATURE_CUSTOM_TITLE)
        setContentView(R.layout.activity_main)
        window.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_main)

        // カレンダーコントロール初期化
        mCurrentDate = calendar
        val date = intent.getSerializableExtra("Date") as Date?
        if (date != null) mCurrentDate.time = date
        findViewById<DatePicker>(R.id.datePicker).init(mCurrentDate[Calendar.YEAR], mCurrentDate[Calendar.MONTH], mCurrentDate[Calendar.DAY_OF_MONTH]) { _, _, _, _ ->
            if (!mInDateChangeFlag) {
                val editTextSearch = findViewById<EditText>(R.id.editTextSearch)
                if (editTextSearch.length() != 0) editTextSearch.setText("")
                changeCalendar(false)
                findViewById<ListView>(R.id.listViewDate).setItemChecked(mCurrentDate[Calendar.DAY_OF_MONTH] - 1, true)
            }
        }

        // 日付リストビュークリック処理
        findViewById<ListView>(R.id.listViewDate).onItemClickListener = OnItemClickListener { parent, _, position, _ ->
            val listItem = parent.getItemAtPosition(position) as ListItem
            dispData(listItem.mCalendar, false)
        }

        // 検索ボタンクリック処理
        findViewById<Button>(R.id.buttonMapSearch).setOnClickListener { v ->
            val editTextSearch = findViewById<EditText>(R.id.editTextSearch)
            if (editTextSearch.text.isEmpty()) {
                // 検索文字が空の場合
                val items = arrayOf<CharSequence>("写真検索", "GPSログ検索", "携帯GPS検索")
                AlertDialog.Builder(this@MainActivity)
                        .setTitle("検索")
                        .setItems(items) { _, item ->
                            val intent = Intent(this@MainActivity, MapActivity::class.java)
                            intent.putExtra("MapSearch", item + 1)
                            startActivity(intent)
                        }.show()
            } else {
                // 検索文字が入っている場合
                val db = Database()
                dispMonthList(db)
                db.close()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        }

        // 検索文字テキスト
        findViewById<EditText>(R.id.editTextSearch).setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val db = Database()
                dispMonthList(db)
                db.close()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            }

            true
        }

        val buttonToday = findViewById<Button>(R.id.buttonMainToday)
        if (date == null) {
            buttonToday.setOnClickListener {
                val editTextSearch = findViewById<EditText>(R.id.editTextSearch)
                if (editTextSearch.length() != 0) editTextSearch.setText("")
                dispData(calendar, true)
            }
        } else {
            (findViewById<View>(R.id.layoutMainTitle) as ViewGroup).removeView(buttonToday)
        }

        // 編集ボタンクリック処理
        findViewById<Button>(R.id.buttonMainEdit).setOnClickListener {
            val intent = Intent(this@MainActivity, EditActivity::class.java)
            intent.putExtra("Date", mCurrentDate)
            startActivityForResult(intent, REQUEST_CODE_EDIT)
        }

        // 場所ボタンクリック処理
        findViewById<Button>(R.id.buttonMainPlace).setOnClickListener {
            val intent = Intent(this@MainActivity, PlaceActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_PLACE)
        }

        // 携帯GPSクリック処理
        findViewById<ListView>(R.id.listViewKeitaiGps).onItemClickListener = OnItemClickListener { parent, _, position, _ ->
            val keitaiGpsItem = parent.getItemAtPosition(position) as KeitaiGpsItem
            val intent = Intent(this@MainActivity, MapActivity::class.java)
            intent.putExtra("KeitaiGpsSeqNo", keitaiGpsItem.mSeqNo)
            startActivity(intent)
        }

        // 写真クリック処理
        val photoView = findViewById<View>(R.id.imageViewPhoto)
        photoView.setOnClickListener {
            val intent = Intent(this@MainActivity, PhotoActivity::class.java)
            intent.putExtra("Date", mCurrentDate)
            startActivity(intent)
        }

        // GPS Loggerクリック処理
        findViewById<View>(R.id.imageViewGps).setOnClickListener {
            val intent = Intent(this@MainActivity, MapActivity::class.java)
            intent.putExtra("GpsSeqNo", mGpsLoggerSeqNo)
            startActivity(intent)
        }

        changeCalendar(true)
    }

    // 権限取得結果
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            var allGrant = true
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allGrant = false
                    break
                }
            }
            if (allGrant) reload() else moveTaskToBack(false)
        }
    }

    // リロード
    private fun reload() {
        overridePendingTransition(0, 0)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
    }

    // Activity Result ?
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_PERMISSION -> reload()
            REQUEST_CODE_EDIT, REQUEST_CODE_PLACE -> if (resultCode != 0) changeCalendar(true)
        }
    }

    // カレンダー変更処理
    private fun changeCalendar(bForce: Boolean) {
        val datePicker = findViewById<DatePicker>(R.id.datePicker)
        val calendar = calendar
        calendar.set(datePicker.year, datePicker.month, datePicker.dayOfMonth)
        // 最初は下記のように変換されていた。意味は上記と同じだがわかりにくいので書き換えた
//        calendar[datePicker.year, datePicker.month] = datePicker.dayOfMonth
        dispData(calendar, bForce)
    }

    // データ表示
    private fun dispData(calendar: Calendar, bForce: Boolean) {
        val oldDate = mCurrentDate.clone() as Calendar
        if (bForce || calendar != mCurrentDate) {
            mCurrentDate = calendar
            val db = Database()
            if (bForce || (mCurrentDate[Calendar.YEAR] != oldDate[Calendar.YEAR] || mCurrentDate[Calendar.MONTH] != oldDate[Calendar.MONTH]) && findViewById<EditText>(R.id.editTextSearch).text.isEmpty()) {
                dispMonthList(db)
            }
            mInDateChangeFlag = true
            findViewById<DatePicker>(R.id.datePicker).updateDate(mCurrentDate[Calendar.YEAR], mCurrentDate[Calendar.MONTH], mCurrentDate[Calendar.DAY_OF_MONTH])
            mInDateChangeFlag = false
            dispDayData(db)
            db.close()
        }
    }

    // 月リスト表示
    private fun dispMonthList(db: Database) {
        // リストの色、高さ設定
        val adapter: ArrayAdapter<ListItem> = object : ArrayAdapter<ListItem>(this, R.layout.row_date) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                val listItem = getItem(position)
                if (listItem != null) {
                    view.text = HtmlCompat.fromHtml(listItem.mDispText, FROM_HTML_MODE_COMPACT)
                    when (listItem.mCalendar[Calendar.DAY_OF_WEEK]) {
                        1 -> view.setTextColor(Color.RED)
                        7 -> view.setTextColor(Color.BLUE)
                        else -> view.setTextColor(Color.BLACK)
                    }
                }
                val height = parent.height
                if (height != 0) {
                    view.layoutParams.height = height / 31 - 2
                }
                return view
            }
        }

        // リストにデータ表示
        val sql: String
        val searchText = findViewById<EditText>(R.id.editTextSearch)
        if (searchText.text.isEmpty()) {
            // 検索テキストが空の場合、現在の年月のデータを取得
            val date = mCurrentDate.clone() as Calendar
            date[Calendar.DAY_OF_MONTH] = 1
            sql = String.format("SELECT cl_date,"
                    + "(SELECT COUNT(*) FROM t_comment WHERE cm_date=cl_date) AS cm_count,"
                    + "(SELECT COUNT(*) FROM t_event WHERE ev_date=cl_date) AS ev_count,"
                    + "(SELECT COUNT(*) FROM t_train WHERE tr_date=cl_date) AS tr_count,"
                    + "(SELECT COUNT(*) FROM t_photo WHERE ph_date=cl_date) AS ph_count,"
                    + "(SELECT COUNT(*) FROM t_keitai_gps WHERE kg_date=cl_date) AS kg_count,"
                    + "(SELECT COUNT(*) FROM t_gps_logger WHERE gl_date=cl_date) AS gl_count,"
                    + "(SELECT wk_step / 1000 FROM t_walking WHERE wk_date=cl_date) AS wk_count "
                    + "FROM (SELECT date %s + arr.i AS cl_date FROM generate_series(0,%d) AS arr(i)) T "
                    + "ORDER BY cl_date", sqlDate(date.time), date.getActualMaximum(Calendar.DATE) - 1)
        } else {
            // 検索テキストが空でない場合は検索したデータを取得
            val searchEqu = searchText.text.toString()
            val searchTemp = searchEqu.split(" ").toTypedArray()
            val searchTemp2 = arrayOfNulls<String>(searchTemp.size)
            for (i in searchTemp.indices) {
                searchTemp2[i] = String.format("\t ILIKE '%%%s%%'", searchTemp[i])
            }
            val searchLike = searchTemp2.joinToString(" AND ")
            val commentLike = searchLike.replace("\t".toRegex(), "cm_comment")
            val photoLike1 = searchLike.replace("\t".toRegex(), "ph_folder_name")
            val photoLike2 = searchLike.replace("\t".toRegex(), "ph_comment")
            val photoLike = String.format("%s OR %s", photoLike1, photoLike2)
            val keitaiGpsLike1 = searchLike.replace("\t".toRegex(), "kg_address")
            val keitaiGpsLike2 = searchLike.replace("\t".toRegex(), "kg_comment")
            val keitaiGpsLike = String.format("%s OR %s", keitaiGpsLike1, keitaiGpsLike2)
            val gpsLoggerLike = searchLike.replace("\t".toRegex(), "gc_comment")
            sql = String.format("SELECT s1_date AS cl_date,"
                    + "(SELECT COUNT(*) FROM t_comment WHERE cm_date=s1_date) AS cm_count,"
                    + "(SELECT COUNT(*) FROM t_event WHERE ev_date=s1_date) AS ev_count,"
                    + "(SELECT COUNT(*) FROM t_train WHERE tr_date=s1_date) AS tr_count,"
                    + "(SELECT COUNT(*) FROM t_photo WHERE ph_date=s1_date) AS ph_count,"
                    + "(SELECT COUNT(*) FROM t_keitai_gps WHERE kg_date=s1_date) AS kg_count,"
                    + "(SELECT COUNT(*) FROM t_gps_logger WHERE gl_date=s1_date) AS gl_count,"
                    + "(SELECT wk_step / 1000 FROM t_walking WHERE wk_date=s1_date) AS wk_count "
                    + "FROM ("
                    + "SELECT cm_date AS s1_date FROM t_comment WHERE %s UNION "
                    + "SELECT ev_date AS s1_date FROM t_event WHERE ev_event_id IN (SELECT em_event_id FROM m_event WHERE em_text='%s') UNION "
                    + "SELECT tr_date AS s1_date FROM t_train WHERE tr_from_line='%s' OR tr_from_station='%s' OR tr_to_line='%s' OR tr_to_station='%s' UNION "
                    + "SELECT ph_date AS s1_date FROM t_photo WHERE %s UNION "
                    + "SELECT kg_date AS s1_date FROM t_keitai_gps WHERE %s UNION "
                    + "SELECT gl_date AS s1_date FROM t_gps_logger JOIN t_gps_comment ON gc_gps_logger_id=gl_seq_no WHERE %s"
                    + ") T "
                    + "ORDER BY s1_date", commentLike, searchEqu, searchEqu, searchEqu, searchEqu, searchEqu, photoLike, keitaiGpsLike, gpsLoggerLike)
        }

        // リストにデータ表示
        val week = arrayOf("日", "月", "火", "水", "木", "金", "土")
        val rs = db.query(sql)
        var pos = -1
        while (rs.next()) {
            val cl = Calendar.getInstance()
            cl.time = rs.getDate("cl_date")
            if (cl == mCurrentDate) pos = adapter.count
            val listItem = ListItem()
            listItem.mDispText = String.format("%04d/%02d/%02d（%s）%s %s %s %s %s %s %s",
                    cl[Calendar.YEAR],
                    cl[Calendar.MONTH] + 1,
                    cl[Calendar.DAY_OF_MONTH],
                    week[cl[Calendar.DAY_OF_WEEK] - 1],
                    getDispCount(rs.getInt("cm_count")),
                    getDispCount(rs.getInt("ev_count")),
                    getDispCount(rs.getInt("tr_count")),
                    getDispCount(rs.getInt("kg_count")),
                    getDispCount(rs.getInt("wk_count")),
                    getDispCount(rs.getInt("ph_count")),
                    getDispCount(rs.getInt("gl_count")))
            listItem.mCalendar = cl
            adapter.add(listItem)
        }

        val listView = findViewById<ListView>(R.id.listViewDate)
        listView.adapter = adapter
        if (pos != -1) listView.setItemChecked(pos, true)
    }

    // 件数表示
    private fun getDispCount(count: Int): String {
        var count2 = count
        var setColor = false
        if (count2 >= 100) {
            count2 /= 100
            setColor = true
        } else if (count2 >= 10) {
            count2 /= 10
            setColor = true
        }
        return if (setColor) String.format("<b>%d</b>", count2) else String.format("%d", count2)
    }

    // カレンダーのデータ
    private val calendar: Calendar
        get() {
            val calendar = Calendar.getInstance()
            calendar[Calendar.HOUR] = 0
            calendar[Calendar.HOUR_OF_DAY] = 0
            calendar[Calendar.MINUTE] = 0
            calendar[Calendar.SECOND] = 0
            calendar[Calendar.MILLISECOND] = 0
            return calendar
        }

    // 左側のデータ表示
    private fun dispDayData(db: Database) {
        val date = sqlDate(mCurrentDate.time)
        var rs: ResultSet
        var sql: String
        var textView: TextView
        var stringAdapter: ArrayAdapter<String?>
        val keitaiGpsAdapter: ArrayAdapter<KeitaiGpsItem>

        // コメント表示
        sql = String.format("SELECT cm_comment FROM t_comment WHERE cm_date=%s ORDER BY cm_seq_no", date)
        rs = db.query(sql)
        textView = findViewById(R.id.textViewComment)
        textView.text = if (rs.next()) rs.getString("cm_comment") else ""
        val scrollView: ScrollView = findViewById(R.id.scrollViewComment)
        scrollView.scrollY = 0

        // 定型表示
        sql = String.format("SELECT em_text FROM t_event JOIN m_event ON ev_event_id=em_event_id WHERE ev_date=%s ORDER BY ev_seq_no", date)
        rs = db.query(sql)
        stringAdapter = object : ArrayAdapter<String?>(this, R.layout.row_item) {
            override fun isEnabled(position: Int): Boolean {
                return false
            }
        }
        while (rs.next()) {
            stringAdapter.add(rs.getString("em_text"))
        }
        findViewById<ListView>(R.id.listViewEvent).adapter = stringAdapter

        // 電車表示
        sql = String.format("SELECT tr_from_line,tr_from_station,tr_to_line,tr_to_station FROM t_train WHERE tr_date=%s ORDER BY tr_seq_no", date)
        rs = db.query(sql)
        stringAdapter = object : ArrayAdapter<String?>(this, R.layout.row_item) {
            override fun isEnabled(position: Int): Boolean {
                return false
            }
        }
        while (rs.next()) {
            val fromLine = rs.getString("tr_from_line")
            val fromStation = rs.getString("tr_from_station")
            val toLine = rs.getString("tr_to_line")
            val toStation = rs.getString("tr_to_station")
            if (fromLine == toLine) stringAdapter.add(String.format("%s %s → %s", fromLine, fromStation, toStation)) else stringAdapter.add(String.format("%s %s → %s %s", fromLine, fromStation, toLine, toStation))
        }
        findViewById<ListView>(R.id.listViewTrain).adapter = stringAdapter

        // 携帯GPS表示
        sql = String.format("SELECT kg_seq_no,kg_datetime,COALESCE(kg_address,'不明') AS kg_address FROM t_keitai_gps WHERE kg_date=%s ORDER BY kg_seq_no", date)
        rs = db.query(sql)
        keitaiGpsAdapter = object : ArrayAdapter<KeitaiGpsItem>(this, R.layout.row_item) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                val keitaiGpsItem = getItem(position)
                if (keitaiGpsItem != null) view.text = keitaiGpsItem.mDispText
                return view
            }
        }
        while (rs.next()) {
            val keitaiGpsItem = KeitaiGpsItem()
            keitaiGpsItem.mDispText = String.format("%s %s", rs.getString("kg_datetime").subSequence(11, 19), rs.getString("kg_address"))
            keitaiGpsItem.mSeqNo = rs.getInt("kg_seq_no")
            keitaiGpsAdapter.add(keitaiGpsItem)
        }
        findViewById<ListView>(R.id.listViewKeitaiGps).adapter = keitaiGpsAdapter

        // 歩数表示
        sql = String.format("SELECT wk_step,wk_calorie,wk_fat,wk_distance,wk_time,wk_speed FROM t_walking WHERE wk_date=%s", date)
        rs = db.query(sql)
        if (rs.next()) {
            findViewById<TextView>(R.id.textViewWalkingStep).text = rs.getString("wk_step")
            findViewById<TextView>(R.id.textViewWalkingDistance).text = rs.getString("wk_distance")
            findViewById<TextView>(R.id.textViewWalkingCalorie).text = rs.getString("wk_calorie")
            findViewById<TextView>(R.id.textViewWalkingFat).text = rs.getString("wk_fat")
        } else {
            findViewById<TextView>(R.id.textViewWalkingStep).text = ""
            findViewById<TextView>(R.id.textViewWalkingDistance).text = ""
            findViewById<TextView>(R.id.textViewWalkingCalorie).text = ""
            findViewById<TextView>(R.id.textViewWalkingFat).text = ""
        }

        // 写真表示
        val photoView = findViewById<ImageView>(R.id.imageViewPhoto)
        sql = String.format("SELECT ph_date,ph_folder_name,ph_file_name FROM t_photo WHERE ph_date=%s ORDER BY ph_datetime LIMIT 1", date)
        rs = db.query(sql)
        if (rs.next()) {
            val bitmap = MyUtils.getBitmapFromServer(rs.getDate("ph_date"), rs.getString("ph_folder_name"), rs.getString("ph_file_name"), mPhotoViewSize)
            photoView.setImageBitmap(bitmap)
            photoView.isClickable = true
            textView = findViewById(R.id.textMapTitle)
            textView.text = rs.getString("ph_folder_name")
            sql = String.format("SELECT COUNT(*) FROM t_photo WHERE ph_date=%s", date)
            rs = db.query(sql)
            rs.next()
            textView = findViewById(R.id.textViewPhotoNum)
            textView.text = String.format("%d枚", rs.getInt(1))
        } else {
            photoView.setImageBitmap(null)
            photoView.isClickable = false
            textView = findViewById(R.id.textMapTitle)
            textView.text = ""
            textView = findViewById(R.id.textViewPhotoNum)
            textView.text = "0枚"
        }

        // GPS Logger表示
        val gpsView = findViewById<ImageView>(R.id.imageViewGps)
        sql = String.format("SELECT gl_seq_no,gl_point_data,gl_point_num FROM t_gps_logger WHERE gl_date=%s ORDER BY gl_seq_no LIMIT 1", date)
        rs = db.query(sql)
        if (rs.next()) {
            val appliInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val key = appliInfo.metaData.getString("com.google.android.geo.API_KEY")
            mGpsLoggerSeqNo = rs.getInt("gl_seq_no")
            val pointNum = rs.getInt("gl_point_num")
            var url = String.format("https://maps.googleapis.com/maps/api/staticmap?key=%s&size=%dx%d&sensor=false&language=ja&path=color:0xff00aaaa|weight:4", key, mGpsViewSize, mGpsViewSize)
            val step: Int = if (pointNum <= 50) {
                1
            } else {
                (pointNum - 1) / 49
            }
            val gpsDataList: ArrayList<GpsData> = GpsData.getGpsData(rs.getBytes("gl_point_data"), step)
            val sb = StringBuilder()
            for (gpsData in gpsDataList) {
                sb.append(String.format("|%f,%f", gpsData.mPos.latitude, gpsData.mPos.longitude))
            }
            url += sb.toString()
            val bitmap = MyUtils.getBitmapFromURL(url)
            gpsView.setImageBitmap(bitmap)
            gpsView.isClickable = true
        } else {
            gpsView.setImageBitmap(null)
            gpsView.isClickable = false
        }
    }

    // Inflate the menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    // リストデータ
    private class ListItem {
        lateinit var mDispText: String
        lateinit var mCalendar: Calendar
    }

    // 携帯GPSデータ
    private class KeitaiGpsItem {
        lateinit var mDispText: String
        var mSeqNo = 0
    }

    companion object {
        private const val mPhotoViewSize = 229
        private const val mGpsViewSize = 229
        private val REQUEST_PERMISSIONS = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
        )
        private const val REQUEST_PERMISSIONS_CODE = 0
        private const val REQUEST_CODE_PERMISSION = 0
        private const val REQUEST_CODE_EDIT = 1
        private const val REQUEST_CODE_PLACE = 2
    }
}