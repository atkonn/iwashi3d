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
import android.util.Log;

public class Iwashi {
  private static final String TAG = Iwashi.class.getName();
  private FloatBuffer mVertexBuffer;
  private final FloatBuffer mTextureBuffer;  
  private final FloatBuffer mNormalBuffer;  
  private static int texid;
  private long tick = 0;
  private float scale = 0.1035156288414f;

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

  private void animate() {
    if (tick == 0) {
      tick = System.currentTimeMillis();
    }
    else {
      long current = System.currentTimeMillis();
      float nf = (float)(current % 10000);
      long ni = (long)Math.floor(nf);
      float w = 2f*((float)Math.PI)*(nf - (float)ni);
      float s = (float)Math.sin((double)nf) * scale;
       
      //303 101 {4.725803, 1.603915, -0.000000}
      //309 103 {4.725803, 1.603915, -0.000000}
      IwashiData.vertices[2+3*101] = IwashiData.org_vertices[2+3*101] + (1.0f * s);
      IwashiData.vertices[2+3*103] = IwashiData.org_vertices[2+3*103] + (1.0f * s);

      //300 100 {4.734376, 1.502248, -0.009085}
      //312 104 {4.727424, 1.502259, 0.009085}
      //1290 430 {4.727424, 1.502259, 0.009085}
      //1317 439 {4.734376, 1.502248, -0.009085}
      IwashiData.vertices[2+3*100] = IwashiData.org_vertices[2+3*100] + (1.0f * s);
      IwashiData.vertices[2+3*104] = IwashiData.org_vertices[2+3*104] + (1.0f * s);
      IwashiData.vertices[2+3*430] = IwashiData.org_vertices[2+3*430] + (1.0f * s);
      IwashiData.vertices[2+3*439] = IwashiData.org_vertices[2+3*439] + (1.0f * s);

      //318 106 {4.497553, 1.130905, 0.009254}
      //1293 431 {4.497553, 1.130905, 0.009254}
      //1299 433 {4.497553, 1.130905, 0.009254}
      IwashiData.vertices[2+3*106] = IwashiData.org_vertices[2+3*106] + (1.0f * s);
      IwashiData.vertices[2+3*431] = IwashiData.org_vertices[2+3*431] + (1.0f * s);
      IwashiData.vertices[2+3*433] = IwashiData.org_vertices[2+3*433] + (1.0f * s);

      // 096 032 {3.943874, 0.549283, 0.006373}
      // 102 034 {3.943874, 0.549283, 0.006373}
      // 132 044 {3.931480, 0.549297, -0.006373}
      // 138 046 {3.931480, 0.549297, -0.006373}
      // 285 095 {3.943874, 0.549283, 0.006373}
      // 288 096 {3.943874, 0.549283, 0.006373}
      // 321 107 {3.931480, 0.549297, -0.006373}
      // 324 108 {3.931480, 0.549297, -0.006373}
      {
        float width = 0.6f * s;
        IwashiData.vertices[2+3* 32] = IwashiData.org_vertices[2+3* 32] + width;
        IwashiData.vertices[2+3* 34] = IwashiData.org_vertices[2+3* 34] + width;
        IwashiData.vertices[2+3* 44] = IwashiData.org_vertices[2+3* 44] + width;
        IwashiData.vertices[2+3* 46] = IwashiData.org_vertices[2+3* 46] + width;
        IwashiData.vertices[2+3* 95] = IwashiData.org_vertices[2+3* 95] + width;
        IwashiData.vertices[2+3* 96] = IwashiData.org_vertices[2+3* 96] + width;
        IwashiData.vertices[2+3*107] = IwashiData.org_vertices[2+3*107] + width;
        IwashiData.vertices[2+3*108] = IwashiData.org_vertices[2+3*108] + width;
      }
   
      // 264 088 {4.587202, 0.163779, 0.009247}
      // 276 092 {4.597796, 0.163766, -0.009247}
      // 282 094 {4.597796, 0.163766, -0.009247}
      // 327 109 {4.587202, 0.163779, 0.009247}
      {
        float width = 1.0f * s;
        IwashiData.vertices[2+3* 88] = IwashiData.org_vertices[2+3* 88] + width;
        IwashiData.vertices[2+3* 92] = IwashiData.org_vertices[2+3* 92] + width;
        IwashiData.vertices[2+3* 94] = IwashiData.org_vertices[2+3* 94] + width;
        IwashiData.vertices[2+3*109] = IwashiData.org_vertices[2+3*109] + width;
      }
      // 267 089 {4.865566, -0.206893, 0.009037}
      // 273 091 {4.871437, -0.206896, -0.009037}
      //1329 443 {4.871437, -0.206896, -0.009037}
      //1335 445 {4.871437, -0.206896, -0.009037}
      //1344 448 {4.865566, -0.206893, 0.009037}
      //1350 450 {4.865566, -0.206893, 0.009037}
      {
        float width = 1.0f * s;
        IwashiData.vertices[2+3* 89] = IwashiData.org_vertices[2+3* 89] + width;
        IwashiData.vertices[2+3* 91] = IwashiData.org_vertices[2+3* 91] + width;
        IwashiData.vertices[2+3*443] = IwashiData.org_vertices[2+3*443] + width;
        IwashiData.vertices[2+3*445] = IwashiData.org_vertices[2+3*445] + width;
        IwashiData.vertices[2+3*448] = IwashiData.org_vertices[2+3*448] + width;
        IwashiData.vertices[2+3*450] = IwashiData.org_vertices[2+3*450] + width;
      }
      //291 097 {4.508326, 1.130889, -0.009254}
      //1308 436 {4.508326, 1.130889, -0.009254}
      //1314 438 {4.508326, 1.130889, -0.009254}
      {
        float width = 1.0f * s;
        IwashiData.vertices[2+3* 97] = IwashiData.org_vertices[2+3* 97] + width;
        IwashiData.vertices[2+3*436] = IwashiData.org_vertices[2+3*436] + width;
        IwashiData.vertices[2+3*438] = IwashiData.org_vertices[2+3*438] + width;
      }
      //1326 442 {4.868408, -0.319613, -0.000000}
      //1353 451 {4.868408, -0.319613, -0.000000}
      {
        float width = 1.0f * s;
        IwashiData.vertices[2+3*442] = IwashiData.org_vertices[2+3*442] + width;
        IwashiData.vertices[2+3*451] = IwashiData.org_vertices[2+3*451] + width;
      }
      // 231 077 {4.189324, -0.027536, -0.000000}
      // 237 079 {4.189324, -0.027536, -0.000000}
      //1323 441 {4.189324, -0.027536, -0.000000}
      //1332 444 {4.189324, -0.027536, -0.000000}
      //1347 449 {4.189324, -0.027536, -0.000000}
      //1356 452 {4.189324, -0.027536, -0.000000}
      {
        float width = 0.6f * s;
        IwashiData.vertices[2+3* 77] = IwashiData.org_vertices[2+3* 77] + width;
        IwashiData.vertices[2+3* 79] = IwashiData.org_vertices[2+3* 79] + width;
        IwashiData.vertices[2+3*441] = IwashiData.org_vertices[2+3*441] + width;
        IwashiData.vertices[2+3*444] = IwashiData.org_vertices[2+3*444] + width;
        IwashiData.vertices[2+3*449] = IwashiData.org_vertices[2+3*449] + width;
        IwashiData.vertices[2+3*452] = IwashiData.org_vertices[2+3*452] + width;
      }
      //084 028 {3.994344, 0.212614, -0.011905}
      //093 031 {3.994344, 0.212614, -0.011905}
      //141 047 {3.985378, 0.212621, -0.040541}
      //150 050 {3.985378, 0.212621, -0.040541}
      //228 076 {3.985378, 0.212621, -0.040541}
      //240 080 {3.994344, 0.212614, -0.011905}
      //261 087 {3.985378, 0.212621, -0.040541}
      //270 090 {3.994344, 0.212614, -0.011905}
      //279 093 {3.994344, 0.212614, -0.011905}
      //330 110 {3.985378, 0.212621, -0.040541}
      //1338 446 {3.994344, 0.212614, -0.011905}
      //1341 447 {3.985378, 0.212621, -0.040541}
      {
        float width = 0.6f * s;
        IwashiData.vertices[2+3* 28] = IwashiData.org_vertices[2+3* 28] + width;
        IwashiData.vertices[2+3* 31] = IwashiData.org_vertices[2+3* 31] + width;
        IwashiData.vertices[2+3* 47] = IwashiData.org_vertices[2+3* 47] + width;
        IwashiData.vertices[2+3* 50] = IwashiData.org_vertices[2+3* 50] + width;
        IwashiData.vertices[2+3* 76] = IwashiData.org_vertices[2+3* 76] + width;
        IwashiData.vertices[2+3* 80] = IwashiData.org_vertices[2+3* 80] + width;
        IwashiData.vertices[2+3* 87] = IwashiData.org_vertices[2+3* 87] + width;
        IwashiData.vertices[2+3* 90] = IwashiData.org_vertices[2+3* 90] + width;
        IwashiData.vertices[2+3* 93] = IwashiData.org_vertices[2+3* 93] + width;
        IwashiData.vertices[2+3*110] = IwashiData.org_vertices[2+3*110] + width;
        IwashiData.vertices[2+3*446] = IwashiData.org_vertices[2+3*446] + width;
        IwashiData.vertices[2+3*447] = IwashiData.org_vertices[2+3*447] + width;
      }
      //105 035 {4.001855, 0.959487, -0.012866}
      //111 037 {4.001855, 0.959487, -0.012866}
      //246 082 {4.001855, 0.959487, -0.012866}
      //294 098 {4.001855, 0.959487, -0.012866}
      //1305 435 {4.001855, 0.959487, -0.012866}
      // XXXX
      //120 040 {3.992240, 0.959496, -0.039771}
      //129 043 {3.992240, 0.959496, -0.039771}
      //258 086 {3.992240, 0.959496, -0.039771}
      //315 105 {3.992240, 0.959496, -0.039771}
      //1302 434 {3.992240, 0.959496, -0.039771}
      {
        float width = 0.6f * s;
        IwashiData.vertices[2+3* 35] = IwashiData.org_vertices[2+3* 35] + width;
        IwashiData.vertices[2+3* 37] = IwashiData.org_vertices[2+3* 37] + width;
        IwashiData.vertices[2+3* 82] = IwashiData.org_vertices[2+3* 82] + width;
        IwashiData.vertices[2+3* 98] = IwashiData.org_vertices[2+3* 98] + width;
        IwashiData.vertices[2+3*435] = IwashiData.org_vertices[2+3*435] + width;

        IwashiData.vertices[2+3* 40] = IwashiData.org_vertices[2+3* 40] + width;
        IwashiData.vertices[2+3* 43] = IwashiData.org_vertices[2+3* 43] + width;
        IwashiData.vertices[2+3* 86] = IwashiData.org_vertices[2+3* 86] + width;
        IwashiData.vertices[2+3*105] = IwashiData.org_vertices[2+3*105] + width;
        IwashiData.vertices[2+3*434] = IwashiData.org_vertices[2+3*434] + width;
      }
      // 249 083 {4.250497, 1.351480, -0.030413}
      // 255 085 {4.250497, 1.351480, -0.030413}
      // 297 099 {4.250497, 1.351480, -0.030413}
      // 306 102 {4.250497, 1.351480, -0.030413}
      //1287 429 {4.250497, 1.351480, -0.030413}
      //1296 432 {4.250497, 1.351480, -0.030413}
      //1311 437 {4.250497, 1.351480, -0.030413}
      //1320 440 {4.250497, 1.351480, -0.030413}
      {
        float width = 0.6f * s;
        IwashiData.vertices[2+3* 83] = IwashiData.org_vertices[2+3* 83] + width;
        IwashiData.vertices[2+3* 85] = IwashiData.org_vertices[2+3* 85] + width;
        IwashiData.vertices[2+3* 99] = IwashiData.org_vertices[2+3* 99] + width;
        IwashiData.vertices[2+3*102] = IwashiData.org_vertices[2+3*102] + width;
        IwashiData.vertices[2+3*429] = IwashiData.org_vertices[2+3*429] + width;
        IwashiData.vertices[2+3*432] = IwashiData.org_vertices[2+3*432] + width;
        IwashiData.vertices[2+3*437] = IwashiData.org_vertices[2+3*437] + width;
        IwashiData.vertices[2+3*440] = IwashiData.org_vertices[2+3*440] + width;
      }
      //114 038 {3.393267, 0.860405, -0.028042}
      //117 039 {3.393267, 0.860405, -0.028042}
      //243 081 {3.393267, 0.860405, -0.028042}
      //252 084 {3.393267, 0.860405, -0.028042}
      //705 235 {3.393267, 0.860405, -0.028042}
      //714 238 {3.393267, 0.860405, -0.028042}
      {
        float width = 0.5f * s;
        IwashiData.vertices[2+3* 38] = IwashiData.org_vertices[2+3* 38] + width;
        IwashiData.vertices[2+3* 39] = IwashiData.org_vertices[2+3* 39] + width;
        IwashiData.vertices[2+3* 81] = IwashiData.org_vertices[2+3* 81] + width;
        IwashiData.vertices[2+3* 84] = IwashiData.org_vertices[2+3* 84] + width;
        IwashiData.vertices[2+3*235] = IwashiData.org_vertices[2+3*235] + width;
        IwashiData.vertices[2+3*238] = IwashiData.org_vertices[2+3*238] + width;
      }
      //081 027 {3.465865, 0.220323, -0.023851}
      //144 048 {3.465865, 0.220323, -0.023851}
      //225 075 {3.465865, 0.220323, -0.023851}
      //234 078 {3.465865, 0.220323, -0.023851}
      //660 220 {3.465865, 0.220323, -0.023851}
      //690 230 {3.465865, 0.220323, -0.023851}
      //696 232 {3.465865, 0.220323, -0.023851}
      //720 240 {3.465865, 0.220323, -0.023851}
      {
        float width = 0.5f * s;
        IwashiData.vertices[2+3* 27] = IwashiData.org_vertices[2+3* 27] + width;
        IwashiData.vertices[2+3* 48] = IwashiData.org_vertices[2+3* 48] + width;
        IwashiData.vertices[2+3* 75] = IwashiData.org_vertices[2+3* 75] + width;
        IwashiData.vertices[2+3* 78] = IwashiData.org_vertices[2+3* 78] + width;
        IwashiData.vertices[2+3*220] = IwashiData.org_vertices[2+3*220] + width;
        IwashiData.vertices[2+3*230] = IwashiData.org_vertices[2+3*230] + width;
        IwashiData.vertices[2+3*232] = IwashiData.org_vertices[2+3*232] + width;
        IwashiData.vertices[2+3*240] = IwashiData.org_vertices[2+3*240] + width;
      }
      //663 221 {3.128526, 0.180488, -0.023306}
      //669 223 {3.128526, 0.180488, -0.023306}
      //678 226 {3.128526, 0.180488, -0.023306}
      //687 229 {3.128526, 0.180488, -0.023306}
      {
        float width = 0.5f * s;
        IwashiData.vertices[2+3*221] = IwashiData.org_vertices[2+3*221] + width;
        IwashiData.vertices[2+3*223] = IwashiData.org_vertices[2+3*223] + width;
        IwashiData.vertices[2+3*226] = IwashiData.org_vertices[2+3*226] + width;
        IwashiData.vertices[2+3*229] = IwashiData.org_vertices[2+3*229] + width;
      }
      //123 041 {2.897367, 0.545929, -0.111540}
      //126 042 {2.897367, 0.545929, -0.111540}
      //135 045 {2.897367, 0.545929, -0.111540}
      //147 049 {2.897367, 0.545929, -0.111540}
      //201 067 {2.897367, 0.545929, -0.111540}
      //210 070 {2.897367, 0.545929, -0.111540}
      //222 074 {2.897367, 0.545929, -0.111540}
      //645 215 {2.897367, 0.545929, -0.111540}
      //654 218 {2.897367, 0.545929, -0.111540}
      //717 239 {2.897367, 0.545929, -0.111540}
      //726 242 {2.897367, 0.545929, -0.111540}
      //1371 457 {2.897367, 0.545929, -0.111540}
      {
        float width = 0.45f * s;
        IwashiData.vertices[2+3* 41] = IwashiData.org_vertices[2+3* 41] + width;
        IwashiData.vertices[2+3* 42] = IwashiData.org_vertices[2+3* 42] + width;
        IwashiData.vertices[2+3* 45] = IwashiData.org_vertices[2+3* 45] + width;
        IwashiData.vertices[2+3* 49] = IwashiData.org_vertices[2+3* 49] + width;
        IwashiData.vertices[2+3* 67] = IwashiData.org_vertices[2+3* 67] + width;
        IwashiData.vertices[2+3* 70] = IwashiData.org_vertices[2+3* 70] + width;
        IwashiData.vertices[2+3* 74] = IwashiData.org_vertices[2+3* 74] + width;
        IwashiData.vertices[2+3*215] = IwashiData.org_vertices[2+3*215] + width;
        IwashiData.vertices[2+3*218] = IwashiData.org_vertices[2+3*218] + width;
        IwashiData.vertices[2+3*239] = IwashiData.org_vertices[2+3*239] + width;
        IwashiData.vertices[2+3*242] = IwashiData.org_vertices[2+3*242] + width;
        IwashiData.vertices[2+3*457] = IwashiData.org_vertices[2+3*457] + width;
      }
      //672 224 {2.755704, 0.041151, -0.025086}
      //675 225 {2.755704, 0.041151, -0.025086}
      //1182 394 {2.755704, 0.041151, -0.025086}
      //1188 396 {2.755704, 0.041151, -0.025086}
      //1203 401 {2.755704, 0.041151, -0.025086}
      //1209 403 {2.755704, 0.041151, -0.025086}
      {
        float width = 0.45f * s;
        IwashiData.vertices[2+3*224] = IwashiData.org_vertices[2+3*224] + width;
        IwashiData.vertices[2+3*225] = IwashiData.org_vertices[2+3*225] + width;
        IwashiData.vertices[2+3*394] = IwashiData.org_vertices[2+3*394] + width;
        IwashiData.vertices[2+3*396] = IwashiData.org_vertices[2+3*396] + width;
        IwashiData.vertices[2+3*401] = IwashiData.org_vertices[2+3*401] + width;
        IwashiData.vertices[2+3*403] = IwashiData.org_vertices[2+3*403] + width;
      }
      // 606 202 {2.601744, 0.072730, -0.082255}
      // 615 205 {2.608089, 0.072728, 0.042083}
      // 624 208 {2.608089, 0.072728, 0.042083}
      // 648 216 {2.601744, 0.072730, -0.082255}
      // 657 219 {2.601744, 0.072730, -0.082255}
      // 666 222 {2.601744, 0.072730, -0.082255}
      // 681 227 {2.608089, 0.072728, 0.042083}
      // 684 228 {2.608089, 0.072728, 0.042083}
      // 693 231 {2.608089, 0.072728, 0.042083}
      // 723 241 {2.601744, 0.072730, -0.082255}
      //1191 397 {2.608089, 0.072728, 0.042083}
      //1200 400 {2.601744, 0.072730, -0.082255}

    ByteBuffer vbb = ByteBuffer.allocateDirect(IwashiData.vertices.length * 4);
    vbb.order(ByteOrder.nativeOrder());
    mVertexBuffer = vbb.asFloatBuffer();
    mVertexBuffer.put(IwashiData.vertices);
    mVertexBuffer.position(0);

      tick = current;
    }
  }

  int angle = 0;
  public void draw(GL10 gl10) {
    gl10.glPushMatrix();

    animate();

    // forDebug
    //gl10.glRotatef(90.0f, 0.0f, 1.0f, 0.0f);
    gl10.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
    //gl10.glRotatef((float)(angle % 360), 0.0f, 0.0f, 1.0f);
    //angle += 10;
    //gl10.glRotatef(90.0f, 0.0f, 0.0f, 1.0f);

    gl10.glColor4f(1,1,1,1);
    gl10.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
    gl10.glNormalPointer(GL10.GL_FLOAT, 0, mNormalBuffer);
    gl10.glEnable(GL10.GL_TEXTURE_2D);
    gl10.glBindTexture(GL10.GL_TEXTURE_2D, texid);
    gl10.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTextureBuffer);
    gl10.glDrawArrays(GL10.GL_TRIANGLES, 0, IwashiData.iwashiNumVerts);
    gl10.glPopMatrix();
  }
}
