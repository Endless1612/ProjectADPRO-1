package model.edgedetector.detectors;

import model.edgedetector.util.Grayscale;
import model.edgedetector.util.Threshold;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class LaplacianEdgeDetector implements EdgeDetector {

    private boolean[][] edges;  // ผลลัพธ์ขอบภาพ
    private int threshold;      // ค่าธรชโฮลด์สำหรับขอบ
    private int maskSize;       // ขนาดของ Mask (3x3 หรือ 5x5)

    // Laplacian kernel 5x5
    private final double[][] kernel5x5 = {
            {0, 0, -1, 0, 0},
            {0, -1, -2, -1, 0},
            {-1, -2, 16, -2, -1},
            {0, -1, -2, -1, 0},
            {0, 0, -1, 0, 0}
    };

    // Laplacian kernel 3x3
    private final double[][] kernel3x3 = {
            {0, -1, 0},
            {-1, 4, -1},
            {0, -1, 0}
    };

    private final double[][] kernel7x7 = {
            {0, 0, -1, -1, -1, 0, 0},
            {0, -1, -3, -3, -3, -1, 0},
            {-1, -3, 0, 7, 0, -3, -1},
            {-1, -3, 7, 24, 7, -3, -1},
            {-1, -3, 0, 7, 0, -3, -1},
            {0, -1, -3, -3, -3, -1, 0},
            {0, 0, -1, -1, -1, 0, 0}
    };

    // คอนสตรัคเตอร์ที่รับขนาดของ mask เป็นพารามิเตอร์
    public LaplacianEdgeDetector(int maskSize) {
        this.maskSize = maskSize;
    }

    private void findEdges(int[][] image) {
        // ขั้นตอนที่ 1: Gaussian smoothing เพื่อลด noise
        int[][] smoothedImage = applyGaussianSmoothing(image);

        // ขั้นตอนที่ 2: ทำการคอนโวลูชันด้วย Laplacian kernel ที่เลือก
        double[][] chosenKernel;
        if (maskSize == 3) {
            chosenKernel = kernel3x3;
        } else if (maskSize == 5) {
            chosenKernel = kernel5x5;
        } else {
            chosenKernel = kernel7x7; // ใช้ Kernel 7x7
        }
        int[][] convolvedImage = applyLaplacian(smoothedImage, chosenKernel);

        int rows = convolvedImage.length;
        int columns = convolvedImage[0].length;

        // ขั้นตอนที่ 3: คำนวณ threshold โดยใช้ adaptive thresholding
        threshold = Threshold.adaptiveThreshold(convolvedImage);

        // ขั้นตอนที่ 4: ค้นหาขอบโดยใช้ threshold
        edges = new boolean[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                edges[i][j] = Math.abs(convolvedImage[i][j]) > threshold;
            }
        }
    }

    // ฟังก์ชันคืนค่า edges
    public boolean[][] getEdges() {
        return edges;
    }

    // ฟังก์ชันคืนค่า threshold
    public int getThreshold() {
        return threshold;
    }

    // ฟังก์ชัน detectEdges ที่ใช้ใน ImageController
    @Override
    public File detectEdges(File imageFile) throws IOException {
        BufferedImage originalImage = ImageIO.read(imageFile);
        if (originalImage == null) {
            throw new IOException("Failed to load image.");
        }
        int[][] grayImage = Grayscale.imgToGrayPixels(originalImage);

        // เรียกใช้ฟังก์ชันการค้นหาขอบ
        findEdges(grayImage);

        BufferedImage edgeImage = Threshold.applyThresholdReversed(edges);

        // สร้างไฟล์ผลลัพธ์แบบชั่วคราวที่มีชื่อไม่ซ้ำกัน
        File result = File.createTempFile("laplacian_result_", ".png");
        ImageIO.write(edgeImage, "png", result);
        result.deleteOnExit(); // ลบไฟล์เมื่อ JVM สิ้นสุด
        return result;
    }

    // ฟังก์ชัน Gaussian smoothing
    private int[][] applyGaussianSmoothing(int[][] image) {
        double[][] gaussianKernel = {
                {1 / 16.0, 1 / 8.0, 1 / 16.0},
                {1 / 8.0, 1 / 4.0, 1 / 8.0},
                {1 / 16.0, 1 / 8.0, 1 / 16.0}
        };
        return applyConvolution(image, gaussianKernel);
    }

    // ฟังก์ชัน Laplacian Convolution
    private int[][] applyLaplacian(int[][] image, double[][] kernel) {
        return applyConvolution(image, kernel);
    }

    // ฟังก์ชัน Convolution สำหรับ Gaussian และ Laplacian
    private int[][] applyConvolution(int[][] image, double[][] kernel) {
        int kernelSize = kernel.length;
        int offset = kernelSize / 2;
        int width = image[0].length;
        int height = image.length;
        int[][] result = new int[height][width];

        for (int y = offset; y < height - offset; y++) {
            for (int x = offset; x < width - offset; x++) {
                double sum = 0.0;
                for (int ky = -offset; ky <= offset; ky++) {
                    for (int kx = -offset; kx <= offset; kx++) {
                        int pixel = image[y + ky][x + kx];
                        double weight = kernel[ky + offset][kx + offset];
                        sum += pixel * weight;
                    }
                }
                result[y][x] = (int) Math.max(0, Math.min(255, sum));
            }
        }
        return result;
    }
}