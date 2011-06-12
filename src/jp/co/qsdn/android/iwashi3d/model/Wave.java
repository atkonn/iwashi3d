package jp.co.qsdn.android.iwashi3d.model;

import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;
import android.opengl.GLUtils;
import android.graphics.Paint;
import android.graphics.Color;


public class Wave {
  private FloatBuffer mVertexBuffer;
  private final FloatBuffer mTextureBuffer;  
  private static int texid;
  private Bitmap mBitmap;

  float one = 4.5f;
  float half = one / 2.0f;
  private float scale = 0.05f;
  float vertices[] = {
     one, one - 0.2f,  one,   // 左下
    -one, one - 0.2f,  one,   // 右下
     one, one - 0.2f, -one,   // 左上
    -one, one - 0.2f, -one,   // 右上
  };
  float org_vertices[];

  public Wave() {

    float texCoords[] = {
      0.0f,1.0f,
      1.0f,1.0f,
      0.0f,0.0f,
      1.0f,0.0f,
    };

    mVertexBuffer = createFloatBuffer(vertices);
    mTextureBuffer = createFloatBuffer(texCoords);
    org_vertices = new float[vertices.length];
    System.arraycopy(vertices,0, org_vertices, 0, vertices.length);
  }

  public FloatBuffer createFloatBuffer(float[] vert) {
    ByteBuffer vbb = ByteBuffer.allocateDirect(vert.length * 4);
    vbb.order(ByteOrder.nativeOrder());
    FloatBuffer fb = vbb.asFloatBuffer();
    fb.put(vert);
    fb.position(0);
    return fb;
  }

  public static void loadTexture(GL10 gl10, Context context, int resource) {
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


  void animate() {
    long current = System.currentTimeMillis();
    float nf1 = (float)((current / 200) % 10000);
    float nf2 = (float)(((current + 500) / 200) % 10000);
    float nf3 = (float)(((current + 1000) / 200) % 10000);
    float nf4 = (float)(((current + 1500) / 200) % 10000);
    float s1 = (float)Math.sin((double)nf1) * scale;
    float s2 = (float)Math.sin((double)nf2) * scale;
    float s3 = (float)Math.sin((double)nf3) * scale;
    float s4 = (float)Math.sin((double)nf4) * scale;

    vertices[0*3+1] = org_vertices[0*3+1] + s1;
    vertices[1*3+1] = org_vertices[1*3+1] + s2;
    vertices[2*3+1] = org_vertices[2*3+1] + s3;
    vertices[3*3+1] = org_vertices[3*3+1] + s4;
    mVertexBuffer = createFloatBuffer(vertices);
  }

  public void calc() {
    animate();
  }

  public void draw(GL10 gl10) {

    /*-----------------------------------------------------------------------*/
    /* 背景描画                                                              */
    /*-----------------------------------------------------------------------*/
    gl10.glMatrixMode(GL10.GL_MODELVIEW);  // ModelView行列をクリア
    gl10.glPushMatrix();
    /* シースルーモード */
    gl10.glEnable(GL10.GL_BLEND);
    gl10.glBlendFunc(GL10.GL_ONE, GL10.GL_SRC_ALPHA);
    //gl10.glBlendFunc(GL10.GL_ONE_MINUS_DST_ALPHA,GL10.GL_DST_ALPHA);
    //gl10.glBlendFunc(GL10.GL_ZERO, GL10.GL_ONE_MINUS_SRC_ALPHA);
    //gl10.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_DST_ALPHA);


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
    gl10.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
    gl10.glEnable(GL10.GL_TEXTURE_2D);

    gl10.glBindTexture(GL10.GL_TEXTURE_2D, texid);
    gl10.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTextureBuffer);


    /*-----------------------------------------------------------------------*/
    /* 頂点描画                                                              */
    /*-----------------------------------------------------------------------*/
    gl10.glColor4f(1,1,1,1);
    gl10.glNormal3f(0,-1,0);
    gl10.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);

    gl10.glDisable(GL10.GL_BLEND);
    gl10.glPopMatrix();
  }
}
