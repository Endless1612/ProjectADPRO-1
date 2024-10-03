package controller;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


//zip
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.FileInputStream;
import java.util.zip.ZipOutputStream;

// Main class for handling image file operations
public class ImageFileHandler {

    // Fields for handling images, UI components, and various handlers
    private final ImageView imageView;
    private File selectedFile;
    public Image originalImage;
    private String originalFilename;
    private ZoomHandler zoomHandler;
    private CropHandler cropHandler;
    private EdgeDetectionHandler edgeDetectionHandler;
    private ImageView selectedImageView;
    // UI components
    @FXML
    private Label statusLabel;
    @FXML
    private Label dropArea;
    @FXML
    private ScrollPane imageScrollPane, listimageScrollPane;
    @FXML
    private BorderPane imagePane;
    @FXML
    private Button startCropButton;
    @FXML
    private Button confirmCropButton;
    @FXML
    private Button confirmSelectButton;
    @FXML
    private Button nextImage;
    @FXML
    private Button previousImage;
    @FXML
    private MenuItem clearStored;

    private HBox imageContainer;
    @FXML
    private TextFlow filestatusTextFlow;

    @FXML
    private ImageView pictureIcon;
    @FXML
    private Label uploadImageLabel;
    @FXML
    private  HBox scaleHbox;

    // Custom Exceptions
    public static class FileSelectionException extends Exception {
        public FileSelectionException(String message) {
            super(message);
        }
    }
    public static class ImageLoadException extends Exception {
        public ImageLoadException(String message) {
            super(message);
        }
    }

    // Lists for handling images
    private List<Image> selectedImages = new ArrayList<>();
    private List<Image> imagesToShow = new ArrayList<>();
    private List<File> ListFiles;
    private List<Image> orginalList = new ArrayList<>();
    private int currentIndex = 0;
    private boolean imageClicked = false;


    public ImageFileHandler(ImageView imageView,
                            Label statusLabel,
                            Label dropArea,
                            TextFlow filestatusTextFlow,
                            BorderPane imagePane,
                            ScrollPane imageScrollPane,
                            ScrollPane listimageScrollPane,
                            Button startCropButton,
                            Button confirmCropButton,
                            Button selectButton,
                            Button selectAllButton,
                            Button confirmSelectButton,
                            Button nextImage,
                            Button previousImage,
                            HBox imageContainer,
                            CropHandler cropHandler,
                            EdgeDetectionHandler edgeDetectionHandler
                            ,MenuItem clearStored ,
                            ImageView pictureIcon,
                            Label uploadImageLabel,
                            HBox scaleHbox) {

        this.imageView = imageView;
        this.statusLabel = statusLabel;
        this.dropArea = dropArea;
        this.filestatusTextFlow = filestatusTextFlow;
        this.imagePane = imagePane;
        this.imageScrollPane = imageScrollPane;
        this.listimageScrollPane = listimageScrollPane;
        this.zoomHandler = new ZoomHandler(imageView, imageScrollPane);
        this.cropHandler = cropHandler;
        this.startCropButton = startCropButton;
        this.confirmCropButton = confirmCropButton;
        this.confirmSelectButton = confirmSelectButton;
        this.nextImage = nextImage;
        this.previousImage = previousImage;
        this.imageContainer = imageContainer;
        this.imageContainer.setVisible(false);
        this.ListFiles = new ArrayList<>();
        this.edgeDetectionHandler = edgeDetectionHandler;
        this.clearStored = clearStored;
        this.pictureIcon = pictureIcon;
        this.uploadImageLabel = uploadImageLabel;
        this.scaleHbox = scaleHbox;
        imageContainer.getChildren().clear();
        setUpDragAndDrop();

        imageScrollPane.setFitToWidth(true);
        imageScrollPane.setFitToHeight(true);
        imagePane.setCenterShape(true);
        imagePane.setStyle("-fx-alignment: center;");
        orginalList.clear();
        selectedImages.clear();
        imagesToShow.clear();
    }

    // ===================== File Handling Methods =====================

