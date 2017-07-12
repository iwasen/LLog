package jp.co.troot.llog;

import java.sql.ResultSet;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;

public class PhotoListActivity extends MyActivity {
	private int mPhotoSeqNo;
	Handler  mHandler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_photo_list);

		Configuration config = getResources().getConfiguration();
		if(config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		Intent intent = getIntent();
		mPhotoSeqNo = intent.getIntExtra("PhotoSeqNo", 0);

		new MyAsyncTask().execute();
	}

	public class MyAsyncTask extends AsyncTask<String, Integer, Long> {
		@Override
		protected Long doInBackground(String... params) {
			WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
			Display disp = wm.getDefaultDisplay();
			Point size = new Point();
			disp.getSize(size);
			GridLayout gridLayout = (GridLayout)findViewById(R.id.gridLayout);
			gridLayout.setColumnCount((size.x - 10) / (148 + 10));
			try {
				Database db = new Database();
				String sql = String.format("SELECT ph_date FROM t_photo WHERE ph_seq_no=%d", mPhotoSeqNo);
				ResultSet rs = db.query(sql);
				if (rs.next()) {
					String date = rs.getString("ph_date");
					sql = String.format("SELECT ph_seq_no FROM t_photo WHERE ph_date=%s ORDER BY ph_datetime", Database.sqlString(date));
					rs = db.query(sql);
					while (rs.next()) {
						if (PhotoListActivity.this.isDestroyed())
							break;

						int seqNo = rs.getInt("ph_seq_no");
						final ImageView imageView = new ImageView(PhotoListActivity.this);
						imageView.setTag(seqNo);
						imageView.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								setResult((Integer)v.getTag());
								finish();
							}
						});
						imageView.setImageBitmap(MyUtils.getBitmapFromServer(seqNo, 148));
						imageView.setPadding(5, 5, 5, 5);
						mHandler.post(new Runnable() {
							@Override 
							public void run() {
								GridLayout gridLayout = (GridLayout)findViewById(R.id.gridLayout);
								gridLayout.addView(imageView);
							}  
						});
					}
				}
				db.close();
			} catch (Exception e) {
                e.printStackTrace();
			}

			return null;
		}
		
	}
}
