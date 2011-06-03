package jp.co.qsdn.android.iwashi3d.setting;

import android.content.Context;

import android.os.Bundle;

import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import android.util.Log;

import jp.co.qsdn.android.iwashi3d.R;

public class SettingActivity extends PreferenceActivity {
  private static final String TAG = SettingActivity.class.getName();

  private static final String KEY_IWASHI_COUNT = "iwashi_count";
  private static final String DEFAULT_IWASHI_COUNT = "1";

  private static final String KEY_IWASHI_SPEED = "iwashi_speed";
  private static final String DEFAULT_IWASHI_SPEED = "1";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.setting);
  }

  public static int getIwashiCount(Context context) {
    return Prefs.getInstance(context).getIwashiCount();
  }

  public static float getIwashiSpeed(Context context) {
    return Prefs.getInstance(context).getIwashiSpeed();
  }

  public static void setIwashiCount(Context context, int iwashiCount) {
    Prefs.getInstance(context).setIwashiCount(iwashiCount);
  }

  public static void setIwashiSpeed(Context context, int iwashiSpeed) {
    Prefs.getInstance(context).setIwashiSpeed(iwashiSpeed);
  }
}
