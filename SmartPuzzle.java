package assignment2;

import java.util.*;
import java.lang.IndexOutOfBoundsException;
import java.util.concurrent.TimeUnit;
import java.util.Timer;

public class SmartPuzzle implements Puzzle {
    /* NOTE:
     *
     * Each cell of the puzzle is to be interpreted as a bitfield with the top
     * 2 bits representing flags and the bottom 30 bits representing a
     * superposision of colors.
     */
    private int[][] puzzle;

    private static final boolean PRINT_EVERY_FORWARD_CELL = false;
    private static final boolean PRINT_EVERY_BACKWARD_CELL = false;
    private static final boolean PRINT_AFTER_PASS = false;

    private static final int FIXED_FLAG = 1 << 31;
    private static final int DECIDED_FLAG = 1 << 30;
    private static final int FLAGS_MASK = FIXED_FLAG | DECIDED_FLAG;
    private static final int SUPERPOSISION_MASK = ~FLAGS_MASK;
    private final int ALL_COLORS_MASK;

    private List<Character> palette;
    private static final char UNSET_SYMBOL = '_';

    public SmartPuzzle(char[][] raw) {
        puzzle = new int[raw.length][];
        palette = new ArrayList<>();

        palette.add(UNSET_SYMBOL);

        for (int r = 0; r < raw.length; ++r) {
            char[] row = raw[r];
            puzzle[r] = new int[row.length];

            for (int c = 0; c < row.length; ++c) {
                char symbol = row[c];

                int idx;
                if (!palette.contains(symbol)) {
                    idx = palette.size();
                    palette.add(symbol);
                }
                else {
                    idx = palette.indexOf(symbol);
                }

                assert idx < 30;

                int field = colorFlagRepresenting(idx);

                if (symbol != UNSET_SYMBOL) {
                    field |= FIXED_FLAG;
                }

                puzzle[r][c] = field;
            }
        }

        printBufferSize = palette.size() + 1;
        printBuffer = new char[printBufferSize];

        ALL_COLORS_MASK = (1 << palette.size()) - 1;
    }

