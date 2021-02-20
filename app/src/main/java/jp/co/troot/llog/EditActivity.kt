package jp.co.troot.llog

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.core.app.ActivityCompat
import jp.co.troot.llog.Database.Companion.sqlDate
import jp.co.troot.llog.Database.Companion.sqlString
import java.util.*

// 編集処理
class EditActivity : MyActivity(), LocationListener {
    private lateinit var mDate: Calendar
    private lateinit var mOrgComment: String
    private lateinit var mLocationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初期化処理
        window.requestFeature(Window.FEATURE_CUSTOM_TITLE)
        setContentView(R.layout.activity_edit)
        window.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_edit)

        // 編集ボタンクリック処理
        mDate = intent.getSerializableExtra("Date") as Calendar
        findViewById<Button>(R.id.buttonEditUpload).setOnClickListener {
            val intent = Intent(this@EditActivity, UploadActivity::class.java)
            startActivityForResult(intent, 0)
        }

        // ランチボタンクリック処理
        mLocationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
        findViewById<Button>(R.id.buttonEditLunch).setOnClickListener {
            findViewById<Button>(R.id.buttonEditLunch).setBackgroundResource(R.color.orange)
            requestLocation()
        }

        // リストビュー（定型左）クリック処理
        findViewById<ListView>(R.id.listViewEvent1).onItemClickListener = OnItemClickListener { parent, _, position, _ ->
            val eventItem = parent.getItemAtPosition(position) as EventItem
            val db = Database()
            val sql = String.format("INSERT INTO t_event (ev_date,ev_event_id) VALUES (%s,%d)", sqlDate(mDate.time), eventItem.mEventId)
            db.exec(sql)
            displayData(db)
            db.close()
        }

        // リストビュー（定型右）クリック処理
        findViewById<ListView>(R.id.listViewEvent2).onItemClickListener = OnItemClickListener { parent, _, position, _ ->
            val eventItem = parent.getItemAtPosition(position) as EventItem
            val db = Database()
            val sql = String.format("DELETE FROM t_event WHERE ev_date=%s AND ev_event_id=%d", sqlDate(mDate.time), eventItem.mEventId)
            db.exec(sql)
            displayData(db)
            db.close()
        }

        // 電車追加ボタンクリック処理
        findViewById<Button>(R.id.buttonTrainAdd).setOnClickListener {
            val editTextTrainLineFrom = findViewById<EditText>(R.id.editTextTrainLineFrom)
            val editTextTrainStationFrom = findViewById<EditText>(R.id.editTextTrainStationFrom)
            val editTextTrainLineTo = findViewById<EditText>(R.id.editTextTrainLineTo)
            val editTextTrainStationTo = findViewById<EditText>(R.id.editTextTrainStationTo)
            val db = Database()
            if (editTextTrainLineFrom.text.isNotEmpty() && editTextTrainStationFrom.text.isNotEmpty() && editTextTrainLineTo.text.isNotEmpty() && editTextTrainStationTo.text.isNotEmpty()) {
                val sql = String.format("INSERT INTO t_train (tr_date,tr_from_line,tr_from_station,tr_to_line,tr_to_station) VALUES (%s,%s,%s,%s,%s)",
                        sqlDate(mDate.time),
                        sqlString(editTextTrainLineFrom.text.toString()),
                        sqlString(editTextTrainStationFrom.text.toString()),
                        sqlString(editTextTrainLineTo.text.toString()),
                        sqlString(editTextTrainStationTo.text.toString()))
                db.exec(sql)
            }
            displayData(db)
            db.close()
        }

        // 電車削除ボタンクリック処理
        findViewById<Button>(R.id.buttonTrainDelete).setOnClickListener {
            val db = Database()
            val listView = findViewById<ListView>(R.id.listViewTrain)
            val pos = listView.checkedItemPosition
            if (pos != -1) {
                val trainItem = listView.getItemAtPosition(pos) as TrainItem
                val sql = String.format("DELETE FROM t_train WHERE tr_seq_no=%d", trainItem.mSeqNo)
                db.exec(sql)
                displayData(db)
            }
            db.close()
        }

        // データ表示
        val db = Database()
        displayData(db)
        db.close()

        setResult(1, intent)
    }

    // 戻るボタンクリック処理
    override fun onBackPressed() {
        val editTextComment = findViewById<EditText>(R.id.editTextComment)
        val comment = editTextComment.text.toString().replace("\r", "").replace("\n", "\r\n")
        if (comment != mOrgComment) {
            // コメントが変更されていた場合
            val db = Database()
            var sql = String.format("SELECT cm_seq_no FROM t_comment WHERE cm_date=%s ORDER BY cm_seq_no", sqlDate(mDate.time))
            val rs = db.query(sql)
            if (rs.next()) {
                // 更新
                val seqNo = rs.getInt("cm_seq_no")
                sql = if (comment.isNotEmpty()) {
                    String.format("UPDATE t_comment SET cm_comment=%s WHERE cm_seq_no=%d", sqlString(comment), seqNo)
                } else {
                    String.format("DELETE FROM t_comment WHERE cm_seq_no=%d", seqNo)
                }
                db.exec(sql)
            } else {
                // 追加
                if (comment.isNotEmpty()) {
                    sql = String.format("INSERT INTO t_comment (cm_date,cm_comment) VALUES (%s,%s)", sqlDate(mDate.time), sqlString(comment))
                    db.exec(sql)
                }
            }
            db.close()
        }

        super.onBackPressed()
    }

    // データ表示処理
    @Throws(Exception::class)
    private fun displayData(db: Database) {
        // Adapter取得
        val eventAdapter1 = EventAdapter(this, R.layout.row_edit)
        val eventAdapter2 = EventAdapter(this, R.layout.row_edit)
        val trainAdapter = TrainAdapter(this, R.layout.row_edit)

        // コメント表示
        var sql = String.format("SELECT cm_comment FROM t_comment WHERE cm_date=%s ORDER BY cm_seq_no", sqlDate(mDate.time))
        var rs = db.query(sql)
        mOrgComment = if (rs.next()) {
            val editTextComment = findViewById<EditText>(R.id.editTextComment)
            editTextComment.setText(rs.getString("cm_comment"))
            editTextComment.text.toString()
        } else ""

        // 定型（左右リストビュー）表示
        sql = String.format("SELECT em_event_id,em_text,ev_event_id FROM m_event LEFT JOIN t_event ON ev_event_id=em_event_id AND ev_date=%s ORDER BY em_event_id", sqlDate(mDate.time))
        rs = db.query(sql)
        while (rs.next()) {
            val eventItem = EventItem()
            eventItem.mEventId = rs.getInt("em_event_id")
            eventItem.mText = rs.getString("em_text")
            rs.getInt("ev_event_id")
            if (rs.wasNull()) eventAdapter1.add(eventItem) else eventAdapter2.add(eventItem)
        }
        findViewById<ListView>(R.id.listViewEvent1).adapter = eventAdapter1
        findViewById<ListView>(R.id.listViewEvent2).adapter = eventAdapter2

        // 電車表示
        sql = String.format("SELECT tr_seq_no,tr_from_line,tr_from_station,tr_to_line,tr_to_station FROM t_train WHERE tr_date=%s ORDER BY tr_seq_no", sqlDate(mDate.time))
        rs = db.query(sql)
        while (rs.next()) {
            val trainItem = TrainItem()
            trainItem.mSeqNo = rs.getInt("tr_seq_no")
            trainItem.mText = String.format("%s %s → %s %s", rs.getString("tr_from_line"), rs.getString("tr_from_station"), rs.getString("tr_to_line"), rs.getString("tr_to_station"))
            trainAdapter.add(trainItem)
        }
        findViewById<ListView>(R.id.listViewTrain).adapter = trainAdapter
    }

    // 現在地取得リクエスト
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

    // 現在地取得完了処理
    override fun onLocationChanged(location: Location) {
        var placeName: String? = null

        // 現在地に最も近い公園を取得
        mLocationManager.removeUpdates(this)
        location.altitude = 0.0
        val place = Location(location)
        var minDistance = Double.MAX_VALUE
        val db = Database()
        var sql = "SELECT pl_name, pl_location[0] AS pl_lat, pl_location[1] AS pl_lon FROM t_place WHERE pl_kind = 1 AND pl_location IS NOT NULL"
        var rs = db.query(sql)
        while (rs.next()) {
            place.latitude = rs.getDouble("pl_lat")
            place.longitude = rs.getDouble("pl_lon")
            val distance = location.distanceTo(place).toDouble()
            if (distance < minDistance) {
                minDistance = distance
                placeName = rs.getString("pl_name")
            }
        }

        // 公園名をコメントにセット
        if (placeName != null) {
            val seqNo: Int
            var comment: String
            sql = String.format("SELECT cm_seq_no, cm_comment FROM t_comment WHERE cm_date=%s ORDER BY cm_seq_no", sqlDate(mDate.time))
            rs = db.query(sql)
            if (rs.next()) {
                seqNo = rs.getInt("cm_seq_no")
                comment = rs.getString("cm_comment").trim()
                if (comment.isNotEmpty()) {
                    comment += "\n"
                }
            } else {
                seqNo = 0
                comment = ""
            }
            if (placeName == "南にこにこ児童公園") {
                placeName = "自宅"
            }
            comment += "ランチ：$placeName"
            sql = if (seqNo == 0) {
                String.format("INSERT INTO t_comment (cm_date,cm_comment) VALUES (%s,%s)", sqlDate(mDate.time), sqlString(comment))
            } else {
                String.format("UPDATE t_comment SET cm_comment=%s WHERE cm_seq_no=%d", sqlString(comment), seqNo)
            }
            db.exec(sql)
        }
        db.close()
        finish()
    }

    // 定型データ
    private class EventItem {
        var mEventId = 0
        var mText: String? = null
    }

    // 定型Adapter
    private class EventAdapter(context: Context, textViewResourceId: Int) : ArrayAdapter<EventItem?>(context, textViewResourceId) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent) as TextView
            val eventItem = getItem(position)
            if (eventItem != null) view.text = eventItem.mText
            return view
        }
    }

    // 電車データ
    private class TrainItem {
        var mSeqNo = 0
        var mText: String? = null
    }

    // 電車Adapter
    private class TrainAdapter(context: Context, textViewResourceId: Int) : ArrayAdapter<TrainItem?>(context, textViewResourceId) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent) as TextView
            val trainItem = getItem(position)
            if (trainItem != null) view.text = trainItem.mText
            return view
        }
    }
}