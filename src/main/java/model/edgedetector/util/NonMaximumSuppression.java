package model.edgedetector.util;

public class NonMaximumSuppression {

   public enum EdgeDirection {
      VERTICAL,
      HORIZONTAL,
      DIAG_LEFT_UP,
      DIAG_RIGHT_UP;

      public static final double UP = Math.PI / 2.0;
      public static final double UP_TILT = Math.PI * 77.5 / 180.0;
      public static final double FLAT_TILT = Math.PI * 22.5 / 180.0;
      public static final double FLAT = 0;

      public static EdgeDirection getDirection(double G_x, double G_y) {
         return (G_x != 0) ? getDirection(Math.atan(G_y / G_x)) : ((G_y == 0) ? EdgeDirection.HORIZONTAL : EdgeDirection.VERTICAL);
      }

      public static EdgeDirection getDirection(double radians) {
         double radians_abs = Math.abs(radians);
         if (radians_abs >= UP_TILT && radians_abs <= UP)
            return EdgeDirection.VERTICAL;
         else if (radians_abs <= FLAT_TILT)
            return EdgeDirection.HORIZONTAL;
         else if (radians >= FLAT_TILT && radians <= UP_TILT)
            return EdgeDirection.DIAG_RIGHT_UP;
         else
            return EdgeDirection.DIAG_LEFT_UP;
      }
   }

   /**
    * ตรวจสอบว่าพิกเซลที่ตำแหน่ง (i, j) เป็นขอบหรือไม่ โดยพิจารณาจากเงื่อนไขสองข้อ:
    * 1. mag[i][j] > threshold
    * 2. Non-maximum suppression
    *
    * @param mag          ขนาดของการเปลี่ยนแปลงความเข้มของพิกเซล
    * @param angle        ทิศทางของขอบ
    * @param i            ตำแหน่งแถวของพิกเซล
    * @param j            ตำแหน่งคอลัมน์ของพิกเซล
    * @param threshold    ค่าความเข้มที่ใช้เป็นเกณฑ์ในการกำหนดขอบ
    * @return             คืนค่า true ถ้าพิกเซลเป็นขอบ
    */
   public static boolean nonMaximumSuppression(int[][] mag, EdgeDirection angle, int i, int j, int threshold) {
      // ตรวจสอบว่าค่าของ magnitude ที่ตำแหน่ง (i, j) มีค่ามากกว่า threshold หรือไม่
      if (mag[i][j] <= threshold) {
         return false; // ไม่เป็นขอบ
      }

      // คำนวณตำแหน่งของพิกเซล 2 จุดที่ต้องใช้ในการตรวจสอบ
      int[] indices = indicesMaxSuppresion(angle, i, j);

      // จุดแรก
      int i1 = indices[0];
      int j1 = indices[1];

      // จุดที่สอง
      int i2 = indices[2];
      int j2 = indices[3];

      // ตรวจสอบการกดขอบ (non-maximum suppression)
      boolean suppress1 = checkInBounds(i1, j1, mag.length, mag[0].length) && mag[i1][j1] > mag[i][j];
      boolean suppress2 = checkInBounds(i2, j2, mag.length, mag[0].length) && mag[i2][j2] > mag[i][j];

      // คืนค่า true ถ้า (i, j) ไม่ถูกกดจากทั้งสองข้าง
      return !(suppress1 || suppress2);
   }

   /**
    * คำนวณตำแหน่งของพิกเซล 2 จุดที่ต้องใช้ในการตรวจสอบการกดขอบ
    * @param d ทิศทางของขอบ
    * @param i ตำแหน่งแถวของพิกเซล
    * @param j ตำแหน่งคอลัมน์ของพิกเซล
    * @return  ตำแหน่งของพิกเซล 2 จุดที่ต้องใช้ในการตรวจสอบ
    */
   public static int[] indicesMaxSuppresion(EdgeDirection d, int i, int j) {
      int[] indices = new int[4];

      switch (d) {
         case VERTICAL:
            indices[0] = i - 1;
            indices[1] = j;
            indices[2] = i + 1;
            indices[3] = j;
            break;
         case HORIZONTAL:
            indices[0] = i;
            indices[1] = j - 1;
            indices[2] = i;
            indices[3] = j + 1;
            break;
         case DIAG_LEFT_UP:
            indices[0] = i - 1;
            indices[1] = j - 1;
            indices[2] = i + 1;
            indices[3] = j + 1;
            break;
         default: // DIAG_RIGHT_UP
            indices[0] = i - 1;
            indices[1] = j + 1;
            indices[2] = i + 1;
            indices[3] = j - 1;
            break;
      }

      return indices;
   }

   private static boolean checkInBounds(int i, int j, int rows, int columns) {
      return (i >= 0 && i < rows && j >= 0 && j < columns);
   }
}