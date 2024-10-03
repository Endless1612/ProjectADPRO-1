package model.edgedetector.util;

import java.awt.image.BufferedImage;

public class Grayscale {

    /**
     * Converts a given BufferedImage into grayscale.
     *
     * @param image the input BufferedImage
     * @return the grayscale version of the input image
     */
    public static BufferedImage convertToGrayscale(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Create a new BufferedImage to store the grayscale image
        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        // Loop through each pixel in the image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y); // Get RGB value of the current pixel

                // Extract individual RGB values
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Calculate the grayscale value using average method
                int gray = (r + g + b) / 3;

                // Create grayscale color
                int grayRgb = (gray << 16) | (gray << 8) | gray;

                // Set the new grayscale value into the grayscale image
                grayImage.setRGB(x, y, grayRgb);
            }
        }
        return grayImage;
    }

    /**
     * Converts a given BufferedImage into a grayscale 2D array.
     *
     * @param image the input BufferedImage
     * @return a 2D array representing the grayscale intensities
     */
    public static int[][] imgToGrayPixels(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] grayPixels = new int[height][width];

        // Loop through each pixel in the image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                // Extract individual RGB values
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Calculate the grayscale value using the average method
                int gray = (r + g + b) / 3;

                // Store grayscale value in 2D array
                grayPixels[y][x] = gray;
            }
        }
        return grayPixels;
    }
}