    // Method to load image files or extract from ZIP
    public void chooseFile() throws FileSelectionException, ImageLoadException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Image File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("ZIP Files", "*.zip")
        );

        List<File> newFiles = fileChooser.showOpenMultipleDialog(new Stage());
        if (newFiles != null && !newFiles.isEmpty()) {
            List<File> allFiles = new ArrayList<>();
            for (File file : newFiles) {

                if (file.getName().endsWith(".zip")) {

                    List<File> extractedFiles = extractImagesFromZip(file);
                    allFiles.addAll(extractedFiles);

                } else if (file.getName().endsWith(".png") || file.getName().endsWith(".jpg") || file.getName().endsWith(".jpeg")) {
                        allFiles.add(file);
                } else {
                throw new FileSelectionException("Unsupported file format: " + file.getName());
                }
            }
            LoadandprocessFileStatus(allFiles);

            Optional<File> firstValidFile = newFiles.stream()
                    .filter(file -> checkImageInFileList(file.getName()))
                    .findFirst();

            if (firstValidFile.isPresent()) {
                this.selectedFile = firstValidFile.get();
                originalImage = new Image(selectedFile.toURI().toString());
                imageView.setImage(originalImage);
                originalFilename = selectedFile.getName();
                statusLabel.setText("File loaded: " + originalFilename);
                statusLabel.setStyle("-fx-text-fill: green;");

                cropHandler.resetCroppedImage();
                startCropButton.setDisable(false);
                confirmCropButton.setDisable(false);
                zoomHandler.resetZoom(); // Reset zoom when a new image is loaded
                imagePane.minWidthProperty().bind(imageScrollPane.widthProperty());
                imagePane.minHeightProperty().bind(imageScrollPane.heightProperty());
            }
        } else {
            statusLabel.setText("File selection cancelled.");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    // Method to extract images from a ZIP file
    private List<File> extractImagesFromZip(File zipFile) {
        List<File> extractedImages = new ArrayList<>();
        String tempDir = System.getProperty("java.io.tmpdir"); // Use the system temp directory

        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().startsWith("__MACOSX") || entry.getName().startsWith("._") ||
                        !(entry.getName().endsWith(".png") || entry.getName().endsWith(".jpg") || entry.getName().endsWith(".jpeg"))) {
                    continue; // Skip this entry
                }

                File extractedFile = new File(tempDir, entry.getName());
                extractedFile.getParentFile().mkdirs();

                try (FileOutputStream fileOutputStream = new FileOutputStream(extractedFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = zipInputStream.read(buffer)) > 0) {
                        fileOutputStream.write(buffer, 0, length);
                    }
                }
                extractedImages.add(extractedFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Failed to extract images from ZIP file.");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
        return extractedImages;
    }

    // Method to save a single image to a file
    public void saveImage(Image imageToSave) {
        if (imageToSave == null) {
            statusLabel.setText("No image to save.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        if (selectedFile == null) {
            statusLabel.setText("No file selected to save.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Image");
        if (originalFilename != null && !originalFilename.isEmpty()) {
            String fileNameWithoutExtension = generateNewFileName(selectedFile);
            fileChooser.setInitialFileName(fileNameWithoutExtension);
        }
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Files", "*.png"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPG Files", "*.jpg"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Files", "*.zip"));
        File saveFile = fileChooser.showSaveDialog(new Stage());

        if (saveFile != null) {
            try {
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(imageToSave, null);
                ImageIO.write(bufferedImage, "png", saveFile);
                statusLabel.setText("Image saved successfully.");
                statusLabel.setStyle("-fx-text-fill: green;");
            } catch (IOException e) {
                e.printStackTrace();
                statusLabel.setText("Failed to save image.");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        }
    }

    // Method to save all images as a ZIP file
    public void saveAllImagesAsZip(List<Image> images) {
        if (images == null || images.isEmpty()) {
            statusLabel.setText("No images to save.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        try {
            // สร้างชื่อไฟล์ ZIP อัตโนมัติ
            String zipFileName = generateZipFileName();
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save All Images as ZIP");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP files", "*.zip"));

            // ตั้งชื่อไฟล์ ZIP ที่จะบันทึก
            fileChooser.setInitialFileName(zipFileName);
            File file = fileChooser.showSaveDialog(null);

            if (file == null) {
                return; // ยกเลิกการบันทึกหากผู้ใช้ไม่ได้เลือกไฟล์
            }

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
                for (int i = 0; i < images.size(); i++) {
                    // ตั้งชื่อไฟล์ภายใน ZIP เป็นชื่อเดิม
                    String imageName = "image_" + (i + 1) + ".png"; // หรือใช้ชื่อเดิมตามที่กำหนดไว้
                    BufferedImage bufferedImage = SwingFXUtils.fromFXImage(images.get(i), null);
                    ZipEntry zipEntry = new ZipEntry(imageName);
                    zos.putNextEntry(zipEntry);

                    ImageIO.write(bufferedImage, "png", zos);
                    zos.closeEntry();
                }
            }

            statusLabel.setText("Images saved successfully as " + zipFileName);
            statusLabel.setStyle("-fx-text-fill: green;");

        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Failed to save images.");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    // Method to generate a new file name to avoid conflicts
    private String generateNewFileName(File originalFile) {
        String originalFileName = originalFile.getName();
        String baseName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        String extension = originalFileName.substring(originalFileName.lastIndexOf('.'));

        String newFileName = baseName + "1" + extension;
        int counter = 1;

        while (new File(originalFile.getParent(), newFileName).exists()) {
            counter++;
            newFileName = baseName + counter + extension;
        }

        return newFileName;
    }
    private String generateZipFileName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        return "images_" + timestamp + ".zip"; // ชื่อไฟล์ ZIP เช่น images_20240928_124500.zip
    }

    // Method to truncate file names that are too long
    private String truncateFileName(String fileName, int maxLength) {
        if (fileName.length() > maxLength) {
            return fileName.substring(0, maxLength - 3) + "...";
        } else {
            return fileName;
        }
    }

    // ===================== Image Selection and Display Methods =====================

    // Method to select images with checkboxes
    @FXML
    public void selectImages() {
        for (Node node : imageContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox hbox = (HBox) node;
                for (Node child : hbox.getChildren()) {
                    if (child instanceof CheckBox) {
                        CheckBox checkBox = (CheckBox) child;
                        checkBox.setVisible(true);
                    }
                }
            }
        }

        confirmSelectButton.setDisable(false);
    }

    // Method to select all images with checkboxes
    @FXML
    public void selectAllImages() {
        for (Node node : imageContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox hbox = (HBox) node;
                for (Node child : hbox.getChildren()) {
                    if (child instanceof CheckBox) {
                        CheckBox checkBox = (CheckBox) child;
                        checkBox.setVisible(true);
                        checkBox.setSelected(true);
                    }
                }
            }
        }
        confirmSelectButton.setDisable(false);

    }

    // Method to confirm the selected images
    @FXML
    public void confirmSelect() {
        scaleHbox.setVisible(true);
        nextImage.setVisible(true);
        previousImage.setVisible(true);
        orginalList.clear();
        currentIndex = 0 ;
        resetImageClicked();
        selectedImages.clear();
        startCropButton.setDisable(false);
        confirmCropButton.setDisable(false);
        for (Node node : imageContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox hbox = (HBox) node;
                ImageView imageView = null;
                CheckBox checkBox = null;

                for (Node child : hbox.getChildren()) {
                    if (child instanceof ImageView) {
                        imageView = (ImageView) child;
                    } else if (child instanceof CheckBox) {
                        checkBox = (CheckBox) child;
                    }
                }

                if (checkBox != null && checkBox.isSelected() && imageView != null) {
                    selectedImages.add(imageView.getImage());
                }

                if (checkBox != null) {
                    checkBox.setVisible(false);
                    checkBox.setSelected(false);
                }
            }
        }

        setImagesToShow(selectedImages);
        orginalList = new ArrayList<>(selectedImages);

        if (!selectedImages.isEmpty()) {
            imageView.setImage(selectedImages.get(0));
            statusLabel.setText(selectedImages.size() + " images selected for processing.");
            statusLabel.setStyle("-fx-text-fill: green;");
        } else {
            statusLabel.setText("No images selected.");
            statusLabel.setStyle("-fx-text-fill: red;");
        }

        confirmSelectButton.setDisable(true);
    }

    // Method to show the current image based on index
    public void showImageAtCurrentIndex() {
        if (imagesToShow.isEmpty()) {
            statusLabel.setText("No images to display.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if (currentIndex < 0) currentIndex = 0;
        if (currentIndex >= imagesToShow.size()) currentIndex = imagesToShow.size() - 1;

        Image currentImage = imagesToShow.get(currentIndex);
        imageView.setImage(currentImage);


        imagePane.minWidthProperty().bind(imageScrollPane.widthProperty());
        imagePane.minHeightProperty().bind(imageScrollPane.heightProperty());

        statusLabel.setText("Displaying image " + (currentIndex + 1) + " of " + imagesToShow.size());
        statusLabel.setStyle("-fx-text-fill: green;");
    }

    // Method to show the next image
    @FXML
    public void onNextImage() {
        currentIndex++;
        if (currentIndex >= imagesToShow.size()) {
            currentIndex = imagesToShow.size() - 1;
            statusLabel.setText("Reached the last image.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        showImageAtCurrentIndex();
    }

    // Method to show the previous image
    @FXML
    public void onPreviousImage() {
        currentIndex--;
        if (currentIndex < 0) {
            currentIndex = 0;
            statusLabel.setText("Reached the first image.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        showImageAtCurrentIndex();
    }

    // Method to revert to the original image
    @FXML
    public void onRevertToOriginal() {

        if (!imagesToShow.isEmpty()) {

            setImagesToShow(orginalList);
            imageView.setImage(orginalList.get(currentIndex));

            statusLabel.setText("Reverted to original image.");
            statusLabel.setStyle("-fx-text-fill: green;");
            zoomHandler.resetZoom();

        } else {
            statusLabel.setText("No images to display.");
            statusLabel.setStyle("-fx-text-fill: red;");
        }

    }

    // Method to handle image click events
    public void ImageClicked(Image image, File imageFile) {
        scaleHbox.setVisible(true);
        pictureIcon.setVisible(false);
        nextImage.setVisible(false);
        previousImage.setVisible(false);
        currentIndex = 0 ;
        imageClicked = true;
        clearImagesToShow();
        clearSelectedImages();
        orginalList.clear();
        selectedImages.add(image);
        orginalList.add(image);
        setImagesToShow(selectedImages);
        cropHandler.resetCroppedImage();
        zoomHandler.resetZoom();
        this.imageView.setImage(image);
        this.selectedFile = imageFile;
        this.originalFilename = imageFile.getName();
        statusLabel.setText("Selecting Image: " + imageFile.getName());
        statusLabel.setStyle("-fx-text-fill: green;");
        filestatusTextFlow.getChildren().clear();

        startCropButton.setDisable(false);
        confirmCropButton.setDisable(false);
        imagePane.minWidthProperty().bind(imageScrollPane.widthProperty());
        imagePane.minHeightProperty().bind(imageScrollPane.heightProperty());
    }

    //Method To return imageClicked Or Not
    public boolean checkImageClicked(){
        return imageClicked;
    }

    //Method To resetImageClicked
    public void resetImageClicked(){
        imageClicked = false;
    }

    // Method to clear selected images
    public void clearSelectedImages() {
        selectedImages.clear();
    }

    // Method to retrieve selected images
    public List<Image> getSelectedImages() {
        return new ArrayList<>(selectedImages);
    }

    public void onClearStoredList(){
        System.out.println("CLEAR SUCCESS");
        imageContainer.getChildren().clear();
        uploadImageLabel.setVisible(true);
        ListFiles.clear();
        statusLabel.setText("All stored images have been cleared.");
        statusLabel.setStyle("-fx-text-fill: green;");
    }

    // ===================== Utility and Setup Methods =====================

    // Method to set up drag and drop for the drop area
    private void setUpDragAndDrop() {
        dropArea.setOnDragOver(event -> {
            if (event.getGestureSource() != dropArea && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        dropArea.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                List<File> dropFiles = db.getFiles();
                List<File> allFiles = new ArrayList<>();

                for (File file : dropFiles) {
                    if (file.getName().endsWith(".zip")) {
                        List<File> extractedFiles = extractImagesFromZip(file);
                        allFiles.addAll(extractedFiles);
                    } else {
                        allFiles.add(file);
                    }
                }

                LoadandprocessFileStatus(allFiles);

                Optional<File> firstValidFile = dropFiles.stream()
                        .filter(file -> checkImageInFileList(file.getName()))
                        .findFirst();

                if (firstValidFile.isPresent()) {
                    this.selectedFile = firstValidFile.get();
                    originalImage = new Image(selectedFile.toURI().toString());
                    imageView.setImage(originalImage);
                    originalFilename = selectedFile.getName();
                    statusLabel.setText("File loaded: " + originalFilename);
                    statusLabel.setStyle("-fx-text-fill: green;");

                    cropHandler.resetCroppedImage();
                    startCropButton.setDisable(false);
                    confirmCropButton.setDisable(false);
                    zoomHandler.resetZoom();
                    imagePane.minWidthProperty().bind(imageScrollPane.widthProperty());
                    imagePane.minHeightProperty().bind(imageScrollPane.heightProperty());
                }
            } else {
                statusLabel.setText("Drag and drop failed.");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
            event.setDropCompleted(db.hasFiles());
            event.consume();
            uploadImageLabel.setVisible(false);
        });
    }

    // Method to process and update status of loaded files
    private void LoadandprocessFileStatus(List<File> imageFiles) {
        List<String> fileStatusList = new ArrayList<>();

        for (File imageFile : imageFiles) {
            String truncatedFileName = truncateFileName(imageFile.getName(), 30);

            if (checkImageInFileList(imageFile.getName())) {
                addImage(imageFile);
                ListFiles.add(imageFile);
                fileStatusList.add("Upload New File: " + truncatedFileName);
            } else {
                fileStatusList.add("File Already Exists: " + truncatedFileName);

            }
        }
        updateStatusLabel(fileStatusList);
    }

    // Method to add an image to the view with a checkbox
    public void addImage(File imageFile) {
        Image image = new Image(imageFile.toURI().toString());

        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(170);
        imageView.setFitWidth(170);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        CheckBox checkBox = new CheckBox();
        checkBox.setVisible(false);

        Tooltip tooltip = new Tooltip(imageFile.getName());
        Tooltip.install(imageView, tooltip);

        HBox imageBoxWithLabel = new HBox();
        imageBoxWithLabel.setSpacing(10);
        imageBoxWithLabel.getChildren().addAll(imageView, checkBox);

        imageContainer.getChildren().add(imageBoxWithLabel);

        imageView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                ImageClicked(image, imageFile);
            }
        });

        if (!imageContainer.isVisible()) {
            imageContainer.setVisible(true);
        }
    }
    private void updateStatusLabel(List<String> statusMessages) {
        filestatusTextFlow.getChildren().clear();

        for (String message : statusMessages) {
            Text text = new Text(message + "\n");
            if (message.startsWith("Upload New File:")) {
                text.setStyle("-fx-fill: green;");
            } else {
                text.setStyle("-fx-fill: red;");
            }
            filestatusTextFlow.getChildren().add(text);
        }
    }

    public void setCroppedImageInList(Image croppedImage){
        imagesToShow.set(currentIndex,croppedImage);
    }


    // Utility Methods to set and get images to show
    public void setImagesToShow(List<Image> imageList) {
        imagesToShow = new ArrayList<>(imageList);
    }

    public void clearImagesToShow(){
        imagesToShow.clear();
    }


    public Image getImageSelecting() {
        if (imagesToShow != null && !imagesToShow.isEmpty() && currentIndex >= 0 && currentIndex < imagesToShow.size()) {
            return imagesToShow.get(currentIndex);
        }
        return null;
    }
    public int getCurrentIndex(){
        return currentIndex;
    }

    // Method to check if image file is already in the list
    public boolean checkImageInFileList(String fileName) {
        if (this.ListFiles == null || this.ListFiles.isEmpty()) {
            return true;
        }
        for (File imageFile : this.ListFiles) {
            if (imageFile.getName().equals(fileName)) {
                return false;
            }
        }
        return true;
    }



}
