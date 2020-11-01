package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.List;
import org.javatuples.*;

public class FlightController {
	
	private final Coords START_POINT;
	private List<NoFlyZone> noFlyZones;
	private List<Coords> coordsList;
	private FlightPath[][] pathMatrix;
	private List<String> sensors;
	
	public FlightController(List<NoFlyZone> noFly, List<Coords> coords, Coords start, List<String> sensorNames) {
		this.noFlyZones = noFly;
		this.coordsList = coords;
		this.START_POINT = start;
		this.sensors = sensorNames;
		coordsList.add(0, START_POINT);
		this.pathMatrix = generatePathMatrix(coords);
	}

	// The primary algorithm for generating the order in which we visit the sensors
	// This uses the sorted edges approach to form a hamiltonian circuit - read documentation for more details
	public List<FlightPath> getOptimalOrder() {
		int[][] connectivityMatrix = new int[coordsList.size()][coordsList.size()];
		List<Integer> usedNodes = new ArrayList<Integer>();
		List<Integer> fullNodes = new ArrayList<Integer>();
		List<Integer> order = new ArrayList<Integer>();
		usedNodes.add(0);
		order.add(0);
		
		while(usedNodes.size() != 0) {
			Triplet<Integer, Integer, Integer> shortest = new Triplet<Integer, Integer, Integer>(-1, -1, 99999);
			for(int used : usedNodes) {
				Triplet<Integer, Integer, Integer> newShortest = getShortestValidDist(connectivityMatrix, used, fullNodes);
				if(newShortest.getValue2() < shortest.getValue2()) {
					shortest = newShortest;
				}
			}
			
			if(!isDegree(connectivityMatrix, shortest.getValue1(), 1)) usedNodes.add(shortest.getValue1());
			connectivityMatrix[shortest.getValue0()][shortest.getValue1()] = 1;
			connectivityMatrix[shortest.getValue1()][shortest.getValue0()] = 1;

			if(order.size() < coordsList.size()) {
				if(order.indexOf(shortest.getValue0()) == order.size()-1) {
					order.add(shortest.getValue1());
				} else {
					order.add(order.indexOf(shortest.getValue0()), shortest.getValue1());
				}
			}

			for(int node : usedNodes) {
				if(isDegree(connectivityMatrix, node, 2)) {
					fullNodes.add(node);
				}
			}
			
			for(int node : fullNodes) {
				if(usedNodes.contains(node)) {
					usedNodes.remove(usedNodes.indexOf(node));
				}
			}
		}
		
		order = beginAtStartPoint(order);
		
		return createPathList(order);
	}

	
	// Takes an existing order and reshuffles it so that the path begins at our start point (represented as 'sensor' 0).
	// This does not alter the path itself, just the point at which it begins.
	private List<Integer> beginAtStartPoint(List<Integer> order) {
		List<Integer> newOrder = order.subList(order.indexOf(0), order.size());
		newOrder.addAll(order.subList(0, order.indexOf(0)));
		return newOrder;
	}

	// Takes an ordered list of integers as input, and generates the final list of flightPath to be used as output.
	// The function creates new flightPaths rather than using the pathMatrix, allowing it to ensure one paths begins where the previous ended.
	private List<FlightPath> createPathList(List<Integer> order) {
		List<FlightPath> pathList = new ArrayList<FlightPath>();
		pathList.add(pathMatrix[order.get(0)][order.get(1)]); // We can rely on the path from 0 to the first node, as we start from the exact node position.
		System.out.println("Adding line path between " + order.get(0) + " and " + order.get(1));
		for(int i = 1; i < order.size(); i++) {
			
			List<Move> prevMoves = pathList.get(i-1).getMoveList();
			Coords prevEnd = prevMoves.get(prevMoves.size()-1).getEnd(); // Fetches the point at which our last flightpath ended
			
			if(i == order.size()-1) {
				pathList.add(new FlightPath(prevEnd, coordsList.get(order.get(0)), noFlyZones, sensors.get(order.get(0)))); // For the last connection, we need to return to the start
				System.out.println("Adding line path between " + order.get(i) + " and " + order.get(0));
			} else {
				pathList.add(new FlightPath(prevEnd, coordsList.get(order.get(i+1)), noFlyZones, sensors.get(order.get(i+1)))); // Add the path between the last node's end and the new node to connect to
				System.out.println("Adding line path between " + order.get(i) + " and " + order.get(i+1));
			}
		}
		return pathList;
	}

