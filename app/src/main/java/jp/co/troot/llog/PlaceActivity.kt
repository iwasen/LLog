package jp.co.troot.llog

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONObject
import java.util.*

class PlaceActivity : MyActivity(), LocationListener, OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private val mMarkerMap: MutableMap<String, Int> = HashMap()
    private lateinit var mLocationManager: LocationManager
    private var mCurrentLocation: Marker? = null
    private var mAddress: JSONObject? = null
    private val mMarkers = ArrayList<Marker>()
    private var mKind = 1

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 画面設定
        val config = resources.configuration
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        window.requestFeature(Window.FEATURE_CUSTOM_TITLE)
        setContentView(R.layout.activity_place)
        window.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_place)

        // Google Mapの表示
        @Suppress("DEPRECATION")
        val mapFragment = fragmentManager.findFragmentById(R.id.map) as MapFragment
        mapFragment.getMapAsync(this)
    }

    // Google Map表示コールバック
    override fun onMapReady(map: GoogleMap) {
        mMap = map
        mMap.uiSettings.isRotateGesturesEnabled = false

        // 現在地を取得して表示
        mLocationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
        requestLocation()
        val latlng = LatLng(35.574517, 139.737765)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 16f))
        setMarkers()

        // 種類ボタン処理
        findViewById<Button>(R.id.buttonPlaceKind).setOnClickListener {
            AlertDialog.Builder(this@PlaceActivity)
                    .setTitle("種類")
                    .setItems(mPlaces) { _, item ->
                        mKind = item + 1
                        setMarkers()
                    }.show()
        }

        // 現在地ボタン処理
        findViewById<Button>(R.id.buttonPlaceLocation).setOnClickListener { requestLocation() }
        val selectedColor = -0x555501

        // 地図ボタン処理
        val buttonMap = findViewById<Button>(R.id.buttonPlaceMap)
        buttonMap.setBackgroundColor(selectedColor)
        buttonMap.setOnClickListener {
            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            findViewById<Button>(R.id.buttonPlaceMap).setBackgroundColor(selectedColor)
            findViewById<Button>(R.id.buttonPlaceSatellite).setBackgroundResource(android.R.drawable.btn_default_small)
            findViewById<Button>(R.id.buttonPlaceHybrid).setBackgroundResource(android.R.drawable.btn_default_small)
        }

        // 写真ボタン処理
        findViewById<Button>(R.id.buttonPlaceSatellite).setOnClickListener {
            mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            findViewById<Button>(R.id.buttonPlaceMap).setBackgroundResource(android.R.drawable.btn_default_small)
            findViewById<Button>(R.id.buttonPlaceSatellite).setBackgroundColor(selectedColor)
            findViewById<Button>(R.id.buttonPlaceHybrid).setBackgroundResource(android.R.drawable.btn_default_small)
        }

        // 地＋写ボタン処理
        findViewById<Button>(R.id.buttonPlaceHybrid).setOnClickListener {
            mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            findViewById<Button>(R.id.buttonPlaceMap).setBackgroundResource(android.R.drawable.btn_default_small)
            findViewById<Button>(R.id.buttonPlaceSatellite).setBackgroundResource(android.R.drawable.btn_default_small)
            findViewById<Button>(R.id.buttonPlaceHybrid).setBackgroundColor(selectedColor)
        }

        // マーカクリック処理
        mMap.setOnInfoWindowClickListener { marker ->
            // 現在地マーカ
            if (marker == mCurrentLocation) {
                val messages = ArrayList<String?>()
                messages.add("住所")
                messages.add(String.format("　%s", mAddress!!.getString("address")))
                messages.add("")
                messages.add("自宅")
                messages.add(String.format("　距離：%s", mAddress!!.getString("home_distance")))
                messages.add(String.format("　方向：%s", mAddress!!.getString("home_dir")))
                messages.add("")
                messages.add("最寄駅")
                val stations = mAddress!!.getJSONArray("stations")
                for (i in 0 until stations.length()) {
                    val station = stations.getJSONObject(i)
                    messages.add(String.format("　%s.%s %s（%sに%s先・%s）",
                            i + 1,
                            station.getString("line"),
                            station.getString("name"),
                            station.getString("direction"),
                            if (station.getInt("distance") < 1000) String.format("%dm", station.getInt("distance")) else station.getString("distanceKm"),
                            station.getString("traveltime")
                    ))
                }
                AlertDialog.Builder(this@PlaceActivity)
                        .setTitle("現在地情報")
                        .setMessage(messages.toTypedArray().joinToString("\n"))
                        .setNegativeButton("閉じる", null)
                        .show()
            } else {
                // それ以外のマーカの場合情報を表示
                val seqNo = mMarkerMap[marker.id]
                val db = Database()
                val sql = String.format("SELECT pl_name, pl_address, pl_comment, pl_add_info FROM t_place WHERE pl_seq_no = %d", seqNo)
                val rs = db.query(sql)
                if (rs.next()) {
                    val messages = ArrayList<String?>()
                    val name = rs.getString("pl_name")
                    if (name != null) messages.add(String.format("名称：%s", name))
                    val address = rs.getString("pl_address")
                    if (address != null) messages.add(String.format("場所：%s", address))
                    val comment = rs.getString("pl_comment")
                    if (comment != null) messages.add(String.format("コメント：%s", comment))
                    val addInfo = rs.getArray("pl_add_info")
                    if (addInfo != null) {
                        val addInfoStr = addInfo.array as Array<*>
                        when (mKind) {
                            1 -> {
                                if (addInfoStr[0] != "") messages.add(String.format("広さ：%s ㎡", addInfoStr[0]))
                                messages.add(String.format("ゴミ箱：%s", if (addInfoStr[1] == "t") "有り" else "無し"))
                                messages.add(String.format("トイレ：%s", if (addInfoStr[2] == "t") "有り" else "無し"))
                            }
                            3 -> messages.add(String.format("宗派：%s", addInfoStr[0]))
                        }
                    }
                    val dialog = AlertDialog.Builder(this@PlaceActivity)
                    dialog.setTitle("情報")
                    dialog.setMessage(messages.toTypedArray().joinToString("\n"))
                    dialog.setNegativeButton("閉じる", null)

                    // 訪問ボタン処理
                    dialog.setPositiveButton(
                            "訪問"
                    ) { _, _ ->
                        val db1 = Database()
                        var sql1 = String.format("SELECT pl_visited FROM t_place WHERE pl_seq_no = %d", seqNo)
                        val rs1 = db1.query(sql1)
                        if (rs1.next()) {
                            val date: String = if (rs1.getDate("pl_visited") == null) "current_date" else "null"
                            sql1 = String.format("UPDATE t_place SET pl_visited = %s WHERE pl_seq_no = %d", date, seqNo)
                            db1.exec(sql1)
                            setMarkers()
                        }
                    }

                    // 編集ボタン処理
                    if (mKind == 1) {
                        dialog.setNeutralButton(
                                "編集"
                        ) { _, _ ->
                            val intent = Intent(this@PlaceActivity, PlaceEditActivity::class.java)
                            intent.putExtra("SeqNo", seqNo)
                            startActivity(intent)
                        }
                    }
                    dialog.show()
                }
                db.close()
            }
        }

        // 長押しでストリートビュー表示処理
        mMap.setOnMapLongClickListener { latLng ->
            val intent = Intent(
                    "android.intent.action.VIEW",
                    Uri.parse(String.format("google.streetview:cbll=%f,%f&cbp=1,0,,0,1", latLng.latitude, latLng.longitude)))
            startActivity(intent)
        }
    }

    // 位置取得後取得
    override fun onLocationChanged(location: Location) {
        val latlng = LatLng(location.latitude, location.longitude)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng))
        if (mCurrentLocation != null) {
            mCurrentLocation!!.remove()
        }
        val options = MarkerOptions()
