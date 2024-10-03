package model.crop;

import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

public class ResizableRectangle extends Rectangle {

    private static final double RESIZER_SQUARE_SIDE = 8;
    private Paint resizerSquareColor = Color.WHITE;
    private Paint rectangleStrokeColor = Color.BLACK;

    private double mouseClickPozX;
    private double mouseClickPozY;

    // เก็บจุดควบคุมทั้งหมด
    private final List<Rectangle> resizeHandles = new ArrayList<>();
    private Pane parentPane; // ตัวแปรสำหรับเก็บ parentPane
    private Runnable updateDarkAreaCallback; // Callback function สำหรับอัพเดต Dark Area

    public ResizableRectangle(double x, double y, double width, double height, Pane pane, Runnable updateDarkAreaCallback) {
        super(x, y, width, height);
        this.parentPane = pane;
        this.updateDarkAreaCallback = updateDarkAreaCallback;
        pane.getChildren().add(this);
        super.setStroke(rectangleStrokeColor);
        super.setStrokeWidth(1);
        super.setFill(Color.color(1, 1, 1, 0));

        // สร้างจุดควบคุมการปรับขนาด
        makeResizerSquares(pane);

        // เคลื่อนย้ายสี่เหลี่ยมเมื่อคลิกตรงกลาง
        this.setOnMousePressed(event -> {
            mouseClickPozX = event.getX();
            mouseClickPozY = event.getY();
            getParent().setCursor(Cursor.MOVE);
        });

        this.setOnMouseDragged(event -> {
            double offsetX = event.getX() - mouseClickPozX;
            double offsetY = event.getY() - mouseClickPozY;

            double newX = getX() + offsetX;
            double newY = getY() + offsetY;

            // ตรวจสอบว่าไม่เกินขอบของ parent pane
            if (newX >= 0 && newX + getWidth() <= parentPane.getWidth()) {
                setX(newX);
            }
            if (newY >= 0 && newY + getHeight() <= parentPane.getHeight()) {
                setY(newY);
            }

            mouseClickPozX = event.getX();
            mouseClickPozY = event.getY();

            // อัพเดต Dark Area
            updateDarkAreaCallback.run();
        });

        this.setOnMouseReleased(event -> getParent().setCursor(Cursor.DEFAULT));
    }

    // ฟังก์ชันเพื่อลบจุดควบคุมทั้งหมด
    public void removeResizeHandles(Pane pane) {
        for (Rectangle handle : resizeHandles) {
            pane.getChildren().remove(handle);
        }
        resizeHandles.clear(); // ล้างรายการจุดควบคุม
    }

    private void makeResizerSquares(Pane pane) {
        makeNWResizerSquare(pane);
        makeCWResizerSquare(pane);
        makeSWResizerSquare(pane);
        makeSCResizerSquare(pane);
        makeSEResizerSquare(pane);
        makeCEResizerSquare(pane);
        makeNEResizerSquare(pane);
        makeNCResizerSquare(pane);
    }

    private void makeNWResizerSquare(Pane pane) {
        Rectangle squareNW = new Rectangle(RESIZER_SQUARE_SIDE, RESIZER_SQUARE_SIDE);
        squareNW.xProperty().bind(super.xProperty().subtract(squareNW.widthProperty().divide(2.0)));
        squareNW.yProperty().bind(super.yProperty().subtract(squareNW.heightProperty().divide(2.0)));
        pane.getChildren().add(squareNW);
        resizeHandles.add(squareNW);  // เพิ่มจุดควบคุมลงใน List

        squareNW.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> squareNW.getParent().setCursor(Cursor.NW_RESIZE));
        prepareResizerSquare(squareNW);

        squareNW.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            double offsetX = event.getX() - getX();
            double offsetY = event.getY() - getY();

            if (getWidth() - offsetX > 0) {
                setX(event.getX());
                setWidth(getWidth() - offsetX);
            }

            if (getHeight() - offsetY > 0) {
                setY(event.getY());
                setHeight(getHeight() - offsetY);
            }

