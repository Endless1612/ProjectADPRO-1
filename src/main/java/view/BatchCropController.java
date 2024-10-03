package view;

import controller.CropHandler;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.stage.Stage;

import java.util.*;
import java.util.concurrent.*;

public class BatchCropController {

    private List<Image> cropListImage = new ArrayList<>();  // List of images to crop
    private List<Image> croppedListImage = new ArrayList<>();
    private ImageController imageController;  // Reference to ImageController
    private Map<ImageView, CropHandler> cropHandlers = new HashMap<>();  // Map to store handlers for each ImageView
    private boolean isCropepded = false;
    private Stage mainStage;

    @FXML
    private ProgressBar progressBar;
    @FXML
    private ScrollPane imageScrollPane;

    @FXML
    private FlowPane imageFlowPane;

    @FXML
    public void initialize() {
        System.out.println("BatchCropController initialized.");
        progressBar.setVisible(true);
    }

    public void setMainStage(Stage mainStage) {
        this.mainStage = mainStage;
    }

    public void setImageController(ImageController imageController) {
        try {
            cropListImage.clear();
            this.imageController = imageController;
            loadCroppedImages();  // Load the cropped images from the ImageController
            System.out.println("ImageController has been set in BatchCropController.");
        } catch (Exception e) {
            System.err.println("Error setting ImageController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadCroppedImages() {
        try {
            if (imageController != null) {
                cropListImage = imageController.getResultListImages();  // Fetch images from ImageController
                addImagesToFlowPane();  // Display the images
            } else {
                System.out.println("ImageController is not set.");
            }
        } catch (Exception e) {
            System.err.println("Error loading cropped images: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Add images to the FlowPane for cropping
    private void addImagesToFlowPane() {
        try {
            imageFlowPane.getChildren().clear();  // Clear any previous images
            double vboxWidth = 250;  // Width of each VBox
            double vboxHeight = 250; // Height of each VBox
            double imagePadding = 10; // Padding around each image

            for (Image image : cropListImage) {
                if (image != null) {  // Ensure image is not null
                    ImageView imageView = new ImageView(image);
                    // Set the size of the ImageView to fit inside the VBox
                    imageView.setFitWidth(vboxWidth - imagePadding * 2);
                    imageView.setFitHeight(vboxHeight - imagePadding * 2);
                    imageView.setPreserveRatio(true);

                    // Create a BorderPane to center the ImageView
                    BorderPane imagePane = new BorderPane();
                    imagePane.setCenter(imageView);

                    // Create a VBox to add padding, borders, or background
                    VBox imageBox = new VBox(imagePane);
                    imageBox.setPrefSize(vboxWidth, vboxHeight);
                    imageBox.setStyle("-fx-padding: 10; -fx-border-color: black; -fx-background-color: white;");
                    imageBox.setAlignment(Pos.CENTER);  // Center the image inside the VBox

                    // Add the VBox to the FlowPane
                    imageFlowPane.getChildren().add(imageBox);

                    // Initialize CropHandler for each image and ensure the cropping area starts in the center
                    CropHandler cropHandler = new CropHandler(imageView, imagePane, imageScrollPane);
                    cropHandlers.put(imageView, cropHandler);

                }
            }
        } catch (Exception e) {
            System.err.println("Error adding images to FlowPane: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Start cropping for all images
    @FXML
    public void startCropForAll() {
        try {
            for (ImageView imageView : cropHandlers.keySet()) {
                CropHandler cropHandler = cropHandlers.get(imageView);
                cropHandler.startCrop();  // Start crop for each image, centered
            }
            System.out.println("Started cropping for all images.");
        } catch (Exception e) {
            System.err.println("Error starting crop for all images: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Confirm cropping for all images
    @FXML
    public void confirmCropForAll() {
        progressBar.setVisible(true);
        isCropepded = true;  // Set the cropping flag
        croppedListImage.clear();  // Clear the cropped image list

        Task<Void> cropTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    int totalImages = cropHandlers.size();
                    int processedImages = 0;

                    // Use ExecutorService to process images concurrently
                    int numThreads = Math.min(totalImages, Runtime.getRuntime().availableProcessors());
                    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
                    List<Future<Map.Entry<ImageView, WritableImage>>> futures = new ArrayList<>();

                    for (Map.Entry<ImageView, CropHandler> entry : cropHandlers.entrySet()) {
                        ImageView imageView = entry.getKey();
                        CropHandler cropHandler = entry.getValue();

                        // Submit each cropping task to the executor
                        Future<Map.Entry<ImageView, WritableImage>> future = executorService.submit(() -> {
                            String threadName = Thread.currentThread().getName();
                            System.out.println("Processing image on thread: " + threadName);
                            try {
                                WritableImage croppedImage = cropHandler.confirmALlCrop();
                                return new AbstractMap.SimpleEntry<>(imageView, croppedImage);
                            } catch (Exception e) {
                                System.err.println("Error processing image on thread " + threadName + ": " + e.getMessage());
                                e.printStackTrace();
                                return null;  // Return null if there's an error
                            }
                        });

                        futures.add(future);
                    }

                    // Collect results and update progress
                    for (Future<Map.Entry<ImageView, WritableImage>> future : futures) {
                        try {
                            Map.Entry<ImageView, WritableImage> result = future.get();  // This will block until the image is processed
                            if (result != null) {
                                ImageView imageView = result.getKey();
                                WritableImage croppedImage = result.getValue();

                                if (croppedImage != null) {
                                    croppedListImage.add(croppedImage);

                                    // Update the ImageView on the JavaFX Application Thread
                                    Platform.runLater(() -> {
                                        imageView.setImage(croppedImage);
                                    });
                                }
                            }
                        } catch (InterruptedException e) {
                            System.err.println("Image processing was interrupted: " + e.getMessage());
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            System.err.println("Error during image processing: " + e.getCause().getMessage());
                            e.getCause().printStackTrace();
                        }

                        processedImages++;
                        updateProgress(processedImages, totalImages);
                    }

                    executorService.shutdown();

                    return null;
                } catch (Exception e) {
                    System.err.println("Error in cropping task: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }
        };

        cropTask.setOnRunning(e -> {
            System.out.println("Running cropping for all images.");
        });

        cropTask.setOnSucceeded(e -> {
            System.out.println("Confirmed cropping for all images.");
            progressBar.setVisible(false);
        });

        cropTask.setOnFailed(e -> {
            System.err.println("Failed cropping for all images.");
            Throwable exception = cropTask.getException();
            if (exception != null) {
                exception.printStackTrace();
            }
            progressBar.setVisible(false);
        });

        // Bind ProgressBar with Task
        progressBar.progressProperty().bind(cropTask.progressProperty());
        progressBar.setVisible(true);

        // Start Task in a new Thread
        new Thread(cropTask).start();
    }

    @FXML
    public void cancleCropForAll() {
        try {
            for (ImageView imageView : cropHandlers.keySet()) {
                CropHandler cropHandler = cropHandlers.get(imageView);
                cropHandler.cancelCrop();
            }
            System.out.println("Cancelled cropping for all images.");
        } catch (Exception e) {
            System.err.println("Error cancelling crop for all images: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void saveCropForAll(ActionEvent event) {
        try {
            if (isCropepded) {
                imageController.setImageFromBatchCrop(croppedListImage);
            } else {
                imageController.setImageFromBatchCrop(cropListImage);
            }
            isCropepded = false;
            closeCurrentWindow(event);
        } catch (Exception e) {
            System.err.println("Error saving cropped images: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void closeCurrentWindow(ActionEvent event) {
        try {
            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            stage.close();

            if (mainStage != null) {
                mainStage.show();
            }
        } catch (Exception e) {
            System.err.println("Error closing current window: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
