package controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.edgedetector.detectors.EdgeDetector;
import model.edgedetector.detectors.CannyEdgeDetector;
import model.edgedetector.detectors.GaussianEdgeDetector;
import model.edgedetector.detectors.LaplacianEdgeDetector;
import model.edgedetector.detectors.PrewittEdgeDetector;
import model.edgedetector.detectors.RobertsCrossEdgeDetector;
import model.edgedetector.detectors.SobelEdgeDetector;
import model.edgedetector.util.Grayscale;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class EdgeDetectionHandler {

    private final ImageView imageView;
    private List<Image> imageListDetected = new ArrayList<>();
    private ComboBox<String> algorithmChoice;
    private final Map<String, Class<? extends EdgeDetector>> edgeAlgorithms = new HashMap<>();
    private final Label statusLabel;

    // Custom Exceptions
    public static class ImageProcessingException extends Exception {
        public ImageProcessingException(String message) {
            super(message);
        }
    }

    public static class AlgorithmNotSupportedException extends Exception {
        public AlgorithmNotSupportedException(String message) {
            super(message);
        }
    }

    public static class BatchProcessingException extends Exception {
        private final List<Exception> exceptions;
        public BatchProcessingException(String message, List<Exception> exceptions) {
            super(message);
            this.exceptions = exceptions;
        }
        public List<Exception> getExceptions() {
            return exceptions;
        }
    }

    public EdgeDetectionHandler(
            ImageView imageView,
            Label statusLabel,
            ComboBox<String> algorithmChoice,
            Map<String, Class<? extends EdgeDetector>> edgeAlgorithms) {
        this.imageView = imageView;
        this.statusLabel = statusLabel;
        this.algorithmChoice = algorithmChoice;
        this.edgeAlgorithms.putAll(edgeAlgorithms);
        initializeEdgeAlgorithms();
    }


    private void initializeEdgeAlgorithms() {
        edgeAlgorithms.put("Canny", CannyEdgeDetector.class);
        edgeAlgorithms.put("Sobel", SobelEdgeDetector.class);
        edgeAlgorithms.put("Laplacian", LaplacianEdgeDetector.class);
        edgeAlgorithms.put("Prewitt", PrewittEdgeDetector.class);
        edgeAlgorithms.put("Roberts Cross", RobertsCrossEdgeDetector.class);
        edgeAlgorithms.put("Gaussian", GaussianEdgeDetector.class);

        algorithmChoice.getItems().addAll(edgeAlgorithms.keySet());
        algorithmChoice.setPromptText("Select Algorithm");
    }


    public Task<List<Image>> batchDetectEdges(String selectedAlgorithm, List<Image> listDetect, Object... params) {
        return new Task<List<Image>>() {
            @Override
            protected List<Image> call() throws Exception {
                List<Image> detectedImages = new ArrayList<>();
                List<Exception> exceptionList = new ArrayList<>();

                if (listDetect == null || listDetect.isEmpty()) {
                    throw new ImageProcessingException("Please load images first.");
                }

                if (selectedAlgorithm == null) {
                    throw new AlgorithmNotSupportedException("Please select an edge detection algorithm.");
                }

                Class<? extends EdgeDetector> detectorClass = edgeAlgorithms.get(selectedAlgorithm);
                if (detectorClass == null) {
                    throw new AlgorithmNotSupportedException("Selected algorithm is not supported.");
                }

                // Limit the thread pool size to prevent excessive memory usage
                int maxThreads = 4;
                ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
                List<Future<Image>> futures = new ArrayList<>();

                for (int i = 0; i < listDetect.size(); i++) {
                    Image imageToProcess = listDetect.get(i);
                    final int index = i;
                    Callable<Image> task = () -> {
                        try {
                            String threadName = Thread.currentThread().getName();
                            System.out.println("Processing image " + (index + 1) + " on thread: " + threadName);

                            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(imageToProcess, null);
                            if (bufferedImage == null) {
                                Platform.runLater(() -> setStatus("Failed to convert JavaFX Image to BufferedImage. Check image.", "red"));
                                return null;
                            }

                            EdgeDetector detector = null;

                            switch (selectedAlgorithm) {
                                case "Canny":
                                    if (params.length < 2) {
                                        throw new IllegalArgumentException("Canny requires low and high thresholds.");
                                    }
                                    int lowThreshold = (int) params[0];
                                    int highThreshold = (int) params[1];
                                    detector = new CannyEdgeDetector.Builder(
                                            Grayscale.imgToGrayPixels(bufferedImage))
                                            .thresholds(lowThreshold, highThreshold)
                                            .L1norm(false)
                                            .minEdgeSize(10)
                                            .build();
                                    break;

                                case "Roberts Cross":
                                    if (params.length < 1) {
                                        throw new IllegalArgumentException("Roberts Cross requires strength parameter.");
                                    }
                                    double strength = (double) params[0];
                                    detector = new RobertsCrossEdgeDetector(strength);
                                    break;

                                case "Laplacian":
                                    if (params.length < 1) {
                                        throw new IllegalArgumentException("Laplacian requires mask size parameter.");
                                    }
                                    String maskSize = (String) params[0];
                                    int maskSizeInt = Integer.parseInt(maskSize.substring(0, maskSize.indexOf('x')));
                                    detector = new LaplacianEdgeDetector(maskSizeInt);
                                    break;

                                case "Sobel":
                                    if (params.length < 2) {
                                        throw new IllegalArgumentException("Sobel requires kernel size and threshold parameters.");
                                    }
                                    int sobelKernelSize = (int) params[0];
                                    int sobelThreshold = (int) params[1];
                                    detector = new SobelEdgeDetector(sobelKernelSize, sobelThreshold);
                                    break;

                                case "Prewitt":
                                    if (params.length < 2) {
                                        throw new IllegalArgumentException("Prewitt requires kernel size and threshold parameters.");
                                    }
                                    int prewittKernelSize = (int) params[0];
                                    int prewittThreshold = (int) params[1];
                                    detector = new PrewittEdgeDetector(prewittKernelSize, prewittThreshold);
                                    break;

                                case "Gaussian":
                                    if (params.length < 2) {
                                        throw new IllegalArgumentException("Gaussian requires sigma and kernel size parameters.");
                                    }
                                    double sigma = (double) params[0];
                                    int kernelSize = (int) params[1];
                                    detector = new GaussianEdgeDetector(sigma, kernelSize);
                                    break;

                                default:
                                    detector = detectorClass.getDeclaredConstructor().newInstance();
                                    break;
                            }

                            if (detector != null) {
                                File tempFile = imageFileToTempFile(bufferedImage);
                                File processedFile = detector.detectEdges(tempFile);
                                if (processedFile != null && processedFile.exists()) {
                                    BufferedImage processedBufferedImage = ImageIO.read(processedFile);
                                    Image fxImage = SwingFXUtils.toFXImage(processedBufferedImage, null);

                                    // Delete temporary files to free up memory and disk space
                                    tempFile.delete();
                                    processedFile.delete();

                                    return fxImage;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            final int displayIndex = index + 1;
                            Platform.runLater(() -> setStatus("Edge detection failed for image " + displayIndex + ".", "red"));
                        }
                        return null;
                    };

                    futures.add(executor.submit(task));
                }

                // Collect results and update progress
                for (int i = 0; i < futures.size(); i++) {
                    try {
                        Image resultImage = futures.get(i).get(); // blocking
                        if (resultImage != null) {
                            detectedImages.add(resultImage);
                        } else {
                            detectedImages.add(listDetect.get(i)); // Add original image if processing failed
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        detectedImages.add(listDetect.get(i));
                        final int displayIndex = i + 1;
                        Platform.runLater(() -> setStatus("Error processing image " + displayIndex + ".", "red"));
                    }
                    // Update progress
                    updateProgress(i + 1, listDetect.size());
                }

                executor.shutdown();

                if (!exceptionList.isEmpty()) {
                    throw new BatchProcessingException("Errors occurred during batch processing.", exceptionList);
                }
                Platform.runLater(() -> setStatus("Edge detection completed successfully.", "green"));

                return detectedImages;
            }
        };
    }

    private File imageFileToTempFile(BufferedImage image) throws IOException {
        File tempFile = File.createTempFile("edge_detect_", ".png");
        ImageIO.write(image, "png", tempFile);
        tempFile.deleteOnExit();
        return tempFile;
    }


    private void setStatus(String message, String color) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-text-fill: " + color + ";");
        });
    }


    @Deprecated
    public void ListDetectEdges(String selectedAlgorithm, List<Image> listDetect, Object... params) {
        if (listDetect != null && listDetect.size() == 1) {
            Task<List<Image>> singleTask = batchDetectEdges(selectedAlgorithm, listDetect, params);
            singleTask.setOnSucceeded(e -> {
                List<Image> results = singleTask.getValue();
                if (!results.isEmpty()) {
                    imageListDetected.addAll(results);
                    setStatus("Edge detection completed successfully.", "green");
                } else {
                    setStatus("Edge detection failed or no images were processed.", "red");
                }
            });
            singleTask.setOnFailed(e -> {
                setStatus("Edge detection failed.", "red");
            });
            new Thread(singleTask).start();
        } else {
            Task<List<Image>> batchTask = batchDetectEdges(selectedAlgorithm, listDetect, params);
            new Thread(batchTask).start();
        }
    }


    public List<Image> getImageListDetected() {
        return imageListDetected;
    }

    public void resetImageListDetected() {
        imageListDetected.clear();
    }
}