package org.dancres.blitz.tools.dash;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

public class StatsTableModel extends AbstractTableModel {
	private List _data = new ArrayList();
	private String[] _cols = new String[]{"Type", "Count", "reads", "writes",
			"takes"};

	StatsTableModel() {
	}
	public int getRowCount() {
		return _data.size();
	}
	public int getColumnCount() {
		return _cols.length;
	}
	public String getColumnName(int col) {
		return _cols[col];
	}
	public Object getValueAt(int r, int c) {
		Object[] rowData = (Object[]) _data.get(r);
		return rowData[c];
	}
	public void update(List data) {
		_data = data;
		fireTableDataChanged();
	}
}
