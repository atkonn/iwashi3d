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

import android.content.Context;

import android.graphics.Canvas;
import android.graphics.Paint;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.Matrix;

import android.os.Bundle;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import jp.co.qsdn.android.jinbei3d.model.Background;
import jp.co.qsdn.android.jinbei3d.model.Ground;
import jp.co.qsdn.android.jinbei3d.model.Iwashi;
import jp.co.qsdn.android.jinbei3d.model.IwashiData;
import jp.co.qsdn.android.jinbei3d.model.Jinbei;
import jp.co.qsdn.android.jinbei3d.model.Model;
import jp.co.qsdn.android.jinbei3d.model.Wave;
import jp.co.qsdn.android.jinbei3d.setting.Prefs;
import jp.co.qsdn.android.jinbei3d.util.CoordUtil;

public class GLRenderer {
  private static final boolean _debug = true;
  private static final String TAG = GLRenderer.class.getName();
  public static final int MAX_IWASHI_COUNT = 100;
  private final Background background = new Background();
  private final Ground ground = new Ground();
  private final Wave wave = new Wave();
  private Model[] iwashi = null;
  private Jinbei jinbei = null;
  private int iwashi_count = 1;
  private boolean enableIwashiBoids = true;
  private float iwashi_speed = 0.03f;
  private float jinbei_speed = 0.03f;
  /* カメラの位置 */
  private float[] camera = {0f,0f,0f};
  private float[] org_camera = {0f,0f,0f};
  private int cameraMode = R.id.radio3; /* radio3: 通常モード radio2: ジンベイザメモード: radio1: 鰯モード */
  private float cameraDistance = 10f; /* 群れまでの距離 */
  private float zFar = 80.0f;
  private float zNear = 1.0f;
  private float perspectiveAngle = 45.0f;
  public long tick = 0L;
  public long prevTick = 0L;

  private BaitManager baitManager = new BaitManager();
  private float baseAngle = 0f;
  private float[] mScratch32 = new float[32];
  private float[] mScratch4f = new float[4];
  public static GLRenderer glRenderer = null;
  /* 群れの中心 */
  float[] schoolCenter = {0f,0f,0f};
  private CoordUtil coordUtil = new CoordUtil();

  private GLRenderer(Context context) {
    iwashi_count = Prefs.getInstance(context).getIwashiCount();
    iwashi_speed = ((float)Prefs.getInstance(context).getIwashiSpeed() / 50f) * Iwashi.DEFAULT_SPEED;
    jinbei_speed = ((float)Prefs.getInstance(context).getJinbeiSpeed() / 50f) * Jinbei.DEFAULT_SPEED;
    enableIwashiBoids = Prefs.getInstance(context).getIwashiBoids();
    cameraDistance = (float)Prefs.getInstance(context).getCameraDistance();
    cameraMode = Prefs.getInstance(context).getCameraMode();

    IwashiData.init();

    jinbei = new Jinbei(0);
    jinbei.setX(0.0f);
    jinbei.setY(0.0f);
    jinbei.setZ(0.0f);
    jinbei.setBaitManager(baitManager);
    jinbei.setSpeed(iwashi_speed);
    

    iwashi = new Model[MAX_IWASHI_COUNT + 1];
    for (int ii=0; ii<MAX_IWASHI_COUNT; ii++) {
      iwashi[ii] = new Iwashi(ii);
    }
    iwashi[MAX_IWASHI_COUNT] = jinbei;
    for (int ii=0; ii<MAX_IWASHI_COUNT; ii++) {
      ((Iwashi)iwashi[ii]).setEnableBoids(enableIwashiBoids);
      ((Iwashi)iwashi[ii]).setSpecies(iwashi);
      ((Iwashi)iwashi[ii]).setSpeed(iwashi_speed);
      ((Iwashi)iwashi[ii]).setIwashiCount(iwashi_count);
      ((Iwashi)iwashi[ii]).setBaitManager(baitManager);
    }
/*DEBUG*/
if (false) {
    for (int ii=0; ii<MAX_IWASHI_COUNT; ii++) {
      ((Iwashi)iwashi[ii]).setX(0.0f);
      ((Iwashi)iwashi[ii]).setY(0.0f);
      ((Iwashi)iwashi[ii]).setZ(0.0f);
    }
}
/*DEBUG*/
  }
  
