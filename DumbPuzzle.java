package assignment2;

import java.util.*;
import java.lang.IndexOutOfBoundsException;
import java.util.concurrent.TimeUnit;

public class DumbPuzzle {
    private ArrayList<Character> succession;
    private int[][] puzzle;

    public DumbPuzzle(char[][] input){
        this.succession = new ArrayList<Character>();
        this.puzzle = new int[input.length][];

        succession.add('_');

        for (int i = 0; i < input.length; ++i) {
            this.puzzle[i] = new int[input[i].length];

            for (int j = 0; j < input[i].length; ++j) {
                char c = input[i][j];

                if (!succession.contains(c)) {
                    succession.add(c);
                }

                this.puzzle[i][j] = -succession.indexOf(c);
            }
        }
    }

    public void solve() {
        Stack<Coord> decisions = new Stack<>();

        System.out.print("Succession: ");
        for (char c : succession) {
            System.out.print(c);
        }
        System.out.println();

        Coord current = new Coord(0,0);
        for (;;) {
            Coord next = nextEmpty(current);

            if (next == null) {
                if (isSolved()) {
                    return;
                }
                else {
                    current = backtrack(decisions);
                    if (current == null) {
                        return;
                    }
                }
            }
            else {
                if (isValid()) {
                    current = next;
                    performSuccessionAt(current);
                }
                else {
                    current = backtrack(decisions);
                    if (current == null) {
                        return;
                    }
                }
            }

            decisions.push(current);
            System.out.printf("\r%d", ++counter);
            //print();
            //System.out.println();
            //try {
            //    TimeUnit.MILLISECONDS.sleep(20);
            //}
            //catch (Exception e) { }
        }
    }
    private int counter = 0;

    private Coord backtrack(Stack<Coord> decisions) {
        Coord decision = null;

        while (!decisions.empty()) {
            decision = decisions.pop();

            if (performSuccessionAt(decision) != 0) {
                break;
            }
        }

        return decision;
    }

    public void print() {
        for(int[] row : this.puzzle) {
            assert row != null;

            for(int s : row) {
                char c;

                if (s < 0) {
                    c = succession.get(-s);
                }
                else {
                    c = Character.toLowerCase(succession.get(s));
                }

                System.out.print(c);
            }
            System.out.println();
        }
    }

    private int successor(int i) {
        return (i+1) % this.succession.size();
    }

    private int performSuccessionAt(Coord c) {
        int result = puzzle[c.r][c.c] = successor(puzzle[c.r][c.c]);
        return result;
    }

    /* solely here so that I don't need to new it every time */
    private Coord[] candidates = new Coord[4];

    public boolean isSolved() {
        for (int i = 0; i < puzzle.length; ++i) {
            int[] row = puzzle[i];

            for (int j = 0; j < row.length; ++j) {
                int cell = row[j];

                if (cell == 0) {
                    return false;
                }

                int count = 0;
                if (cell < 0) {
                    ++count;
                    cell = -cell;
                }

                candidates[0] = new Coord(i-1, j);
                candidates[1] = new Coord(i+1, j);
                candidates[2] = new Coord(i, j-1);
                candidates[3] = new Coord(i, j+1);

                for (Coord candidate : candidates) {
                    try {
                        int other = puzzle[candidate.r][candidate.c];
                        if (other < 0) other = -other;

                        if (other == cell) {
                            ++count;
                        }
                    }
                    catch (IndexOutOfBoundsException e) {
                        /* ignore */
                    }
                }

                if (count != 2) {
                    return false;
                }
            }
        }
        return true;
    }

    private Coord nextEmpty(Coord start) {
        int[] row = puzzle[start.r];

        for (int j = start.c; j < row.length; ++j) {
            if (row[j] == 0) {
                return new Coord(start.r,j);
            }
        }

        for (int i = start.r+1; i < puzzle.length; ++i) {
            row = puzzle[i];

            for (int j = 0; j < row.length; ++j) {
                if (row[j] == 0) {
                    return new Coord(i,j);
                }
            }
        }

        return null;
    }

    public boolean isValid() {
        for (int i = 0; i < puzzle.length; ++i) {
            int[] row = puzzle[i];

            for (int j = 0; j < row.length; ++j) {
                int cell = row[j];

                if (cell == 0) {
                    /* disregard blank cells */
                    continue;
                }

                int count = 0;
                if (cell < 0) {
                    ++count;
                    cell = -cell;
                }

                candidates[0] = new Coord(i-1, j);
                candidates[1] = new Coord(i+1, j);
                candidates[2] = new Coord(i, j-1);
                candidates[3] = new Coord(i, j+1);

                for (Coord candidate : candidates) {
                    try {
                        if (puzzle[candidate.r][candidate.c] == cell) {
                            ++count;
                        }
                    }
                    catch (IndexOutOfBoundsException e) {
                        /* ignore */
                    }
                }

                if (count > 2) {
                    return false;
                }
            }
        }
        return true;
    }
}
