package uk.ac.ed.inf.aqmaps;

public class Sensor {
	private String location;
	private double battery;
	private String reading;
	
	public Sensor(String location, double battery, String reading) {
		this.location = location;
		this.battery = battery;
		this.reading = reading;
	}

	public String getLocation() {
		return location;
	}

	public double getBattery() {
		return battery;
	}


	public String getReading() {
		return reading;
	}

}
