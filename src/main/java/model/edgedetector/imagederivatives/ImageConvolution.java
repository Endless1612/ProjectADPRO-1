package model.edgedetector.imagederivatives;

public class ImageConvolution {

   private int[][] image;
   private double[][] kernel;
   private int[][] convolvedImage;

   public ImageConvolution(int[][] image, double[][] kernel) {
      this.image = image;
      this.kernel = kernel;
      this.convolvedImage = convolve();
   }

   private int[][] convolve() {
      int rows = image.length;
      int cols = image[0].length;
      int kernelRows = kernel.length;
      int kernelCols = kernel[0].length;
      int[][] output = new int[rows - kernelRows + 1][cols - kernelCols + 1];

      for (int i = 0; i < output.length; i++) {
         for (int j = 0; j < output[0].length; j++) {
            double sum = 0;
            for (int ki = 0; ki < kernelRows; ki++) {
               for (int kj = 0; kj < kernelCols; kj++) {
                  sum += image[i + ki][j + kj] * kernel[ki][kj];
               }
            }
            output[i][j] = (int) sum;
         }
      }
      return output;
   }

   public int[][] getConvolvedImage() {
      return convolvedImage;
   }
}