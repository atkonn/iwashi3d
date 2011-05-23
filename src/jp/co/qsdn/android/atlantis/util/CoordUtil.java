package jp.co.qsdn.android.atlantis.util;

public class CoordUtil {

  protected float[][] matrix;

  public float convertDegreeToRadian(float angle) {
    return angle * ((float)Math.PI) / 180.0f;
  }
  
  protected void initMatrix() {
    matrix = new float[4][];
    for (int ii=0; ii<4; ii++) {
      matrix[ii] = new float[4];
    }
    for (int x = 0; x < 4; x++) {
      for (int y = 0; y < 4; y++) {
        matrix[x][y] = 0f;
      }
    }
  }
  public void setMatrixRotateX(float angle) {
    initMatrix();
    matrix[0][0] = 1f;
    matrix[1][1] = (float)Math.cos((double)convertDegreeToRadian(angle));
    matrix[2][1] = (float)-Math.sin((double)convertDegreeToRadian(angle));
    matrix[1][2] = (float)Math.sin((double)convertDegreeToRadian(angle));
    matrix[2][2] = (float)Math.cos((double)convertDegreeToRadian(angle));
    matrix[3][3] = 1f;
  }
  
  public void setMatrixRotateY(float angle) {
    initMatrix();
    matrix[0][0] = (float)Math.cos((double)convertDegreeToRadian(angle));
    matrix[2][0] = (float)Math.sin((double)convertDegreeToRadian(angle));
    matrix[1][1] = 1f;
    matrix[0][2] = (float)-Math.sin((double)convertDegreeToRadian(angle));
    matrix[2][2] = (float)Math.cos((double)convertDegreeToRadian(angle));
    matrix[3][3] = 1f;
  }
  
  public void setMatrixRotateZ(float angle) {
    initMatrix();
    matrix[0][0] = (float)Math.cos((double)convertDegreeToRadian(angle));
    matrix[1][0] = (float)-Math.sin((double)convertDegreeToRadian(angle));
    matrix[0][1] = (float)Math.sin((double)convertDegreeToRadian(angle));
    matrix[1][1] = (float)Math.cos((double)convertDegreeToRadian(angle));
    matrix[2][2] = 1f;
    matrix[3][3] = 1f;
  }
  
  public void setMatrixScale(float s1, float s2, float s3) {
    initMatrix();
    matrix[0][0] = s1;
    matrix[1][1] = s2;
    matrix[2][2] = s3;
    matrix[3][3] = 1f;
  }
  
  public void setMatrixTranslate(float t1, float t2, float t3) {
    initMatrix();
    matrix[0][0] = 1f; 
    matrix[3][0] = t1;
    matrix[1][1] = 1f; 
    matrix[3][1] = t2;
    matrix[2][2] = 1f; 
    matrix[3][2] = t3;
    matrix[3][3] = 1f;
  }
  
  // 座標変換（アフィン変換実行）
  public float[] affine(
      float x,
      float y,
      float z)
  {
    float[] ret = new float[3];
    ret[0] = 
          matrix[0][0] * x +
          matrix[1][0] * y +
          matrix[2][0] * z +
          matrix[3][0] * 1f;
    ret[1] =
          matrix[0][1] * x +
          matrix[1][1] * y +
          matrix[2][1] * z +
          matrix[3][1] * 1f;
    ret[2] =
          matrix[0][2] * x +
          matrix[1][2] * y +
          matrix[2][2] * z +
          matrix[3][2] * 1f;
    return ret;
  }
}
