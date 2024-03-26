package org.emrick.project;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScrubBarGUI extends JComponent implements ActionListener {

    // String definitions
    private static final String PATH_SYNC_ICON = "./src/main/resources/images/scrub/time_sync_flaticon.png";
    private static final String PATH_PREV_SET_ICON = "./src/main/resources/images/scrub/prev_set_flaticon.png";
    private static final String PATH_NEXT_SET_ICON = "./src/main/resources/images/scrub/next_set_flaticon.png";
    private static final String PATH_PLAY_ICON = "./src/main/resources/images/scrub/play_flaticon.png";
    private static final String PATH_PAUSE_ICON = "./src/main/resources/images/scrub/pause_flaticon.png";
    private static final String PATH_PREV_COUNT_ICON = "./src/main/resources/images/scrub/prev_count_flaticon.png";
    private static final String PATH_NEXT_COUNT_ICON = "./src/main/resources/images/scrub/next_count_flaticon.png";
    private static final String PATH_AUDIO_ICON = "./src/main/resources/images/scrub/audio_flaticon.png";
    private static final String PATH_FAST_PLAY_ICON = "./src/main/resources/images/scrub/fast_play_flaticon.png";
    private static final String PATH_FULL_PLAY_ICON = "./src/main/resources/images/scrub/double_arrow_flaticon.png";

    // Swing
    private JPanel scrubBarPanel;
    private final JFrame parent;

    // Status
    private long currTimeMSec;

    // Scrub Bars / Sliders
    private JSlider topSlider;
    private JSlider botSlider;

    // Scrub-Bar Status
    private boolean isReady; // Whether the drill is ready to play
    private boolean isPlaying; // Whether the drill is being played

    // Scrub-Bar Toolbar Buttons
    private JButton syncButton;
    private JButton prevSetButton;
    private JButton nextSetButton;
    private JButton playPauseButton;
    private JButton prevCountButton;
    private JButton nextCountButton;

    // Additional settings
    JComboBox<String> playbackSpeedsBox;
    private JCheckBox audioCheckbox;

    // Page Tabs / Counts
    private Map<String, Integer> pageTab2Count; // [pageTab]:[count] e.g., k:"2A", v:30
    private int lastCount;
    private int currSetStartCount;
    private int currSetEndCount;
    private final FootballFieldPanel footballFieldPanel;

    // Listener
    private final ScrubBarListener scrubBarListener;
    private final SyncListener syncListener;
    private final ImageIcon PLAY_ICON;
    private final ImageIcon PAUSE_ICON;

    public ScrubBarGUI(JFrame parent, ScrubBarListener scrubBarListener, SyncListener syncListener, FootballFieldPanel footballFieldPanel) {
        this.parent = parent;

        // Placeholder. E.g., When Emrick Designer is first opened, no project is loaded.
        this.pageTab2Count = new HashMap<>();
        this.pageTab2Count.put("1", 0);

        updateLastCount();
        updateCurrSetCounts("1");

        this.scrubBarListener = scrubBarListener;
        this.syncListener = syncListener;

        this.isReady = false;
        this.isPlaying = false;
        this.footballFieldPanel = footballFieldPanel;

        PLAY_ICON = scaleImageIcon(new ImageIcon(PATH_PLAY_ICON));
        PAUSE_ICON = scaleImageIcon(new ImageIcon(PATH_PAUSE_ICON));

        initialize();
    }

    public void updatePageTabCounts(Map<String, Integer> pageTabCounts) {

        this.pageTab2Count = pageTabCounts;

        // Because SyncTimeGUI depends on pageTabCounts, update it as well
//        syncTimeGUI = new SyncTimeGUI(pageTabCounts);

        // There should exist at least a first Page Tab, for logic purposes
        if (pageTabCounts.isEmpty()) {
            System.out.println("Note: No page tabs found.");
            return;
        }

        // Can't find Page Tab 1
        else if (pageTabCounts.get("1") == null) {
            System.out.println("Note: Can't find page tab 1.");
            return;
        }

        reinitialize();
    }

    /**
     * Upon receiving new pageTab2Count data, call this method to update the Scrub Bar.
     */
    private void reinitialize() {
        updateLastCount();
        updateCurrSetCounts("1");

        // Debugging - Existing components cause UI bugging
        scrubBarPanel.removeAll();

        initialize();
    }

    /**
     * Update lastCount field. Important for managing display of the bottom slider.
     * Call this whenever the Scrub Bar receives a new set of Page Tab : Count data.
     * i.e., upon receiving new data for pageTabCount.
     */
    private void updateLastCount() {
        this.lastCount = Collections.max(
                pageTab2Count.entrySet(),
                Map.Entry.comparingByValue()
        ).getValue();
    }

    private void initialize() {

        scrubBarPanel = new JPanel(new BorderLayout(10, 10));

        // Toolbar Panel.
        JPanel toolBarPanel = getToolBarPanel();

        scrubBarPanel.add(toolBarPanel, BorderLayout.WEST);

        // Status Panel.
        // TODO: Add more info (like start/end times?)
        JPanel statusPanel = new JPanel(new GridLayout(3, 1));
        statusPanel.setPreferredSize(new Dimension(100, 1));
        JLabel statusLabel = new JLabel("Set: 0", JLabel.CENTER);
        JLabel timeLabel = new JLabel("0:00:000", JLabel.CENTER);
        statusPanel.add(statusLabel);
        statusPanel.add(timeLabel);

        scrubBarPanel.add(statusPanel, BorderLayout.EAST);

        JPanel sliderPanel = new JPanel(new GridLayout(2, 1));

        // Slider for navigating different sets
        topSlider = new JSlider(JSlider.HORIZONTAL,0, pageTab2Count.size() - 1, 0);
        Hashtable<Integer, JLabel> labelTable = buildLabelTable();
        topSlider.setLabelTable(labelTable);
        topSlider.setMinorTickSpacing(1);
        topSlider.setPaintTicks(true);
        topSlider.setPaintLabels(true);

        // Slider for navigating within a set
        botSlider = new JSlider(JSlider.HORIZONTAL, currSetStartCount, currSetEndCount, 0);
        botSlider.setMinorTickSpacing(1);
        botSlider.setMajorTickSpacing(2);
        botSlider.setPaintTicks(true);
        botSlider.setPaintLabels(true);

        // Change Listeners

        topSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {

                // Status label
                int val = ((JSlider)e.getSource()).getValue();
                String set = labelTable.get(val).getText();
                statusLabel.setText("Set: " + set);

                updateCurrSetCounts(set);
                for (Set s : footballFieldPanel.drill.sets) {
                    if (s.equalsString(set)) {
                        footballFieldPanel.addSetToField(s);
                    }
                }

                // Update bottom slider
                botSlider.setMinimum(currSetStartCount);
                footballFieldPanel.setCurrentSetStartCount(currSetStartCount);
                botSlider.setMaximum(currSetEndCount);
                botSlider.setValue(currSetStartCount);

                scrubBarListener.onScrub();
            }
        });

        botSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int val = ((JSlider)e.getSource()).getValue();
                footballFieldPanel.setCurrentCount(val);

                long currTimeMSec = scrubBarListener.onScrub();

                // Convert milliseconds to minutes and seconds
                long minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(currTimeMSec);
                long seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(currTimeMSec) % 60;
                long milliseconds = currTimeMSec % 1000;
                timeLabel.setText(String.format("%d:%02d:%03d", minutes, seconds, milliseconds));
            }
        });

        sliderPanel.add(topSlider);
        sliderPanel.add(botSlider);

        scrubBarPanel.add(sliderPanel, BorderLayout.CENTER);
    }

    private Hashtable<Integer, JLabel> buildLabelTable() {

        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();

        List<Map.Entry<String, Integer>> list = sortMap(pageTab2Count);

        int val = 0;
        for (Map.Entry<String, Integer> entry : list) {
            labelTable.put(val++, new JLabel(entry.getKey()));
        }

        return labelTable;
    }

    /**
     * Takes in a map of [String]:[Integer] entries and returns a list of those entries, sorted by the
     * value [Integer] in ascending order. Useful for a variety of situations, not only within this class.
     *
     * @param map - The map of [String]:[Integer] entries
     * @return a list of map entries, sorted by the value [Integer] in ascending order.
     */
    public static List<Map.Entry<String, Integer>> sortMap(Map<String, Integer> map) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(map.entrySet());
