package jp.co.qsdn.android.iwashi3d;


import java.util.Stack;
import android.util.Log;

/**
 * 餌
 */
public class BaitManager {
  private static final String TAG = BaitManager.class.getName();
  private int max_count = 100;
  private int nowCount = 0;
  private Stack<Bait> stack = new Stack<Bait>();
  public void addBait(float x, float y, float z) {
    synchronized (this) {
      if (nowCount < max_count) {
        Log.d(TAG,"("+x+","+y+","+z+")に餌を追加します");
        stack.push(new Bait(x,y,z));
        nowCount++;
      }
    }
  }

  public Bait getBait() {
    synchronized (this) {
      if (nowCount!=0) {
        return stack.get(0);
      }
      return null;
    }
  }

  public void eat(Bait bait) {
    synchronized (this) {
      // 要素が無かった場合は無視
      if (stack.remove(bait)) {
        // 消せた
        nowCount--;
        Log.d(TAG,"("+bait.getX()+","+bait.getY()+","+bait.getZ()+")の餌を食べました");
      }
    }
  }
}
