package model.edgedetector.detectors;

import model.edgedetector.imagederivatives.ConvolutionKernel;
import model.edgedetector.imagederivatives.ImageConvolution;
import model.edgedetector.util.Grayscale;
import model.edgedetector.util.Threshold;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class RobertsCrossEdgeDetector implements EdgeDetector {

    private boolean[][] edges;
    private int rows;
    private int columns;
    private int threshold;
    private double strength;  // Strength parameter for controlling the sensitivity

    // Roberts Cross Kernels for X and Y direction
    private static final double[][] X_KERNEL = {
            {1, 0},
            {0, -1}
    };

    private static final double[][] Y_KERNEL = {
            {0, -1},
            {1, 0}
    };

    // Constructor to take strength as a parameter
    public RobertsCrossEdgeDetector(double strength) {
        this.strength = strength;
    }

    public File detectEdges(File imageFile) throws IOException {
        // Step 1: Load and convert the image to grayscale
        BufferedImage originalImage = ImageIO.read(imageFile);
        if (originalImage == null) {
            throw new IOException("Failed to load image.");
        }
        int[][] pixels = Grayscale.imgToGrayPixels(originalImage);

        // Step 2: Apply Gaussian Smoothing to reduce noise
        int[][] smoothedImage = applyGaussianSmoothing(pixels);

        // Step 3: Apply Roberts Cross Operator (calculate gradient)
        int[][] gradientMagnitude = applyRobertsCrossOperator(smoothedImage);

        // Step 4: Detect edges by applying thresholding
        calculateEdges(gradientMagnitude);

        // Step 5: Create output image showing the detected edges
        BufferedImage edgeImage = Threshold.applyThresholdReversed(edges);
        File result = File.createTempFile("roberts_cross_edge_result_", ".png");
        ImageIO.write(edgeImage, "png", result);
        result.deleteOnExit(); // ลบไฟล์เมื่อ JVM สิ้นสุด
        return result;
    }


    // Step 2: Gaussian Smoothing to reduce noise
    private int[][] applyGaussianSmoothing(int[][] pixels) {
        ImageConvolution gaussianConvolution = new ImageConvolution(pixels, ConvolutionKernel.GAUSSIAN_KERNEL);
        return gaussianConvolution.getConvolvedImage();
    }

    // Step 3: Apply Roberts Cross Operator to calculate gradient magnitude
    // Step 3: Apply Roberts Cross Operator to calculate gradient magnitude
    // Step 3: Apply Roberts Cross Operator to calculate gradient magnitude
    // Step 3: Apply Roberts Cross Operator to calculate gradient magnitude
    private int[][] applyRobertsCrossOperator(int[][] smoothedImage) {
        ImageConvolution xConvolution = new ImageConvolution(smoothedImage, X_KERNEL);
        ImageConvolution yConvolution = new ImageConvolution(smoothedImage, Y_KERNEL);

        int[][] xGradient = xConvolution.getConvolvedImage();
        int[][] yGradient = yConvolution.getConvolvedImage();

        rows = xGradient.length;
        columns = xGradient[0].length;
        int[][] gradientMagnitude = new int[rows][columns];

        // ปรับการคำนวณ strength ให้ละเอียดขึ้นโดยลดค่าคูณและเพิ่มความละเอียดในช่วงค่าต่ำ
        double adjustedStrength = Math.pow(strength, 1.2); // ลดการคูณเพื่อให้ภาพไม่เข้มเกินไปและทำให้ละเอียดขึ้นในค่าต่ำ

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                // Multiply gradient with adjusted strength
                gradientMagnitude[i][j] = (int) Math.min(255, Math.hypot(xGradient[i][j], yGradient[i][j]) * adjustedStrength);
            }
        }
        return gradientMagnitude;
    }

    // Step 4: Apply thresholding to detect edges
    private void calculateEdges(int[][] gradientMagnitude) {
        edges = new boolean[rows][columns];

        // ปรับ threshold ให้ควบคุมได้ละเอียดขึ้นโดยลดการคูณในส่วนของ strength
        threshold = (int) Math.max(50, 255 - (strength * 300)); // ลดค่า threshold เพื่อให้ได้ความเข้มที่พอเหมาะในค่าที่สูง

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                edges[i][j] = gradientMagnitude[i][j] >= threshold;
            }
        }
    }

    // Getter for edges
    public boolean[][] getEdges() {
        return edges;
    }
}