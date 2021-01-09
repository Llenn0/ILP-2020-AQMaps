# ILP-2020-AQMaps

This is my implementation of Coursework 2 of the Informatics Large Practical 2020. The AQmaps system retrieves data from a web server, and uses this data to compute an effective path for a drone to take so that it visits every sensor on a given day. The path is outputted as GeoJSON which can be rendered using a website such as geojson.io. The system was completed in December 2020.

# The Specification

The web server contains information for the sensors needing visited on every day of the year, 33 on each day. It also contains the no-fly zones that the drone must avoid at all costs, as well as information for converting from w3w (what3words) addresses to (lng,lat) pairs. The drone can move in multiples of 10 degrees, and always travels exactly 0.0003 degrees in one move. The drone can make a maximum of 150 moves, during which it must visit each sensor around the George Square area and make it back to its starting point. The program runs in the following steps:  

Take in the user's input. This comes in the form of a date, a starting point, a random seed (not used) and a web server port.  
Fetch the data from the server and use it to construct 33 GeoJSON markers representing the sensors needing visited, and the readings picked up from them represented by certain colours and icons. (Note - The web server was given as part of the task)  
Use the pathfinding algorithm to guide the drone between the 33 sensors and end up back at the start. My implementation of this uses the sorted edges algorithm, modified to use the number of drone moves between any two sensors as a distance measure. This takes avoiding no-fly zones into account when deciding what order to visit the sensors, and ensures a short and valid path.  
Render the path as a GeoJSON line string, and create two output files. One file contains the GeoJSON for the user to visualise, and the other contains the drone's flightpath and details which moves it took, what angles, what sensors it reached etc.  

# The Results

This project was an interesting experience that I enjoyed working on, with plenty of problems to solve and no set way in which to solve them. I'm very pleased with the results, as I found that most paths completed in roughly 100 moves, far off from the 150 limit. This project recieved a mark of [Not yet marked].
