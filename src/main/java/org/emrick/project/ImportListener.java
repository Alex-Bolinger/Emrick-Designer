package org.emrick.project;

import java.awt.*;
import java.io.File;
import java.net.URI;

public interface ImportListener {
    void onImport();
    void onFileSelect(URI archivePath, URI drillPath);
    void onFloorCoverImport(Image image);
    void onSurfaceImport(Image image);
    void onAudioImport(File audioFile);
    void onDrillImport(String drill);
}
