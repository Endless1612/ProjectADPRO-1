package model.edgedetector.util;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public class Threshold {

   /**
    * Calculates threshold as the mean of the |G| matrix for edge detection algorithms.
    * @param magnitude the magnitude of the gradient for each pixel in the image
    * @return the calculated threshold based on the mean value of the magnitudes
    */
   public static int calcThresholdEdges(int[][] magnitude) {
      return (int) Statistics.calcMean(magnitude); // อ้างอิงคลาส Statistics ที่ใช้คำนวณค่าเฉลี่ย
   }

   /**
    * Binarizes the image based on the threshold value.
    * @param pixels the pixel values of the image
    * @param threshold the threshold value for binarization
    * @return the thresholded BufferedImage
    */
   public static BufferedImage applyThreshold(int[][] pixels, int threshold) {
      int height = pixels.length;
      int width = pixels[0].length;

      BufferedImage thresholdedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
      WritableRaster raster = thresholdedImage.getRaster();

      int[] black = {0};
      int[] white = {255};

      // Cache-efficient for both BufferedImage and int[][]
      for (int row = 0; row < height; row++) {
         for (int col = 0; col < width; col++) {
            raster.setPixel(col, row, pixels[row][col] > threshold ? white : black);
         }
      }
      return thresholdedImage;
   }

   /**
    * Binarizes the image using a boolean array.
    * @param pixels the binary values representing edges
    * @return a BufferedImage with white for true (edge) and black for false (non-edge)
    */
   public static BufferedImage applyThreshold(boolean[][] pixels) {
      int height = pixels.length;
      int width = pixels[0].length;

      BufferedImage thresholdedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
      WritableRaster raster = thresholdedImage.getRaster();

      int[] black = {0};
      int[] white = {255};

      for (int row = 0; row < height; row++) {
         for (int col = 0; col < width; col++) {
            raster.setPixel(col, row, pixels[row][col] ? white : black);
         }
      }
      return thresholdedImage;
   }

   /**
    * Reverses the binarization process by swapping black and white.
    * @param pixels the binary values representing edges
    * @return a BufferedImage with black for true (edge) and white for false (non-edge)
    */
   public static BufferedImage applyThresholdReversed(boolean[][] pixels) {
      int height = pixels.length;
      int width = pixels[0].length;

      BufferedImage thresholdedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
      WritableRaster raster = thresholdedImage.getRaster();

      int[] black = {0};
      int[] white = {255};

      for (int row = 0; row < height; row++) {
         for (int col = 0; col < width; col++) {
            raster.setPixel(col, row, pixels[row][col] ? black : white);
         }
      }
      return thresholdedImage;
   }

   /**
    * Distinguishes between weak and strong edges using different colors.
    * @param weakEdges the weak edge binary values
    * @param strongEdges the strong edge binary values
    * @return a BufferedImage with blue for weak edges and green for strong edges
    */
   public static BufferedImage applyThresholdWeakStrongCanny(boolean[][] weakEdges, boolean[][] strongEdges) {
      int height = weakEdges.length;
      int width = weakEdges[0].length;

      BufferedImage thresholdedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
      WritableRaster raster = thresholdedImage.getRaster();

      int[] white = {255, 255, 255};
      int[] blue = {0, 0, 255};
      int[] green = {0, 255, 0};

      for (int row = 0; row < height; row++) {
         for (int col = 0; col < width; col++) {
            if (strongEdges[row][col]) {
               raster.setPixel(col, row, green);
            } else if (weakEdges[row][col]) {
               raster.setPixel(col, row, blue);
            } else {
               raster.setPixel(col, row, white);
            }
         }
      }

      return thresholdedImage;
   }

   /**
    * Applies a threshold to the image while keeping the original image's color for edges.
    * @param edges the binary values representing edges
    * @param originalImage the original image to maintain its color
    * @return a new BufferedImage with white for non-edges and original color for edges
    */
   public static BufferedImage applyThresholdOriginal(boolean[][] edges, BufferedImage originalImage) {
      int height = edges.length;
      int width = edges[0].length;

      BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
      WritableRaster rasterNew = newImage.getRaster();
      WritableRaster rasterOld = originalImage.getRaster();

      int[] white = {255, 255, 255};
      int[] arr = new int[3]; // Array to store RGB values

      for (int row = 0; row < height; row++) {
         for (int col = 0; col < width; col++) {
            if (!edges[row][col]) {
               rasterNew.setPixel(col, row, white);
            } else {
               // Copy original pixel color
               rasterOld.getPixel(col, row, arr);
               rasterNew.setPixel(col, row, arr);
            }
         }
      }

      return newImage;
   }

   /**
    * Adaptive thresholding based on mean and standard deviation of the magnitudes.
    * This method adapts the threshold according to the pixel values in the image.
    * @param magnitude the gradient magnitude matrix
    * @return the adaptive threshold calculated based on image statistics
    */
   public static int adaptiveThreshold(int[][] magnitude) {
      int rows = magnitude.length;
      int cols = magnitude[0].length;

      int sum = 0;
      int count = 0;

      // คำนวณค่าเฉลี่ย (mean) ของ magnitude
      for (int i = 0; i < rows; i++) {
         for (int j = 0; j < cols; j++) {
            sum += magnitude[i][j];
            count++;
         }
      }

      int mean = sum / count;

      // คำนวณค่าส่วนเบี่ยงเบนมาตรฐาน (Standard Deviation)
      int varianceSum = 0;
      for (int i = 0; i < rows; i++) {
         for (int j = 0; j < cols; j++) {
            varianceSum += Math.pow(magnitude[i][j] - mean, 2);
         }
      }

      int stdDev = (int) Math.sqrt(varianceSum / count);

      // ใช้ค่า mean และ stdDev ในการปรับ threshold
      int adaptiveThreshold = mean + (int) (0.5 * stdDev);  // สามารถปรับตัวคูณ stdDev เพื่อผลลัพธ์ที่แตกต่าง

      return adaptiveThreshold;
   }
}