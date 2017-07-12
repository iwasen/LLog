package jp.co.troot.llog;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;

import java.sql.Array;
import java.sql.ResultSet;

/**
 * Created by aizawa on 2015/01/07.
 */
public class PlaceEditActivity extends MyActivity {
    private int mSeqNo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_place_edit);

            Intent intent = getIntent();
            mSeqNo = intent.getIntExtra("SeqNo", 0);

            Database db = new Database();

            String sql = String.format("SELECT pl_seq_no, pl_name, pl_yomi, pl_address, pl_comment, pl_add_info FROM t_place WHERE pl_seq_no = %d", mSeqNo);
            ResultSet rs = db.query(sql);
            if (rs.next()) {
                EditText editTextName = (EditText)findViewById(R.id.editTextName);
                editTextName.setText(rs.getString("pl_name"));

                EditText editTextYomi = (EditText)findViewById(R.id.editTextYomi);
                editTextYomi.setText(rs.getString("pl_yomi"));

                EditText editTextAddress = (EditText)findViewById(R.id.editTextAddress);
                editTextAddress.setText(rs.getString("pl_address"));

                EditText editTextComment = (EditText)findViewById(R.id.editTextComment);
                editTextComment.setText(rs.getString("pl_comment"));

                Array addInfo = rs.getArray("pl_add_info");
                if (addInfo != null) {
                    String[] addInfoStr = (String[])addInfo.getArray();

                    EditText editTextArea = (EditText)findViewById(R.id.editTextArea);
                    editTextArea.setText(addInfoStr[0]);

                    CheckBox checkBoxTrash = (CheckBox)findViewById(R.id.checkBoxTrash);
                    checkBoxTrash.setChecked(addInfoStr[1].equals("t"));

                    CheckBox checkBoxToilet = (CheckBox)findViewById(R.id.checkBoxToilet);
                    checkBoxToilet.setChecked(addInfoStr[2].equals("t"));
                }
            }

            db.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        try {
            EditText editTextName = (EditText)findViewById(R.id.editTextName);
            EditText editTextYomi = (EditText)findViewById(R.id.editTextYomi);
            EditText editTextAddress = (EditText)findViewById(R.id.editTextAddress);
            EditText editTextComment = (EditText)findViewById(R.id.editTextComment);
            EditText editTextArea = (EditText)findViewById(R.id.editTextArea);
            CheckBox checkBoxTrash = (CheckBox)findViewById(R.id.checkBoxTrash);
            CheckBox checkBoxToilet = (CheckBox)findViewById(R.id.checkBoxToilet);

            String[] addInfo = new String[3];
            addInfo[0] = editTextArea.getText().toString();
            addInfo[1] = checkBoxTrash.isChecked() ? "t" : "f";
            addInfo[2] = checkBoxToilet.isChecked() ? "t" : "f";

            String sql = String.format(
                    "UPDATE t_place SET pl_name = %s, pl_yomi = %s, pl_address = %s, pl_comment = %s, pl_add_info = %s WHERE pl_seq_no = %d",
                    Database.sqlString(editTextName.getText().toString()),
                    Database.sqlString(editTextYomi.getText().toString()),
                    Database.sqlString(editTextAddress.getText().toString()),
                    Database.sqlString(editTextComment.getText().toString()),
                    Database.sqlStringArray(addInfo),
                    mSeqNo);

            Database db = new Database();
            db.exec(sql);
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onBackPressed();
    }
}