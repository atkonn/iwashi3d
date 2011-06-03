package jp.co.qsdn.android.iwashi3d;

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
  public static Float min_x = new Float(-4.0f);
  public static Float max_x = new Float(4.0f);

  public static Float min_y = new Float(-4.0f);
  public static Float max_y = new Float(4.0f);

  public static Float min_z = new Float(-4.0f);
  public static Float max_z = new Float(4.0f);

  // 水槽の中心
  public static float[] center = { 0.0f, 0.0f, 0.0f };
}
