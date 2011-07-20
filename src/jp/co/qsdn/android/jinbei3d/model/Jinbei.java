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
package jp.co.qsdn.android.jinbei3d.model;

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

import jp.co.qsdn.android.jinbei3d.Aquarium;
import jp.co.qsdn.android.jinbei3d.Bait;
import jp.co.qsdn.android.jinbei3d.BaitManager;
import jp.co.qsdn.android.jinbei3d.GLRenderer;
import jp.co.qsdn.android.jinbei3d.util.CoordUtil;

public class Jinbei implements Model {
  private static final boolean traceBOIDS = false;
  private static final boolean debug = false;
  private static final String TAG = Jinbei.class.getName();
  private static final long BASE_TICK = 17852783L;
  private static boolean mTextureLoaded = false;
  private final FloatBuffer mVertexBuffer;
  private final FloatBuffer mTextureBuffer;  
  private final FloatBuffer mNormalBuffer;  
  private long prevTime = 0;
  private long tick = 0;
  private static final float scale = 0.119717280459159f;
  private float center_xyz[] = {0.944553637254902f, -0.0858584215686275f, 0.00370374509803921f};
  private CoordUtil coordUtil = new CoordUtil();
  private long seed = 0;
  private BaitManager baitManager;
  private boolean enableBoids = true;
  public float[] distances = new float[GLRenderer.MAX_IWASHI_COUNT + 1];
  private Random rand = null;
  public static final float GL_JINBEI_SCALE = 8f;
  private float size = 10f * scale * GL_JINBEI_SCALE;
  private int jinbeiCount;
  /*
   * 仲間、同種
   */
  public static final double separate_dist  = 5.0d * scale * (double)GL_JINBEI_SCALE;
  private static double[] separate_dist_xyz = { 
                                    5.404d * scale * (double)GL_JINBEI_SCALE, 
                                    0.734d * scale * (double)GL_JINBEI_SCALE, 
                                    0.347d * scale * (double)GL_JINBEI_SCALE,
                                  };
  public static double[] aabb_org = {
    -separate_dist_xyz[0], -separate_dist_xyz[1], -separate_dist_xyz[2],
    separate_dist_xyz[0], separate_dist_xyz[1], separate_dist_xyz[2],
  };
  public static double[] sep_aabb = {
    0d,0d,0d,
    0d,0d,0d,
  };
  public static double[] al1_aabb = {
    0d,0d,0d,
    0d,0d,0d,
  };
  public static double[] al2_aabb = {
    0d,0d,0d,
    0d,0d,0d,
  };
  public static double[] sch_aabb = {
    0d,0d,0d,
    0d,0d,0d,
  };
  public static double[] coh_aabb = {
    0d,0d,0d,
    0d,0d,0d,
  };
  public static final double alignment_dist1= 15.0d * scale * (double)Iwashi.GL_IWASHI_SCALE;
  public static final double alignment_dist2= 35.0d * scale * (double)Iwashi.GL_IWASHI_SCALE;
  public static final double school_dist    = 70.0d * scale * (double)Iwashi.GL_IWASHI_SCALE;
  public static final double cohesion_dist  = 110.0d * scale * (double)Iwashi.GL_IWASHI_SCALE;
  private float[] schoolCenter = {0f,0f,0f};
  private float[] schoolDir = {0f,0f,0f};

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

  private enum TURN_DIRECTION {
    TURN_RIGHT, /* 右に曲がり中 */
    STRAIGHT,   /* まっすぐ */
    TURN_LEFT,  /* 左に曲がり中 */
  };

  /** 現在曲がろうとしているかどうか */
  private TURN_DIRECTION turnDirection = TURN_DIRECTION.STRAIGHT;


  private int[] mScratch128i = new int[128];
  private float[] mScratch4f = new float[4];
  private float[] mScratch4f_1 = new float[4];
  private float[] mScratch4f_2 = new float[4];
  private Jinbei[] mScratch4Jinbei = new Jinbei[4];


  /*=========================================================================*/
  /* 現在位置                                                                */
  /*=========================================================================*/
  private float[] position = { 0.0f, 1.0f, 0.0f };
  /*=========================================================================*/
  /* 向き                                                                    */
  /*=========================================================================*/
  private float[] direction = { -1.0f, 0.0f, 0.0f};

  /* 上下 */
  private float x_angle = 0;
  /* 左右 */
  private float y_angle = 0;

  /* angle for animation */
  private float angleForAnimation = 0f;
  /*=========================================================================*/
  /* スピード                                                                */
  /*=========================================================================*/
  public static final float DEFAULT_SPEED = 0.03456f;
  private float speed = DEFAULT_SPEED * 0.5f;
  private float speed_unit = DEFAULT_SPEED / 5f * 0.5f;
  private float speed_max = DEFAULT_SPEED * 3f * 0.5f;
  private float speed_min = speed_unit;
  private float cohesion_speed = speed * 5f * 0.5f;
  private float sv_speed = speed;

  private int jinbeiNo = 0;

