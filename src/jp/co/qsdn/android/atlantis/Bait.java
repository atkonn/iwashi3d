package jp.co.qsdn.android.atlantis;

/**
 * 餌
 */
public class Bait {
  private float[] pos = {0f,0f,0f};
  public Bait() {
  }
  public Bait(float x, float y, float z) {
    setX(x);
    setY(y);
    setZ(z);
  }
  public float getX() {
    return pos[0];
  }
  public float getY() {
    return pos[1];
  }
  public float getZ() {
    return pos[2];
  }
  public void setX(float x) {
    this.pos[0] = x;
  }
  public void setY(float x) {
    this.pos[1] = x;
  }
  public void setZ(float x) {
    this.pos[2] = x;
  }
}
