package view;

import controller.CropHandler;
import controller.EdgeDetectionHandler;
import controller.ImageFileHandler;
import controller.ZoomHandler;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import model.edgedetector.detectors.EdgeDetector;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageController {

    @FXML
    private ImageView imageView;
    @FXML
    private ComboBox<String> algorithmChoice;
    @FXML
    private Label statusLabel;
    @FXML
    private BorderPane imagePane;
    @FXML
    public ScrollPane imageScrollPane;
    @FXML
    public ScrollPane listimageScrollPane;
    @FXML
    private Label dropArea;
    @FXML
    private Button startCropButton , confirmCropButton , selectButton,selectAllButton,confirmSelectButton,nextImage,previousImage,detectButton;

    @FXML
    private MenuItem clearStored;
    @FXML
    private HBox imageContainer;
    @FXML
    private TextFlow fileStatus;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressLabel;
    @FXML
    private ImageView pictureIcon;
    @FXML
    private HBox scaleHbox;

    private ImageFileHandler imageFileHandler;
    private CropHandler cropHandler;
    private EdgeDetectionHandler edgeDetectionHandler;
    private ZoomHandler zoomHandler;


    private final Map<String, Class<? extends EdgeDetector>> edgeAlgorithms = new HashMap<>();

    //EdgeDetect Option
    //Robert
    @FXML
    private HBox robertContainer;
    @FXML
    private Slider strengthSlider;
    @FXML
    private Label strengthValueLabel;
    //Lapicain
    @FXML
    private HBox lapicianOption;
    @FXML
    private ComboBox<String> LapicianmaskSizeComboBox;
    //Canny
    @FXML
    private VBox cannyOptions;
    @FXML
    private Slider lowThresholdSlider;
    @FXML
    private Slider highThresholdSlider;
    @FXML
    private Label lowThresholdLabel;
    @FXML
    private Label highThresholdLabel;
    //Sobel
    @FXML
    private ComboBox<String> sobelKernelSizeComboBox;
    @FXML
    private Slider sobelThresholdSlider;
    @FXML
    private Label sobelThresholdLabel;
    @FXML
    private VBox sobelOptions;
    //Prewitt
    @FXML
    private ComboBox<String> prewittKernelSizeComboBox;
    @FXML
    private Slider prewittThresholdSlider;
    @FXML
    private Label prewittThresholdLabel;
    @FXML
    private VBox prewittOptions;
    //Gaussian option
    @FXML
    private Slider gaussianSigmaSlider;
    @FXML
    private ComboBox<String> gaussianKernelSizeComboBox;
    @FXML
    private Label gaussianSigmaLabel;
    @FXML
    private VBox gaussianOptions;
    @FXML
    private HBox setDefaultOption;
    @FXML
    private Button batchCrop;

    // RESULT SYSTEM
    List<Image> selectedListImage = new ArrayList<>();
    @FXML
    private Button confirmEditButton; // ปุ่ม Confirm Edit
    @FXML
    private VBox resultContainer; // ช่องแสดงภาพผลลัพธ์
    @FXML
    private ScrollPane resultScrollPane;
    @FXML
    private Button saveAllButton;
    @FXML
    private boolean isBatchCropped = false;
    @FXML
    private Label uploadImageLabel;
    @FXML
    private Label resultImageLabel;


    private List<Image> originalImages = new ArrayList<>();
    private List<Image> resultListImages = new ArrayList<>();
    private boolean checkDetected = false ;
    @FXML
    private void initialize() {

        cropHandler = new CropHandler(imageView, imagePane, imageScrollPane);
        edgeDetectionHandler = new EdgeDetectionHandler(imageView, statusLabel,algorithmChoice,new HashMap<>());
        imageFileHandler = new ImageFileHandler(imageView, statusLabel,dropArea,fileStatus,imagePane,imageScrollPane,
                listimageScrollPane,startCropButton,confirmCropButton,selectButton,
                selectAllButton,confirmSelectButton,nextImage,previousImage,imageContainer,
                cropHandler,edgeDetectionHandler,clearStored,pictureIcon, uploadImageLabel,scaleHbox);
        zoomHandler = new ZoomHandler(imageView, imageScrollPane);
        setValueOptionAlgorithm();
        imageFileHandler.resetImageClicked();
        resultListImages.clear();
        confirmEditButton.setDisable(true);
        progressBar.setVisible(false);
        progressLabel.setVisible(false);

    }

    @FXML
    public void onGoToBatchCrop() {
        try {
            // Load the BatchCrop FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("BatchCrop.fxml"));
            Parent batchCropView = loader.load();

            // Get the BatchCropController
            BatchCropController batchCropController = loader.getController();

            // ส่ง ImageController (ตัวนี้) ไปยัง BatchCropController
            batchCropController.setImageController(this);

            // ส่ง Stage หลักไปยัง BatchCropController
            Stage mainStage = (Stage) imagePane.getScene().getWindow(); // ใช้ imagePane หรือ Node อื่น ๆ ใน ImageController
            batchCropController.setMainStage(mainStage);

            // Create a new stage for the Batch Crop window
            Stage stage = new Stage();
            stage.setTitle("Batch Crop");
            stage.setScene(new Scene(batchCropView));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error loading BatchCrop.fxml");
        }
    }

    public void setImageFromBatchCrop(List<Image> imageList){
        isBatchCropped = true;
        resultListImages.clear();
        selectedListImage.clear();
        imageFileHandler.setImagesToShow(imageList);
        imageView.setImage(imageFileHandler.getImageSelecting());
        selectedListImage = new ArrayList<>(imageList);
        resultListImages = new ArrayList<>(imageList);
    }

    @FXML
    public void onChooseFile() throws ImageFileHandler.FileSelectionException ,ImageFileHandler.ImageLoadException {
        try {
            imageFileHandler.chooseFile();
            cropHandler.resetCroppedImage();
            uploadImageLabel.setVisible(false);
        } catch (ImageFileHandler.FileSelectionException | ImageFileHandler.ImageLoadException e) {
            statusLabel.setText(e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void onStartCrop() {
        scaleHbox.setVisible(false);
        previousImage.setVisible(false);
        nextImage.setVisible(false);
        cropHandler.startCrop();
    }

    @FXML
    public void onConfirmCrop() throws CropHandler.ImageNotFoundException, CropHandler.InvalidCropAreaException {
        try {
            cropHandler.confirmCrop();
            confirmEditButton.setDisable(false);
            if (imageFileHandler.checkImageClicked()) {

                selectedListImage.clear();
                selectedListImage.add(cropHandler.getCroppedImage());
            } else {

                selectedListImage.set(imageFileHandler.getCurrentIndex(), cropHandler.getCroppedImage());
                imageFileHandler.setCroppedImageInList(cropHandler.getCroppedImage());
            }

            if (!resultListImages.isEmpty()) {
                resultListImages.set(imageFileHandler.getCurrentIndex(), cropHandler.getCroppedImage());
            }
        } catch (CropHandler.InvalidCropAreaException | CropHandler.ImageNotFoundException e) {
            statusLabel.setText(e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
        scaleHbox.setVisible(true);
        previousImage.setVisible(true);
        nextImage.setVisible(true);
    }

    @FXML
    public void onZoomIn() {
        zoomHandler.zoomIn();
    }
    @FXML
    public void onZoomOut() {
        zoomHandler.zoomOut();
    }
    @FXML
    public void onResetZoom() {
        zoomHandler.resetZoom();
    }

    @FXML
    public void onRevertToOriginal() {
        checkDetected = false;
        imageFileHandler.onRevertToOriginal();
        cropHandler.resetCroppedImage();
        selectedListImage = new ArrayList<>(originalImages);
        resultListImages = new ArrayList<>(originalImages);

    }

    @FXML
    public void onSaveImage(){
        imageFileHandler.saveImage(imageView.getImage());
    }


    @FXML
    public void onSaveAllImage() {
        List<Image> imagesToSave = new ArrayList<>();
        for (Node node : resultContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox vbox = (VBox) node;
                for (Node innerNode : vbox.getChildren()) {
                    if (innerNode instanceof ImageView) {
                        imagesToSave.add(((ImageView) innerNode).getImage());
                    }
                }
            }
        }
        imageFileHandler.saveAllImagesAsZip(imagesToSave);
    }

    @FXML
    public void onDetectEdges() {
        try{
        confirmEditButton.setDisable(false);
        progressBar.setStyle("-fx-accent: linear-gradient(to right, #007AFF, #34C759);");

        checkDetected = true;
        isBatchCropped = false;
        String selectedAlgorithm = algorithmChoice.getValue();


        List<Image> imageListDetect = new ArrayList<>(); // Prepare the list of images for detection

        if (imageFileHandler.checkImageClicked()) {
            if (cropHandler.getCroppedImage() != null) {
                selectedListImage.clear();
                selectedListImage.add(cropHandler.getCroppedImage());
            } else {
                selectedListImage = new ArrayList<>(imageFileHandler.getSelectedImages());
            }
        }
        imageListDetect.clear();
        imageListDetect.addAll(selectedListImage); // Add all selected images to the list

        // Determine parameters based on the selected algorithm
        Object[] params = getAlgorithmParameters(selectedAlgorithm);

        progressBar.setVisible(true);
        progressLabel.setVisible(true);

        // Create the batch processing task
        Task<List<Image>> batchTask = edgeDetectionHandler.batchDetectEdges(selectedAlgorithm, imageListDetect, params);

        // Bind progress indicators
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(batchTask.progressProperty());

        batchTask.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            Platform.runLater(() -> progressLabel.setText(newMsg));
        });

        batchTask.setOnRunning(e -> {
            progressLabel.setText("Processing...");
            disableUIControls(true);
        });

        batchTask.setOnSucceeded(e -> {
            List<Image> detectedImages = batchTask.getValue();
            if (detectedImages == null) {
                return;
            }
            imageFileHandler.setImagesToShow(detectedImages); // Update the images to show
            imageView.setImage(imageFileHandler.getImageSelecting());

            resultListImages = detectedImages;

            progressLabel.setText("Processing completed.");
            disableUIControls(false);
            progressBar.setVisible(false);
            progressLabel.setVisible(false);
        });

            batchTask.setOnFailed(e -> {
                Throwable exception = batchTask.getException();
                if (exception instanceof EdgeDetectionHandler.BatchProcessingException) {
                    EdgeDetectionHandler.BatchProcessingException ex = (EdgeDetectionHandler.BatchProcessingException) exception;
                    List<Exception> exceptions = ex.getExceptions();
                    for (Exception exItem : exceptions) {
                        System.err.println(exItem.getMessage());
                    }
                    statusLabel.setText("Edge detection failed for some images.");
                    statusLabel.setStyle("-fx-text-fill: red;");
                } else if (exception instanceof EdgeDetectionHandler.ImageProcessingException ||
                        exception instanceof EdgeDetectionHandler.AlgorithmNotSupportedException) {
                    statusLabel.setText(exception.getMessage());
                    statusLabel.setStyle("-fx-text-fill: red;");
                } else {
                    statusLabel.setText("An unexpected error occurred.");
                    statusLabel.setStyle("-fx-text-fill: red;");
                    exception.printStackTrace();
                }
                disableUIControls(false);
                progressBar.setVisible(false);
                progressLabel.setVisible(false);
            });

            new Thread(batchTask).start();
        } catch (NullPointerException e) {
            statusLabel.setText("Please select an algorithm first.");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private Object[] getAlgorithmParameters(String algorithm) {


            switch (algorithm) {
                case "Sobel":
                    int sobelKernelSize = "3x3".equals(sobelKernelSizeComboBox.getValue()) ? 3 : 5;
                    int sobelThreshold = (int) sobelThresholdSlider.getValue();
                    return new Object[]{sobelKernelSize, sobelThreshold};
                case "Gaussian":
                    double sigma = gaussianSigmaSlider.getValue();
                    int kernelSize = "3x3".equals(gaussianKernelSizeComboBox.getValue()) ? 3 :
                            ("5x5".equals(gaussianKernelSizeComboBox.getValue()) ? 5 : 7);
                    return new Object[]{sigma, kernelSize};
                case "Canny":
                    int lowThreshold = (int) lowThresholdSlider.getValue();
                    int highThreshold = (int) highThresholdSlider.getValue();
                    return new Object[]{lowThreshold, highThreshold};
                case "Roberts Cross":
                    double strength = strengthSlider.getValue();
                    return new Object[]{strength};
                case "Laplacian":
                    String maskSize = LapicianmaskSizeComboBox.getValue();
                    return new Object[]{maskSize};
                case "Prewitt":
                    int prewittKernelSize = "3x3".equals(prewittKernelSizeComboBox.getValue()) ? 3 : 5;
                    int prewittThreshold = (int) prewittThresholdSlider.getValue();
                    return new Object[]{prewittKernelSize, prewittThreshold};
                default:
                    return new Object[]{};
            }

    }

    private void disableUIControls(boolean disable) {
        Platform.runLater(() -> {
            startCropButton.setDisable(disable);
            confirmCropButton.setDisable(disable);
            selectButton.setDisable(disable);
            selectAllButton.setDisable(disable);
            confirmSelectButton.setDisable(disable);
            nextImage.setDisable(disable);
            previousImage.setDisable(disable);
            algorithmChoice.setDisable(disable);
        });
    }

    //Method Option EdgeDetection
    private void setValueOptionAlgorithm() {

        //gaussian
        gaussianKernelSizeComboBox.getItems().clear();
        gaussianKernelSizeComboBox.getItems().addAll("3x3", "5x5", "7x7");
        gaussianKernelSizeComboBox.setValue("5x5");
        gaussianSigmaSlider.setMin(0.5);
        gaussianSigmaSlider.setMax(5.0);
        gaussianSigmaSlider.setValue(1.4);
        gaussianSigmaLabel.setText(String.format("%.1f", gaussianSigmaSlider.getValue()));

        gaussianSigmaSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            gaussianSigmaLabel.setText(String.format("%.1f", newValue.doubleValue()));
        });

        //sobel
        sobelKernelSizeComboBox.getItems().clear();
        sobelKernelSizeComboBox.getItems().addAll("3x3", "5x5");
        sobelKernelSizeComboBox.setValue("3x3");
        sobelThresholdSlider.setMin(0);
        sobelThresholdSlider.setMax(350);
        sobelThresholdSlider.setValue(105); // Default threshold value
        sobelThresholdLabel.setText(String.format("%.0f", sobelThresholdSlider.getValue()));
        sobelThresholdSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            sobelThresholdLabel.setText(String.format("%.0f", newValue.doubleValue()));
        });


        //prewitt
        prewittKernelSizeComboBox.getItems().clear();
        prewittKernelSizeComboBox.getItems().addAll("3x3", "5x5");
        prewittKernelSizeComboBox.setValue("3x3");
        prewittThresholdSlider.setMin(0);
        prewittThresholdSlider.setMax(120);
        prewittThresholdSlider.setValue(25); // Default threshold value
        prewittThresholdLabel.setText(String.format("%.0f", prewittThresholdSlider.getValue()));

        prewittThresholdSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            prewittThresholdLabel.setText(String.format("%.0f", newValue.doubleValue()));
        });

        //Lapician
        LapicianmaskSizeComboBox.getItems().clear();
        LapicianmaskSizeComboBox.getItems().addAll("3x3", "5x5" , "7x7");
        LapicianmaskSizeComboBox.setValue("3x3");

        //Robert
        strengthSlider.setValue(4);
        strengthSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            strengthValueLabel.setText(String.format("%.0f", newValue.doubleValue()));
        });

        //Canny
        lowThresholdSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() > highThresholdSlider.getValue()) {
                lowThresholdSlider.setValue(highThresholdSlider.getValue()); // Reset lowThreshold to highThreshold value
            } else {
                lowThresholdLabel.setText(String.format("%.1f", newValue.doubleValue()));
            }
        });

        highThresholdSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() < lowThresholdSlider.getValue()) {
                highThresholdSlider.setValue(lowThresholdSlider.getValue()); // Reset highThreshold to lowThreshold value
            } else {
                highThresholdLabel.setText(String.format("%.1f", newValue.doubleValue()));
            }
        });

        lowThresholdSlider.setMin(1);
        lowThresholdSlider.setMax(150);
        lowThresholdSlider.setValue(20);
        highThresholdSlider.setMin(1);
        highThresholdSlider.setMax(90);
        highThresholdSlider.setValue(40);
        lowThresholdLabel.setText(String.format("%.1f", lowThresholdSlider.getValue()));
        highThresholdLabel.setText(String.format("%.1f", highThresholdSlider.getValue()));

        if (algorithmChoice.getItems().isEmpty()) {
            algorithmChoice.getItems().addAll("Canny", "Sobel", "Laplacian", "Prewitt", "Roberts Cross", "Gaussian");
        }
        algorithmChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateAlgorithmOptions(newValue);
        });

    }

    private void updateAlgorithmOptions(String algorithm) {
        robertContainer.setVisible(false);
        lapicianOption.setVisible(false);
        cannyOptions.setVisible(false);
        sobelOptions.setVisible(false);
        prewittOptions.setVisible(false);
        gaussianOptions.setVisible(false);
        setDefaultOption.setVisible(false);

        switch (algorithm) {
            case "Sobel":
                sobelOptions.setVisible(true);
                setDefaultOption.setVisible(true);
                break;
            case "Roberts Cross":
                robertContainer.setVisible(true);
                setDefaultOption.setVisible(true);
                break;
            case "Laplacian":
                lapicianOption.setVisible(true);
                setDefaultOption.setVisible(true);
                break;
            case "Canny":
                cannyOptions.setVisible(true);
                setDefaultOption.setVisible(true);
                break;
            case "Prewitt":
                prewittOptions.setVisible(true);
                setDefaultOption.setVisible(true);
                break;
            case "Gaussian":
                gaussianOptions.setVisible(true);
                setDefaultOption.setVisible(true);
                break;
            default:
                break;
        }
    }

    @FXML
    private void setDefaults() {
        // Gaussian Defaults
        gaussianKernelSizeComboBox.setValue("5x5");
        gaussianSigmaSlider.setValue(1.4);
        gaussianSigmaLabel.setText(String.format("%.1f", gaussianSigmaSlider.getValue()));

        // Sobel Defaults
        sobelKernelSizeComboBox.setValue("3x3");
        sobelThresholdSlider.setValue(105);
        sobelThresholdLabel.setText(String.format("%.0f", sobelThresholdSlider.getValue()));

        // Prewitt Defaults
        prewittKernelSizeComboBox.setValue("3x3");
        prewittThresholdSlider.setValue(25);
        prewittThresholdLabel.setText(String.format("%.0f", prewittThresholdSlider.getValue()));

        // Laplacian Defaults
        LapicianmaskSizeComboBox.setValue("3x3");

        // Roberts Cross Defaults
        strengthSlider.setValue(4);
        strengthValueLabel.setText(String.format("%.0f", strengthSlider.getValue()));

        // Canny Defaults
        lowThresholdSlider.setValue(20);
        highThresholdSlider.setValue(40);
        lowThresholdLabel.setText(String.format("%.1f", lowThresholdSlider.getValue()));
        highThresholdLabel.setText(String.format("%.1f", highThresholdSlider.getValue()));

        statusLabel.setText("Parameters reset to default values.");
        statusLabel.setStyle("-fx-text-fill: green;");
    }


    @FXML
    public void onConfirmEdit() {
        confirmEditButton.setStyle("-fx-background-color: #34C759; -fx-text-fill: white;"); // สีเขียว Apple

        if(!checkDetected && !isBatchCropped){
            resultListImages = selectedListImage;
            selectedListImage = new ArrayList<>(originalImages);
            addImageToResultContainer(resultListImages);
            cropHandler.resetCroppedImage();
            imageFileHandler.setImagesToShow(originalImages);
            imageFileHandler.onRevertToOriginal();
            resultListImages = originalImages;
            statusLabel.setText("Edited Original image confirmed successfully.");
            statusLabel.setStyle("-fx-text-fill: green;");

        }
        else if (resultListImages != null && checkDetected) {
            checkDetected = false;
            selectedListImage = new ArrayList<>(originalImages);
            cropHandler.resetCroppedImage();
            addImageToResultContainer(resultListImages);
            imageFileHandler.setImagesToShow(originalImages);
            imageFileHandler.onRevertToOriginal(); // Reset the image view to original after confirming edit
            resultListImages = new ArrayList<>(originalImages);
            statusLabel.setText("Edited Detected image confirmed successfully.");
            statusLabel.setStyle("-fx-text-fill: green;");
        } else if (resultListImages != null && isBatchCropped){
            isBatchCropped = false;
            selectedListImage = new ArrayList<>(originalImages);
            cropHandler.resetCroppedImage();
            addImageToResultContainer(resultListImages);
            imageFileHandler.setImagesToShow(originalImages);
            imageFileHandler.onRevertToOriginal();
            resultListImages = originalImages;
            statusLabel.setText("Edited BatchCropped image confirmed successfully.");
            statusLabel.setStyle("-fx-text-fill: green;");
        }
        else {
            statusLabel.setText("No edited image to confirm.");
            statusLabel.setStyle("-fx-text-fill: red;");
        }


    }

    private void addImageToResultContainer(List<Image> imageList) {
        resultImageLabel.setVisible(false);
        VBox imageBox = new VBox();
        imageBox.setSpacing(5);
        // Create an ImageView for the result image
        for (Image image : imageList) {
            ImageView resultImageView = new ImageView(image);
            resultImageView.setFitWidth(150); // Set the width
            resultImageView.setFitHeight(150); // Set the height
            resultImageView.setPreserveRatio(true); // Preserve aspect ratio
            resultImageView.setSmooth(true);

            resultImageView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) { // Check if the image is clicked once
                    imageView.setImage(resultImageView.getImage()); // Set the main ImageView to the selected image
                    confirmEditButton.setDisable(false); // Enable the Confirm Edit button
                    statusLabel.setText("Image selected for further editing."); // Update status message
                    statusLabel.setStyle("-fx-text-fill: green;"); // Set status label color to green
                }
            });
            imageBox.getChildren().add(resultImageView); // Add ImageView to VBox
        }

        // Add the VBox containing the ImageView to the resultContainer
        resultContainer.getChildren().add(imageBox);
        // Enable the Save All button if there are images in the result area
        saveAllButton.setDisable(resultContainer.getChildren().isEmpty());
    }

    @FXML
    private void onSelectImages() {

        imageFileHandler.selectImages();
    }

    @FXML
    private void onSelectAllImages() {
        imageFileHandler.selectAllImages();
    }

    public List<Image> getResultListImages(){
        return resultListImages;
    }

    @FXML
    private void onConfirmSelect() {
        pictureIcon.setVisible(false);
        confirmEditButton.setDisable(false);
        detectButton.setDisable(false);
        imageFileHandler.confirmSelect();
        selectedListImage = imageFileHandler.getSelectedImages();
        originalImages = imageFileHandler.getSelectedImages();
        resultListImages = imageFileHandler.getSelectedImages();
        batchCrop.setDisable(false);


    }
    @FXML
    private void onClearStored(){
        imageFileHandler.onClearStoredList();
    }

    @FXML
    private void onNextImage(){
        imageFileHandler.onNextImage();
    }
    @FXML
    private void onPreviousImage(){
        imageFileHandler.onPreviousImage();
    }

    @FXML
    private void onClearListImage(){
        scaleHbox.setVisible(false);
        previousImage.setVisible(false);
        nextImage.setVisible(false);
        imageView.setImage(null);
        this.selectedListImage.clear();
        imageFileHandler.clearSelectedImages();
        imageFileHandler.clearImagesToShow();

        // ปิดการทำงานของปุ่มที่ไม่จำเป็นเมื่อไม่มีภาพ
        confirmEditButton.setDisable(true);
        saveAllButton.setDisable(true);

        // ตั้งค่าข้อความสถานะเพื่อแจ้งผู้ใช้ว่าภาพทั้งหมดถูกลบแล้ว
        statusLabel.setText("All images cleared.");
        statusLabel.setStyle("-fx-text-fill: red;");
        pictureIcon.setVisible(true);
    }

    @FXML
    private void onClearResultImage(){
        resultImageLabel.setVisible(true);
        resultListImages.clear();
        resultContainer.getChildren().clear();

        saveAllButton.setDisable(true);
        statusLabel.setText("Result images cleared.");
        statusLabel.setStyle("-fx-text-fill: red;");
    }

}
