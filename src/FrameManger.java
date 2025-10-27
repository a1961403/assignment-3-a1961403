import java.awt.*;
import javax.swing.*;
import nz.sodium.*;
import swidgets.*;
import java.util.List;

public class FrameManger {
	
	private StreamSink<Unit> rangeUpdateSink = new StreamSink<>();

    private CellSink<Double> latMinCell = new CellSink<>(-90.0);
    private CellSink<Double> latMaxCell = new CellSink<>(90.0);
    private CellSink<Double> lonMinCell = new CellSink<>(-180.0);
    private CellSink<Double> lonMaxCell = new CellSink<>(180.0);

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
        
        JPanel filteredEventPanel = new JPanel();
        filteredEventPanel.setBorder(BorderFactory.createTitledBorder("Filtered Events (within range)"));
        filteredEventField(streams, filteredEventPanel);
        mainPanel.add(filteredEventPanel);

        frame.add(mainPanel);
        frame.pack();
        frame.setSize(1400, 450);
        frame.setVisible(true);
    }

    private void tenTrackers(Stream<GpsEvent>[] streams, JPanel container) {
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
    }

    private void latestEventField(Stream<GpsEvent>[] streams, JPanel container) {
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
        merged.listen(ev -> { timerRef[0].stop(); timerRef[0].start(); });

        merged.listen(ev -> {
            timerRef[0].stop();
            timerRef[0].start();
        });
        
        container.add(label);
    }

    private void filteredEventField(Stream<GpsEvent>[] streams, JPanel container) {
        container.setLayout(new BorderLayout());
        
        JPanel rangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rangePanel.setBorder(BorderFactory.createTitledBorder("Current Range"));
        Cell<String> rangeText = latMinCell.lift(latMaxCell, lonMinCell, lonMaxCell,
            (latMin, latMax, lonMin, lonMax) ->
                String.format("Lat[%.2f~%.2f], Lon[%.2f~%.2f]", latMin, latMax, lonMin, lonMax)
        );
        rangePanel.add(new SLabel(rangeText));
        container.add(rangePanel, BorderLayout.NORTH);

        JPanel trackerPanel = new JPanel(new GridLayout(2, 5, 5, 5));

        for (int i = 0; i < streams.length; i++) {
            int trackerId = i;
            Stream<GpsEvent> gpsStream = streams[i];
            java.util.List<GpsEvent> eventList = new java.util.ArrayList<>();

            Stream<GpsEvent> filteredGpsStream = gpsStream.filter(ev -> {
                double latMin = latMinCell.sample();
                double latMax = latMaxCell.sample();
                double lonMin = lonMinCell.sample();
                double lonMax = lonMaxCell.sample();
                return ev.latitude >= latMin && ev.latitude <= latMax &&
                       ev.longitude >= lonMin && ev.longitude <= lonMax;
            });

            Stream<String> filteredDisplayStream = filteredGpsStream.map(GpsEvent::toStringRemoved);

            Stream<String> outOfRangeStream = rangeUpdateSink.snapshot(
                gpsStream.hold(null),
                (u, lastEv) -> {
                    if (lastEv == null) return "waiting...";
                    double latMin = latMinCell.sample();
                    double latMax = latMaxCell.sample();
                    double lonMin = lonMinCell.sample();
                    double lonMax = lonMaxCell.sample();
                    boolean inRange = lastEv.latitude >= latMin && lastEv.latitude <= latMax &&
                                      lastEv.longitude >= lonMin && lastEv.longitude <= lonMax;
                    return inRange
                        ? lastEv.toStringRemoved()
                        : "Out of Range";
                }
            );

            Stream<String> combinedStream = filteredDisplayStream.orElse(outOfRangeStream);

            Cell<String> latest = combinedStream.hold("waiting...");
            SLabel trackerLabel = new SLabel(latest);

            Stream<String> distanceStream = gpsStream.orElse(rangeUpdateSink.map(u -> null)).map(ev -> {
                long now = System.currentTimeMillis();
                if (ev != null) {
                    ev.timestamp = now;
                    eventList.removeIf(e -> now - e.timestamp > 5 * 60 * 1000);
                    eventList.add(ev);
                }

                double latMin = latMinCell.sample();
                double latMax = latMaxCell.sample();
                double lonMin = lonMinCell.sample();
                double lonMax = lonMaxCell.sample();
                List<GpsEvent> inRangeList = eventList.stream()
                        .filter(e -> e.latitude >= latMin && e.latitude <= latMax &&
                                     e.longitude >= lonMin && e.longitude <= lonMax)
                        .toList();

                if (inRangeList.size() < 2)
                    return "Distance (last 5 min): 0 m";

                double total = 0;
                for (int j = 1; j < inRangeList.size(); j++) {
                    total += calculateDistance(inRangeList.get(j-1), inRangeList.get(j));
                }

                return String.format("Distance (last 5 min): %.0f m", total);
            });

            Cell<String> latestDistance = distanceStream.hold("Distance (last 5 min): 0 m");
            SLabel distanceLabel = new SLabel(latestDistance);

            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(2, 1));
            panel.setBorder(BorderFactory.createTitledBorder("Tracker " + trackerId));
            panel.add(trackerLabel);
            panel.add(distanceLabel);

            trackerPanel.add(panel);
        }

        container.add(trackerPanel, BorderLayout.CENTER);
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
        JButton searchButton = new JButton("Set Range");

        panel.add(latMinField);
        panel.add(latMaxField);
        panel.add(lonMinField);
        panel.add(lonMaxField);
        panel.add(searchButton);

        searchButton.addActionListener(e -> {
            try {
                latMinCell.send(Double.parseDouble(latMinField.getText().isEmpty()?"-90":latMinField.getText()));
                latMaxCell.send(Double.parseDouble(latMaxField.getText().isEmpty()?"90":latMaxField.getText()));
                lonMinCell.send(Double.parseDouble(lonMinField.getText().isEmpty()?"-180":lonMinField.getText()));
                lonMaxCell.send(Double.parseDouble(lonMaxField.getText().isEmpty()?"180":lonMinField.getText()));

                rangeUpdateSink.send(Unit.UNIT);

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        });
    }
    
    private double calculateDistance(GpsEvent a, GpsEvent b) {
        double latDiff = (a.latitude - b.latitude) * 111_000;
        double lonDiff = (a.longitude - b.longitude) * 111_000;
        double altDiff = (a.altitude - b.altitude) * 0.3048;
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff + altDiff * altDiff);
    }
}
