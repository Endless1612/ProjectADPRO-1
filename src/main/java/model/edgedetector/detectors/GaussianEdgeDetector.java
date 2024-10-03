package model.edgedetector.detectors;

import model.edgedetector.imagederivatives.ConvolutionKernel;
import model.edgedetector.imagederivatives.ImageConvolution;
import model.edgedetector.util.Grayscale;
import model.edgedetector.util.Threshold;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class GaussianEdgeDetector implements EdgeDetector {

    private static final double[][] X_KERNEL = {
            {-1, 0, 1},
            {-2, 0, 2},
            {-1, 0, 1}
    };

    private static final double[][] Y_KERNEL = {
            {1, 2, 1},
            {0, 0, 0},
            {-1, -2, -1}
    };

    private boolean[][] edges;
    private double sigma; // เพิ่มฟิลด์สำหรับ Sigma
    private int kernelSize; // เพิ่มฟิลด์สำหรับ Kernel Size

    // Constructor ที่รับค่า Sigma และ Kernel Size เป็นพารามิเตอร์
    public GaussianEdgeDetector(double sigma, int kernelSize) {
        this.sigma = sigma;
        this.kernelSize = kernelSize;
    }

    @Override
    public File detectEdges(File imageFile) throws IOException {
        BufferedImage originalImage = ImageIO.read(imageFile);
        if (originalImage == null) {
            throw new IOException("Failed to load image.");
        }
        int[][] pixels = Grayscale.imgToGrayPixels(originalImage);

        // Step 1: Apply Gaussian Smoothing to reduce noise
        int[][] smoothedImage = applyGaussianSmoothing(pixels);

        // Step 2: Apply Sobel Operator to calculate gradients in X and Y directions
        int[][] xGradient = applySobelOperator(smoothedImage, X_KERNEL);
        int[][] yGradient = applySobelOperator(smoothedImage, Y_KERNEL);

        // Step 3: Calculate the gradient magnitude
        int[][] gradientMagnitude = calculateGradientMagnitude(xGradient, yGradient);

        // Step 4: Calculate the threshold using mean-based thresholding
        int threshold = Threshold.calcThresholdEdges(gradientMagnitude);

        // Step 5: Apply the threshold to detect edges
        edges = applyThreshold(gradientMagnitude, threshold);

        // Step 6: Generate the edge-detected image
        BufferedImage edgeImage = Threshold.applyThresholdReversed(edges);
        // สร้างไฟล์ผลลัพธ์แบบชั่วคราวที่มีชื่อไม่ซ้ำกัน
        File result = File.createTempFile("gaussian_edge_result_", ".png");
        ImageIO.write(edgeImage, "png", result);
        result.deleteOnExit(); // ลบไฟล์เมื่อ JVM สิ้นสุด
        return result;
    }

    // Step 1: Gaussian Smoothing function with adjustable sigma and kernel size
    private int[][] applyGaussianSmoothing(int[][] pixels) {
        double[][] gaussianKernel = ConvolutionKernel.generateGaussianKernel(sigma, kernelSize); // ใช้ Kernel ที่กำหนดค่า Sigma และ Kernel Size
        ImageConvolution gaussianConvolution = new ImageConvolution(pixels, gaussianKernel);
        return gaussianConvolution.getConvolvedImage();
    }

    // Step 2: Sobel Operator function
    private int[][] applySobelOperator(int[][] image, double[][] kernel) {
        ImageConvolution convolution = new ImageConvolution(image, kernel);
        return convolution.getConvolvedImage();
    }

    // Step 3: Calculate gradient magnitude from X and Y gradients
    private int[][] calculateGradientMagnitude(int[][] xGradient, int[][] yGradient) {
        int rows = xGradient.length;
        int columns = xGradient[0].length;
        int[][] gradientMagnitude = new int[rows][columns];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                gradientMagnitude[i][j] = (int) Math.hypot(xGradient[i][j], yGradient[i][j]);
            }
        }
        return gradientMagnitude;
    }

    // Step 4: Apply threshold to gradient magnitude
    private boolean[][] applyThreshold(int[][] gradientMagnitude, int threshold) {
        int rows = gradientMagnitude.length;
        int columns = gradientMagnitude[0].length;
        boolean[][] edges = new boolean[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                edges[i][j] = gradientMagnitude[i][j] >= threshold;
            }
        }
        return edges;
    }

    public boolean[][] getEdges() {
        return edges;
    }
}