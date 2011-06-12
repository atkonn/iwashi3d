package jp.co.qsdn.android.iwashi3d.model;

import android.util.Log;
import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

import jp.co.qsdn.android.iwashi3d.Aquarium;

public class Ground {
  private static final String TAG = Ground.class.getName();
  private final FloatBuffer mVertexBuffer;
  private final FloatBuffer mTextureBuffer;  
  private static int texid;
  private static Bitmap mBitmap;
  private int scratch_ii = 0;

  public Ground() {
    float one = 4.5f;
    float half = one / 2.0f;
    float vertices[] = {
      -one, -one,  one,   // 左下
       one, -one,  one,   // 右下
      -one, -one, -one,   // 左上
       one, -one, -one,   // 右上
    };

    float texCoords[] = {
      0.0f,1.0f,
      1.0f,1.0f,
      0.0f,0.0f,
      1.0f,0.0f,
    };

    ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
    vbb.order(ByteOrder.nativeOrder());
    mVertexBuffer = vbb.asFloatBuffer();
    mVertexBuffer.put(vertices);
    mVertexBuffer.position(0);


    ByteBuffer tbb = ByteBuffer.allocateDirect(texCoords.length * 4);
    tbb.order(ByteOrder.nativeOrder());
    mTextureBuffer = tbb.asFloatBuffer();
    mTextureBuffer.put(texCoords);
    mTextureBuffer.position(0);
  }

  public static void loadTexture(GL10 gl10, Context context, int resource) {
    mBitmap = BitmapFactory.decodeResource(context.getResources(), resource);
    int a[] = new int[1];
    gl10.glGenTextures(1, a, 0);
    gl10.glBindTexture(GL10.GL_TEXTURE_2D, a[0]);
    texid = a[0];
    GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, mBitmap, 0);
    gl10.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
    gl10.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
    // bmp.recycle();
  }

  float[] scratch4 = new float[4];
  public void draw(GL10 gl10, Model[] model) {
    /*-----------------------------------------------------------------------*/
    /* 背景描画                                                              */
    /*-----------------------------------------------------------------------*/
    gl10.glMatrixMode(GL10.GL_MODELVIEW);  // ModelView行列をクリア
    gl10.glPushMatrix();


    /*=======================================================================*/
    /* 環境光の材質色設定                                                    */
    /*=======================================================================*/
    scratch4[0] = 1.0f;
    scratch4[1] = 1.0f;
    scratch4[2] = 1.0f;
    scratch4[3] = 1.0f;
    gl10.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, scratch4, 0);
    /*=======================================================================*/
    /* 拡散反射光の色設定                                                    */
    /*=======================================================================*/
    gl10.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, scratch4, 0);
    /*=======================================================================*/
    /* 鏡面反射光の質感色設定                                                */
    /*=======================================================================*/
    scratch4[0] = 0.0f;
    scratch4[1] = 0.0f;
    scratch4[2] = 0.0f;
    gl10.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, scratch4, 0);

    gl10.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, 100f);

    Bitmap bmp = mBitmap.copy(Bitmap.Config.ARGB_8888,true);
    Canvas canvas = new Canvas(bmp);
    Paint p = new Paint();
    
    
    for (scratch_ii=0; scratch_ii<model.length; scratch_ii++) {
      if (model[scratch_ii].getY() < (Aquarium.min_y / 2.0f)) {
        float dist = (model[scratch_ii].getY() + (float)Math.abs(Aquarium.min_y));
        float x = (model[scratch_ii].getX() + (-Aquarium.min_x)) * 16f;
        float y = (model[scratch_ii].getZ() + (-Aquarium.min_z)) * 16f;
        float sz = model[scratch_ii].getSize() * 16f / 3f;
if (false) {
Log.d(TAG, ""
  + "dist:[" + dist + "]:"
  + "x:[" + x + "]:"
  + "y:[" + y + "]:"
  + "sz:[" + sz + "]:"
);
}
        p.setARGB((int)(0x22 - (0x22 * (dist/((float)Math.abs(Aquarium.min_y) / 2.0f)))), 
                  0, 
                  0, 
                  0);
        p.setStyle(Paint.Style.FILL);
//        RectF rect = new RectF(x-(sz/2.0f)-dist-2.0f, 
//                               y-(sz/4.0f)-dist, 
//                               x+(sz/2.0f)+dist+2.0f, 
//                               y+(sz/4.0f)+dist);
//        canvas.drawOval(rect, p);
        canvas.drawCircle(x,y,sz/2.0f+dist*2.0f, p);
      }
    }
    gl10.glBindTexture(GL10.GL_TEXTURE_2D, texid);
    GLUtils.texSubImage2D(GL10.GL_TEXTURE_2D, 0, 0, 0, bmp);
    bmp.recycle();


    /*-----------------------------------------------------------------------*/
    /* 頂点座標バッファを読み込む                                            */
    /*-----------------------------------------------------------------------*/
    gl10.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
    gl10.glEnable(GL10.GL_TEXTURE_2D);
    gl10.glBindTexture(GL10.GL_TEXTURE_2D, texid);
    gl10.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTextureBuffer);

    /*-----------------------------------------------------------------------*/
    /* 頂点描画                                                              */
    /*-----------------------------------------------------------------------*/
    gl10.glColor4f(1,1,1,1);
    gl10.glNormal3f(0,1,0);
    gl10.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);

    gl10.glPopMatrix();
  }
}
