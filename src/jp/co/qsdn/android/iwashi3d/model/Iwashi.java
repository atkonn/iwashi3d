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
package jp.co.qsdn.android.iwashi3d.model;

import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.opengl.GLUtils;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import java.util.Random;

import javax.microedition.khronos.opengles.GL10;

import jp.co.qsdn.android.iwashi3d.Aquarium;
import jp.co.qsdn.android.iwashi3d.Bait;
import jp.co.qsdn.android.iwashi3d.BaitManager;
import jp.co.qsdn.android.iwashi3d.util.CoordUtil;

public class Iwashi implements Model {
  private static final boolean traceBOIDS = false;
  private static final boolean debug = false;
  private static final String TAG = Iwashi.class.getName();
  private static final long BASE_TICK = 17852783L;
  private static boolean mTextureLoaded = false;
  private final FloatBuffer mTextureBuffer;  
  private final FloatBuffer mNormalBuffer;  
  private long prevTime = 0;
  private long tick = 0;
  public static float scale = 0.1035156288414f;
  public static float center_xyz[] = {-0.185271816326531f, 0.344428326530612f, -0.00509786734693878f };
  private CoordUtil coordUtil = new CoordUtil();
  private long seed = 0;
  private BaitManager baitManager;
  private boolean enableBoids = true;
  public float[] distances = new float[100];
  private Random rand = null;
  public static final float GL_IWASHI_SCALE = 0.65f;
  private float size = 10f * scale * GL_IWASHI_SCALE;
  private int iwashiCount;
  private int finTick = 0;
  /*
   * 仲間、同種
   */
  private Iwashi[] species;
  private double separate_dist  = 5.0d * scale * (double)GL_IWASHI_SCALE;
  private double alignment_dist1= 15.0d * scale * (double)GL_IWASHI_SCALE;
  private double alignment_dist2= 35.0d * scale * (double)GL_IWASHI_SCALE;
  private double school_dist    = 70.0d * scale * (double)GL_IWASHI_SCALE;
  private double cohesion_dist  = 110.0d * scale * (double)GL_IWASHI_SCALE;
  private float[] schoolCenter = {0f,0f,0f};
  private float[] schoolDir = {0f,0f,0f};
  private int schoolCount = 0;
  private int alignmentCount = 0;

  private enum STATUS {
    TO_CENTER, /* 画面の真ん中へ向かい中 */
    TO_BAIT,   /* 餌へ向かっている最中   */
    SEPARATE,  /* 近づき過ぎたので離れる */
    ALIGNMENT, /* 整列中 */
    COHESION,  /* 近づく */
    TO_SCHOOL_CENTER,   /* 群れの真ん中へ */
    NORMAL,    /* ランダム */
  };

  /** 現在の行動中の行動 */
  private STATUS status = STATUS.NORMAL;


  private int[] mScratch128i = new int[128];
  private float[] mScratch4f = new float[4];
  private float[] mScratch4f_1 = new float[4];
  private float[] mScratch4f_2 = new float[4];
  private Iwashi[] mScratch4Iwashi = new Iwashi[4];


  /*=========================================================================*/
  /* 現在位置                                                                */
  /*=========================================================================*/
  // メモ 1.0f >= z >= -50.0fまで
  // zが0.0fのときy=1.0fが限界
  // zが0.0fのときy=-1.0fは半分土に埋まっている
  // zが-20.0fのとき、x=-5.0f, x=5.0fで半分切れる
  //
  // 水槽の大きさ（案）
  // 10.0f >= x  >= -10.0f
  // 8.0f >= y >= 0.0f
  // -50.0f > z >= 0.0f
  private float[] position = { 0.0f, 1.0f, 0.0f };
  /*=========================================================================*/
  /* 向き                                                                    */
  /*=========================================================================*/
  private float[] direction = { -1.0f, 0.0f, 0.0f};

  /* 上下 */
  private float x_angle = 0;
  /* 左右 */
  private float y_angle = 0;
  /*=========================================================================*/
  /* スピード                                                                */
  /*=========================================================================*/
  public static final float DEFAULT_SPEED = 0.01728f;
  //public static final float DEFAULT_SPEED = 0.0001728f;
  private float speed = DEFAULT_SPEED * 0.5f;
  private float speed_unit = DEFAULT_SPEED / 5f * 0.5f;
  private float speed_max = DEFAULT_SPEED * 3f * 0.5f;
  private float speed_min = speed_unit;
  private float cohesion_speed = speed * 5f * 0.5f;
  private float sv_speed = speed;

  private int iwashiNo = 0;

  public Iwashi(int ii) {

    ByteBuffer nbb = ByteBuffer.allocateDirect(IwashiData.normals.length * 4);
    nbb.order(ByteOrder.nativeOrder());
    mNormalBuffer = nbb.asFloatBuffer();
    mNormalBuffer.put(IwashiData.normals);
    mNormalBuffer.position(0);

    ByteBuffer tbb = ByteBuffer.allocateDirect(IwashiData.texCoords.length * 4);
    tbb.order(ByteOrder.nativeOrder());
    mTextureBuffer = tbb.asFloatBuffer();
    mTextureBuffer.put(IwashiData.texCoords);
    mTextureBuffer.position(0);


    // 初期配置
    this.rand = new java.util.Random(System.nanoTime() + (ii * 500));
    this.seed = (long)(this.rand.nextFloat() * 5000f);
    position[0] = this.rand.nextFloat() * 8f - 4f;
    position[1] = this.rand.nextFloat() * 8f - 4f;
    position[2] = this.rand.nextFloat() * 4f - 2f;

    // 初期方向セット
    x_angle = rand.nextFloat() * 45f - 22.5f;
    y_angle = rand.nextFloat() * 360f;
    coordUtil.setMatrixRotateZ(x_angle);
    synchronized (mScratch4f_1) {
      synchronized (mScratch4f_2) {
        coordUtil.affine(-1.0f,0.0f, 0.0f, mScratch4f_1);
        coordUtil.setMatrixRotateY(y_angle);
        coordUtil.affine(mScratch4f_1[0],mScratch4f_1[1], mScratch4f_1[2], mScratch4f_2);
        direction[0] = mScratch4f_2[0];
        direction[1] = mScratch4f_2[1];
        direction[2] = mScratch4f_2[2];
      }
    }
    // 鰯番号セット
    iwashiNo = ii;

    finTick = (int) (this.rand.nextFloat() * 1000 + iwashiNo);
  }

