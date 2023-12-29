package com.maltino.net.talkback;

import android.location.Location;
import android.widget.TextView;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class LocalAreaDescriptions implements Serializable {
    private List<PointOfInterest> Areas;
    private transient PointOfInterest currentSpot;
    public transient String currentContext = "";
    public String Description = "";

    public static String StorageFileName = "locationInfo.txt";

    public LocalAreaDescriptions(String dataSetDescription, List<PointOfInterest> data) {
        Description = dataSetDescription;
        Areas = data;
        Areas.sort(new SortByArea());
    }

    private static class SortByArea implements Comparator<PointOfInterest> {
        public int compare(PointOfInterest a, PointOfInterest b) {
            return (int)(a.Area - b.Area);
        }
    }

    public static LocalAreaDescriptions getTestData() {

        android.location.Location sw = new Location("");
        android.location.Location se = new Location("");
        android.location.Location ne = new Location("");
        android.location.Location nw = new Location("");

        //sw: 38.039519, -122.610994
        sw.setLatitude(38.039519);
        sw.setLongitude(-122.610994);

        //se: 38.066823, -122.489801
        se.setLatitude(38.066823);
        se.setLongitude(-122.489801);

        //nw: 38.153807, -122.609277
        nw.setLatitude(38.153807);
        nw.setLongitude(-122.609277);

        //ne: 38.152187, -122.501817
        ne.setLatitude(38.152187);
        ne.setLongitude(-122.501817);

        HashMap<String, String> localStuff = new HashMap<>();
        localStuff.putIfAbsent("Summary", "Novato (Spanish for \"Novatus\") is a city in Marin County, California, United States, situated in the North Bay region of the Bay Area. At the 2020 census, Novato had a population of 53,225.  What is now Novato was originally the site of several Coast Miwok villages: Chokecherry, near downtown Novato; Puyuku, near Ignacio; and Olómpali, at the present-day Olompali State Historic Park. In 1839, the Mexican government granted the 8,876-acre (35.92 km2) Rancho Novato to Fernando Feliz. The rancho was named after a local Miwok leader who had probably been given the name of Saint Novatus at his baptism.[10] Subsequently, four additional land grants were made in the area: Rancho Corte Madera de Novato, to John Martin in 1839; Rancho San Jose, to Ignacio Pacheco in 1840; Rancho Olómpali, awarded in 1843 to Camilo Ynitia, son of a Coast Miwok chief; and Rancho Nicasio, by far the largest at 56,621 acres (229.1 km2), awarded to Pablo de la Guerra and John B.R. Cooper in 1844. ");
        localStuff.putIfAbsent("geographical", "According to the United States Census Bureau, Novato has a total area of 28.0 square miles (73 km2) and is the largest city in area in Marin County. A total of 27.4 square miles (71 km2) is land and 0.5 square miles (1.3 km2) (1.85%) is water. Major geographical features nearby include Mount Burdell and Mount Burdell Open Space Preserve to the north and Big Rock Ridge to the southwest. Stafford Lake to the west is a secondary water supply for Novato, with the Russian River in Sonoma County to the north supplying most of the city's water.");
        localStuff.putIfAbsent("recreational", "Novato includes ten Marin County Open Space District preserves: Mount Burdell, Rush Creek, Little Mountain, Verissimo Hills, Indian Tree, Deer Island, Indian Valley, Ignacio Valley, Loma Verde, and Pacheco Valle. Although Novato is located on the water, access to the water is blocked by expansive farmland and wetlands");
        PointOfInterest myTown = new PointOfInterest("Novato", "The City of Novato", Arrays.asList(ne, nw, sw, se), localStuff);

        return new LocalAreaDescriptions("Hardcoded test data - Novato only", Arrays.asList(myTown));
    }

    public void updateLocation(Location latLng, TextView userInterface) {
        String locationDescription = "No Context found, current location Latitude: "+ latLng.getLatitude() + "\n" + "Longitude: "+ latLng.getLongitude();
        for(PointOfInterest c : Areas) {
            if (c.isPointInPolygon(latLng)) {
                if (currentSpot != c) {
                    currentContext = c.getFullContextForMachineLearning();
                    locationDescription = currentContext;
                    userInterface.setText(locationDescription);
                    currentSpot = c;
                }
            }
        }
    }

    public boolean IsEmpty() {
        return Areas.size() == 0;
    }

    private static class PointOfInterest {

        List<Location> Boundary;
        public HashMap<String, String> Entries = new HashMap<>();
        public String Name = "";
        public String Description = "";
        public double Area = 0;

        public PointOfInterest(String name, String description, List<Location> boundary, HashMap<String, String> location_information) {
            Name = name;
            Description = description;
            Entries = location_information;
            Boundary = boundary;
            Area = calculateArea();
        }

        public String getFullContextForMachineLearning() {
            String locationDescription = Entries.keySet().stream()
                    .map(key -> Entries.get(key))
                    .collect(Collectors.joining(".\n"));
            return String.format("We are in %s.\n%s", Name, locationDescription);
        }

        public boolean isPointInPolygon(Location testLocation) {
            int numVertices = Boundary.size();
            boolean inside = false;

            double testLat = testLocation.getLatitude();
            double testLon = testLocation.getLongitude();

            for (int i = 0, j = numVertices - 1; i < numVertices; j = i++) {
                Location point_i = Boundary.get(i);
                Location point_j = Boundary.get(j);

                double lat_i = point_i.getLatitude();
                double lat_j = point_j.getLatitude();
                double lon_i = point_i.getLongitude();
                double lon_j = point_j.getLongitude();

                if ((lat_i > testLat) != (lat_j > testLat) &&
                        (testLon < (lon_j - lon_i) * (testLat - lat_i) / (lat_j - lat_i) + lon_i)) {
                    inside = !inside;
                }
            }
            return inside;
        }

        double calculateArea() {
            int numVertices = Boundary.size();

            double area = 0.0;

            for (int i = 0, j = numVertices - 1; i < numVertices; j = i++) {
                Location point_i = Boundary.get(i);
                Location point_j = Boundary.get(j);

                double lat_i = point_i.getLatitude();
                double lat_j = point_j.getLatitude();
                double lon_i = point_i.getLongitude();
                double lon_j = point_j.getLongitude();

                area += (lon_j + lon_i) * (lat_j - lat_i);

            }

            // Take the absolute value of the result and divide by 2
            area = Math.abs(area) / 2.0;

            return area;
        }
    }
}
