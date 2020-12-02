package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.*;

public class GeoJSONHandler {

	private final Coords START_POINT;
	
	public GeoJSONHandler(Coords start) {
		this.START_POINT = start;
	}
	
	// Uses the webserver data we retrieve to add a marker for each sensor location
	// The markers' colours and icons differ based on reading and battery level, according to the spec
	public Feature generateMarker(Sensor loc, Coords coords) {
		
		Point point = Point.fromLngLat(coords.getLng(), coords.getLat());
		Feature pointFeature = Feature.fromGeometry(point);
		pointFeature.addStringProperty("location", loc.getLocation());
    	if(loc.getBattery() > 10) { // If battery is below 10%, the reading cannot be trusted
    		var rgb = getRGBFromReading(Float.parseFloat(loc.getReading()));
    		var symbol = getSymbolFromReading(Float.parseFloat(loc.getReading()));
        	pointFeature.addStringProperty("rgb-string", rgb);
        	pointFeature.addStringProperty("marker-color", rgb);
        	pointFeature.addStringProperty("marker-symbol", symbol);
    	} else {
        	pointFeature.addStringProperty("rgb-string", "#000000");
        	pointFeature.addStringProperty("marker-color", "#000000");
        	pointFeature.addStringProperty("marker-symbol", "cross");
    	}
    	return pointFeature;
	}

	// Generates the final geojson output of the program, writing to file
	public void outputJSON(List<Feature> featureList, String day, String month, String year) {
        FeatureCollection featureColl = FeatureCollection.fromFeatures(featureList);
        var featureJSON = featureColl.toJson();
        // We write the geoJSON to the file using FileWriter
        try {
            FileWriter writer = new FileWriter("readings-"+day+"-"+month+"-"+year+".geojson");
            writer.write(featureJSON);
            writer.close();
            System.out.println("Successfully wrote geoJSON.");
          } catch (IOException e) {
            System.out.println("An error occurred in geoJSON writing.");
            e.printStackTrace();
          }
	}
	
	// Given the geojson data from the webserver, parse into valid noFlyZone objects
	public List<NoFlyZone> parseNoFlyZones(String data) {
		List<NoFlyZone> noFlyZones = new ArrayList<NoFlyZone>();
		FeatureCollection coll = FeatureCollection.fromJson(data);
		List<Feature> features = coll.features();
		for(Feature f : features) { // For each feature, we find its polygon boundary and use that to generate a noFlyZone
			Polygon poly = (Polygon) f.geometry();
			var name = f.getStringProperty("name");
			NoFlyZone noFlyZone = new NoFlyZone(poly, name);
			noFlyZones.add(noFlyZone);
		}
		return noFlyZones;
	}
	
	// Creates a lineString path given the calculated list of flightpaths
	// Each move is considered to be one point on the lineString.
	public Feature generatePath(List<FlightPath> pathList) {
		List<Point> pointList = new ArrayList<Point>();
		pointList.add(Point.fromLngLat(START_POINT.getLng(), START_POINT.getLat())); // Add our start point so that the first move generates a line
		for(FlightPath path: pathList) {
			for(Move m : path.getMoveList()) { // Iterate through all moves and add the coords of the drone
				pointList.add(Point.fromLngLat(m.getEnd().getLng(), m.getEnd().getLat()));
			}
		}
		LineString path = LineString.fromLngLats(pointList);
		return Feature.fromGeometry(path); // Return the lineString as a feature
	}

	// Determines whether to attach a lighthouse or danger icon to a marker based on its reading
	private String getSymbolFromReading(float reading) {
		if(reading < 128) {
			return "lighthouse";
		} else {
			return "danger";
		}
	}

	// Assign the correct RGB colour using the reading
	private static String getRGBFromReading(float reading) {
		if(inRange(reading, 0, 32)) {
			return "#00ff00";
		} else if(inRange(reading, 32, 64)) {
			return "#40ff00";
		} else if(inRange(reading, 64, 96)) {
			return "#80ff00";
		} else if(inRange(reading, 96, 128)) {
			return "#c0ff00";
		} else if(inRange(reading, 128, 160)) {
			return "#ffc000";
		} else if(inRange(reading, 160, 192)) {
			return "#ff8000";
		} else if(inRange(reading, 192, 224)) {
			return "#ff4000";
		} else if(inRange(reading, 224, 256)) {
			return "#ff0000";
		} else { // Throw an error if the input file contained a value outside the 0 - 256 range
			System.out.println("Error: Prediction not in range.");
			System.exit(0);
		}
		return ""; // This should never be reached, but the method is required to have a return
	}
	
	// Helper function that stands in for min <= x < max
	private static boolean inRange(float val, int min, int max)
	{
	  return((val >= min) && (val < max));
	}
	
}
