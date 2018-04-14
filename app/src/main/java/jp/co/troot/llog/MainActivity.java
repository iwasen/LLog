package jp.co.troot.llog;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Color;
import android.provider.Settings;
import android.text.Html;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.DatePicker;
import android.widget.Button;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.inputmethod.InputMethodManager;
import android.view.WindowManager.LayoutParams;
import android.view.View.OnClickListener;
import android.view.Window;
import android.content.Intent;
import android.widget.TextView.OnEditorActionListener;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Date;
import java.sql.ResultSet;

public class MainActivity extends MyActivity {
    private Calendar mCurrentDate;
    private boolean mInDateChangeFlag;
    private int mGpsLoggerSeqNo;
    private static final int mPhotoViewSize = 229;
    private static final int mGpsViewSize = 229;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
/*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(this)) {
                // Do stuff here
            } else {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return;
            }
        }
*/
        try {
            Window window = getWindow();
            window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            window.requestFeature(Window.FEATURE_CUSTOM_TITLE);
            setContentView(R.layout.activity_main);
            window.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_main);

            mCurrentDate = getCalendar();
            Intent intent = getIntent();
            Date date = (Date)intent.getSerializableExtra("Date");
            if (date != null)
                mCurrentDate.setTime(date);

            DatePicker datePicker = (DatePicker)findViewById(R.id.datePicker);
            datePicker.init(mCurrentDate.get(Calendar.YEAR), mCurrentDate.get(Calendar.MONTH), mCurrentDate.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {
                @Override
                public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    try {
                        if (!mInDateChangeFlag) {
                            EditText editTextSearch = (EditText)findViewById(R.id.editTextSearch);
                            if (editTextSearch.length() != 0)
                                editTextSearch.setText("");

                            ChangeCalendar(false);

                            ListView listView = (ListView)findViewById(R.id.listViewDate);
                            listView.setItemChecked(mCurrentDate.get(Calendar.DAY_OF_MONTH) - 1, true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            ListView listViewDate = (ListView)findViewById(R.id.listViewDate);
            listViewDate.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        ListItem listItem = (ListItem)parent.getItemAtPosition(position);
                        dispData(listItem.mCalendar, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            Button buttonSearch = (Button)findViewById(R.id.buttonMapSearch);
            buttonSearch.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    EditText editTextSearch = (EditText)findViewById(R.id.editTextSearch);
                    if (editTextSearch.getText().length() == 0) {
                        final CharSequence[] items = {"写真検索", "GPSログ検索", "携帯GPS検索"};

                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("検索")
                                .setItems(items, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int item) {
                                        Intent intent = new Intent(MainActivity.this, MapActivity.class);
                                        intent.putExtra("MapSearch", item + 1);
                                        startActivity(intent);
                                    }
                                }).show();
                    } else {
                        try {
                            Database db = new Database();
                            dispMonthList(db);
                            db.close();

                            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            EditText editTextSearch = (EditText)findViewById(R.id.editTextSearch);
            editTextSearch.setOnEditorActionListener(new OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    try {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                            Database db = new Database();
                            dispMonthList(db);
                            db.close();

                            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                }
            });

            Button buttonToday = (Button)findViewById(R.id.buttonMainToday);
            if (date == null) {
                buttonToday.setOnClickListener(new OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        try {
                            EditText editTextSearch = (EditText)findViewById(R.id.editTextSearch);
                            if (editTextSearch.length() != 0)
                                editTextSearch.setText("");

                            dispData(getCalendar(), true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                ((ViewGroup)findViewById(R.id.layoutMainTitle)).removeView(buttonToday);
            }

            Button buttonEdit = (Button)findViewById(R.id.buttonMainEdit);
            buttonEdit.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, EditActivity.class);
                    intent.putExtra("Date", mCurrentDate);
                    startActivityForResult(intent, 0);
                }
            });

            Button buttonPlace = (Button)findViewById(R.id.buttonMainPlace);
            buttonPlace.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, PlaceActivity.class);
                    startActivityForResult(intent, 0);
                }
            });

