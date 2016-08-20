package com.hoanguyenhs.invercargillbuses;

import android.content.res.AssetManager;
import android.graphics.Color;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by HoaNguyenHS on 8/4/2015.
 */
public class BusesDetail {

    private AssetManager assetManager;
    private Document doc;
    private BusStopsDetail busStopsDetail;

    public BusesDetail(AssetManager assetManager) {
        this.assetManager = assetManager;
        this.busStopsDetail = new BusStopsDetail(assetManager);
        try {
            InputStream is = assetManager.open("buses_detail.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Integer countBuses() {
        NodeList nodeList = doc.getElementsByTagName("BUS");
        return nodeList.getLength();
    }

    public PolylineOptions getBusRoute(String busID) {
        PolylineOptions busRoute = new PolylineOptions();
        String colour = "#";
        NodeList nodeList = doc.getElementsByTagName("BUS");
        for (Integer i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            Element element = (Element) node;
            if (element.getAttribute("id").equals(busID)) {
                colour = element.getElementsByTagName("COLOUR").item(0).getTextContent();
                String route = element.getElementsByTagName("ROUTE").item(0).getTextContent();
                String[] points = route.split(";");
                for (String temp : points) {
                    String[] xy = temp.split(",");
                    busRoute.add(new LatLng(Double.parseDouble(xy[0]), Double.parseDouble(xy[1])));
                }
            }
        }
        busRoute.width(5)
                .color(Color.parseColor(colour))
                .geodesic(true);
        return busRoute;
    }

    public String getBusRouteName(String busID) {
        String name = "";
        NodeList nodeList = doc.getElementsByTagName("BUS");
        for (Integer i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            Element element = (Element) node;
            if (element.getAttribute("id").equals(busID)) {
                name = element.getElementsByTagName("NAME").item(0).getTextContent();
            }

        }
        return name;
    }

    public List<LatLng> getBusRoutePoints(String busID) {
        List<LatLng> busRoutePoints = new ArrayList<LatLng>();
        NodeList nodeList = doc.getElementsByTagName("BUS");
        for (Integer i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            Element element = (Element) node;
            if (element.getAttribute("id").equals(busID)) {
                String route = element.getElementsByTagName("ROUTE").item(0).getTextContent();
                String[] point = route.split(";");
                for (String temp : point) {
                    String[] xy = temp.split(",");
                    busRoutePoints.add(new LatLng(Double.parseDouble(xy[0]), Double.parseDouble(xy[1])));
                }
            }
        }
        return busRoutePoints;
    }

    public LatLng simulateBusLocation(String busID) {
        LatLng latLng = new LatLng(0, 0);
        NodeList nodeList = doc.getElementsByTagName("BUS");
        for (Integer i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            Element element = (Element) node;
            if (element.getAttribute("id").equals(busID)) {
                String route = element.getElementsByTagName("ROUTE").item(0).getTextContent();
                String[] points = route.split(";");

                Element timeTableDetail = (Element) element.getElementsByTagName("TIMETABLEDETAIL").item(0);
                float duration = Float.parseFloat(timeTableDetail.getElementsByTagName("DURATION").item(0).getTextContent());
                float totalPoints = Float.parseFloat(timeTableDetail.getElementsByTagName("TOTALPOINTS").item(0).getTextContent());
                float ratio = duration / totalPoints;

                DateFormat dayOfTheWeekFormat = new SimpleDateFormat("EEEE");
                Date date = new Date();
                String dayOfTheWeek = dayOfTheWeekFormat.format(date);
                if (!dayOfTheWeek.equals("Sunday")) { // Set this equal to Sunday
                    String timetable = "";
                    if (dayOfTheWeek.equals("Saturday")) {
                        timetable = timeTableDetail.getElementsByTagName("SAT").item(0).getTextContent();
                    } else {
                        timetable = timeTableDetail.getElementsByTagName("MONFRI").item(0).getTextContent();
                    }
                    String[] times = timetable.split(";");
                    for (String time : times) {
                        try {
                            String[] startAndFinish = time.split(",");
                            DateFormat timeFormat1 = new SimpleDateFormat("HH:mm");
                            DateFormat timeFormat2 = new SimpleDateFormat("HH:mm:ss");
                            Date startTime = timeFormat1.parse(startAndFinish[0].trim());
                            Date finishTime = timeFormat1.parse(startAndFinish[1].trim());
                            Date currentTime = timeFormat2.parse(timeFormat2.format(date));
                            if (currentTime.after(startTime) && currentTime.before(finishTime)) {
                                long start = startTime.getTime();
                                long current = currentTime.getTime();
                                long offset = current - start;
                                int pos = (int) ((float) offset / ratio);
                                String[] xy = points[pos].split(",");
                                latLng = new LatLng(Double.parseDouble(xy[0]), Double.parseDouble(xy[1]));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
        return latLng;
    }

    public Object[] getDirection(Marker startingStop, Marker destinationStop) {
        Object[] objects = new Object[2];
        List<PolylineOptions> directions = new ArrayList<PolylineOptions>();
        List<MarkerOptions> transferStops = new ArrayList<MarkerOptions>();
        String[] startingIndex = getIndexOfPoint(startingStop);
        String[] destinationIndex = getIndexOfPoint(destinationStop);
        if (startingIndex[0].equals(destinationIndex[0])) {
            PolylineOptions direction = getBusRouteWithRange(startingIndex[0],
                    Integer.parseInt(startingIndex[1]), Integer.parseInt(destinationIndex[1]));
            directions.add(direction);
        } else {
            String sBusID = startingIndex[0];
            LatLng sPoint = startingStop.getPosition();
            List<String> excludeStops = new ArrayList<String>();
            String[] startIndex = null;
            String[] endIndex = null;
            while (true) {
                // Get extra bus stop
                MarkerOptions transferStop = busStopsDetail.transferStop(sBusID, destinationIndex[0], sPoint, excludeStops);
                String transfers = transferStop.getSnippet().split(";")[0];
                transferStops.add(transferStop);
                // Draw bus route
                startIndex = getIndexOfPointByBusID(sBusID, sPoint);
                endIndex = getIndexOfPointByBusID(sBusID, transferStop.getPosition());
                PolylineOptions direction = getBusRouteWithRange(sBusID,
                        Integer.parseInt(startIndex[1]), Integer.parseInt(endIndex[1]));
                directions.add(direction);
                // Change variable to call again
                for (String transfer : transfers.split(",")) {
                    if (!transfer.equals(sBusID)) {
                        sBusID = transfer;
                        break;
                    }
                }
                sPoint = transferStop.getPosition();
                excludeStops.add(transfers);
                // Check break loop conditions
                if (transfers.indexOf(destinationIndex[0]) != -1) {
                    break;
                }
            }
            PolylineOptions direction = getBusRouteWithRange(destinationIndex[0],
                    Integer.parseInt(getIndexOfPointByBusID(destinationIndex[0], sPoint)[1]),
                    Integer.parseInt(destinationIndex[1]));
            directions.add(direction);
        }
        objects[0] = directions;
        objects[1] = transferStops;
        return objects;
    }

    public String[] getIndexOfPointByBusID(String busID, LatLng point) {
        String[] result = new String[2];
        NodeList nodeList = doc.getElementsByTagName("BUS");
        finderLoop:
        for (int j = 0; ; j++) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                Element element = (Element) node;
                if (element.getAttribute("id").equals(busID)) {
                    String route = element.getElementsByTagName("ROUTE").item(0).getTextContent();
                    String[] points = route.split(";");
                    for (Integer k = 0; k < points.length; k++) {
                        String[] xy = points[k].split(",");
                        if (isInCircle(point, xy, j)) {
                            result[0] = element.getAttribute("id");
                            result[1] = k.toString();
                            break finderLoop;
                        }
                    }
                }
            }
        }
        return result;
    }

    public String[] getIndexOfPoint(Marker busStop) {
        String[] result = new String[2];
        NodeList nodeList = doc.getElementsByTagName("BUS");
        finderLoop:
        for (int j = 0; ; j++) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                Element element = (Element) node;
                String route = element.getElementsByTagName("ROUTE").item(0).getTextContent();
                String[] points = route.split(";");
                for (Integer k = 0; k < points.length; k++) {
                    String[] xy = points[k].split(",");
                    if (isInCircle(busStop.getPosition(), xy, j)) {
                        result[0] = element.getAttribute("id");
                        result[1] = k.toString();
                        break finderLoop;
                    }
                }
            }
        }
        return result;
    }

    public PolylineOptions getBusRouteWithRange(String busID, int start, int end) {
        PolylineOptions busRoute = new PolylineOptions();
        String colour = "#";
        NodeList nodeList = doc.getElementsByTagName("BUS");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            Element element = (Element) node;
            if (element.getAttribute("id").equals(busID)) {
                colour = element.getElementsByTagName("COLOUR").item(0).getTextContent();
                String route = element.getElementsByTagName("ROUTE").item(0).getTextContent();
                String[] points = route.split(";");
                if (start < end) {
                    for (int j = start; j <= end; j++) {
                        String[] xy = points[j].split(",");
                        busRoute.add(new LatLng(Double.parseDouble(xy[0]), Double.parseDouble(xy[1])));
                    }
                } else {
                    for (int j = start; j < points.length; j++) {
                        String[] xy = points[j].split(",");
                        busRoute.add(new LatLng(Double.parseDouble(xy[0]), Double.parseDouble(xy[1])));
                    }
                    for (int j = 0; j <= end; j++) {
                        String[] xy = points[j].split(",");
                        busRoute.add(new LatLng(Double.parseDouble(xy[0]), Double.parseDouble(xy[1])));
                    }
                }

            }
        }
        busRoute.width(5)
                .color(Color.parseColor(colour))
                .geodesic(true);
        return busRoute;
    }

    private boolean isInCircle(LatLng point, String[] busStop, int token) {
        boolean result = false;
        double xx1 = Math.abs(point.latitude - Double.parseDouble(busStop[0]));
        double yy1 = Math.abs(point.longitude - Double.parseDouble(busStop[1]));
        double radius = token * 0.0001;
        if ((xx1 * xx1) + (yy1 * yy1) <= (radius * radius)) {
            result = true;
        }
        return result;
    }
}