  public static GLRenderer getInstance(Context context) {
    if (_debug) Log.d(TAG, "start getInstance()");
    if (glRenderer == null) {
      if (_debug) Log.d(TAG, "new GLRenderer");
      glRenderer = new GLRenderer(context);
    }
    if (_debug) Log.d(TAG, "end getInstance()");
    return glRenderer;
  }

  public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig, Context context) {
    if (_debug) Log.d(TAG, "start onSurfaceCreated()");
    gl10.glEnable(GL10.GL_DEPTH_TEST);
    gl10.glDepthFunc(GL10.GL_LEQUAL);

    gl10.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl10.glEnableClientState(GL10.GL_NORMAL_ARRAY);
    gl10.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    /*=======================================================================*/
    /* カリングの有効化                                                      */
    /*=======================================================================*/
    gl10.glEnable(GL10.GL_CULL_FACE);
    /*=======================================================================*/
    /* ティザーを無効に                                                      */
    /*=======================================================================*/
    gl10.glDisable(GL10.GL_DITHER);  

    /*=======================================================================*/
    /* OpenGLにスムージングを設定                                            */
    /*=======================================================================*/
    gl10.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);  

    /*=======================================================================*/
    /* テクスチャ                                                            */
    /*=======================================================================*/
    gl10.glEnable(GL10.GL_TEXTURE_2D);
    Background.loadTexture(gl10, context, R.drawable.background);
    Ground.loadTexture(gl10, context, R.drawable.sand);
    Wave.loadTexture(gl10, context, R.drawable.wave);
    Iwashi.loadTexture(gl10, context, R.drawable.iwashi);
    Jinbei.loadTexture(gl10, context, R.drawable.jinbei);


    org_camera[0] = camera[0] = 0f;
    org_camera[1] = camera[1] = 0f;
    org_camera[2] = camera[2] = Aquarium.max_z + zNear;

    /*=======================================================================*/
    /* フォグのセットアップ                                                  */
    /*=======================================================================*/
    setupFog(gl10);

    gl10.glEnable(GL10.GL_NORMALIZE) ;
    gl10.glEnable(GL10.GL_RESCALE_NORMAL);
    gl10.glShadeModel(GL10.GL_SMOOTH);
    //背景のクリア
    gl10.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    gl10.glClearDepthf(1.0f);

    // ステンシルのクリア
//    gl10.glClearStencil(0);

    if (_debug) Log.d(TAG, "end onSurfaceCreated()");
  }

  public void onSurfaceDestroyed(GL10 gl10) {
    Background.deleteTexture(gl10);
    Ground.deleteTexture(gl10);
    Wave.deleteTexture(gl10);
    Iwashi.deleteTexture(gl10);
  }

  /**
   * 光のセットアップ
   */
  public void setupLighting1(GL10 gl10) {
    gl10.glEnable(GL10.GL_LIGHTING);
    gl10.glEnable(GL10.GL_LIGHT0);
    gl10.glEnable(GL10.GL_LIGHT1);
  }
  public void setupLighting2(GL10 gl10) {
    {
      /*=======================================================================*/
      /* 環境光の色設定                                                        */
      /*=======================================================================*/
//      float[] amb = { 1.0f, 1.0f, 1.0f, 1.0f };
      synchronized(mScratch4f) {
        mScratch4f[0] = 1.0f;
        mScratch4f[1] = 1.0f;
        mScratch4f[2] = 1.0f;
        mScratch4f[3] = 1.0f;
        gl10.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, mScratch4f, 0);
      }
      /*=======================================================================*/
      /* 拡散反射光の色設定                                                    */
      /*=======================================================================*/
//      float[] diff = { 1.0f, 1.0f, 1.0f, 1.0f };
      synchronized(mScratch4f) {
        mScratch4f[0] = 1.0f;
        mScratch4f[1] = 1.0f;
        mScratch4f[2] = 1.0f;
        mScratch4f[3] = 1.0f;
        gl10.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, mScratch4f, 0);
      }
      /*=======================================================================*/
      /* 鏡面反射光の色設定                                                    */
      /*=======================================================================*/
