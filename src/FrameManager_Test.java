import static org.junit.Assert.*;
import org.junit.*;

import nz.sodium.Cell;
import nz.sodium.Stream;
import nz.sodium.StreamSink;

import java.util.*;

public class FrameManager_Test {
    FrameManger fm;

    @Before
    public void setup() {
        fm = new FrameManger();
    }

    @Test
    public void testIsInRange() {
        GpsEvent e = new GpsEvent("A", 5, 10, 0);
        assertTrue(fm.isInRange(e, 0, 10, 0, 20));
        assertFalse(fm.isInRange(e, 6, 10, 0, 20));
    }

    @Test
    public void testFilterEvents() {
        List<GpsEvent> list = List.of(
            new GpsEvent("T1", 1, 1, 0),
            new GpsEvent("T2", 50, 50, 0)
        );
        List<GpsEvent> filtered = fm.filterEvents(list, 0, 10, 0, 10);
        assertEquals(1, filtered.size());
        assertEquals("T1", filtered.get(0).name);
    }

    @Test
    public void testCalculateDistance() {
        GpsEvent a = new GpsEvent("A", 0, 0, 0);
        GpsEvent b = new GpsEvent("B", 0.001, 0.001, 0);
        double d = fm.calculateDistance(a, b);
        assertTrue(d > 150 && d < 160);
    }

    @Test
    public void testCalculateTotalDistance() {
        List<GpsEvent> events = new ArrayList<>();
        events.add(new GpsEvent("A", 0, 0, 0));
        events.add(new GpsEvent("B", 0.001, 0.001, 0));
        events.add(new GpsEvent("C", 0.002, 0.002, 0));
        double total = fm.calculateTotalDistance(events);
        assertTrue(total > 300 && total < 320);
    }
    
    @Test
    public void testMergeStreams() {
        FrameManger fm = new FrameManger();
        StreamSink<GpsEvent> s1 = new StreamSink<>();
        StreamSink<GpsEvent> s2 = new StreamSink<>();

        Stream<GpsEvent> merged = fm.mergeStreams(new Stream[]{s1, s2});
        Cell<String> latest = merged.map(ev -> ev.name).hold("None");

        s1.send(new GpsEvent("Tracker1", 0, 0, 0));
        assertEquals("Tracker1", latest.sample());

        s2.send(new GpsEvent("Tracker2", 0, 0, 0));
        assertEquals("Tracker2", latest.sample());
    }
}
