# assignment2

REQUIREMENTS
each non-source cell should have only two surrounding spaces filled
need dumb implementation(no forward checking)
need smart implementation(choices are not random and make sense)
output needs attempted assignments and execution time
dumb implementation can get cut off after a set time
-----------------------------------------------------------
PROCESS
each time the current position is moved on a color,
all available moves should be collected and then validated
valid moves should be kept in consideration
when no valid moves, get rid of that pipe maze
	and algorithm backtracks to the last known pipe maze
-----------------------------------------------------------
COMPONENTS
There needs to be pipe maze objects
each maze object will have:
	current position in maze(coordinates)
	the current maze for that branch
	a list of possible mazes that is created when the object is created
	the last known maze object(for backtracking; NULL for initial maze)
	color?(if there's an initial maze object for each color
maze class may include methods that do the following:
	constructor that takes parent maze, generated maze, and position as arguments
	find the valid moves: checks all neighbors and constructs the children objects
	backtrack: go back to the parent maze and get rid of this maze
