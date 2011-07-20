/*
 * Copyright (C) 2011 QSDN,Inc.
 * Copyright (C) 2011 Atsushi Konno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.co.qsdn.android.jinbei3d;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.content.Context;
import android.content.Intent;

import jp.co.qsdn.android.jinbei3d.setting.SettingActivity;

/**
 * ステータスバーの実行中領域にアイコンを表示
 */
public class AtlantisNotification {

  public static final int NOTIFICATION_ID = 1;

  private static Notification createNotification(Context context) {
    Notification notification = new Notification(
      R.drawable.smallicon,
      "",
      System.currentTimeMillis()
    );
    PendingIntent pi = PendingIntent.getActivity(
        context,
        0,
        new Intent(context, SettingActivity.class),
        0 
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

  public static void putNotice(Context context) {
    NotificationManager nm = (NotificationManager)
       context.getSystemService(Context.NOTIFICATION_SERVICE);
    Notification notification = createNotification(context);
    nm.notify(AtlantisNotification.NOTIFICATION_ID, notification);
  }

  public static void removeNotice(Context context) {
    NotificationManager nm = (NotificationManager)
      context.getSystemService(Context.NOTIFICATION_SERVICE);
    nm.cancel(NOTIFICATION_ID);
  }
}
