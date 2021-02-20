package jp.co.troot.llog

import android.os.StrictMode
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement
import java.text.SimpleDateFormat
import java.util.*

// データベースアクセスクラス
internal class Database {
    private val mCon: Connection
    private val mStmt: Statement

    fun query(sql: String): ResultSet {
        return mStmt.executeQuery(sql)
    }

    fun exec(sql: String) {
        mStmt.execute(sql)
    }

    fun close() {
        mStmt.close()
        mCon.close()
    }

    companion object {
		fun sqlDate(date: Date): String {
            val sdf = SimpleDateFormat("''yyyy'-'MM'-'dd''", Locale.JAPANESE)
            return sdf.format(date)
        }

		fun sqlString(str: String): String {
            return if (str.isEmpty()) "null" else "'" + str.replace("'", "\\'") + "'"
        }

		fun sqlStringArray(array: Array<String?>): String {
            return "'{\"" + array.joinToString("\",\"") + "\"}'"
        }
    }

    init {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
        Class.forName("org.postgresql.Driver")
        mCon = DriverManager.getConnection("jdbc:postgresql://inet.troot.co.jp/llog", "postgres", "")
        mStmt = mCon.createStatement()
    }
}