	// Uses the connectivityMatrix to check whether we have already made a certain connection between two nodes.
	private boolean alreadyConnected(int[][] connectivityMatrix, int from, int to) {
		if(connectivityMatrix[from][to] == 1) {
			return true;
		} else {
			return false;
		}
	}

	// Calculates the degree of a given node using the connectivityMatrix.
	private boolean isDegree(int[][] connectivityMatrix, int node, int degree) {
	    int sum = 0;
	    for (int value : connectivityMatrix[node]) { // Check how many nodes are connected to the node in question.
	        sum += value;
	    }
	    
	    if(sum == degree) {
	    	return true;
	    } else {
	    	return false;
	    }
	}

	// Checks whether making a given connection would complete a loop before all nodes were included in it
	private boolean wouldCompleteLoopEarly(int[][] connectivityMatrix, int from, int to) {
		boolean wouldComplete = true;
		boolean readyToComplete = true;
		
	    for(int i = 0; i < connectivityMatrix[0].length; i++) {
	    	int sum = 0;
		    for(int j = 0; j < connectivityMatrix[0].length; j++) {
		    	sum += connectivityMatrix[i][j];
		    }
		    if(i == from || i == to) {
		    	if(sum != 1) wouldComplete = false; // Unless the degree of both the nodes is 1, connecting them will not complete the loop
		    } else {
		    	if(sum != 2) readyToComplete = false; // If the degree of all nodes other than these two is 2, the loop is ready to be completed anyway
		    }
	    }
	    
	    if(wouldComplete) {
	    	if(readyToComplete) {
	    		return false;
	    	} else {
	    		return true;
	    	}
	    } else {
	    	return false;
	    }
	}

	// Returns a Triplet containing the index we are interested in, the node closest to it, and the distance to that node (in moves)
	private Triplet<Integer, Integer, Integer> getShortestValidDist(int[][] connectivityMatrix, int index, List<Integer> fullNodes) {
		
		int shortestNode = 0;
		int shortestDistance = 99999999;
		
		for(int i = 0; i < pathMatrix[index].length; i++) { // Iterate through all paths from the given index node
			if((i != index) && (pathMatrix[index][i].getMoveCount() < shortestDistance) && pathMatrix[index][i].getMoveCount() > 0) { // If we have a new shortest node with a movecount > 0...
				if(!wouldCompleteLoopEarly(connectivityMatrix, index, i) && !alreadyConnected(connectivityMatrix, index, i) && !fullNodes.contains(i)) { // Check that none of our three 'bad' conditions are true...
					shortestNode = i; // If it reaches here, the node is a valid new shortest connection.
					shortestDistance = pathMatrix[index][i].getMoveCount();
				}
			}
		}
		
		return new Triplet<Integer, Integer, Integer>(index, shortestNode, shortestDistance);
	}

	// Generates the pathMatrix, giving us estimates of the number of moves to go between any two points on the map.
	// The reason these are only estimates is that in reality we don't begin (most) paths at the exact coordinates of a node.
	private FlightPath[][] generatePathMatrix(List<Coords> coords) {
		FlightPath[][] matrix = new FlightPath[coords.size()][coords.size()];
		
		for(int i = 0; i < coords.size(); i++) {
			for(int j = 0; j < coords.size(); j++) {
				FlightPath currPath = null;
				
				if(i != j) { // A flightPath is only valid if it connects two different points
					currPath = new FlightPath(coords.get(i), coords.get(j), this.noFlyZones, sensors.get(j));
				}
				
				matrix[i][j] = currPath;
			}
		}
		
		return matrix;
	}

}
