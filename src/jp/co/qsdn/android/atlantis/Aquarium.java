package jp.co.qsdn.android.atlantis;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

/**
 * 水槽
 */
public class Aquarium {
  // 水槽の大きさ（案）
  // 10.0f >= x  >= -10.0f
  // 8.0f >= y >= 0.0f
  // -50.0f > z >= 0.0f
  public static Float min_x = new Float(-4.0f);
  public static Float max_x = new Float(4.0f);

  public static Float min_y = new Float(-4.0f);
  public static Float max_y = new Float(4.0f);

  public static Float min_z = new Float(-4.0f);
  public static Float max_z = new Float(4.0f);

  // 水槽の中心
  public static float[] center = { 0.0f, 0.0f, 0.0f };

  private final FloatBuffer mVertexBuffer1;
  private final FloatBuffer mVertexBuffer2;
  private final FloatBuffer mVertexBuffer3;
  private final FloatBuffer mVertexBuffer4;

  public Aquarium() {
    /* 背面 */
    float[] vertices1 = {
      -0.2f, -0.2f, -0.1f,
      0.2f, -0.2f, -0.1f,
      0.2f, 0.2f, -0.1f,
      -0.2f, 0.2f, -0.1f,
    };
    /* 前面 */
    float[] vertices2 = {
      -0.2f, -0.2f, 0.1f,
      0.2f, -0.2f, 0.1f,
      0.2f, 0.2f, 0.1f,
      -0.2f, 0.2f, 0.1f,
    };
    /* 左面 */
    float[] vertices3 = {
      -0.2f, -0.2f, -0.1f,
      -0.2f, -0.2f, 0.1f,
      -0.2f, 0.2f, 0.1f,
      -0.2f, 0.2f, -0.1f,
    };
    /* 右面 */
    float[] vertices4 = {
      0.2f, -0.2f, -0.1f,
      0.2f, -0.2f, 0.1f,
      0.2f, 0.2f, 0.1f,
      0.2f, 0.2f, -0.1f,
    };

    ByteBuffer vbb = ByteBuffer.allocateDirect(vertices1.length * 4);
    vbb.order(ByteOrder.nativeOrder());
    mVertexBuffer1 = vbb.asFloatBuffer();
    mVertexBuffer1.put(vertices1);
    mVertexBuffer1.position(0);
    vbb = ByteBuffer.allocateDirect(vertices2.length * 4);
    vbb.order(ByteOrder.nativeOrder());
    mVertexBuffer2 = vbb.asFloatBuffer();
    mVertexBuffer2.put(vertices2);
    mVertexBuffer2.position(0);
    vbb = ByteBuffer.allocateDirect(vertices3.length * 4);
    vbb.order(ByteOrder.nativeOrder());
    mVertexBuffer3 = vbb.asFloatBuffer();
    mVertexBuffer3.put(vertices3);
    mVertexBuffer3.position(0);
    vbb = ByteBuffer.allocateDirect(vertices4.length * 4);
    vbb.order(ByteOrder.nativeOrder());
    mVertexBuffer4 = vbb.asFloatBuffer();
    mVertexBuffer4.put(vertices4);
    mVertexBuffer4.position(0);
  }

  

  public void draw(GL10 gl10) {
    gl10.glPushMatrix();
    gl10.glDisable(GL10.GL_DEPTH_TEST);

    /*-----------------------------------------------------------------------*/
    /* 頂点座標バッファを読み込む                                            */
    /*-----------------------------------------------------------------------*/
    gl10.glPushMatrix();
    gl10.glDisable(GL10.GL_CULL_FACE);
    gl10.glScalef(40f,40f,40f);
    gl10.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer1);
    gl10.glPointSize(1f);
    gl10.glColor4f(1f,0,0,1f);
    gl10.glDrawArrays(GL10.GL_LINE_LOOP, 0, 4);
    gl10.glPopMatrix();

    gl10.glPushMatrix();
    gl10.glDisable(GL10.GL_CULL_FACE);
    gl10.glScalef(40f,40f,40f);
    gl10.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer2);
    gl10.glPointSize(1f);
    gl10.glColor4f(1f,0,0,1f);
    gl10.glDrawArrays(GL10.GL_LINE_LOOP, 0, 4);
    gl10.glPopMatrix();

    gl10.glPushMatrix();
    gl10.glDisable(GL10.GL_CULL_FACE);
    gl10.glScalef(40f,40f,40f);
    gl10.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer3);
    gl10.glPointSize(20f);
    gl10.glColor4f(1f,0,0,0.5f);
    gl10.glDrawArrays(GL10.GL_LINE_LOOP, 0, 4);
    gl10.glPopMatrix();

    gl10.glPushMatrix();
    gl10.glDisable(GL10.GL_CULL_FACE);
    gl10.glScalef(40f,40f,40f);
    gl10.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer4);
    gl10.glPointSize(20f);
    gl10.glColor4f(1f,0,0,0.5f);
    gl10.glDrawArrays(GL10.GL_LINE_LOOP, 0, 4);
    gl10.glPopMatrix();

    gl10.glEnable(GL10.GL_DEPTH_TEST);
    gl10.glPopMatrix();
  }
}
