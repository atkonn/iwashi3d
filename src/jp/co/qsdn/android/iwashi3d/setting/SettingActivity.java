package jp.co.qsdn.android.iwashi3d.setting;

import android.content.Context;
import android.content.res.Resources;

import android.os.Bundle;

import android.preference.CheckBoxPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference;
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

  private static final String KEY_IWASHI_BOIDS = "iwashi_boids";
  private static final boolean DEFAULT_IWASHI_BOIDS = true;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.setting);

    boolean b = Prefs.getInstance(this).getIwashiBoids();
    Log.d(TAG,"b:[" + b + "]");

    Resources res = getResources();

    String key = res.getString(R.string.key_preference_iwashi_boids);
    CheckBoxPreference pref = (CheckBoxPreference)findPreference(key);
    final Prefs prefs = Prefs.getInstance(this);
    pref.setOnPreferenceChangeListener(
      new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
          Boolean nv = (Boolean)newValue;
          prefs.setIwashiBoids(nv);
          ((CheckBoxPreference) preference).setChecked((Boolean)newValue);
          return false;
        }
      });
    pref.setChecked(b);
  }

  @Override
  protected void onDestroy() {
    Log.d(TAG, "start onDestroy");
    boolean b = getPreferenceManager().getSharedPreferences().getBoolean(KEY_IWASHI_BOIDS, DEFAULT_IWASHI_BOIDS);
    Prefs.getInstance(this).setIwashiBoids(b);
    super.onDestroy();
    Log.d(TAG, "end onDestroy");
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
