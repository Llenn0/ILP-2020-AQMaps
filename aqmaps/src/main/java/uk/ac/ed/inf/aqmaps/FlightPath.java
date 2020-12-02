package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.List;

public class FlightPath {

	private static final double MOVE = 0.0003;
	private final int moveCount;
	private final List<Move> moveList;
	private final Coords startPos;
	private final Coords endPos;
	private final List<NoFlyZone> noFlyZones;
	private final String sensorName;
	
	public FlightPath(Coords start, Coords end, List<NoFlyZone> noFly, String sensorName) {
		this.startPos = start;
		this.endPos = end;
		this.noFlyZones = noFly;
		this.sensorName = sensorName;
		this.moveList = calculateFlightPath();
		this.moveCount = moveList.size();
	}

	// This is the main function of the class. Using our given start and end positions, we move one step at a time towards the goal.
	// This will loop until a path is found, and recalculate angles if it runs into a no-fly zone.
	private List<Move> calculateFlightPath() {
		Coords currPos = startPos;
		List<Move> moves = new ArrayList<Move>();
		
		while(!isClose(currPos, endPos)) {
			double optimalAngle = Math.toDegrees(getAngleBetween(currPos, endPos));
			if(optimalAngle < 0) optimalAngle = optimalAngle + 360; // Ensures angle is positive
			int roundedAngle = (int) (10 * (Math.round(optimalAngle/10))); // Rounds to the nearest multiple of 10
			Coords newPos = getNewLocation(currPos, roundedAngle);
			if(entersNoFlyZone(currPos, newPos) || leavesConfinementZone(newPos)) { // If we enter a no-fly zone or leave the drone confinement area...
				var previous = -1;
				// If a previous move exists, fetch it.
				if(moves.size() > 0) previous = moves.get(moves.size()-1).getBearing();
				// Calculate the optimal angle to avoid the obstacle
				int newAngle = calculateNewAngle(currPos, roundedAngle, previous);
				roundedAngle = newAngle;
				newPos = getNewLocation(currPos, roundedAngle);
			}
			if(roundedAngle == 360) roundedAngle = 0;
			var sensorClose = "null";
			if(isClose(newPos, endPos)) sensorClose = sensorName;
			moves.add(new Move(currPos, newPos, roundedAngle, sensorClose)); // Finally, add the move to our list
			currPos = newPos;
		}
		// Ensures we never return a flightpath of 0 length, we move in the optimal direction to ensure we remain as close as possible
		if(moves.size() == 0) {
			double optimalAngle = Math.toDegrees(getAngleBetween(currPos, endPos));
			if(optimalAngle < 0) optimalAngle = optimalAngle + 360;
			int roundedAngle = (int) (10 * (Math.round(optimalAngle/10)));
			Coords newPos = getNewLocation(currPos, roundedAngle);
			if(roundedAngle == 360) roundedAngle = 0;

			if(!isClose(newPos, endPos)) { // If we are unfortunate and move out of range, perform the opposite move to return back to where we came.
				moves.add(new Move(startPos, newPos, roundedAngle, "null"));
				currPos = newPos;
				int newAngle = roundedAngle + 180;
				if(newAngle > 350) newAngle -= 360;
				newPos = getNewLocation(currPos, newAngle);
				moves.add(new Move(currPos, newPos, newAngle, sensorName));
			} else {
				moves.add(new Move(startPos, newPos, roundedAngle, sensorName));
				currPos = newPos;
			}
		}
		
		return moves;
	}

	// Helper function that checks whether the current position of the drone is beyond any of it's limits
	private boolean leavesConfinementZone(Coords newPos) {
		if(newPos.getLng() > -3.184319 || newPos.getLng() < -3.192473 || newPos.getLat() > 55.946233 || newPos.getLat() < 55.942617) {
			return true;
		} else {
			return false;
		}
	}

	// Finds the best angle to travel at whilst avoiding no-fly zones and remaining in the confinement area
	// This function can be called with previous = some angle, or = -1. If it is -1, this is the first move on the path.
	private int calculateNewAngle(Coords currPos, int optimalAngle, int previous) {
		
		var turnInc = optimalAngle + 10;
		var turnDec = optimalAngle - 10;
		
		// Find the closest angle to the optimal one by turning in a positive direction, and a negative direction
		while(entersNoFlyZone(currPos, getNewLocation(currPos, turnInc)) || leavesConfinementZone(getNewLocation(currPos, turnInc))) {
			turnInc += 10;
		}
		
		while(entersNoFlyZone(currPos, getNewLocation(currPos, turnDec)) || leavesConfinementZone(getNewLocation(currPos, turnDec))) {
			turnDec -= 10;
		}
		
		// Clean the outputs to remain within 0 - 350.
		if(turnInc > 350) turnInc = turnInc - 360;
		if(turnDec < 0) turnDec = turnDec + 360;

		if(previous == -1) { // If we don't have a previous move, choose the angle closest to the optimal
			if(angleDistance(optimalAngle, turnInc) < angleDistance(optimalAngle, turnDec)) {
				return turnInc;
			} else {
				return turnDec;
			}
		} else { // Otherwise, choose the angle closest to the last angle we travelled at. This ensures we continue to smoothly move around a building.
			if(angleDistance(previous, turnInc) < angleDistance(previous, turnDec)) {
				return turnInc;
			} else {
				return turnDec;
			}
		}
	}

	// Helper function to find the distance between any two angles
	private int angleDistance(int angle1, int angle2) {
		var dist = (int) ((angle1 - angle2) % 360.0);
        if (dist < -180) dist += 360;
        if (dist >= 180) dist -= 360;
		return Math.abs(dist);
	}

	// Utilises NoFlyZone.intersects, and checks for every zone that our current line segment doesn't enter it.
	private boolean entersNoFlyZone(Coords currPos, Coords newPos) {
		for(NoFlyZone zone : noFlyZones) {
			if(zone.intersects(currPos.getLng(), currPos.getLat(), newPos.getLng(), newPos.getLat())) return true;
		}
		return false;
	}

	// Performs the actual movement, using trigonometry to find the position of the drone after it moves at a given angle.
	private Coords getNewLocation(Coords currPos, int roundedAngle) {
		var lngIncrement = MOVE * Math.cos(Math.toRadians(roundedAngle));
		var latIncrement = MOVE * Math.sin(Math.toRadians(roundedAngle));
		return new Coords(currPos.getLng() + lngIncrement, currPos.getLat() + latIncrement);
	}

	// Helper function which uses atan2 to calculate the exact angle between two points
	private double getAngleBetween(Coords c1, Coords c2) {
		return Math.atan2((c2.getLat() - c1.getLat()), (c2.getLng() - c1.getLng()));
	}

	// Helper function that checks whether we are within range of a sensor. Used above to break from the loop when we reach our target.
	private boolean isClose(Coords c1, Coords c2) {
		if(getDistBetween(c1, c2) <= 0.0002) {
			return true;
		} else {
			return false;
		}
	}
	
	// Simple Euclidean distance function
	private double getDistBetween(Coords c1, Coords c2) {
		var xdiff = Math.pow(c2.getLng() - c1.getLng(), 2);
		var ydiff = Math.pow(c2.getLat() - c1.getLat(), 2);
		double dist = Math.sqrt(xdiff + ydiff);
		return dist;
	}

	public int getMoveCount() {
		return this.moveCount;
	}

	public List<Move> getMoveList() {
		return this.moveList;
	}
	
}
