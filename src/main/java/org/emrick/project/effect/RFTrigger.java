package org.emrick.project.effect;

import org.emrick.project.TimeManager;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class RFTrigger implements TimelineEvent {

    int count;
    long timestampMillis;

    public RFTrigger(int count, long timestampMillis) {
        this.count = count;
        this.timestampMillis = timestampMillis;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    @Override
    public JPanel getTimelineWidget() {
        Border outerBorder = BorderFactory.createLineBorder(Color.lightGray);
        Border innerBorder = BorderFactory.createEmptyBorder(2,2,2,2);

        JPanel widgetPanel = new JPanel(new GridLayout(5,1));
        widgetPanel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));

        JLabel titleLabel = new JLabel("<html><b>RF Trigger</b></html>");
        JLabel countLabel = new JLabel("Count: " + count);
        JLabel timeLabel = new JLabel("Time: " + TimeManager.getFormattedTime(timestampMillis));

        widgetPanel.add(titleLabel);
        widgetPanel.add(countLabel);
        widgetPanel.add(timeLabel);
        widgetPanel.setPreferredSize(new Dimension(100, 85));

        return widgetPanel;
    }
}
