package jp.co.troot.llog;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.widget.TextView;

import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

public class MapActivity extends MyActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Map<Marker, Integer> mPhotoMarkerMap = new HashMap<Marker, Integer>();
    private LatLng mSearchCenter;
    private Circle mSearchCircle;
    private boolean mSearchFlag;
    private ArrayList<Marker> mMarkerList;
    private Spinner mSpinner;
    private int mDpi;
    private ArrayList<Polyline> mPolyline;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mDpi = metrics.densityDpi;

            Window window = getWindow();
            window.requestFeature(Window.FEATURE_CUSTOM_TITLE);

            setContentView(R.layout.activity_map);

            Configuration config = getResources().getConfiguration();
            if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            window.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_map);

            MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onMapReady(GoogleMap map) {
        try {
            mMap = map;

            mMap.getUiSettings().setRotateGesturesEnabled(false);

            Database db = new Database();

            Intent intent = getIntent();
            final int gpsSeqNo = intent.getIntExtra("GpsSeqNo", 0);
            if (gpsSeqNo != 0) {
                ArrayList<GpsData> gpsDataList = setPolyline(db, gpsSeqNo);
                setMarkers(db, gpsSeqNo, gpsDataList);
            }

            final int photoSeqNo = intent.getIntExtra("PhotoSeqNo", 0);
            if (photoSeqNo != 0) {
                TextView textView = (TextView)findViewById(R.id.textMapTitle);
                textView.setText("撮影位置");
                setPhotoMarker(db, photoSeqNo);
            }

            final int keitaiGpsSeqNo = intent.getIntExtra("KeitaiGpsSeqNo", 0);
            if (keitaiGpsSeqNo != 0) {
                TextView textView = (TextView)findViewById(R.id.textMapTitle);
                textView.setText("携帯GPS");
                setKeitaiGpsMarker(db, keitaiGpsSeqNo);
            }

            Button buttonSearch = (Button)findViewById(R.id.buttonMapSearch);
            final int mapSearch = intent.getIntExtra("MapSearch", 0);
            if (mapSearch != 0) {
                TextView textView = (TextView)findViewById(R.id.textMapTitle);

                switch (mapSearch) {
                    case 1:
                        textView.setText("写真検索");
                        break;
                    case 2:
                        textView.setText("GPSログ検索");
                        break;
                    case 3:
                        textView.setText("携帯GPS検索");
                        mMarkerList = new ArrayList<Marker>();
                        break;
                }

                buttonSearch.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        toggleSearch();
                    }
                });

                LatLng latlng = new LatLng(35.574517, 139.737765);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 16));

                MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
                View view = ((FrameLayout)((FrameLayout)mapFragment.getView()).getChildAt(0)).getChildAt(0);
                view.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (mSearchFlag) {
                            Projection projection = mMap.getProjection();
                            LatLng latLng = projection.fromScreenLocation(new Point((int)event.getX(), (int)event.getY()));
                            double distance;

                            switch (event.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    if (mSearchCircle != null) {
                                        mSearchCircle.remove();
                                        mSearchCircle = null;
                                    }
                                    if (mSpinner != null) {
                                        MapFragment mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
                                        ((FrameLayout)mapFragment.getView()).removeView(mSpinner);
                                        mSpinner = null;
                                    }
                                    if (mPolyline != null) {
                                        for (Polyline polyline : mPolyline)
                                            polyline.remove();
                                        mPolyline = null;
                                    }
                                    mSearchCenter = latLng;
                                    break;
                                case MotionEvent.ACTION_MOVE:
                                    distance = getDistance(mSearchCenter, latLng);
                                    if (mSearchCircle == null) {
                                        mSearchCircle = mMap.addCircle(new CircleOptions().center(mSearchCenter).radius(distance).strokeColor(Color.MAGENTA).strokeWidth(5));
                                    } else {
                                        mSearchCircle.setRadius(distance);
                                    }
                                    break;
                                case MotionEvent.ACTION_UP:
                                    distance = getDistance(mSearchCenter, latLng);
                                    switch (mapSearch) {
                                        case 1:
                                            searchPhoto(mSearchCenter, distance);
                                            break;
                                        case 2:
                                            searchGpsLogger(mSearchCenter, distance);
                                            break;
                                        case 3:
                                            searchKeitaiGps(mSearchCenter, distance);
                                            break;
                                    }
                                    toggleSearch();
                                    break;
                            }
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
            } else {
                ((ViewGroup)findViewById(R.id.layoutMapTitle)).removeView(buttonSearch);
            }

            db.close();

            final int selectedColor = 0xffaaaaff;
            Button buttonMap = (Button)findViewById(R.id.buttonMapMap);
            buttonMap.setBackgroundColor(selectedColor);
            buttonMap.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    Button buttonMap = (Button)findViewById(R.id.buttonMapMap);
                    Button buttonSatellite = (Button)findViewById(R.id.buttonMapSatellite);
                    Button buttonHybrid = (Button)findViewById(R.id.buttonMapHybrid);

                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    buttonMap.setBackgroundColor(selectedColor);
                    buttonSatellite.setBackgroundResource(android.R.drawable.btn_default_small);
                    buttonHybrid.setBackgroundResource(android.R.drawable.btn_default_small);
                }
            });

            Button buttonSatellite = (Button)findViewById(R.id.buttonMapSatellite);
            buttonSatellite.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    Button buttonMap = (Button)findViewById(R.id.buttonMapMap);
                    Button buttonSatellite = (Button)findViewById(R.id.buttonMapSatellite);
                    Button buttonHybrid = (Button)findViewById(R.id.buttonMapHybrid);

                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    buttonMap.setBackgroundResource(android.R.drawable.btn_default_small);
                    buttonSatellite.setBackgroundColor(selectedColor);
                    buttonHybrid.setBackgroundResource(android.R.drawable.btn_default_small);
                }
            });

            Button buttonHybrid = (Button)findViewById(R.id.buttonMapHybrid);
            buttonHybrid.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    Button buttonMap = (Button)findViewById(R.id.buttonMapMap);
                    Button buttonSatellite = (Button)findViewById(R.id.buttonMapSatellite);
                    Button buttonHybrid = (Button)findViewById(R.id.buttonMapHybrid);

                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    buttonMap.setBackgroundResource(android.R.drawable.btn_default_small);
                    buttonSatellite.setBackgroundResource(android.R.drawable.btn_default_small);
                    buttonHybrid.setBackgroundColor(selectedColor);
                }
            });

            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    Integer seqNo = mPhotoMarkerMap.get(marker);
                    if (seqNo != null) {
                        Intent intent = new Intent(MapActivity.this, PhotoActivity.class);
                        intent.putExtra("SeqNo", seqNo);
                        intent.putExtra("EnableTop", mapSearch != 0);
                        startActivity(intent);
                        return true;
                    } else {
                        return false;
                    }
                }
            });

            mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {
                    Intent intent = new Intent(
                            "android.intent.action.VIEW",
                            Uri.parse(String.format("google.streetview:cbll=%f,%f&cbp=1,0,,0,1", latLng.latitude, latLng.longitude)));
                    startActivity(intent);
                }
            });

            Button buttonMapTop = (Button)findViewById(R.id.buttonMapTop);
            if (intent.getBooleanExtra("EnableTop", false)) {
                buttonMapTop.setOnClickListener(new OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        try {
                            Database db = new Database();
                            String sql = String.format("SELECT gl_date FROM t_gps_logger WHERE gl_seq_no=%d", gpsSeqNo);
                            ResultSet rs = db.query(sql);
                            if (rs.next()) {
                                Intent intent = new Intent(MapActivity.this, MainActivity.class);
                                intent.putExtra("Date", rs.getDate("gl_date"));
                                startActivity(intent);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                ((ViewGroup)findViewById(R.id.layoutMapTitle)).removeView(buttonMapTop);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ArrayList<GpsData> setPolyline(Database db, int seqNo) throws Exception {
        ArrayList<GpsData> gpsDataList = null;

        String sql = String.format("SELECT gl_point_data FROM t_gps_logger WHERE gl_seq_no=%d", seqNo);
        ResultSet rs = db.query(sql);
        if (rs.next()) {
            gpsDataList = GpsData.getGpsData(rs.getBytes("gl_point_data"), 1);
            PolylineOptions po = new PolylineOptions();
            for (int i = 0; i < gpsDataList.size(); i++) {
                po.add(gpsDataList.get(i).mPos);
            }
            po.color(0x99ff00aa);
            po.width(7);
            mMap.addPolyline(po);
            LatLngBounds bounds = GpsData.getBounds(gpsDataList);
            WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
            Display disp = wm.getDefaultDisplay();
            Point size = new Point();
            disp.getSize(size);
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, size.x, size.y - 80, 20));
        }

        return gpsDataList;
    }

    private void setMarkers(Database db, int seqNo, ArrayList<GpsData> gpsDataList) throws Exception {
        double distance = 0;
        double kilo2 = 0;
        int hour2 = -1;

        addKiloMarker(gpsDataList.get(0).mPos, 0, gpsDataList.get(0).mCalendar);
        for (int i = 1; i < gpsDataList.size(); i++) {
            GpsData gpsData1 = gpsDataList.get(i - 1);
            GpsData gpsData2 = gpsDataList.get(i);

            distance += getDistance(gpsData1.mPos, gpsData2.mPos);
            int kilo = (int)Math.floor(distance / 1000);
            if (kilo != kilo2) {
                addKiloMarker(gpsData2.mPos, kilo, gpsData2.mCalendar);
                kilo2 = kilo;
            }

            int hour = gpsData2.mCalendar.get(Calendar.HOUR_OF_DAY);
            if (hour != hour2) {
                if (hour2 != -1) {
                    addHourMarker(gpsData2.mPos, hour, distance);
                }
                hour2 = hour;
            }
        }

        String sql = String.format("SELECT ph_seq_no,ph_datetime,ph_location[0] AS ph_lat,ph_location[1] AS ph_lon FROM t_gps_logger JOIN t_photo ON ph_date=gl_date AND ph_location IS NOT NULL WHERE gl_seq_no=%d ORDER BY ph_seq_no", seqNo);
        ResultSet rs = db.query(sql);
        while (rs.next()) {
            addPhotoMarker(new LatLng(rs.getDouble("ph_lat"), rs.getDouble("ph_lon")), rs.getInt("ph_seq_no"));
        }

        sql = String.format("SELECT gc_point[0] AS gc_lat,gc_point[1] AS gc_lon,gc_comment FROM t_gps_comment WHERE gc_gps_logger_id=%d ORDER BY gc_seq_no", seqNo);
        rs = db.query(sql);
        while (rs.next()) {
            addCommentMarker(new LatLng(rs.getDouble("gc_lat"), rs.getDouble("gc_lon")), rs.getString("gc_comment"));
        }
    }

    private void addKiloMarker(LatLng pos, int kilo, Calendar time) {
        if (kilo > 100)
            kilo = 100;

        MarkerOptions options = new MarkerOptions();
        options.position(pos);
        options.title(String.format("%02d:%02d:%02d", time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), time.get(Calendar.SECOND)));
        options.icon(getMarker(String.format("marker_kilo/marker%d.png", kilo)));
        mMap.addMarker(options);
    }

    private void addHourMarker(LatLng pos, int hour, double distance) {
        MarkerOptions options = new MarkerOptions();
        options.position(pos);
        NumberFormat format = NumberFormat.getNumberInstance();
        options.title(format.format((int)distance) + "m");
        options.icon(getMarker(String.format("marker_hour/hour%d.png", hour)));
        mMap.addMarker(options);
    }

    private void addPhotoMarker(LatLng pos, int seqNo) {
        MarkerOptions options = new MarkerOptions();
        options.position(pos);
        options.icon(getMarker("yellow.png"));
        mPhotoMarkerMap.put(mMap.addMarker(options), seqNo);
    }

    private void addCommentMarker(LatLng pos, String comment) {
        MarkerOptions options = new MarkerOptions();
        options.position(pos);
        options.title(comment);
        options.icon(getMarker("green.png"));
        mMap.addMarker(options);
    }

    private void setPhotoMarker(Database db, int seqNo) throws Exception {
        String sql = String.format("SELECT ph_datetime,ph_location[0] AS ph_lat,ph_location[1] AS ph_lon FROM t_photo WHERE ph_seq_no=%d", seqNo);
        ResultSet rs = db.query(sql);
        if (rs.next()) {
            LatLng latLng = new LatLng(rs.getFloat("ph_lat"), rs.getFloat("ph_lon"));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy'/'MM'/'dd' 'HH':'mm", Locale.JAPANESE);
            MarkerOptions options = new MarkerOptions();
            options.position(latLng);
            options.title(sdf.format(rs.getTimestamp("ph_datetime")));
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
            mMap.addMarker(options);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        }
    }

    private void setKeitaiGpsMarker(Database db, int seqNo) throws Exception {
        String sql = String.format("SELECT kg_datetime,kg_point[0] AS kg_lat,kg_point[1] AS kg_lon FROM t_keitai_gps WHERE kg_seq_no=%d", seqNo);
        ResultSet rs = db.query(sql);
        if (rs.next()) {
            LatLng latLng = new LatLng(rs.getFloat("kg_lat"), rs.getFloat("kg_lon"));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy'/'MM'/'dd' 'HH':'mm':'ss", Locale.JAPANESE);
            MarkerOptions options = new MarkerOptions();
            options.position(latLng);
            options.title(sdf.format(rs.getTimestamp("kg_datetime")));
            mMap.addMarker(options);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        }
    }

    private double getDistance(LatLng pos1, LatLng pos2) {
        double f1 = pos1.latitude;
        double g1 = pos1.longitude;
        double f2 = pos2.latitude;
        double g2 = pos2.longitude;

        double a = 6378136.0;				// 赤道半径(m)
        double e2 = 0.006694470;			// 地球の離心率の自乗

        double rad = Math.PI / 180.0;
        double h1 = 0.0, h2 = 0.0;			// ここでは、標高を無視
        if (g1 < 0) g1 = 360.0 + g1;
        double fr1 = f1 * rad; double gr1 = g1 * rad;
        if (g2 < 0) g2 = 360.0 + g2;
        double fr2 = f2 * rad; double gr2 = g2 * rad;

        double N1 = a / (Math.sqrt(1.0 - e2 * Math.sin(fr1) * Math.sin(fr1)));
        double x1 = (N1 + h1) * Math.cos(fr1) * Math.cos(gr1);
        double y1 = (N1 + h1) * Math.cos(fr1) * Math.sin(gr1);
        double z1 = (N1 * (1.0 - e2) + h1) * Math.sin(fr1);

        double N2 = a / (Math.sqrt(1.0 - e2 * Math.sin(fr2) * Math.sin(fr2)));
        double x2 = (N2 + h2) * Math.cos(fr2) * Math.cos(gr2);
        double y2 = (N2 + h2) * Math.cos(fr2) * Math.sin(gr2);
        double z2 = (N2 * (1.0 - e2) + h2) * Math.sin(fr2);

        double r = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2));	// 直距離
        double wr = Math.asin(r / 2 / a);	// 半射程角(rad)
