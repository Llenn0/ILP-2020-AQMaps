package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.List;
import org.javatuples.*;

public class FlightController {
	
	private final Coords START_POINT;
	private List<NoFlyZone> noFlyZones;
	private List<Coords> coordsList;
	private FlightPath[][] pathMatrix;
	private List<String> sensorNames;
	private int[][] connectivityMatrix;
	
	public FlightController(List<NoFlyZone> noFly, List<Coords> coords, Coords start, List<String> sensors) {
		this.noFlyZones = noFly;
		this.coordsList = coords;
		this.START_POINT = start;
		this.sensorNames = sensors;
		coordsList.add(0, START_POINT);
		this.pathMatrix = generatePathMatrix(coords);
		this.connectivityMatrix = new int[coordsList.size()][coordsList.size()];
	}

	// The primary algorithm for generating the order in which we visit the sensors
	// This uses the sorted edges approach to form a hamiltonian circuit - read documentation for more details
	public List<FlightPath> generateOrder() {
		List<Integer> usedNodes = new ArrayList<Integer>();
		List<Integer> fullNodes = new ArrayList<Integer>();
		
		for(int i = 0; i <= 33; i++) {
			usedNodes.add(i);
		}
		
		while(usedNodes.size() != 0) {
			Triplet<Integer, Integer, Integer> shortest = new Triplet<Integer, Integer, Integer>(-1, -1, 99999);
			for(int used : usedNodes) {
				Triplet<Integer, Integer, Integer> newShortest = getShortestValidDist(used, fullNodes);
				if(newShortest.getValue2() < shortest.getValue2()) {
					shortest = newShortest;
				}
			}
			
			System.out.println("Creating link between " + shortest.getValue0() + " and " + shortest.getValue1() + " of length " + shortest.getValue2());
			connectivityMatrix[shortest.getValue0()][shortest.getValue1()] = 1;
			connectivityMatrix[shortest.getValue1()][shortest.getValue0()] = 1;

			for(int node : usedNodes) {
				if(isDegree(node, 2)) {
					fullNodes.add(node);
				}
			}
			
			for(int node : fullNodes) {
				if(usedNodes.contains(node)) {
					usedNodes.remove(usedNodes.indexOf(node));
				}
			}
		}
		
		// Form the order using the connectivity matrix
		List<Integer> order = traverseConnections(0);
		
		return createPathList(order);
	}

	// Takes an ordered list of integers as input, and generates the final list of flightPath to be used as output.
	// The function creates new flightPaths rather than using the pathMatrix, allowing it to ensure one paths begins where the previous ended.
	private List<FlightPath> createPathList(List<Integer> order) {
		List<FlightPath> pathList = new ArrayList<FlightPath>();
		pathList.add(pathMatrix[order.get(0)][order.get(1)]); // We can rely on the path from 0 to the first node, as we start from the exact node position.
		System.out.println("Adding line path between " + order.get(0) + " and " + order.get(1));
		for(int i = 1; i < order.size()-1; i++) {
			
			List<Move> prevMoves = pathList.get(i-1).getMoveList();
			Coords prevEnd = prevMoves.get(prevMoves.size()-1).getEnd(); // Fetches the point at which our last flightpath ended
			System.out.println("Adding line path between " + order.get(i) + " and " + order.get(i+1));
			pathList.add(new FlightPath(prevEnd, coordsList.get(order.get(i+1)), noFlyZones, sensorNames.get(order.get(i+1)))); // Add the path between the last node's end and the new node to connect to

		}
		return pathList;
	}

	// Calculates the degree of a given node using the connectivityMatrix.
	private boolean isDegree(int node, int degree) {
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
	private boolean wouldCompleteLoopEarly(int from, int to) {
		var connectionList = traverseConnections(from);
		var wouldComplete = false;
		var readyToComplete = false;
		
		if(connectionList.get(connectionList.size()-1) == to) {
			wouldComplete = true;
		}
		
		if(connectionList.size() == coordsList.size()) {
			readyToComplete = true;
		}
		
		if(wouldComplete && !readyToComplete) {
			return true;
		} else {
			return false;
		}
	}
	
	// Uses the connectivity matrix to form an ordered list from the given point
	// This is used to check where a current line segment ends, and to form the final order.
	private List<Integer> traverseConnections(int from) {
		var prevNode = -1;
		var currNode = from;
		var nextNode = -1;
		
		List<Integer> order = new ArrayList<Integer>();
		order.add(from);
		
		// Find the first link, as we require a nextNode to loop
		for(int i = 0; i < connectivityMatrix[from].length; i++) {
			if(connectivityMatrix[from][i] == 1) {
				nextNode = i;
				order.add(nextNode);
				break;
			}
		}
		
		// If there is no connected node, we early exit
		if(nextNode == -1) {
			return order;
		}
		
		// Until the links stop, or we loop back to where we came from...
		while(nextNode != currNode && nextNode != from) {
			prevNode = currNode;
			currNode = nextNode;
			for(int i = 0; i < connectivityMatrix[currNode].length; i++) {
				if(connectivityMatrix[currNode][i] == 1 && i != prevNode) {
					nextNode = i;
					order.add(nextNode);
				}
			}
		}
		return order;
	}

	// Returns a Triplet containing the index we are interested in, the node closest to it, and the distance to that node (in moves)
	private Triplet<Integer, Integer, Integer> getShortestValidDist(int index, List<Integer> fullNodes) {
		
		int shortestNode = 0;
		int shortestDistance = 99999999;
		
		for(int i = 0; i < pathMatrix[index].length; i++) { // Iterate through all paths from the given index node
			if((i != index) && (pathMatrix[index][i].getMoveCount() < shortestDistance)) { // If we have a new shortest node...
				if(!wouldCompleteLoopEarly(index, i) && !fullNodes.contains(i)) { // Check that none of our 'bad' conditions are true...
					shortestNode = i; // If it reaches here, the node is a valid new shortest connection.
					shortestDistance = pathMatrix[index][i].getMoveCount();
				}
			}
		}
		
		return new Triplet<Integer, Integer, Integer>(index, shortestNode, shortestDistance);
	}

	// Generates the pathMatrix, giving us estimates of the number of moves to go between any two points on the map.
	// The reason these are only estimates is that in reality we will almost never begin paths at the exact coordinates of a node.
	private FlightPath[][] generatePathMatrix(List<Coords> coords) {
		FlightPath[][] matrix = new FlightPath[coords.size()][coords.size()];
		
		for(int i = 0; i < coords.size(); i++) {
			for(int j = 0; j < coords.size(); j++) {
				FlightPath currPath = null;
				
				if(i != j) { // A flightPath is only valid if it connects two different points
					currPath = new FlightPath(coords.get(i), coords.get(j), this.noFlyZones, sensorNames.get(j));
				}
				
				matrix[i][j] = currPath;
			}
		}
		
		return matrix;
	}

}