            ListView listViewKeitaiGps = (ListView)findViewById(R.id.listViewKeitaiGps);
            listViewKeitaiGps.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    KeitaiGpsItem keitaiGpsItem = (KeitaiGpsItem)parent.getItemAtPosition(position);
                    Intent intent = new Intent(MainActivity.this, MapActivity.class);
                    intent.putExtra("KeitaiGpsSeqNo", keitaiGpsItem.mSeqNo);
                    startActivity(intent);
                }
            });

            View photoView = findViewById(R.id.imageViewPhoto);
            photoView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, PhotoActivity.class);
                    intent.putExtra("Date", mCurrentDate);
                    startActivity(intent);
                }
            });

            View gpsView = findViewById(R.id.imageViewGps);
            gpsView.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, MapActivity.class);
                    intent.putExtra("GpsSeqNo", mGpsLoggerSeqNo);
                    startActivity(intent);
                }
            });

            ChangeCalendar(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (resultCode != 0)
                ChangeCalendar(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ChangeCalendar(boolean bForce) throws Exception {
        DatePicker datePicker = (DatePicker)findViewById(R.id.datePicker);
        Calendar calendar = getCalendar();
        calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
        dispData(calendar, bForce);
    }

    private void dispData(Calendar calendar, boolean bForce) throws Exception {
        Calendar oldDate = (Calendar)mCurrentDate.clone();

        if (bForce || !calendar.equals(mCurrentDate)) {
            mCurrentDate = calendar;

            Database db = new Database();

            EditText searchText = (EditText)findViewById(R.id.editTextSearch);
            if (bForce || ((mCurrentDate.get(Calendar.YEAR) != oldDate.get(Calendar.YEAR) || mCurrentDate.get(Calendar.MONTH) != oldDate.get(Calendar.MONTH)) && searchText.getText().length() == 0)) {
                dispMonthList(db);
            }

            mInDateChangeFlag = true;
            DatePicker datePicker = (DatePicker)findViewById(R.id.datePicker);
            datePicker.updateDate(mCurrentDate.get(Calendar.YEAR), mCurrentDate.get(Calendar.MONTH), mCurrentDate.get(Calendar.DAY_OF_MONTH));
            mInDateChangeFlag = false;

            dispDayData(db);

            db.close();
        }
    }

    private void dispMonthList(Database db) throws Exception {
        ArrayAdapter<ListItem> adapter = new ArrayAdapter<ListItem>(this, R.layout.row_date) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView)super.getView(position, convertView, parent);
                ListItem listItem = getItem(position);
                view.setText(Html.fromHtml(listItem.mDispText));
                switch (listItem.mCalendar.get(Calendar.DAY_OF_WEEK)) {
                    case 1:
                        view.setTextColor(Color.RED);
                        break;
                    case 7:
                        view.setTextColor(Color.BLUE);
                        break;
                    default:
                        view.setTextColor(Color.BLACK);
                        break;
                }
                return view;
            }
        };
        String sql;

        EditText searchText = (EditText)findViewById(R.id.editTextSearch);
        if (searchText.getText().length() == 0) {
            Calendar date = (Calendar)mCurrentDate.clone();
            date.set(Calendar.DAY_OF_MONTH, 1);

            sql = String.format("SELECT cl_date,"
                    + "(SELECT COUNT(*) FROM t_comment WHERE cm_date=cl_date) AS cm_count,"
                    + "(SELECT COUNT(*) FROM t_event WHERE ev_date=cl_date) AS ev_count,"
                    + "(SELECT COUNT(*) FROM t_train WHERE tr_date=cl_date) AS tr_count,"
                    + "(SELECT COUNT(*) FROM t_photo WHERE ph_date=cl_date) AS ph_count,"
                    + "(SELECT COUNT(*) FROM t_keitai_gps WHERE kg_date=cl_date) AS kg_count,"
                    + "(SELECT COUNT(*) FROM t_gps_logger WHERE gl_date=cl_date) AS gl_count,"
                    + "(SELECT wk_step / 1000 FROM t_walking WHERE wk_date=cl_date) AS wk_count "
                    + "FROM (SELECT date %s + arr.i AS cl_date FROM generate_series(0,%d) AS arr(i)) T "
                    + "ORDER BY cl_date", Database.sqlDate(date.getTime()), date.getActualMaximum(Calendar.DATE) - 1);
        } else {
            String searchEqu = searchText.getText().toString();
            String[] searchTemp = searchEqu.split(" ");
            String[] searchTemp2 = new String[searchTemp.length];
            for (int i = 0; i < searchTemp.length; i++) {
                searchTemp2[i] = String.format("\t ILIKE '%%%s%%'", searchTemp[i]);
            }
            String searchLike = MyUtils.join(searchTemp2, " AND ");
            String commentLike = searchLike.replaceAll("\t", "cm_comment");
            String photoLike1 = searchLike.replaceAll("\t", "ph_folder_name");
            String photoLike2 = searchLike.replaceAll("\t", "ph_comment");
            String photoLike = String.format("%s OR %s", photoLike1, photoLike2);
            String keitaiGpsLike1 = searchLike.replaceAll("\t", "kg_address");
            String keitaiGpsLike2 = searchLike.replaceAll("\t", "kg_comment");
            String keitaiGpsLike = String.format("%s OR %s", keitaiGpsLike1, keitaiGpsLike2);
            String gpsLoggerLike = searchLike.replaceAll("\t", "gc_comment");
            sql = String.format("SELECT s1_date AS cl_date,"
                    + "(SELECT COUNT(*) FROM t_comment WHERE cm_date=s1_date) AS cm_count,"
                    + "(SELECT COUNT(*) FROM t_event WHERE ev_date=s1_date) AS ev_count,"
                    + "(SELECT COUNT(*) FROM t_train WHERE tr_date=s1_date) AS tr_count,"
                    + "(SELECT COUNT(*) FROM t_photo WHERE ph_date=s1_date) AS ph_count,"
                    + "(SELECT COUNT(*) FROM t_keitai_gps WHERE kg_date=s1_date) AS kg_count,"
                    + "(SELECT COUNT(*) FROM t_gps_logger WHERE gl_date=s1_date) AS gl_count,"
                    + "(SELECT wk_step / 1000 FROM t_walking WHERE wk_date=s1_date) AS wk_count "
                    + "FROM ("
                    + "SELECT cm_date AS s1_date FROM t_comment WHERE %s UNION "
                    + "SELECT ev_date AS s1_date FROM t_event WHERE ev_event_id IN (SELECT em_event_id FROM m_event WHERE em_text='%s') UNION "
                    + "SELECT tr_date AS s1_date FROM t_train WHERE tr_from_line='%s' OR tr_from_station='%s' OR tr_to_line='%s' OR tr_to_station='%s' UNION "
                    + "SELECT ph_date AS s1_date FROM t_photo WHERE %s UNION "
                    + "SELECT kg_date AS s1_date FROM t_keitai_gps WHERE %s UNION "
                    + "SELECT gl_date AS s1_date FROM t_gps_logger JOIN t_gps_comment ON gc_gps_logger_id=gl_seq_no WHERE %s"
                    + ") T "
                    + "ORDER BY s1_date", commentLike, searchEqu, searchEqu, searchEqu, searchEqu, searchEqu, photoLike, keitaiGpsLike, gpsLoggerLike);
        }

        final String[] week = {"日", "月", "火", "水", "木", "金", "土"};

        ResultSet rs = db.query(sql);
        int pos = -1;
        while (rs.next()) {
            Calendar cl = Calendar.getInstance();
            cl.setTime(rs.getDate("cl_date"));

            if (cl.equals(mCurrentDate))
                pos = adapter.getCount();

            ListItem listItem = new ListItem();
            listItem.mDispText = String.format("%04d/%02d/%02d（%s）%s %s %s %s %s %s %s",
                    cl.get(Calendar.YEAR),
                    cl.get(Calendar.MONTH) + 1,
                    cl.get(Calendar.DAY_OF_MONTH),
                    week[cl.get(Calendar.DAY_OF_WEEK) - 1],
                    getDispCount(rs.getInt("cm_count")),
                    getDispCount(rs.getInt("ev_count")),
                    getDispCount(rs.getInt("tr_count")),
                    getDispCount(rs.getInt("kg_count")),
                    getDispCount(rs.getInt("wk_count")),
                    getDispCount(rs.getInt("ph_count")),
                    getDispCount(rs.getInt("gl_count")));
            listItem.mCalendar = cl;
            adapter.add(listItem);
        }
        ListView listView = (ListView)findViewById(R.id.listViewDate);
        listView.setAdapter(adapter);
        if (pos != -1)
            listView.setItemChecked(pos, true);
    }

    private String getDispCount(int count) {
        boolean setColor = false;

        if (count >= 100) {
            count /= 100;
            setColor = true;
        } else if (count >= 10) {
            count /= 10;
            setColor = true;
        }

        if (setColor)
            return String.format("<b>%d</b>", count);
        else
            return String.format("%d", count);
    }

    private Calendar getCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private void dispDayData(Database db) throws Exception {
        String date = Database.sqlDate(mCurrentDate.getTime());
        ResultSet rs;
        String sql;
        TextView textView;
        ListView listView;
        ScrollView scrollView;
        ArrayAdapter<String> stringAdapter;
        ArrayAdapter<KeitaiGpsItem> keitaiGpsAdapter;

        sql = String.format("SELECT cm_comment FROM t_comment WHERE cm_date=%s ORDER BY cm_seq_no", date);
        rs = db.query(sql);
        textView = (TextView)findViewById(R.id.textViewComment);
        textView.setText(rs.next() ? rs.getString("cm_comment") : "");
        scrollView = (ScrollView)findViewById(R.id.scrollViewComment);
        scrollView.setScrollY(0);

        sql = String.format("SELECT em_text FROM t_event JOIN m_event ON ev_event_id=em_event_id WHERE ev_date=%s ORDER BY ev_seq_no", date);
        rs = db.query(sql);
        stringAdapter = new ArrayAdapter<String>(this, R.layout.row_item) {
            @Override
            public boolean isEnabled(int position) {
                return false;
            }
        };
        while (rs.next()) {
            stringAdapter.add(rs.getString("em_text"));
        }
        listView = (ListView)findViewById(R.id.listViewEvent);
        listView.setAdapter(stringAdapter);

        sql = String.format("SELECT tr_from_line,tr_from_station,tr_to_line,tr_to_station FROM t_train WHERE tr_date=%s ORDER BY tr_seq_no", date);
        rs = db.query(sql);
        stringAdapter = new ArrayAdapter<String>(this, R.layout.row_item) {
            @Override
            public boolean isEnabled(int position) {
                return false;
            }
        };
        while (rs.next()) {
            String fromLine = rs.getString("tr_from_line");
            String fromStation = rs.getString("tr_from_station");
            String toLine = rs.getString("tr_to_line");
            String toStation = rs.getString("tr_to_station");
            if (fromLine.equals(toLine))
                stringAdapter.add(String.format("%s %s → %s", fromLine, fromStation, toStation));
            else
                stringAdapter.add(String.format("%s %s → %s %s", fromLine, fromStation, toLine, toStation));
        }
        listView = (ListView)findViewById(R.id.listViewTrain);
        listView.setAdapter(stringAdapter);

        sql = String.format("SELECT kg_seq_no,kg_datetime,COALESCE(kg_address,'不明') AS kg_address FROM t_keitai_gps WHERE kg_date=%s ORDER BY kg_seq_no", date);
        rs = db.query(sql);
        keitaiGpsAdapter = new ArrayAdapter<KeitaiGpsItem>(this, R.layout.row_item) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView)super.getView(position, convertView, parent);
                KeitaiGpsItem keitaiGpsItem = getItem(position);
                view.setText(keitaiGpsItem.mDispText);
                return view;
            }
        };
        while (rs.next()) {
            KeitaiGpsItem keitaiGpsItem = new KeitaiGpsItem();
            keitaiGpsItem.mDispText = String.format("%s %s", rs.getString("kg_datetime").subSequence(11, 19), rs.getString("kg_address"));
            keitaiGpsItem.mSeqNo = rs.getInt("kg_seq_no");
            keitaiGpsAdapter.add(keitaiGpsItem);
        }
        listView = (ListView)findViewById(R.id.listViewKeitaiGps);
        listView.setAdapter(keitaiGpsAdapter);

        sql = String.format("SELECT wk_step,wk_calorie,wk_fat,wk_distance,wk_time,wk_speed FROM t_walking WHERE wk_date=%s", date);
        rs = db.query(sql);
        if (rs.next()) {
            textView = (TextView)findViewById(R.id.textViewWalkingStep);
            textView.setText(rs.getString("wk_step"));

            textView = (TextView)findViewById(R.id.textViewWalkingDistance);
            textView.setText(rs.getString("wk_distance"));

            textView = (TextView)findViewById(R.id.textViewWalkingCalorie);
            textView.setText(rs.getString("wk_calorie"));

            textView = (TextView)findViewById(R.id.textViewWalkingFat);
            textView.setText(rs.getString("wk_fat"));
        } else {
            textView = (TextView)findViewById(R.id.textViewWalkingStep);
            textView.setText("");

            textView = (TextView)findViewById(R.id.textViewWalkingDistance);
            textView.setText("");

            textView = (TextView)findViewById(R.id.textViewWalkingCalorie);
            textView.setText("");

            textView = (TextView)findViewById(R.id.textViewWalkingFat);
            textView.setText("");
        }

        ImageView photoView = (ImageView)findViewById(R.id.imageViewPhoto);
        sql = String.format("SELECT ph_date,ph_folder_name,ph_file_name FROM t_photo WHERE ph_date=%s ORDER BY ph_datetime LIMIT 1", date);
        rs = db.query(sql);
        if (rs.next()) {
            Bitmap bitmap = MyUtils.getBitmapFromServer(rs.getDate("ph_date"), rs.getString("ph_folder_name"), rs.getString("ph_file_name"), mPhotoViewSize);
            photoView.setImageBitmap(bitmap);
            photoView.setClickable(true);

            textView = (TextView)findViewById(R.id.textMapTitle);
            textView.setText(rs.getString("ph_folder_name"));

            sql = String.format("SELECT COUNT(*) FROM t_photo WHERE ph_date=%s", date);
            rs = db.query(sql);
            rs.next();
            textView = (TextView)findViewById(R.id.textViewPhotoNum);
            textView.setText(String.format("%d枚", rs.getInt(1)));
        } else {
            photoView.setImageBitmap(null);
            photoView.setClickable(false);

            textView = (TextView)findViewById(R.id.textMapTitle);
            textView.setText("");

            textView = (TextView)findViewById(R.id.textViewPhotoNum);
            textView.setText("0枚");
        }

        ImageView gpsView = (ImageView)findViewById(R.id.imageViewGps);
        sql = String.format("SELECT gl_seq_no,gl_point_data,gl_point_num FROM t_gps_logger WHERE gl_date=%s ORDER BY gl_seq_no LIMIT 1", date);
        rs = db.query(sql);
        if (rs.next()) {
            mGpsLoggerSeqNo = rs.getInt("gl_seq_no");
            int pointNum = rs.getInt("gl_point_num");
            String url = String.format("http://maps.googleapis.com/maps/api/staticmap?size=%dx%d&sensor=false&language=ja&path=color:0xff00aaaa|weight:4", mGpsViewSize, mGpsViewSize);

            int step;
            if (pointNum <= 50) {
                step = 1;
            } else {
                step = (pointNum - 1) / 49;
            }

            ArrayList<GpsData> gpsDataList = GpsData.getGpsData(rs.getBytes("gl_point_data"), step);

            for (int i = 0; i < gpsDataList.size(); i++) {
                url += String.format("|%f,%f", gpsDataList.get(i).mPos.latitude, gpsDataList.get(i).mPos.longitude);
            }

            Bitmap bitmap = MyUtils.getBitmapFromURL(url);
            gpsView.setImageBitmap(bitmap);
            gpsView.setClickable(true);
        } else {
            gpsView.setImageBitmap(null);
            gpsView.setClickable(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private class ListItem {
        public String mDispText;
        public Calendar mCalendar;
    }

    private class KeitaiGpsItem {
        public String mDispText;
        int mSeqNo;
    }
}
