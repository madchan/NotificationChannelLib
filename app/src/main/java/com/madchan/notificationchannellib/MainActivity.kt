package com.madchan.notificationchannellib

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.notify_message).setOnClickListener {
            PushNotificationHelper.notifyMessage(
                this,
                1,
                "MadChan",
                "这是一条聊天消息"
            )
        }
        findViewById<Button>(R.id.notify_mention).setOnClickListener {
            it.postDelayed({
                PushNotificationHelper.notifyMention(
                    this,
                    2,
                    "MadChan",
                    "这是一条@提醒消息"
                )
            }, 2000)
        }
        findViewById<Button>(R.id.notify_notice).setOnClickListener {
            PushNotificationHelper.notifyNotice(
                this,
                3,
                "系统通知",
                "MadChan想加你为好友"
            )
        }
        findViewById<Button>(R.id.notify_call).setOnClickListener {
            it.postDelayed({
                PushNotificationHelper.notifyCall(this, 4, "MadChan", "[视频通话]")
            }, 2000)
        }
    }
}