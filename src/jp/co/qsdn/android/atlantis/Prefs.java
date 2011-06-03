package jp.co.qsdn.android.atlantis;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;

import android.util.Log;

/**
 * Preference管理クラス
 */
public class Prefs {
  private static final String TAG = Prefs.class.getName();
  private static Prefs mPrefs = null;
  private static final String PACKAGE_NAME = "jp.co.qsdn.android.atlantis";
  private Context mContext = null;

  private static final String KEY_IWASHI_COUNT = "iwashi_count";
  private static final int DEFAULT_IWASHI_COUNT = 20;

  private static final String KEY_IWASHI_SPEED = "iwashi_speed";
  private static final int DEFAULT_IWASHI_SPEED = 50;

  public static Prefs getInstance(Context context) {
    if (mPrefs == null) {
      mPrefs = new Prefs(context);
    }
    return mPrefs;
  }

  private Prefs(Context context) {
    try {
      mContext = context.createPackageContext(PACKAGE_NAME, Context.CONTEXT_RESTRICTED);  
    }
    catch (NameNotFoundException ex) {
      Log.e(TAG, ex.getLocalizedMessage());
    }
  }

  public void setIwashiCount(int count) {
    SharedPreferences sharedPreferences = mContext.getSharedPreferences(PACKAGE_NAME,Context.MODE_PRIVATE);  
    sharedPreferences
      .edit()
      .putString(KEY_IWASHI_COUNT, "" + count)
      .commit();  
  }
  public int getIwashiCount() {
    SharedPreferences sharedPreferences = mContext.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE);  
    String ret = sharedPreferences.getString(KEY_IWASHI_COUNT, "" + DEFAULT_IWASHI_COUNT);
    int iwashiCount = DEFAULT_IWASHI_COUNT;
    if (ret != null) {
      try {
        iwashiCount = Integer.parseInt(ret);
      }
      catch (NumberFormatException ex) {
        Log.e(TAG, ex.toString());
      }
    }
    return iwashiCount;
  }

  public void setIwashiSpeed(int speed) {
    SharedPreferences sharedPreferences = mContext.getSharedPreferences(PACKAGE_NAME,Context.MODE_PRIVATE);  
    sharedPreferences
      .edit()
      .putString(KEY_IWASHI_SPEED, "" + speed)
      .commit();  
  }
  public int getIwashiSpeed() {
    SharedPreferences sharedPreferences = mContext.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE);  
    String ret = sharedPreferences.getString(KEY_IWASHI_SPEED, "" + DEFAULT_IWASHI_SPEED);
    int iwashiSpeed = DEFAULT_IWASHI_SPEED;
    if (ret != null) {
      try {
        iwashiSpeed = Integer.parseInt(ret);
      }
      catch (NumberFormatException ex) {
        Log.e(TAG, ex.toString());
      }
    }
    return iwashiSpeed;
  }
}
