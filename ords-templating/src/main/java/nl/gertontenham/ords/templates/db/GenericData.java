package nl.gertontenham.ords.templates.db;

/**
 * Database column object. Includes some metadata and the value
 */
public class GenericData {

    private final String columnType;
    private final int columnNullable;
    private final int columnPrecision;
    private final int columnScale;
    private final int columnDisplaySize;
    private final Object columnValue;

    public GenericData(String columnType, int columnNullable, int columnPrecision,
                       int columnScale, int columnDisplaySize, Object columnValue) {
        this.columnType = columnType;
        this.columnNullable = columnNullable;
        this.columnPrecision = columnPrecision;
        this.columnScale = columnScale;
        this.columnDisplaySize = columnDisplaySize;
        this.columnValue = columnValue;
    }

    public String getColumnType() {
        return columnType;
    }

    public int getColumnNullable() {
        return columnNullable;
    }

    public int getColumnPrecision() {
        return columnPrecision;
    }

    public int getColumnScale() {
        return columnScale;
    }

    public int getColumnDisplaySize() {
        return columnDisplaySize;
    }

    public Object getColumnValue() {
        return columnValue;
    }
}
