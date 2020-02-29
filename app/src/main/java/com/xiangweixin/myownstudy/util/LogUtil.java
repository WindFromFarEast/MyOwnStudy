package com.xiangweixin.myownstudy.util;

import android.util.Log;

public class LogUtil {

    private static final String TAG = "XWX-";

    public static void i(String tag, String msg) {
        Log.i(TAG + tag, msg);
    }

    public static void e(String tag, String msg) {
        Log.e(TAG + tag, msg);
    }

}