  protected static int[] textureIds = null;
  public static void loadTexture(GL10 gl10, Context context, int resource) {
    textureIds = new int[1];
    Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), resource);
    gl10.glGenTextures(1, textureIds, 0);
    gl10.glBindTexture(GL10.GL_TEXTURE_2D, textureIds[0]);
    GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);
    gl10.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
    gl10.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
    bmp.recycle();
    bmp = null;
    mTextureLoaded = true;
  }
  public static void deleteTexture(GL10 gl10) {
    if (textureIds != null) {
      gl10.glDeleteTextures(1, textureIds, 0);
    }
  }
  public static boolean isTextureLoaded() {
    return mTextureLoaded;
  }

  public void calc(long tickCounter) {
    synchronized (this) {
      think(tickCounter);
      move();
    }
  }

  public void draw(GL10 gl10) {
    gl10.glPushMatrix();


    gl10.glPushMatrix();
    {
      /*=======================================================================*/
      /* 環境光の材質色設定                                                    */
      /*=======================================================================*/
      synchronized (mScratch4f) {
        mScratch4f[0] = 0.07f;
        mScratch4f[1] = 0.07f;
        mScratch4f[2] = 0.07f;
        mScratch4f[3] = 1.0f;
        gl10.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, mScratch4f, 0);
      }
      /*=======================================================================*/
      /* 拡散反射光の色設定                                                    */
      /*=======================================================================*/
      synchronized (mScratch4f) {
        mScratch4f[0] = 0.24f;
        mScratch4f[1] = 0.24f;
        mScratch4f[2] = 0.24f;
        mScratch4f[3] = 1.0f;
        gl10.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, mScratch4f, 0);
      }
      /*=======================================================================*/
      /* 鏡面反射光の質感色設定                                                */
      /*=======================================================================*/
      synchronized (mScratch4f) {
        mScratch4f[0] = 1.0f;
        mScratch4f[1] = 1.0f;
        mScratch4f[2] = 1.0f;
        mScratch4f[3] = 1.0f;
        gl10.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, mScratch4f, 0);
      }
      gl10.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, 64f);
    }
    gl10.glTranslatef(getX(),getY(),getZ());
    gl10.glScalef(GL_IWASHI_SCALE,GL_IWASHI_SCALE,GL_IWASHI_SCALE);

    gl10.glRotatef(y_angle, 0.0f, 1.0f, 0.0f);
    gl10.glRotatef(x_angle * -1f, 0.0f, 0.0f, 1.0f);

    gl10.glColor4f(1,1,1,1);
    gl10.glVertexPointer(3, GL10.GL_FLOAT, 0, IwashiData.mVertexBuffer[Math.abs((finTick++ / 2) % 40)]);
    gl10.glNormalPointer(GL10.GL_FLOAT, 0, mNormalBuffer);
    gl10.glEnable(GL10.GL_TEXTURE_2D);
    gl10.glBindTexture(GL10.GL_TEXTURE_2D, textureIds[0]);
    gl10.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTextureBuffer);
    gl10.glDrawArrays(GL10.GL_TRIANGLES, 0, IwashiData.iwashiNumVerts);

    gl10.glPopMatrix();
    gl10.glPopMatrix();
  }

  public boolean doSeparation(Iwashi target) {
    /*===================================================================*/
    /* セパレーション領域にターゲットがいる場合                          */
    /*===================================================================*/
    setStatus(STATUS.SEPARATE);
    turnSeparation(target);
    return true;
  }
  public boolean doAlignment1(Iwashi target) {
    return _doAlignment(target, 2000);
  }
  public boolean doAlignment2(Iwashi target) {
    return _doAlignment(target, 4000);
  }
  public boolean _doAlignment(Iwashi target, int per) {
    if (this.alignmentCount < 3) {
      /* 3匹以上群れてなければ高確率でCohesion/Schoolへ */
      per = 9000; 
    }
    per = adjustTick(per);
    if (this.rand.nextInt(10000) <= per) {
      return false;
    }
    /*===================================================================*/
    /* アラインメント領域にターゲットがいる場合                          */
    /*===================================================================*/
    if (debug) {
      if (iwashiNo == 0) {
        Log.d(TAG, "doAlignment");
      }
    }
    setStatus(STATUS.ALIGNMENT);
    turnAlignment(target);
    return true;
  }
  public int adjustTick(int val) {
    return val;
  }
  public boolean doCohesion(Iwashi target) {
    /*===================================================================*/
    /* 鰯は結構な確率でCohesionするものと思われる                        */
    /*===================================================================*/
    if (getStatus() == STATUS.COHESION) {
      if (this.rand.nextInt(10000) <= adjustTick(500)) {
        /*===============================================================*/
        /* 前回COHESIONである場合今回もCOHESIONである可能性は高い        */
        /*===============================================================*/
        return false;
      }
    }
    else {
      if (this.rand.nextInt(10000) <= adjustTick(1000)) {
        return false;
      }
    }
    /*===================================================================*/
    /* コアージョン領域にターゲットがいる場合                            */
    /*===================================================================*/
    setStatus(STATUS.COHESION);
    turnCohesion(target);
    return true;
  }
  public boolean doSchoolCenter() {
    if (this.rand.nextInt(10000) <= adjustTick(3000)) {
      return false;
    }
    setStatus(STATUS.TO_SCHOOL_CENTER);
    aimSchoolCenter();
    return true;
  }
  public void update_speed() {
    sv_speed = speed;
    if (getStatus() == STATUS.COHESION || getStatus() == STATUS.TO_SCHOOL_CENTER || getStatus() == STATUS.TO_BAIT) {
      speed = cohesion_speed;
      return;
    }
    speed = sv_speed;

    if (this.rand.nextInt(10000) <= adjustTick(1000)) {
      // 変更なし
      return;
    }
    speed += (this.rand.nextFloat() * (speed_unit * 2f) / 2f);
    if (speed <= speed_min) {
      speed = speed_min;
    }
    if (speed > speed_max) {
      speed = speed_max;
    }
  }

  /** 
   * もっとも近い鰯を返す
   */
  public Iwashi[] getTarget() {
    float targetDistanceS = 10000f;
    float targetDistanceA1 = 10000f;
    float targetDistanceA2 = 10000f;
    float targetDistanceC = 10000f;
    int targetS = 9999;
    int targetA1 = 9999;
    int targetA2 = 9999;
    int targetC = 9999;
    /* alignment数をカウント */
    this.alignmentCount = 0;
    this.schoolCount = 0;
    this.schoolCenter[0] = 0f;
    this.schoolCenter[1] = 0f;
    this.schoolCenter[2] = 0f;
    this.schoolDir[0] = 0f;
    this.schoolDir[1] = 0f;
    this.schoolDir[2] = 0f;
    for (int ii=0; ii<species.length; ii++) {
      float dist = 0f;
      if (ii < iwashiNo) {
        dist = species[ii].distances[iwashiNo];
      }
      else if (ii == iwashiNo) {
        continue;
      }
      else {
        dist = (float)Math.sqrt(
            Math.pow(getX()-species[ii].getX(), 2)
          + Math.pow(getY()-species[ii].getY(), 2)
          + Math.pow(getZ()-species[ii].getZ(), 2));
      }
      this.distances[ii] = dist;
      if (dist < separate_dist) {
        if (targetDistanceS > dist) {
          targetDistanceS = dist;
          targetS = ii;
        }
        continue;
      }
      if (dist < alignment_dist1) {
        {
          /* alignmentの位置にいれば、それだけでカウント */
          this.alignmentCount++;
          this.schoolCount++;
          schoolCenter[0] += species[ii].getX();
          schoolCenter[1] += species[ii].getY();
          schoolCenter[2] += species[ii].getZ();
          schoolDir[0] += species[ii].getDirectionX();
          schoolDir[1] += species[ii].getDirectionY();
          schoolDir[2] += species[ii].getDirectionZ();
        }
        if (targetDistanceA1 > dist) {
          synchronized (mScratch4f_1) {
            synchronized (mScratch4f_2) {
              mScratch4f_1[0] = getDirectionX();
              mScratch4f_1[1] = getDirectionY();
              mScratch4f_1[2] = getDirectionZ();
              mScratch4f_2[0] = species[ii].getX() - getX();
              mScratch4f_2[1] = species[ii].getY() - getY();
              mScratch4f_2[2] = species[ii].getZ() - getZ();
              float degree = CoordUtil.includedAngle(mScratch4f_1, mScratch4f_2, 3);
              if (degree <= 150f && degree >= 0f) {
                targetDistanceA1 = dist;
                targetA1 = ii;
              }
            }
          }
        }
        continue;
      }
      if (dist < alignment_dist2) {
        {
          /* alignmentの位置にいれば、それだけでカウント */
          this.alignmentCount++;
          this.schoolCount++;
          schoolCenter[0] += species[ii].getX();;
          schoolCenter[1] += species[ii].getY();;
          schoolCenter[2] += species[ii].getZ();;
          schoolDir[0] += species[ii].getDirectionX();
          schoolDir[1] += species[ii].getDirectionY();
          schoolDir[2] += species[ii].getDirectionZ();
        }
        if (targetDistanceA2 > dist) {
          synchronized (mScratch4f_1) {
            synchronized (mScratch4f_2) {
              mScratch4f_1[0] = getDirectionX();
              mScratch4f_1[1] = getDirectionY();
              mScratch4f_1[2] = getDirectionZ();
              mScratch4f_2[0] = species[ii].getX() - getX();
              mScratch4f_2[1] = species[ii].getY() - getY();
              mScratch4f_2[2] = species[ii].getZ() - getZ();
              float degree = CoordUtil.includedAngle(mScratch4f_1, mScratch4f_2, 3);
              if (degree <= 150f && degree >= 0f) {
                targetDistanceA2 = dist;
                targetA2 = ii;
              }
            }
          }
        }
        continue;
      }
      if (dist < cohesion_dist) {
        if (dist < school_dist) {
          this.schoolCount++;
          schoolCenter[0] += species[ii].getX();;
          schoolCenter[1] += species[ii].getY();;
          schoolCenter[2] += species[ii].getZ();;
          schoolDir[0] += species[ii].getDirectionX();
          schoolDir[1] += species[ii].getDirectionY();
          schoolDir[2] += species[ii].getDirectionZ();
        }
        if (targetDistanceC > dist) {
          synchronized (mScratch4f_1) {
            synchronized (mScratch4f_2) {
              mScratch4f_1[0] = getDirectionX();
              mScratch4f_1[1] = getDirectionY();
              mScratch4f_1[2] = getDirectionZ();
              mScratch4f_2[0] = species[ii].getX() - getX();
              mScratch4f_2[1] = species[ii].getY() - getY();
              mScratch4f_2[2] = species[ii].getZ() - getZ();
              float degree = CoordUtil.includedAngle(mScratch4f_1, mScratch4f_2, 3);
              if (degree <= 150f && degree >= 0f) {
                targetDistanceC = dist;
                targetC = ii;
              }
            }
          }
        }
      }
    }
    if (schoolCount != 0) {
      schoolCenter[0] = schoolCenter[0] / (float)schoolCount;
      schoolCenter[1] = schoolCenter[1] / (float)schoolCount;
      schoolCenter[2] = schoolCenter[2] / (float)schoolCount;
      schoolDir[0] /= (float)schoolCount;
      schoolDir[1] /= (float)schoolCount;
      schoolDir[2] /= (float)schoolCount;
      CoordUtil.normalize3fv(schoolDir);
    }
    if (targetS != 9999) {
      mScratch4Iwashi[0] = species[targetS];
    }
    else {
      mScratch4Iwashi[0] = null;
    }
    if (targetA1 != 9999) {
      mScratch4Iwashi[1] = species[targetA1];
    }
    else {
      mScratch4Iwashi[1] = null;
    }
    if (targetA2 != 9999) {
      mScratch4Iwashi[2] = species[targetA2];
    }
    else {
      mScratch4Iwashi[2] = null;
    }
    if (targetC != 9999) {
      mScratch4Iwashi[3] = species[targetC];
    }
    else {
      mScratch4Iwashi[3] = null;
    }
    return mScratch4Iwashi;
  }
  /**
   * どの方向に進むか考える
   */
  public void think(long tickCounter) {
    long nowTime = System.nanoTime();
    if (prevTime != 0) {
      tick = nowTime - prevTime;
    }
    if (getStatus() == STATUS.COHESION || getStatus() == STATUS.TO_SCHOOL_CENTER || getStatus() == STATUS.TO_BAIT) {
      /* 元に戻す */
      speed = sv_speed;
    }
    prevTime = nowTime;
    if (  (Aquarium.min_x.floatValue() >= position[0] || Aquarium.max_x.floatValue() <= position[0])
      ||  (Aquarium.min_y.floatValue() >= position[1] || Aquarium.max_y.floatValue() <= position[1])
      ||  (Aquarium.min_z.floatValue() >= position[2] || Aquarium.max_z.floatValue() <= position[2])) {
      /*=====================================================================*/
      /* 水槽からはみ出てる                                                  */
      /*=====================================================================*/
      setStatus(STATUS.TO_CENTER);
      aimAquariumCenter();
      if (traceBOIDS && iwashiNo == 0) Log.d(TAG, "to Aquarium Center");
      update_speed();
      return;
    }
    /**
     * 餌ロジック
     */
    Bait bait = baitManager.getBait();
    if (bait != null) {
      if (this.rand.nextInt(10000) <= adjustTick(5500)) {
        if (aimBait(bait)) {
          if (traceBOIDS && iwashiNo == 0) Log.d(TAG, "to Bait");
          setStatus(STATUS.TO_BAIT);
          update_speed();
          return;
        }
      }
    }

    if (getEnableBoids()) {
      /**
       * １　セパレーション（Separation）：分離
       *  　　→仲間に近づきすぎたら離れる
       * ２　アラインメント（Alignment）：整列
       *  　　→仲間と同じ方向に同じ速度で飛ぶ
       * ３　コアージョン（Cohesion）：凝集
       *  　　→仲間の中心方向に飛ぶ
       */
      // separation
      Iwashi[] target = getTarget();
      if (target[0] != null) {
        if (doSeparation(target[0])) {
          if (traceBOIDS && iwashiNo == 0) Log.d(TAG, "Separate");
          update_speed();
          target[0] = null;
          target[1] = null;
          target[2] = null;
          target[3] = null;
          return;
        }
      }
      if (target[1] != null) {
        // alignment
        if (doAlignment1(target[1])) {
          if (traceBOIDS && iwashiNo == 0) Log.d(TAG, "Alignment-1");
          target[0] = null;
          target[1] = null;
          target[2] = null;
          target[3] = null;
          return;
        }
      }
      if (target[2] != null) {
        // alignment
        if (doAlignment2(target[2])) {
          if (traceBOIDS && iwashiNo == 0) Log.d(TAG, "Alignment-2");
          target[0] = null;
          target[1] = null;
          target[2] = null;
          target[3] = null;
          return;
        }
      }
      if (schoolCount >= (iwashiCount / 4)) {
        if (doSchoolCenter()) {
          if (traceBOIDS && iwashiNo == 0) Log.d(TAG, "Cohesion(to School)");
          update_speed();
          target[0] = null;
          target[1] = null;
          target[2] = null;
          target[3] = null;
          return;
        }
      }
      if (target[3] != null) {
        // cohesion
        if (doCohesion(target[3])) {
          if (traceBOIDS && iwashiNo == 0) Log.d(TAG, "Cohesion(normal)");
          update_speed();
          target[0] = null;
          target[1] = null;
          target[2] = null;
          target[3] = null;
          return;
        }
      }
      target[0] = null;
      target[1] = null;
      target[2] = null;
      target[3] = null;
    }

    if (this.rand.nextInt(10000) <= adjustTick(9500)) {
      if (traceBOIDS && iwashiNo == 0) Log.d(TAG, "Nop");
      // 変更なし
      return;
    }
    setStatus(STATUS.NORMAL);
    turn();
    if (traceBOIDS && iwashiNo == 0) Log.d(TAG, "Normal");
    update_speed();
  }


  public void turn() {
    // 方向転換
    // 45 >= x >= -45
    // 360 >= y >= 0
    // 一回の方向転換のMAX
    // 45 >= x >= -45
    // 45 >= y >= -45
    float old_angle_x = x_angle;
    float old_angle_y = y_angle;
    x_angle = old_angle_x;
    y_angle = old_angle_y;
    float newAngleX = this.rand.nextFloat() * 45f - 22.5f;
    float newAngleY = this.rand.nextFloat() * 45f - 22.5f;
    if (newAngleX + x_angle <= 45f && newAngleX + x_angle >= -45f) {
      x_angle = x_angle + newAngleX;
    } 
    else {
      if (newAngleX + x_angle >= 45f) {
        x_angle = (this.rand.nextFloat() * 45f);
      }
      else if (newAngleX + x_angle <= -45f) {
        x_angle = (this.rand.nextFloat() * -45f);
      }
    }
    y_angle = (float)((int)(y_angle + newAngleY) % 360);
    coordUtil.setMatrixRotateZ(x_angle);
    synchronized (mScratch4f_1) {
      synchronized (mScratch4f_2) {
        coordUtil.affine(-1.0f,0.0f, 0.0f, mScratch4f_1);
        coordUtil.setMatrixRotateY(y_angle);
        coordUtil.affine(mScratch4f_1[0],mScratch4f_1[1], mScratch4f_1[2], mScratch4f_2);
        direction[0] = mScratch4f_2[0];
        direction[1] = mScratch4f_2[1];
        direction[2] = mScratch4f_2[2];
      }
    }
  }
  public void aimTargetDegree(float angle_x, float angle_y) {
    float newAngle = this.rand.nextFloat() * 22.5f;
    float xx = angle_x - x_angle;
    if (xx < 0.0f) {
      if (xx > -22.5f) {
        x_angle += xx;
      }
      else {
        x_angle += -newAngle;
      }
    }
    else {
      if (xx < 22.5f) {
        x_angle += xx;
      }
      else {
        x_angle += newAngle;
      }
    }
    if (x_angle > 45.0f) {
      x_angle = 45.0f;
    }
    if (x_angle < -45.0f) {
      x_angle = -45.0f;
    }

    float yy = angle_y - y_angle;
    if (yy > 180.0f) {
      yy = -360f + yy;
    }
    else if (yy < -180.0f) {
      yy = 360f - yy;
    }

    if (yy < 0.0f) {
      if (yy > -22.5f) {
        y_angle += yy;
      }
      else {
        y_angle += -newAngle;
      }
    }
    else {
      if (yy < 22.5f) {
        y_angle += yy;
      }
      else {
        y_angle += newAngle;
      }
    }
    y_angle = y_angle % 360f;
    if (y_angle < 0f) {
      y_angle = 360f + y_angle;
    }
  }
  public void aimTargetSpeed(float t_speed) {
    if (t_speed <= speed) {
      /* 自分のスピードよりも相手の方が遅い場合 */
      if (false) {
        speed -= (this.rand.nextFloat() * speed_unit);
        if (speed <= speed_min) {
          speed = speed_unit;
        }
      }
      else {
       update_speed();
      }
    }
    else {
      /* 相手の方が早い場合 */
      speed += (this.rand.nextFloat() * speed_unit);
      if (t_speed < speed) {
        /* 越えちゃったらちょっとだけ遅く*/
        speed = t_speed - (this.rand.nextFloat() * speed_unit);
      }
      if (speed > speed_max) {
        speed = speed_max;
      }
    }
  }
  public void turnSeparation(Iwashi target) {
    if (debug) { Log.d(TAG, "start turnSeparation"); }
    float v_x = 0f;
    float v_y = 0f;
    float v_z = 0f;
    synchronized (mScratch4f_1) {
      /*=======================================================================*/
      /* Separationしたいターゲットの方向取得                                  */
      /*=======================================================================*/
      mScratch4f_1[0] = target.getDirectionX();
      mScratch4f_1[1] = target.getDirectionY();
      mScratch4f_1[2] = target.getDirectionZ();
      CoordUtil.normalize3fv(mScratch4f_1);
      synchronized (mScratch4f_2) {
        /*=====================================================================*/
        /* ターゲットから見て、自分の方向を算出                                */
        /*=====================================================================*/
        mScratch4f_2[0] = getX() - target.getX();
        mScratch4f_2[1] = getY() - target.getY();
        mScratch4f_2[2] = getZ() - target.getZ();
        CoordUtil.normalize3fv(mScratch4f_2);
        /*=====================================================================*/
        /* ややターゲットの方向に沿いたいので、x2                              */
        /*=====================================================================*/
        mScratch4f_1[0] *= 2f;
        mScratch4f_1[1] *= 2f;
        mScratch4f_1[2] *= 2f;
        /*=====================================================================*/
        /* 足し込む                                                            */
        /*=====================================================================*/
        mScratch4f_1[0] += mScratch4f_2[0];
        mScratch4f_1[1] += mScratch4f_2[1];
        mScratch4f_1[2] += mScratch4f_2[2];
      }
      /*=====================================================================*/
      /* 平均算出                                                            */
      /*=====================================================================*/
      mScratch4f_1[0] /= 3f;
      mScratch4f_1[1] /= 3f;
      mScratch4f_1[2] /= 3f;

      v_x = mScratch4f_1[0];
      v_y = mScratch4f_1[1];
      v_z = mScratch4f_1[2];
    }
    if (debug) {
      Log.d(TAG, "向かいたい方向"
       + " x:[" + v_x + "]:"
       + " y:[" + v_y + "]:"
       + " z:[" + v_z + "]:");
    }

    /* 上下角度算出 (-1dを乗算しているのは0度の向きが違うため) */
    float angle_x = (float)coordUtil.convertDegreeXY((double)v_x, (double)v_y);
    /* 左右角度算出 (-1dを乗算しているのは0度の向きが違うため) */
    float angle_y = (float)coordUtil.convertDegreeXZ((double)v_x * -1d, (double)v_z);
    if (angle_x > 180f) {
      angle_x = angle_x - 360f;
    }
    if ((angle_x < 0.0f && v_y > 0.0f) || (angle_x > 0.0f && v_y < 0.0f)) {
      angle_x *= -1f;
    }
    if (debug) {
      Log.d(TAG, "向かいたい方向のangle_y:[" + angle_y + "]");
      Log.d(TAG, "向かいたい方向のangle_x:[" + angle_x + "]");
    }

    /* その角度へ近づける */
    aimTargetDegree(angle_x, angle_y);
    if (debug) {
      Log.d(TAG, "実際に向かう方向のy_angle:[" + y_angle + "]");
      Log.d(TAG, "実際に向かう方向のx_angle:[" + x_angle + "]");
    }

    /* direction設定 */
    coordUtil.setMatrixRotateZ(x_angle);
    synchronized (mScratch4f_1) {
      synchronized (mScratch4f_2) {
        coordUtil.affine(-1.0f,0.0f, 0.0f, mScratch4f_1);
        coordUtil.setMatrixRotateY(y_angle);
        coordUtil.affine(mScratch4f_1[0],mScratch4f_1[1], mScratch4f_1[2], mScratch4f_2);
        direction[0] = mScratch4f_2[0];
        direction[1] = mScratch4f_2[1];
        direction[2] = mScratch4f_2[2];
      }
    }
    if (debug) {
      Log.d(TAG, "結果的に向かう方向"
       + " x:[" + direction[0] + "]:"
       + " y:[" + direction[1] + "]:"
       + " z:[" + direction[2] + "]:");
      Log.d(TAG, "end turnSeparation");
    }
  }
  public void turnAlignment(Iwashi target) {
    if (debug) {
      Log.d(TAG, "start turnAlignment");
    }
    /* ターゲットの角度 */
    float angle_x = target.getX_angle();
    float angle_y = target.getY_angle();
    if (debug) {
      Log.d(TAG, "向かいたい方向のangle_y:[" + angle_y + "]");
      Log.d(TAG, "向かいたい方向のangle_x:[" + angle_x + "]");
    }

    /* その角度へ近づける */
    aimTargetDegree(angle_x, angle_y);
    if (debug) {
      Log.d(TAG, "実際に向かう方向のy_angle:[" + y_angle + "]");
      Log.d(TAG, "実際に向かう方向のx_angle:[" + x_angle + "]");
    }

    /* direction設定 */
    coordUtil.setMatrixRotateZ(x_angle);
    synchronized (mScratch4f_1) {
      synchronized (mScratch4f_2) {
        coordUtil.affine(-1.0f,0.0f, 0.0f, mScratch4f_1);
        coordUtil.setMatrixRotateY(y_angle);
        coordUtil.affine(mScratch4f_1[0],mScratch4f_1[1], mScratch4f_1[2], mScratch4f_2);
        direction[0] = mScratch4f_2[0];
        direction[1] = mScratch4f_2[1];
        direction[2] = mScratch4f_2[2];
      }
    }
    if (debug) {
      Log.d(TAG, "結果的に向かう方向"
       + " x:[" + direction[0] + "]:"
       + " y:[" + direction[1] + "]:"
       + " z:[" + direction[2] + "]:");
    }

    /* スピードも合わせる */
    aimTargetSpeed(target.getSpeed());

    if (debug) {
      Log.d(TAG, "end turnAlignment");
    }
  }
  public void turnCohesion(Iwashi target) {
    if (debug) { Log.d(TAG, "start turnCohesion"); }
    float v_x = 0f;
    float v_y = 0f;
    float v_z = 0f;
    synchronized (mScratch4f_1) {
      /*=======================================================================*/
      /* Separationしたいターゲットの方向取得                                  */
      /*=======================================================================*/
      mScratch4f_1[0] = target.getDirectionX();
      mScratch4f_1[1] = target.getDirectionY();
      mScratch4f_1[2] = target.getDirectionZ();
      CoordUtil.normalize3fv(mScratch4f_1);
      synchronized (mScratch4f_2) {
        /*=====================================================================*/
        /* 自分から見て、ターゲットの方向を算出                                */
        /*=====================================================================*/
        mScratch4f_2[0] = target.getX() - getX();
        mScratch4f_2[1] = target.getY() - getY();
        mScratch4f_2[2] = target.getZ() - getZ();
        CoordUtil.normalize3fv(mScratch4f_2);
        /*=====================================================================*/
        /* ややターゲットに近づきたいので x2                                   */
        /*=====================================================================*/
        mScratch4f_2[0] *= 2f;
        mScratch4f_2[1] *= 2f;
        mScratch4f_2[2] *= 2f;
        /*=====================================================================*/
        /* 足し込む                                                            */
        /*=====================================================================*/
        mScratch4f_1[0] += mScratch4f_2[0];
        mScratch4f_1[1] += mScratch4f_2[1];
        mScratch4f_1[2] += mScratch4f_2[2];
      }
      /*=====================================================================*/
      /* 平均算出                                                            */
      /*=====================================================================*/
      mScratch4f_1[0] /= 3f;
      mScratch4f_1[1] /= 3f;
      mScratch4f_1[2] /= 3f;

      v_x = mScratch4f_1[0];
      v_y = mScratch4f_1[1];
      v_z = mScratch4f_1[2];
    }
    if (debug) {
      Log.d(TAG, "向かいたい方向"
       + " x:[" + v_x + "]:"
       + " y:[" + v_y + "]:"
       + " z:[" + v_z + "]:");
    }


    /* 上下角度算出 (-1dを乗算しているのは0度の向きが違うため) */
    float angle_x = (float)coordUtil.convertDegreeXY((double)v_x, (double)v_y);
    /* 左右角度算出 (-1dを乗算しているのは0度の向きが違うため) */
    float angle_y = (float)coordUtil.convertDegreeXZ((double)v_x * -1d, (double)v_z);
    if (angle_x > 180f) {
      angle_x = angle_x - 360f;
    }
    if ((angle_x < 0.0f && v_y > 0.0f) || (angle_x > 0.0f && v_y < 0.0f)) {
      angle_x *= -1f;
    }
    if (debug) {
      Log.d(TAG, "向かいたい方向のangle_y:[" + angle_y + "]");
      Log.d(TAG, "向かいたい方向のangle_x:[" + angle_x + "]");
    }

    /* その角度へ近づける */
    aimTargetDegree(angle_x, angle_y);
    if (debug) {
      Log.d(TAG, "実際に向かう方向のy_angle:[" + y_angle + "]");
      Log.d(TAG, "実際に向かう方向のx_angle:[" + x_angle + "]");
    }

    /* direction設定 */
    coordUtil.setMatrixRotateZ(x_angle);
    synchronized (mScratch4f_1) {
      synchronized (mScratch4f_2) {
        coordUtil.affine(-1.0f,0.0f, 0.0f, mScratch4f_1);
        coordUtil.setMatrixRotateY(y_angle);
        coordUtil.affine(mScratch4f_1[0],mScratch4f_1[1], mScratch4f_1[2], mScratch4f_2);
        direction[0] = mScratch4f_2[0];
        direction[1] = mScratch4f_2[1];
        direction[2] = mScratch4f_2[2];
      }
    }
    if (debug) {
      Log.d(TAG, "結果的に向かう方向"
       + " x:[" + direction[0] + "]:"
       + " y:[" + direction[1] + "]:"
       + " z:[" + direction[2] + "]:");
      Log.d(TAG, "end turnCohesion");
    }
  }

  /**
   * 強制的に水槽の中心へ徐々に向ける
   */
  public void aimAquariumCenter() {
    if (debug) {
      Log.d(TAG, "start aimAquariumCenter ");
    }
    float v_x = (Aquarium.center[0] - getX());
    float v_y = (Aquarium.center[1] - getY());
    float v_z = (Aquarium.center[2] - getZ());
    if (Aquarium.min_x.floatValue() < getX() && Aquarium.max_x.floatValue() > getX()
    &&  Aquarium.min_y.floatValue() < getY() && Aquarium.max_y.floatValue() > getY()) {
      /* Zだけはみ出た */
      v_x = 0.0f;
      v_y = 0.0f;
    }
    else 
    if (Aquarium.min_x.floatValue() < getX() && Aquarium.max_x.floatValue() > getX()
    &&  Aquarium.min_z.floatValue() < getZ() && Aquarium.max_z.floatValue() > getZ()) {
      /* Yだけはみ出た */
      v_x = 0.0f;
      v_z = 0.0f;
    }
    else 
    if (Aquarium.min_y.floatValue() < getY() && Aquarium.max_y.floatValue() > getY()
    &&  Aquarium.min_z.floatValue() < getZ() && Aquarium.max_z.floatValue() > getZ()) {
      /* Xだけはみ出た */
      v_y = 0.0f;
      v_z = 0.0f;
    }
    /* 上下角度算出 (-1dを乗算しているのは0度の向きが違うため) */
    float angle_x = (float)coordUtil.convertDegreeXY((double)v_x, (double)v_y);
    /* 左右角度算出 (-1dを乗算しているのは0度の向きが違うため) */
    float angle_y = (float)coordUtil.convertDegreeXZ((double)v_x * -1d, (double)v_z);
    if (angle_x > 180f) {
      angle_x = angle_x - 360f;
    }
    if ((angle_x < 0.0f && v_y > 0.0f) || (angle_x > 0.0f && v_y < 0.0f)) {
      angle_x *= -1f;
    }
    if (debug) {
      Log.d(TAG, "向かいたい方向のangle_y:[" + angle_y + "]");
      Log.d(TAG, "向かいたい方向のangle_x:[" + angle_x + "]");
    }

    if (angle_y < 0.0f) {
      angle_y = 360f + angle_y;
    }
    angle_y = angle_y % 360f;

    /* その角度へ近づける */
    aimTargetDegree(angle_x, angle_y);
    if (debug) {
      Log.d(TAG, "実際に向かう方向のy_angle:[" + y_angle + "]");
      Log.d(TAG, "実際に向かう方向のx_angle:[" + x_angle + "]");
    }

    coordUtil.setMatrixRotateZ(x_angle);
    synchronized (mScratch4f_1) {
      synchronized (mScratch4f_2) {
        coordUtil.affine(-1.0f,0.0f, 0.0f, mScratch4f_1);
        coordUtil.setMatrixRotateY(y_angle);
        coordUtil.affine(mScratch4f_1[0],mScratch4f_1[1], mScratch4f_1[2], mScratch4f_2);
        direction[0] = mScratch4f_2[0];
        direction[1] = mScratch4f_2[1];
        direction[2] = mScratch4f_2[2];
      }
    }
    if (debug) {
      Log.d(TAG, "end aimAquariumCenter "
        + "x:[" + direction[0] + "]:"
        + "y:[" + direction[1] + "]:"
        + "z:[" + direction[2] + "]:");
    }
  }
  public void aimSchoolCenter() {
    if (debug) {
      Log.d(TAG, "start aimSchoolCenter ");
    }

    float v_x = 0f;
    float v_y = 0f;
    float v_z = 0f;
    synchronized (mScratch4f_1) {
      /*=======================================================================*/
      /* 向かいたいschoolの方向取得                                            */
      /*=======================================================================*/
      mScratch4f_1[0] = schoolDir[0];
      mScratch4f_1[1] = schoolDir[1];
      mScratch4f_1[2] = schoolDir[2];
      CoordUtil.normalize3fv(mScratch4f_1);
      synchronized (mScratch4f_2) {
        /*=====================================================================*/
        /* 自分から見て、ターゲットの方向を算出                                */
        /*=====================================================================*/
        mScratch4f_2[0] = schoolCenter[0] - getX();
        mScratch4f_2[1] = schoolCenter[1] - getY();
        mScratch4f_2[2] = schoolCenter[2] - getZ();
        CoordUtil.normalize3fv(mScratch4f_2);
        /*=====================================================================*/
        /* ややターゲットに近づきたいので x2                                   */
        /*=====================================================================*/
        mScratch4f_2[0] *= 2f;
        mScratch4f_2[1] *= 2f;
        mScratch4f_2[2] *= 2f;
        /*=====================================================================*/
        /* 足し込む                                                            */
        /*=====================================================================*/
        mScratch4f_1[0] += mScratch4f_2[0];
        mScratch4f_1[1] += mScratch4f_2[1];
        mScratch4f_1[2] += mScratch4f_2[2];
      }
      /*=====================================================================*/
      /* 平均算出                                                            */
      /*=====================================================================*/
      mScratch4f_1[0] /= 3f;
      mScratch4f_1[1] /= 3f;
      mScratch4f_1[2] /= 3f;

      v_x = mScratch4f_1[0];
      v_y = mScratch4f_1[1];
      v_z = mScratch4f_1[2];
    }
    //float v_x = (schoolCenter[0] - getX());
    //float v_y = (schoolCenter[1] - getY());
    //float v_z = (schoolCenter[2] - getZ());

    /* 上下角度算出 (-1dを乗算しているのは0度の向きが違うため) */
    float angle_x = (float)coordUtil.convertDegreeXY((double)v_x, (double)v_y);
    /* 左右角度算出 (-1dを乗算しているのは0度の向きが違うため) */
    float angle_y = (float)coordUtil.convertDegreeXZ((double)v_x * -1d, (double)v_z);
    if (angle_x > 180f) {
      angle_x = angle_x - 360f;
    }
    if ((angle_x < 0.0f && v_y > 0.0f) || (angle_x > 0.0f && v_y < 0.0f)) {
      angle_x *= -1f;
    }
    if (debug) {
      Log.d(TAG, "向かいたい方向のangle_y:[" + angle_y + "]");
      Log.d(TAG, "向かいたい方向のangle_x:[" + angle_x + "]");
    }

    if (angle_y < 0.0f) {
      angle_y = 360f + angle_y;
    }
    angle_y = angle_y % 360f;

    /* その角度へ近づける */
    aimTargetDegree(angle_x, angle_y);
    if (debug) {
      Log.d(TAG, "実際に向かう方向のy_angle:[" + y_angle + "]");
      Log.d(TAG, "実際に向かう方向のx_angle:[" + x_angle + "]");
    }

    coordUtil.setMatrixRotateZ(x_angle);
    synchronized (mScratch4f_1) {
      synchronized (mScratch4f_2) {
        coordUtil.affine(-1.0f,0.0f, 0.0f, mScratch4f_1);
        coordUtil.setMatrixRotateY(y_angle);
        coordUtil.affine(mScratch4f_1[0],mScratch4f_1[1], mScratch4f_1[2], mScratch4f_2);
        direction[0] = mScratch4f_2[0];
        direction[1] = mScratch4f_2[1];
        direction[2] = mScratch4f_2[2];
      }
    }
    if (debug) {
      Log.d(TAG, "end aimSchoolCenter "
        + "x:[" + direction[0] + "]:"
        + "y:[" + direction[1] + "]:"
        + "z:[" + direction[2] + "]:");
    }
  }
  public boolean aimBait(Bait bait) {
    if (debug) {
      Log.d(TAG, "start aimBait ");
    }
    double dist = Math.sqrt(
        Math.pow(position[0]-bait.getX(), 2)
      + Math.pow(position[1]-bait.getY(), 2)
      + Math.pow(position[2]-bait.getZ(), 2));
    if (dist <= separate_dist) {
      baitManager.eat(bait);
      return false;
    }
    float v_x = (bait.getX() - getX());
    float v_y = (bait.getY() - getY());
    float v_z = (bait.getZ() - getZ());
    if (debug) {
      Log.d(TAG, "向かいたい方向"
       + " x:[" + v_x + "]:"
       + " y:[" + v_y + "]:"
       + " z:[" + v_z + "]:");
    }

    /* 上下角度算出 (-1dを乗算しているのは0度の向きが違うため) */
    float angle_x = (float)coordUtil.convertDegreeXY((double)v_x, (double)v_y);
    /* 左右角度算出 (-1dを乗算しているのは0度の向きが違うため) */
    float angle_y = (float)coordUtil.convertDegreeXZ((double)v_x * -1d, (double)v_z);
    if (angle_x > 180f) {
      angle_x = angle_x - 360f;
    }
    if ((angle_x < 0.0f && v_y > 0.0f) || (angle_x > 0.0f && v_y < 0.0f)) {
      angle_x *= -1f;
    }
    if (debug) {
      Log.d(TAG, "向かいたい方向のangle_y:[" + angle_y + "]");
      Log.d(TAG, "向かいたい方向のangle_x:[" + angle_x + "]");
    }

    /* その角度へ近づける */
    aimTargetDegree(angle_x, angle_y);
    if (debug) {
      Log.d(TAG, "実際に向かう方向のy_angle:[" + y_angle + "]");
      Log.d(TAG, "実際に向かう方向のx_angle:[" + x_angle + "]");
    }

    coordUtil.setMatrixRotateZ(x_angle);
    synchronized (mScratch4f_1) {
      synchronized (mScratch4f_2) {
        coordUtil.affine(-1.0f,0.0f, 0.0f, mScratch4f_1);
        coordUtil.setMatrixRotateY(y_angle);
        coordUtil.affine(mScratch4f_1[0],mScratch4f_1[1], mScratch4f_1[2], mScratch4f_2);
        direction[0] = mScratch4f_2[0];
        direction[1] = mScratch4f_2[1];
        direction[2] = mScratch4f_2[2];
      }
    }
    if (debug) {
      Log.d(TAG, "end aimBait "
        + "x:[" + direction[0] + "]:"
        + "y:[" + direction[1] + "]:"
        + "z:[" + direction[2] + "]:");
    }
    return true;
  }
  public void move() {
    /*=======================================================================*/
    /* 処理速度を考慮した増分                                                */
    /*=======================================================================*/
    float moveWidth = getSpeed() * (float)(tick / BASE_TICK);

    if (getX() + getDirectionX() * moveWidth >= Aquarium.max_x) {
      setX(Aquarium.max_x);
    }
    else if (getX() + getDirectionX() * moveWidth <= Aquarium.min_x) {
      setX(Aquarium.min_x);
    }
    else {
      setX(getX() + getDirectionX() * moveWidth);
    }
    if (getY() + getDirectionY() * moveWidth >= Aquarium.max_y) {
      setY(Aquarium.max_y);
    }
    else if (getY() + getDirectionY() * moveWidth <= Aquarium.min_y) {
      setY(Aquarium.min_y);
    }
    else {
      setY(getY() + getDirectionY() * moveWidth);
    }
    if (getZ() + getDirectionZ() * moveWidth >= Aquarium.max_z) {
      setZ(Aquarium.max_z);
    }
    else if (getZ() + getDirectionZ() * moveWidth <= Aquarium.min_z) {
      setZ(Aquarium.min_z);
    }
    else {
      setZ(getZ() + getDirectionZ() * moveWidth);
    }
    if (debug) {
      Log.d(TAG, "end move "
        + "dx:[" + getDirectionX() + "]:"
        + "dy:[" + getDirectionY() + "]:"
        + "dz:[" + getDirectionZ() + "]:"
        + "speed:[" + getSpeed() + "]:"
        + "x:[" + getX() + "]:"
        + "y:[" + getY() + "]:"
        + "z:[" + getZ() + "]:"
        + "x_angle:[" + x_angle + "]:"
        + "y_angle:[" + y_angle + "]:"
        );
    }
  }


  public float[] getPosition() {
    return position;
  }
  public void setPosition(float[] pos) {
    this.position = pos;
  }
  
  public float getX() {
    return position[0];
  }
  
  public void setX(float x) {
    this.position[0] = x;
  }
  
  public float getY() {
    return position[1];
  }
  
  public void setY(float y) {
    this.position[1] = y;
  }
  
  public float getZ() {
    return position[2];
  }
  
  public void setZ(float z) {
    this.position[2] = z;
  }

  public float getDirectionX() {
    return direction[0];
  }
  public float getDirectionY() {
    return direction[1];
  }
  public float getDirectionZ() {
    return direction[2];
  }
  public void setDirectionX(float x) {
    this.direction[0] = x;
  }
  public void setDirectionY(float y) {
    this.direction[1] = y;
  }
  public void setDirectionZ(float z) {
    this.direction[2] = z;
  }
  
  public float getSpeed() {
    return speed;
  }
  
  public void setSpeed(float speed) {
    this.speed = speed * 0.5f;
    this.speed_unit = speed / 5f * 0.5f;
    this.speed_max = speed * 3f * 0.5f;
    this.speed_min = this.speed_unit * 2f;
    this.cohesion_speed = speed * 5f * 0.5f;
    this.sv_speed = speed;
  }
  
  public float[] getDirection() {
    return direction;
  }
  
  public float getDirection(int index) {
    return direction[index];
  }
  
  public void setDirection(float[] direction) {
    this.direction = direction;
  }
  
  public void setDirection(float direction, int index) {
    this.direction[index] = direction;
  }
  
  /**
   * Get species.
   *
   * @return species as Iwashi[].
   */
  public Iwashi[] getSpecies() {
    return species;
  }
  
  /**
   * Get species element at specified index.
   *
   * @param index the index.
   * @return species at index as Iwashi.
   */
  public Iwashi getSpecies(int index) {
    return species[index];
  }
  
  /**
   * Set species.
   *
   * @param species the value to set.
   */
  public void setSpecies(Iwashi[] species) {
    this.species = species;
    for (int ii=0; ii<species.length; ii++) {
      this.distances[ii] = 10000f;
    }
  }
  
  /**
   * Set species at the specified index.
   *
   * @param species the value to set.
   * @param index the index.
   */
  public void setSpecies(Iwashi species, int index) {
    this.species[index] = species;
  }
  
  /**
   * Get x_angle.
   *
   * @return x_angle as float.
   */
  public float getX_angle()
  {
      return x_angle;
  }
  
  /**
   * Set x_angle.
   *
   * @param x_angle the value to set.
   */
  public void setX_angle(float x_angle)
  {
      this.x_angle = x_angle;
  }
  
  /**
   * Get y_angle.
   *
   * @return y_angle as float.
   */
  public float getY_angle()
  {
      return y_angle;
  }
  
  /**
   * Set y_angle.
   *
   * @param y_angle the value to set.
   */
  public void setY_angle(float y_angle)
  {
      this.y_angle = y_angle;
  }
  
  /**
   * Get schoolCenter.
   *
   * @return schoolCenter as float[].
   */
  public float[] getSchoolCenter()
  {
      return schoolCenter;
  }
  
  /**
   * Get schoolCenter element at specified index.
   *
   * @param index the index.
   * @return schoolCenter at index as float.
   */
  public float getSchoolCenter(int index)
  {
      return schoolCenter[index];
  }
  
  /**
   * Set schoolCenter.
   *
   * @param schoolCenter the value to set.
   */
  public void setSchoolCenter(float[] schoolCenter) {
      this.schoolCenter = schoolCenter;
  }
  
  /**
   * Set schoolCenter at the specified index.
   *
   * @param schoolCenter the value to set.
   * @param index the index.
   */
  public void setSchoolCenter(float schoolCenter, int index)
  {
      this.schoolCenter[index] = schoolCenter;
  }
  
  /**
   * Get baitManager.
   *
   * @return baitManager as BaitManager.
   */
  public BaitManager getBaitManager()
  {
      return baitManager;
  }
  
  /**
   * Set baitManager.
   *
   * @param baitManager the value to set.
   */
  public void setBaitManager(BaitManager baitManager)
  {
      this.baitManager = baitManager;
  }
  
  
  /**
   * Get enableBoids.
   *
   * @return enableBoids as boolean.
   */
  public boolean getEnableBoids()
  {
      return enableBoids;
  }
  
  /**
   * Set enableBoids.
   *
   * @param enableBoids the value to set.
   */
  public void setEnableBoids(boolean enableBoids)
  {
      this.enableBoids = enableBoids;
  }
  
  /**
   * Get status.
   *
   * @return status as STATUS.
   */
  public STATUS getStatus() {
    return status;
  }
  
  /**
   * Set status.
   *
   * @param status the value to set.
   */
  public void setStatus(STATUS status) {
    this.status = status;
  }
  
  /**
   * Get size.
   *
   * @return size as float.
   */
  public float getSize()
  {
      return size;
  }
  
  /**
   * Set size.
   *
   * @param size the value to set.
   */
  public void setSize(float size)
  {
      this.size = size;
  }
  
  /**
   * Get iwashiCount.
   *
   * @return iwashiCount as int.
   */
  public int getIwashiCount()
  {
      return iwashiCount;
  }
  
  /**
   * Set iwashiCount.
   *
   * @param iwashiCount the value to set.
   */
  public void setIwashiCount(int iwashiCount)
  {
      this.iwashiCount = iwashiCount;
  }
}