//        list.sort(Map.Entry.comparingByKey()); // Was there a reason that this was changed to comparingByKey(), that I am missing?
        list.sort(Map.Entry.comparingByValue());
        return list;
    }

    private JPanel getToolBarPanel() {
        JPanel toolBarPanel = new JPanel(new GridLayout(3, 1));

        JPanel topToolBarPanel = new JPanel(new GridLayout(1,3));

        // Scrub-bar Toolbar Buttons

        // Sync
        syncButton = new JButton();
        syncButton.setIcon(scaleImageIcon(new ImageIcon(PATH_SYNC_ICON)));
        syncButton.setToolTipText("Sync audio timing");
        syncButton.addActionListener(this);

        // Previous Set
        prevSetButton = new JButton();
        prevSetButton.setIcon(scaleImageIcon(new ImageIcon(PATH_PREV_SET_ICON)));
        prevSetButton.setToolTipText("Previous set");
        prevSetButton.addActionListener(this);

        // Next Set
        nextSetButton = new JButton();
        nextSetButton.setIcon(scaleImageIcon(new ImageIcon(PATH_NEXT_SET_ICON)));
        nextSetButton.setToolTipText("Next set");
        nextSetButton.addActionListener(this);

        topToolBarPanel.add(syncButton);
        topToolBarPanel.add(prevSetButton);
        topToolBarPanel.add(nextSetButton);

        JPanel midToolBarPanel = new JPanel(new GridLayout(1,3));

        // Play or Pause
        playPauseButton = new JButton();
        playPauseButton.setIcon(scaleImageIcon(new ImageIcon(PATH_PLAY_ICON)));
        playPauseButton.setToolTipText("Play/Pause playback");
        playPauseButton.addActionListener(this);

        // Previous Count
        prevCountButton = new JButton();
        prevCountButton.setIcon(scaleImageIcon(new ImageIcon(PATH_PREV_COUNT_ICON)));
        prevCountButton.setToolTipText("Previous count");
        prevCountButton.addActionListener(this);

        // Next Count
        nextCountButton = new JButton();
        nextCountButton.setIcon(scaleImageIcon(new ImageIcon(PATH_NEXT_COUNT_ICON)));
        nextCountButton.setToolTipText("Next count");
        nextCountButton.addActionListener(this);

        midToolBarPanel.add(playPauseButton);
        midToolBarPanel.add(prevCountButton);
        midToolBarPanel.add(nextCountButton);

        JPanel botToolBarPanel = new JPanel(new GridLayout(1,3));

        // Audio
        this.audioCheckbox = new JCheckBox();
        this.audioCheckbox.setToolTipText("Toggle audio on/off");
        JLabel audioLabel = new JLabel();
        audioLabel.setIcon(scaleImageIcon(new ImageIcon(PATH_AUDIO_ICON)));
        JPanel audioPanel = new JPanel(new FlowLayout());
        audioPanel.add(this.audioCheckbox);
        audioPanel.add(audioLabel);

        // Speed
        String[] playbackSpeeds = { "0.5", "Normal", "1.5", "2.0", "4.0" };
        playbackSpeedsBox = new JComboBox<>(playbackSpeeds);
        playbackSpeedsBox.setToolTipText("Select playback speed");
        playbackSpeedsBox.setSelectedIndex(1);
        playbackSpeedsBox.addActionListener(this);
        JPanel playbackSpeedsPanel = new JPanel();
        playbackSpeedsPanel.add(playbackSpeedsBox);

        botToolBarPanel.add(playbackSpeedsPanel);
        botToolBarPanel.add(audioPanel);

        toolBarPanel.add(topToolBarPanel);
        toolBarPanel.add(midToolBarPanel);
        toolBarPanel.add(botToolBarPanel);

        return toolBarPanel;
    }

    /**
     * Rescale ImageIcon to fit for toolbar icons, or for other purposes
     *
     * @param imageIcon - ImageIcon object to rescale.
     * @return Altered ImageIcon with rescaled icon.
     */
    public static ImageIcon scaleImageIcon(ImageIcon imageIcon, int width, int height) {
        Image image = imageIcon.getImage(); // transform it
        Image newImg = image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
        return new ImageIcon(newImg);  // transform it back
    }

    // Overloaded
    private ImageIcon scaleImageIcon(ImageIcon imageIcon) {
        return scaleImageIcon(imageIcon, 16, 16);
    }

    public void setReady(boolean ready) {
        this.isReady = ready;
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
    }

    public JPanel getScrubBarPanel() {
        return scrubBarPanel;
    }

    public JCheckBox getAudioCheckbox() {
        return audioCheckbox;
    }

    /**
     * Call this method to update currSetBeginCount and currSetEndCount, which are important
     * for displaying Count values on the bottom Scrub Bar.
     * Provide the Page Tab of the Set (a Page Tab denotes the start of a new Set).
     * Implementation Details:
     * If you provide:
     * "2", then consider "2A" or "3" as next Page Tab,
     * "2A", then consider "2B" or "3" as next Page Tab,
     * "2Z", then consider "3" as next Page Tab,
     * By knowing the current Page Tab and next Page Tab, we find the beginning and end Counts
     * for the given set. (Technically the parameter is the Page Tab String).
     *
     * @param set - For example, "1", "2A", "5B"
     */
    private void updateCurrSetCounts(String set) {

        currSetStartCount = pageTab2Count.get(set);

        // There is no further page tab
        if (currSetStartCount == lastCount) {
            currSetEndCount = currSetStartCount;
            printSetStartEndCounts(set);
            return;
        }

        // Find end count of set
        Pattern digitPattern = Pattern.compile("\\d+"); // Digit, one or more
        Pattern letterPattern = Pattern.compile("\\D+"); // Non digit, one or more

        Matcher digitMatcher = digitPattern.matcher(set);
        Matcher letterMatcher = letterPattern.matcher(set);

        int setNum = digitMatcher.find() ? Integer.parseInt(digitMatcher.group()) : -1;
        char subSetChar = letterMatcher.find() ? letterMatcher.group().charAt(0) : '!';

        // The character is not 'Z'
        if (subSetChar != 'Z') {

            // There is a character
            if (subSetChar != '!') {

                // Consider existence of next sub-set (e.g., if "2A", consider "2B")
                subSetChar += 1;

                if (pageTab2Count.get(Integer.toString(setNum) + subSetChar) != null) {
                    currSetEndCount = pageTab2Count.get(Integer.toString(setNum) + subSetChar);
                    printSetStartEndCounts(set);
                    return;
                }
            }

            // There is NO character
            else {
                if (pageTab2Count.get(setNum + "A") != null) {
                    currSetEndCount = pageTab2Count.get(setNum + "A");
                    printSetStartEndCounts(set);
                    return;
                }
            }
        }

        currSetEndCount = pageTab2Count.get(Integer.toString(setNum + 1));
        printSetStartEndCounts(set);
    }

    public void printSetStartEndCounts(String set) {
        // Debugging
//        System.out.println("set = " + set);
//        System.out.println("currSetStartCount = " + currSetStartCount);
//        System.out.println("currSetEndCount = " + currSetEndCount);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource().equals(playPauseButton)) {
            if (!isReady) {
                return;
            }
            if (isPlaying) {
                // Pause
                if (scrubBarListener.onPause()) {
                    setIsPlayingPlay();
                    isPlaying = false;
                }
            } else {
                // Play
                if (scrubBarListener.onPlay()) {
                    setIsPlayingPause();
                    isPlaying = true;
                }
            }
        }
        else if (e.getSource().equals(prevSetButton)) {
            prevSet();
        }
        else if (e.getSource().equals(nextSetButton)) {
            nextSet();
        }
        else if (e.getSource().equals(prevCountButton)) {
            prevCount();
        }
        else if (e.getSource().equals(nextCountButton)) {
            nextCount();
        }
        else if (e.getSource().equals(syncButton)) {
            if (isReady) {
                new SyncTimeGUI(parent, syncListener, pageTab2Count);
            }
        }
        else if (e.getSource().equals(playbackSpeedsBox)) {
            String selectedItem = (String) playbackSpeedsBox.getSelectedItem();
            assert selectedItem != null;
            if (selectedItem.equals("Normal")) {
                selectedItem = "1.0";
            }
            float playbackSpeed = Float.parseFloat(selectedItem);
            scrubBarListener.onSpeedChange(playbackSpeed);
        }

        int val = botSlider.getValue();
        footballFieldPanel.setCurrentCount(val);
    }

    // these might be misleading, fix?
    public void setIsPlayingPause() {
        playPauseButton.setIcon(PAUSE_ICON);
        isPlaying = true;
    }
    public void setIsPlayingPlay() {
        playPauseButton.setIcon(PLAY_ICON);
        isPlaying = false;
    }

    public void prevSet() {
        topSlider.setValue(topSlider.getValue() - 1);
    }

    public void nextSet() {
        topSlider.setValue(topSlider.getValue() + 1);
    }

    public void prevCount() {
        botSlider.setValue(botSlider.getValue() - 1);
    }

    public void nextCount() {
        botSlider.setValue(botSlider.getValue() + 1);
    }

    public int getCurrentSetIndex() {
        return topSlider.getValue();
    }

