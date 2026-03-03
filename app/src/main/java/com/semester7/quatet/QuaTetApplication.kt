package com.semester7.quatet

import android.app.Application
import com.semester7.quatet.data.remote.RetrofitClient

class QuaTetApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
    }
}