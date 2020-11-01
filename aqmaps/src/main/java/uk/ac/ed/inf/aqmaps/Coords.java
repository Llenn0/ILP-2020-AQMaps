package uk.ac.ed.inf.aqmaps;

public class Coords {
	
	private final double lng;
	private final double lat;
	
	public Coords(double lng, double lat) {
		this.lng = lng;
		this.lat = lat;
	}

	public double getLng() {
		return lng;
	}

	public double getLat() {
		return lat;
	}
}
