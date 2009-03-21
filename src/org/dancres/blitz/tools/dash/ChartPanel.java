package org.dancres.blitz.tools.dash;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JPanel;

import org.dancres.blitz.tools.dash.graph.*;

public class ChartPanel extends JPanel {
	private Chart _chart = new Chart();
	private Map _lookup = new HashMap();
	private long[] _lastValues;
	private final int MAX_SIZE = 100;
	public ChartPanel() {
		setLayout(new BorderLayout());
		add(_chart, BorderLayout.CENTER);
	}
	public void update(String[] names, long[] newValues) {
		try {
			if (_lastValues == null) {
				doUpdate(names, newValues);
			}
			for (int i = 0; i < newValues.length; i++) {
				if (newValues[i] != _lastValues[i]) {
					doUpdate(names, newValues);
					return;
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	private void doUpdate(String[] names, long[] newValues) throws Exception {
		_lastValues = newValues;
		for (int i = 0; i < newValues.length; i++) {
			ArrayList history = (ArrayList) _lookup.get(names[i]);
			if (history == null) {
				if (newValues.length == 0 && newValues[i] == 0) {
					return;
				}
				if (newValues.length > 0) {
					//if they are all zero ommit
					boolean ok = false;
					for (int j = 0; !ok && j < newValues.length; j++) {
						if (newValues[j] > 0) {
							ok = true;
						}
					}
					if (!ok) {
						return;
					}
				}
				
				history = new ArrayList();
				_lookup.put(names[i], history);
				//add twice the first time to get a line
				history.add(new Long(newValues[i]));
				//first time so add the data
				long startVal=0;//newValues[i];// - 1;
				//if(startVal<0){
					//startVal=0;
				//}
				addData(names[i], new double[]{startVal, newValues[i]});
			} else {
				
				history.add(new Long(newValues[i]));
				int n = history.size();
				if (n > MAX_SIZE) {
					n--;
					history.remove(0);
					
				}
				
				double[] data = new double[n];
				String[] labels = new String[n];
				for (int j = 0; j < n; j++) {
					labels[j] = "";
					data[j] = ((Long) history.get(j)).longValue();
					
				}
				labels[0] = "Last update: " + new java.util.Date();
				_chart.setDataAt(i, names[i], data, labels, null);
			}
		}
		_chart.repaint();
	}
	private void addData(String name, double[] data) throws Exception {
		String[] labs = new String[data.length];
		labs[0] = "Last update: " + new java.util.Date();
		for (int i = 1; i < labs.length; i++) {
			labs[i] = "";
		}
		_chart.addData(name, data, labs);
	}
}
