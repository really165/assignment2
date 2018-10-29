# assignment2
# Due: November 7th, apparently?
* submission folder says 7th, paper says 12th

# REQUIREMENTS
* each non-source cell should have only two surrounding spaces filled
* need dumb implementation(no forward checking)
* need smart implementation(choices are not random and make sense)
* output needs attempted assignments and execution time
* dumb implementation can get cut off after a set time

# PROCESS
* puzzle file is read in just like before
* each time the current position is moved on a color,
* all available moves should be collected and then validated
* valid moves should be kept in consideration
* when no valid moves, get rid of that pipe puzzle and algorithm backtracks to the last known pipe maze

# COMPONENTS
* There needs to be pipe puzzle objects
* each puzzle object will have:
	* current position in puzzle(coordinates)
	* the current puzzle for that branch(an array most likely)
	* a list of possible puzzle that is created when the object is created
	* the last known puzzle object(for backtracking; NULL for initial maze)
	* color?(if there's an initial puzzle object for each color
* puzzle class may include methods that do the following:
	* constructor that takes parent puzzle, generated maze, and position as arguments
	* find the valid moves: checks all neighbors and constructs the children objects
	* backtrack: go back to the parent puzzle and get rid of this maze
