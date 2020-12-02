package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.*;

public class ServerHandler {
	private final String uri;
	
	public ServerHandler(int port) {
		this.uri = String.format("http://localhost:%d", port);
	}
	
	// Retrieves sensor data from the server for a given day
	public Sensor[] getSensorData(String year, String month, String day) throws IOException, InterruptedException {
		
		// Retrieve the server data using getServerData
		String data = getServerData(buildUri("maps", new String[] {year, month, day}, "air-quality-data.json"));
		
		Gson gson = new GsonBuilder().create();
		
		// Parse into MapLocation classes
		var locations = gson.fromJson(data, Sensor[].class);
		
		return locations;
	}
	
	// Converts a given w3w word into it's coordinates by retrieving the relevant server data
	public Coords convertWordToCoords(String location) throws IOException, InterruptedException {
		String[] words = location.split("\\.");
		
		// Retrieve the server data using getServerData
		String data = getServerData(buildUri("words", words, "details.json"));
		
		Gson gson = new GsonBuilder().create();
		
		// Parse into a JsonObject so that we can dig into it and retrieve the coords
		var jsonData = gson.fromJson(data, JsonObject.class);
		
		var coords = jsonData.getAsJsonObject("coordinates");
		
		// Return the coords as an instance of our Coords class
		return new Coords(coords.get("lng").getAsDouble(), coords.get("lat").getAsDouble());
	}
	
	// Simple method to retrieve the no-fly zones geojson data
	public String getNoFlyZoneData() throws IOException, InterruptedException {
		String data = getServerData(buildUri("buildings", new String[] {}, "no-fly-zones.geojson"));
		return data;
	}
	
	// Helper method used for all of the above methods - does the actual server interaction
	// We define a Java HttpClient and send a request using the given uri, returning whatever response we get
	private String getServerData(URI fullUri) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.GET()
				.uri(fullUri)
				.build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		return response.body();
	}

	// Helper method that builds a Uri to retrieve a certain resource in the server
	// Directory is the first folder, pathArgs allows us to traverse deeper into the folder in question, and file specifies the filename we want
	private URI buildUri(String directory, String[] pathArgs, String file) {
		
		var uriString = uri + "/" + directory;
		
		for(String arg : pathArgs) {
			uriString = (uriString + "/" + arg);
		}
		
		uriString += "/" + file;
		
		return URI.create(uriString);
	}

}
