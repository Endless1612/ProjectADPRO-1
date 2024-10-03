package controller;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import model.crop.ResizableRectangle;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class CropHandler {

    private final ImageView imageView;
    private Image croppedImage;
    private final Pane imagePane;
    private final FlowPane imageFlowPane = new FlowPane();
    private final ScrollPane imageScrollPane;
    private ResizableRectangle selectionRectangle;
    private Rectangle darkArea;
    private boolean isAreaSelected = false;
    public boolean isCroppingActive = false;

    private final Map<ImageView, ResizableRectangle> selectionRectangles = new HashMap<>();

    // Custom Exceptions
    public static class InvalidCropAreaException extends Exception {
        public InvalidCropAreaException(String message) {
            super(message);
        }
    }

    public static class ImageNotFoundException extends Exception {
        public ImageNotFoundException(String message) {
            super(message);
        }
    }

    public CropHandler(ImageView imageView, BorderPane imagePane, ScrollPane imageScrollPane) {
        this.imageView = imageView;
        this.imagePane = imagePane;
        this.imageScrollPane = imageScrollPane;
        setupCropArea();

        imagePane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE && isCroppingActive) {
                        cancelCrop();
                    }
                });
            }
        });
    }



    private void setupCropArea() {
        darkArea = new Rectangle();
        darkArea.setFill(Color.color(0, 0, 0, 0.5));
        darkArea.setVisible(false);
        imagePane.getChildren().add(darkArea);
    }


    public void startCrop() {
        isCroppingActive = true;
        imageScrollPane.setPannable(false);
        removeExistingSelection();

//        double imageWidth = imageView.getFitWidth();
//        double imageHeight = imageView.getFitHeight();

        double imageWidth =  imagePane.getWidth();
        double imageHeight = imagePane.getHeight();
        double rectWidth = imageWidth / 2;
        double rectHeight = imageHeight / 2;

        double rectX = (imageWidth - rectWidth) / 2;
        double rectY = (imageHeight - rectHeight) / 2;


        selectionRectangle = new ResizableRectangle(rectX, rectY, rectWidth, rectHeight, imagePane, this::updateDarkArea);

        isAreaSelected = true;
        updateDarkArea();


        imagePane.requestFocus();

    }

    public void confirmCrop() throws InvalidCropAreaException, ImageNotFoundException {
        if (isAreaSelected && selectionRectangle != null) {
            imageScrollPane.setPannable(true);
            cropImage(selectionRectangle.getBoundsInParent());

            removeExistingSelection();
            selectionRectangle = null;
            isAreaSelected = false;
            darkArea.setVisible(false);
            isCroppingActive = false;
        } else {
            throw new InvalidCropAreaException("No area selected for cropping.");
        }
    }

    // Return the cropped image after confirmation
    public WritableImage confirmALlCrop() {
        if (isAreaSelected && selectionRectangle != null) {
            imageScrollPane.setPannable(true);
            WritableImage cropped = cropAllImage(selectionRectangle.getBoundsInParent());

            Platform.runLater(() -> {
                imageScrollPane.setPannable(true);  // ปรับ UI: เปิดการเลื่อน ScrollPane
                removeExistingSelection();  // ลบการเลือก
                selectionRectangle = null;
                isAreaSelected = false;
                darkArea.setVisible(false);  // ซ่อนพื้นที่ที่มืด
                isCroppingActive = false;
            });

            return cropped;  // Return the cropped image
        }
        return null;  // Return null if no cropping is done
    }

    public void cancelCrop() {
        removeExistingSelection();
        removeDarkArea();
        imageScrollPane.setPannable(true);
        isAreaSelected = false;
        isCroppingActive = false;
    }

    private void cropImage(Bounds bounds) throws ImageNotFoundException {
        Image image = imageView.getImage();
        if (image == null) {
            throw new ImageNotFoundException("Image is null.");
        }

        // แปลงพิกัดของสี่เหลี่ยมเลือกพื้นที่จากพิกัดของ parent ให้เป็นพิกัดของ ImageView
        Bounds selectionBounds = selectionRectangle.getBoundsInParent();
        Bounds imageViewBoundsInParent = imageView.getBoundsInParent();

        double x = selectionBounds.getMinX() - imageViewBoundsInParent.getMinX();
        double y = selectionBounds.getMinY() - imageViewBoundsInParent.getMinY();
        double width = selectionBounds.getWidth();
        double height = selectionBounds.getHeight();

        // คำนวณอัตราส่วนการสเกลระหว่าง ImageView และ Image จริง
        double scaleX = image.getWidth() / imageView.getBoundsInLocal().getWidth();
        double scaleY = image.getHeight() / imageView.getBoundsInLocal().getHeight();

        // แปลงพิกัดของพื้นที่ที่เลือกให้เป็นพิกัดของภาพจริง
        double imageX = x * scaleX;
        double imageY = y * scaleY;
        double imageWidth = width * scaleX;
        double imageHeight = height * scaleY;

        // ตรวจสอบว่าพิกัดอยู่ในขอบเขตของภาพหรือไม่
        if (imageX < 0) imageX = 0;
        if (imageY < 0) imageY = 0;
        if (imageX + imageWidth > image.getWidth()) imageWidth = image.getWidth() - imageX;
        if (imageY + imageHeight > image.getHeight()) imageHeight = image.getHeight() - imageY;

        PixelReader reader = image.getPixelReader();
        WritableImage croppedImageWritable = new WritableImage(reader, (int) imageX, (int) imageY, (int) imageWidth, (int) imageHeight);

        imageView.setImage(croppedImageWritable);
        croppedImage = croppedImageWritable;
    }

    private WritableImage cropAllImage(Bounds bounds) {
        Image image = imageView.getImage();
        if (image == null) {
            System.out.println("Image is null.");
            return null;
        }

        Bounds selectionBounds = selectionRectangle.getBoundsInParent();
        Bounds imageViewBoundsInParent = imageView.getBoundsInParent();

        double x = selectionBounds.getMinX() - imageViewBoundsInParent.getMinX();
        double y = selectionBounds.getMinY() - imageViewBoundsInParent.getMinY();
        double width = selectionBounds.getWidth();
        double height = selectionBounds.getHeight();

        double scaleX = image.getWidth() / imageView.getBoundsInLocal().getWidth();
        double scaleY = image.getHeight() / imageView.getBoundsInLocal().getHeight();

        double imageX = x * scaleX;
        double imageY = y * scaleY;
        double imageWidth = width * scaleX;
        double imageHeight = height * scaleY;

        if (imageX < 0) imageX = 0;
        if (imageY < 0) imageY = 0;
        if (imageX + imageWidth > image.getWidth()) imageWidth = image.getWidth() - imageX;
        if (imageY + imageHeight > image.getHeight()) imageHeight = image.getHeight() - imageY;

        PixelReader reader = image.getPixelReader();
        WritableImage croppedImageWritable = new WritableImage(reader, (int) imageX, (int) imageY, (int) imageWidth, (int) imageHeight);

        croppedImage = croppedImageWritable;
        return croppedImageWritable;
    }

    private void updateDarkArea() {
        if (selectionRectangle != null) {

            double imageWidth =  imagePane.getWidth();
            double imageHeight = imagePane.getHeight();
            double rectX = selectionRectangle.getX();
            double rectY = selectionRectangle.getY();
            double rectWidth = selectionRectangle.getWidth();
            double rectHeight = selectionRectangle.getHeight();

            darkArea.setWidth(imageWidth);
            darkArea.setHeight(imageHeight);
            darkArea.setLayoutX(0);
            darkArea.setLayoutY(0);

            Rectangle outerRect = new Rectangle(0, 0, imageWidth, imageHeight);
            Rectangle innerRect = new Rectangle(rectX, rectY, rectWidth, rectHeight);
            Shape clippedArea = Shape.subtract(outerRect, innerRect);

            darkArea.setClip(clippedArea);
            darkArea.setVisible(true);
        }
    }

    private void removeExistingSelection() {
        if (selectionRectangle != null) {
            // แทนที่การลบ selectionRectangle ออกไป ให้แค่ซ่อนหรือรีเซ็ตการตั้งค่าของมัน
            selectionRectangle.setVisible(false);  // ซ่อน rectangle แทนการลบออกจาก pane
            selectionRectangle.removeResizeHandles(imagePane);  // ลบ handles สำหรับการ resize ออกจาก UI

            // หากจำเป็นให้รีเซ็ตการตั้งค่าของ selectionRectangle ที่ใช้สำหรับการปรับขนาดใหม่
            // เช่น reset ขนาดหรือพิกัดของ selectionRectangle
        }
    }

    private void removeDarkArea(){
        darkArea.setVisible(false);
    }

    public Image getCroppedImage() {
        return croppedImage;
    }

    public void resetCroppedImage() {
        croppedImage = null ;
    }

}
