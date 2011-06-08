package jp.co.qsdn.android.iwashi3d;

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

import jp.co.qsdn.android.iwashi3d.model.Iwashi;
import jp.co.qsdn.android.iwashi3d.setting.Prefs;
import jp.co.qsdn.android.iwashi3d.util.CoordUtil;

public class GLRenderer implements GLSurfaceView.Renderer {
  private static final boolean _debug = false;
  private static final String TAG = GLRenderer.class.getName();
  private final Context context;
  private final Background background = new Background();
  private final Ground ground = new Ground();
  private final Wave wave = new Wave();
  private Iwashi[] iwashi = null;
  private int iwashi_count = 1;
  private boolean enableIwashiBoids = true;
  private float iwashi_speed = 0.03f;
  /* カメラの位置 */
  private float[] camera = {0f,0f,0f};
  private float[] org_camera = {0f,0f,0f};
  private boolean cameraMode = false; /* false:通常モード true:鰯視点モード */
  private float cameraDistance = 10f; /* 群れまでの距離 */

  private BaitManager baitManager = new BaitManager();
  private float baseAngle = 0f;

  GLRenderer(Context context) { 
    this.context = context;
  }

  public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
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
    /* テクスチャ                                                            */
    /*=======================================================================*/
    gl10.glEnable(GL10.GL_TEXTURE_2D);
    Background.loadTexture(gl10, context, R.drawable.background);
    Ground.loadTexture(gl10, context, R.drawable.sand);
    Wave.loadTexture(gl10, context, R.drawable.wave);
    Iwashi.loadTexture(gl10, context, R.drawable.iwashi);

    iwashi_count = Prefs.getInstance(context).getIwashiCount();
    iwashi_speed = ((float)Prefs.getInstance(context).getIwashiSpeed() / 50f) * 0.04f;
    if (_debug) Log.d(TAG, "現在のスピード:[" + iwashi_speed + "]");
    enableIwashiBoids = Prefs.getInstance(context).getIwashiBoids();
    cameraDistance = (float)Prefs.getInstance(context).getCameraDistance();
    cameraMode = Prefs.getInstance(context).getCameraMode();
    

    iwashi = new Iwashi[iwashi_count];
    for (int ii=0; ii<iwashi_count; ii++) {
      iwashi[ii] = new Iwashi(ii);
    }
    for (int ii=0; ii<iwashi_count; ii++) {
      iwashi[ii].setEnableBoids(enableIwashiBoids);
      iwashi[ii].setSpecies(iwashi);
      iwashi[ii].setSpeed(iwashi_speed);
      iwashi[ii].setBaitManager(baitManager);
    }

    org_camera[0] = camera[0] = 0f;
    org_camera[1] = camera[1] = 0f;
    org_camera[2] = camera[2] = Aquarium.max_z;

    /*=======================================================================*/
    /* フォグのセットアップ                                                  */
    /*=======================================================================*/
    setupFog(gl10);

    gl10.glEnable(GL10.GL_NORMALIZE) ;
    gl10.glEnable(GL10.GL_RESCALE_NORMAL);
    gl10.glShadeModel(GL10.GL_SMOOTH);


