package uk.ac.ed.inf.aqmaps;

public class Move {

	private Coords start;
	private Coords end;
	private int bearing;
	private String sensor;

	public Move(Coords currPos, Coords newPos, int roundedAngle, String sensor) {
		this.start = currPos;
		this.end = newPos;
		this.bearing = roundedAngle;
		this.sensor = sensor;
	}

	public Coords getStart() {
		return start;
	}

	public Coords getEnd() {
		return end;
	}

	public int getBearing() {
		return bearing;
	}

	public String getSensor() {
		return this.sensor;
	}

}
