package uk.ac.ed.inf.aqmaps;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.*;

public class NoFlyZone {
	
	private List<Coords> pointList;
	private String name;

	public NoFlyZone(Polygon poly, String name) {
		this.name = name;
		List<List<Point>> coords = poly.coordinates();
		this.pointList = convertCoords(coords);
		
	}
	
	// Converts from the geojson polygon to a list of Coords
	// We feed in the List<List<Point>> given by the polygon's coordinates, and convert each to a Coords object
	private List<Coords> convertCoords(List<List<Point>> points) {
		
		var outerList = points.get(0);
		List<Coords> coordsList = new ArrayList<Coords>();
		
		for(int i = 0; i < outerList.size(); i++) {
			coordsList.add(new Coords(outerList.get(i).longitude(), outerList.get(i).latitude()));
		}
		
		return coordsList;
		
	}
	
	// Checks whether a given line segment intersects the noFlyZone's boundaries
	public boolean intersects(double x1, double y1, double x2, double y2) {
		boolean doesIntersect = false;
		// We go through each point in the polygon, taking the line defined by it and the point after it, then checking whether it intersects with our move
		// Line2D's intersect method is used for this calculation
		for(int i = 0; i < pointList.size()-1; i++) {
			if(!doesIntersect) {
				doesIntersect = Line2D.linesIntersect(x1, y1, x2, y2, pointList.get(i).getLng(), pointList.get(i).getLat(), pointList.get(i+1).getLng(), pointList.get(i+1).getLat());
			}		
		}
		return doesIntersect;
	}
	
	public String getName() {
		return name;
	}
	
}
