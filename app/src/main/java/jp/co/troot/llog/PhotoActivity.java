package jp.co.troot.llog;

import android.os.Bundle;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Button;
import android.support.v4.view.ViewPager;
import android.app.AlertDialog;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import com.drew.metadata.Metadata;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

public class PhotoActivity extends MyActivity {
    private ArrayList<PhotoData> mPhotoList;
    private ViewPager mViewPager;

    public PhotoActivity() {
        mPhotoList = new ArrayList<PhotoData>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Window window = getWindow();
            window.requestFeature(Window.FEATURE_CUSTOM_TITLE);

            int currentPage = 0;

            Intent intent = getIntent();

            Database db = new Database();
            final Calendar date;
            String sql;

            int seqNo = intent.getIntExtra("SeqNo", 0);
            if (seqNo != 0) {
                sql = String.format("SELECT ph_date FROM t_photo WHERE ph_seq_no=%d", seqNo);
                ResultSet rs = db.query(sql);
                rs.next();
                date = Calendar.getInstance();
                date.setTime(rs.getDate("ph_date"));
            } else
                date = (Calendar)intent.getSerializableExtra("Date");

            int counter = 0;
            sql = String.format("SELECT ph_seq_no,ph_datetime,ph_folder_name,ph_file_name,ph_location IS NOT NULL AS ph_location_flag FROM t_photo WHERE ph_date=%s ORDER BY ph_datetime", Database.sqlDate(date.getTime()));
            ResultSet rs = db.query(sql);
            while (rs.next()) {
                PhotoData photoData = new PhotoData();
                photoData.mSeqNo = rs.getInt("ph_seq_no");
                photoData.mDate = rs.getTimestamp("ph_datetime");
                photoData.mFolderName = rs.getString("ph_folder_name");
                photoData.mFileName = rs.getString("ph_file_name");
                photoData.mLocationFlag = rs.getBoolean("ph_location_flag");
                mPhotoList.add(photoData);

                if (seqNo != 0 && photoData.mSeqNo == seqNo)
                    currentPage = counter;

                counter++;
            }

            db.close();

            PhotoAdapter adapter = new PhotoAdapter(this, mPhotoList);

            // ViewPager を生成
            mViewPager = new ViewPager(this);
            mViewPager.setPageMargin(16);
            mViewPager.setAdapter(adapter);
            mViewPager.setCurrentItem(currentPage);

            mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                public void onPageSelected(int position) {
                    setPhotoTitle(position);
                }
            });

            // レイアウトにセット
            setContentView(mViewPager);

            Configuration config = getResources().getConfiguration();
            if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            window.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_photo);

            setPhotoTitle(currentPage);

            Button buttonPhotoMap = (Button)findViewById(R.id.buttonMapMap);
            buttonPhotoMap.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(PhotoActivity.this, MapActivity.class);
                    intent.putExtra("PhotoSeqNo", mPhotoList.get(mViewPager.getCurrentItem()).mSeqNo);
                    startActivity(intent);
                }
            });

            Button buttonPhotoInfo = (Button)findViewById(R.id.buttonPhotoInfo);
            buttonPhotoInfo.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    displayPhotoInfo();
                }
            });

            Button buttonPhotoList = (Button)findViewById(R.id.buttonPhotoList);
            buttonPhotoList.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(PhotoActivity.this, PhotoListActivity.class);
                    intent.putExtra("PhotoSeqNo", mPhotoList.get(mViewPager.getCurrentItem()).mSeqNo);
                    startActivityForResult(intent, 1);
                }
            });

            Button buttonPhotoZoom = (Button)findViewById(R.id.buttonPhotoZoom);
            buttonPhotoZoom.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(PhotoActivity.this, PhotoZoomActivity.class);
                    intent.putExtra("PhotoSeqNo", mPhotoList.get(mViewPager.getCurrentItem()).mSeqNo);
                    startActivity(intent);
                }
            });

            Button buttonPhotoTop = (Button)findViewById(R.id.buttonPhotoTop);
            if (intent.getBooleanExtra("EnableTop", false)) {
                buttonPhotoTop.setOnClickListener(new OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(PhotoActivity.this, MainActivity.class);
                        intent.putExtra("Date", date.getTime());
                        startActivity(intent);
                    }
                });
            } else {
                ((ViewGroup)findViewById(R.id.layoutPhotoTitle)).removeView(buttonPhotoTop);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Intent intent = getIntent();
        intent.putExtra("SeqNo", mPhotoList.get(mViewPager.getCurrentItem()).mSeqNo);
    }

    private void setPhotoTitle(int position) {
        PhotoData photoData = mPhotoList.get(position);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy'/'MM'/'dd' 'HH':'mm", Locale.JAPANESE);
        TextView view = (TextView)findViewById(R.id.txtPhotoDate);
        view.setText(String.format("[%d/%d] ", position + 1, mPhotoList.size()) + sdf.format(photoData.mDate));

        Button buttonPhotoMap = (Button)findViewById(R.id.buttonMapMap);
        buttonPhotoMap.setEnabled(photoData.mLocationFlag);
    }

    private void displayPhotoInfo() {
        try {
            PhotoData photoData = mPhotoList.get(mViewPager.getCurrentItem());
            Metadata metadata = JpegMetadataReader.readMetadata(MyUtils.getPhotoStreamFromServer(photoData.mDate, photoData.mFolderName, photoData.mFileName));
            ExifIFD0Directory exifDirectory = metadata.getDirectory(ExifIFD0Directory.class);
            ExifSubIFDDirectory exifSubIFDDirectory = metadata.getDirectory(ExifSubIFDDirectory.class);

            ArrayList<String> messages = new ArrayList<String>();

            String model = exifDirectory.getString(ExifIFD0Directory.TAG_MODEL);
            if (model != null)
                messages.add(String.format("機種名　　　　：%s", model));

            Float shutter_speed = exifSubIFDDirectory.getFloatObject(ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
            if (shutter_speed != null)
                messages.add(String.format("シャッター速度：1/%.0f 秒", 1 / shutter_speed));
            else {
                shutter_speed = exifSubIFDDirectory.getFloatObject(ExifSubIFDDirectory.TAG_SHUTTER_SPEED);
                if (shutter_speed != null)
                    messages.add(String.format("シャッター速度：1/%.0f 秒", Math.pow(2, shutter_speed)));
            }

            Float f_number = exifSubIFDDirectory.getFloatObject(ExifSubIFDDirectory.TAG_APERTURE);
            if (f_number != null)
                messages.add(String.format("絞り　　　　　：F%.1f", Math.sqrt(Math.pow(2, f_number))));
            else {
                f_number = exifSubIFDDirectory.getFloatObject(ExifSubIFDDirectory.TAG_FNUMBER);
                if (f_number != null)
                    messages.add(String.format("絞り　　　　　：F%.1f", f_number));
            }

            Integer iso_equivalent = exifSubIFDDirectory.getInteger(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT);
            if (iso_equivalent != null)
                messages.add(String.format("ＩＳＯ感度　　：%d", iso_equivalent));

            Float exposure_bias = exifSubIFDDirectory.getFloatObject(ExifSubIFDDirectory.TAG_EXPOSURE_BIAS);
            if (exposure_bias != null)
                messages.add(String.format("露出補正　　　：%.1f EV", exposure_bias));

            Float focal_length = exifSubIFDDirectory.getFloatObject(ExifSubIFDDirectory.TAG_FOCAL_LENGTH);
            if (focal_length != null)
                messages.add(String.format("焦点距離　　　：%.1f mm", focal_length));

            new AlertDialog.Builder(PhotoActivity.this)
                    .setTitle("写真情報")
                    .setMessage(MyUtils.join(messages.toArray(new String[messages.size()]), "\n"))
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1:
                if (resultCode != 0) {
                    for (int i = 0; i < mPhotoList.size(); i++) {
                        if (mPhotoList.get(i).mSeqNo == resultCode) {
                            mViewPager.setCurrentItem(i);
                            break;
                        }
                    }
                }
                break;
        }
    }
}
