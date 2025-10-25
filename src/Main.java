import nz.sodium.*;
import swidgets.*;
import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame("FRP GPS Tracker Display");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(5, 2));

        GpsService service = new GpsService();
        Stream<GpsEvent>[] streams = service.getEventStreams();

        for (int i = 0; i < streams.length; i++) {
            int trackerId = i;

            Stream<String> displayStream = streams[i].map(ev ->
            	ev.toString()
            );

            Cell<String> latest = displayStream.hold("Tracker" + trackerId + " | waiting...");

            SLabel label = new SLabel(latest);

            JPanel panel = new JPanel();
            panel.setBorder(BorderFactory.createTitledBorder("Tracker " + trackerId));
            panel.add(label);

            frame.add(panel);
        }

        frame.pack();
        frame.setSize(800, 400);
        frame.setVisible(true);
    }
}
