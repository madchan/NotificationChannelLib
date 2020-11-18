package com.madchan.notificationchannellib

import android.app.Application
import android.content.Context

class BaseApplication : Application() {

    init {
        mContext = this
    }

    companion object {
        lateinit var mContext: Context
        fun getContext() = mContext
    }
}