/*
        lat,lonから住所を求める。（今まで使っていたサイトが閉鎖したため、とりあえず削除
        val postParams = JSONObject()
        postParams.put("lat", location.latitude)
        postParams.put("lon", location.longitude)
        mAddress = JSONObject(MyUtils.requestJSON("https://llog.troot.co.jp/pl_address.php", postParams.toString()))
        options.title(mAddress!!.getString("address"))
*/
        options.position(latlng)
        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        mCurrentLocation = mMap.addMarker(options)
        mLocationManager.removeUpdates(this)
    }

    // マップにマーカセット処理
    private fun setMarkers() {
        val textPlaceTitle = findViewById<TextView>(R.id.textPlaceTitle)
        textPlaceTitle.text = mPlaces[mKind - 1]
        for (marker in mMarkers) marker.remove()
        mMarkers.clear()
        mMarkerMap.clear()
        val db = Database()
        val sql = String.format("SELECT pl_seq_no, pl_name, pl_location[0] AS pl_lat, pl_location[1] AS pl_lon, pl_visited FROM t_place WHERE pl_kind = %d AND pl_location IS NOT NULL", mKind)
        val rs = db.query(sql)
        while (rs.next()) {
            val options = MarkerOptions()
            options.position(LatLng(rs.getDouble("pl_lat"), rs.getDouble("pl_lon")))
            options.title(rs.getString("pl_name"))
            if (rs.getDate("pl_visited") != null) options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
            val marker = mMap.addMarker(options)
            mMarkerMap[marker.id] = rs.getInt("pl_seq_no")
            mMarkers.add(marker)
        }
        db.close()
    }

    // 現在地リクエスト
    private fun requestLocation() {
        when {
            mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
            }
            mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, this)
            }
            else -> {
                val ts = Toast.makeText(this, "現在位置が取得できません", Toast.LENGTH_SHORT)
                ts.show()
            }
        }
    }

    public override fun onStop() {
        mLocationManager.removeUpdates(this)
        super.onStop()
    }

    companion object {
        private val mPlaces = arrayOf("公園", "坂道", "寺", "神社", "学校", "橋")
    }
}