package jp.co.troot.llog;

import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.content.Intent;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import java.sql.ResultSet;
import java.util.Calendar;

public class EditActivity extends MyActivity {
    Calendar mDate;
    String mOrgComment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Window window = getWindow();
            window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            window.requestFeature(Window.FEATURE_CUSTOM_TITLE);
            setContentView(R.layout.activity_edit);
            window.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_edit);

            Intent intent = getIntent();
            mDate = (Calendar)intent.getSerializableExtra("Date");

            Button buttonEdit = (Button)findViewById(R.id.buttonEditUpload);
            buttonEdit.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(EditActivity.this, UploadActivity.class);
                    startActivityForResult(intent, 0);
                }
            });

            ((ListView)findViewById(R.id.listViewEvent1)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        EventItem eventItem = (EventItem)parent.getItemAtPosition(position);

                        Database db = new Database();
                        String sql = String.format("INSERT INTO t_event (ev_date,ev_event_id) VALUES (%s,%d)", Database.sqlDate(mDate.getTime()), eventItem.mEventId);
                        db.exec(sql);

                        displayData(db);

                        db.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            ((ListView)findViewById(R.id.listViewEvent2)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        EventItem eventItem = (EventItem)parent.getItemAtPosition(position);

                        Database db = new Database();
                        String sql = String.format("DELETE FROM t_event WHERE ev_date=%s AND ev_event_id=%d", Database.sqlDate(mDate.getTime()), eventItem.mEventId);
                        db.exec(sql);

                        displayData(db);

                        db.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            Button buttonTrainAdd = (Button)findViewById(R.id.buttonTrainAdd);
            buttonTrainAdd.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    try {
                        EditText editTextTrainLineFrom = (EditText)findViewById(R.id.editTextTrainLineFrom);
                        EditText editTextTrainStationFrom = (EditText)findViewById(R.id.editTextTrainStationFrom);
                        EditText editTextTrainLineTo = (EditText)findViewById(R.id.editTextTrainLineTo);
                        EditText editTextTrainStationTo = (EditText)findViewById(R.id.editTextTrainStationTo);

                        Database db = new Database();
                        if (editTextTrainLineFrom.getText().length() != 0 &&
                                editTextTrainStationFrom.getText().length() != 0 &&
                                editTextTrainLineTo.getText().length() != 0 &&
                                editTextTrainStationTo.getText().length() != 0) {
                            String sql = String.format("INSERT INTO t_train (tr_date,tr_from_line,tr_from_station,tr_to_line,tr_to_station) VALUES (%s,%s,%s,%s,%s)",
                                    Database.sqlDate(mDate.getTime()),
                                    Database.sqlString(editTextTrainLineFrom.getText().toString()),
                                    Database.sqlString(editTextTrainStationFrom.getText().toString()),
                                    Database.sqlString(editTextTrainLineTo.getText().toString()),
                                    Database.sqlString(editTextTrainStationTo.getText().toString()));
                            db.exec(sql);
                        }

                        displayData(db);

                        db.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            Button buttonTrainDelete = (Button)findViewById(R.id.buttonTrainDelete);
            buttonTrainDelete.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    try {
                        Database db = new Database();
                        ListView listView = (ListView)findViewById(R.id.listViewTrain);
                        int pos = listView.getCheckedItemPosition();
                        if (pos != -1) {
                            TrainItem trainItem = (TrainItem)listView.getItemAtPosition(pos);
                            String sql = String.format("DELETE FROM t_train WHERE tr_seq_no=%d", trainItem.mSeqNo);
                            db.exec(sql);

                            displayData(db);
                        }
                        db.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            Database db = new Database();
            displayData(db);
            db.close();

            setResult(1, intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        try {
            EditText editTextComment = (EditText)findViewById(R.id.editTextComment);
            String comment = editTextComment.getText().toString().replace("\r", "").replace("\n", "\r\n");
            if (!comment.equals(mOrgComment)) {
                Database db = new Database();
                String sql = String.format("SELECT cm_seq_no FROM t_comment WHERE cm_date=%s ORDER BY cm_seq_no", Database.sqlDate(mDate.getTime()));
                ResultSet rs = db.query(sql);
                if (rs.next()) {
                    int seqNo = rs.getInt("cm_seq_no");
                    if (comment.length() != 0) {
                        sql = String.format("UPDATE t_comment SET cm_comment=%s WHERE cm_seq_no=%d", Database.sqlString(comment), seqNo);
                        db.exec(sql);
                    } else {
                        sql = String.format("DELETE FROM t_comment WHERE cm_seq_no=%d", seqNo);
                        db.exec(sql);
                    }
                } else {
                    if (comment.length() != 0) {
                        sql = String.format("INSERT INTO t_comment (cm_date,cm_comment) VALUES (%s,%s)", Database.sqlDate(mDate.getTime()), Database.sqlString(comment));
                        db.exec(sql);
                    }
                }
                db.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onBackPressed();
    }

    private void displayData(Database db) throws Exception {
        EventAdapter eventAdapter1 = new EventAdapter(this, R.layout.row_edit);
        EventAdapter eventAdapter2 = new EventAdapter(this, R.layout.row_edit);
        TrainAdapter trainAdapter = new TrainAdapter(this, R.layout.row_edit);

        String sql = String.format("SELECT cm_comment FROM t_comment WHERE cm_date=%s ORDER BY cm_seq_no", Database.sqlDate(mDate.getTime()));
        ResultSet rs = db.query(sql);
        if (rs.next()) {
            EditText editTextComment = (EditText)findViewById(R.id.editTextComment);
            editTextComment.setText(rs.getString("cm_comment"));
            mOrgComment = editTextComment.getText().toString();
        } else
            mOrgComment = "";

        sql = String.format("SELECT em_event_id,em_text,ev_event_id FROM m_event LEFT JOIN t_event ON ev_event_id=em_event_id AND ev_date=%s ORDER BY em_event_id", Database.sqlDate(mDate.getTime()));
        rs = db.query(sql);
        while (rs.next()) {
            EventItem eventItem = new EventItem();
            eventItem.mEventId = rs.getInt("em_event_id");
            eventItem.mText = rs.getString("em_text");

            rs.getInt("ev_event_id");
            if (rs.wasNull())
                eventAdapter1.add(eventItem);
            else
                eventAdapter2.add(eventItem);
        }
        ListView listViewEvent1 = (ListView)findViewById(R.id.listViewEvent1);
        listViewEvent1.setAdapter(eventAdapter1);
        ListView listViewEvent2 = (ListView)findViewById(R.id.listViewEvent2);
        listViewEvent2.setAdapter(eventAdapter2);

        sql = String.format("SELECT tr_seq_no,tr_from_line,tr_from_station,tr_to_line,tr_to_station FROM t_train WHERE tr_date=%s ORDER BY tr_seq_no", Database.sqlDate(mDate.getTime()));
        rs = db.query(sql);
        while (rs.next()) {
            TrainItem trainItem = new TrainItem();
            trainItem.mSeqNo = rs.getInt("tr_seq_no");
            trainItem.mText = String.format("%s %s → %s %s", rs.getString("tr_from_line"), rs.getString("tr_from_station"), rs.getString("tr_to_line"), rs.getString("tr_to_station"));
            trainAdapter.add(trainItem);
        }
        ListView listViewTrain = (ListView)findViewById(R.id.listViewTrain);
        listViewTrain.setAdapter(trainAdapter);
    }

    private class EventItem {
        public int mEventId;
        public String mText;
    }

    private class EventAdapter extends ArrayAdapter<EventItem> {
        public EventAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView)super.getView(position, convertView, parent);
            EventItem eventItem = getItem(position);
            view.setText(eventItem.mText);
            return view;
        }
    }

    private class TrainItem {
        public int mSeqNo;
        public String mText;
    }

    private class TrainAdapter extends ArrayAdapter<TrainItem> {
        public TrainAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView)super.getView(position, convertView, parent);
            TrainItem trainItem = getItem(position);
            view.setText(trainItem.mText);
            return view;
        }
    }
}
