package org.openl.rules.excel.builder.export;

import java.util.Objects;

public class Cursor {

    private final int column;
    private final int row;

    public Cursor(int column, int row) {
        this.column = column;
        this.row = row;
    }

    public Cursor moveLeft(int x) {
        return new Cursor(column - x, row);
    }

    public Cursor moveRight(int x) {
        return new Cursor(column + x, row);
    }

    public Cursor moveDown(int y) {
        return new Cursor(column, row + y);
    }

    public Cursor moveUp(int y) {
        return new Cursor(column, row - y);
    }

    public Cursor setColumn(int x) {
        return new Cursor(x, row);
    }

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Cursor cursor = (Cursor) o;
        return column == cursor.column && row == cursor.row;
    }

    @Override
    public int hashCode() {
        return Objects.hash(column, row);
    }
}
