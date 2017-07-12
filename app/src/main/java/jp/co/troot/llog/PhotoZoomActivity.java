package jp.co.troot.llog;

import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class PhotoZoomActivity extends MyActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_photo_zoom);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		WebView webView = (WebView)findViewById(R.id.webView);

		WebSettings ws = webView.getSettings();
		ws.setBuiltInZoomControls(true);
		ws.setSupportZoom(true);
		ws.setDisplayZoomControls(false);

		try {
			Intent intent = getIntent();

			Database db = new Database();
			int seqNo = (Integer)intent.getSerializableExtra("PhotoSeqNo");
			String sql = String.format("SELECT ph_date,ph_folder_name,ph_file_name FROM t_photo WHERE ph_seq_no=%d", seqNo);
			ResultSet rs = db.query(sql);
			if (rs.next()) {
				Date date = rs.getDate("ph_date");
				String folderName = rs.getString("ph_folder_name");
				String fileName = rs.getString("ph_file_name");

				Calendar c = Calendar.getInstance();
				c.setTime(date);
				int year = c.get(Calendar.YEAR);

				String url = String.format("http://inet.troot.co.jp/photo_org/%d/%s/%s", year, folderName, fileName);
				webView.loadUrl(url);
			}
		} catch (Exception e) {
                e.printStackTrace();
		}
	}
}
