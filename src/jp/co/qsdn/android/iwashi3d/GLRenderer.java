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

import jp.co.qsdn.android.iwashi3d.model.Background;
import jp.co.qsdn.android.iwashi3d.model.Ground;
import jp.co.qsdn.android.iwashi3d.model.Iwashi;
import jp.co.qsdn.android.iwashi3d.model.Wave;
import jp.co.qsdn.android.iwashi3d.setting.Prefs;
import jp.co.qsdn.android.iwashi3d.util.CoordUtil;

//public class GLRenderer implements GLSurfaceView.Renderer {
public class GLRenderer {
  private static final boolean _debug = false;
  private static final String TAG = GLRenderer.class.getName();
  private static final int MAX_IWASHI_COUNT = 50;
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
  private float zFar = 50.0f;
  private float zNear = 1.0f;
  private float perspectiveAngle = 45.0f;

  private BaitManager baitManager = new BaitManager();
  private float baseAngle = 0f;
  private float[] mScratch32 = new float[32];
  private float[] mScratch4f = new float[4];
  public static GLRenderer glRenderer = null;
  /* 群れの中心 */
  float[] schoolCenter = {0f,0f,0f};

  private GLRenderer(Context context) {
    iwashi_count = Prefs.getInstance(context).getIwashiCount();
    iwashi_speed = ((float)Prefs.getInstance(context).getIwashiSpeed() / 50f) * Iwashi.DEFAULT_SPEED;
    enableIwashiBoids = Prefs.getInstance(context).getIwashiBoids();
    cameraDistance = (float)Prefs.getInstance(context).getCameraDistance();
    cameraMode = Prefs.getInstance(context).getCameraMode();
    

    iwashi = new Iwashi[MAX_IWASHI_COUNT];
    for (int ii=0; ii<MAX_IWASHI_COUNT; ii++) {
      iwashi[ii] = new Iwashi(ii);
    }
    for (int ii=0; ii<MAX_IWASHI_COUNT; ii++) {
      iwashi[ii].setEnableBoids(enableIwashiBoids);
      iwashi[ii].setSpecies(iwashi);
      iwashi[ii].setSpeed(iwashi_speed);
      iwashi[ii].setBaitManager(baitManager);
    }
/*DEBUG*/
if (false) {
    for (int ii=0; ii<MAX_IWASHI_COUNT; ii++) {
      iwashi[ii].setX(0.0f);
      iwashi[ii].setY(0.0f);
      iwashi[ii].setZ(0.0f);
    }
}
/*DEBUG*/
  }
  
