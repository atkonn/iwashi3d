package jp.co.qsdn.android.atlantis;

import android.content.Context;

import android.os.Bundle;

import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import android.util.Log;

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
    String ret = PreferenceManager
                  .getDefaultSharedPreferences(context)    
                  .getString(KEY_IWASHI_COUNT, DEFAULT_IWASHI_COUNT);
    int iwashi_count = 1;
    if (ret != null) {
      try {
        iwashi_count = Integer.parseInt(ret);
      }
      catch (NumberFormatException ex) {
        Log.d(TAG, ex.toString());
      }
    }
    return iwashi_count;
  }

  public static float getIwashiSpeed(Context context) {
    String ret = PreferenceManager
                  .getDefaultSharedPreferences(context)    
                  .getString(KEY_IWASHI_SPEED, DEFAULT_IWASHI_SPEED);
    float iwashi_speed = 0.05f;
    if (ret != null) {
      try {
        iwashi_speed = Float.parseFloat(ret);
      }
      catch (NumberFormatException ex) {
        Log.d(TAG, ex.toString());
      }
    }
    return iwashi_speed;
  }
}
