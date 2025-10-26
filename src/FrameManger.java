import java.awt.*;
import javax.swing.*;
import nz.sodium.*;
import swidgets.*;

public class FrameManger {

    public void mainFrame(Stream<GpsEvent>[] streams) {
        JFrame frame = new JFrame("FRP GPS Tracker Display");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel trackersPanel = new JPanel(new GridLayout(2, 5, 5, 5));
        tenTrackers(streams, trackersPanel);
        frame.add(trackersPanel, BorderLayout.CENTER);

        
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Latest Event"));
        latestEventField(streams, bottomPanel);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.pack();
        frame.setSize(1200, 300);
        frame.setVisible(true);
    }

    private void tenTrackers(Stream<GpsEvent>[] streams, JPanel container) {
    	System.out.println("Building 10 trackers...");
        for (int i = 0; i < streams.length; i++) {
            int trackerId = i;
            Stream<String> displayStream = streams[i].map(ev ->
                ev.toStringRemoved()
            );
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
        Stream<String> merged = streams[0].map(GpsEvent::toString);
        for (int i = 1; i < streams.length; i++) {
            merged = merged.orElse(streams[i].map(GpsEvent::toString));
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

}
