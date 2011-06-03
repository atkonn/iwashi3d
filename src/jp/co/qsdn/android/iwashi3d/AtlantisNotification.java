package jp.co.qsdn.android.iwashi3d;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.content.Context;
import android.content.Intent;

import jp.co.qsdn.android.iwashi3d.setting.SettingActivity;

/**
 * ステータスバーの実行中領域にアイコンを表示
 */
public class AtlantisNotification {

  public static final int NOTIFICATION_ID = 1;

  private static Notification createNotification(Context context) {
    Notification notification = new Notification(
      R.drawable.smallicon,    // ステータスに置くスモールアイコン
      "",                                        // アイコン横のツールテキスト無し
      System.currentTimeMillis()                 // システム時刻
    );
    PendingIntent pi = PendingIntent.getActivity(
        context,
        0,                                             // requestCode
        new Intent(context, SettingActivity.class),
        0                                              // Default flags
    );
    notification.setLatestEventInfo(
        context,
        context.getString(R.string.app_name),
        context.getString(R.string.summary_setting),
        pi
    );
    /**
     * Notification.FLAG_NO_CLEAR ==> クリアボタンを表示しない
     * Notification.FLAG_ONGOING_EVENT ==> 実行中領域に表示
     */
    notification.flags = notification.flags | Notification.FLAG_NO_CLEAR  | Notification.FLAG_ONGOING_EVENT; 
    notification.number = 0;
    return notification;
  }

  //1. ステータスバーに通知
  // 1-1. ブロードキャストレシーバーから起動時に呼び出す
  // 1-2. アプリの終了時に呼び出す
  public static void putNotice(Context context) {
    NotificationManager nm = (NotificationManager)
       context.getSystemService(Context.NOTIFICATION_SERVICE);
    Notification notification = createNotification(context);
    nm.notify(AtlantisNotification.NOTIFICATION_ID, notification);
  }

  //2. ステータスバーから削除
  // 2-1. アプリの起動時に呼び出す
  public static void removeNotice(Context context) {
    NotificationManager nm = (NotificationManager)
      context.getSystemService(Context.NOTIFICATION_SERVICE);
    nm.cancel(NOTIFICATION_ID);
  }
}