    public void solve() {
        final Stack<Coord> decisionLocations = new Stack<>();
        final Stack<Integer> decisionPreviousValues = new Stack<>();

        resetConstraints();

        print();
        System.out.println();
        try {
            TimeUnit.MILLISECONDS.sleep(200);
        }
        catch (Exception e) { }

        constrain();

        if (evaluate() == BoardStatus.VALID) {
            print();
            System.out.println();
            try {
                TimeUnit.MILLISECONDS.sleep(200);
            }
            catch (Exception e) { }

            for (Coord decision;;) {
                decision = nextMostConstrainedButUnDecided();

                decisionLocations.push(decision);
                decisionPreviousValues.push(superposisionAt(decision));

                puzzle[decision.r][decision.c] = DECIDED_FLAG | Integer.lowestOneBit(superposisionAt(decision));

                constrain();

                print();
                System.out.println();
                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                }
                catch (Exception e) { }

                BoardStatus status = evaluate();
                if (status == BoardStatus.INVALID) {
                    if (decisionLocations.empty()) {
                        break;
                    }
                    else {
                        /* we've got to back track */

                        decision = decisionLocations.pop();
                        int prevSP = decisionPreviousValues.pop();
                        int triedValue = superposisionAt(decision);
                        int nextValue = prevSP ^ triedValue;

                        resetConstraints();
                        puzzle[decision.r][decision.c] = nextValue;

                        if (Integer.bitCount(nextValue) == 1) {
                            puzzle[decision.r][decision.c] |= DECIDED_FLAG;
                        }
                    }
                }
                else if (status == BoardStatus.SOLVED) {
                    break;
                }
            }
        }
    }

    /* so I don't have to new it every time */
    private final Coord neighbors[] = new Coord[4];
    private final List<Coord> superCells = new ArrayList<>();
    private final List<Coord> equalCells = new ArrayList<>();
    private final List<Coord> disjointCells = new ArrayList<>();
    private final List<Coord> underConstrainedCells = new ArrayList<>();

    private void resetConstraints() {
        /* maximally "superpose" every cell */
        for (int r = 0; r < puzzle.length; ++r) {
            int[] row = puzzle[r];

            for (int c = 0; c < row.length; ++c) {
                if (flagsFor(row[c]) == 0) {
                    row[c] |= ALL_COLORS_MASK;
                }
            }
        }
    }

    private void constrain() {
        boolean constrainedCell;

        /* using the forward-backward thrashing model */

        do {
            constrainedCell = false;

            for (int r = 0; r < puzzle.length; ++r) {
                int[] row = puzzle[r];
                for (int c = 0; c < row.length; ++c) {
                    /* forward constrain */

                    /* reset our lists for reuse */
                    superCells.clear();
                    equalCells.clear();
                    disjointCells.clear();
                    underConstrainedCells.clear();

                    neighbors[0] = new Coord(r-1, c);
                    neighbors[1] = new Coord(r+1, c);
                    neighbors[2] = new Coord(r, c-1);
                    neighbors[3] = new Coord(r, c+1);

                    int cellSuperposision = superposisionFor(row[c]);

                    if (cellSuperposision == 0) {
                        break;
                    }

                    for (Coord neighbor : neighbors) {
                        int neighborSuperposision = superposisionAt(neighbor);
                        int intersection = neighborSuperposision & cellSuperposision;

                        if (intersection == cellSuperposision) {
                            superCells.add(neighbor);
                        }

                        if (neighborSuperposision == cellSuperposision) {
                            equalCells.add(neighbor);
                        }

                        if (intersection == 0) {
                            disjointCells.add(neighbor);
                        }

                        if (Integer.bitCount(neighborSuperposision) > 1) {
                            underConstrainedCells.add(neighbor);
                        }
                    }

                    if (flagsFor(row[c]) == FIXED_FLAG) {
                        assert Integer.bitCount(cellSuperposision) == 1;

                        if (superCells.size() == 1) {
                            for (Coord neighbor : neighbors) {
                                if (superCells.contains(neighbor)) {
                                    constrainedCell |= applySuperposisionAt(neighbor, cellSuperposision);
                                }
                                else {
                                    constrainedCell |= applySuperposisionAt(neighbor, ~cellSuperposision);
                                }
                            }
                        }
                        else if (equalCells.size() == 1) {
                            for (Coord neighbor : neighbors) {
                                if (!equalCells.contains(neighbor)) {
                                    constrainedCell |= applySuperposisionAt(neighbor, ~cellSuperposision);
                                }
                            }
                        }
                    }
                    else {
                        //if (Integer.bitCount(cellSuperposision) == 1 && equalCells.size() == 2) {
                            //for (Coord neighbor : neighbors) {
                                //if (!equalCells.contains(neighbor)) {
                                    //constrainedCell |= applySuperposisionAt(neighbor, ~cellSuperposision);
                                //}
                            //}
                        //}
                        //else if (disjointCells.size() == 2) {
                            //for (Coord neighbor : neighbors) {
                                //if (!disjointCells.contains(neighbor)) {
                                    //constrainedCell |= applySuperposisionAt(neighbor, cellSuperposision);
                                //}
                            //}
                        //}
                        //else if (underConstrainedCells.size() == 1) {
                            //Coord cell = underConstrainedCells.get(0);
                            //boolean doit = true;

                            //for (Coord neighbor : neighbors) {
                                //if (neighbor == cell) continue;

                                //for (Coord otherNeighbor : neighbors) {
                                    //if (neighbor == otherNeighbor) continue;

                                    //if (superposisionAt(neighbor) == superposisionAt(otherNeighbor)) {
                                        //doit = false;
                                    //}
                                //}
                            //}

                            //if (doit) {
                                //constrainedCell |= applySuperposisionAt(cell, cellSuperposision);
                            //}
                        //}
                    }

                    if (PRINT_EVERY_FORWARD_CELL) {
                        print(r,c);
                        System.out.println();
                    }
                }
            }

            if (PRINT_AFTER_PASS) {
                System.out.printf("forward\n");
                print();
                try {
                    //TimeUnit.MILLISECONDS.sleep(200);
                    System.in.read();
                }
                catch (Exception e) { }
            }

            for (int r = 0; r < puzzle.length; ++r) {
                int[] row = puzzle[r];
                for (int c = 0; c < row.length; ++c) {
                    if (flagsFor(row[c]) != 0) continue;

                    /* backward constrain */
                    int colorsRouted = 0;

                    neighbors[0] = new Coord(r-1, c);
                    neighbors[1] = new Coord(r+1, c);
                    neighbors[2] = new Coord(r, c-1);
                    neighbors[3] = new Coord(r, c+1);

                    for (Coord neighbor : neighbors) {
                        for (Coord otherNeighbor : neighbors) {
                            if (neighbor == otherNeighbor) {
                                continue;
                            }

                            int neighborSuperposision = superposisionAt(neighbor);
                            int otherNeighborSuperposision = superposisionAt(otherNeighbor);

                            colorsRouted |= neighborSuperposision & otherNeighborSuperposision;
                        }
                    }

                    int old = row[c];
                    row[c] &= FLAGS_MASK | (colorsRouted & SUPERPOSISION_MASK);
                    constrainedCell |= (row[c] != old);
                }
            }

            if (PRINT_AFTER_PASS) {
                System.out.printf("backward\n");
                print();
                System.out.println();
                try {
                    //TimeUnit.MILLISECONDS.sleep(200);
                    System.in.read();
                }
                catch (Exception e) { }
            }
        }
        while (constrainedCell);
    }

    /* here so that I don't neet to recalculate them every time I print */
    private final int printBufferSize;
    private final char[] printBuffer;

    public void print() {
        print(-1,-1);
    }

    public void print(int rr, int cc) {
        //for (int[] row : puzzle) {
        for (int r = 0; r < puzzle.length; ++r) {
            int[] row = puzzle[r];
            //for (int field : row) {
            for (int c = 0; c < row.length; ++c) {
                int field = row[c];
                boolean toLower = true;
                if ((field & FIXED_FLAG) != 0) {
                    toLower = false;
                    printBuffer[0] = '[';
                    printBuffer[printBufferSize-1] = ']';
                }
                else if ((field & DECIDED_FLAG) != 0) {
                    printBuffer[0] = '(';
                    printBuffer[printBufferSize-1] = ')';
                }
                else {
                    printBuffer[0] = '{';
                    printBuffer[printBufferSize-1] = '}';
                }

                if (r == rr && c == cc) {
                    printBuffer[0] = '>';
                    printBuffer[printBufferSize-1] = '<';
                }

                for (int i = 1; i < palette.size(); ++i) {
                    char symbol = ' ';
                    if ((field & (1 << (i-1))) != 0) {
                        symbol = palette.get(i);
                    }

                    if (toLower) {
                        symbol = Character.toLowerCase(symbol);
                    }

                    printBuffer[i] = symbol;
                }

                System.out.print(printBuffer);
            }
            System.out.println();
        }
    }

    private BoardStatus evaluate() {
        BoardStatus status = BoardStatus.SOLVED;

        for (int r = 0; r < puzzle.length; ++r) {
            int[] row = puzzle[r];

            for (int c = 0; c < row.length; ++c) {
                int sp = superposisionFor(row[c]);

                if (sp == 0) {
                    return BoardStatus.INVALID;
                }
                else if (Integer.bitCount(sp) > 1) {
                    status = BoardStatus.VALID;
                }
            }
        }

        return status;
    }

    private enum BoardStatus {
        INVALID,
        VALID,
        SOLVED,
    }

    private Coord nextMostConstrainedButUnDecided() {
        Coord theThing = new Coord(-1, -1);
        int constraints = Integer.MAX_VALUE;

        for (int r = 0; r < puzzle.length; ++r) {
            int[] row = puzzle[r];

            for (int c = 0; c < row.length; ++c) {
                int count = Integer.bitCount(superposisionFor(row[c]));

                if (count > 1 && count < constraints) {
                    constraints = count;
                    theThing.r = r;
                    theThing.c = c;
                    System.out.printf(">>>found better! (%d,%d)\n", r, c);
                }
            }
        }

        if (theThing.r == -1) {
            /* no cell was found */
            return null;
        }
        else {
            return theThing;
        }
    }



    /* CONVENIENCE FUNCTIONS BELOW */



    private int colorFlagRepresenting(int idx) {
        /* basically equivalent to floor(pow(2, idx-1) */
        return (1 << idx) >>> 1;
    }

    private int superposisionFor(int cell) {
        return cell & SUPERPOSISION_MASK;
    }

    private int flagsFor(int cell) {
        return cell & FLAGS_MASK;
    }

    private int superposisionAt(Coord loc) {
        try {
            return superposisionFor(puzzle[loc.r][loc.c]);
        }
        catch (Exception e) {
            return 0;
        }
    }

    private int flagsAt(Coord loc) {
        try {
            return flagsFor(puzzle[loc.r][loc.c]);
        }
        catch (Exception e) {
            return 0;
        }
    }

    private boolean applySuperposisionAt(Coord loc, int sp) {
        try {
            int old = puzzle[loc.r][loc.c];

            puzzle[loc.r][loc.c] &= FLAGS_MASK | (sp & SUPERPOSISION_MASK);

            return (puzzle[loc.r][loc.c] != old);
        }
        catch (Exception e) {
            return false;
        }
    }
}
