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
package jp.co.qsdn.android.jinbei3d.setting;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;

import android.util.Log;

import jp.co.qsdn.android.jinbei3d.R;

/**
 * Preference管理クラス
 */
public class Prefs {
  private static final String TAG = Prefs.class.getName();
  private static Prefs mPrefs = null;
  private static final String PACKAGE_NAME = "jp.co.qsdn.android.jinbei3d";
  private Context mContext = null;

  public static final String KEY_IWASHI_COUNT = "iwashi_count";
  public static final int DEFAULT_IWASHI_COUNT = 20;

  public static final String KEY_IWASHI_SPEED = "iwashi_speed";
  public static final int DEFAULT_IWASHI_SPEED = 50;

  public static final String KEY_IWASHI_BOIDS = "iwashi_boids";
  public static final boolean DEFAULT_IWASHI_BOIDS = true;

  public static final String KEY_CAMERA_MODE = "camera_mode";
  public static final int DEFAULT_CAMERA_MODE = R.id.radio3;

  public static final String KEY_CAMERA_DISTANCE = "camera_distance";
  public static final int DEFAULT_CAMERA_DISTANCE = 10;

  public static final String KEY_JINBEI_SPEED = "jinbei_speed";
  public static final int DEFAULT_JINBEI_SPEED = 50;

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


  public void setIwashiBoids(boolean enableIwashiBoids) {
    SharedPreferences sharedPreferences = mContext.getSharedPreferences(PACKAGE_NAME,Context.MODE_PRIVATE);  
    sharedPreferences
      .edit()
      .putBoolean(KEY_IWASHI_BOIDS, enableIwashiBoids)
      .commit();  
  }
  public boolean getIwashiBoids() {
    SharedPreferences sharedPreferences = mContext.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE);  
    return  sharedPreferences.getBoolean(KEY_IWASHI_BOIDS, DEFAULT_IWASHI_BOIDS);
  }
  public void setCameraMode(int cameraMode) {
Log.d(TAG, "setCameraMode(" + cameraMode + ")");
    SharedPreferences sharedPreferences = mContext.getSharedPreferences(PACKAGE_NAME,Context.MODE_PRIVATE);  
    sharedPreferences
      .edit()
      .putInt(KEY_CAMERA_MODE, cameraMode)
      .commit();  
  }
  public int getCameraMode() {
    SharedPreferences sharedPreferences = mContext.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE);  
    int ret = sharedPreferences.getInt(KEY_CAMERA_MODE, DEFAULT_CAMERA_MODE);
Log.d(TAG, ret + " = getCameraMode()");
    return ret;
  }

  public void setCameraDistance(int distance) {
    SharedPreferences sharedPreferences = mContext.getSharedPreferences(PACKAGE_NAME,Context.MODE_PRIVATE);  
    sharedPreferences
      .edit()
      .putString(KEY_CAMERA_DISTANCE, "" + distance)
      .commit();  
  }
  public int getCameraDistance() {
    SharedPreferences sharedPreferences = mContext.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE);  
    String ret = sharedPreferences.getString(KEY_CAMERA_DISTANCE, "" + DEFAULT_CAMERA_DISTANCE);
    int distance = DEFAULT_CAMERA_DISTANCE;
    if (ret != null) {
      try {
        distance = Integer.parseInt(ret);
      }
      catch (NumberFormatException ex) {
        Log.e(TAG, ex.toString());
      }
    }
    return distance;
  }



  public void setJinbeiSpeed(int speed) {
    SharedPreferences sharedPreferences = mContext.getSharedPreferences(PACKAGE_NAME,Context.MODE_PRIVATE);  
    sharedPreferences
      .edit()
      .putString(KEY_JINBEI_SPEED, "" + speed)
      .commit();  
  }
  public int getJinbeiSpeed() {
    SharedPreferences sharedPreferences = mContext.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE);  
    String ret = sharedPreferences.getString(KEY_JINBEI_SPEED, "" + DEFAULT_JINBEI_SPEED);
    int jinbeiSpeed = DEFAULT_JINBEI_SPEED;
    if (ret != null) {
      try {
        jinbeiSpeed = Integer.parseInt(ret);
      }
      catch (NumberFormatException ex) {
        Log.e(TAG, ex.toString());
      }
    }
    return jinbeiSpeed;
  }
}
