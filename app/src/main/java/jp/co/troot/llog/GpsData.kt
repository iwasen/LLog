package jp.co.troot.llog

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

// GPSデータ処理クラス
internal class GpsData {
    lateinit var mPos: LatLng

    //	private int mAlt;
    lateinit var mCalendar: Calendar

    companion object {
        // GPSデータを取り出す
        fun getGpsData(gpsBytes: ByteArray, step: Int): ArrayList<GpsData> {
            val gpsDataList = ArrayList<GpsData>()
            val bb = ByteBuffer.wrap(gpsBytes)
            bb.order(ByteOrder.LITTLE_ENDIAN)
            var count = 0
            while (bb.remaining() > 0) {
                bb.short
                var tDate = bb.int
                val tLat = bb.int
                val tLon = bb.int
                /*short t_alt = */bb.short
                if (count++ % step == 0) {
                    val gpsData = GpsData()
                    gpsData.mPos = LatLng(tLat / 10000000.0, tLon / 10000000.0)
                    //				gpsData.mAlt = t_alt;
                    gpsData.mCalendar = Calendar.getInstance()
                    val second = tDate and 0x3f
                    tDate = tDate shr 6
                    val minute = tDate and 0x3f
                    tDate = tDate shr 6
                    val hour = tDate and 0x1f
                    tDate = tDate shr 5
                    val day = tDate and 0x1f
                    tDate = tDate shr 5
                    val month = tDate and 0x0f
                    tDate = tDate shr 4
                    val year = tDate and 0x3f
                    gpsData.mCalendar.set(year, month, day, hour, minute, second)
                    gpsData.mCalendar.add(Calendar.HOUR_OF_DAY, 9)
                    gpsDataList.add(gpsData)
                }
            }
            return gpsDataList
        }

        // GPSデータの範囲を取得
        fun getBounds(gpsDataList: ArrayList<GpsData>): LatLngBounds {
            val builder = LatLngBounds.Builder()
            for (gpsData in gpsDataList) builder.include(gpsData.mPos)
            return builder.build()
        }
    }
}