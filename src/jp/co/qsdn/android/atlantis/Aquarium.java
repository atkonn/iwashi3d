package jp.co.qsdn.android.atlantis;


/**
 * 水槽
 */
public class Aquarium {
  // 水槽の大きさ（案）
  // 10.0f >= x  >= -10.0f
  // 8.0f >= y >= 0.0f
  // -50.0f > z >= 0.0f
  public static Float min_x = new Float(-8.0f);
  public static Float max_x = new Float(8.0f);

  public static Float min_y = new Float(-2.0f);
  public static Float max_y = new Float(8.0f);

  public static Float min_z = new Float(-10.0f);
  public static Float max_z = new Float(-1.0f);

  // 水槽の中心
  public static float[] center = { 0.0f, 4.0f, -25f };

}
