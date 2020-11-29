package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.*;

public class App 
{
    public static void main( String[] args )
    {
        // Split args into its corresponding variables
        final String day = args[0];
        final String month = args[1];
        final String year = args[2];
        final double startLat = Double.parseDouble(args[3]);
        final double startLng = Double.parseDouble(args[4]);
        final int seed = Integer.parseInt(args[5]); // Unused, but still worth having as a variable
        final int port = Integer.parseInt(args[6]);
        
        // Initialise variables
        ServerHandler server = new ServerHandler(port);
        Coords startPoint = new Coords(startLng, startLat);
        Sensor[] locations = new Sensor[] {};
        GeoJSONHandler geo = new GeoJSONHandler(startPoint);
		List<Feature> featureList = new ArrayList<Feature>();
		List<Coords> coordsList = new ArrayList<Coords>();
		List<String> sensorNames = new ArrayList<String>();
		sensorNames.add("null");
		String noFlyZoneData = "";
		
		// Fetch data from the server
        try {
			locations = server.getSensorData(year, month, day);
			var index = 1;
			for(var loc : locations) {
				Coords coords = server.convertWordToCoords(loc.getLocation());
				Feature marker = geo.generateMarker(loc, coords);
				marker.addStringProperty("text", Integer.toString(index));
				featureList.add(marker);
				coordsList.add(coords);
				sensorNames.add(loc.getLocation());
				index++;
			}
			
			noFlyZoneData = server.getNoFlyZoneData();
			
		} catch (IOException | InterruptedException e) {
			System.out.println("Error: Server connection exception");
			e.printStackTrace();
			System.exit(0);
		}

        // Perform pathfinding algorithm
		List<NoFlyZone> noFlyZones = geo.parseNoFlyZones(noFlyZoneData);
		FlightController controller = new FlightController(noFlyZones, coordsList, startPoint, sensorNames);
		ArrayList<FlightPath> totalPath = (ArrayList<FlightPath>) controller.generateOrder();
		
		// Display some logging output
		var sum = 0;
		for(FlightPath path : totalPath) {
			sum = sum + path.getMoveCount();
		}
		System.out.println("Total path length: " + sum);
		
		if(sum > 150) {
			System.out.println("A suitable path was unable to be found.");
			System.exit(1);
		}
		
		// Generate the text and geojson output
		Feature path = geo.generatePath(totalPath);
		featureList.add(path);
		outputFlightPath(totalPath, day, month, year);
		geo.outputJSON(featureList, day, month, year);
    }

    // Generates the flightpath text file for output, using the list of flightpaths
	private static void outputFlightPath(ArrayList<FlightPath> totalPath, String day, String month, String year) {
		
	    try {
	        FileWriter writer = new FileWriter("flightpath-"+day+"-"+month+"-"+year+".txt");
			var moveNo = 1;
			
			// For each move, we read it's properties and write to the file in the order specified
			for(FlightPath currPath:totalPath) {
				List<Move> moves = currPath.getMoveList();
				for(int i = 0; i < moves.size(); i++) {
					Coords start = moves.get(i).getStart();
					Coords end = moves.get(i).getEnd();
					String currLine = String.format("%d,%f,%f,%d,%f,%f,%s\n", moveNo, start.getLng(), start.getLat(), moves.get(i).getBearing(), end.getLng(), end.getLat(), moves.get(i).getSensor());
					writer.write(currLine);
					moveNo++;
				}
			}
			System.out.println("Successfully wrote flightpath.");
	        writer.close();
	      } catch (IOException e) {
	        System.out.println("An error occurred in writing flightpath.");
	        e.printStackTrace();
	        System.exit(0);
	      }
		
	}
}
