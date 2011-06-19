package jp.co.qsdn.android.iwashi3d.tls;

import android.graphics.Bitmap;

public class BitmapContextImpl extends BitmapContext {
  private Bitmap bitmap;
  public Bitmap getBitmap() {
    return bitmap;
  }
  public void setBitmap(Bitmap bitmap) {
    this.bitmap = bitmap;
  }
}
