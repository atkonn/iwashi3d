package jp.co.qsdn.android.atlantis.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

public class Iwashi {
  private final FloatBuffer mVertexBuffer;
  private final FloatBuffer mTextureBuffer;  
  private final FloatBuffer mNormalBuffer;  
  private static int texid;
  private int iwashiNumVerts = 1008;

  public Iwashi() {

    ByteBuffer vbb = ByteBuffer.allocateDirect(IwashiData.vertices.length * 4);
    vbb.order(ByteOrder.nativeOrder());
    mVertexBuffer = vbb.asFloatBuffer();
    mVertexBuffer.put(IwashiData.vertices);
    mVertexBuffer.position(0);

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

  public void draw(GL10 gl10) {
    gl10.glPushMatrix();
    gl10.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
    gl10.glNormalPointer(GL10.GL_FLOAT, 0, mNormalBuffer);
    gl10.glEnable(GL10.GL_TEXTURE_2D);
    gl10.glBindTexture(GL10.GL_TEXTURE_2D, texid);
    gl10.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTextureBuffer);
    gl10.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, iwashiNumVerts);
    gl10.glPopMatrix();
  }
}
