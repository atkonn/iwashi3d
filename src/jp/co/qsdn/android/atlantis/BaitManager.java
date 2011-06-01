package jp.co.qsdn.android.atlantis;


import java.util.Stack;

/**
 * 餌
 */
public class BaitManager {
  private int max_count = 100;
  private int nowCount = 0;
  private Stack<Bait> stack = new Stack<Bait>();
  public void addBait(int count, int x, int y) {
    for (int ii=0; ii<count; ii++) {
      stack.push(new Bait());
    }
  }
}
