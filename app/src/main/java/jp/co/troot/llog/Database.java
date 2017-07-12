package jp.co.troot.llog;

import android.os.StrictMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;

public class Database {
	private Connection mCon;
	private Statement mStmt;

	Database() throws Exception {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
		Class.forName("org.postgresql.Driver");

		mCon = DriverManager.getConnection("jdbc:postgresql://inet.troot.co.jp/llog", "postgres", "");
		mStmt = mCon.createStatement();
	}

	public ResultSet query(String sql) throws Exception {
		return mStmt.executeQuery(sql);
	}

	public void exec(String sql) throws Exception {
		mStmt.execute(sql);
	}

	public void close() throws Exception {
		mStmt.close();
		mCon.close();
	}

	public static String sqlDate(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("''yyyy'-'MM'-'dd''", Locale.JAPANESE);
		return sdf.format(date);
	}

	public static String sqlString(String str) {
        if (str.length() == 0)
            return "null";
        else
    	    return "'" + str.replace("'", "\\'") + "'";
	}

    public static String sqlNumber(String str) {
        if (str.length() == 0)
            return "null";
        else
            return str;
    }

    public static String sqlStringArray(String[] array) {
        StringBuilder sb = new StringBuilder();

        sb.append("'{\"");
        sb.append(MyUtils.join(array, "\",\""));
        sb.append("\"}'");

        return sb.toString();
    }

    public static String sqlBoolean(boolean b) {
        return b ? "'t'" : "'f'";
    }
}
