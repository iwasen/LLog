package jp.co.troot.llog;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;

public class MyUtils {
	public static String join(String[] arry, String with) {
        StringBuilder buf = new StringBuilder();
		for (String s: arry) {
			if (buf.length() > 0) {
				buf.append(with);
			}
			buf.append(s);
		 }
		 return buf.toString();
	}

	public static Bitmap getBitmapFromServer(Date date, String folderName, String fileName) throws Exception {
		return getBitmapFromURL(getServerURL(date, folderName, fileName));
	}

	public static Bitmap getBitmapFromServer(Date date, String folderName, String fileName, int maxSize) throws Exception {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		String url = String.format("http://inet.troot.co.jp/llog/ph_img.php?year=%d&folder=%s&file=%s&m=%d&c=1", cal.get(Calendar.YEAR), URLEncoder.encode(folderName, "UTF-8"), URLEncoder.encode(fileName, "UTF-8"), maxSize);
		return getBitmapFromURL(url);
	}

	public static Bitmap getBitmapFromServer(int seqNo, int maxSize) throws Exception {
		String url = String.format("http://inet.troot.co.jp/llog/ph_img.php?seq_no=%d&m=%d&c=1", seqNo, maxSize);
		return getBitmapFromURL(url);
	}

	public static InputStream getPhotoStreamFromServer(Date date, String folderName, String fileName) throws Exception {
		return getPhotoStreamFromURL(getServerURL(date, folderName, fileName));
	}

	public static Bitmap getBitmapFromURL(String src) throws Exception {
		return BitmapFactory.decodeStream(getPhotoStreamFromURL(src));
	}

	private static String getServerURL(Date date, String folderName, String fileName) throws Exception {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return String.format("http://inet.troot.co.jp/photo/%d/%s/%s", cal.get(Calendar.YEAR), URLEncoder.encode(folderName, "UTF-8"), URLEncoder.encode(fileName, "UTF-8"));
	}

	private static InputStream getPhotoStreamFromURL(String src) throws Exception {
		URL url = new URL(src);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setDoInput(true);
		connection.connect();
		return connection.getInputStream();
	}

	public static String requestJSON(String url, String requestData) throws Exception {
		URL object = new URL(url);

		HttpURLConnection con = (HttpURLConnection)object.openConnection();
		con.setDoOutput(true);
		con.setDoInput(true);
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Accept", "application/json");
		con.setRequestMethod("POST");

		OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
		wr.write(requestData);
		wr.flush();

		StringBuilder sb = new StringBuilder();
		int HttpResult = con.getResponseCode();
		if (HttpResult == HttpURLConnection.HTTP_OK) {
			BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}

			br.close();
		}

		return sb.toString();
	}
}
