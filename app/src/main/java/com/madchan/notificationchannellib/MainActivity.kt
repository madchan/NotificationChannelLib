package com.madchan.notificationchannellib

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.notify_message).setOnClickListener {
            Toast.makeText(this, "已推送，可下拉抽屉通知栏查看", Toast.LENGTH_LONG).show()
            PushNotificationHelper.notifyMessage(
                this,
                1,
                "MadChan",
                "这是一条聊天消息"
            )
        }
        findViewById<Button>(R.id.notify_mention).setOnClickListener {
            Toast.makeText(this, "将在3s后推送，可锁定屏幕查看", Toast.LENGTH_LONG).show()
            it.postDelayed({
                PushNotificationHelper.notifyMention(
                    this,
                    2,
                    "MadChan",
                    "这是一条@提醒消息"
                )
            }, 3000)
        }
        findViewById<Button>(R.id.notify_notice).setOnClickListener {
            Toast.makeText(this, "已推送，可下拉抽屉通知栏查看", Toast.LENGTH_LONG).show()
            PushNotificationHelper.notifyNotice(
                this,
                3,
                "系统通知",
                "MadChan想加你为好友"
            )
        }
        findViewById<Button>(R.id.notify_call).setOnClickListener {
            Toast.makeText(this, "将在3s后推送，可锁定屏幕查看", Toast.LENGTH_LONG).show()
            it.postDelayed({
                PushNotificationHelper.notifyCall(this, 4, "MadChan", "[视频通话]")
            }, 3000)
        }
    }
}