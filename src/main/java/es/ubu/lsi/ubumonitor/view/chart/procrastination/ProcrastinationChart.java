package es.ubu.lsi.ubumonitor.view.chart.procrastination;

import java.io.IOException;

import es.ubu.lsi.ubumonitor.controllers.MainController;
import es.ubu.lsi.ubumonitor.util.JSArray;
import es.ubu.lsi.ubumonitor.util.JSObject;
import es.ubu.lsi.ubumonitor.view.chart.ChartType;
import es.ubu.lsi.ubumonitor.view.chart.Plotly;

public class ProcrastinationChart extends Plotly {

	public ProcrastinationChart(MainController mainController) {
		super(mainController, ChartType.PROCRASTINATION_CHART);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createData(JSArray data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createLayout(JSObject layout) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void exportCSV(String path) throws IOException {
		// TODO Auto-generated method stub
		
	}

}