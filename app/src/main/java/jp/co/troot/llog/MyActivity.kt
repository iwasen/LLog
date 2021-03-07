package jp.co.troot.llog

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.provider.Settings

abstract class MyActivity : Activity() {
    override fun onStart() {
        super.onStart()
        if (!mForeground) {
            mRotation = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            if (mRotation == 0) Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
        }
        mForeground = true
    }

    override fun onStop() {
        super.onStop()
        if (!checkRunningAppProcess()) mForeground = false
        if (!mForeground) {
            if (mRotation == 0) {
                if (Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) != 0) Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            }
        }
    }

    private fun checkRunningAppProcess(): Boolean {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val processInfoList = am.runningAppProcesses
        for (info in processInfoList) {
            if (info.processName == packageName) {
                if (info.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    return true
                }
            }
        }
        return false
    }

    // ホームボタンが押されたら終了する（次回最初から起動させるため）
    override fun onUserLeaveHint() {
        finish()
    }

    companion object {
        private var mForeground = false
        private var mRotation = 0
    }
}