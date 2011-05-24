package jp.co.qsdn.android.atlantis;

import android.content.Context;

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
  private final Ground ground = new Ground();
  private Iwashi[] iwashi = null;
  private int iwashi_count = 10;

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
    Ground.loadTexture(gl10, context, R.drawable.sand);
    Iwashi.loadTexture(gl10, context, R.drawable.iwashi);
    iwashi = new Iwashi[iwashi_count];
    for (int ii=0; ii<iwashi_count; ii++) {
      iwashi[ii] = new Iwashi();
    }
    for (int ii=0; ii<iwashi_count; ii++) {
      iwashi[ii].setSpecies(iwashi);
    }

    /* for Debug */
/*
    for (int ii=0; ii<iwashi_count; ii++) {
      iwashi[ii].setX(0);
      iwashi[ii].setY(0);
      iwashi[ii].setZ(0);
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
    // カメラの位置を決める
    gl10.glMatrixMode(GL10.GL_MODELVIEW);
    gl10.glLoadIdentity();
    GLU.gluLookAt(gl10, 300f, 10f, 0, 0, 0, 0, 0, 1f, 0f);

    Log.d(TAG, "end onSurfaceChanged()"); 
  }

  float count = 0;

  public void onDrawFrame(GL10 gl10) {
    //Log.d(TAG, "start onDrawFrame()");
    // 画面をクリアする
    gl10.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);


    // モデルの位置を決める
    gl10.glMatrixMode(GL10.GL_MODELVIEW);
    gl10.glLoadIdentity();

    // 背景描画
    background.draw(gl10);
    ground.draw(gl10);
    
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



    //Log.d(TAG, "end onDrawFrame()");
  }
}
