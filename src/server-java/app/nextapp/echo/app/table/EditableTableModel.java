package nextapp.echo.app.table;

/**
 * Interface that marks the capability of a table model to receive modifications
 * to it's contents.
 * 
 * @author Lloyd Colling
 * 
 */
public interface EditableTableModel extends TableModel {

    /**
     * Sets the contents of the table cell at the specified coordinate.
     * 
     * @param newValue
     *            the new value
     * @param col
     *            the column index
     * @param row
     *            the row index
     * @throws ArrayIndexOutOfBoundsException
     *             if the column or row index exceed the column or row count
     */
    public void setValueAt(Object newValue, int col, int row);
}
