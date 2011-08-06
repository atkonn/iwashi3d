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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

import jp.co.qsdn.android.iwashi3d.Aquarium;

public class Background {
  private final FloatBuffer mVertexBuffer;
  private final FloatBuffer mTextureBuffer;  
  private static boolean mTextureLoaded = false;

  private float[] mScratch4f = new float[4];

  public Background() {
    float oneW = Aquarium.max_x + 0.5f;
    float oneH = Aquarium.max_y + 0.5f;
    float vertices[] = {
      -oneW, -oneH, -oneW,
       oneW, -oneH, -oneW,
      -oneW,  oneH, -oneW,
       oneW,  oneH, -oneW,

       oneW, -oneH, -oneW,
       oneW, -oneH,  oneW,
       oneW,  oneH, -oneW,
       oneW,  oneH,  oneW,

       -oneW, -oneH,  oneW,
       -oneW, -oneH, -oneW,
       -oneW,  oneH,  oneW,
       -oneW,  oneH, -oneW,

       oneW, -oneH,  oneW,
      -oneW, -oneH,  oneW,
       oneW,  oneH,  oneW,
      -oneW,  oneH,  oneW,
    };

    float texCoords[] = {
      0.0f,1.0f,
      1.0f,1.0f,
      0.0f,0.0f,
      1.0f,0.0f,

      0.0f,1.0f,
      1.0f,1.0f,
      0.0f,0.0f,
      1.0f,0.0f,

      0.0f,1.0f,
      1.0f,1.0f,
      0.0f,0.0f,
      1.0f,0.0f,

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

  private static int[] textureIds = null;
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

  public void draw(GL10 gl10) {
    /*-----------------------------------------------------------------------*/
    /*-----------------------------------------------------------------------*/
    gl10.glMatrixMode(GL10.GL_MODELVIEW);
    gl10.glPushMatrix();

    /*=======================================================================*/
    /*=======================================================================*/
    synchronized(mScratch4f) {
      mScratch4f[0] = 0.07f;
      mScratch4f[1] = 0.07f;
      mScratch4f[2] = 0.07f;
      mScratch4f[3] = 1.0f;
      gl10.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, mScratch4f, 0);
    }
    /*=======================================================================*/
    /*=======================================================================*/
    synchronized (mScratch4f) {
      mScratch4f[0] = 0.07f;
      mScratch4f[1] = 0.07f;
      mScratch4f[2] = 0.07f;
      mScratch4f[3] = 1.0f;
      gl10.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, mScratch4f, 0);
    }
    /*=======================================================================*/
    /*=======================================================================*/
    synchronized (mScratch4f) {
      mScratch4f[0] = 0.07f;
      mScratch4f[1] = 0.07f;
      mScratch4f[2] = 0.07f;
      mScratch4f[3] = 1.0f;
      gl10.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, mScratch4f, 0);
    }
    gl10.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, 100f);

    /*-----------------------------------------------------------------------*/
    /*-----------------------------------------------------------------------*/
    gl10.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
    gl10.glEnable(GL10.GL_TEXTURE_2D);
    gl10.glBindTexture(GL10.GL_TEXTURE_2D, textureIds[0]);
    gl10.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTextureBuffer);

    /*-----------------------------------------------------------------------*/
    /*-----------------------------------------------------------------------*/
    gl10.glColor4f(1,1,1,1);
    gl10.glNormal3f(0,0,1);
    gl10.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);

    gl10.glNormal3f(-1,0,0);
    gl10.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 4, 4);

    gl10.glNormal3f(1,0,0);
    gl10.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 8, 4);

    gl10.glNormal3f(0,0,-1);
    gl10.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 12, 4);

    gl10.glPopMatrix();
  }
}
