package jp.co.qsdn.android.iwashi3d;

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
      AtlantisNotification.putNotice(context);
    }
    if (_debug) Log.d(TAG,"end onReceive");
  }
}