    if (_debug) Log.d(TAG, "end onSurfaceCreated()");
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
      float[] amb = { 1.0f, 1.0f, 1.0f, 1.0f };
      gl10.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, amb, 0);
      /*=======================================================================*/
      /* 拡散反射光の色設定                                                    */
      /*=======================================================================*/
      float[] diff = { 1.0f, 1.0f, 1.0f, 1.0f };
      gl10.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, diff, 0);
      /*=======================================================================*/
      /* 鏡面反射光の色設定                                                    */
      /*=======================================================================*/
      float[] spec = { 1.0f, 1.0f, 1.0f, 1.0f };
      gl10.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, spec, 0);
      /*=======================================================================*/
      /* そもそもの光の位置設定                                                */
      /*=======================================================================*/
      float[] pos1 = { 0.0f, 10.0f, 0.0f, 1.0f };
      gl10.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, pos1, 0);
      /*=======================================================================*/
      /* そもそもの光の向き設定                                                */
      /*=======================================================================*/
      float[] dir = { 0.0f, -1.0f, 0.0f };
      gl10.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPOT_DIRECTION, dir, 0);
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
      float[] amb = { 0.019f, 0.9606f, 1.0f, 1.0f };
      gl10.glLightfv(GL10.GL_LIGHT1, GL10.GL_AMBIENT, amb, 0);
      /*=======================================================================*/
      /* 拡散反射光の色設定                                                    */
      /*=======================================================================*/
      float[] diff = { 0.019f, 0.9606f, 1.0f, 1.0f };
      gl10.glLightfv(GL10.GL_LIGHT1, GL10.GL_DIFFUSE, diff, 0);
      /*=======================================================================*/
      /* 鏡面反射光の色設定                                                    */
      /*=======================================================================*/
      float[] spec = { 0.019f, 0.9606f, 1.0f, 1.0f };
      gl10.glLightfv(GL10.GL_LIGHT1, GL10.GL_SPECULAR, spec, 0);
      /*=======================================================================*/
      /* そもそもの光の位置設定                                                */
      /*=======================================================================*/
      float[] pos = { 0.0f, -10.0f, 0.0f, 1.0f };
      gl10.glLightfv(GL10.GL_LIGHT1, GL10.GL_POSITION, pos, 0);
      /*=======================================================================*/
      /* そもそもの光の向き設定                                                */
      /*=======================================================================*/
      float[] dir = { 0.0f, 1.0f, 0.0f };
      gl10.glLightfv(GL10.GL_LIGHT1, GL10.GL_SPOT_DIRECTION, dir, 0);
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
    gl10.glFogf(GL10.GL_FOG_END, 21.0f + (cameraDistance - 5f));

    float[] color = {
      0.011f, 0.4218f, 0.6445f, 1.0f,
    };
    gl10.glFogfv(GL10.GL_FOG_COLOR, color, 0);
  }

  public void updateSetting() {
    if (iwashi == null) {
      return;
    }
    int _iwashi_count = Prefs.getInstance(context).getIwashiCount();
    float _iwashi_speed = ((float)Prefs.getInstance(context).getIwashiSpeed() / 50f) * 0.04f;
    boolean _iwashi_boids = Prefs.getInstance(context).getIwashiBoids();
    boolean _camera_mode = Prefs.getInstance(context).getCameraMode();
    float _camera_distance = (float)Prefs.getInstance(context).getCameraDistance();
/*DEBUG*/
if (false){
  _iwashi_count = 1;
  _camera_distance = 10f;
}
/*DEBUG*/
    if (_debug) Log.d(TAG, "現在のスピード:[" + _iwashi_speed + "]");

    if (_debug) Log.d(TAG,"現在のBOIDS:[" + _iwashi_boids + "]");

    if (_iwashi_count != iwashi_count) {
      synchronized (this) {
        Iwashi[] newIwashi = new Iwashi[_iwashi_count];
        for (int ii=0; ii<_iwashi_count; ii++) {
          if (ii < iwashi_count) {
            newIwashi[ii] = iwashi[ii];
          }
          else {
            newIwashi[ii] = new Iwashi(ii);
          }
        }
        for (int ii=0; ii<iwashi_count; ii++) {
          iwashi[ii] = null;
        }
        iwashi = null;
        iwashi_count = _iwashi_count;
        iwashi = newIwashi;
        for (int ii=0; ii<iwashi_count; ii++) {
          iwashi[ii].setEnableBoids(enableIwashiBoids);
          iwashi[ii].setSpecies(iwashi);
          iwashi[ii].setBaitManager(baitManager);
        }
      }
    }
    if (_iwashi_speed != iwashi_speed) {
      synchronized (this) {
        for (int ii=0; ii<iwashi_count; ii++) {
          iwashi[ii].setSpeed(_iwashi_speed);
        }
        iwashi_speed = _iwashi_speed;
      }
    }
    if (_iwashi_boids != enableIwashiBoids) {
      for (int ii=0; ii<iwashi_count; ii++) {
        iwashi[ii].setEnableBoids(_iwashi_boids);
      }
      enableIwashiBoids = _iwashi_boids;
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
    CoordUtil.perspective(gl10,45.0f, ratio, 0.1f, 50f);
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
if (false) {
      if (xOffset >= 0.0f && xOffset <= 1.0f) {
        float newCamera_x = xOffset - 0.5f;
        camera[0] = org_camera[0] + (newCamera_x * 3f);
      }
}
else {
      if (xOffset >= 0.0f && xOffset <= 1.0f) {
        float offset = xOffset - 0.5f;
        baseAngle = offset * (-180f);
      }
}
    }
    if (_debug) {
      Log.d(TAG, "end onOffsetsChanged()");
    }
  }

  public void onCommand(GL10 gl10, String action, int x, int y, int z, Bundle extras, boolean resultRequested) {
    if (_debug) Log.d(TAG, "start onCommand");
    if (cameraMode) {
      /* 鰯視点モードのため、何もしない */
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
      GLU.gluUnProject((float)x, (float)y, 0.85f, view, 0, projection, 0, new int[]{0, 0, screen_width, screen_height}, 0, ret, 0);
      if (_debug) Log.d(TAG,"変換結果(UnProject):[" + ret[0] + "][" + ret[1] + "][" + ret[2] + "][" + ret[3] + "]");
      if (_debug) Log.d(TAG,"変換結果(UnProject):[" + (ret[0]/ret[3]) + "][" + (ret[1]/ret[3]) + "][" + (ret[2]/ret[3]) + "][" + ret[3] + "]");
      nx = (ret[0] / ret[3]);
      ny = (ret[1] / ret[3]) * -1f;
      //nz = (ret[2] / ret[3]);
      nz = Aquarium.max_z - 0.2f;
      if (_debug) {
        Log.d(TAG,"変換結果"
         + "x:[" + nx + "] "
         + "y:[" + ny + "] "
         + "z:[" + nz + "] "
        );
      }
    }

    baitManager.addBait(nx,ny,nz);

    if (_debug) Log.d(TAG, "end onCommand");
  }


  public synchronized void onDrawFrame(GL10 gl10) {
    if (_debug) Log.d(TAG, "start onDrawFrame()");
    setupFog(gl10);
    gl10.glMatrixMode(GL10.GL_MODELVIEW);
    gl10.glPushMatrix(); 

    // 画面をクリアする
    gl10.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

    // モデルの位置を決める
    gl10.glMatrixMode(GL10.GL_MODELVIEW);
    gl10.glLoadIdentity();


    // カメラ
    if (cameraMode) {
      /* 鰯視点モード */
      float c_x = iwashi[0].getX() - iwashi[0].getDirectionX() * (cameraDistance/5.0f);
      float c_y = iwashi[0].getY() - iwashi[0].getDirectionY() * (cameraDistance/5.0f);
      float c_z = iwashi[0].getZ() - iwashi[0].getDirectionZ() * (cameraDistance/5.0f);
      CoordUtil.lookAt(gl10,
                    c_x, c_y, c_z,
                    iwashi[0].getDirectionX(),iwashi[0].getDirectionY(),iwashi[0].getDirectionZ(),
                    0,1,0);
    }
    else {
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

    // 背景描画
    background.draw(gl10);
    ground.draw(gl10);
    wave.draw(gl10);
    
    // model
    /* 群れの中心算出 */
    float[] schoolCenter = {0f,0f,0f};
    for (int ii=0; ii<iwashi_count; ii++) {
      schoolCenter[0] += iwashi[ii].getX();;
      schoolCenter[1] += iwashi[ii].getY();;
      schoolCenter[2] += iwashi[ii].getZ();;
    }
    schoolCenter[0] /= iwashi_count;
    schoolCenter[1] /= iwashi_count;
    schoolCenter[2] /= iwashi_count;
    synchronized (this) {
      for (int ii=0; ii<iwashi_count; ii++) {
          iwashi[ii].setSchoolCenter(schoolCenter);
          iwashi[ii].draw(gl10);
      }
    }
    gl10.glPopMatrix(); 
            
    gl10.glPopMatrix();
    if (_debug) Log.d(TAG, "end onDrawFrame()");
  }
}