//    	double w = wr / rad;			// 半射程角(°)
        return a * 2 * wr;			 // 地表面距離
    }

    private void searchPhoto(LatLng centerLatLng, double distance) {
        double c = 6378136 * 2 * Math.PI;
        double dLat = distance / c * 360;
        double dLon = dLat / Math.cos(centerLatLng.latitude / 180 * Math.PI);

        for (Marker marker : mPhotoMarkerMap.keySet())
            marker.remove();
        mPhotoMarkerMap.clear();

        try {
            Database db = new Database();
            String sql = String.format("SELECT ph_seq_no, ph_location[0] AS lat, ph_location[1] AS lon FROM t_photo WHERE circle '((%.9g, %.9g), 1)' @> point(ph_location[0] + (ph_location[0] - %.9g) / %.9g, ph_location[1] + (ph_location[1] - %.9g) / %.9g) ORDER BY ph_datetime", centerLatLng.latitude, centerLatLng.longitude, centerLatLng.latitude, dLat, centerLatLng.longitude, dLon);
            ResultSet rs = db.query(sql);
            while (rs.next()) {
                MarkerOptions options = new MarkerOptions();
                options.position(new LatLng(rs.getDouble("lat"), rs.getDouble("lon")));
                options.icon(getMarker("yellow.png"));
                mPhotoMarkerMap.put(mMap.addMarker(options), rs.getInt("ph_seq_no"));
            }
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void searchGpsLogger(LatLng centerLatLng, double distance) {
        ArrayAdapter<GpsListItem> adapter = new ArrayAdapter<GpsListItem>(this, android.R.layout.simple_spinner_item) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView)super.getView(position, convertView, parent);
                GpsListItem gpsListItem = getItem(position);
                view.setText(gpsListItem.mItemName);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                CheckedTextView view = (CheckedTextView)super.getDropDownView(position, convertView, parent);
                GpsListItem gpsListItem = getItem(position);
                view.setText(gpsListItem.mItemName);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        try {
            URL url = new URL(String.format("http://inet.troot.co.jp/llog/gl_search_log.php?circle_lat=%.9g&circle_lon=%.9g&circle_r=%.9g",
                    centerLatLng.latitude, centerLatLng.longitude, distance));
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String result = br.readLine();

            if (!result.isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy'/'MM'/'dd", Locale.JAPANESE);

                int count = 0;
                Database db = new Database();
                String sql = String.format("SELECT gl_seq_no, gl_date FROM t_gps_logger WHERE gl_seq_no IN (%s) ORDER BY gl_start_time", result);
                ResultSet rs = db.query(sql);
                while (rs.next()) {
                    GpsListItem gpsListItem = new GpsListItem();
                    gpsListItem.mSeqNo = rs.getInt("gl_seq_no");
                    gpsListItem.mItemName = sdf.format(rs.getDate("gl_date"));
                    adapter.add(gpsListItem);
                    count++;
                }

                if (count < 20) {
                    mPolyline = new ArrayList<Polyline>();
                    sql = String.format("SELECT gl_point_data FROM t_gps_logger WHERE gl_seq_no IN (%s) ORDER BY gl_start_time", result);
                    rs = db.query(sql);
                    while (rs.next()) {
                        Random rnd = new Random();
                        ArrayList<GpsData> gpsDataList = GpsData.getGpsData(rs.getBytes("gl_point_data"), 1);
                        PolylineOptions po = new PolylineOptions();
                        for (int i = 0; i < gpsDataList.size(); i++) {
                            po.add(gpsDataList.get(i).mPos);
                        }
                        po.color(0x99000000 | (rnd.nextInt(256) << 16) | (rnd.nextInt(256) << 8) | (rnd.nextInt(256)));
                        po.width(7);
                        mPolyline.add(mMap.addPolyline(po));
                    }
                }

                db.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        GpsListItem gpsListItem = new GpsListItem();
        gpsListItem.mSeqNo = 0;
        gpsListItem.mItemName = String.format("%d 件", adapter.getCount());
        adapter.insert(gpsListItem, 0);

        mSpinner = new Spinner(this);
        mSpinner.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) {
                    GpsListItem gpsListItem = (GpsListItem)parent.getItemAtPosition(position);
                    Intent intent = new Intent(MapActivity.this, MapActivity.class);
                    intent.putExtra("GpsSeqNo", gpsListItem.mSeqNo);
                    intent.putExtra("EnableTop", true);
                    startActivity(intent);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        MapFragment mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
        FrameLayout view = (FrameLayout)mapFragment.getView();
        if (view != null)
            view.addView(mSpinner);
    }

    private void searchKeitaiGps(LatLng centerLatLng, double distance) {
        double c = 6378136 * 2 * Math.PI;
        double d_lat = distance / c * 360;
        double d_lon = d_lat / Math.cos(centerLatLng.latitude / 180 * Math.PI);

        for (Marker marker : mMarkerList)
            marker.remove();
        mMarkerList.clear();

        try {
            Database db = new Database();
            String sql = String.format("SELECT kg_datetime,kg_point[0] AS kg_lat,kg_point[1] AS kg_lon FROM t_keitai_gps WHERE circle '((%.9g, %.9g), 1)' @> point(kg_point[0] + (kg_point[0] - %.9g) / %.9g, kg_point[1] + (kg_point[1] - %.9g) / %.9g) ORDER BY kg_datetime", centerLatLng.latitude, centerLatLng.longitude, centerLatLng.latitude, d_lat, centerLatLng.longitude, d_lon);
            ResultSet rs = db.query(sql);
            while (rs.next()) {
                LatLng latLng = new LatLng(rs.getFloat("kg_lat"), rs.getFloat("kg_lon"));
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy'/'MM'/'dd' 'HH':'mm':'ss", Locale.JAPANESE);
                MarkerOptions options = new MarkerOptions();
                options.position(latLng);
                options.title(sdf.format(rs.getTimestamp("kg_datetime")));
                mMarkerList.add(mMap.addMarker(options));
            }
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleSearch() {
        Button buttonSearch = (Button)findViewById(R.id.buttonMapSearch);
        if (mSearchFlag) {
            buttonSearch.setBackgroundResource(android.R.drawable.btn_default_small);
            mSearchFlag = false;
        } else {
            buttonSearch.setBackgroundResource(R.color.orange);
            mSearchFlag = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mSpinner != null)
            mSpinner.setSelection(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private BitmapDescriptor getMarker(String path) {
        BitmapDescriptor bmpdesc = null;
        Matrix matrix = new Matrix();
        matrix.setScale(mDpi / 180f, mDpi / 180f);
        try {
            InputStream is = getResources().getAssets().open(path);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            bmpdesc =BitmapDescriptorFactory.fromBitmap(Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bmpdesc;
    }

    private class GpsListItem {
        int mSeqNo;
        String mItemName;
    }
}
