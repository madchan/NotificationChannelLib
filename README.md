图挂了可以看这里：https://www.jianshu.com/p/38d6e5c5d6f4

# 前言

你有强迫症吗？

作为用户的你，有没有试过这样的经历，常常会被一款APP的频繁推送烦扰，但又因为怕错过其中的重要信息，而不敢一刀切地将该APP的通知功能禁用掉？

而作为开发者的你，又有没有遇到这样的需求，要求应用内的有些通知能让用户立即看到(如@提醒消息)，而有些通知却只要求佛性地在抽屉通知栏躺着(如下载进度通知)？

如果你有以上的痛苦，那么这篇文章就是你的解苦良药。

假如给你展示以下两张图，哪一张会让你看起来感觉更舒爽？

![536798612903647605.jpg](https://upload-images.jianshu.io/upload_images/5530180-ec0aee987d6baf53.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


以上「类别」下的选项即是Android 8引入的**通知渠道（Notification Channel）**。

做过Android 8系统适配工作的人可能知道，以Android 8（API 级别 26）及更高版本为平台的应用，如果没有为所有通知分配渠道，则会显示不了通知。然而，大部分开发者都是知其然而不知其所以然，并不清楚Android 8引入这个机制的初衷是什么，于是就出现了图二这种莫名其妙的表现。也难怪，「通知渠道」这个名称确实不太好理解，用「通知场景」来描述，可能就清晰准确得多。

# 场景剖析

什么是通知场景呢？以一款即时通讯APP为例，可能包含系统通知、聊天消息、@提醒消息、音视频通话等多种场景类型下的消息，不同类型的消息要求对用户的提醒程度不一样，具体就体现在对用户视觉、听觉上的干扰程度上。

举个栗子，对于系统通知类的推送（比如谁加了我好友），可能并不需要用户立即处理，所以只需要在状态栏显示一个小图标，并在抽屉式通知栏显示一条通知，让用户知道有这件事即可。

而对于聊天消息，由于可能包含用户关心的内容，除了以上两个手段，常常还需要辅以提示音和震动以加强提醒。

至于@提醒消息和音视频消息，一般都是有针对性地推送，是需要用户立即处理的，要求能以浮动通知的形式显示，并且此时如果处于息屏状态，还需要能够唤醒屏幕，并在锁屏页面显示通知，音视频消息还可能需要自定义提示音以及持续震动，才能达到强提醒的目的。

此处总结为一张表格：
![微信图片_20201119005053.png](https://upload-images.jianshu.io/upload_images/5530180-40d14e74796281b3.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

那么，具体如何用通知渠道来实现呢？接下来，就让我们在本文的引领下，一扫之前应用内通知混乱无章的局面，重新整理出应用通知渠道的规范吧。

# 知识储备

### NotificationChannel

代表一个「通知渠道」对象，该类的构造函数中包含了3个最重要的属性，即：唯一渠道 ID、用户可见名称和重要性级别。

*   **唯一渠道 ID**

通知渠道的唯一标识，我们可以通过该标识查询特定的渠道设置，打开通知渠道的系统设置或删除特定的通知渠道。

*   **用户可见名称**

即打开通知管理界面后最能直观看到的通知渠道名称。

*   **重要性级别**

渠道重要性会影响在渠道中发布的所有通知的干扰级别，即上文所提及的提示音、状态栏显示、抽屉式通知栏显示以及浮动通知等。

如需支持 Android 7.1（API 级别 25）或更低版本的平台，还需调用NotificationCompat 类中的 setPriority()方法，针对每条通知设置对应的优先级常量。

渠道重要性级别及其对应的干扰级别如下：

![微信图片_20201119005312.png](https://upload-images.jianshu.io/upload_images/5530180-d63b0a19963159b6.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


需要注意的是，当我们创建好通知渠道并提交后，便无法再更改通知行为，此时用户拥有完全控制权，用户可以随时可以在通知设置页更改他们对应用渠道的偏好设置。

我们能做的，仅仅包括以下几个方面：

*   更改现有渠道的名称和说明
*   读取现有通知渠道的不良设置，从而建议用户更改该设置
*   删除现有通知渠道

有了这些知识储备后，我们就可以着手开始代码实践了。

# 代码实践

首先，我们定义一个数据类Channel，用以描述某个通知渠道的具体设置：
```
/**
 * 通知渠道
 */
data class Channel(
    val channelId: String,      // 唯一渠道ID
    val name: CharSequence,     // 用户可见名称
    val importance: Int,        // 重要性级别
    val description: String? = null,      // 描述
    @NotificationCompat.NotificationVisibility
    val lockScreenVisibility: Int = NotificationCompat.VISIBILITY_SECRET,        // 锁定屏幕公开范围
    val vibrate: LongArray? = null,      // 震动模式
    val sound: Uri? = null               // 声音
)
```

>为什么不直接用NotificationChannel？
因为NotificationChannel是API 26才引入的对象，参考NotificationCompat类的设计，为了对客户端屏蔽版本兼容的细节，所以采用了自定义的数据类 。

接着，我们定义一个通知工具类，用以封装创建通知、创建通知渠道、发布通知、取消通知等公用方法以隔离具体业务，并处理好版本兼容工作。其中，createNotificationBuilder方法会返回创建通知的Builder对象，以允许业务方根据需要进一步扩展通知表现形式。

```
/**
 * 通知兼容工具类
 *
 * 本类中的代码使用Android支持库中的NotificationCompatAPI。
 * 这些API允许您添加仅在较新版本Android上可用的功能，同时仍向后兼容Android4.0（API级别14）。
 * 但是，诸如内嵌回复操作等部分新功能在较旧版本上会导致发生空操作。
 */
class NotificationCompatUtil {

    companion object {

        /**
         * 创建通知
         * @param context           上下文
         * @param channel           通知渠道
         * @param title             标题
         * @param text              正文文本
         * @param intent            对点按操作做出响应意图
         * @return
         */
        fun createNotificationBuilder(
            context: Context,
            channel: Channel,
            title: CharSequence? = null,
            text: CharSequence? = null,
            intent: Intent? = null
        ): NotificationCompat.Builder {
            // 必须先创建通知渠道，然后才能在Android 8.0及更高版本上发布任何通知
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createChannel(context, channel)
            }

            val builder =
                NotificationCompat.Builder(context, channel.channelId)
                    .setPriority(getLowVersionPriority(channel)) // 通知优先级，优先级确定通知在Android7.1和更低版本上的干扰程度。
                    .setVisibility(channel.lockScreenVisibility) // 锁定屏幕公开范围
                    .setVibrate(channel.vibrate) // 震动模式
                    .setSound(channel.sound ?: Settings.System.DEFAULT_NOTIFICATION_URI)    // 声音
                    .setOnlyAlertOnce(true) // 设置通知只会在通知首次出现时打断用户（通过声音、振动或视觉提示），而之后更新则不会再打断用户。

            // 标题，此为可选内容
            if (!TextUtils.isEmpty(title)) builder.setContentTitle(title)

            // 正文文本，此为可选内容
            if (!TextUtils.isEmpty(text)) builder.setContentText(text)

            // 设置通知的点按操作，每个通知都应该对点按操作做出响应，通常是在应用中打开对应于该通知的Activity。
            if (intent != null) {
                val pendingIntent =
                    PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                builder.setContentIntent(pendingIntent)
                    .setAutoCancel(true) // 在用户点按通知后自动移除通知
                if(NotificationManager.IMPORTANCE_HIGH == channel.importance) builder.setFullScreenIntent(pendingIntent, false)
            }

            return builder
        }

        /**
         * 获取低版本的优先级
         * 要支持搭载 Android 7.1（API 级别 25）或更低版本的设备，
         * 您还必须使用 NotificationCompat 类中的优先级常量针对每条通知调用 setPriority()。
         * @param channel
         * @return
         */
        private fun getLowVersionPriority(channel: Channel): Int {
            return when (channel.importance) {
                NotificationManager.IMPORTANCE_HIGH -> NotificationCompat.PRIORITY_HIGH
                NotificationManager.IMPORTANCE_LOW -> NotificationCompat.PRIORITY_LOW
                NotificationManager.IMPORTANCE_MIN -> NotificationCompat.PRIORITY_MIN
                else -> NotificationCompat.PRIORITY_DEFAULT
            }
        }

        /**
         * 创建通知渠道
         * <p>
         * 反复调用这段代码也是安全的，因为创建现有通知渠道不会执行任何操作。
         * 注意：创建通知渠道后，您便无法更改通知行为，此时用户拥有完全控制权。不过，您仍然可以更改渠道的名称和说明。
         * @param context 上下文
         * @param channel 通知渠道
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        private fun createChannel(
            context: Context,
            channel: Channel
        ) {
            val notificationChannel =
                NotificationChannel(channel.channelId, channel.name, channel.importance)
            notificationChannel.description = channel.description   // 描述
            notificationChannel.vibrationPattern = channel.vibrate  // 震动模式
            notificationChannel.setSound(channel.sound ?: Settings.System.DEFAULT_NOTIFICATION_URI, notificationChannel.audioAttributes)    // 声音
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }

        /**
         * 显示通知
         *
         *
         * 请记得保存您传递到 NotificationManagerCompat.notify() 的通知 ID，因为如果之后您想要更新或移除通知，将需要使用这个 ID。
         * @param context      上下文
         * @param id           通知的唯一ID
         * @param notification 通知
         */
        fun notify(
            context: Context,
            id: Int,
            notification: Notification?
        ) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(id, notification)
        }

        /**
         * 取消通知
         * @param context 上下文
         * @param id      通知的唯一ID
         */
        fun cancel(context: Context, id: Int) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(id)
        }

        /**
         * 取消所有通知
         * @param context 上下文
         */
        fun cancelAll(context: Context) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancelAll()
        }
    }
}
```

然后，在业务模块定义一个推送通知管理类，用于管理具体业务场景下的通知，分为两个部分，渠道对象和通知方法。

*   **渠道对象**

由于前面在定义Channel类时已经做好部分属性的默认值设置，所以这里我们只需按需配置Channel对象的属性，以这种编码形式能够很直观地将Channel对象映射到通知设置里对应的通知渠道配置。

```
// PushNotificationHelper.kt

/** 通知渠道-聊天消息(重要性级别-高：发出声音) */
private val MESSAGE = NotificationCompatUtil.Channel(
    channelId = "MESSAGE",
    name = BaseApplication.getContext().getString(R.string.channel_message),
    importance = NotificationManager.IMPORTANCE_DEFAULT
)

/** 通知渠道-@提醒消息(重要性级别-紧急：发出提示音，并以浮动通知的形式显示 & 锁屏显示 & 振动0.25s )*/
private val MENTION = NotificationCompatUtil.Channel(
    channelId = "MENTION",
    name = BaseApplication.getContext().getString(R.string.channel_mention),
    importance = NotificationManager.IMPORTANCE_HIGH,
    lockScreenVisibility = NotificationCompat.VISIBILITY_PUBLIC,
    vibrate = longArrayOf(0, 250)
)

/** 通知渠道-系统通知(重要性级别-中：无提示音) */
private val NOTICE = NotificationCompatUtil.Channel(
    channelId = "NOTICE",
    name = BaseApplication.getContext().getString(R.string.channel_notice),
    importance = NotificationManager.IMPORTANCE_LOW
)

/** 通知渠道-音视频通话(重要性级别-紧急：发出提示音，并以浮动通知的形式显示 & 锁屏显示 & 振动4s停2s再振动4s ) */
private val CALL = NotificationCompatUtil.Channel(
    channelId = "CALL",
    name = BaseApplication.getContext().getString(R.string.channel_call),
    importance = NotificationManager.IMPORTANCE_HIGH,
    lockScreenVisibility = NotificationCompat.VISIBILITY_PUBLIC,
    vibrate = longArrayOf(0, 4000, 2000, 4000),
    sound = Uri.parse("android.resource://" + BaseApplication.getContext().packageName + "/" + R.raw.iphone)
)
```

*   **通知方法**

即具体业务场景下的推送逻辑实现，每种方法都必须指定一个通知渠道，调用NotificationCompatUtil类的createNotificationBuilder方法获取创建通知的Builder之后，可以根据需要丰富通知的表现形式：

```
// PushNotificationHelper.kt

/**
 * 显示聊天消息
 * @param context 上下文
 * @param id      通知的唯一ID
 * @param title   标题
 * @param text    正文文本
 */
fun notifyMessage(
    context: Context,
    id: Int,
    title: String?,
    text: String?
) {
    val intent = Intent(context, MainActivity::class.java)

    val builder = NotificationCompatUtil.createNotificationBuilder(
        context,
        MESSAGE,
        title,
        text,
        intent
    )

    // 默认情况下，通知的文字内容会被截断以放在一行。如果您想要更长的通知，可以使用 setStyle() 添加样式模板来启用可展开的通知。
    builder.setStyle(
        NotificationCompat.BigTextStyle()
            .bigText(text)
    )

    NotificationCompatUtil.notify(context, id, buildDefaultConfig(builder));
}

/**
 * 显示@提醒消息
 * @param context 上下文
 * @param id      通知的唯一ID
 * @param title   标题
 * @param text    正文文本
 */
fun notifyMention(
    context: Context,
    id: Int,
    title: String?,
    text: String?
) {
    val intent = Intent(context, MainActivity::class.java)

    val builder = NotificationCompatUtil.createNotificationBuilder(
        context,
        MENTION,
        title,
        text,
        intent
    )

    // 默认情况下，通知的文字内容会被截断以放在一行。如果您想要更长的通知，可以使用 setStyle() 添加样式模板来启用可展开的通知。
    builder.setStyle(
        NotificationCompat.BigTextStyle()
            .bigText(text)
    )

    NotificationCompatUtil.notify(context, id, buildDefaultConfig(builder));
}

/**
 * 显示系统通知
 * @param context 上下文
 * @param id      通知的唯一ID
 * @param title   标题
 * @param text    正文文本
 */
fun notifyNotice(
    context: Context,
    id: Int,
    title: String?,
    text: String?
) {
    val intent = Intent(context, MainActivity::class.java)

    val builder = NotificationCompatUtil.createNotificationBuilder(
        context,
        NOTICE,
        title,
        text,
        intent
    )

    NotificationCompatUtil.notify(context, id, buildDefaultConfig(builder));
}

/**
 * 显示音视频通话
 * @param context 上下文
 * @param id      通知的唯一ID
 * @param title   标题
 * @param text    正文文本
 */
fun notifyCall(
    context: Context,
    id: Int,
    title: String?,
    text: String?
) {
    val intent = Intent(context, MainActivity::class.java)

    val builder = NotificationCompatUtil.createNotificationBuilder(
        context,
        CALL,
        title,
        text,
        intent
    )

    NotificationCompatUtil.notify(context, id, buildDefaultConfig(builder));
}

/**
 * 构建应用通知的默认配置
 * @param builder 构建器
 */
private fun buildDefaultConfig(builder: NotificationCompat.Builder): Notification {
    builder.setSmallIcon(R.drawable.ic_launcher_foreground)
    return builder.build()
}
```

至此，我们便完成了通知方法→渠道对象→通知渠道三者间的映射关系，建立了通知渠道的使用规范，在分别推送了各自渠道下的通知之后，我们可以在通知设置里看到：

![342339134046568431.jpg](https://upload-images.jianshu.io/upload_images/5530180-8e54b727f13c6efd.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


具体的Demo已经上传到GitHub([https://github.com/madchan/NotificationChannelLib](https://github.com/madchan/NotificationChannelLib))，如果对你有帮助的话给个Star吧~

# 总结

在我看来，规范使用「通知渠道」的好处可以概括为：

**更细粒度地划分应用内通知的场景，可以单独控制每种场景对用户的干扰程度，降低对用户的打扰，从而提高用户体验。**

幸运的是，很多APP已经意识到了这个好处并付诸实践，但仍有更多APP的通知管理仍处于很混乱的状态，因此希望这部文章能够起到一定指导作用 ，帮助他们建立规范。
