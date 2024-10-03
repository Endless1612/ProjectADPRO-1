package model.edgedetector.detectors;



import java.io.File;

public interface EdgeDetector {
    File detectEdges(File inputFile) throws Exception;
}