package jp.co.troot.llog;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLng;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.ArrayList;

public class GpsData {
	public LatLng mPos;
	public int mAlt;
	public Calendar mCalendar;

	public static ArrayList<GpsData> getGpsData(byte[] gpsBytes, int step) {
		ArrayList<GpsData> gpsDataList = new ArrayList<GpsData>();
		ByteBuffer bb = ByteBuffer.wrap(gpsBytes);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		int count = 0;
		while (bb.remaining() > 0) {
			bb.getShort();
			int t_date = bb.getInt();
			int t_lat = bb.getInt();
			int t_lon = bb.getInt();
			short t_alt = bb.getShort();

			if (count++ % step == 0) {
				GpsData gpsData = new GpsData();
	
				gpsData.mPos = new LatLng(t_lat / 10000000.0, t_lon / 10000000.0);
				gpsData.mAlt = t_alt;
				gpsData.mCalendar = Calendar.getInstance();
				int second = t_date & 0x3f;
				t_date >>= 6;
				int minute = t_date & 0x3f;
				t_date >>= 6;
				int hour = t_date & 0x1f;
				t_date >>= 5;
				int day = t_date & 0x1f;
				t_date >>= 5;
				int month = t_date & 0x0f;
				t_date >>= 4;
				int year = t_date & 0x3f;
				gpsData.mCalendar.set(year, month, day, hour, minute, second);
				gpsData.mCalendar.add(Calendar.HOUR_OF_DAY, 9);
				gpsDataList.add(gpsData);
			}
		}
		return gpsDataList;
	}

	public static LatLngBounds getBounds(ArrayList<GpsData> gpsDataList) {
		LatLngBounds.Builder builder = new LatLngBounds.Builder();
		for (int i = 0; i < gpsDataList.size(); i++)
			builder.include(gpsDataList.get(i).mPos);

		return builder.build();
	}
}
