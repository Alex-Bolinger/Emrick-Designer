package org.emrick.project;

public interface ScrubBarListener {
    boolean onPlay();
    boolean onPause();
    void onScrub();
    void onSpeedChange(float playbackSpeed);
}
