package model.edgedetector.util;

import java.util.Arrays;
import java.util.Random;

public class KMeans {

    private int k;
    private double[][] points;
    private double[][] centroids;
    private int maxIterations;
    private double epsilon;
    private boolean useEpsilon;

    // Constructor
    public KMeans(Builder builder) {
        this.k = builder.k;
        this.points = builder.points;
        this.maxIterations = builder.maxIterations;
        this.epsilon = builder.epsilon;
        this.useEpsilon = builder.useEpsilon;
        initializeCentroids();
        runKMeans();
    }

    // Method to initialize centroids randomly
    private void initializeCentroids() {
        centroids = new double[k][points[0].length];
        Random rand = new Random();
        for (int i = 0; i < k; i++) {
            centroids[i] = points[rand.nextInt(points.length)].clone();
        }
    }

    // Main method to run the K-Means clustering algorithm
    private void runKMeans() {
        double[][] oldCentroids = new double[k][points[0].length];
        int[] labels = new int[points.length];
        int iterations = 0;

        while (iterations < maxIterations) {
            // Save previous centroids
            for (int i = 0; i < k; i++) {
                oldCentroids[i] = centroids[i].clone();
            }

            // Assign labels based on the closest centroid
            for (int i = 0; i < points.length; i++) {
                labels[i] = getClosestCentroid(points[i]);
            }

            // Update centroids based on new labels
            updateCentroids(labels);

            // Check for convergence
            if (hasConverged(oldCentroids, centroids)) {
                break;
            }

            iterations++;
        }
    }

    // Helper method to find the closest centroid to a point
    private int getClosestCentroid(double[] point) {
        double minDist = Double.MAX_VALUE;
        int label = 0;

        for (int i = 0; i < k; i++) {
            double dist = euclideanDistance(point, centroids[i]);
            if (dist < minDist) {
                minDist = dist;
                label = i;
            }
        }

        return label;
    }

    // Helper method to update centroids
    private void updateCentroids(int[] labels) {
        int[] counts = new int[k];
        double[][] newCentroids = new double[k][points[0].length];

        for (int i = 0; i < points.length; i++) {
            int label = labels[i];
            counts[label]++;
            for (int j = 0; j < points[i].length; j++) {
                newCentroids[label][j] += points[i][j];
            }
        }

        for (int i = 0; i < k; i++) {
            if (counts[i] != 0) {
                for (int j = 0; j < newCentroids[i].length; j++) {
                    newCentroids[i][j] /= counts[i];
                }
            }
        }

        centroids = newCentroids;
    }

    // Helper method to check for convergence
    private boolean hasConverged(double[][] oldCentroids, double[][] newCentroids) {
        for (int i = 0; i < k; i++) {
            if (euclideanDistance(oldCentroids[i], newCentroids[i]) > epsilon) {
                return false;
            }
        }
        return true;
    }

    // Helper method to calculate Euclidean distance
    private double euclideanDistance(double[] point1, double[] point2) {
        double sum = 0.0;
        for (int i = 0; i < point1.length; i++) {
            sum += Math.pow(point1[i] - point2[i], 2);
        }
        return Math.sqrt(sum);
    }

    // Method to get the centroids after running K-Means
    public double[][] getCentroids() {
        return centroids;
    }

    // Builder pattern for constructing KMeans objects
    public static class Builder {

        private int k;
        private double[][] points;
        private int maxIterations = 100;
        private double epsilon = 1e-4;
        private boolean useEpsilon = true;
        private boolean useKMeansPlusPlus = false; // Add this field

        public Builder(int k, double[][] points) {
            this.k = k;
            this.points = points;
        }

        public Builder iterations(int maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }

        public Builder epsilon(double epsilon) {
            this.epsilon = epsilon;
            return this;
        }

        public Builder useEpsilon(boolean useEpsilon) {
            this.useEpsilon = useEpsilon;
            return this;
        }

        // Add this method to support pp(true)
        public Builder pp(boolean useKMeansPlusPlus) {
            this.useKMeansPlusPlus = useKMeansPlusPlus;
            return this;
        }

        public KMeans build() {
            return new KMeans(this);
        }
    }

    // For testing purposes (Optional)
    public static void main(String[] args) {
        double[][] points = {
                {1.0, 2.0},
                {2.0, 3.0},
                {4.0, 5.0},
                {7.0, 8.0},
                {8.0, 9.0}
        };

        KMeans kMeans = new KMeans.Builder(2, points)
                .iterations(10)
                .epsilon(1e-4)
                .useEpsilon(true)
                .build();

        System.out.println("Centroids: " + Arrays.deepToString(kMeans.getCentroids()));
    }
}