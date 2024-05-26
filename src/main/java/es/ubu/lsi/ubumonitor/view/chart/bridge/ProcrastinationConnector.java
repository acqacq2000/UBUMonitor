package es.ubu.lsi.ubumonitor.view.chart.bridge;

import org.controlsfx.control.CheckComboBox;

import es.ubu.lsi.ubumonitor.controllers.MainController;
import es.ubu.lsi.ubumonitor.controllers.configuration.MainConfiguration;
import es.ubu.lsi.ubumonitor.controllers.tabs.ProcrastinationController;
import es.ubu.lsi.ubumonitor.model.ComponentEvent;
import es.ubu.lsi.ubumonitor.model.Course;
import es.ubu.lsi.ubumonitor.model.CourseModule;
import es.ubu.lsi.ubumonitor.view.chart.ChartType;
import es.ubu.lsi.ubumonitor.view.chart.Tabs;
import es.ubu.lsi.ubumonitor.view.chart.procrastination.ProcrastinationBars;
import es.ubu.lsi.ubumonitor.view.chart.procrastination.ProcrastinationScatter;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebView;

public class ProcrastinationConnector extends JavaConnectorAbstract {
	private ProcrastinationController procrastinationController;
	
	private GridPane dateGridPane;

	public ProcrastinationConnector(WebView webView, MainConfiguration mainConfiguration, MainController mainController,
			Course actualCourse, GridPane dateGridPane, DatePicker datePickerStart, DatePicker datePickerEnd) {
		super(webView, mainConfiguration, mainController, actualCourse);
		this.dateGridPane = dateGridPane;
		ListView<CourseModule> listViewProcrastination = mainController.getSelectionMainController()
				.getSelectionProcrastinationController()
				.getListViewProcrastination();
		CheckComboBox<ComponentEvent> listViewProcrastinationEvent = mainController.getSelectionMainController()
				.getSelectionProcrastinationController()
				.getListViewProcrastinationEvent();
		addChart(new ProcrastinationBars(mainController, listViewProcrastination, listViewProcrastinationEvent, datePickerStart, datePickerEnd));
		addChart(new ProcrastinationScatter(mainController, listViewProcrastination, listViewProcrastinationEvent));

		currentChart = charts.get(ChartType.getDefault(Tabs.PROCRASTINATION));
	}


	@Override
	public void manageOptions() {
		dateGridPane.setVisible(currentChart.isUseRangeDate());
	}
}