//      float[] spec = { 1.0f, 1.0f, 1.0f, 1.0f };
      synchronized(mScratch4f){
        mScratch4f[0] = 1.0f;
        mScratch4f[1] = 1.0f;
        mScratch4f[2] = 1.0f;
        mScratch4f[3] = 1.0f;
        gl10.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, mScratch4f, 0);
      }
      /*=======================================================================*/
      /* そもそもの光の位置設定                                                */
      /*=======================================================================*/
//      float[] pos1 = { 0.0f, 10.0f, 0.0f, 1.0f };
      synchronized(mScratch4f){
        mScratch4f[0] = 0.0f;
        mScratch4f[1] = 10.0f;
        mScratch4f[2] = 0.0f;
        mScratch4f[3] = 1.0f;
        gl10.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, mScratch4f, 0);
      }
      /*=======================================================================*/
      /* そもそもの光の向き設定                                                */
      /*=======================================================================*/
//      float[] dir = { 0.0f, -1.0f, 0.0f };
      synchronized(mScratch4f) {
        mScratch4f[0] = 0.0f;
        mScratch4f[1] = -1.0f;
        mScratch4f[2] = 0.0f;
        mScratch4f[3] = 0.0f;
        gl10.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPOT_DIRECTION, mScratch4f, 0);
      }
      gl10.glLightf(GL10.GL_LIGHT0, GL10.GL_SPOT_CUTOFF, 90);
      gl10.glLightf(GL10.GL_LIGHT0, GL10.GL_SPOT_EXPONENT, 0);
      /*=======================================================================*/
      /* 減衰ほとんどなしに設定                                                */
      /*=======================================================================*/
      gl10.glLightf(GL10.GL_LIGHT0, GL10.GL_CONSTANT_ATTENUATION, 0.2f);
      gl10.glLightf(GL10.GL_LIGHT0, GL10.GL_LINEAR_ATTENUATION, 0.002f);
      gl10.glLightf(GL10.GL_LIGHT0, GL10.GL_QUADRATIC_ATTENUATION, 0.0f);
    }
    {
      /*=======================================================================*/
      /* 環境光の色設定                                                        */
      /*=======================================================================*/
      //float[] amb = { 0.019f, 0.9606f, 1.0f, 1.0f };
      synchronized(mScratch4f) {
        mScratch4f[0] = 0.019f * 0.6f;
        mScratch4f[1] = 0.9606f * 0.6f;
        mScratch4f[2] = 1.0f * 0.6f;
        mScratch4f[3] = 1.0f;
        gl10.glLightfv(GL10.GL_LIGHT1, GL10.GL_AMBIENT, mScratch4f, 0);
      /*=======================================================================*/
      /* 拡散反射光の色設定                                                    */
      /*=======================================================================*/
        //float[] diff = { 0.019f, 0.9606f, 1.0f, 1.0f };
        gl10.glLightfv(GL10.GL_LIGHT1, GL10.GL_DIFFUSE, mScratch4f, 0);
      /*=======================================================================*/
      /* 鏡面反射光の色設定                                                    */
      /*=======================================================================*/
        //float[] spec = { 0.019f, 0.9606f, 1.0f, 1.0f };
        mScratch4f[0] *= 0.5f;
        mScratch4f[1] *= 0.5f;
        mScratch4f[2] *= 0.5f;
        gl10.glLightfv(GL10.GL_LIGHT1, GL10.GL_SPECULAR, mScratch4f, 0);
      }
      /*=======================================================================*/
      /* そもそもの光の位置設定                                                */
      /*=======================================================================*/
      //float[] pos = { 0.0f, -10.0f, 0.0f, 1.0f };
      synchronized (mScratch4f) {
        mScratch4f[0] = 0.0f;
        mScratch4f[1] = -10.0f;
        mScratch4f[2] = 0.0f;
        mScratch4f[3] = 1.0f;
        gl10.glLightfv(GL10.GL_LIGHT1, GL10.GL_POSITION, mScratch4f, 0);
      }
      /*=======================================================================*/
      /* そもそもの光の向き設定                                                */
      /*=======================================================================*/
      //float[] dir = { 0.0f, 1.0f, 0.0f };
      synchronized (mScratch4f) {
        mScratch4f[0] = 0.0f;
        mScratch4f[1] = 1.0f;
        mScratch4f[2] = 0.0f;
        mScratch4f[3] = 0.0f;
        gl10.glLightfv(GL10.GL_LIGHT1, GL10.GL_SPOT_DIRECTION, mScratch4f, 0);
      }
      gl10.glLightf(GL10.GL_LIGHT1, GL10.GL_SPOT_CUTOFF, 90);
      gl10.glLightf(GL10.GL_LIGHT1, GL10.GL_SPOT_EXPONENT, 0);
      /*=======================================================================*/
      /* 減衰ほとんどなしに設定                                                */
      /*=======================================================================*/
      gl10.glLightf(GL10.GL_LIGHT1, GL10.GL_CONSTANT_ATTENUATION, 0.2f);
      gl10.glLightf(GL10.GL_LIGHT1, GL10.GL_LINEAR_ATTENUATION, 0.002f);
      gl10.glLightf(GL10.GL_LIGHT1, GL10.GL_QUADRATIC_ATTENUATION, 0.0f);
    }

  }

  /**
   * フォグのセットアップ
   */
  public void setupFog(GL10 gl10) {
    gl10.glEnable(GL10.GL_FOG);
    gl10.glFogf(GL10.GL_FOG_MODE, GL10.GL_LINEAR);
    gl10.glFogf(GL10.GL_FOG_START, 7f + (cameraDistance - 5f));
    gl10.glFogf(GL10.GL_FOG_END, Aquarium.max_x + 28.0f + (cameraDistance - 5f));

    synchronized (mScratch4f) {
      mScratch4f[0] = 0.011f;
      mScratch4f[1] = 0.4218f;
      mScratch4f[2] = 0.6445f;
      mScratch4f[3] = 1.0f;
      gl10.glFogfv(GL10.GL_FOG_COLOR, mScratch4f, 0);
    }
  }
  public void setupFog2(GL10 gl10) {
    gl10.glEnable(GL10.GL_FOG);
    gl10.glFogf(GL10.GL_FOG_MODE, GL10.GL_LINEAR);
    gl10.glFogf(GL10.GL_FOG_START, cameraDistance + 1f);
    gl10.glFogf(GL10.GL_FOG_END, cameraDistance + 10f);

    synchronized (mScratch4f) {
      mScratch4f[0] = 1.0f;
      mScratch4f[1] = 1.0f;
      mScratch4f[2] = 1.0f;
      mScratch4f[3] = 1.0f;
      gl10.glFogfv(GL10.GL_FOG_COLOR, mScratch4f, 0);
    }
  }

  public void updateSetting(Context context) {
    if (iwashi == null) {
      return;
    }
    int _iwashi_count = Prefs.getInstance(context).getIwashiCount();
    float _iwashi_speed = ((float)Prefs.getInstance(context).getIwashiSpeed() / 50f) * Iwashi.DEFAULT_SPEED;
    float _jinbei_speed = ((float)Prefs.getInstance(context).getJinbeiSpeed() / 50f) * Jinbei.DEFAULT_SPEED;
    boolean _iwashi_boids = Prefs.getInstance(context).getIwashiBoids();
    int _camera_mode = Prefs.getInstance(context).getCameraMode();
    float _camera_distance = (float)Prefs.getInstance(context).getCameraDistance();
/*DEBUG*/
if (false){
  _iwashi_count = 1;
  _camera_distance = 1f;
}
/*DEBUG*/
    if (_debug) Log.d(TAG, "現在のスピード:[" + _iwashi_speed + "]");

    if (_debug) Log.d(TAG,"現在のBOIDS:[" + _iwashi_boids + "]");

    if (_iwashi_count != iwashi_count) {
      synchronized (this) {
        iwashi_count = _iwashi_count;
        for (int ii=0; ii<MAX_IWASHI_COUNT; ii++) {
          ((Iwashi)iwashi[ii]).setIwashiCount(iwashi_count);
        }
      }
    }
    if (_iwashi_speed != iwashi_speed) {
      synchronized (this) {
        for (int ii=0; ii<MAX_IWASHI_COUNT; ii++) {
          ((Iwashi)iwashi[ii]).setSpeed(_iwashi_speed);
        }
        iwashi_speed = _iwashi_speed;
      }
    }
    if (_iwashi_boids != enableIwashiBoids) {
      synchronized (this) {
        for (int ii=0; ii<MAX_IWASHI_COUNT; ii++) {
          ((Iwashi)iwashi[ii]).setEnableBoids(_iwashi_boids);
        }
        enableIwashiBoids = _iwashi_boids;
      }
    }
    if (_jinbei_speed != jinbei_speed) {
      synchronized (this) {
        jinbei.setSpeed(_jinbei_speed);
        jinbei_speed = _jinbei_speed;
      }
    }
    if (_camera_mode != cameraMode) {
      cameraMode = _camera_mode;
    }
    if (_camera_distance != cameraDistance) {
      cameraDistance = _camera_distance;
    }
  }


  private int screen_width =0;
  private int screen_height = 0;
  public void onSurfaceChanged(GL10 gl10, int width, int height) {
    if (_debug) Log.d(TAG, "start onSurfaceChanged()");
    gl10.glViewport(0,0,width,height);
    gl10.glMatrixMode(GL10.GL_PROJECTION);
    gl10.glLoadIdentity();
    float ratio = (float) width / height;
    CoordUtil.perspective(gl10,perspectiveAngle, ratio, zNear, zFar);
    this.screen_width = width;
    this.screen_height = height;

    if (_debug) Log.d(TAG, "end onSurfaceChanged()"); 
  }
  public void onOffsetsChanged(GL10 gl10, float xOffset, float yOffset,
                               float xOffsetStep, float yOffsetStep,
                               int xPixelOffset, int yPixelOffset) {
    if (_debug) Log.d(TAG, "start onOffsetsChanged()");
    if (_debug) {
      Log.d(TAG,
          "xOffset:[" + xOffset + "]:"
        + "yOffset:[" + yOffset + "]:"
        + "xOffsetStep:[" + xOffsetStep + "]:"
        + "yOffsetStep:[" + yOffsetStep + "]:"
        + "xPixelOffset:[" + xPixelOffset + "]:"
        + "yPixelOffset:[" + yPixelOffset + "]:");
    }
    synchronized(this) {
      float xx = (float)xPixelOffset / 480f;
      baseAngle = xx * 180f + 90f;
//      if (xOffset >= 0.0f && xOffset <= 1.0f) {
//        float offset = xOffset - 0.5f;
//        baseAngle = offset * (-180f);
//      }
    }
    if (_debug) {
      Log.d(TAG, 
          "end onOffsetsChanged():" 
        + "new baseAngle:[" + baseAngle + "]"
      );
    }
  }

  public void onCommand(GL10 gl10, String action, int x, int y, int z, Bundle extras, boolean resultRequested) {
    if (_debug) Log.d(TAG, "start onCommand");
    if (cameraMode == R.id.radio1 || cameraMode == R.id.radio2) {
      /* 鰯視点モード/ジンベイザメ視点モードのため、何もしない */
      return ;
    }
    /*=======================================================================*/
    /* タッチされたら寄ってくる                                              */
    /* 餌はiwashi_count分                                                    */
    /*=======================================================================*/

    /*=======================================================================*/
    /* スクリーン座標ー＞ワールド座標変換                                    */
    /*=======================================================================*/
    float[] modelview = new float[16];
    ((GL11)gl10).glGetFloatv(GL10.GL_MODELVIEW, modelview, 0);
    float[] projection = new float[16];
    ((GL11)gl10).glGetFloatv(GL10.GL_PROJECTION, projection, 0);
    float[] viewport = new float[16];
    ((GL11)gl10).glGetFloatv(GL11.GL_VIEWPORT, viewport, 0);

    float[] view = new float[16];
    System.arraycopy(CoordUtil.viewMatrix, 0, view, 0, 16);
    float nx = 0f;
    float ny = 0f;
    float nz = 0f;
    {
      float[] ret = new float[4];
      /* カメラから水槽までの距離を算出 */
      float dist_from_camera = 0.0f;
      dist_from_camera = cameraDistance;
      if (dist_from_camera < 0.0f) {
        dist_from_camera = 0.0f;
      }
      else {
        dist_from_camera = (dist_from_camera / (zFar - zNear));
        if (dist_from_camera > 1.0f) {
          dist_from_camera = 1.0f;
        }
      }
      
      GLU.gluUnProject((float)x, (float)y, dist_from_camera, view, 0, projection, 0, new int[]{0, 0, screen_width, screen_height}, 0, ret, 0);
      if (_debug) Log.d(TAG,"変換結果(UnProject):[" + ret[0] + "][" + ret[1] + "][" + ret[2] + "][" + ret[3] + "]");
      {
        float bb = (cameraDistance == 0.0f) ? 0.1f : cameraDistance;
        nx = ret[0] * bb / ret[3];
        ny = ret[1] * -bb / ret[3];
        nz = ret[2] / ret[3];
      }
      if (_debug) {
        Log.d(TAG,"変換結果"
         + "dist:[" + dist_from_camera + "] "
         + "x:[" + nx + "] "
         + "y:[" + ny + "] "
         + "z:[" + nz + "] "
        );
      }
    }

    synchronized (mScratch4f) {
      coordUtil.setMatrixRotateY(-baseAngle);
      coordUtil.affine(nx,ny, nz, mScratch4f);
      nx = mScratch4f[0];
      ny = mScratch4f[1];
      nz = mScratch4f[2];
      
    }

    {
      float tmp = 0f;
      if (nx > Aquarium.max_x.floatValue() - 0.2f) {
        tmp = (Aquarium.max_x.floatValue() - 0.2f) / nx;
        nx *= tmp;
        ny *= tmp;
        nz *= tmp;
      }
      if (ny > Aquarium.max_y.floatValue() - 0.2f) {
        tmp = (Aquarium.max_y.floatValue() - 0.2f) / ny;
        nx *= tmp;
        ny *= tmp;
        nz *= tmp;
      }
      if (nz > Aquarium.max_z.floatValue() - 0.2f) {
        tmp = (Aquarium.max_z.floatValue() - 0.2f) / nz;
        nx *= tmp;
        ny *= tmp;
        nz *= tmp;
      }
      if (nx < Aquarium.min_x.floatValue() + 0.2f) {
        tmp = (Aquarium.min_x.floatValue() + 0.2f) / nx;
        nx *= tmp;
        ny *= tmp;
        nz *= tmp;
      }
      if (ny < Aquarium.min_y.floatValue() + 0.2f) {
        tmp = (Aquarium.min_y.floatValue() + 0.2f) / ny;
        nx *= tmp;
        ny *= tmp;
        nz *= tmp;
      }
      if (nz < Aquarium.min_z.floatValue() + 0.2f) {
        tmp = (Aquarium.min_z.floatValue() + 0.2f) / nz;
        nx *= tmp;
        ny *= tmp;
        nz *= tmp;
      }
    }
    baitManager.addBait(nx,ny,nz);


    if (_debug) Log.d(TAG, "end onCommand");
  }


  public synchronized void onDrawFrame(GL10 gl10) {
    setupFog(gl10);
    gl10.glMatrixMode(GL10.GL_MODELVIEW);
    gl10.glPushMatrix(); 

    // 画面をクリアする
    //gl10.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT | GL10.GL_STENCIL_BUFFER_BIT);
    gl10.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

    // モデルの位置を決める
    gl10.glMatrixMode(GL10.GL_MODELVIEW);
    gl10.glLoadIdentity();
//    gl10.glDisable(GL10.GL_STENCIL_TEST);


    // カメラ
    if (cameraMode == R.id.radio1) {
      /* 鰯視点モード */
      float c_x = iwashi[0].getX();
      float c_y = iwashi[0].getY();
      float c_z = iwashi[0].getZ();
      CoordUtil.lookAt(gl10,
                    c_x, 
                    c_y,
                    c_z,
                    c_x + iwashi[0].getDirectionX(),
                    c_y + iwashi[0].getDirectionY(),
                    c_z + iwashi[0].getDirectionZ(),
                    0,1,0);
      Log.d(TAG, "eye [" + c_x + "," + c_y + "," + c_z + "] "
               + "tar [" + iwashi[0].getDirectionX() + "," + iwashi[0].getDirectionY() + "," + iwashi[0].getDirectionZ() + "]");
    }
    else if (cameraMode == R.id.radio2) {
      /* ジンベイザメモード */
      float c_x = jinbei.getX();
      float c_y = jinbei.getY();
      float c_z = jinbei.getZ();
      CoordUtil.lookAt(gl10,
                    c_x - jinbei.getDirectionX(),
                    c_y - jinbei.getDirectionY(),
                    c_z - jinbei.getDirectionZ(),
                    c_x + jinbei.getDirectionX(),
                    c_y + jinbei.getDirectionY(),
                    c_z + jinbei.getDirectionZ(),
                    0,1,0);
    }
    else {
      /* 通常モード */
      CoordUtil.lookAt(gl10,
                    camera[0],camera[1],camera[2]+cameraDistance,
                    camera[0],camera[1],-10f,
                    0,1,0);
    }
    gl10.glPushMatrix();
    gl10.glRotatef(baseAngle, 0.0f, 1.0f, 0.0f);


    /*=======================================================================*/
    /* 光のセットアップ                                                      */
    /*=======================================================================*/
    setupLighting2(gl10);
    setupLighting1(gl10);

//    gl10.glDisable(GL10.GL_STENCIL_TEST);
    gl10.glDisable(GL10.GL_DEPTH_TEST);

    jinbei.calc();
    synchronized (this) {
      for (int ii=0; ii<iwashi_count; ii++) {
        iwashi[ii].calc();
      }
    }

    // 背景描画
    background.draw(gl10);
    ground.draw(gl10, iwashi);
    wave.calc();
    
    // model
    // リフレクションの描画
//    synchronized (this) {
      wave.draw(gl10);
//      //ステンシルテストの有効化
//      gl10.glColorMask(false,false,false,false);
//      gl10.glDepthMask(false);
//      gl10.glEnable(GL10.GL_STENCIL_TEST);
//      gl10.glStencilOp(GL10.GL_REPLACE,GL10.GL_REPLACE,GL10.GL_REPLACE);
//      gl10.glStencilFunc(GL10.GL_ALWAYS,1,~0);
//      wave.draw(gl10);
//      {
//        gl10.glPushMatrix();
//        gl10.glStencilOp(GL10.GL_KEEP, GL10.GL_KEEP, GL10.GL_INCR);
//        gl10.glStencilFunc(GL10.GL_EQUAL, 1, ~0);
//        gl10.glRotatef(-baseAngle, 0.0f, 1.0f, 0.0f);
//        wave.drawForStencil(gl10);
//        gl10.glPopMatrix();
//      }
//      gl10.glColorMask(true,true,true,true);
//      gl10.glDepthMask(true);
//      gl10.glStencilOp(GL10.GL_KEEP, GL10.GL_KEEP, GL10.GL_KEEP);
//      gl10.glStencilFunc(GL10.GL_EQUAL, 2, ~0);
//      gl10.glPushMatrix();
//      gl10.glScalef(1.0f,-1.0f,1.0f);
//      gl10.glTranslatef(0f,-Aquarium.max_y+Aquarium.min_y-0.5f,0f);
//      gl10.glPushMatrix();
//
//      {
//        setupFog2(gl10);
//        for (int ii=0; ii<iwashi_count; ii++) {
//          if (iwashi[ii].getY() >= Aquarium.max_y / 3.0f * 2.0f) {
//            iwashi[ii].draw(gl10);
//          }
//        }
//        setupFog(gl10);
//      }
//      gl10.glPopMatrix();
//     gl10.glPopMatrix();
//
//      gl10.glDisable(GL10.GL_STENCIL_TEST);
//    }
    gl10.glEnable(GL10.GL_DEPTH_TEST);
    synchronized (this) {
      for (int ii=0; ii<iwashi_count; ii++) {
        if (cameraMode == R.id.radio1 && ii == 0) {
          // 鰯視点モードのときは、自分は描画しない
        }
        else {
          iwashi[ii].draw(gl10);
        }
      }
    }
    if (cameraMode != R.id.radio2) {
      // ジンベイザメ視点モードのときは、自分は描画しない
      jinbei.draw(gl10);
    }
    gl10.glDisable(GL10.GL_DEPTH_TEST);
    gl10.glPopMatrix(); 
            
    gl10.glPopMatrix();
  }

  public void onDestroy() {
  }
}
