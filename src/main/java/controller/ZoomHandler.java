package controller;

import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;

public class ZoomHandler {

    private final ImageView imageView;
    private final ScrollPane imageScrollPane;

    public ZoomHandler(ImageView imageView, ScrollPane imageScrollPane) {
        this.imageView = imageView;
        this.imageScrollPane = imageScrollPane;
    }

    public void zoomIn() {
        imageView.setFitWidth(imageView.getFitWidth() * 1.1);
        imageView.setFitHeight(imageView.getFitHeight() * 1.1);
    }

    public void zoomOut() {
        imageView.setFitWidth(imageView.getFitWidth() * 0.9);
        imageView.setFitHeight(imageView.getFitHeight() * 0.9);
    }

    public void resetZoom() {
        imageView.setFitWidth(imageScrollPane.getWidth());
        imageView.setFitHeight(imageScrollPane.getHeight());
    }
}