            // อัพเดต Dark Area
            updateDarkAreaCallback.run();
        });
    }

    private void makeCWResizerSquare(Pane pane) {
        Rectangle squareCW = new Rectangle(RESIZER_SQUARE_SIDE, RESIZER_SQUARE_SIDE);
        squareCW.xProperty().bind(super.xProperty().subtract(squareCW.widthProperty().divide(2.0)));
        squareCW.yProperty().bind(super.yProperty().add(super.heightProperty().divide(2.0).subtract(
                squareCW.heightProperty().divide(2.0))));
        pane.getChildren().add(squareCW);
        resizeHandles.add(squareCW);  // เพิ่มจุดควบคุมลงใน List

        squareCW.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> squareCW.getParent().setCursor(Cursor.W_RESIZE));
        prepareResizerSquare(squareCW);

        squareCW.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            double offsetX = event.getX() - getX();
            double newX = getX() + offsetX;

            if (newX >= 0 && newX <= getX() + getWidth() - 5) {
                setX(newX);
                setWidth(getWidth() - offsetX);
            }

            // อัพเดต Dark Area
            updateDarkAreaCallback.run();
        });
    }

    private void makeSWResizerSquare(Pane pane) {
        Rectangle squareSW = new Rectangle(RESIZER_SQUARE_SIDE, RESIZER_SQUARE_SIDE);
        squareSW.xProperty().bind(super.xProperty().subtract(squareSW.widthProperty().divide(2.0)));
        squareSW.yProperty().bind(super.yProperty().add(super.heightProperty().subtract(
                squareSW.heightProperty().divide(2.0))));
        pane.getChildren().add(squareSW);
        resizeHandles.add(squareSW);  // เพิ่มจุดควบคุมลงใน List

        squareSW.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> squareSW.getParent().setCursor(Cursor.SW_RESIZE));
        prepareResizerSquare(squareSW);

        squareSW.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            double offsetX = event.getX() - getX();
            double offsetY = event.getY() - getY();
            double newX = getX() + offsetX;

            if (newX >= 0 && newX <= getX() + getWidth() - 5) {
                setX(newX);
                setWidth(getWidth() - offsetX);
            }

            if (offsetY >= 0 && offsetY <= getY() + getHeight() - 5) {
                setHeight(offsetY);
            }

            // อัพเดต Dark Area
            updateDarkAreaCallback.run();
        });
    }

    private void makeSCResizerSquare(Pane pane) {
        Rectangle squareSC = new Rectangle(RESIZER_SQUARE_SIDE, RESIZER_SQUARE_SIDE);

        squareSC.xProperty().bind(super.xProperty().add(super.widthProperty().divide(2.0).subtract(
                squareSC.widthProperty().divide(2.0))));
        squareSC.yProperty().bind(super.yProperty().add(super.heightProperty().subtract(
                squareSC.heightProperty().divide(2.0))));
        pane.getChildren().add(squareSC);
        resizeHandles.add(squareSC);  // เพิ่มจุดควบคุมลงใน List

        squareSC.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> squareSC.getParent().setCursor(Cursor.S_RESIZE));
        prepareResizerSquare(squareSC);

        squareSC.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            double offsetY = event.getY() - getY();

            if (offsetY >= 0 && offsetY <= getY() + getHeight() - 5) {
                setHeight(offsetY);
            }

            // อัพเดต Dark Area
            updateDarkAreaCallback.run();
        });
    }

    private void makeSEResizerSquare(Pane pane) {
        Rectangle squareSE = new Rectangle(RESIZER_SQUARE_SIDE, RESIZER_SQUARE_SIDE);
        squareSE.xProperty().bind(super.xProperty().add(super.widthProperty()).subtract(
                squareSE.widthProperty().divide(2.0)));
        squareSE.yProperty().bind(super.yProperty().add(super.heightProperty().subtract(
                squareSE.heightProperty().divide(2.0))));
        pane.getChildren().add(squareSE);
        resizeHandles.add(squareSE);  // เพิ่มจุดควบคุมลงใน List

        squareSE.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> squareSE.getParent().setCursor(Cursor.SE_RESIZE));
        prepareResizerSquare(squareSE);

        squareSE.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            double offsetX = event.getX() - getX();
            double offsetY = event.getY() - getY();

            if (offsetX >= 0 && offsetX <= getX() + getWidth() - 5) {
                setWidth(offsetX);
            }

            if (offsetY >= 0 && offsetY <= getY() + getHeight() - 5) {
                setHeight(offsetY);
            }

            // อัพเดต Dark Area
            updateDarkAreaCallback.run();
        });
    }

    private void makeCEResizerSquare(Pane pane) {
        Rectangle squareCE = new Rectangle(RESIZER_SQUARE_SIDE, RESIZER_SQUARE_SIDE);
        squareCE.xProperty().bind(super.xProperty().add(super.widthProperty()).subtract(
                squareCE.widthProperty().divide(2.0)));
        squareCE.yProperty().bind(super.yProperty().add(super.heightProperty().divide(2.0).subtract(
                squareCE.heightProperty().divide(2.0))));
        pane.getChildren().add(squareCE);
        resizeHandles.add(squareCE);  // เพิ่มจุดควบคุมลงใน List

        squareCE.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> squareCE.getParent().setCursor(Cursor.E_RESIZE));
        prepareResizerSquare(squareCE);

        squareCE.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            double offsetX = event.getX() - getX();
            if (offsetX >= 0 && offsetX <= getX() + getWidth() - 5) {
                setWidth(offsetX);
            }

            // อัพเดต Dark Area
            updateDarkAreaCallback.run();
        });
    }

    private void makeNEResizerSquare(Pane pane) {
        Rectangle squareNE = new Rectangle(RESIZER_SQUARE_SIDE, RESIZER_SQUARE_SIDE);

        squareNE.xProperty().bind(super.xProperty().add(super.widthProperty()).subtract(
                squareNE.widthProperty().divide(2.0)));
        squareNE.yProperty().bind(super.yProperty().subtract(squareNE.heightProperty().divide(2.0)));
        pane.getChildren().add(squareNE);
        resizeHandles.add(squareNE);  // เพิ่มจุดควบคุมลงใน List

        squareNE.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> squareNE.getParent().setCursor(Cursor.NE_RESIZE));
        prepareResizerSquare(squareNE);

        squareNE.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            double offsetX = event.getX() - getX();
            double offsetY = event.getY() - getY();
            double newY = getY() + offsetY;

            if (offsetX >= 0 && offsetX <= getX() + getWidth() - 5) {
                setWidth(offsetX);
            }

            if (newY >= 0 && newY <= getY() + getHeight() - 5) {
                setY(newY);
                setHeight(getHeight() - offsetY);
            }

            // อัพเดต Dark Area
            updateDarkAreaCallback.run();
        });
    }

    private void makeNCResizerSquare(Pane pane) {
        Rectangle squareNC = new Rectangle(RESIZER_SQUARE_SIDE, RESIZER_SQUARE_SIDE);

        squareNC.xProperty().bind(super.xProperty().add(super.widthProperty().divide(2.0).subtract(
                squareNC.widthProperty().divide(2.0))));
        squareNC.yProperty().bind(super.yProperty().subtract(
                squareNC.heightProperty().divide(2.0)));
        pane.getChildren().add(squareNC);
        resizeHandles.add(squareNC);  // เพิ่มจุดควบคุมลงใน List

        squareNC.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> squareNC.getParent().setCursor(Cursor.N_RESIZE));
        prepareResizerSquare(squareNC);

        squareNC.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            double offsetY = event.getY() - getY();
            double newY = getY() + offsetY;

            if (newY >= 0 && newY <= getY() + getHeight()) {
                setY(newY);
                setHeight(getHeight() - offsetY);
            }

            // อัพเดต Dark Area
            updateDarkAreaCallback.run();
        });
    }
    private void makeNorthResizerSquare(Pane pane) {
        Rectangle squareN = new Rectangle(RESIZER_SQUARE_SIDE, RESIZER_SQUARE_SIDE);
        squareN.xProperty().bind(super.xProperty().add(super.widthProperty().divide(2.0).subtract(squareN.widthProperty().divide(2.0))));
        squareN.yProperty().bind(super.yProperty().subtract(squareN.heightProperty().divide(2.0)));
        pane.getChildren().add(squareN);
        resizeHandles.add(squareN);  // เพิ่มจุดควบคุมลงใน List

        squareN.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> squareN.getParent().setCursor(Cursor.N_RESIZE));
        prepareResizerSquare(squareN);

        squareN.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            double offsetY = event.getY() - getY();
            if (getHeight() - offsetY > 0) {
                setY(event.getY());
                setHeight(getHeight() - offsetY);
            }

            // อัพเดต Dark Area
            updateDarkAreaCallback.run();
        });
    }

    private void makeSouthResizerSquare(Pane pane) {
        Rectangle squareS = new Rectangle(RESIZER_SQUARE_SIDE, RESIZER_SQUARE_SIDE);
        squareS.xProperty().bind(super.xProperty().add(super.widthProperty().divide(2.0).subtract(squareS.widthProperty().divide(2.0))));
        squareS.yProperty().bind(super.yProperty().add(super.heightProperty().subtract(squareS.heightProperty().divide(2.0))));
        pane.getChildren().add(squareS);
        resizeHandles.add(squareS);  // เพิ่มจุดควบคุมลงใน List

        squareS.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> squareS.getParent().setCursor(Cursor.S_RESIZE));
        prepareResizerSquare(squareS);

        squareS.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            double offsetY = event.getY() - getY();
            if (offsetY > 0) {
                setHeight(offsetY);
            }

            // อัพเดต Dark Area
            updateDarkAreaCallback.run();
        });
    }

    private void makeEastResizerSquare(Pane pane) {
        Rectangle squareE = new Rectangle(RESIZER_SQUARE_SIDE, RESIZER_SQUARE_SIDE);
        squareE.xProperty().bind(super.xProperty().add(super.widthProperty().subtract(squareE.widthProperty().divide(2.0))));
        squareE.yProperty().bind(super.yProperty().add(super.heightProperty().divide(2.0).subtract(squareE.heightProperty().divide(2.0))));
        pane.getChildren().add(squareE);
        resizeHandles.add(squareE);  // เพิ่มจุดควบคุมลงใน List

        squareE.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> squareE.getParent().setCursor(Cursor.E_RESIZE));
        prepareResizerSquare(squareE);

        squareE.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            double offsetX = event.getX() - getX();
            if (offsetX > 0) {
                setWidth(offsetX);
            }

            // อัพเดต Dark Area
            updateDarkAreaCallback.run();
        });
    }

    private void makeWestResizerSquare(Pane pane) {
        Rectangle squareW = new Rectangle(RESIZER_SQUARE_SIDE, RESIZER_SQUARE_SIDE);
        squareW.xProperty().bind(super.xProperty().subtract(squareW.widthProperty().divide(2.0)));
        squareW.yProperty().bind(super.yProperty().add(super.heightProperty().divide(2.0).subtract(squareW.heightProperty().divide(2.0))));
        pane.getChildren().add(squareW);
        resizeHandles.add(squareW);  // เพิ่มจุดควบคุมลงใน List

        squareW.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> squareW.getParent().setCursor(Cursor.W_RESIZE));
        prepareResizerSquare(squareW);

        squareW.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            double offsetX = event.getX() - getX();
            if (getWidth() - offsetX > 0) {
                setX(event.getX());
                setWidth(getWidth() - offsetX);
            }

            // อัพเดต Dark Area
            updateDarkAreaCallback.run();
        });
    }

    private void prepareResizerSquare(Rectangle rect) {
        rect.setFill(resizerSquareColor);

        rect.addEventHandler(MouseEvent.MOUSE_EXITED, event -> rect.getParent().setCursor(Cursor.DEFAULT));
    }
}