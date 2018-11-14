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
     * superposition of colors.
     */
    private int[][] puzzle;

    private static final boolean PRINT_EVERY_FORWARD_CELL = false;
    private static final boolean PRINT_EVERY_BACKWARD_CELL = false;
    private static final boolean PRINT_AFTER_PASS = false;
    private static final boolean PRINT_AFTER_SELECTION = false;

    private static final int FIXED_FLAG = 1 << 31;
    private static final int DECIDED_FLAG = 1 << 30;
    private static final int FLAGS_MASK = FIXED_FLAG | DECIDED_FLAG;
    private static final int SUPERPOSITION_MASK = ~FLAGS_MASK;
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
        final Stack<Coord> locations = new Stack<>();
        final Stack<Integer> values = new Stack<>();
        BoardStatus status;

        resetConstraints();
        for (;;) {
            constrain();
            Coord decision = nextMostConstrainedButUnDecided();
            int superposition = superpositionAt(decision);

            if (evaluate() == BoardStatus.INVALID) {
                /* back track */
                int nextSP;

                do {
                    decision = locations.pop();
                    superposition = values.pop() ^ superpositionAt(decision);

                    setFlagsAt(decision, 0);
                }
                while (superposition == 0);

                resetConstraints();
            }

            if (decision != null) {
                locations.push(decision);
                values.push(superposition);

                puzzle[decision.r][decision.c] = DECIDED_FLAG | Integer.lowestOneBit(superposition);

                if (PRINT_AFTER_SELECTION) {
                    System.out.printf("select %d,%d\n", decision.r, decision.c);
                    print();
                }
            }
            else {
                break;
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

                    if (superpositionFor(row[c]) == 0) {
                        return;
                    }

                    /* reset our lists for reuse */
                    superCells.clear();
                    equalCells.clear();
                    disjointCells.clear();
                    underConstrainedCells.clear();

                    neighbors[0] = new Coord(r-1, c);
                    neighbors[1] = new Coord(r+1, c);
                    neighbors[2] = new Coord(r, c-1);
                    neighbors[3] = new Coord(r, c+1);

                    int cellsuperposition = superpositionFor(row[c]);

                    if (cellsuperposition == 0) {
                        break;
                    }

                    for (Coord neighbor : neighbors) {
                        int neighborsuperposition = superpositionAt(neighbor);
                        int intersection = neighborsuperposition & cellsuperposition;

                        if (intersection == cellsuperposition) {
                            superCells.add(neighbor);
                        }

                        if (neighborsuperposition == cellsuperposition) {
                            equalCells.add(neighbor);
                        }

                        if (intersection == 0) {
                            disjointCells.add(neighbor);
                        }

                        if (Integer.bitCount(neighborsuperposition) > 1) {
                            underConstrainedCells.add(neighbor);
                        }
                    }

                    if (flagsFor(row[c]) == FIXED_FLAG) {
                        assert Integer.bitCount(cellsuperposition) == 1;

                        if (superCells.size() == 1) {
                            for (Coord neighbor : neighbors) {
                                if (superCells.contains(neighbor)) {
                                    constrainedCell |= applySuperpositionAt(neighbor, cellsuperposition);
                                }
                                else {
                                    constrainedCell |= applySuperpositionAt(neighbor, ~cellsuperposition);
                                }
                            }
                        }
                        else if (equalCells.size() == 1) {
                            for (Coord neighbor : neighbors) {
                                if (!equalCells.contains(neighbor)) {
                                    constrainedCell |= applySuperpositionAt(neighbor, ~cellsuperposition);
                                }
                            }
                        }
                    }
                    else {
                        if (Integer.bitCount(cellsuperposition) == 1 && equalCells.size() == 2) {
                            for (Coord neighbor : neighbors) {
                                if (!equalCells.contains(neighbor)) {
                                    constrainedCell |= applySuperpositionAt(neighbor, ~cellsuperposition);
                                }
                            }
                        }
                        else if (disjointCells.size() == 2) {
                            for (Coord neighbor : neighbors) {
                                if (!disjointCells.contains(neighbor)) {
                                    constrainedCell |= applySuperpositionAt(neighbor, cellsuperposition);
                                }
                            }
                        }
                        else if (underConstrainedCells.size() == 1) {
                            Coord cell = underConstrainedCells.get(0);
                            boolean doit = true;

                            for (Coord neighbor : neighbors) {
                                if (neighbor == cell) continue;

                                for (Coord otherNeighbor : neighbors) {
                                    if (neighbor == otherNeighbor) continue;

                                    if (superpositionAt(neighbor) == superpositionAt(otherNeighbor)) {
                                        doit = false;
                                    }
                                }
                            }

                            if (doit) {
                                constrainedCell |= applySuperpositionAt(cell, cellsuperposition);
                            }
                        }
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
            }

            for (int r = 0; r < puzzle.length; ++r) {
                int[] row = puzzle[r];
                for (int c = 0; c < row.length; ++c) {
                    if (flagsFor(row[c]) != 0) continue;
                    if (superpositionFor(row[c]) == 0) {
                        return;
                    }

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

                            int neighborsuperposition = superpositionAt(neighbor);
                            int otherNeighborsuperposition = superpositionAt(otherNeighbor);

                            colorsRouted |= neighborsuperposition & otherNeighborsuperposition;
                        }
                    }

                    int old = row[c];
                    row[c] &= FLAGS_MASK | (colorsRouted & SUPERPOSITION_MASK);
                    constrainedCell |= (row[c] != old);
                }
            }

            if (PRINT_AFTER_PASS) {
                System.out.printf("backward\n");
                print();
            }
        }
        while (constrainedCell);
    }

    /* here so that I don't neet to recalculate them every time I print */
    private final int printBufferSize;
    private final char[] printBuffer;

    public void print() {
        //print(-1,-1);
        for (int[] row : puzzle) {
            for (int field : row) {
                int flags = flagsFor(field);
                int sp = superpositionFor(field);
                char symbol;

                if (Integer.bitCount(sp) > 1) {
                    symbol = '?';
                }
                else {
                    int idx = 32 - Integer.numberOfLeadingZeros(sp);
                    symbol = palette.get(idx);
                }

                if ((flags & FIXED_FLAG) == 0) {
                    symbol = Character.toLowerCase(symbol);
                }

                System.out.print(symbol);
            }
            System.out.println();
        }

        System.out.println();
        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (Exception e) { }
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

        System.out.println();
        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (Exception e) { }
    }

    private BoardStatus evaluate() {
        BoardStatus status = BoardStatus.SOLVED;

        for (int r = 0; r < puzzle.length; ++r) {
            int[] row = puzzle[r];

            for (int c = 0; c < row.length; ++c) {
                int sp = superpositionFor(row[c]);

                if (sp == 0) {
                    if (PRINT_AFTER_SELECTION) {
                        System.err.println("BLANK!");
                    }
                    return BoardStatus.INVALID;
                }
                else if (Integer.bitCount(sp) == 1) {
                    int equalCount = 0;
                    int superCount = 0;
                    boolean hasUnderConstrainedNeighbor = false;
                    if (flagsFor(row[c]) == FIXED_FLAG) {
                        ++equalCount;
                        ++superCount;
                    }

                    neighbors[0] = new Coord(r-1, c);
                    neighbors[1] = new Coord(r+1, c);
                    neighbors[2] = new Coord(r, c-1);
                    neighbors[3] = new Coord(r, c+1);

                    for (Coord neighbor : neighbors) {
                        int other = superpositionAt(neighbor);

                        if (other == sp) {
                            ++equalCount;
                        }

                        if ((other & sp) == sp) {
                            ++superCount;
                        }

                        if (Integer.bitCount(other) > 1) {
                            hasUnderConstrainedNeighbor = true;
                        }
                    }

                    if (equalCount > 2) {
                        if (PRINT_AFTER_SELECTION) {
                            System.err.printf("TOO MANY! (%d,%d)\n", r, c);
                        }
                        return BoardStatus.INVALID;
                    }
                    else if (superCount < 2) {
                        if (PRINT_AFTER_SELECTION) {
                            System.err.printf("TOO FEW! (%d,%d)\n", r, c);
                        }
                        return BoardStatus.INVALID;
                    }
                    else if (!hasUnderConstrainedNeighbor && equalCount != 2) {
                        if (PRINT_AFTER_SELECTION) {
                            System.err.printf("NOT THE RIGHT AMOUNT! (%d,%d) <%d>\n", r, c, equalCount);
                        }
                        return BoardStatus.INVALID;
                    }
                    else {
                        status = BoardStatus.VALID;
                    }
                }
                else {
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
        Coord coord = new Coord(-1, -1);
        int constraints = Integer.MAX_VALUE;

        for (int r = 0; r < puzzle.length; ++r) {
            int[] row = puzzle[r];

            for (int c = 0; c < row.length; ++c) {
                int count = Integer.bitCount(superpositionFor(row[c]));

                if (count > 1 && count < constraints) {
                    constraints = count;
                    coord.r = r;
                    coord.c = c;
                }
            }
        }

        if (coord.r == -1) {
            /* no cell was found */
            return null;
        }
        else {
            return coord;
        }
    }



    /* CONVENIENCE FUNCTIONS BELOW */



    private int colorFlagRepresenting(int idx) {
        /* basically equivalent to floor(pow(2, idx-1) */
        return (1 << idx) >>> 1;
    }

    private int superpositionFor(int cell) {
        return cell & SUPERPOSITION_MASK;
    }

    private int flagsFor(int cell) {
        return cell & FLAGS_MASK;
    }

    private int superpositionAt(Coord loc) {
        try {
            return superpositionFor(puzzle[loc.r][loc.c]);
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

    private boolean applySuperpositionAt(Coord loc, int sp) {
        try {
            int old = puzzle[loc.r][loc.c];

            puzzle[loc.r][loc.c] &= FLAGS_MASK | (sp & SUPERPOSITION_MASK);

            return (puzzle[loc.r][loc.c] != old);
        }
        catch (Exception e) {
            return false;
        }
    }

    private void setFlagsAt(Coord loc, int flags) {
        puzzle[loc.r][loc.c] &= SUPERPOSITION_MASK; /* keep superposition */
        puzzle[loc.r][loc.c] |= flags & FLAGS_MASK; /* set flags */
    }

    private void setSuperpositionAt(Coord loc, int sp) {
        puzzle[loc.r][loc.c] &= FLAGS_MASK; /* keep flags */
        puzzle[loc.r][loc.c] |= sp & SUPERPOSITION_MASK; /* set superposition */
    }
}
