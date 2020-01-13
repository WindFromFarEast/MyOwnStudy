package com.xiangweixin.myownstudy.util

import android.content.Context
import android.util.Log
import android.widget.Toast

fun Context.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun logi(tag: String = "", msg: String) {
    Log.i("XWX-$tag", msg)
}

fun logw(tag: String = "", msg: String) {
    Log.w("XWX-$tag", msg)
}

fun loge(tag: String = "", msg: String) {
    Log.e("XWX-$tag", msg)
}