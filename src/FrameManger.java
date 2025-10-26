import java.awt.*;
import javax.swing.*;
import nz.sodium.*;
import swidgets.*;

public class FrameManger {

    public void mainFrame(Stream<GpsEvent>[] streams) {
        JFrame frame = new JFrame("FRP GPS Tracker Display");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel trackersPanel = new JPanel(new GridLayout(2, 5));
        tenTrackers(streams, trackersPanel);
        mainPanel.add(trackersPanel);

        JPanel latestEventPanel = new JPanel();
        latestEventPanel.setBorder(BorderFactory.createTitledBorder("Latest Event"));
        latestEventField(streams, latestEventPanel);
        mainPanel.add(latestEventPanel);

        JPanel searchPanel = new JPanel(new GridLayout(2, 5));
        addSearchFields(searchPanel);
        mainPanel.add(searchPanel);

        frame.add(mainPanel);
        frame.pack();
        frame.setSize(1400, 300);
        frame.setVisible(true);
    }

    private void tenTrackers(Stream<GpsEvent>[] streams, JPanel container) {
    	System.out.println("Building 10 trackers...");
        for (int i = 0; i < streams.length; i++) {
            int trackerId = i;
            Stream<String> displayStream = streams[i].map(ev -> ev.toStringRemoved());
            Cell<String> latest = displayStream.hold("Tracker" + trackerId + " | waiting...");
            SLabel label = new SLabel(latest);
            JPanel panel = new JPanel();
            panel.setBorder(BorderFactory.createTitledBorder("Tracker " + trackerId));
            panel.add(label);
            container.add(panel);
        }
        System.out.println("Build 10 trackers completed");
    }

    private void latestEventField(Stream<GpsEvent>[] streams, JPanel container) {
    	System.out.println("Building Latest Even Field...");
        Stream<String> merged = streams[0].map(ev -> ev.toString());
        for (int i = 1; i < streams.length; i++) {
            merged = merged.orElse(streams[i].map(ev -> ev.toString()));
        }

        StreamSink<String> displaySink = new StreamSink<>();
        Stream<String> displayStream = merged.orElse(displaySink);

        Cell<String> latestDisplay = displayStream.hold("Waiting for tracker events...");
        SLabel label = new SLabel(latestDisplay);

        Timer[] timerRef = new Timer[1];
        timerRef[0] = new Timer(3000, e -> displaySink.send("Waiting for tracker events..."));
        timerRef[0].setRepeats(false);

        merged.listen(ev -> {
            timerRef[0].stop();
            timerRef[0].start();
        });
        
        container.add(label);
    	System.out.println("Build Latest Even Field completed");
    }

    private void addSearchFields(JPanel panel) {
        panel.add(new JLabel("Latitude Min"));
        panel.add(new JLabel("Latitude Max"));
        panel.add(new JLabel("Longitude Min"));
        panel.add(new JLabel("Longitude Max"));
        panel.add(new JLabel(""));

        JTextField latMinField = new JTextField();
        JTextField latMaxField = new JTextField();
        JTextField lonMinField = new JTextField();
        JTextField lonMaxField = new JTextField();
        JButton searchButton = new JButton("Search");

        panel.add(latMinField);
        panel.add(latMaxField);
        panel.add(lonMinField);
        panel.add(lonMaxField);
        panel.add(searchButton);

        searchButton.addActionListener(e -> {
            String latMin = latMinField.getText();
            String latMax = latMaxField.getText();
            String lonMin = lonMinField.getText();
            String lonMax = lonMaxField.getText();
            System.out.println("Search pressed:");
            System.out.println("LatMin=" + latMin + " LatMax=" + latMax +
                               " LonMin=" + lonMin + " LonMax=" + lonMax);
        });
    }
}