//    public int getCurrentCountInIndex() {
//        return botSlider.getValue() - botSlider.getMinimum();
//    }
//
//    public int getCurrentCount() {
//        return botSlider.getValue();
//    }

    public boolean isAtEndOfSet() {
        return botSlider.getValue() == botSlider.getMaximum();
    }

    public boolean isAtLastSet() {
        return topSlider.getValue() == topSlider.getMaximum();
    }

    public boolean isAtStartOfSet() {
        return botSlider.getValue() == botSlider.getMinimum();
    }

    public boolean isAtFirstSet() {
        return topSlider.getValue() == topSlider.getMinimum();
    }

    public int getCurrSetDuration() {
        return botSlider.getMaximum() - botSlider.getMinimum(); // gets duration in counts
    }

    // For testing
//    public static void main(String[] args) {
//
//        // Run Swing programs on the Event Dispatch Thread (EDT)
//        SwingUtilities.invokeLater(new Runnable() {
//            @Override
//            public void run() {
//
//                ScrubBarGUI scrubBarGUI = new ScrubBarGUI(new ScrubBarListener() {
//                    @Override
//                    public void onPlay() {
//                        System.out.println("onPlay called.");
//                    }
//
//                    @Override
//                    public void onPause() {
//                        System.out.println("onPause called.");
//                    }
//                }, new FootballFieldPanel());
//
//                // Dummy input
//                Map<String, Integer> dummyData1 = new HashMap<>();
//                dummyData1.put("1", 0); // Page tab 1 maps to count 0
//                dummyData1.put("1A", 16); // Page tab 1A maps to count 16
//                dummyData1.put("2", 32); // Page tab 2 maps to count 32
//                dummyData1.put("2A", 48); // etc.
//                dummyData1.put("3", 64);
//                dummyData1.put("3A", 88);
//                dummyData1.put("4", 96);
//                dummyData1.put("4A", 112);
//                dummyData1.put("4B", 128);
//
//                // When updating Scrub Bar GUI
//                scrubBarGUI.updatePageTabCounts(dummyData1);
//
//                JFrame testFrame = new JFrame();
//                testFrame.add(scrubBarGUI.getScrubBarPanel());
//                testFrame.setSize(new Dimension(800, 200));
//                testFrame.setLocationRelativeTo(null);
//                testFrame.setVisible(true);
//            }
//        });
//    }
}
