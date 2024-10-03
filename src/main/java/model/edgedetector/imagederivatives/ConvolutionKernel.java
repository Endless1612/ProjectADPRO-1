package model.edgedetector.imagederivatives;

public class ConvolutionKernel {

   // Default 5x5 Gaussian kernel with sigma = 1.4
   public static final double[][] GAUSSIAN_KERNEL = {
           {2/159.0, 4/159.0 , 5/159.0 , 4/159.0 , 2/159.0},
           {4/159.0, 9/159.0 , 12/159.0, 9/159.0 , 4/159.0},
           {5/159.0, 12/159.0, 15/159.0, 12/159.0, 5/159.0},
           {4/159.0, 9/159.0 , 12/159.0, 9/159.0 , 4/159.0},
           {2/159.0, 4/159.0 , 5/159.0 , 4/159.0 , 2/159.0}
   };


   /**
    * Generates a Gaussian kernel matrix based on the specified sigma and size.
    *
    * @param sigma the standard deviation of the Gaussian distribution
    * @param size  the size of the kernel (must be odd, e.g., 3, 5, 7...)
    * @return a 2D Gaussian kernel matrix
    */
   public static double[][] generateGaussianKernel(double sigma, int size) {
      if (size % 2 == 0 || size < 3) {
         throw new IllegalArgumentException("Kernel size must be an odd number and at least 3.");
      }

      double[][] kernel = new double[size][size];
      int center = size / 2;
      double sum = 0.0;

      // Compute the Gaussian function for each element in the kernel
      for (int i = 0; i < size; i++) {
         for (int j = 0; j < size; j++) {
            double x = i - center;
            double y = j - center;
            kernel[i][j] = Math.exp(-(x * x + y * y) / (2 * sigma * sigma));
            sum += kernel[i][j];
         }
      }

      // Normalize the kernel so that the sum of all elements is 1
      for (int i = 0; i < size; i++) {
         for (int j = 0; j < size; j++) {
            kernel[i][j] /= sum;
         }
      }

      return kernel;
   }

}