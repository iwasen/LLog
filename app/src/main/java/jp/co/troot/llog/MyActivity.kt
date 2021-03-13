package jp.co.troot.llog

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings


open class MyActivity : Activity() {
    private var homeButtonReceiver: HomeButtonReceive? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /** レシーバーを登録  */
        homeButtonReceiver = HomeButtonReceive()
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        registerReceiver(homeButtonReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()

        /** レシーバーの登録を解除  */
        if (homeButtonReceiver != null) {
            unregisterReceiver(homeButtonReceiver)
            homeButtonReceiver = null
        }
    }

    /** HOMEボタンの押下を受け取るレシーバー  */
    class HomeButtonReceive : BroadcastReceiver() {
        override fun onReceive(arg0: Context?, arg1: Intent?) {
            if (arg0 is Activity) {
                arg0.finish()
            }
        }
    }

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

    companion object {
        private var mForeground = false
        private var mRotation = 0
    }
}