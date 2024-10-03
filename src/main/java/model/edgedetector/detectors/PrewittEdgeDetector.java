package model.edgedetector.detectors;

import model.edgedetector.imagederivatives.ConvolutionKernel;
import model.edgedetector.imagederivatives.ImageConvolution;
import model.edgedetector.util.Grayscale;
import model.edgedetector.util.Threshold;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PrewittEdgeDetector implements EdgeDetector {

    private boolean[][] edges;
    private int rows;
    private int columns;
    private int threshold;
    private int kernelSize;

    // Prewitt Kernels for X and Y direction
    private static final double[][] X_KERNEL_3x3 = {
            {-1, 0, 1},
            {-1, 0, 1},
            {-1, 0, 1}
    };

    private static final double[][] Y_KERNEL_3x3 = {
            {1, 1, 1},
            {0, 0, 0},
            {-1, -1, -1}
    };

    private static final double[][] X_KERNEL_5x5 = {
            {-2, -1, 0, 1, 2},
            {-2, -1, 0, 1, 2},
            {-4, -2, 0, 2, 4},
            {-2, -1, 0, 1, 2},
            {-2, -1, 0, 1, 2}
    };

    private static final double[][] Y_KERNEL_5x5 = {
            {2, 2, 4, 2, 2},
            {1, 1, 2, 1, 1},
            {0, 0, 0, 0, 0},
            {-1, -1, -2, -1, -1},
            {-2, -2, -4, -2, -2}
    };

    // Constructor with kernel size and threshold
    public PrewittEdgeDetector(int kernelSize, int threshold) {
        this.kernelSize = kernelSize;
        this.threshold = threshold;
    }

    @Override
    public File detectEdges(File imageFile) throws IOException {
        BufferedImage originalImage = ImageIO.read(imageFile);
        if (originalImage == null) {
            throw new IOException("Failed to load image.");
        }
        int[][] pixels = Grayscale.imgToGrayPixels(originalImage);

        // Step 1: Gaussian Smoothing (Blur)
        int[][] smoothedImage = applyGaussianSmoothing(pixels);

        // Step 2: Apply Prewitt Operator with chosen kernel size
        int[][] gradientMagnitude = applyPrewittOperator(smoothedImage);

        // Step 3: Thresholding to detect edges
        calculateEdges(gradientMagnitude);

        // Step 4: Generate output image with detected edges
        BufferedImage edgeImage = Threshold.applyThresholdReversed(edges);

        // สร้างไฟล์ผลลัพธ์แบบชั่วคราวที่มีชื่อไม่ซ้ำกัน
        File result = File.createTempFile("prewitt_edge_result_", ".png");
        ImageIO.write(edgeImage, "png", result);
        result.deleteOnExit(); // ลบไฟล์เมื่อ JVM สิ้นสุด
        return result;
    }

    // Step 1: Gaussian Smoothing to reduce noise
    private int[][] applyGaussianSmoothing(int[][] pixels) {
        ImageConvolution gaussianConvolution = new ImageConvolution(pixels, ConvolutionKernel.GAUSSIAN_KERNEL);
        return gaussianConvolution.getConvolvedImage();
    }

    // Step 2: Apply Prewitt Operator (calculate gradients)
    private int[][] applyPrewittOperator(int[][] smoothedImage) {
        double[][] xKernel = (kernelSize == 3) ? X_KERNEL_3x3 : X_KERNEL_5x5;
        double[][] yKernel = (kernelSize == 3) ? Y_KERNEL_3x3 : Y_KERNEL_5x5;

        ImageConvolution xConvolution = new ImageConvolution(smoothedImage, xKernel);
        ImageConvolution yConvolution = new ImageConvolution(smoothedImage, yKernel);

        int[][] xGradient = xConvolution.getConvolvedImage();
        int[][] yGradient = yConvolution.getConvolvedImage();

        rows = xGradient.length;
        columns = xGradient[0].length;
        int[][] gradientMagnitude = new int[rows][columns];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                gradientMagnitude[i][j] = (int) Math.hypot(xGradient[i][j], yGradient[i][j]);
            }
        }

        return gradientMagnitude;
    }

    // Step 3: Apply threshold to detect edges
    private void calculateEdges(int[][] gradientMagnitude) {
        edges = new boolean[rows][columns];

        // ถ้า threshold เป็น 0 ใช้ค่า threshold ที่คำนวณจาก magnitude
        int effectiveThreshold = (threshold == 0) ? Threshold.calcThresholdEdges(gradientMagnitude) : threshold;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                edges[i][j] = gradientMagnitude[i][j] >= effectiveThreshold;
            }
        }
    }

    // Getter for edges
    public boolean[][] getEdges() {
        return edges;
    }
}