  public static GLRenderer getInstance(Context context) {
    Log.d(TAG, "start getInstance()");
    if (glRenderer == null) {
      Log.d(TAG, "new GLRenderer");
      glRenderer = new GLRenderer(context);
    }
    Log.d(TAG, "end getInstance()");
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
    gl10.glClearStencil(0);

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
        mScratch4f[0] = 0.019f;
        mScratch4f[1] = 0.9606f;
        mScratch4f[2] = 1.0f;
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
    gl10.glFogf(GL10.GL_FOG_END, 26.0f + (cameraDistance - 5f));

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
    boolean _iwashi_boids = Prefs.getInstance(context).getIwashiBoids();
    boolean _camera_mode = Prefs.getInstance(context).getCameraMode();
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
      }
    }
    if (_iwashi_speed != iwashi_speed) {
      synchronized (this) {
        for (int ii=0; ii<MAX_IWASHI_COUNT; ii++) {
          iwashi[ii].setSpeed(_iwashi_speed);
        }
        iwashi_speed = _iwashi_speed;
      }
    }
    if (_iwashi_boids != enableIwashiBoids) {
      synchronized (this) {
        for (int ii=0; ii<MAX_IWASHI_COUNT; ii++) {
          iwashi[ii].setEnableBoids(_iwashi_boids);
        }
        enableIwashiBoids = _iwashi_boids;
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
      if (xOffset >= 0.0f && xOffset <= 1.0f) {
        float offset = xOffset - 0.5f;
        baseAngle = offset * (-180f);
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

    if (baseAngle > 0.0f) {
      if (nz > Aquarium.max_x - 0.2f) {
        nz = Aquarium.max_x - 0.2f;
      }
      if (nz < Aquarium.min_x + 0.2f) {
        nz = Aquarium.min_x + 0.2f;
      }
      if (ny > Aquarium.max_y - 0.2f) {
        ny = Aquarium.max_y - 0.2f;
      }
      if (ny < Aquarium.min_y + 0.2f) {
        ny = Aquarium.min_y + 0.2f;
      }
      if (nx > Aquarium.max_z - 0.2f) {
        nx = Aquarium.max_z - 0.2f; 
      }
      if (nx < Aquarium.min_z + 0.2f) {
        nx = Aquarium.min_z + 0.2f; 
      }
      baitManager.addBait(-nz,ny,nx);
    }
    else if (baseAngle < 0.0f) {
      if (nz > Aquarium.max_x - 0.2f) {
        nz = Aquarium.max_x - 0.2f;
      }
      if (nz < Aquarium.min_x + 0.2f) {
        nz = Aquarium.min_x + 0.2f;
      }
      if (ny > Aquarium.max_y - 0.2f) {
        ny = Aquarium.max_y - 0.2f;
      }
      if (ny < Aquarium.min_y + 0.2f) {
        ny = Aquarium.min_y + 0.2f;
      }
      if (nx > Aquarium.max_z - 0.2f) {
        nx = Aquarium.max_z - 0.2f; 
      }
      if (nx < Aquarium.min_z + 0.2f) {
        nx = Aquarium.min_z + 0.2f; 
      }
      baitManager.addBait(nz,ny,-nx);
    }
    else {
      if (nx > Aquarium.max_x - 0.2f) {
        nx = Aquarium.max_x - 0.2f;
      }
      if (nx < Aquarium.min_x + 0.2f) {
        nx = Aquarium.min_x + 0.2f;
      }
      if (ny > Aquarium.max_y - 0.2f) {
        ny = Aquarium.max_y - 0.2f;
      }
      if (ny < Aquarium.min_y + 0.2f) {
        ny = Aquarium.min_y + 0.2f;
      }
      if (nz > Aquarium.max_z - 0.2f) {
        nz = Aquarium.max_z - 0.2f; 
      }
      if (nz < Aquarium.min_z + 0.2f) {
        nz = Aquarium.min_z + 0.2f; 
      }
      baitManager.addBait(nx,ny,nz);
    }

    if (_debug) Log.d(TAG, "end onCommand");
  }


  public synchronized void onDrawFrame(GL10 gl10) {
    setupFog(gl10);
    gl10.glMatrixMode(GL10.GL_MODELVIEW);
    gl10.glPushMatrix(); 

    // 画面をクリアする
    gl10.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT | GL10.GL_STENCIL_BUFFER_BIT);

    // モデルの位置を決める
    gl10.glMatrixMode(GL10.GL_MODELVIEW);
    gl10.glLoadIdentity();
    gl10.glDisable(GL10.GL_STENCIL_TEST);


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

    gl10.glDisable(GL10.GL_STENCIL_TEST);
    gl10.glDisable(GL10.GL_DEPTH_TEST);

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
    synchronized (this) {
      wave.draw(gl10);
      //ステンシルテストの有効化
      gl10.glColorMask(false,false,false,false);
      gl10.glDepthMask(false);
      gl10.glEnable(GL10.GL_STENCIL_TEST);

      gl10.glStencilOp(GL10.GL_REPLACE,GL10.GL_REPLACE,GL10.GL_REPLACE);
      gl10.glStencilFunc(GL10.GL_ALWAYS,1,~0);
      wave.draw(gl10);
      {
        gl10.glPushMatrix();
        gl10.glStencilOp(GL10.GL_KEEP, GL10.GL_KEEP, GL10.GL_INCR);
        gl10.glStencilFunc(GL10.GL_EQUAL, 1, ~0);
        gl10.glRotatef(-baseAngle, 0.0f, 1.0f, 0.0f);
        wave.drawForStencil(gl10);
        gl10.glPopMatrix();
      }
      gl10.glColorMask(true,true,true,true);
      gl10.glDepthMask(true);
      gl10.glStencilOp(GL10.GL_KEEP, GL10.GL_KEEP, GL10.GL_KEEP);
      gl10.glStencilFunc(GL10.GL_EQUAL, 2, ~0);
      gl10.glPushMatrix();
      gl10.glScalef(1.0f,-1.0f,1.0f);
      gl10.glTranslatef(0f,-Aquarium.max_y+Aquarium.min_y-0.5f,0f);
      gl10.glPushMatrix();

      {
        setupFog2(gl10);
        for (int ii=0; ii<iwashi_count; ii++) {
          if (iwashi[ii].getY() >= Aquarium.max_y / 3.0f * 2.0f) {
            iwashi[ii].draw(gl10);
          }
        }
        setupFog(gl10);
      }
      gl10.glPopMatrix();
      gl10.glPopMatrix();

      gl10.glDisable(GL10.GL_STENCIL_TEST);
    }
    gl10.glEnable(GL10.GL_DEPTH_TEST);
    synchronized (this) {
      for (int ii=0; ii<iwashi_count; ii++) {
        iwashi[ii].draw(gl10);
      }
    }
    gl10.glDisable(GL10.GL_DEPTH_TEST);
    gl10.glPopMatrix(); 
            
    gl10.glPopMatrix();
  }

  public void onDestroy() {
  }
}
