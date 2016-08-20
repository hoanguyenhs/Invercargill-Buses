package com.hoanguyenhs.invercargillbuses;

import android.content.res.AssetManager;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

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
 * Created by HoaNguyenHS on 8/9/2015.
 */
public class BusStopsDetail {

    private AssetManager assetManager;
    private Document doc;

    public BusStopsDetail(AssetManager assetManager) {
        this.assetManager = assetManager;
        try {
            InputStream is = assetManager.open("bus_stops_detail.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Integer countBuses() {
        NodeList nodeList = doc.getElementsByTagName("BUSSTOP");
        return nodeList.getLength();
    }

    public List<MarkerOptions> getBusStopsDetail(String busID) {
        List<MarkerOptions> busStopsDetail = new ArrayList<MarkerOptions>();
        NodeList nodeList = doc.getElementsByTagName("BUSSTOP");
        for (Integer i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            Element element = (Element) node;
            String transfer = element.getElementsByTagName("TRANSFER").item(0).getTextContent();
            if (transfer.indexOf(busID) != -1) {
                String code = element.getAttribute("id");
                String name = element.getElementsByTagName("NAME").item(0).getTextContent();
                String coordinate = element.getElementsByTagName("COORDINATE").item(0).getTextContent();
                String[] xy = coordinate.split(",");
                MarkerOptions busStop = new MarkerOptions();
                busStop.position(new LatLng(Double.parseDouble(xy[0]), Double.parseDouble(xy[1])));
                busStop.icon(BitmapDescriptorFactory.fromResource(R.drawable.busstop));
                busStop.title(name);
                /*
                Get timetable of bus stop
                 */
                String timeResultOut = "";
                String timeResultIn = "";
                NodeList subNodeList = element.getElementsByTagName("TIMETABLE");
                for (Integer j = 0; j < subNodeList.getLength(); j++) {
                    Node subNode = subNodeList.item(j);
                    Element subElement = (Element) subNode;
                    outerloop:
                    for (String bus : transfer.split(",")) {
                        if (subElement.getAttribute("id").equals(bus)) {
                            DateFormat dayOfTheWeekFormat = new SimpleDateFormat("EEEE");
                            Date date = new Date();
                            String dayOfTheWeek = dayOfTheWeekFormat.format(date);
                            String timetable = "";
                            if (dayOfTheWeek.equals("Saturday")) {
                                timetable = subElement.getElementsByTagName("SAT").item(0).getTextContent();
                            } else {
                                timetable = subElement.getElementsByTagName("MONFRI").item(0).getTextContent();
                            }
                            try {
                                timeResultIn = "";
                                String[] times = timetable.split(";");
                                DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
                                Date currentTime = timeFormat.parse(timeFormat.format(new Date()));
                                for (Integer k = 0; k < times.length - 1; k++) {
                                    Date startTime = timeFormat.parse(times[k].trim());
                                    Date next = timeFormat.parse(times[k + 1].trim());
                                    if (currentTime.after(startTime) && currentTime.before(next)) {
                                        timeResultIn = times[k + 1];
                                        break outerloop;
                                    }
                                }
                                if (timeResultIn.equals("")) {
                                    timeResultIn = times[0];
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    timeResultOut += timeResultIn.trim() + ",";
                }
                /*
                End
                 */
                String snippet = transfer + ";" + code + ";" + timeResultOut.substring(0, timeResultOut.lastIndexOf(","));
                busStop.snippet(snippet);
                // Add value to ArrayList
                busStopsDetail.add(busStop);
            }
        }
        return busStopsDetail;
    }

    public Integer countBusStopsPerBus(String busID) {
        Integer count = 0;
        NodeList nodeList = doc.getElementsByTagName("BUSSTOP");
        for (Integer i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            Element element = (Element) node;
            String transfer = element.getElementsByTagName("TRANSFER").item(0).getTextContent();
            if (transfer.indexOf(busID) != -1) {
                count++;
            }
        }
        return count;
    }

    public MarkerOptions nearestBusStop(LatLng point) {
        MarkerOptions nearestBusStop = null;
        NodeList nodeList = doc.getElementsByTagName("BUSSTOP");
        finderLoop:
        for (int token = 1; ; token++) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                Element element = (Element) node;
                String[] coordinate = element.getElementsByTagName("COORDINATE").item(0).getTextContent().split(",");
                if (isInCircle(point, coordinate, token)) {
                    nearestBusStop = new MarkerOptions();
                    nearestBusStop.position(new LatLng(Double.parseDouble(coordinate[0]), Double.parseDouble(coordinate[1])));
                    nearestBusStop.icon(BitmapDescriptorFactory.fromResource(R.drawable.busstop));
                    String name = element.getElementsByTagName("NAME").item(0).getTextContent();
                    nearestBusStop.title(name);
                    nearestBusStop.snippet(getTimeTable(element));
                    break finderLoop;
                }
            }
        }
        return nearestBusStop;
    }

    private String getTimeTable(Element element) {
        String code = element.getAttribute("id");
        String transfer = element.getElementsByTagName("TRANSFER").item(0).getTextContent();
        String timeResultOut = "";
        String timeResultIn = "";
        NodeList subNodeList = element.getElementsByTagName("TIMETABLE");
        for (int j = 0; j < subNodeList.getLength(); j++) {
            Node subNode = subNodeList.item(j);
            Element subElement = (Element) subNode;
            outerloop:
            for (String bus : transfer.split(",")) {
                if (subElement.getAttribute("id").equals(bus)) {
                    DateFormat dayOfTheWeekFormat = new SimpleDateFormat("EEEE");
                    Date date = new Date();
                    String dayOfTheWeek = dayOfTheWeekFormat.format(date);
                    String timetable = "";
                    if (dayOfTheWeek.equals("Saturday")) {
                        timetable = subElement.getElementsByTagName("SAT").item(0).getTextContent();
                    } else {
                        timetable = subElement.getElementsByTagName("MONFRI").item(0).getTextContent();
                    }
                    try {
                        timeResultIn = "";
                        String[] times = timetable.split(";");
                        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
                        Date currentTime = timeFormat.parse(timeFormat.format(new Date()));
                        for (Integer k = 0; k < times.length - 1; k++) {
                            Date startTime = timeFormat.parse(times[k].trim());
                            Date next = timeFormat.parse(times[k + 1].trim());
                            if (currentTime.after(startTime) && currentTime.before(next)) {
                                timeResultIn = times[k + 1];
                                break outerloop;
                            }
                        }
                        if (timeResultIn.equals("")) {
                            timeResultIn = times[0];
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            timeResultOut += timeResultIn.trim() + ",";
        }
        return transfer + ";" + code + ";" + timeResultOut.substring(0, timeResultOut.lastIndexOf(","));
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

    private double vectorLength(String[] coordinate, LatLng startingStop) {
        double x = Double.parseDouble(coordinate[0]);
        double y = Double.parseDouble(coordinate[1]);
        x = Math.abs(x - startingStop.latitude);
        y = Math.abs(y - startingStop.longitude);
        x = x * x;
        y = y * y;
        return Math.sqrt(x + y);
    }

    public MarkerOptions transferStop(String sBusID, String dBusID, LatLng startingStop, List<String> excludeStops) {
        MarkerOptions transferStop = null;
        List<Element> transferStops = new ArrayList<Element>();

        NodeList nodeList = doc.getElementsByTagName("BUSSTOP");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            Element element = (Element) node;
            String transfers = element.getElementsByTagName("TRANSFER").item(0).getTextContent();
            if (excludeStops.size() > 0) {
                for (String excludeStop : excludeStops) {
                    if (transfers.length() > 1 && transfers.indexOf(sBusID) != -1 && !transfers.equals(excludeStop)) {
                        transferStops.add(element);
                    }
                }
            } else {
                if (transfers.length() > 1 && transfers.indexOf(sBusID) != -1) {
                    transferStops.add(element);
                }
            }
        }
        Element min = transferStops.get(0);
        String[] minCoordinate = min.getElementsByTagName("COORDINATE").item(0).getTextContent().split(",");
        for (int i = 0; i < transferStops.size(); i++) {
            String[] coordinate = transferStops.get(i).getElementsByTagName("COORDINATE").item(0).getTextContent().split(",");
            String transfers = transferStops.get(i).getElementsByTagName("TRANSFER").item(0).getTextContent();
            if (transfers.indexOf(dBusID) != -1) {
                min = transferStops.get(i);
                break;
            }
            if (vectorLength(coordinate, startingStop) < vectorLength(minCoordinate, startingStop)) {
                min = transferStops.get(i);
            }
        }

        transferStop = new MarkerOptions();
        String[] coordinate = min.getElementsByTagName("COORDINATE").item(0).getTextContent().split(",");
        transferStop.position(new LatLng(Double.parseDouble(coordinate[0]), Double.parseDouble(coordinate[1])));
        transferStop.icon(BitmapDescriptorFactory.fromResource(R.drawable.busstop));
        String name = min.getElementsByTagName("NAME").item(0).getTextContent();
        transferStop.title(name);
        transferStop.snippet(getTimeTable(min));
        return transferStop;
    }
}
