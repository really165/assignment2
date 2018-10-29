package assignment2;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args){
        if (args.length == 2) {
            try {
                InputStream input;

                if (args[1].equals("-")) {
                    input = System.in;
                }
                else {
                    input = new FileInputStream(args[1]);
                }

                char[][] pipeMaze = readFile(input);
                printMaze(pipeMaze);

                Searcher searcher;
                switch (args[0]) {
                    case "smart":
                        searcher = new Smart(pipeMaze);
                        break;

                    case "dumb":
                        searcher = new Dumb(pipeMaze);
                        break;

                    default:
                        throw new IllegalArgumentException("Must be smart/dumb");
                }

                searcher.findSolution();
            }
            catch (FileNotFoundException e) {
                System.err.println("File not found");
            }
        }
        else {
            System.err.println("Usage: java assignment2.Main METHOD MAZE_FILE");
        }
    }

    private static char[][] readFile(InputStream stream) throws FileNotFoundException {
        ArrayList<char[]> maze = new ArrayList<>();
        char[][] finalizedMaze = null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        try {
            while (reader.ready()) {
                String line = reader.readLine();

                if (line.length() > 0) {
                    maze.add(line.toCharArray());
                }
            }

            finalizedMaze = maze.toArray(new char[0][0]);
        }
        catch (IOException e) {
            System.err.println("Fuck.");
        }

        return finalizedMaze;
    }

    public static void printMaze(char[][] maze) {
        for(char[] row : maze) {
            assert row != null;

            System.out.println(row);
        }
    }
}
