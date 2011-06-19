package jp.co.qsdn.android.iwashi3d.tls;

import android.graphics.Bitmap;

public abstract class BitmapContext {
  private static ThreadLocal instance = new ThreadLocal() {  
    protected Object initialValue() {  
      return null;  
    }  
  };

  public abstract Bitmap getBitmap();
  public abstract void setBitmap(Bitmap bmp);
    
  @SuppressWarnings("unchecked")
  public static BitmapContext instance() {
    BitmapContext ctx = (BitmapContext)instance.get();
    if (ctx != null) {
      return ctx;
    }
    instance.set(new BitmapContextImpl());
    return (BitmapContext)instance.get();
  }
}
