package jp.co.qsdn.android.atlantis;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AtlantisBroadcastReceiver extends BroadcastReceiver {
  private static final String TAG = AtlantisBroadcastReceiver.class.getName();
  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d(TAG,"start onReceive");
    String action = intent.getAction();

    if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
      Log.d(TAG,"ACTION_BOOT_COMPLETED受信");
      // プリファレンスの設定を確認する
//      boolean checked = ... 中略 ...
//      if (checked) {
        // 端末ブート完了時にアプリアイコンを置く
        AtlantisNotification.putNotice(context);
//      }
    }
    Log.d(TAG,"end onReceive");
  }
}
