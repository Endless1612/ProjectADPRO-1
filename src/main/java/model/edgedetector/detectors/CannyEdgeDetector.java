package model.edgedetector.detectors;

import model.edgedetector.imagederivatives.ConvolutionKernel;
import model.edgedetector.imagederivatives.ImageConvolution;
import model.edgedetector.util.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Stack;

public class CannyEdgeDetector implements EdgeDetector {

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

    private boolean L1norm;
    private boolean calcThreshold;
    private int highThreshold;
    private int lowThreshold;
    private int minEdgeSize;

    private boolean[][] edges;
    private boolean[][] strongEdges;
    private boolean[][] weakEdges;

    private int rows;
    private int columns;

    public CannyEdgeDetector() {}

    private CannyEdgeDetector(Builder builder) {
        this.L1norm = builder.L1norm;
        this.minEdgeSize = builder.minEdgeSize;
        if (!(this.calcThreshold = builder.calcThreshold)) {
            this.lowThreshold = builder.lowThreshold;
            this.highThreshold = builder.highThreshold;
        }
        findEdges(builder.image);
    }

    public static class Builder {
        private int[][] image;
        private boolean calcThreshold = true;
        private int lowThreshold;
        private int highThreshold;
        private boolean L1norm = false;
        private int minEdgeSize = 0;

        public Builder(int[][] image) {
            this.image = image;
        }

        public Builder thresholds(int lowThreshold, int highThreshold) {
            if (lowThreshold > highThreshold || lowThreshold < 0 || highThreshold > 255) {
                throw new IllegalArgumentException("Invalid threshold values");
            }
            this.calcThreshold = false;
            this.lowThreshold = lowThreshold;
            this.highThreshold = highThreshold;
            return this;
        }

        public Builder L1norm(boolean L1norm) {
            this.L1norm = L1norm;
            return this;
        }

        public Builder minEdgeSize(int minEdgeSize) {
            this.minEdgeSize = minEdgeSize;
            return this;
        }

        public CannyEdgeDetector build() {
            return new CannyEdgeDetector(this);
        }
    }

    private void findEdges(int[][] image) {
        // Step 1: Gaussian smoothing to reduce noise
        ImageConvolution gaussianConvolution = new ImageConvolution(image, ConvolutionKernel.GAUSSIAN_KERNEL);
        int[][] smoothedImage = gaussianConvolution.getConvolvedImage();

        // Step 2: Compute gradients in X and Y direction using Sobel operator
        ImageConvolution x_ic = new ImageConvolution(smoothedImage, X_KERNEL);
        ImageConvolution y_ic = new ImageConvolution(smoothedImage, Y_KERNEL);

        int[][] x_imageConvolution = x_ic.getConvolvedImage();
        int[][] y_imageConvolution = y_ic.getConvolvedImage();

        rows = x_imageConvolution.length;
        columns = x_imageConvolution[0].length;

        // Step 3: Compute gradient magnitude and direction
        int[][] mag = new int[rows][columns];
        NonMaximumSuppression.EdgeDirection[][] angle = new NonMaximumSuppression.EdgeDirection[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                mag[i][j] = hypotenuse(x_imageConvolution[i][j], y_imageConvolution[i][j]);
                angle[i][j] = direction(x_imageConvolution[i][j], y_imageConvolution[i][j]);
            }
        }

        edges = new boolean[rows][columns];
        weakEdges = new boolean[rows][columns];
        strongEdges = new boolean[rows][columns];

