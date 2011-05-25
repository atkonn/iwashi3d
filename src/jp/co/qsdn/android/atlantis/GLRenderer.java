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
    gl10.glEnable(GL10.GL_TEXTURE_2D);
    Background.loadTexture(gl10, context, R.drawable.background);
    //Ground.loadTexture(gl10, context, R.drawable.sand);
    Iwashi.loadTexture(gl10, context, R.drawable.iwashi);

    iwashi_count = SettingActivity.getIwashiCount(context);

    iwashi = new Iwashi[iwashi_count];
    for (int ii=0; ii<iwashi_count; ii++) {
      iwashi[ii] = new Iwashi();
    }
    for (int ii=0; ii<iwashi_count; ii++) {
      iwashi[ii].setSpecies(iwashi);
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
    Log.d(TAG, "end onSurfaceCreated()");
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
    float newCamera_x = xOffset - 0.5f;
    camera[0] = camera[0] + newCamera_x;
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
    GLU.gluLookAt(gl10,
                  camera[0],camera[1],camera[2] + 10f,
                  camera[0],camera[1],-100f,
                  0,1,0);

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
    for (int ii=0; ii<iwashi_count; ii++) {
      iwashi[ii].setSchoolCenter(schoolCenter);
      iwashi[ii].draw(gl10);
    }
    gl10.glPopMatrix(); 
            
    //Log.d(TAG, "end onDrawFrame()");
  }
}
