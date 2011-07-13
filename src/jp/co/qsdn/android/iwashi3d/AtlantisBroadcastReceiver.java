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
package jp.co.qsdn.android.iwashi3d;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.util.Log;

public class AtlantisBroadcastReceiver extends BroadcastReceiver {
  private static final String TAG = AtlantisBroadcastReceiver.class.getName();
  private static final boolean _debug = false;
  @Override
  public void onReceive(Context context, Intent intent) {
    if (_debug) Log.d(TAG,"start onReceive");
    String action = intent.getAction();

    if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
      if (_debug) Log.d(TAG,"ACTION_BOOT_COMPLETED受信");
      // 端末ブート完了時にアプリアイコンを置く
      if (isRunning(context)) {
        AtlantisNotification.putNotice(context);
      }
    }
    if (_debug) Log.d(TAG,"end onReceive");
  }

  protected boolean isRunning(Context context) {
    WallpaperManager wallpaperManager = (WallpaperManager)context.getSystemService(Context.WALLPAPER_SERVICE);

    WallpaperInfo wallpaperInfo = wallpaperManager.getWallpaperInfo();
    if (wallpaperInfo != null) {
      if (_debug) Log.d(TAG, "serviceName:[" + wallpaperInfo.getServiceName() + "]");
      if ("jp.co.qsdn.android.iwashi3d.AtlantisService".equals(wallpaperInfo.getServiceName())) {
        return true;
      }
    }
    return false;
  }
}