  public Jinbei(int ii) {

    ByteBuffer nbb = ByteBuffer.allocateDirect(JinbeiData.normals.length * 4);
    nbb.order(ByteOrder.nativeOrder());
    mNormalBuffer = nbb.asFloatBuffer();
    mNormalBuffer.put(JinbeiData.normals);
    mNormalBuffer.position(0);

    ByteBuffer tbb = ByteBuffer.allocateDirect(JinbeiData.texCoords.length * 4);
    tbb.order(ByteOrder.nativeOrder());
    mTextureBuffer = tbb.asFloatBuffer();
    mTextureBuffer.put(JinbeiData.texCoords);
    mTextureBuffer.position(0);

    ByteBuffer vbb = ByteBuffer.allocateDirect(JinbeiData.vertices.length * 4);
    vbb.order(ByteOrder.nativeOrder());
    mVertexBuffer = vbb.asFloatBuffer();

    // 初期配置
    this.rand = new java.util.Random(System.nanoTime() + (ii * 500));
    this.seed = (long)(this.rand.nextFloat() * 5000f);
    position[0] = this.rand.nextFloat() * 8f - 4f;
    position[1] = 0f;
    position[2] = this.rand.nextFloat() * 4f - 2f;

    // 初期方向セット
    x_angle = 0f;
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
    jinbeiNo = ii;
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

  private float getMoveWidth(float x) {
    /*=======================================================================*/
    /* z = 1/3 * x^2 の2次関数から算出                                       */
    /*=======================================================================*/
    float xt = x / scale + center_xyz[0];
    return xt * xt / 20.0f;
  }


  private void animate() {
    long current = System.currentTimeMillis() + this.seed;
    float nf = (float)((current / 100) % 10000);
    float s = (float)Math.sin((double)nf / 6f);
    if (getTurnDirection() == TURN_DIRECTION.TURN_LEFT) {
      s += -0.2f;
    }
    else if (getTurnDirection() == TURN_DIRECTION.TURN_RIGHT) {
      s += 0.2f;
    }
    s *= scale;
    angleForAnimation = 3.0625f * (float)Math.cos((double)nf / 6f) * -1f;


    //006 002 {-2.940366, -0.693884, 0.308179}
    //012 004 {-2.943983, -0.624048, 0.435656}
    //015 005 {-2.940366, -0.693884, 0.308179}
    //024 008 {-2.943983, -0.624048, 0.435656}
    //033 011 {-2.943983, -0.624048, -0.435656}
    //039 013 {-2.940366, -0.693884, -0.308178}
    //042 014 {-2.943983, -0.624048, -0.435656}
    //051 017 {-2.940366, -0.693884, -0.308178}
    //054 018 {-2.949410, -0.624049, 0.000000}
    //057 019 {-2.940366, -0.693884, 0.308179}
    //060 020 {-2.943983, -0.624048, 0.435656}
    //063 021 {-2.949410, -0.624049, 0.000000}
    //066 022 {-2.943983, -0.624048, 0.435656}
    //069 023 {-2.946395, -0.551650, 0.289866}
    //072 024 {-2.949410, -0.624049, 0.000000}
    //075 025 {-2.946395, -0.551650, -0.289866}
    //078 026 {-2.943983, -0.624048, -0.435656}
    //081 027 {-2.949410, -0.624049, 0.000000}
    //084 028 {-2.943983, -0.624048, -0.435656}
    //087 029 {-2.940366, -0.693884, -0.308178}
    //1518 506 {-2.943983, -0.624048, -0.435656}
    //1524 508 {-2.943983, -0.624048, -0.435656}
    //1527 509 {-2.946395, -0.551650, -0.289866}
    //1536 512 {-2.946395, -0.551650, -0.289866}
    //1542 514 {-2.946395, -0.551650, -0.289866}
    //1545 515 {-2.949410, -0.624049, 0.000000}
    //1554 518 {-2.946395, -0.551650, 0.289866}
    //1560 520 {-2.949410, -0.624049, 0.000000}
    //1563 521 {-2.946395, -0.551650, 0.289866}
    //1572 524 {-2.943983, -0.624048, 0.435656}
    //1578 526 {-2.946395, -0.551650, 0.289866}
    //1581 527 {-2.943983, -0.624048, 0.435656}
    //1590 530 {-2.940366, -0.693884, 0.308179}
    //1596 532 {-2.940366, -0.693884, 0.308179}
    //1599 533 {-2.949410, -0.624049, 0.000000}
    //1608 536 {-2.940366, -0.693884, -0.308178}
    //1614 538 {-2.949410, -0.624049, 0.000000}
    //1617 539 {-2.940366, -0.693884, -0.308178}
    synchronized (mScratch128i) {
      mScratch128i[0] = 2;
      mScratch128i[1] = 4;
      mScratch128i[2] = 5;
      mScratch128i[3] = 8;
      mScratch128i[4] = 11;
      mScratch128i[5] = 13;
      mScratch128i[6] = 14;
      mScratch128i[7] = 17;
      mScratch128i[8] = 18;
      mScratch128i[9] = 19;
      mScratch128i[10] = 20;
      mScratch128i[11] = 21;
      mScratch128i[12] = 22;
      mScratch128i[13] = 23;
      mScratch128i[14] = 24;
      mScratch128i[15] = 25;
      mScratch128i[16] = 26;
      mScratch128i[17] = 27;
      mScratch128i[18] = 28;
      mScratch128i[19] = 29;
      mScratch128i[20] = 506;
      mScratch128i[21] = 508;
      mScratch128i[22] = 509;
      mScratch128i[23] = 512;
      mScratch128i[24] = 514;
      mScratch128i[25] = 515;
      mScratch128i[26] = 518;
      mScratch128i[27] = 520;
      mScratch128i[28] = 521;
      mScratch128i[29] = 524;
      mScratch128i[30] = 526;
      mScratch128i[31] = 527;
      mScratch128i[32] = 530;
      mScratch128i[33] = 532;
      mScratch128i[34] = 533;
      mScratch128i[35] = 536;
      mScratch128i[36] = 538;
      mScratch128i[37] = 539;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii; 
      for (ii=0; ii<38; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //000 000 {-2.796664, -0.733715, 0.327929}
    //003 001 {-2.807316, -0.649690, 0.532374}
    //009 003 {-2.807316, -0.649690, 0.532374}
    //018 006 {-2.807316, -0.649690, 0.532374}
    //021 007 {-2.796664, -0.537303, 0.569071}
    //027 009 {-2.796664, -0.537303, -0.569071}
    //030 010 {-2.807316, -0.649691, -0.532374}
    //036 012 {-2.807316, -0.649691, -0.532374}
    //045 015 {-2.807316, -0.649691, -0.532374}
    //048 016 {-2.796664, -0.733715, -0.327928}
    //1512 504 {-2.796664, -0.420061, -0.325690}
    //1515 505 {-2.796664, -0.537303, -0.569071}
    //1521 507 {-2.796664, -0.420061, -0.325690}
    //1530 510 {-2.796664, -0.389806, 0.000000}
    //1533 511 {-2.796664, -0.420061, -0.325690}
    //1539 513 {-2.796664, -0.389806, 0.000000}
    //1548 516 {-2.796664, -0.420061, 0.325690}
    //1551 517 {-2.796664, -0.389806, 0.000000}
    //1557 519 {-2.796664, -0.389806, 0.000000}
    //1566 522 {-2.796664, -0.537303, 0.569071}
    //1569 523 {-2.796664, -0.420061, 0.325690}
    //1575 525 {-2.796664, -0.420061, 0.325690}
    //1584 528 {-2.796664, -0.760088, 0.000000}
    //1587 529 {-2.796664, -0.733715, 0.327929}
    //1593 531 {-2.796664, -0.760088, 0.000000}
    //1602 534 {-2.796664, -0.733715, -0.327928}
    //1605 535 {-2.796664, -0.760088, 0.000000}
    //1611 537 {-2.796664, -0.760088, 0.000000}
    //1626 542 {-2.796664, -0.760088, 0.000000}
    //1632 544 {-2.796664, -0.760088, 0.000000}
    //1635 545 {-2.796664, -0.733715, -0.327928}
    //1644 548 {-2.796664, -0.760088, 0.000000}
    //1650 550 {-2.796664, -0.733715, 0.327929}
    //1653 551 {-2.796664, -0.760088, 0.000000}
    //1662 554 {-2.807316, -0.649690, 0.532374}
    //1668 556 {-2.807316, -0.649690, 0.532374}
    //1671 557 {-2.796664, -0.733715, 0.327929}
    //1680 560 {-2.796664, -0.537303, 0.569071}
    //1686 562 {-2.796664, -0.537303, 0.569071}
    //1689 563 {-2.807316, -0.649690, 0.532374}
    //1698 566 {-2.796664, -0.420061, 0.325690}
    //1704 568 {-2.796664, -0.420061, 0.325690}
    //1707 569 {-2.796664, -0.537303, 0.569071}
    //1716 572 {-2.796664, -0.420061, 0.325690}
    //1722 574 {-2.796664, -0.389806, 0.000000}
    //1725 575 {-2.796664, -0.420061, 0.325690}
    //1734 578 {-2.796664, -0.420061, -0.325690}
    //1740 580 {-2.796664, -0.420061, -0.325690}
    //1743 581 {-2.796664, -0.389806, 0.000000}
    //1752 584 {-2.796664, -0.420061, -0.325690}
    //1758 586 {-2.796664, -0.537303, -0.569071}
    //1761 587 {-2.796664, -0.420061, -0.325690}
    //1770 590 {-2.796664, -0.537303, -0.569071}
    //1776 592 {-2.807316, -0.649691, -0.532374}
    //1779 593 {-2.796664, -0.537303, -0.569071}
    //1788 596 {-2.807316, -0.649691, -0.532374}
    //1794 598 {-2.796664, -0.733715, -0.327928}
    //1797 599 {-2.807316, -0.649691, -0.532374}

    synchronized (mScratch128i) {
      mScratch128i[0] = 0;
      mScratch128i[1] = 1;
      mScratch128i[2] = 3;
      mScratch128i[3] = 6;
      mScratch128i[4] = 7;
      mScratch128i[5] = 9;
      mScratch128i[6] = 10;
      mScratch128i[7] = 12;
      mScratch128i[8] = 15;
      mScratch128i[9] = 16;
      mScratch128i[10] = 504;
      mScratch128i[11] = 505;
      mScratch128i[12] = 507;
      mScratch128i[13] = 510;
      mScratch128i[14] = 511;
      mScratch128i[15] = 513;
      mScratch128i[16] = 516;
      mScratch128i[17] = 517;
      mScratch128i[18] = 519;
      mScratch128i[19] = 522;
      mScratch128i[20] = 523;
      mScratch128i[21] = 525;
      mScratch128i[22] = 528;
      mScratch128i[23] = 529;
      mScratch128i[24] = 531;
      mScratch128i[25] = 534;
      mScratch128i[26] = 535;
      mScratch128i[27] = 537;
      mScratch128i[28] = 542;
      mScratch128i[29] = 544;
      mScratch128i[30] = 545;
      mScratch128i[31] = 548;
      mScratch128i[32] = 550;
      mScratch128i[33] = 551;
      mScratch128i[34] = 554;
      mScratch128i[35] = 556;
      mScratch128i[36] = 557;
      mScratch128i[37] = 560;
      mScratch128i[38] = 562;
      mScratch128i[39] = 563;
      mScratch128i[40] = 566;
      mScratch128i[41] = 568;
      mScratch128i[42] = 569;
      mScratch128i[43] = 572;
      mScratch128i[44] = 574;
      mScratch128i[45] = 575;
      mScratch128i[46] = 578;
      mScratch128i[47] = 580;
      mScratch128i[48] = 581;
      mScratch128i[49] = 584;
      mScratch128i[50] = 586;
      mScratch128i[51] = 587;
      mScratch128i[52] = 590;
      mScratch128i[53] = 592;
      mScratch128i[54] = 593;
      mScratch128i[55] = 596;
      mScratch128i[56] = 598;
      mScratch128i[57] = 599;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii; 
      for (ii=0; ii<58; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }

    //327 109 {-2.007910, -0.690907, -0.410588}
    //330 110 {-2.007910, -0.523013, -0.691613}
    //336 112 {-2.007910, -0.523013, -0.691613}
    //339 113 {-2.025395, -0.312963, -0.728151}
    //345 115 {-2.025395, -0.312963, -0.728151}
    //357 119 {-2.025395, -0.013043, 0.000094}
    //363 121 {-2.025395, -0.013043, 0.000094}
    //366 122 {-2.007910, -0.076778, 0.408629}
    //372 124 {-2.007910, -0.076778, 0.408629}
    //375 125 {-2.007910, -0.302522, 0.726521}
    //381 127 {-2.007910, -0.302522, 0.726521}
    //384 128 {-2.025395, -0.521707, 0.694609}
    //390 130 {-2.025395, -0.521707, 0.694609}
    //393 131 {-2.007910, -0.683544, 0.414976}
    //618 206 {-2.007910, -0.076778, 0.408629}
    //627 209 {-2.007910, -0.302522, 0.726521}
    //636 212 {-2.007910, -0.683544, 0.414976}
    //645 215 {-2.007910, -0.690907, -0.410588}
    //1620 540 {-2.007910, -0.690907, -0.410588}
    //1629 543 {-2.007910, -0.690907, -0.410588}
    //1641 547 {-2.007910, -0.683544, 0.414976}
    //1647 549 {-2.007910, -0.683544, 0.414976}
    //1656 552 {-2.007910, -0.683544, 0.414976}
    //1659 553 {-2.025395, -0.521707, 0.694609}
    //1665 555 {-2.007910, -0.683544, 0.414976}
    //1674 558 {-2.025395, -0.521707, 0.694609}
    //1677 559 {-2.007910, -0.302522, 0.726521}
    //1683 561 {-2.025395, -0.521707, 0.694609}
    //1692 564 {-2.007910, -0.302522, 0.726521}
    //1695 565 {-2.007910, -0.076778, 0.408629}
    //1701 567 {-2.007910, -0.302522, 0.726521}
    //1710 570 {-2.007910, -0.076778, 0.408629}
    //1713 571 {-2.025395, -0.013043, 0.000094}
    //1719 573 {-2.025395, -0.013043, 0.000094}
    //1728 576 {-2.025395, -0.013043, 0.000094}
    //1737 579 {-2.025395, -0.013043, 0.000094}
    //1749 583 {-2.025395, -0.312963, -0.728151}
    //1755 585 {-2.025395, -0.312963, -0.728151}
    //1764 588 {-2.025395, -0.312963, -0.728151}
    //1767 589 {-2.007910, -0.523013, -0.691613}
    //1773 591 {-2.007910, -0.523013, -0.691613}
    //1782 594 {-2.007910, -0.523013, -0.691613}
    //1785 595 {-2.007910, -0.690907, -0.410588}
    //1791 597 {-2.007910, -0.690907, -0.410588}
    synchronized (mScratch128i) {
      mScratch128i[0] = 109;
      mScratch128i[1] = 110;
      mScratch128i[2] = 112;
      mScratch128i[3] = 113;
      mScratch128i[4] = 115;
      mScratch128i[5] = 119;
      mScratch128i[6] = 121;
      mScratch128i[7] = 122;
      mScratch128i[8] = 124;
      mScratch128i[9] = 125;
      mScratch128i[10] = 127;
      mScratch128i[11] = 128;
      mScratch128i[12] = 130;
      mScratch128i[13] = 131;
      mScratch128i[14] = 206;
      mScratch128i[15] = 209;
      mScratch128i[16] = 212;
      mScratch128i[17] = 215;
      mScratch128i[18] = 540;
      mScratch128i[19] = 543;
      mScratch128i[20] = 547;
      mScratch128i[21] = 549;
      mScratch128i[22] = 552;
      mScratch128i[23] = 553;
      mScratch128i[24] = 555;
      mScratch128i[25] = 558;
      mScratch128i[26] = 559;
      mScratch128i[27] = 561;
      mScratch128i[28] = 564;
      mScratch128i[29] = 565;
      mScratch128i[30] = 567;
      mScratch128i[31] = 570;
      mScratch128i[32] = 571;
      mScratch128i[33] = 573;
      mScratch128i[34] = 576;
      mScratch128i[35] = 579;
      mScratch128i[36] = 583;
      mScratch128i[37] = 585;
      mScratch128i[38] = 588;
      mScratch128i[39] = 589;
      mScratch128i[40] = 591;
      mScratch128i[41] = 594;
      mScratch128i[42] = 595;
      mScratch128i[43] = 597;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii; 
      for (ii=0; ii<44; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }

    //348 116 {-1.996253, -0.080894, -0.415381}
    //354 118 {-1.996253, -0.080894, -0.415381}
    //600 200 {-1.996253, -0.080894, -0.415381}
    //609 203 {-1.996253, -0.080894, -0.415381}
    //1731 577 {-1.996253, -0.080894, -0.415381}
    //1746 582 {-1.996253, -0.080894, -0.415381}
    synchronized (mScratch128i) {
      mScratch128i[0] = 116;
      mScratch128i[1] = 118;
      mScratch128i[2] = 200;
      mScratch128i[3] = 203;
      mScratch128i[4] = 577;
      mScratch128i[5] = 582;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii; 
      for (ii=0; ii<6; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }

    //324 108 {-1.005245, -0.194155, -0.741758}
    //333 111 {-1.005245, -0.194155, -0.741758}
    //342 114 {-1.005245, -0.194155, -0.741758}
    //561 187 {-1.005245, -0.194155, -0.741758}
    //597 199 {-1.005245, -0.194155, -0.741758}
    //630 210 {-1.031251, -0.575919, 0.011390}
    //639 213 {-1.005245, -0.194155, -0.741758}
    //642 214 {-1.031251, -0.575919, 0.011390}
    //648 216 {-1.031251, -0.575919, 0.011390}
    //651 217 {-1.005245, -0.194155, -0.741758}
    //663 221 {-1.031251, -0.575919, 0.011390}
    //672 224 {-1.031251, -0.575919, 0.011390}
    //675 225 {-1.031251, -0.575919, 0.011390}
    //723 241 {-1.005245, -0.194155, -0.741758}
    //732 244 {-1.005245, -0.194155, -0.741758}
    //1623 541 {-1.031251, -0.575919, 0.011390}
    //1638 546 {-1.031251, -0.575919, 0.011390}
    synchronized (mScratch128i) {
      mScratch128i[0] = 108;
      mScratch128i[1] = 111;
      mScratch128i[2] = 114;
      mScratch128i[3] = 187;
      mScratch128i[4] = 199;
      mScratch128i[5] = 210;
      mScratch128i[6] = 213;
      mScratch128i[7] = 214;
      mScratch128i[8] = 216;
      mScratch128i[9] = 217;
      mScratch128i[10] = 221;
      mScratch128i[11] = 224;
      mScratch128i[12] = 225;
      mScratch128i[13] = 241;
      mScratch128i[14] = 244;
      mScratch128i[15] = 541;
      mScratch128i[16] = 546;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii; 
      for (ii=0; ii<17; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }

    //378 126 {-0.989233, -0.195721, 0.684538}
    //387 129 {-0.989233, -0.195721, 0.684538}
    //552 184 {-0.989233, -0.195721, 0.684538}
    //621 207 {-0.989233, -0.195721, 0.684538}
    //633 211 {-0.989233, -0.195721, 0.684538}
    //669 223 {-0.989233, -0.195721, 0.684538}
    //705 235 {-0.989233, -0.195721, 0.684538}
    //714 238 {-0.989233, -0.195721, 0.684538}
    synchronized (mScratch128i) {
      mScratch128i[0] = 126;
      mScratch128i[1] = 129;
      mScratch128i[2] = 184;
      mScratch128i[3] = 207;
      mScratch128i[4] = 211;
      mScratch128i[5] = 223;
      mScratch128i[6] = 235;
      mScratch128i[7] = 238;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<8; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //351 117 {-0.608894, 0.390471, 0.002347}
    //360 120 {-0.608894, 0.390471, 0.002347}
    //423 141 {-0.608894, 0.390471, 0.002347}
    //471 157 {-0.608894, 0.390471, 0.002347}
    //477 159 {-0.608894, 0.390471, 0.002347}
    //603 201 {-0.608894, 0.390471, 0.002347}
    //615 205 {-0.608894, 0.390471, 0.002347}
    synchronized (mScratch128i) {
      mScratch128i[0] = 117;
      mScratch128i[1] = 120;
      mScratch128i[2] = 141;
      mScratch128i[3] = 157;
      mScratch128i[4] = 159;
      mScratch128i[5] = 201;
      mScratch128i[6] = 205;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<7; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }

    //534 178 {-0.538050, -0.158871, 1.255698}
    //549 183 {-0.538050, -0.158871, 1.255698}
    //564 188 {-0.538050, -0.158849, -1.255534}
    //570 190 {-0.538050, -0.158849, -1.255534}
    //690 230 {-0.538050, -0.158871, 1.255698}
    //708 236 {-0.538050, -0.158871, 1.255698}
    //729 243 {-0.538050, -0.158849, -1.255534}
    //747 249 {-0.538050, -0.158849, -1.255534}
    synchronized (mScratch128i) {
      mScratch128i[0] = 178;
      mScratch128i[1] = 183;
      mScratch128i[2] = 188;
      mScratch128i[3] = 190;
      mScratch128i[4] = 230;
      mScratch128i[5] = 236;
      mScratch128i[6] = 243;
      mScratch128i[7] = 249;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<8; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //369 123 {-0.390884, 0.165959, 0.542608}
    //405 135 {-0.389607, 0.179183, -0.536572}
    //414 138 {-0.389607, 0.179183, -0.536572}
    //432 144 {-0.390884, 0.165959, 0.542608}
    //462 154 {-0.389607, 0.179183, -0.536572}
    //468 156 {-0.389607, 0.179183, -0.536572}
    //480 160 {-0.390884, 0.165959, 0.542608}
    //486 162 {-0.390884, 0.165959, 0.542608}
    //504 168 {-0.317034, -0.214291, -0.660777}
    //516 172 {-0.313114, -0.209553, 0.667733}
    //528 176 {-0.313114, -0.209553, 0.667733}
    //555 185 {-0.313114, -0.209553, 0.667733}
    //558 186 {-0.317034, -0.214291, -0.660777}
    //591 197 {-0.317034, -0.214291, -0.660777}
    //594 198 {-0.389607, 0.179183, -0.536572}
    //606 202 {-0.389607, 0.179183, -0.536572}
    //612 204 {-0.390884, 0.165959, 0.542608}
    //624 208 {-0.390884, 0.165959, 0.542608}
    //654 218 {-0.317034, -0.214291, -0.660777}
    //657 219 {-0.317034, -0.214291, -0.660777}
    //666 222 {-0.313114, -0.209553, 0.667733}
    //681 227 {-0.313114, -0.209553, 0.667733}
    //684 228 {-0.313114, -0.209553, 0.667733}
    //693 231 {-0.313114, -0.209553, 0.667733}
    //711 237 {-0.390884, 0.165959, 0.542608}
    //726 242 {-0.389607, 0.179183, -0.536572}
    //744 248 {-0.317034, -0.214291, -0.660777}
    //753 251 {-0.317034, -0.214291, -0.660777}
    synchronized (mScratch128i) {
      mScratch128i[0] = 123;
      mScratch128i[1] = 135;
      mScratch128i[2] = 138;
      mScratch128i[3] = 144;
      mScratch128i[4] = 154;
      mScratch128i[5] = 156;
      mScratch128i[6] = 160;
      mScratch128i[7] = 162;
      mScratch128i[8] = 168;
      mScratch128i[9] = 172;
      mScratch128i[10] = 176;
      mScratch128i[11] = 185;
      mScratch128i[12] = 186;
      mScratch128i[13] = 197;
      mScratch128i[14] = 198;
      mScratch128i[15] = 202;
      mScratch128i[16] = 204;
      mScratch128i[17] = 208;
      mScratch128i[18] = 218;
      mScratch128i[19] = 219;
      mScratch128i[20] = 222;
      mScratch128i[21] = 227;
      mScratch128i[22] = 228;
      mScratch128i[23] = 231;
      mScratch128i[24] = 237;
      mScratch128i[25] = 242;
      mScratch128i[26] = 248;
      mScratch128i[27] = 251;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<28; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //453 151 {-0.282687, -0.101648, -0.704677}
    //459 153 {-0.282687, -0.101648, -0.704677}
    //573 191 {-0.282687, -0.101648, -0.704677}
    //582 194 {-0.282687, -0.101648, -0.704677}
    //720 240 {-0.282687, -0.101648, -0.704677}
    //735 245 {-0.282687, -0.101648, -0.704677}
    synchronized (mScratch128i) {
      mScratch128i[0] = 151;
      mScratch128i[1] = 153;
      mScratch128i[2] = 191;
      mScratch128i[3] = 194;
      mScratch128i[4] = 240;
      mScratch128i[5] = 245;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<6; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }

    //489 163 {-0.180609, -0.093193, 0.707963}
    //495 165 {-0.180609, -0.093193, 0.707963}
    //531 177 {-0.180609, -0.093193, 0.707963}
    //540 180 {-0.180609, -0.093193, 0.707963}
    //702 234 {-0.180609, -0.093193, 0.707963}
    //717 239 {-0.180609, -0.093193, 0.707963}
    synchronized (mScratch128i) {
      mScratch128i[0] = 163;
      mScratch128i[1] = 165;
      mScratch128i[2] = 177;
      mScratch128i[3] = 180;
      mScratch128i[4] = 234;
      mScratch128i[5] = 239;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<6; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //537 179 {-0.056664, -0.154328, 1.521089}
    //543 181 {-0.056664, -0.154328, 1.521089}
    //567 189 {-0.048639, -0.152439, -1.513808}
    //579 193 {-0.048639, -0.152439, -1.513808}
    //687 229 {-0.056664, -0.154328, 1.521089}
    //699 233 {-0.056664, -0.154328, 1.521089}
    //738 246 {-0.048639, -0.152439, -1.513808}
    //750 250 {-0.048639, -0.152439, -1.513808}
    synchronized (mScratch128i) {
      mScratch128i[0] = 179;
      mScratch128i[1] = 181;
      mScratch128i[2] = 189;
      mScratch128i[3] = 193;
      mScratch128i[4] = 229;
      mScratch128i[5] = 233;
      mScratch128i[6] = 246;
      mScratch128i[7] = 250;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<8; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //402 134 {0.180873, -0.149551, -0.617728}
    //441 147 {0.180873, -0.141819, 0.620578}
    //450 150 {0.180873, -0.149551, -0.617728}
    //498 166 {0.180873, -0.141819, 0.620578}
    //525 175 {0.180873, -0.141819, 0.620578}
    //546 182 {0.180873, -0.141819, 0.620578}
    //576 192 {0.180873, -0.149551, -0.617728}
    //585 195 {0.180873, -0.149551, -0.617728}
    //696 232 {0.180873, -0.141819, 0.620578}
    //741 247 {0.180873, -0.149551, -0.617728}
    synchronized (mScratch128i) {
      mScratch128i[0] = 134;
      mScratch128i[1] = 147;
      mScratch128i[2] = 150;
      mScratch128i[3] = 166;
      mScratch128i[4] = 175;
      mScratch128i[5] = 182;
      mScratch128i[6] = 192;
      mScratch128i[7] = 195;
      mScratch128i[8] = 232;
      mScratch128i[9] = 247;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<10; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }

    //510 170 {0.485043, -0.445618, 0.016730}
    //519 173 {0.485043, -0.445618, 0.016730}
    //660 220 {0.485043, -0.445618, 0.016730}
    //678 226 {0.485043, -0.445618, 0.016730}
    //1032 344 {0.485043, -0.445618, 0.016730}
    //1131 377 {0.485043, -0.445618, 0.016730}
    synchronized (mScratch128i) {
      mScratch128i[0] = 170;
      mScratch128i[1] = 173;
      mScratch128i[2] = 220;
      mScratch128i[3] = 226;
      mScratch128i[4] = 344;
      mScratch128i[5] = 377;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<6; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //294 098 {0.617177, 0.603680, 0.021016}
    //303 101 {0.617177, 0.603680, 0.021016}
    //312 104 {0.617177, 0.603680, 0.021016}
    //321 107 {0.617177, 0.603680, 0.021016}
    //417 139 {0.617177, 0.603680, 0.021016}
    //429 143 {0.617177, 0.603680, 0.021016}
    //474 158 {0.617177, 0.603680, 0.021016}
    synchronized (mScratch128i) {
      mScratch128i[0] = 98;
      mScratch128i[1] = 101;
      mScratch128i[2] = 104;
      mScratch128i[3] = 107;
      mScratch128i[4] = 139;
      mScratch128i[5] = 143;
      mScratch128i[6] = 158;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<7; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }

    //396 132 {0.786476, 0.085024, -0.483987}
    //411 137 {0.786476, 0.085024, -0.483987}
    //435 145 {0.794358, 0.086955, 0.484280}
    //447 149 {0.794358, 0.086955, 0.484280}
    //456 152 {0.786476, 0.085024, -0.483987}
    //465 155 {0.786476, 0.085024, -0.483987}
    //492 164 {0.794358, 0.086955, 0.484280}
    //501 167 {0.794358, 0.086955, 0.484280}
    //1041 347 {0.794358, 0.086955, 0.484280}
    //1065 355 {0.786476, 0.085024, -0.483987}
    //1194 398 {0.794358, 0.086955, 0.484280}
    //1275 425 {0.786476, 0.085024, -0.483987}
    synchronized (mScratch128i) {
      mScratch128i[0] = 132;
      mScratch128i[1] = 137;
      mScratch128i[2] = 145;
      mScratch128i[3] = 149;
      mScratch128i[4] = 152;
      mScratch128i[5] = 155;
      mScratch128i[6] = 164;
      mScratch128i[7] = 167;
      mScratch128i[8] = 347;
      mScratch128i[9] = 355;
      mScratch128i[10] = 398;
      mScratch128i[11] = 425;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<12; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }

    //291 097 {0.833076, 0.494887, -0.290621}
    //315 105 {0.815952, 0.451619, 0.342204}
    //408 136 {0.833076, 0.494887, -0.290621}
    //420 140 {0.833076, 0.494887, -0.290621}
    //426 142 {0.815952, 0.451619, 0.342204}
    //438 146 {0.815952, 0.451619, 0.342204}
    //483 161 {0.815952, 0.451619, 0.342204}
    //1038 346 {0.815952, 0.451619, 0.342204}
    //1050 350 {0.815952, 0.451619, 0.342204}
    //1056 352 {0.833076, 0.494887, -0.290621}
    //1068 356 {0.833076, 0.494887, -0.290621}
    //1221 407 {0.815952, 0.451619, 0.342204}
    //1257 419 {0.833076, 0.494887, -0.290621}
    synchronized (mScratch128i) {
      mScratch128i[0] = 97;
      mScratch128i[1] = 105;
      mScratch128i[2] = 136;
      mScratch128i[3] = 140;
      mScratch128i[4] = 142;
      mScratch128i[5] = 146;
      mScratch128i[6] = 161;
      mScratch128i[7] = 346;
      mScratch128i[8] = 350;
      mScratch128i[9] = 352;
      mScratch128i[10] = 356;
      mScratch128i[11] = 407;
      mScratch128i[12] = 419;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<13; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //399 133 {0.951638, -0.242611, -0.371918}
    //507 169 {0.951638, -0.242611, -0.371918}
    //588 196 {0.951638, -0.242611, -0.371918}
    //1080 360 {0.951638, -0.242611, -0.371918}
    //1116 372 {0.951638, -0.242611, -0.371918}
    //1125 375 {0.951638, -0.242611, -0.371918}
    //1263 421 {0.951638, -0.242611, -0.371918}
    //1272 424 {0.951638, -0.242611, -0.371918}
    synchronized (mScratch128i) {
      mScratch128i[0] = 133;
      mScratch128i[1] = 169;
      mScratch128i[2] = 196;
      mScratch128i[3] = 360;
      mScratch128i[4] = 372;
      mScratch128i[5] = 375;
      mScratch128i[6] = 421;
      mScratch128i[7] = 424;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<8; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //288 096 {1.070709, 0.665553, -0.092275}
    //300 100 {1.070709, 0.665553, -0.092275}
    //306 102 {1.074250, 0.647818, 0.140303}
    //318 106 {1.074250, 0.647818, 0.140303}
    //444 148 {1.020219, -0.226907, 0.383224}
    //513 171 {1.020219, -0.226907, 0.383224}
    //522 174 {1.020219, -0.226907, 0.383224}
    //1029 343 {1.020219, -0.226907, 0.383224}
    //1047 349 {1.074250, 0.647818, 0.140303}
    //1059 353 {1.070709, 0.665553, -0.092275}
    //1149 383 {1.020219, -0.226907, 0.383224}
    //1176 392 {1.020219, -0.226907, 0.383224}
    //1185 395 {1.020219, -0.226907, 0.383224}
    //1188 396 {1.020219, -0.226907, 0.383224}
    //1197 399 {1.020219, -0.226907, 0.383224}
    //1401 467 {1.070709, 0.665553, -0.092275}
    //1404 468 {1.074250, 0.647818, 0.140303}
    synchronized (mScratch128i) {
      mScratch128i[0] = 96;
      mScratch128i[1] = 100;
      mScratch128i[2] = 102;
      mScratch128i[3] = 106;
      mScratch128i[4] = 148;
      mScratch128i[5] = 171;
      mScratch128i[6] = 174;
      mScratch128i[7] = 343;
      mScratch128i[8] = 349;
      mScratch128i[9] = 353;
      mScratch128i[10] = 383;
      mScratch128i[11] = 392;
      mScratch128i[12] = 395;
      mScratch128i[13] = 396;
      mScratch128i[14] = 399;
      mScratch128i[15] = 467;
      mScratch128i[16] = 468;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<17; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //297 099 {1.350999, 1.107983, 0.010789}
    //309 103 {1.350999, 1.107983, 0.010789}
    //1386 462 {1.350999, 1.107983, 0.010789}
    //1395 465 {1.350999, 1.107983, 0.010789}
    //1410 470 {1.350999, 1.107983, 0.010789}
    //1419 473 {1.350999, 1.107983, 0.010789}
    synchronized (mScratch128i) {
      mScratch128i[0] = 99;
      mScratch128i[1] = 103;
      mScratch128i[2] = 462;
      mScratch128i[3] = 465;
      mScratch128i[4] = 470;
      mScratch128i[5] = 473;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<6; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //1044 348 {1.606145, 0.553747, 0.161499}
    //1053 351 {1.600635, 0.590280, -0.077838}
    //1086 362 {1.627900, -0.304227, -0.261185}
    //1089 363 {1.627900, -0.304227, -0.261185}
    //1098 366 {1.627900, -0.304227, -0.261185}
    //1107 369 {1.627900, -0.304227, -0.261185}
    //1119 373 {1.627900, -0.304227, -0.261185}
    //1140 380 {1.630950, -0.314325, 0.248150}
    //1146 382 {1.630950, -0.314325, 0.248150}
    //1152 384 {1.630950, -0.314325, 0.248150}
    //1161 387 {1.630950, -0.314325, 0.248150}
    //1179 393 {1.630950, -0.314325, 0.248150}
    //1212 404 {1.606145, 0.553747, 0.161499}
    //1218 406 {1.606145, 0.553747, 0.161499}
    //1224 408 {1.606145, 0.553747, 0.161499}
    //1230 410 {1.681347, 1.038271, 0.065131}
    //1233 411 {1.600635, 0.590280, -0.077838}
    //1245 415 {1.600635, 0.590280, -0.077838}
    //1248 416 {1.681347, 1.038271, 0.065131}
    //1251 417 {1.600635, 0.590280, -0.077838}
    //1389 463 {1.681347, 1.038271, 0.065131}
    //1392 464 {1.600635, 0.590280, -0.077838}
    //1398 466 {1.600635, 0.590280, -0.077838}
    //1407 469 {1.606145, 0.553747, 0.161499}
    //1413 471 {1.606145, 0.553747, 0.161499}
    //1416 472 {1.681347, 1.038271, 0.065131}
    synchronized (mScratch128i) {
      mScratch128i[0] = 348;
      mScratch128i[1] = 351;
      mScratch128i[2] = 362;
      mScratch128i[3] = 363;
      mScratch128i[4] = 366;
      mScratch128i[5] = 369;
      mScratch128i[6] = 373;
      mScratch128i[7] = 380;
      mScratch128i[8] = 382;
      mScratch128i[9] = 384;
      mScratch128i[10] = 387;
      mScratch128i[11] = 393;
      mScratch128i[12] = 404;
      mScratch128i[13] = 406;
      mScratch128i[14] = 408;
      mScratch128i[15] = 410;
      mScratch128i[16] = 411;
      mScratch128i[17] = 415;
      mScratch128i[18] = 416;
      mScratch128i[19] = 417;
      mScratch128i[20] = 463;
      mScratch128i[21] = 464;
      mScratch128i[22] = 466;
      mScratch128i[23] = 469;
      mScratch128i[24] = 471;
      mScratch128i[25] = 472;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<26; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //1083 361 {1.873442, -0.296808, -0.284279}
    //1092 364 {1.873442, -0.296808, -0.284279}
    //1167 389 {1.841042, -0.278422, 0.268745}
    //1170 390 {1.841042, -0.278422, 0.268745}
    //1182 394 {1.841042, -0.278422, 0.268745}
    //1209 403 {1.873764, 0.557008, 0.051075}
    //1227 409 {1.873764, 0.557008, 0.051075}
    //1236 412 {1.873764, 0.557008, 0.051075}
    //1242 414 {1.873764, 0.557008, 0.051075}
    //1260 420 {1.873442, -0.296808, -0.284279}
    //1284 428 {1.873442, -0.296808, -0.284279}
    //1287 429 {1.873764, 0.557008, 0.051075}
    //1302 434 {1.873764, 0.557008, 0.051075}
    //1311 437 {1.841042, -0.278422, 0.268745}
    //1332 444 {1.873442, -0.296808, -0.284279}
    //1371 457 {1.841042, -0.278422, 0.268745}
    //1377 459 {1.873442, -0.296808, -0.284279}
    synchronized (mScratch128i) {
      mScratch128i[0] = 361;
      mScratch128i[1] = 364;
      mScratch128i[2] = 389;
      mScratch128i[3] = 390;
      mScratch128i[4] = 394;
      mScratch128i[5] = 403;
      mScratch128i[6] = 409;
      mScratch128i[7] = 412;
      mScratch128i[8] = 414;
      mScratch128i[9] = 420;
      mScratch128i[10] = 428;
      mScratch128i[11] = 429;
      mScratch128i[12] = 434;
      mScratch128i[13] = 437;
      mScratch128i[14] = 444;
      mScratch128i[15] = 457;
      mScratch128i[16] = 459;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<17; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //1095 365 {1.927173, -0.416853, -0.482877}
    //1101 367 {1.927173, -0.416853, -0.482877}
    //1158 386 {1.927173, -0.417338, 0.482399}
    //1164 388 {1.927173, -0.417338, 0.482399}
    //1305 435 {1.927173, -0.417338, 0.482399}
    //1338 446 {1.927173, -0.416853, -0.482877}
    synchronized (mScratch128i) {
      mScratch128i[0] = 365;
      mScratch128i[1] = 367;
      mScratch128i[2] = 386;
      mScratch128i[3] = 388;
      mScratch128i[4] = 435;
      mScratch128i[5] = 446;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<6; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //765 255 {2.093644, -0.293797, 0.016645}
    //801 267 {2.093644, -0.293797, 0.016645}
    //1026 342 {2.093644, -0.293797, 0.016645}
    //1113 371 {2.093644, -0.293797, 0.016645}
    //1122 374 {2.093644, -0.293797, 0.016645}
    //1128 376 {2.093644, -0.293797, 0.016645}
    //1134 378 {2.093644, -0.293797, 0.016645}
    //1143 381 {2.093644, -0.293797, 0.016645}
    //1314 438 {2.093644, -0.293797, 0.016645}
    //1329 443 {2.093644, -0.293797, 0.016645}
    synchronized (mScratch128i) {
      mScratch128i[0] = 255;
      mScratch128i[1] = 267;
      mScratch128i[2] = 342;
      mScratch128i[3] = 371;
      mScratch128i[4] = 374;
      mScratch128i[5] = 376;
      mScratch128i[6] = 378;
      mScratch128i[7] = 381;
      mScratch128i[8] = 438;
      mScratch128i[9] = 443;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<10; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //1104 368 {2.108928, -0.297971, -0.207399}
    //1110 370 {2.108928, -0.297971, -0.207399}
    //1137 379 {2.109581, -0.298879, 0.211736}
    //1155 385 {2.109581, -0.298879, 0.211736}
    //1308 436 {2.109581, -0.298879, 0.211736}
    //1320 440 {2.109581, -0.298879, 0.211736}
    //1323 441 {2.108928, -0.297971, -0.207399}
    //1335 445 {2.108928, -0.297971, -0.207399}
    //1374 458 {2.109581, -0.298879, 0.211736}
    //1383 461 {2.108928, -0.297971, -0.207399}
    synchronized (mScratch128i) {
      mScratch128i[0] = 368;
      mScratch128i[1] = 370;
      mScratch128i[2] = 379;
      mScratch128i[3] = 385;
      mScratch128i[4] = 436;
      mScratch128i[5] = 440;
      mScratch128i[6] = 441;
      mScratch128i[7] = 445;
      mScratch128i[8] = 458;
      mScratch128i[9] = 461;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<10; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //876 292 {2.291811, 0.175917, -0.261083}
    //882 294 {2.291811, 0.175917, -0.261083}
    //930 310 {2.229115, 0.215238, 0.272642}
    //936 312 {2.229115, 0.215238, 0.272642}
    //1035 345 {2.229115, 0.215238, 0.272642}
    //1062 354 {2.291811, 0.175917, -0.261083}
    //1071 357 {2.270623, 0.451979, 0.013410}
    //1191 397 {2.229115, 0.215238, 0.272642}
    //1203 401 {2.229115, 0.215238, 0.272642}
    //1206 402 {2.229115, 0.215238, 0.272642}
    //1215 405 {2.229115, 0.215238, 0.272642}
    //1239 413 {2.291811, 0.175917, -0.261083}
    //1254 418 {2.291811, 0.175917, -0.261083}
    //1266 422 {2.291811, 0.175917, -0.261083}
    //1269 423 {2.291811, 0.175917, -0.261083}
    //1278 426 {2.291811, 0.175917, -0.261083}
    //1290 430 {2.270623, 0.451979, 0.013410}
    //1293 431 {2.291811, 0.175917, -0.261083}
    //1296 432 {2.229115, 0.215238, 0.272642}
    //1299 433 {2.270623, 0.451979, 0.013410}
    //1341 447 {2.270623, 0.451979, 0.013410}
    //1344 448 {2.229115, 0.215238, 0.272642}
    //1350 450 {2.270623, 0.451979, 0.013410}
    //1359 453 {2.291811, 0.175917, -0.261083}
    //1362 454 {2.270623, 0.451979, 0.013410}
    synchronized (mScratch128i) {
      mScratch128i[0] = 292;
      mScratch128i[1] = 294;
      mScratch128i[2] = 310;
      mScratch128i[3] = 312;
      mScratch128i[4] = 345;
      mScratch128i[5] = 354;
      mScratch128i[6] = 357;
      mScratch128i[7] = 397;
      mScratch128i[8] = 401;
      mScratch128i[9] = 402;
      mScratch128i[10] = 405;
      mScratch128i[11] = 413;
      mScratch128i[12] = 418;
      mScratch128i[13] = 422;
      mScratch128i[14] = 423;
      mScratch128i[15] = 426;
      mScratch128i[16] = 430;
      mScratch128i[17] = 431;
      mScratch128i[18] = 432;
      mScratch128i[19] = 433;
      mScratch128i[20] = 447;
      mScratch128i[21] = 448;
      mScratch128i[22] = 450;
      mScratch128i[23] = 453;
      mScratch128i[24] = 454;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<25; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //768 256 {2.692342, -0.093702, -0.171834}
    //777 259 {2.692342, -0.093702, -0.171834}
    //807 269 {2.696234, -0.086263, 0.195221}
    //810 270 {2.696234, -0.086263, 0.195221}
    //873 291 {2.692342, -0.093702, -0.171834}
    //939 313 {2.696234, -0.086263, 0.195221}
    //954 318 {2.696234, -0.086263, 0.195221}
    //984 328 {2.692342, -0.093702, -0.171834}
    //1173 391 {2.696234, -0.086263, 0.195221}
    //1200 400 {2.696234, -0.086263, 0.195221}
    //1281 427 {2.692342, -0.093702, -0.171834}
    //1317 439 {2.696234, -0.086263, 0.195221}
    //1326 442 {2.692342, -0.093702, -0.171834}
    //1368 456 {2.696234, -0.086263, 0.195221}
    //1380 460 {2.692342, -0.093702, -0.171834}
    synchronized (mScratch128i) {
      mScratch128i[0] = 256;
      mScratch128i[1] = 259;
      mScratch128i[2] = 269;
      mScratch128i[3] = 270;
      mScratch128i[4] = 291;
      mScratch128i[5] = 313;
      mScratch128i[6] = 318;
      mScratch128i[7] = 328;
      mScratch128i[8] = 391;
      mScratch128i[9] = 400;
      mScratch128i[10] = 427;
      mScratch128i[11] = 439;
      mScratch128i[12] = 442;
      mScratch128i[13] = 456;
      mScratch128i[14] = 460;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<15; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //819 273 {2.704395, 0.418064, 0.032802}
    //831 277 {2.704395, 0.418064, 0.032802}
    //837 279 {2.704395, 0.418064, 0.032802}
    //855 285 {2.704395, 0.418064, 0.032802}
    //1077 359 {2.704395, 0.418064, 0.032802}
    //1353 451 {2.704395, 0.418064, 0.032802}
    synchronized (mScratch128i) {
      mScratch128i[0] = 273;
      mScratch128i[1] = 277;
      mScratch128i[2] = 279;
      mScratch128i[3] = 285;
      mScratch128i[4] = 359;
      mScratch128i[5] = 451;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<6; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //771 257 {2.980200, -0.177075, 0.001311}
    //774 258 {2.980200, -0.177075, 0.001311}
    //783 261 {2.980200, -0.177075, 0.001311}
    //792 264 {2.980200, -0.177075, 0.001311}
    //804 268 {2.980200, -0.177075, 0.001311}
    //813 271 {2.980200, -0.177075, 0.001311}
    //822 274 {2.989846, 0.292160, 0.127853}
    //828 276 {2.946125, 0.557792, -0.000209}
    //840 280 {2.946125, 0.557792, -0.000209}
    //843 281 {2.915475, 0.406418, -0.042615}
    //846 282 {2.915475, 0.406418, -0.042615}
    //849 283 {2.946125, 0.557792, -0.000209}
    //858 286 {2.915475, 0.406418, -0.042615}
    //861 287 {2.943090, 0.327395, -0.109560}
    //864 288 {2.943090, 0.327395, -0.109560}
    //867 289 {2.915475, 0.406418, -0.042615}
    //885 295 {2.943090, 0.327395, -0.109560}
    //891 297 {2.943090, 0.327395, -0.109560}
    //900 300 {2.943090, 0.327395, -0.109560}
    //912 304 {2.989846, 0.292160, 0.127853}
    //921 307 {2.989846, 0.292160, 0.127853}
    //927 309 {2.989846, 0.292160, 0.127853}
    //1074 358 {2.989846, 0.292160, 0.127853}
    //1347 449 {2.989846, 0.292160, 0.127853}
    //1356 452 {2.943090, 0.327395, -0.109560}
    //1365 455 {2.943090, 0.327395, -0.109560}
    synchronized (mScratch128i) {
      mScratch128i[0] = 257;
      mScratch128i[1] = 258;
      mScratch128i[2] = 261;
      mScratch128i[3] = 264;
      mScratch128i[4] = 268;
      mScratch128i[5] = 271;
      mScratch128i[6] = 274;
      mScratch128i[7] = 276;
      mScratch128i[8] = 280;
      mScratch128i[9] = 281;
      mScratch128i[10] = 282;
      mScratch128i[11] = 283;
      mScratch128i[12] = 286;
      mScratch128i[13] = 287;
      mScratch128i[14] = 288;
      mScratch128i[15] = 289;
      mScratch128i[16] = 295;
      mScratch128i[17] = 297;
      mScratch128i[18] = 300;
      mScratch128i[19] = 304;
      mScratch128i[20] = 307;
      mScratch128i[21] = 309;
      mScratch128i[22] = 358;
      mScratch128i[23] = 449;
      mScratch128i[24] = 452;
      mScratch128i[25] = 455;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<26; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //825 275 {3.061522, 0.408129, 0.013388}
    //834 278 {3.061522, 0.408129, 0.013388}
    //852 284 {3.061522, 0.408129, 0.013388}
    //870 290 {3.061522, 0.408129, 0.013388}
    //903 301 {3.061522, 0.408129, 0.013388}
    //909 303 {3.061522, 0.408129, 0.013388}
    synchronized (mScratch128i) {
      mScratch128i[0] = 275;
      mScratch128i[1] = 278;
      mScratch128i[2] = 284;
      mScratch128i[3] = 290;
      mScratch128i[4] = 301;
      mScratch128i[5] = 303;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<6; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //780 260 {3.194957, -0.127989, -0.063934}
    //786 262 {3.194957, -0.127989, -0.063934}
    //975 325 {3.194957, -0.127989, -0.063934}
    //981 327 {3.194957, -0.127989, -0.063934}
    //990 330 {3.194957, -0.127989, -0.063934}
    synchronized (mScratch128i) {
      mScratch128i[0] = 260;
      mScratch128i[1] = 262;
      mScratch128i[2] = 325;
      mScratch128i[3] = 327;
      mScratch128i[4] = 330;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<5; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //789 263 {3.248326, -0.309442, 0.000001}
    //795 265 {3.248326, -0.309442, 0.000001}
    //798 266 {3.209910, -0.120950, 0.065819}
    //816 272 {3.209910, -0.120950, 0.065819}
    //945 315 {3.209910, -0.120950, 0.065819}
    //957 319 {3.209910, -0.120950, 0.065819}
    //963 321 {3.209910, -0.120950, 0.065819}
    //966 322 {3.248326, -0.309442, 0.000001}
    //972 324 {3.248326, -0.309442, 0.000001}
    synchronized (mScratch128i) {
      mScratch128i[0] = 263;
      mScratch128i[1] = 265;
      mScratch128i[2] = 266;
      mScratch128i[3] = 272;
      mScratch128i[4] = 315;
      mScratch128i[5] = 319;
      mScratch128i[6] = 321;
      mScratch128i[7] = 322;
      mScratch128i[8] = 324;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<9; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //756 252 {3.451792, -0.101615, -0.015411}
    //894 298 {3.417057, 0.268542, 0.000605}
    //906 302 {3.417057, 0.268542, 0.000605}
    //915 305 {3.417057, 0.268542, 0.000605}
    //918 306 {3.417057, 0.268542, 0.000605}
    //948 316 {3.451792, -0.101615, -0.015411}
    //969 323 {3.451792, -0.101615, -0.015411}
    //978 326 {3.451792, -0.101615, -0.015411}
    //996 332 {3.451792, -0.101615, -0.015411}
    //1002 334 {3.451792, -0.101615, -0.015411}
    //1008 336 {3.417057, 0.268542, 0.000605}
    //1020 340 {3.417057, 0.268542, 0.000605}
    synchronized (mScratch128i) {
      mScratch128i[0] = 252;
      mScratch128i[1] = 298;
      mScratch128i[2] = 302;
      mScratch128i[3] = 305;
      mScratch128i[4] = 306;
      mScratch128i[5] = 316;
      mScratch128i[6] = 323;
      mScratch128i[7] = 326;
      mScratch128i[8] = 332;
      mScratch128i[9] = 334;
      mScratch128i[10] = 336;
      mScratch128i[11] = 340;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<12; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //108 036 {3.718714, 0.093755, 0.089268}
    //759 253 {3.718877, 0.077507, -0.091371}
    //879 293 {3.718877, 0.077507, -0.091371}
    //888 296 {3.718877, 0.077507, -0.091371}
    //897 299 {3.718877, 0.077507, -0.091371}
    //924 308 {3.718714, 0.093755, 0.089268}
    //933 311 {3.718714, 0.093755, 0.089268}
    //942 314 {3.718714, 0.093755, 0.089268}
    //951 317 {3.718714, 0.093755, 0.089268}
    //960 320 {3.718714, 0.093755, 0.089268}
    //987 329 {3.718877, 0.077507, -0.091371}
    //993 331 {3.718877, 0.077507, -0.091371}
    //999 333 {3.718714, 0.093755, 0.089268}
    //1011 337 {3.718714, 0.093755, 0.089268}
    //1017 339 {3.718877, 0.077507, -0.091371}
    //1461 487 {3.718877, 0.077507, -0.091371}
    //1479 493 {3.718714, 0.093755, 0.089268}
    //1494 498 {3.718877, 0.077507, -0.091371}
    synchronized (mScratch128i) {
      mScratch128i[0] =  36;
      mScratch128i[1] = 253;
      mScratch128i[2] = 293;
      mScratch128i[3] = 296;
      mScratch128i[4] = 299;
      mScratch128i[5] = 308;
      mScratch128i[6] = 311;
      mScratch128i[7] = 314;
      mScratch128i[8] = 317;
      mScratch128i[9] = 320;
      mScratch128i[10] = 329;
      mScratch128i[11] = 331;
      mScratch128i[12] = 333;
      mScratch128i[13] = 337;
      mScratch128i[14] = 339;
      mScratch128i[15] = 487;
      mScratch128i[16] = 493;
      mScratch128i[17] = 498;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<18; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }

    //090 030 {3.965849, 0.236137, -0.023106}
    //099 033 {3.965849, 0.236137, -0.023106}
    //111 037 {3.946237, -0.045008, 0.017905}
    //117 039 {3.946237, -0.045008, 0.017905}
    //762 254 {3.946237, -0.045008, 0.017905}
    //1005 335 {3.946237, -0.045008, 0.017905}
    //1014 338 {3.965849, 0.236137, -0.023106}
    //1023 341 {3.965849, 0.236137, -0.023106}
    //1458 486 {3.946237, -0.045008, 0.017905}
    //1467 489 {3.946237, -0.045008, 0.017905}
    //1476 492 {3.965849, 0.236137, -0.023106}
    //1485 495 {3.965849, 0.236137, -0.023106}
    //1497 499 {3.965849, 0.236137, -0.023106}
    //1503 501 {3.965849, 0.236137, -0.023106}
    synchronized (mScratch128i) {
      mScratch128i[0] =  30;
      mScratch128i[1] =  33;
      mScratch128i[2] =  37;
      mScratch128i[3] =  39;
      mScratch128i[4] = 254;
      mScratch128i[5] = 335;
      mScratch128i[6] = 338;
      mScratch128i[7] = 341;
      mScratch128i[8] = 486;
      mScratch128i[9] = 489;
      mScratch128i[10] = 492;
      mScratch128i[11] = 495;
      mScratch128i[12] = 499;
      mScratch128i[13] = 501;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<14; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //120 040 {4.273219, -0.622190, 0.000425}
    //126 042 {4.273219, -0.622190, 0.000425}
    //135 045 {4.273219, -0.622190, 0.000425}
    //144 048 {4.273219, -0.622190, 0.000425}
    //156 052 {4.273219, -0.622190, 0.000425}
    //1473 491 {4.273219, -0.622190, 0.000425}
    synchronized (mScratch128i) {
      mScratch128i[0] = 40;
      mScratch128i[1] = 42;
      mScratch128i[2] = 45;
      mScratch128i[3] = 48;
      mScratch128i[4] = 52;
      mScratch128i[5] = 491;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<6; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //114 038 {4.407359, 0.002633, 0.068367}
    //123 041 {4.407359, 0.002633, 0.068367}
    //129 043 {4.407360, 0.004900, -0.070817}
    //153 051 {4.407359, 0.002633, 0.068367}
    //162 054 {4.407359, 0.002633, 0.068367}
    //171 057 {4.407359, 0.002633, 0.068367}
    //216 072 {4.407360, 0.004900, -0.070817}
    //225 075 {4.407360, 0.004900, -0.070817}
    //1422 474 {4.407360, 0.004900, -0.070817}
    //1443 481 {4.407359, 0.002633, 0.068367}
    //1464 488 {4.407360, 0.004900, -0.070817}
    //1470 490 {4.407360, 0.004900, -0.070817}
    //1482 494 {4.407359, 0.002633, 0.068367}
    //1488 496 {4.407359, 0.002633, 0.068367}
    //1500 500 {4.407360, 0.004900, -0.070817}
    //1509 503 {4.407360, 0.004900, -0.070817}
    synchronized (mScratch128i) {
      mScratch128i[0] = 38;
      mScratch128i[1] = 41;
      mScratch128i[2] = 43;
      mScratch128i[3] = 51;
      mScratch128i[4] = 54;
      mScratch128i[5] = 57;
      mScratch128i[6] = 72;
      mScratch128i[7] = 75;
      mScratch128i[8] = 474;
      mScratch128i[9] = 481;
      mScratch128i[10] = 488;
      mScratch128i[11] = 490;
      mScratch128i[12] = 494;
      mScratch128i[13] = 496;
      mScratch128i[14] = 500;
      mScratch128i[15] = 503;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<16; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //096 032 {4.682242, 0.381549, -0.070219}
    //102 034 {4.682240, 0.384262, 0.067279}
    //132 044 {4.659094, -0.681220, -0.048759}
    //138 046 {4.659094, -0.681220, -0.048759}
    //141 047 {4.691498, -0.912250, 0.000001}
    //147 049 {4.691498, -0.912250, 0.000001}
    //150 050 {4.659094, -0.681221, 0.048761}
    //159 053 {4.659094, -0.681221, 0.048761}
    //165 055 {4.659094, -0.681221, 0.048761}
    //168 056 {4.603467, -0.263932, 0.074477}
    //174 058 {4.603467, -0.263932, 0.074477}
    //183 061 {4.682240, 0.384262, 0.067279}
    //207 069 {4.682242, 0.381549, -0.070219}
    //222 074 {4.603467, -0.263926, -0.074475}
    //228 076 {4.603467, -0.263926, -0.074475}
    //231 077 {4.659094, -0.681220, -0.048759}
    //237 079 {4.691498, -0.912250, 0.000001}
    //240 080 {4.659094, -0.681220, -0.048759}
    //246 082 {4.659094, -0.681221, 0.048761}
    //249 083 {4.691498, -0.912250, 0.000001}
    //255 085 {4.603467, -0.263932, 0.074477}
    //258 086 {4.659094, -0.681221, 0.048761}
    //282 094 {4.659094, -0.681220, -0.048759}
    //285 095 {4.603467, -0.263926, -0.074475}
    //1425 475 {4.682242, 0.381549, -0.070219}
    //1431 477 {4.682242, 0.381549, -0.070219}
    //1440 480 {4.682240, 0.384262, 0.067279}
    //1449 483 {4.682240, 0.384262, 0.067279}
    //1491 497 {4.682240, 0.384262, 0.067279}
    //1506 502 {4.682242, 0.381549, -0.070219}
    synchronized (mScratch128i) {
      mScratch128i[0] = 32;
      mScratch128i[1] = 34;
      mScratch128i[2] = 44;
      mScratch128i[3] = 46;
      mScratch128i[4] = 47;
      mScratch128i[5] = 49;
      mScratch128i[6] = 50;
      mScratch128i[7] = 53;
      mScratch128i[8] = 55;
      mScratch128i[9] = 56;
      mScratch128i[10] = 58;
      mScratch128i[11] = 61;
      mScratch128i[12] = 69;
      mScratch128i[13] = 74;
      mScratch128i[14] = 76;
      mScratch128i[15] = 77;
      mScratch128i[16] = 79;
      mScratch128i[17] = 80;
      mScratch128i[18] = 82;
      mScratch128i[19] = 83;
      mScratch128i[20] = 85;
      mScratch128i[21] = 86;
      mScratch128i[22] = 94;
      mScratch128i[23] = 95;
      mScratch128i[24] = 475;
      mScratch128i[25] = 477;
      mScratch128i[26] = 480;
      mScratch128i[27] = 483;
      mScratch128i[28] = 497;
      mScratch128i[29] = 502;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<30; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //177 059 {4.755723, 0.023309, 0.000001}
    //219 073 {4.755723, 0.023309, 0.000001}
    //234 078 {4.755723, 0.023309, 0.000001}
    //243 081 {4.755723, 0.023309, 0.000001}
    //252 084 {4.755723, 0.023309, 0.000001}
    //261 087 {4.755723, 0.023309, 0.000001}
    //270 090 {4.755723, 0.023309, 0.000001}
    //279 093 {4.755723, 0.023309, 0.000001}
    //1428 476 {4.755723, 0.023309, 0.000001}
    //1437 479 {4.755723, 0.023309, 0.000001}
    //1446 482 {4.755723, 0.023309, 0.000001}
    //1452 484 {4.755723, 0.023309, 0.000001}
    synchronized (mScratch128i) {
      mScratch128i[0] = 59;
      mScratch128i[1] = 73;
      mScratch128i[2] = 78;
      mScratch128i[3] = 81;
      mScratch128i[4] = 84;
      mScratch128i[5] = 87;
      mScratch128i[6] = 90;
      mScratch128i[7] = 93;
      mScratch128i[8] = 476;
      mScratch128i[9] = 479;
      mScratch128i[10] = 482;
      mScratch128i[11] = 484;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<12; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //093 031 {4.823390, 0.711646, -0.002101}
    //105 035 {4.823390, 0.711646, -0.002101}
    //180 060 {4.823390, 0.711646, -0.002101}
    //189 063 {4.823390, 0.711646, -0.002101}
    //198 066 {4.823390, 0.711646, -0.002101}
    //210 070 {4.823390, 0.711646, -0.002101}
    synchronized (mScratch128i) {
      mScratch128i[0] = 31;
      mScratch128i[1] = 35;
      mScratch128i[2] = 60;
      mScratch128i[3] = 63;
      mScratch128i[4] = 66;
      mScratch128i[5] = 70;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<6; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }

    //186 062 {5.218821, 0.718831, 0.050632}
    //192 064 {5.218821, 0.718831, 0.050632}
    //204 068 {5.218824, 0.718836, -0.050629}
    //213 071 {5.218824, 0.718836, -0.050629}
    //267 089 {5.218821, 0.718831, 0.050632}
    //273 091 {5.218824, 0.718836, -0.050629}
    //1434 478 {5.218824, 0.718836, -0.050629}
    //1455 485 {5.218821, 0.718831, 0.050632}
    synchronized (mScratch128i) {
      mScratch128i[0] = 62;
      mScratch128i[1] = 64;
      mScratch128i[2] = 68;
      mScratch128i[3] = 71;
      mScratch128i[4] = 89;
      mScratch128i[5] = 91;
      mScratch128i[6] = 478;
      mScratch128i[7] = 485;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<8; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }


    //195 065 {5.403603, 0.969894, 0.000001}
    //201 067 {5.403603, 0.969894, 0.000001}
    //264 088 {5.403603, 0.969894, 0.000001}
    //276 092 {5.403603, 0.969894, 0.000001}
    synchronized (mScratch128i) {
      mScratch128i[0] = 65;
      mScratch128i[1] = 67;
      mScratch128i[2] = 88;
      mScratch128i[3] = 92;
      float width = getMoveWidth(JinbeiData.vertices[0+3*mScratch128i[0]]) * s;
      int ii;
      for (ii=0; ii<4; ii++) {
        JinbeiData.vertices[2+3*mScratch128i[ii]] = JinbeiData.org_vertices[2+3*mScratch128i[ii]] + width;
      }
    }
     
    mVertexBuffer.position(0);
    mVertexBuffer.put(JinbeiData.vertices);
    mVertexBuffer.position(0);

  }

  public void calc() {
    synchronized (this) {
      setTurnDirection(TURN_DIRECTION.STRAIGHT);
      think();
      move();
      animate();
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
    gl10.glScalef(GL_JINBEI_SCALE,GL_JINBEI_SCALE,GL_JINBEI_SCALE);

    gl10.glRotatef(angleForAnimation, 0.0f, 1.0f, 0.0f);

    gl10.glRotatef(y_angle, 0.0f, 1.0f, 0.0f);
    gl10.glRotatef(x_angle * -1f, 0.0f, 0.0f, 1.0f);

    // boundingboxを計算
    separateBoundingBox();
    alignment1BoundingBox();
    alignment2BoundingBox();

    gl10.glColor4f(1,1,1,1);
    gl10.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
    gl10.glNormalPointer(GL10.GL_FLOAT, 0, mNormalBuffer);
    gl10.glEnable(GL10.GL_TEXTURE_2D);
    gl10.glBindTexture(GL10.GL_TEXTURE_2D, textureIds[0]);
    gl10.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTextureBuffer);
    gl10.glDrawArrays(GL10.GL_TRIANGLES, 0, JinbeiData.numVerts);



    gl10.glPopMatrix();
    gl10.glPopMatrix();
  }

  public void update_speed() {
    sv_speed = speed;
    if (getStatus() == STATUS.COHESION || getStatus() == STATUS.TO_SCHOOL_CENTER || getStatus() == STATUS.TO_BAIT) {
      speed = cohesion_speed;
      return;
    }
    speed = sv_speed;

    if (this.rand.nextInt(10000) <= 1000) {
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
   * どの方向に進むか考える
   */
  public void think() {
    long nowTime = System.nanoTime();
    if (prevTime != 0) {
      tick = nowTime - prevTime;
    }
    if (getStatus() == STATUS.COHESION || getStatus() == STATUS.TO_SCHOOL_CENTER || getStatus() == STATUS.TO_BAIT) {
      /* 元に戻す */
      speed = sv_speed;
    }
    prevTime = nowTime;
    if (  (Aquarium.min_x.floatValue() + (getSize() * 1.5f) >= position[0] || Aquarium.max_x.floatValue() - (getSize() * 1.5f) <= position[0])
      ||  (Aquarium.min_y.floatValue() + (getSize()/3f) >= position[1] || Aquarium.max_y.floatValue() - (getSize()/3f) <= position[1])
      ||  (Aquarium.min_z.floatValue() + (getSize() * 1.5f) >= position[2] || Aquarium.max_z.floatValue() - (getSize() * 1.5f) <= position[2])) {
      /*=====================================================================*/
      /* 水槽からはみ出てる                                                  */
      /*=====================================================================*/
      setStatus(STATUS.TO_CENTER);
      aimAquariumCenter();
      if (traceBOIDS && jinbeiNo == 0) Log.d(TAG, "to Aquarium Center");
      update_speed();
      return;
    }
    /**
     * 餌ロジック
     */
    Bait bait = baitManager.getBait();
    if (bait != null) {
      if (this.rand.nextInt(10000) <= 5500) {
        if (aimBait(bait)) {
          if (traceBOIDS && jinbeiNo == 0) Log.d(TAG, "to Bait");
          setStatus(STATUS.TO_BAIT);
          update_speed();
          return;
        }
      }
    }

    if (this.rand.nextInt(10000) <= 9500) {
      if (traceBOIDS && jinbeiNo == 0) Log.d(TAG, "Nop");
      // 変更なし
      return;
    }
    setStatus(STATUS.NORMAL);
    turn();
    if (traceBOIDS && jinbeiNo == 0) Log.d(TAG, "Normal");
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
    float newAngleX = this.rand.nextFloat() * 3.0f - 1.5f;
    float newAngleY = 0f;
    if (angleForAnimation < 0f) {
      newAngleY = this.rand.nextFloat() * -1.5f;
      setTurnDirection(TURN_DIRECTION.TURN_RIGHT);
    }
    else {
      newAngleY = this.rand.nextFloat() * 1.5f;
      setTurnDirection(TURN_DIRECTION.TURN_LEFT);
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
    float newAngle = this.rand.nextFloat() * 3f;

    float yy = angle_y - y_angle;
    if (yy > 180.0f) {
      yy = -360f + yy;
    }
    else if (yy < -180.0f) {
      yy = 360f - yy;
    }

    if (yy < 0.0f) {
      if (angleForAnimation < 0f) {
        if (yy > -1.5f) {
          y_angle += yy;
        }
        else {
          y_angle += -newAngle;
        }
        setTurnDirection(TURN_DIRECTION.TURN_LEFT);
      }
    }
    else if (yy > 0.0f) {
      if (angleForAnimation > 0f) {
        if (yy < 1.5f) {
          y_angle += yy;
        }
        else {
          y_angle += newAngle;
        }
        setTurnDirection(TURN_DIRECTION.TURN_RIGHT);
      }
    }
    else {
      setTurnDirection(TURN_DIRECTION.STRAIGHT);
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
  public void turnSeparation(Jinbei target) {
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
  public void turnAlignment(Jinbei target) {
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
  public void turnCohesion(Jinbei target) {
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
    if (Aquarium.min_x.floatValue() + (getSize() * 1.5f)    < getX() && Aquarium.max_x.floatValue() - (getSize() * 1.5f)    > getX()
    &&  Aquarium.min_y.floatValue() + (getSize()/3f) < getY() && Aquarium.max_y.floatValue() - (getSize()/3f) > getY()) {
      /* Zだけはみ出た */
      v_x = 0.0f;
      v_y = 0.0f;
    }
    else 
    if (Aquarium.min_x.floatValue() + (getSize() * 1.5f) < getX() && Aquarium.max_x.floatValue() - (getSize() * 1.5f) > getX()
    &&  Aquarium.min_z.floatValue() + (getSize() * 1.5f) < getZ() && Aquarium.max_z.floatValue() - (getSize() * 1.5f) > getZ()) {
      /* Yだけはみ出た */
      v_x = 0.0f;
      v_z = 0.0f;
    }
    else 
    if (Aquarium.min_y.floatValue() + (getSize()/3f)      < getY() && Aquarium.max_y.floatValue() - (getSize()/3f)     > getY()
    &&  Aquarium.min_z.floatValue() + (getSize() * 1.5f) < getZ() && Aquarium.max_z.floatValue() - (getSize() * 1.5f) > getZ()) {
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
   * Get jinbeiCount.
   *
   * @return jinbeiCount as int.
   */
  public int getJinbeiCount()
  {
      return jinbeiCount;
  }
  
  /**
   * Set jinbeiCount.
   *
   * @param jinbeiCount the value to set.
   */
  public void setJinbeiCount(int jinbeiCount)
  {
      this.jinbeiCount = jinbeiCount;
  }
  
  /**
   * Get distances.
   *
   * @return distances as float[].
   */
  public float[] getDistances()
  {
      return distances;
  }
  
  /**
   * Get distances element at specified index.
   *
   * @param index the index.
   * @return distances at index as float.
   */
  public float getDistances(int index)
  {
      return distances[index];
  }
  
  /**
   * Set distances.
   *
   * @param distances the value to set.
   */
  public void setDistances(float[] distances)
  {
      this.distances = distances;
  }
  
  /**
   * Set distances at the specified index.
   *
   * @param distances the value to set.
   * @param index the index.
   */
  public void setDistances(float distances, int index)
  {
      this.distances[index] = distances;
  }

  public void separateBoundingBox() {
    coordUtil.setMatrixRotateZ(x_angle);
    synchronized (mScratch4f_1) {
      synchronized (mScratch4f_2) {
        coordUtil.affine((float)aabb_org[0],(float)aabb_org[1], (float)aabb_org[2], mScratch4f_1);
        coordUtil.setMatrixRotateY(y_angle);
        coordUtil.affine(mScratch4f_1[0],mScratch4f_1[1], mScratch4f_1[2], mScratch4f_2);
        sep_aabb[0] = mScratch4f_2[0];
        sep_aabb[1] = mScratch4f_2[1];
        sep_aabb[2] = mScratch4f_2[2];
      }
    }
    coordUtil.setMatrixRotateZ(x_angle);
    synchronized (mScratch4f_1) {
      synchronized (mScratch4f_2) {
        coordUtil.affine((float)aabb_org[3],(float)aabb_org[4], (float)aabb_org[5], mScratch4f_1);
        coordUtil.setMatrixRotateY(y_angle);
        coordUtil.affine(mScratch4f_1[0],mScratch4f_1[1], mScratch4f_1[2], mScratch4f_2);
        sep_aabb[3] = mScratch4f_2[0];
        sep_aabb[4] = mScratch4f_2[1];
        sep_aabb[5] = mScratch4f_2[2];
      }
    }
    if (sep_aabb[0] > sep_aabb[3]) {
      double tmp = sep_aabb[0];
      sep_aabb[0] = sep_aabb[3];
      sep_aabb[3] = tmp;
    }
    if (sep_aabb[1] > sep_aabb[4]) {
      double tmp = sep_aabb[1];
      sep_aabb[1] = sep_aabb[4];
      sep_aabb[4] = tmp;
    }
    if (sep_aabb[2] > sep_aabb[5]) {
      double tmp = sep_aabb[2];
      sep_aabb[2] = sep_aabb[5];
      sep_aabb[5] = tmp;
    }
    sep_aabb[0] += getX();
    sep_aabb[1] += getY();
    sep_aabb[2] += getZ();
    sep_aabb[3] += getX();
    sep_aabb[4] += getY();
    sep_aabb[5] += getZ();
  }

  public static boolean crossTestSep(float x, float y, float z) {
    double min_x = sep_aabb[0];
    double min_y = sep_aabb[1];
    double min_z = sep_aabb[2];
    double max_x = sep_aabb[3];
    double max_y = sep_aabb[4];
    double max_z = sep_aabb[5];
    return (   (float)min_x <= x && (float)max_x >= x
            && (float)min_y <= y && (float)max_y >= y
            && (float)min_z <= z && (float)max_z >= z);
  }

  public void alignment1BoundingBox() {
    coordUtil.setMatrixRotateZ(x_angle);
    synchronized (mScratch4f_1) {
      synchronized (mScratch4f_2) {
        coordUtil.affine((float)aabb_org[0] - (float)alignment_dist1,
                         (float)aabb_org[1] - (float)alignment_dist1, 
                         (float)aabb_org[2] - (float)alignment_dist1, 
                         mScratch4f_1);
        coordUtil.setMatrixRotateY(y_angle);
        coordUtil.affine(mScratch4f_1[0],mScratch4f_1[1], mScratch4f_1[2], mScratch4f_2);
        al1_aabb[0] = mScratch4f_2[0];
        al1_aabb[1] = mScratch4f_2[1];
        al1_aabb[2] = mScratch4f_2[2];
      }
    }
    coordUtil.setMatrixRotateZ(x_angle);
    synchronized (mScratch4f_1) {
      synchronized (mScratch4f_2) {
        coordUtil.affine((float)aabb_org[3] + (float)alignment_dist1,
                         (float)aabb_org[4] + (float)alignment_dist1, 
                         (float)aabb_org[5] + (float)alignment_dist1, 
                         mScratch4f_1);
        coordUtil.setMatrixRotateY(y_angle);
        coordUtil.affine(mScratch4f_1[0],mScratch4f_1[1], mScratch4f_1[2], mScratch4f_2);
        al1_aabb[3] = mScratch4f_2[0];
        al1_aabb[4] = mScratch4f_2[1];
        al1_aabb[5] = mScratch4f_2[2];
      }
    }
    if (al1_aabb[0] > al1_aabb[3]) {
      double tmp = al1_aabb[0];
      al1_aabb[0] = al1_aabb[3];
      al1_aabb[3] = tmp;
    }
    if (al1_aabb[1] > al1_aabb[4]) {
      double tmp = al1_aabb[1];
      al1_aabb[1] = al1_aabb[4];
      al1_aabb[4] = tmp;
    }
    if (al1_aabb[2] > al1_aabb[5]) {
      double tmp = al1_aabb[2];
      al1_aabb[2] = al1_aabb[5];
      al1_aabb[5] = tmp;
    }
    al1_aabb[0] += getX();
    al1_aabb[1] += getY();
    al1_aabb[2] += getZ();
    al1_aabb[3] += getX();
    al1_aabb[4] += getY();
    al1_aabb[5] += getZ();
  }
  public static boolean crossTestAl1(float x, float y, float z) {
    double min_x = al1_aabb[0];
    double min_y = al1_aabb[1];
    double min_z = al1_aabb[2];
    double max_x = al1_aabb[3];
    double max_y = al1_aabb[4];
    double max_z = al1_aabb[5];
    return (   (float)min_x <= x && (float)max_x >= x
            && (float)min_y <= y && (float)max_y >= y
            && (float)min_z <= z && (float)max_z >= z);
  }
  public void alignment2BoundingBox() {
    coordUtil.setMatrixRotateZ(x_angle);
    synchronized (mScratch4f_1) {
      synchronized (mScratch4f_2) {
        coordUtil.affine((float)aabb_org[0] - (float)alignment_dist2,
                         (float)aabb_org[1] - (float)alignment_dist2, 
                         (float)aabb_org[2] - (float)alignment_dist2, 
                         mScratch4f_1);
        coordUtil.setMatrixRotateY(y_angle);
        coordUtil.affine(mScratch4f_1[0],mScratch4f_1[1], mScratch4f_1[2], mScratch4f_2);
        al2_aabb[0] = mScratch4f_2[0];
        al2_aabb[1] = mScratch4f_2[1];
        al2_aabb[2] = mScratch4f_2[2];
      }
    }
    coordUtil.setMatrixRotateZ(x_angle);
    synchronized (mScratch4f_1) {
      synchronized (mScratch4f_2) {
        coordUtil.affine((float)aabb_org[3] + (float)alignment_dist2,
                         (float)aabb_org[4] + (float)alignment_dist2, 
                         (float)aabb_org[5] + (float)alignment_dist2, 
                         mScratch4f_1);
        coordUtil.setMatrixRotateY(y_angle);
        coordUtil.affine(mScratch4f_1[0],mScratch4f_1[1], mScratch4f_1[2], mScratch4f_2);
        al2_aabb[3] = mScratch4f_2[0];
        al2_aabb[4] = mScratch4f_2[1];
        al2_aabb[5] = mScratch4f_2[2];
      }
    }
    if (al2_aabb[0] > al2_aabb[3]) {
      double tmp = al2_aabb[0];
      al2_aabb[0] = al2_aabb[3];
      al2_aabb[3] = tmp;
    }
    if (al2_aabb[1] > al2_aabb[4]) {
      double tmp = al2_aabb[1];
      al2_aabb[1] = al2_aabb[4];
      al2_aabb[4] = tmp;
    }
    if (al2_aabb[2] > al2_aabb[5]) {
      double tmp = al2_aabb[2];
      al2_aabb[2] = al2_aabb[5];
      al2_aabb[5] = tmp;
    }
    al2_aabb[0] += getX();
    al2_aabb[1] += getY();
    al2_aabb[2] += getZ();
    al2_aabb[3] += getX();
    al2_aabb[4] += getY();
    al2_aabb[5] += getZ();
  }
  public static boolean crossTestAl2(float x, float y, float z) {
    double min_x = al2_aabb[0];
    double min_y = al2_aabb[1];
    double min_z = al2_aabb[2];
    double max_x = al2_aabb[3];
    double max_y = al2_aabb[4];
    double max_z = al2_aabb[5];
    return (   (float)min_x <= x && (float)max_x >= x
            && (float)min_y <= y && (float)max_y >= y
            && (float)min_z <= z && (float)max_z >= z);
  }
  public void schoolBoundingBox() {
    coordUtil.setMatrixRotateZ(x_angle);
    synchronized (mScratch4f_1) {
      synchronized (mScratch4f_2) {
        coordUtil.affine((float)aabb_org[0] - (float)school_dist,
                         (float)aabb_org[1] - (float)school_dist, 
                         (float)aabb_org[2] - (float)school_dist, 
                         mScratch4f_1);
        coordUtil.setMatrixRotateY(y_angle);
        coordUtil.affine(mScratch4f_1[0],mScratch4f_1[1], mScratch4f_1[2], mScratch4f_2);
        sch_aabb[0] = mScratch4f_2[0];
        sch_aabb[1] = mScratch4f_2[1];
        sch_aabb[2] = mScratch4f_2[2];
      }
    }
    coordUtil.setMatrixRotateZ(x_angle);
    synchronized (mScratch4f_1) {
      synchronized (mScratch4f_2) {
        coordUtil.affine((float)aabb_org[3] + (float)school_dist,
                         (float)aabb_org[4] + (float)school_dist, 
                         (float)aabb_org[5] + (float)school_dist, 
                         mScratch4f_1);
        coordUtil.setMatrixRotateY(y_angle);
        coordUtil.affine(mScratch4f_1[0],mScratch4f_1[1], mScratch4f_1[2], mScratch4f_2);
        sch_aabb[3] = mScratch4f_2[0];
        sch_aabb[4] = mScratch4f_2[1];
        sch_aabb[5] = mScratch4f_2[2];
      }
    }
    if (sch_aabb[0] > sch_aabb[3]) {
      double tmp = sch_aabb[0];
      sch_aabb[0] = sch_aabb[3];
      sch_aabb[3] = tmp;
    }
    if (sch_aabb[1] > sch_aabb[4]) {
      double tmp = sch_aabb[1];
      sch_aabb[1] = sch_aabb[4];
      sch_aabb[4] = tmp;
    }
    if (sch_aabb[2] > sch_aabb[5]) {
      double tmp = sch_aabb[2];
      sch_aabb[2] = sch_aabb[5];
      sch_aabb[5] = tmp;
    }
    sch_aabb[0] += getX();
    sch_aabb[1] += getY();
    sch_aabb[2] += getZ();
    sch_aabb[3] += getX();
    sch_aabb[4] += getY();
    sch_aabb[5] += getZ();
  }
  public static boolean crossTestSch(float x, float y, float z) {
    double min_x = sch_aabb[0];
    double min_y = sch_aabb[1];
    double min_z = sch_aabb[2];
    double max_x = sch_aabb[3];
    double max_y = sch_aabb[4];
    double max_z = sch_aabb[5];
    return (   (float)min_x <= x && (float)max_x >= x
            && (float)min_y <= y && (float)max_y >= y
            && (float)min_z <= z && (float)max_z >= z);
  }
  public void cohesionBoundingBox() {
    coordUtil.setMatrixRotateZ(x_angle);
    synchronized (mScratch4f_1) {
      synchronized (mScratch4f_2) {
        coordUtil.affine((float)aabb_org[0] - (float)cohesion_dist,
                         (float)aabb_org[1] - (float)cohesion_dist, 
                         (float)aabb_org[2] - (float)cohesion_dist, 
                         mScratch4f_1);
        coordUtil.setMatrixRotateY(y_angle);
        coordUtil.affine(mScratch4f_1[0],mScratch4f_1[1], mScratch4f_1[2], mScratch4f_2);
        coh_aabb[0] = mScratch4f_2[0];
        coh_aabb[1] = mScratch4f_2[1];
        coh_aabb[2] = mScratch4f_2[2];
      }
    }
    coordUtil.setMatrixRotateZ(x_angle);
    synchronized (mScratch4f_1) {
      synchronized (mScratch4f_2) {
        coordUtil.affine((float)aabb_org[3] + (float)cohesion_dist,
                         (float)aabb_org[4] + (float)cohesion_dist, 
                         (float)aabb_org[5] + (float)cohesion_dist, 
                         mScratch4f_1);
        coordUtil.setMatrixRotateY(y_angle);
        coordUtil.affine(mScratch4f_1[0],mScratch4f_1[1], mScratch4f_1[2], mScratch4f_2);
        coh_aabb[3] = mScratch4f_2[0];
        coh_aabb[4] = mScratch4f_2[1];
        coh_aabb[5] = mScratch4f_2[2];
      }
    }
    if (coh_aabb[0] > coh_aabb[3]) {
      double tmp = coh_aabb[0];
      coh_aabb[0] = coh_aabb[3];
      coh_aabb[3] = tmp;
    }
    if (coh_aabb[1] > coh_aabb[4]) {
      double tmp = coh_aabb[1];
      coh_aabb[1] = coh_aabb[4];
      coh_aabb[4] = tmp;
    }
    if (coh_aabb[2] > coh_aabb[5]) {
      double tmp = coh_aabb[2];
      coh_aabb[2] = coh_aabb[5];
      coh_aabb[5] = tmp;
    }
    coh_aabb[0] += getX();
    coh_aabb[1] += getY();
    coh_aabb[2] += getZ();
    coh_aabb[3] += getX();
    coh_aabb[4] += getY();
    coh_aabb[5] += getZ();
  }
  public static boolean crossTestCoh(float x, float y, float z) {
    double min_x = coh_aabb[0];
    double min_y = coh_aabb[1];
    double min_z = coh_aabb[2];
    double max_x = coh_aabb[3];
    double max_y = coh_aabb[4];
    double max_z = coh_aabb[5];
    return (   (float)min_x <= x && (float)max_x >= x
            && (float)min_y <= y && (float)max_y >= y
            && (float)min_z <= z && (float)max_z >= z);
  }
  
  /**
   * Get turnDirection.
   *
   * @return turnDirection as TURN_DIRECTION.
   */
  public TURN_DIRECTION getTurnDirection()
  {
      return turnDirection;
  }
  
  /**
   * Set turnDirection.
   *
   * @param turnDirection the value to set.
   */
  public void setTurnDirection(TURN_DIRECTION turnDirection)
  {
      this.turnDirection = turnDirection;
  }
}