        // Step 4: Non-maximum suppression to refine edges
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (NonMaximumSuppression.nonMaximumSuppression(mag, angle[i][j], i, j, lowThreshold)) {
                    mag[i][j] = 0;
                }
            }
        }

        // Step 5: Calculate optimal threshold using KMeans clustering
        if (calcThreshold) {
            calculateThresholds(mag);
        }

        // Step 6: Detect strong and weak edges using the thresholds
        HashSet<Integer> strongSet = new HashSet<>();
        HashSet<Integer> weakSet = new HashSet<>();

        int index = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                if (mag[r][c] >= highThreshold) {
                    strongSet.add(index);
                    strongEdges[r][c] = true;
                } else if (mag[r][c] >= lowThreshold) {
                    weakSet.add(index);
                    weakEdges[r][c] = true;
                }
                index++;
            }
        }

        // Step 7: Hysteresis - connect weak edges to strong edges using Iterative DFS
        boolean[][] marked = new boolean[rows][columns];
        Stack<Integer> toAdd = new Stack<>();

        for (int strongIndex : strongSet) {
            int[] coordinates = ind2sub(strongIndex, columns);
            iterativeDFS(coordinates[0], coordinates[1], weakSet, strongSet, marked, toAdd);

            if (toAdd.size() >= minEdgeSize) {
                for (int edgeIndex : toAdd) {
                    int[] edgeCoordinates = ind2sub(edgeIndex, columns);
                    edges[edgeCoordinates[0]][edgeCoordinates[1]] = true;
                }
            }
            toAdd.clear(); // Clear the stack after processing each strong edge
        }
    }

    // Iterative DFS to prevent stack overflow
    private void iterativeDFS(int startR, int startC, HashSet<Integer> weakSet, HashSet<Integer> strongSet, boolean[][] marked, Stack<Integer> toAdd) {
        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{startR, startC});

        while (!stack.isEmpty()) {
            int[] current = stack.pop();
            int r = current[0];
            int c = current[1];

            if (r < 0 || r >= rows || c < 0 || c >= columns || marked[r][c]) {
                continue;
            }

            marked[r][c] = true;
            int index = sub2ind(r, c, columns);

            if (weakSet.contains(index) || strongSet.contains(index)) {
                toAdd.push(index);

                // Add all neighbors to the stack
                stack.push(new int[]{r - 1, c - 1});
                stack.push(new int[]{r - 1, c});
                stack.push(new int[]{r - 1, c + 1});
                stack.push(new int[]{r, c - 1});
                stack.push(new int[]{r, c + 1});
                stack.push(new int[]{r + 1, c - 1});
                stack.push(new int[]{r + 1, c});
                stack.push(new int[]{r + 1, c + 1});
            }
        }
    }

    // Step 5: KMeans for automatic threshold calculation
    private void calculateThresholds(int[][] mag) {
        int k = 3;
        double[][] points = new double[rows * columns][1];
        int counter = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                points[counter++][0] = mag[i][j];
            }
        }

        KMeans clustering = new KMeans.Builder(k, points)
                .iterations(10)
                .epsilon(.01)
                .useEpsilon(true)
                .build();
        double[][] centroids = clustering.getCentroids();

        // Assuming the two smallest centroids are used for low and high thresholds
        java.util.Arrays.sort(centroids, (a, b) -> Double.compare(a[0], b[0]));

        lowThreshold = (int) centroids[0][0];
        highThreshold = (int) centroids[1][0];
    }

    private static int[] ind2sub(int index, int columns) {
        return new int[]{index / columns, index % columns};
    }

    private static int sub2ind(int r, int c, int columns) {
        return columns * r + c;
    }

    private int hypotenuse(int x, int y) {
        return (int) (L1norm ? Hypotenuse.L1(x, y) : Hypotenuse.L2(x, y));
    }

    private NonMaximumSuppression.EdgeDirection direction(int G_x, int G_y) {
        return NonMaximumSuppression.EdgeDirection.getDirection(G_x, G_y);
    }

    public boolean[][] getEdges() {
        return edges;
    }

    @Override
    public File detectEdges(File imageFile) throws IOException {
        BufferedImage originalImage = ImageIO.read(imageFile);
        if (originalImage == null) {
            throw new IOException("Failed to read the image file for Canny edge detection.");
        }

        int[][] pixels = Grayscale.imgToGrayPixels(originalImage);
        CannyEdgeDetector detector = new Builder(pixels)
                .minEdgeSize(10)
                .thresholds(lowThreshold, highThreshold)
                .L1norm(false)
                .build();

        boolean[][] edges = detector.getEdges();
        if (edges == null || edges.length == 0) {
            throw new IOException("Failed to generate edge map with Canny edge detection.");
        }

        // Convert edge data back into a BufferedImage
        BufferedImage edgeImage = Threshold.applyThresholdReversed(edges);

        // สร้างไฟล์ผลลัพธ์แบบชั่วคราวที่มีชื่อไม่ซ้ำกัน
        File result = File.createTempFile("canny_result_", ".png");
        ImageIO.write(edgeImage, "png", result);
        result.deleteOnExit(); // ลบไฟล์เมื่อ JVM สิ้นสุด
        return result;
    }
}