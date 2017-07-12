package jp.co.troot.llog;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.provider.Settings;

public class MyActivity extends Activity {
	static private boolean mForeground;
	static private int mRotation;

	@Override
	protected void onStart() {
		super.onStart();

		if (!mForeground) {
			mRotation = Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
			if (mRotation == 0)
				Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1);
		}

		mForeground = true;
	}

	@Override
	protected void onStop() {
		super.onStop();

		if (!checkRunningAppProcess())
			mForeground = false;

		if (!mForeground) {
			if (mRotation == 0) {
				if (Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) != 0)
					Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
			}
		}
	}

	private boolean checkRunningAppProcess() {
		ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> processInfoList = am.getRunningAppProcesses();
		for (RunningAppProcessInfo info : processInfoList) {
			if (info.processName.equals(getPackageName())) {
				if (info.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
					return true;
				}
			}
		}
		return false;
	}
}
