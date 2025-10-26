import nz.sodium.*;

public class Main {
    public static void main(String[] args) {

        GpsService service = new GpsService();
        Stream<GpsEvent>[] streams = service.getEventStreams();
        FrameManger frameManger = new FrameManger();
        frameManger.mainFrame(streams);
    }
}
