package jp.co.qsdn.android.atlantis;

import android.content.Context;

import android.graphics.Canvas;
import android.graphics.Paint;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;

import android.util.Log;


import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import jp.co.qsdn.android.atlantis.model.Iwashi;

public class GLRenderer implements GLSurfaceView.Renderer {
  private static final String TAG = GLRenderer.class.getName();
  private final Context context;
  private final Background background = new Background();
  //private final Front front = new Front();
  //private final Ground ground = new Ground();
  private final Aquarium aquarium = new Aquarium();
  private Iwashi[] iwashi = null;
  private int iwashi_count = 1;
  private float iwashi_speed = 0.03f;
  /* カメラの位置 */
  private float[] camera = {0f,0f,0f};

  GLRenderer(Context context) { 
    this.context = context;
  }

  public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
    Log.d(TAG, "start onSurfaceCreated()");
    // GLオプションのセットアップ
    gl10.glEnable(GL10.GL_DEPTH_TEST);
    gl10.glDepthFunc(GL10.GL_LEQUAL);
    gl10.glEnableClientState(GL10.GL_VERTEX_ARRAY);
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
    //Ground.loadTexture(gl10, context, R.drawable.sand);
    Iwashi.loadTexture(gl10, context, R.drawable.iwashi);

    iwashi_count = SettingActivity.getIwashiCount(context);
    iwashi_speed = SettingActivity.getIwashiSpeed(context);

    iwashi = new Iwashi[iwashi_count];
    for (int ii=0; ii<iwashi_count; ii++) {
      iwashi[ii] = new Iwashi(ii);
    }
    for (int ii=0; ii<iwashi_count; ii++) {
      iwashi[ii].setSpecies(iwashi);
      iwashi[ii].setSpeed(iwashi_speed);
    }

    camera[0] = 0f;
    camera[1] = 0f;
    camera[2] = Aquarium.max_z + 0.5f;

    /* for Debug */
/*
    for (int ii=0; ii<iwashi_count; ii++) {
      iwashi[ii].setX(0f);
      iwashi[ii].setY(0f);
      iwashi[ii].setZ(0f);
    }
*/
    /*=======================================================================*/
    /* 光のセットアップ                                                      */
    /*=======================================================================*/
    setupLighting1(gl10);
    Log.d(TAG, "end onSurfaceCreated()");
  }

  /**
   * 光のセットアップ
   */
  public void setupLighting1(GL10 gl10) {
    gl10.glEnable(GL10.GL_LIGHTING);
    gl10.glEnable(GL10.GL_LIGHT0);
    /*=======================================================================*/
    /* 環境光の材質色設定                                                    */
    /*=======================================================================*/
    float[] mat_amb = { 
      0.7f, 
      0.7f, 
      1.0f,
      1.0f,
     };
    gl10.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, mat_amb, 0);
    /*=======================================================================*/
    /* 拡散反射光の色設定                                                    */
    /*=======================================================================*/
    float[] mat_diff = { 
      1.0f * 0.8f, 
      1.0f * 0.8f, 
      1.0f * 0.8f, 
      1.0f,
     };
    gl10.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, mat_diff, 0);
    /*=======================================================================*/
    /* 鏡面反射光の色設定                                                    */
    /*=======================================================================*/
    float[] mat_spec = { 0.1f* 0.2f, 0.1f * 0.2f, 0.1f * 0.8f, 0.8f };
    gl10.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, mat_spec, 0);
    gl10.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, 64f);
  }
  public void setupLighting2(GL10 gl10) {
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
    /* 鏡面反射光の位置設定                                                  */
    /*=======================================================================*/
    float[] spec = { 1.0f, 1.0f, 1.0f, 1.0f };
    gl10.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, spec, 0);
    /*=======================================================================*/
    /* そもそもの光の位置設定                                                */
    /*=======================================================================*/
    float[] pos = { 0.0f, 8.0f, 1.0f, 0.0f };
    gl10.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, pos, 0);
    /*=======================================================================*/
    /* そもそもの光の向き設定                                                */
    /*=======================================================================*/
    float[] dir = { 0.0f, -1.0f, 0.0f };
    gl10.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPOT_DIRECTION, dir, 0);
    gl10.glLightf(GL10.GL_LIGHT0, GL10.GL_SPOT_CUTOFF, 180);
  }

  public void updateSetting() {
    if (iwashi == null) {
      return;
    }
    int _iwashi_count = SettingActivity.getIwashiCount(context);
    float _iwashi_speed = SettingActivity.getIwashiSpeed(context);
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
          iwashi[ii].setSpecies(iwashi);
        }
      }
    }
    if (_iwashi_speed != iwashi_speed) {
      synchronized (this) {
        for (int ii=0; ii<iwashi_count; ii++) {
          iwashi[ii].setSpeed(_iwashi_speed);
          iwashi_speed = _iwashi_speed;
        }
      }
    }
  }

  public void onSurfaceChanged(GL10 gl10, int width, int height) {
    Log.d(TAG, "start onSurfaceChanged()");
    // ビューフラスタムを定義
    gl10.glViewport(0,0,width,height);
    gl10.glMatrixMode(GL10.GL_PROJECTION);
    gl10.glLoadIdentity();
    float ratio = (float) width / height;
    GLU.gluPerspective(gl10, 45.0f, ratio, 1, 100f);

    Log.d(TAG, "end onSurfaceChanged()"); 
  }
  public void onOffsetsChanged(GL10 gl10, float xOffset, float yOffset,
                               float xOffsetStep, float yOffsetStep,
                               int xPixelOffset, int yPixelOffset) {
    Log.d(TAG, "start onOffsetsChanged()");
    Log.d(TAG,
        "xOffset:[" + xOffset + "]:"
      + "yOffset:[" + yOffset + "]:"
      + "xOffsetStep:[" + xOffsetStep + "]:"
      + "yOffsetStep:[" + yOffsetStep + "]:"
      + "xPixelOffset:[" + xPixelOffset + "]:"
      + "yPixelOffset:[" + yPixelOffset + "]:");
    synchronized(this) {
      float newCamera_x = xOffset - 0.5f;
      if (newCamera_x >= 0.0f && newCamera_x <= 1.0f) {
        camera[0] = camera[0] + newCamera_x;
      }
    }
    Log.d(TAG, "end onOffsetsChanged()");
  }


  public void onDrawFrame(GL10 gl10) {
    //Log.d(TAG, "start onDrawFrame()");
    gl10.glPushMatrix(); 

    // 画面をクリアする
    gl10.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

    // モデルの位置を決める
    gl10.glMatrixMode(GL10.GL_MODELVIEW);
    gl10.glLoadIdentity();
    // カメラ
    GLU.gluLookAt(gl10,
                  camera[0],camera[1],camera[2] + 10f,
                  camera[0],camera[1],-100f,
                  0,1,0);
    // ライト
    setupLighting2(gl10);

    // 背景描画
    background.draw(gl10);
    //front.draw(gl10);
//    ground.draw(gl10);
//    aquarium.draw(gl10);
    
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
            
    //Log.d(TAG, "end onDrawFrame()");
  }
}
