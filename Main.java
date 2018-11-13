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

                char[][] rawPuzzle = readFile(input);
                Puzzle puzzle;
                switch (args[0]) {
                    case "smart":
                        puzzle = new SmartPuzzle(rawPuzzle);
                        break;

                    case "dumb":
                        puzzle = new DumbPuzzle(rawPuzzle);
                        break;

                    default:
                        throw new IllegalArgumentException("Must be smart/dumb");
                }

                puzzle.print();
                System.out.println();
                puzzle.solve();
                System.out.println();
                puzzle.print();
            }
            catch (FileNotFoundException e) {
                System.err.println("File not found");
            }
        }
        else {
            System.err.println("Usage: java assignment2.Main METHOD PUZZLE_FILE");
        }
    }

    private static char[][] readFile(InputStream stream) throws FileNotFoundException {
        ArrayList<char[]> puzzle = new ArrayList<>();
        char[][] finalizedPuzzle = null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        try {
            while (reader.ready()) {
                String line = reader.readLine();

                if (line.length() > 0) {
                    puzzle.add(line.toCharArray());
                }
            }

            finalizedPuzzle = puzzle.toArray(new char[0][0]);
        }
        catch (IOException e) {
            System.err.println("Fuck.");
        }

        return finalizedPuzzle;
    }
}
