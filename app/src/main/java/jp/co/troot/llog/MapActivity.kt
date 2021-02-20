package jp.co.troot.llog

import android.content.Intent
import android.content.res.Configuration
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class
MapActivity : MyActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private val mPhotoMarkerMap: MutableMap<Marker, Int> = HashMap()
    private lateinit var mSearchCenter: LatLng
    private var mSearchCircle: Circle? = null
    private var mSearchFlag = false
    private var mMarkerList: ArrayList<Marker>? = null
    private var mSpinner: Spinner? = null
    private var mDpi = 0
    private var mPolyline: ArrayList<Polyline>? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Map表示
        val metrics = MyUtils.getDisplayMetrics(this)
        mDpi = metrics.densityDpi
        val config = resources.configuration
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        window.requestFeature(Window.FEATURE_CUSTOM_TITLE)
        setContentView(R.layout.activity_map)
        window.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_map)
        @Suppress("DEPRECATION")
        val mapFragment = fragmentManager.findFragmentById(R.id.map) as MapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        mMap = map
        mMap.uiSettings.isRotateGesturesEnabled = false
        val db = Database()

        // 軌跡をポリライン表示
        val gpsSeqNo = intent.getIntExtra("GpsSeqNo", 0)
        if (gpsSeqNo != 0) {
            val gpsDataList = setPolyline(db, gpsSeqNo)
            setMarkers(db, gpsSeqNo, gpsDataList)
        }

        // 撮影位置のマーカ表示
        val textMapTitle = findViewById<TextView>(R.id.textMapTitle)
        val photoSeqNo = intent.getIntExtra("PhotoSeqNo", 0)
        if (photoSeqNo != 0) {
            textMapTitle.text = getString(R.string.photo_pos)
            setPhotoMarker(db, photoSeqNo)
        }

        // 携帯GPSのマーカ表示
        val keitaiGpsSeqNo = intent.getIntExtra("KeitaiGpsSeqNo", 0)
        if (keitaiGpsSeqNo != 0) {
            textMapTitle.text = getString(R.string.mobile_gps)
            setKeitaiGpsMarker(db, keitaiGpsSeqNo)
        }

        // タイトル表示
        val buttonSearch = findViewById<TextView>(R.id.buttonMapSearch)
        val mapSearch = intent.getIntExtra("MapSearch", 0)
        if (mapSearch != 0) {
            when (mapSearch) {
                1 -> textMapTitle.text = getString(R.string.photo_search)
                2 -> textMapTitle.text = getString(R.string.log_search)
                3 -> {
                    textMapTitle.text = getString(R.string.mobile_gps_search)
                    mMarkerList = ArrayList()
                }
            }

            // Map表示
            buttonSearch.setOnClickListener { toggleSearch() }
            val latlng = LatLng(35.574517, 139.737765)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 16f))
            @Suppress("DEPRECATION")
            val mapFragment = fragmentManager.findFragmentById(R.id.map) as MapFragment
            @Suppress("DEPRECATION")
            val view = ((mapFragment.view as FrameLayout).getChildAt(0) as FrameLayout).getChildAt(0)
            view.setOnTouchListener { v, event ->
                if (mSearchFlag) {
                    val projection = mMap.projection
                    val latLng = projection.fromScreenLocation(Point(event.x.toInt(), event.y.toInt()))
                    val distance: Double
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            if (mSearchCircle != null) {
                                mSearchCircle?.remove()
                                mSearchCircle = null
                            }
                            if (mSpinner != null) {
                                @Suppress("DEPRECATION")
                                val mapFragment1 = fragmentManager.findFragmentById(R.id.map) as MapFragment
                                @Suppress("DEPRECATION")
                                (mapFragment1.view as FrameLayout).removeView(mSpinner)
                                mSpinner = null
                            }
                            if (mPolyline != null) {
                                for (polyline in mPolyline!!) polyline.remove()
                                mPolyline = null
                            }
                            mSearchCenter = latLng
                        }
                        MotionEvent.ACTION_MOVE -> {
                            distance = getDistance(mSearchCenter, latLng)
                            if (mSearchCircle == null) {
                                mSearchCircle = mMap.addCircle(CircleOptions().center(mSearchCenter).radius(distance).strokeColor(Color.MAGENTA).strokeWidth(5f))
                            } else {
                                mSearchCircle!!.radius = distance
                            }
                        }
                        MotionEvent.ACTION_UP -> {
                            distance = getDistance(mSearchCenter, latLng)
                            when (mapSearch) {
                                1 -> searchPhoto(mSearchCenter, distance)
                                2 -> searchGpsLogger(mSearchCenter, distance)
                                3 -> searchKeitaiGps(mSearchCenter, distance)
                            }
                            toggleSearch()
                            v.performClick()
                        }
                    }
                    true
                } else {
                    false
                }
            }
        } else {
            (findViewById<View>(R.id.layoutMapTitle) as ViewGroup).removeView(buttonSearch)
        }
        db.close()

        val selectedColor = 0xffaaaaff.toInt()

        // 地図ボタン処理
        val buttonMap = findViewById<Button>(R.id.buttonMapMap)
        buttonMap.setBackgroundColor(selectedColor)
        buttonMap.setOnClickListener {
            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            findViewById<Button>(R.id.buttonMapMap).setBackgroundColor(selectedColor)
            findViewById<Button>(R.id.buttonMapSatellite).setBackgroundResource(android.R.drawable.btn_default_small)
            findViewById<Button>(R.id.buttonMapHybrid).setBackgroundResource(android.R.drawable.btn_default_small)
        }

        // 航空写真ボタン処理
        val buttonSatellite = findViewById<Button>(R.id.buttonMapSatellite)
        buttonSatellite.setOnClickListener {
            mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            findViewById<Button>(R.id.buttonMapMap).setBackgroundResource(android.R.drawable.btn_default_small)
            findViewById<Button>(R.id.buttonMapSatellite).setBackgroundColor(selectedColor)
            findViewById<Button>(R.id.buttonMapHybrid).setBackgroundResource(android.R.drawable.btn_default_small)
        }

        val buttonHybrid = findViewById<Button>(R.id.buttonMapHybrid)
        buttonHybrid.setOnClickListener {
            mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            findViewById<Button>(R.id.buttonMapMap).setBackgroundResource(android.R.drawable.btn_default_small)
            findViewById<Button>(R.id.buttonMapSatellite).setBackgroundResource(android.R.drawable.btn_default_small)
            findViewById<Button>(R.id.buttonMapHybrid).setBackgroundColor(selectedColor)
        }

        // 地図＋写真ボタン処理
        mMap.setOnMarkerClickListener { marker ->
            val seqNo = mPhotoMarkerMap[marker]
            if (seqNo != null) {
                val intent = Intent(this@MapActivity, PhotoActivity::class.java)
                intent.putExtra("SeqNo", seqNo)
                intent.putExtra("EnableTop", mapSearch != 0)
                startActivity(intent)
                true
            } else {
                false
            }
        }

        // Map長押し処理
        mMap.setOnMapLongClickListener { latLng ->
            val intent = Intent(
                    "android.intent.action.VIEW",
                    Uri.parse(String.format("google.streetview:cbll=%f,%f&cbp=1,0,,0,1", latLng.latitude, latLng.longitude)))
            startActivity(intent)
        }

        // Topボタン処理
        val buttonMapTop = findViewById<Button>(R.id.buttonMapTop)
        if (intent.getBooleanExtra("EnableTop", false)) {
            buttonMapTop.setOnClickListener {
                val db2 = Database()
                val sql = String.format("SELECT gl_date FROM t_gps_logger WHERE gl_seq_no=%d", gpsSeqNo)
                val rs = db2.query(sql)
                if (rs.next()) {
                    val intent = Intent(this@MapActivity, MainActivity::class.java)
                    intent.putExtra("Date", rs.getDate("gl_date"))
                    startActivity(intent)
                }
            }
        } else {
            (findViewById<View>(R.id.layoutMapTitle) as ViewGroup).removeView(buttonMapTop)
        }
    }

    // GPSの軌跡を取得
    private fun setPolyline(db: Database, seqNo: Int): ArrayList<GpsData>? {
        var gpsDataList: ArrayList<GpsData>? = null
        val sql = String.format("SELECT gl_point_data FROM t_gps_logger WHERE gl_seq_no=%d", seqNo)
        val rs = db.query(sql)
        if (rs.next()) {
            gpsDataList = GpsData.getGpsData(rs.getBytes("gl_point_data"), 1)
            val po = PolylineOptions()
            for (gpsData in gpsDataList) {
                po.add(gpsData.mPos)
            }
            po.color(0x99ff00aa.toInt())
            po.width(7f)
            mMap.addPolyline(po)
            val bounds: LatLngBounds = GpsData.getBounds(gpsDataList)
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            @Suppress("DEPRECATION")
            val disp = wm.defaultDisplay
            if (disp != null) {
                val size = Point()
                @Suppress("DEPRECATION")
                disp.getSize(size)
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, size.x, size.y - 80, 20))
            }
        }
        return gpsDataList
    }

    // 各種マーカ設定
    private fun setMarkers(db: Database, seqNo: Int, gpsDataList: ArrayList<GpsData>?) {
        var distance = 0.0
        var kilo2 = 0.0
        var hour2 = -1
        addKiloMarker(gpsDataList!![0].mPos, 0, gpsDataList[0].mCalendar)
        for (i in 1 until gpsDataList.size) {
            val gpsData1 = gpsDataList[i - 1]
            val gpsData2 = gpsDataList[i]
            distance += getDistance(gpsData1.mPos, gpsData2.mPos)
            val kilo = floor(distance / 1000).toInt()
            if (kilo.toDouble() != kilo2) {
                addKiloMarker(gpsData2.mPos, kilo, gpsData2.mCalendar)
                kilo2 = kilo.toDouble()
            }
            val hour = gpsData2.mCalendar[Calendar.HOUR_OF_DAY]
            if (hour != hour2) {
                if (hour2 != -1) {
                    addHourMarker(gpsData2.mPos, hour, distance)
                }
                hour2 = hour
            }
        }
        var sql = String.format("SELECT ph_seq_no,ph_datetime,ph_location[0] AS ph_lat,ph_location[1] AS ph_lon FROM t_gps_logger JOIN t_photo ON ph_date=gl_date AND ph_location IS NOT NULL WHERE gl_seq_no=%d ORDER BY ph_seq_no", seqNo)
        var rs = db.query(sql)
        while (rs.next()) {
            addPhotoMarker(LatLng(rs.getDouble("ph_lat"), rs.getDouble("ph_lon")), rs.getInt("ph_seq_no"))
        }
        sql = String.format("SELECT gc_point[0] AS gc_lat,gc_point[1] AS gc_lon,gc_comment FROM t_gps_comment WHERE gc_gps_logger_id=%d ORDER BY gc_seq_no", seqNo)
        rs = db.query(sql)
        while (rs.next()) {
            addCommentMarker(LatLng(rs.getDouble("gc_lat"), rs.getDouble("gc_lon")), rs.getString("gc_comment"))
        }
    }

    // kmマーカ表示
    private fun addKiloMarker(pos: LatLng?, kilo: Int, time: Calendar?) {
        var kilo2 = kilo
        if (kilo2 > 100) kilo2 = 100
        val options = MarkerOptions()
        options.position(pos!!)
        options.title(String.format("%02d:%02d:%02d", time!![Calendar.HOUR_OF_DAY], time[Calendar.MINUTE], time[Calendar.SECOND]))
        options.icon(getMarker(String.format("marker_kilo/marker%d.png", kilo2)))
        mMap.addMarker(options)
    }

    // 時刻マーカ表示
    private fun addHourMarker(pos: LatLng?, hour: Int, distance: Double) {
        val options = MarkerOptions()
        options.position(pos!!)
        val format = NumberFormat.getNumberInstance()
        options.title(format.format(distance) + "m")
        options.icon(getMarker(String.format("marker_hour/hour%d.png", hour)))
        mMap.addMarker(options)
    }

    // GPS Loggerの写真マーカ表示
    private fun addPhotoMarker(pos: LatLng, seqNo: Int) {
        val options = MarkerOptions()
        options.position(pos)
        options.icon(getMarker("yellow.png"))
        mPhotoMarkerMap[mMap.addMarker(options)] = seqNo
    }

    // コメントマーカ表示
    private fun addCommentMarker(pos: LatLng, comment: String) {
        val options = MarkerOptions()
        options.position(pos)
        options.title(comment)
        options.icon(getMarker("green.png"))
        mMap.addMarker(options)
    }

    // 写真マーカ表示
    private fun setPhotoMarker(db: Database, seqNo: Int) {
        val sql = String.format("SELECT ph_datetime,ph_location[0] AS ph_lat,ph_location[1] AS ph_lon FROM t_photo WHERE ph_seq_no=%d", seqNo)
        val rs = db.query(sql)
        if (rs.next()) {
            val latLng = LatLng(rs.getFloat("ph_lat").toDouble(), rs.getFloat("ph_lon").toDouble())
            val sdf = SimpleDateFormat("yyyy'/'MM'/'dd' 'HH':'mm", Locale.JAPANESE)
            val options = MarkerOptions()
            options.position(latLng)
            options.title(sdf.format(rs.getTimestamp("ph_datetime")))
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
            mMap.addMarker(options)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        }
    }

    // 携帯GPSマーカ表示
    private fun setKeitaiGpsMarker(db: Database, seqNo: Int) {
        val sql = String.format("SELECT kg_datetime,kg_point[0] AS kg_lat,kg_point[1] AS kg_lon FROM t_keitai_gps WHERE kg_seq_no=%d", seqNo)
        val rs = db.query(sql)
        if (rs.next()) {
            val latLng = LatLng(rs.getFloat("kg_lat").toDouble(), rs.getFloat("kg_lon").toDouble())
            val sdf = SimpleDateFormat("yyyy'/'MM'/'dd' 'HH':'mm':'ss", Locale.JAPANESE)
            val options = MarkerOptions()
            options.position(latLng)
            options.title(sdf.format(rs.getTimestamp("kg_datetime")))
            mMap.addMarker(options)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        }
    }

    // ２点間の距離を計算
    private fun getDistance(pos1: LatLng, pos2: LatLng): Double {
        val f1 = pos1.latitude
        var g1 = pos1.longitude
        val f2 = pos2.latitude
        var g2 = pos2.longitude
        val a = 6378136.0 // 赤道半径(m)
        val e2 = 0.006694470 // 地球の離心率の自乗
        val rad = Math.PI / 180.0
        val h1 = 0.0
        val h2 = 0.0 // ここでは、標高を無視
        if (g1 < 0) g1 += 360.0
        val fr1 = f1 * rad
        val gr1 = g1 * rad
        if (g2 < 0) g2 += 360.0
        val fr2 = f2 * rad
        val gr2 = g2 * rad
        val n1 = a / sqrt(1.0 - e2 * sin(fr1) * sin(fr1))
        val x1 = (n1 + h1) * cos(fr1) * cos(gr1)
        val y1 = (n1 + h1) * cos(fr1) * sin(gr1)
        val z1 = (n1 * (1.0 - e2) + h1) * sin(fr1)
        val n2 = a / sqrt(1.0 - e2 * sin(fr2) * sin(fr2))
        val x2 = (n2 + h2) * cos(fr2) * cos(gr2)
        val y2 = (n2 + h2) * cos(fr2) * sin(gr2)
        val z2 = (n2 * (1.0 - e2) + h2) * sin(fr2)
        val r = sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2)) // 直距離
        val wr = asin(r / 2 / a) // 半射程角(rad)
        //    	double w = wr / rad;			// 半射程角(°)
        return a * 2 * wr // 地表面距離
    }

    // 写真検索処理
    private fun searchPhoto(centerLatLng: LatLng, distance: Double) {
        val c = 6378136 * 2 * Math.PI
        val dLat = distance / c * 360
        val dLon = dLat / cos(centerLatLng.latitude / 180 * Math.PI)
        for (marker in mPhotoMarkerMap.keys) marker.remove()
        mPhotoMarkerMap.clear()
        val db = Database()
        val sql = String.format("SELECT ph_seq_no, ph_location[0] AS lat, ph_location[1] AS lon FROM t_photo WHERE circle '((%.9g, %.9g), 1)' @> point(ph_location[0] + (ph_location[0] - %.9g) / %.9g, ph_location[1] + (ph_location[1] - %.9g) / %.9g) ORDER BY ph_datetime", centerLatLng.latitude, centerLatLng.longitude, centerLatLng.latitude, dLat, centerLatLng.longitude, dLon)
        val rs = db.query(sql)
        while (rs.next()) {
            val options = MarkerOptions()
            options.position(LatLng(rs.getDouble("lat"), rs.getDouble("lon")))
            options.icon(getMarker("yellow.png"))
            mPhotoMarkerMap[mMap.addMarker(options)] = rs.getInt("ph_seq_no")
        }
        db.close()
    }

    // GPS Logger検索
    private fun searchGpsLogger(centerLatLng: LatLng, distance: Double) {
        val adapter: ArrayAdapter<GpsListItem> = object : ArrayAdapter<GpsListItem>(this, android.R.layout.simple_spinner_item) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                val gpsListItem = getItem(position)
                if (gpsListItem != null) {
                    view.text = gpsListItem.mItemName
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as CheckedTextView
                val gpsListItem = getItem(position)
                if (gpsListItem != null) {
                    view.text = gpsListItem.mItemName
                }
                return view
            }
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val url = URL(String.format("https://llog.troot.co.jp/gl_search_log.php?circle_lat=%.9g&circle_lon=%.9g&circle_r=%.9g",
                centerLatLng.latitude, centerLatLng.longitude, distance))
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        val br = BufferedReader(InputStreamReader(connection.inputStream))
        val result = br.readLine()
        if (result.isNotEmpty()) {
            val sdf = SimpleDateFormat("yyyy'/'MM'/'dd", Locale.JAPANESE)
            var count = 0
            val db = Database()
            var sql = String.format("SELECT gl_seq_no, gl_date FROM t_gps_logger WHERE gl_seq_no IN (%s) ORDER BY gl_start_time", result)
            var rs = db.query(sql)
            while (rs.next()) {
                val gpsListItem = GpsListItem()
                gpsListItem.mSeqNo = rs.getInt("gl_seq_no")
                gpsListItem.mItemName = sdf.format(rs.getDate("gl_date"))
                adapter.add(gpsListItem)
                count++
            }
            if (count < 20) {
                mPolyline = ArrayList()
                sql = String.format("SELECT gl_point_data FROM t_gps_logger WHERE gl_seq_no IN (%s) ORDER BY gl_start_time", result)
                rs = db.query(sql)
                while (rs.next()) {
                    val rnd = Random()
                    val gpsDataList: ArrayList<GpsData> = GpsData.getGpsData(rs.getBytes("gl_point_data"), 1)
                    val po = PolylineOptions()
                    for (gpsData in gpsDataList) {
                        po.add(gpsData.mPos)
                    }
                    po.color(-0x67000000 or (rnd.nextInt(256) shl 16) or (rnd.nextInt(256) shl 8) or rnd.nextInt(256))
                    po.width(7f)
                    mPolyline!!.add(mMap.addPolyline(po))
                }
            }
            db.close()
        }

        val gpsListItem = GpsListItem()
        gpsListItem.mSeqNo = 0
        gpsListItem.mItemName = String.format("%d 件", adapter.count)
        adapter.insert(gpsListItem, 0)
        mSpinner = Spinner(this)
        mSpinner!!.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        mSpinner!!.adapter = adapter
        mSpinner!!.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (position != 0) {
                    val gpsListItem2 = parent.getItemAtPosition(position) as GpsListItem
                    val intent = Intent(this@MapActivity, MapActivity::class.java)
                    intent.putExtra("GpsSeqNo", gpsListItem2.mSeqNo)
                    intent.putExtra("EnableTop", true)
                    startActivity(intent)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        @Suppress("DEPRECATION")
        val mapFragment = fragmentManager.findFragmentById(R.id.map) as MapFragment
        @Suppress("DEPRECATION")
        val view = mapFragment.view as FrameLayout
        view.addView(mSpinner)
    }

    // 携帯GPS検索処理
    private fun searchKeitaiGps(centerLatLng: LatLng, distance: Double) {
        val c = 6378136 * 2 * Math.PI
        val dLat = distance / c * 360
        val dLon = dLat / cos(centerLatLng.latitude / 180 * Math.PI)
        for (marker in mMarkerList!!) marker.remove()
        mMarkerList!!.clear()

        val db = Database()
        val sql = String.format("SELECT kg_datetime,kg_point[0] AS kg_lat,kg_point[1] AS kg_lon FROM t_keitai_gps WHERE circle '((%.9g, %.9g), 1)' @> point(kg_point[0] + (kg_point[0] - %.9g) / %.9g, kg_point[1] + (kg_point[1] - %.9g) / %.9g) ORDER BY kg_datetime", centerLatLng.latitude, centerLatLng.longitude, centerLatLng.latitude, dLat, centerLatLng.longitude, dLon)
        val rs = db.query(sql)
        while (rs.next()) {
            val latLng = LatLng(rs.getFloat("kg_lat").toDouble(), rs.getFloat("kg_lon").toDouble())
            val sdf = SimpleDateFormat("yyyy'/'MM'/'dd' 'HH':'mm':'ss", Locale.JAPANESE)
            val options = MarkerOptions()
            options.position(latLng)
            options.title(sdf.format(rs.getTimestamp("kg_datetime")))
            mMarkerList!!.add(mMap.addMarker(options))
        }
        db.close()
    }

    // 検索ボタン切り替え処理
    private fun toggleSearch() {
        val buttonSearch = findViewById<Button>(R.id.buttonMapSearch)
        mSearchFlag = if (mSearchFlag) {
            buttonSearch.setBackgroundResource(android.R.drawable.btn_default_small)
            false
        } else {
            buttonSearch.setBackgroundResource(R.color.orange)
            true
        }
    }

    // Resume処理
    public override fun onResume() {
        super.onResume()
        if (mSpinner != null) mSpinner!!.setSelection(0)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    private fun getMarker(path: String): BitmapDescriptor {
        val matrix = Matrix()
        matrix.setScale(mDpi / 180f, mDpi / 180f)
        val `is` = resources.assets.open(path)
        val bmp = BitmapFactory.decodeStream(`is`)

        return BitmapDescriptorFactory.fromBitmap(Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true))
    }

    private class GpsListItem {
        var mSeqNo = 0
        lateinit var mItemName: String
    }
}