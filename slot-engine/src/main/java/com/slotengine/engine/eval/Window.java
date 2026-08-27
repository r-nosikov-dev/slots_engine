package com.slotengine.engine.eval;

import com.slotengine.model.GridSize;
import com.slotengine.model.Position;
import com.slotengine.model.ReelSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Visible play window. Storage is column-major: {@code cells[reel][row]}, row 0 is the top.
 */
public final class Window {

    private final String[][] cells;
    private final int[] stops;
    private final GridSize grid;

    public Window(String[][] cells, int[] stops) {
        this.grid = new GridSize(cells.length, cells[0].length);
        this.cells = new String[cells.length][];
        for (int r = 0; r < cells.length; r++) {
            this.cells[r] = cells[r].clone();
        }
        this.stops = stops.clone();
    }

    public static Window fromStops(ReelSet reels, int[] stops, int rows) {
        if (stops.length != reels.reelCount()) {
            throw new IllegalArgumentException("stops/reels mismatch");
        }
        String[][] cells = new String[reels.reelCount()][rows];
        for (int reel = 0; reel < reels.reelCount(); reel++) {
            List<String> column = reels.reel(reel).window(stops[reel], rows);
            for (int row = 0; row < rows; row++) {
                cells[reel][row] = column.get(row);
            }
        }
        return new Window(cells, stops);
    }

    public GridSize grid() {
        return grid;
    }

    public int reels() {
        return grid.reels();
    }

    public int rows() {
        return grid.rows();
    }

    public String at(int reel, int row) {
        return cells[reel][row];
    }

    public String at(Position position) {
        return cells[position.reel()][position.row()];
    }

    public int[] stops() {
        return stops.clone();
    }

    public String[] column(int reel) {
        return cells[reel].clone();
    }

    /** Row-major view for JSON/frontends: {@code [row][reel]}. */
    public List<List<String>> rowsFirst() {
        List<List<String>> rows = new ArrayList<>(grid.rows());
        for (int row = 0; row < grid.rows(); row++) {
            List<String> line = new ArrayList<>(grid.reels());
            for (int reel = 0; reel < grid.reels(); reel++) {
                line.add(cells[reel][row]);
            }
            rows.add(List.copyOf(line));
        }
        return List.copyOf(rows);
    }

    /** Column-major view: {@code [reel][row]}. */
    public List<List<String>> columnsFirst() {
        List<List<String>> cols = new ArrayList<>(grid.reels());
        for (int reel = 0; reel < grid.reels(); reel++) {
            cols.add(List.of(cells[reel]));
        }
        return List.copyOf(cols);
    }

    public Window withCell(int reel, int row, String symbolId) {
        String[][] copy = copyCells();
        copy[reel][row] = symbolId;
        return new Window(copy, stops);
    }

    public Window withColumn(int reel, String[] column) {
        String[][] copy = copyCells();
        copy[reel] = column.clone();
        return new Window(copy, stops);
    }

    public int count(String symbolId) {
        int n = 0;
        for (int reel = 0; reel < grid.reels(); reel++) {
            for (int row = 0; row < grid.rows(); row++) {
                if (symbolId.equals(cells[reel][row])) {
                    n++;
                }
            }
        }
        return n;
    }

    public List<Position> positionsOf(String symbolId) {
        List<Position> positions = new ArrayList<>();
        for (int reel = 0; reel < grid.reels(); reel++) {
            for (int row = 0; row < grid.rows(); row++) {
                if (symbolId.equals(cells[reel][row])) {
                    positions.add(new Position(reel, row));
                }
            }
        }
        return List.copyOf(positions);
    }

    private String[][] copyCells() {
        String[][] copy = new String[cells.length][];
        for (int i = 0; i < cells.length; i++) {
            copy[i] = cells[i].clone();
        }
        return copy;
    }

    @Override
    public String toString() {
        return Arrays.deepToString(cells);
    }
}
