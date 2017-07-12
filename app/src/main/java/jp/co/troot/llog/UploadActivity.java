package jp.co.troot.llog;

import android.graphics.Color;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Base64;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UploadActivity extends MyActivity {
    private List<String> mPhotoFiles = new ArrayList<String>();
    private List<String> mUploadPhotoFiles = new ArrayList<String>();
    private List<String> mGpsFiles = new ArrayList<String>();
    private List<String> mUploadGpsFiles = new ArrayList<String>();
    Handler  mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        getPhotoFiles();
        TextView textView = (TextView)findViewById(R.id.textViewPhotoFileNum2);
        textView.setText(String.format("：%d 件", mPhotoFiles.size()));

        getGpsFiles("/MyTracks/gpx");
        getGpsFiles("/com.kamoland/ytlog");
        textView = (TextView)findViewById(R.id.textViewGpsFileNum2);
        textView.setText(String.format("：%d 件", mGpsFiles.size()));

        new GetUploadFiles(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView)findViewById(R.id.textViewUploadPhotoNum2);
                textView.setText(String.format("：%d 件", mUploadPhotoFiles.size()));
                textView.setTextColor(mUploadPhotoFiles.size() == 0 ? Color.BLACK : Color.RED);

                textView = (TextView)findViewById(R.id.textViewUploadGpsNum2);
                textView.setText(String.format("：%d 件", mUploadGpsFiles.size()));
                textView.setTextColor(mUploadGpsFiles.size() == 0 ? Color.BLACK : Color.RED);

                if (mUploadPhotoFiles.size() + mUploadGpsFiles.size() > 0) {
                    Button buttonUpload = (Button)findViewById(R.id.buttonUpload);
                    buttonUpload.setEnabled(true);
                }
            }
        }).execute();

        Button buttonUpload = (Button)findViewById(R.id.buttonUpload);
        buttonUpload.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Button buttonUpload = (Button)findViewById(R.id.buttonUpload);
                buttonUpload.setEnabled(false);

                new UploadFiles(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }).execute();
            }
        });
    }

    private void getPhotoFiles() {
        String cameraDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera";

        File dir = new File(cameraDir);
        final File[] filelist = dir.listFiles();
        if (filelist != null) {
            for (File file: filelist) {
                if (getSuffix(file.getName()).equals("jpg")) {
                    mPhotoFiles.add(file.getAbsolutePath());
                }
            }
        }
    }

    private void getGpsFiles(String path) {
        String gpsDir = Environment.getExternalStorageDirectory().getAbsolutePath() + path;

        File dir = new File(gpsDir);
        final File[] filelist = dir.listFiles();
        if (filelist != null) {
            for (File file: filelist) {
                if (getSuffix(file.getName()).equals("gpx")) {
                    mGpsFiles.add(file.getAbsolutePath());
                }
            }
        }
    }

    private static String getSuffix(String fileName) {
        if (fileName == null)
            return null;
        int point = fileName.lastIndexOf(".");
        if (point != -1) {
            return fileName.substring(point + 1);
        }
        return fileName;
    }

    public class GetUploadFiles extends AsyncTask<String, Integer, Long> {
        Runnable mRunnable;

        public GetUploadFiles(Runnable runnable) {
            mRunnable = runnable;
        }

        @Override
        protected Long doInBackground(String... params) {
            try {
                JSONArray postParams = new JSONArray();
                for (int i = 0; i < mPhotoFiles.size(); i++) {
                    ExifInterface exif = new ExifInterface(mPhotoFiles.get(i));
                    String date = exif.getAttribute(ExifInterface.TAG_DATETIME);
                    if (date != null) {
                        date = date.replaceFirst("([0-9]+):([0-9]+):([0-9]+) ([0-9]+):([0-9]+):([0-9]+)", "$1-$2-$3 $4:$5:$6");
                    } else {
                        File file = new File(mPhotoFiles.get(i));
                        Date date2 = new Date(file.lastModified());
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
                        date = sdf.format(date2);
                    }
                    postParams.put(date);
                }

                JSONArray result = new JSONArray(MyUtils.requestJSON("http://inet.troot.co.jp/llog/ph_upload_check.php", postParams.toString()));
                if (mPhotoFiles.size() == result.length()) {
                    for (int i = 0; i < mPhotoFiles.size(); i++) {
                        if (result.get(i).equals("0")) {
                            mUploadPhotoFiles.add(mPhotoFiles.get(i));
                        }
                    }
                }

                postParams = new JSONArray();
                XmlPullParser parser = Xml.newPullParser();
                for (int i = 0; i < mGpsFiles.size(); i++) {
                    File file = new File(mGpsFiles.get(i));
                    FileReader filereader = new FileReader(file);
                    parser.setInput(filereader);
                    for (int eventType = parser.getEventType(); eventType != XmlPullParser.END_DOCUMENT; eventType = parser.next()) {
                        if (eventType == XmlPullParser.START_TAG && parser.getName().equals("time")) {
                            String time = parser.nextText();
                            time = time.substring(0, 19);
                            time = time.replace('T', ' ');
                            postParams.put(time);
                            break;
                        }
                    }
                }

                result = new JSONArray(MyUtils.requestJSON("http://inet.troot.co.jp/llog/gl_upload_check.php", postParams.toString()));
                if (mGpsFiles.size() == result.length()) {
                    for (int i = 0; i < mGpsFiles.size(); i++) {
                        if (result.get(i).equals("0")) {
                            mUploadGpsFiles.add(mGpsFiles.get(i));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            mHandler.post(mRunnable);

            return null;
        }
    }

    public class UploadFiles extends AsyncTask<String, Integer, Long> {
        Runnable mRunnable;

        public UploadFiles(Runnable runnable) {
            mRunnable = runnable;
        }

        @Override
        protected Long doInBackground(String... params) {
            try {
                JSONObject postParams;

                for (int i = 0; i < mUploadPhotoFiles.size(); i++) {
                    postParams = new JSONObject();
                    File upfile = new File(mUploadPhotoFiles.get(i));
                    byte[] buf = new byte[(int)upfile.length()];
                    FileInputStream fis = new FileInputStream(upfile);
                    if (fis.read(buf) == buf.length)
                        postParams.put("data", Base64.encodeToString(buf, Base64.DEFAULT));
                    MyUtils.requestJSON("http://inet.troot.co.jp/llog/ph_upload2.php", postParams.toString());
                }

                for (int i = 0; i < mUploadGpsFiles.size(); i++) {
                    postParams = new JSONObject();
                    File upfile = new File(mUploadGpsFiles.get(i));
                    byte[] buf = new byte[(int)upfile.length()];
                    FileInputStream fis = new FileInputStream(upfile);
                    if (fis.read(buf) == buf.length)
                        postParams.put("data", new String(buf, "UTF-8"));
                    MyUtils.requestJSON("http://inet.troot.co.jp/llog/gl_upload.php", postParams.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            mHandler.post(mRunnable);

            return null;
        }
    }
}
