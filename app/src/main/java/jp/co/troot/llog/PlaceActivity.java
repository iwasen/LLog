package jp.co.troot.llog;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Array;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PlaceActivity extends MyActivity implements LocationListener, OnMapReadyCallback {
    private GoogleMap mMap;
    private Map<String, Integer> mMarkerMap = new HashMap<String, Integer>();
    private LocationManager mLocationManager;
    private Marker mCurrentLocation;
    private JSONObject mAddress;
    private ArrayList<Marker> mMarkers = new ArrayList<Marker>();
    private int mKind = 1;
    static private String[] mPlaces = {"公園", "坂道", "寺", "神社", "学校", "橋"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Window window = getWindow();
            window.requestFeature(Window.FEATURE_CUSTOM_TITLE);

            setContentView(R.layout.activity_place);

            Configuration config = getResources().getConfiguration();
            if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            window.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_place);

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

            mLocationManager = (LocationManager)this.getSystemService(LOCATION_SERVICE);

            requestLocation();

            LatLng latlng = new LatLng(35.574517, 139.737765);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 16));

            setMarkers();

            Button buttonKind = (Button)findViewById(R.id.buttonPlaceKind);
            buttonKind.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        selectKind();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            Button buttonLocation = (Button)findViewById(R.id.buttonPlaceLocation);
            buttonLocation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestLocation();
                }
            });

            final int selectedColor = 0xffaaaaff;
            Button buttonMap = (Button)findViewById(R.id.buttonPlaceMap);
            buttonMap.setBackgroundColor(selectedColor);
            buttonMap.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    Button buttonMap = (Button)findViewById(R.id.buttonPlaceMap);
                    Button buttonSatellite = (Button)findViewById(R.id.buttonPlaceSatellite);
                    Button buttonHybrid = (Button)findViewById(R.id.buttonPlaceHybrid);

                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    buttonMap.setBackgroundColor(selectedColor);
                    buttonSatellite.setBackgroundResource(android.R.drawable.btn_default_small);
                    buttonHybrid.setBackgroundResource(android.R.drawable.btn_default_small);
                }
            });

            Button buttonSatellite = (Button)findViewById(R.id.buttonPlaceSatellite);
            buttonSatellite.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    Button buttonMap = (Button)findViewById(R.id.buttonPlaceMap);
                    Button buttonSatellite = (Button)findViewById(R.id.buttonPlaceSatellite);
                    Button buttonHybrid = (Button)findViewById(R.id.buttonPlaceHybrid);

                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    buttonMap.setBackgroundResource(android.R.drawable.btn_default_small);
                    buttonSatellite.setBackgroundColor(selectedColor);
                    buttonHybrid.setBackgroundResource(android.R.drawable.btn_default_small);
                }
            });

            Button buttonHybrid = (Button)findViewById(R.id.buttonPlaceHybrid);
            buttonHybrid.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    Button buttonMap = (Button)findViewById(R.id.buttonPlaceMap);
                    Button buttonSatellite = (Button)findViewById(R.id.buttonPlaceSatellite);
                    Button buttonHybrid = (Button)findViewById(R.id.buttonPlaceHybrid);

                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    buttonMap.setBackgroundResource(android.R.drawable.btn_default_small);
                    buttonSatellite.setBackgroundResource(android.R.drawable.btn_default_small);
                    buttonHybrid.setBackgroundColor(selectedColor);
                }
            });

            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    try {
                        if (marker.equals(mCurrentLocation)) {
                            ArrayList<String> messages = new ArrayList<String>();
                            messages.add("住所");
                            messages.add(String.format("　%s", mAddress.getString("address")));

                            messages.add("");
                            messages.add("自宅");
                            messages.add(String.format("　距離：%s", mAddress.getString("home_distance")));
                            messages.add(String.format("　方向：%s", mAddress.getString("home_dir")));

                            messages.add("");
                            messages.add("最寄駅");
                            JSONArray stations = mAddress.getJSONArray("stations");
                            for (int i = 0; i < stations.length(); i++) {
                                JSONObject station = stations.getJSONObject(i);
                                messages.add(String.format("　%s.%s %s（%sに%s先・%s）",
                                        i + 1,
                                        station.getString("line"),
                                        station.getString("name"),
                                        station.getString("direction"),
                                        station.getInt("distance") < 1000 ? String.format("%dm", station.getInt("distance")) : station.getString("distanceKm"),
                                        station.getString("traveltime")
                                        ));
                            }

                            new AlertDialog.Builder(PlaceActivity.this)
                                    .setTitle("現在地情報")
                                    .setMessage(MyUtils.join(messages.toArray(new String[messages.size()]), "\n"))
                                    .setNegativeButton("閉じる", null)
                                    .show();
                        } else {
                            final Integer seqNo = mMarkerMap.get(marker.getId());
                            Database db = new Database();

                            String sql = String.format("SELECT pl_name, pl_address, pl_comment, pl_add_info FROM t_place WHERE pl_seq_no = %d", seqNo);
                            ResultSet rs = db.query(sql);
                            if (rs.next()) {
                                ArrayList<String> messages = new ArrayList<String>();

                                String name = rs.getString("pl_name");
                                if (name != null)
                                    messages.add(String.format("名称：%s", name));

                                String address = rs.getString("pl_address");
                                if (address != null)
                                    messages.add(String.format("場所：%s", address));

                                String comment = rs.getString("pl_comment");
                                if (comment != null)
                                    messages.add(String.format("コメント：%s", comment));

                                Array addInfo = rs.getArray("pl_add_info");
                                if (addInfo != null) {
                                    String[] addInfoStr = (String[]) addInfo.getArray();
                                    switch (mKind) {
                                        case 1:
                                            if (!addInfoStr[0].equals(""))
                                                messages.add(String.format("広さ：%s ㎡", addInfoStr[0]));

                                            messages.add(String.format("ゴミ箱：%s", addInfoStr[1].equals("t") ? "有り" : "無し"));

                                            messages.add(String.format("トイレ：%s", addInfoStr[2].equals("t") ? "有り" : "無し"));
                                            break;
                                        case 3:
                                            messages.add(String.format("宗派：%s", addInfoStr[0]));
                                            break;
                                    }
                                }

                                AlertDialog.Builder dialog = new AlertDialog.Builder(PlaceActivity.this);
                                dialog.setTitle("情報");
                                dialog.setMessage(MyUtils.join(messages.toArray(new String[messages.size()]), "\n"));
                                dialog.setNegativeButton("閉じる", null);
                                dialog.setPositiveButton(
                                        "訪問",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                try {
                                                    Database db = new Database();
                                                    String sql = String.format("SELECT pl_visited FROM t_place WHERE pl_seq_no = %d", seqNo);
                                                    ResultSet rs = db.query(sql);
                                                    if (rs.next()) {
                                                        String date;
                                                        if (rs.getDate("pl_visited") == null)
                                                            date = "current_date";
                                                        else
                                                            date = "null";
                                                        sql = String.format("UPDATE t_place SET pl_visited = %s WHERE pl_seq_no = %d", date, seqNo);
                                                        db.exec(sql);

                                                        setMarkers();
                                                    }
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                });
                                if (mKind == 1) {
                                    dialog.setNeutralButton(
                                            "編集",
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Intent intent = new Intent(PlaceActivity.this, PlaceEditActivity.class);
                                                    intent.putExtra("SeqNo", seqNo);
                                                    startActivity(intent);
                                                }
                                            }
                                    );
                                }
                                dialog.show();
                            }

                            db.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));

        if (mCurrentLocation != null) {
            mCurrentLocation.remove();
        }

        MarkerOptions options = new MarkerOptions();
        try {
            JSONObject postParams = new JSONObject();
            postParams.put("lat", location.getLatitude());
            postParams.put("lon", location.getLongitude());
            mAddress = new JSONObject(MyUtils.requestJSON("http://inet.troot.co.jp/llog/pl_address.php", postParams.toString()));
            options.title(mAddress.getString("address"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        options.position(latlng);
        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        mCurrentLocation = mMap.addMarker(options);

        mLocationManager.removeUpdates(this);
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    private void setMarkers() throws Exception {
        TextView textPlaceTitle = (TextView)findViewById(R.id.textPlaceTitle);
        textPlaceTitle.setText(mPlaces[mKind - 1]);

        for (Marker marker : mMarkers)
            marker.remove();

        mMarkers.clear();
        mMarkerMap.clear();

        Database db = new Database();

        String sql = String.format("SELECT pl_seq_no, pl_name, pl_location[0] AS pl_lat, pl_location[1] AS pl_lon, pl_visited FROM t_place WHERE pl_kind = %d AND pl_location IS NOT NULL", mKind);
        ResultSet rs = db.query(sql);
        while (rs.next()) {
            MarkerOptions options = new MarkerOptions();
            options.position(new LatLng(rs.getDouble("pl_lat"), rs.getDouble("pl_lon")));
            options.title(rs.getString("pl_name"));
            if (rs.getDate("pl_visited") != null)
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
            Marker marker = mMap.addMarker(options);
            mMarkerMap.put(marker.getId(), rs.getInt("pl_seq_no"));
            mMarkers.add(marker);
        }

        db.close();
    }

    private void requestLocation() {
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } else if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        } else {
            Toast ts = Toast.makeText(this, "現在位置が取得できません", Toast.LENGTH_SHORT);
            ts.show();
        }
    }

    private void selectKind() throws Exception {
        new AlertDialog.Builder(PlaceActivity.this)
                .setTitle("種類")
                .setItems(mPlaces, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        try {
                            mKind = item + 1;
                            setMarkers();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).show();
    }

    @Override
    public void onStop() {
        if (mLocationManager != null)
            mLocationManager.removeUpdates(this);

        super.onStop();
    }
}
