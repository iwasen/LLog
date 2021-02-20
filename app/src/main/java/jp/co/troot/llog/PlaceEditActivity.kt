package jp.co.troot.llog

import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import jp.co.troot.llog.Database.Companion.sqlString
import jp.co.troot.llog.Database.Companion.sqlStringArray
import java.util.*

/**
 * Created by aizawa on 2015/01/07.
 */
class PlaceEditActivity : MyActivity() {
    private var mSeqNo = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 画面設定
        setContentView(R.layout.activity_place_edit)

        // seqNo取得
        mSeqNo = intent.getIntExtra("SeqNo", 0)

        // 場所情報表示
        val db = Database()
        val sql = String.format("SELECT pl_seq_no, pl_name, pl_yomi, pl_address, pl_comment, pl_add_info FROM t_place WHERE pl_seq_no = %d", mSeqNo)
        val rs = db.query(sql)
        if (rs.next()) {
            findViewById<EditText>(R.id.editTextName).setText(rs.getString("pl_name"))
            findViewById<EditText>(R.id.editTextYomi).setText(rs.getString("pl_yomi"))
            findViewById<EditText>(R.id.editTextAddress).setText(rs.getString("pl_address"))
            findViewById<EditText>(R.id.editTextComment).setText(rs.getString("pl_comment"))
            val addInfo = rs.getArray("pl_add_info")
            if (addInfo != null) {
                val addInfoStr = addInfo.array as Array<*>
                findViewById<EditText>(R.id.editTextArea).setText(addInfoStr[0] as String)
                findViewById<CheckBox>(R.id.checkBoxTrash).isChecked = addInfoStr[1] == "t"
                findViewById<CheckBox>(R.id.checkBoxToilet).isChecked = addInfoStr[2] == "t"
            }
        }
        db.close()
    }

    // 戻りボタンクリック処理
    override fun onBackPressed() {
        // 変更された情報をDBに保存
        val addInfo = arrayOfNulls<String>(3)
        addInfo[0] = findViewById<EditText>(R.id.editTextArea).text.toString()
        addInfo[1] = if (findViewById<CheckBox>(R.id.checkBoxTrash).isChecked) "t" else "f"
        addInfo[2] = if (findViewById<CheckBox>(R.id.checkBoxToilet).isChecked) "t" else "f"
        val sql = String.format(Locale.US,
                "UPDATE t_place SET pl_name = %s, pl_yomi = %s, pl_address = %s, pl_comment = %s, pl_add_info = %s WHERE pl_seq_no = %d",
                sqlString(findViewById<EditText>(R.id.editTextName).text.toString()),
                sqlString(findViewById<EditText>(R.id.editTextYomi).text.toString()),
                sqlString(findViewById<EditText>(R.id.editTextAddress).text.toString()),
                sqlString(findViewById<EditText>(R.id.editTextComment).text.toString()),
                sqlStringArray(addInfo),
                mSeqNo)
        val db = Database()
        db.exec(sql)
        db.close()

        super.onBackPressed()
    }
}