package jp.co.qsdn.android.atlantis;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

public class Background {
  private final IntBuffer mVertexBuffer;
  private final IntBuffer mTextureBuffer;  
  private static int texid;

  public Background() {
    int one = 65536;
    int half = one / 2;
    int vertices[] = {
      -one, -one, -one,   // 左下
       one, -one, -one,   // 右下
      -one,  one, -one,   // 左上
       one,  one, -one,   // 右上
    };

    int texCoords[] = {
      0,one,
      one,one,
      0,0,
      one,0,
    };

    ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
    vbb.order(ByteOrder.nativeOrder());
    mVertexBuffer = vbb.asIntBuffer();
    mVertexBuffer.put(vertices);
    mVertexBuffer.position(0);


    ByteBuffer tbb = ByteBuffer.allocateDirect(texCoords.length * 4);
    tbb.order(ByteOrder.nativeOrder());
    mTextureBuffer = tbb.asIntBuffer();
    mTextureBuffer.put(texCoords);
    mTextureBuffer.position(0);
  }

  static void loadTexture(GL10 gl10, Context context, int resource) {
    Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), resource);
    int a[] = new int[1];
    gl10.glGenTextures(1, a, 0);
    gl10.glBindTexture(GL10.GL_TEXTURE_2D, a[0]);
    texid = a[0];
    GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);
    gl10.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
    gl10.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
    bmp.recycle();
  }

  public void draw(GL10 gl10) {
    /*-----------------------------------------------------------------------*/
    /* 背景描画                                                              */
    /*-----------------------------------------------------------------------*/
    gl10.glMatrixMode(GL10.GL_MODELVIEW);  // ModelView行列をクリア
    gl10.glPushMatrix();
    gl10.glLoadIdentity();
    gl10.glMatrixMode(GL10.GL_PROJECTION); // Projection行列をクリア
    gl10.glPushMatrix();
    gl10.glLoadIdentity();
    gl10.glDisable(GL10.GL_DEPTH_TEST);    // DepthTestを無効にする


    /*=======================================================================*/
    /* 環境光の材質色設定                                                    */
    /*=======================================================================*/
    float[] mat_amb = { 
      1.0f, 
      1.0f, 
      1.0f,
      1.0f,
     };
    gl10.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, mat_amb, 0);
    /*=======================================================================*/
    /* 拡散反射光の色設定                                                    */
    /*=======================================================================*/
    float[] mat_diff = { 
      1.0f, 
      1.0f, 
      1.0f, 
      1.0f,
     };
    gl10.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, mat_diff, 0);
    /*=======================================================================*/
    /* 鏡面反射光の質感色設定                                                */
    /*=======================================================================*/
    float[] mat_spec = { 
      1.0f, 
      1.0f, 
      1.0f, 
      1.0f,
    };

    gl10.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, mat_spec, 0);
    gl10.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, 100f);

    /*-----------------------------------------------------------------------*/
    /* 頂点座標バッファを読み込む                                            */
    /*-----------------------------------------------------------------------*/
    gl10.glVertexPointer(3, GL10.GL_FIXED, 0, mVertexBuffer);
    gl10.glEnable(GL10.GL_TEXTURE_2D);
    gl10.glBindTexture(GL10.GL_TEXTURE_2D, texid);
    gl10.glTexCoordPointer(2, GL10.GL_FIXED, 0, mTextureBuffer);

    /*-----------------------------------------------------------------------*/
    /* 頂点描画                                                              */
    /*-----------------------------------------------------------------------*/
    gl10.glColor4f(1,1,1,1);
    gl10.glNormal3f(0,0,1);
    gl10.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
    
    gl10.glEnable(GL10.GL_DEPTH_TEST);     // DepthTestを有効にする
    gl10.glMatrixMode(GL10.GL_PROJECTION); // Projection行列を元に戻す
    gl10.glPopMatrix();
    gl10.glMatrixMode(GL10.GL_MODELVIEW);  // ModelView行列を元に戻す
    gl10.glPopMatrix();
  }
}
