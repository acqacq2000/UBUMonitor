package es.ubu.lsi.ubumonitor.view.chart;

import es.ubu.lsi.ubumonitor.controllers.MainController;
import es.ubu.lsi.ubumonitor.controllers.configuration.MainConfiguration;
import es.ubu.lsi.ubumonitor.model.Course;
import es.ubu.lsi.ubumonitor.model.CourseModule;
import es.ubu.lsi.ubumonitor.view.chart.forum.ForumBar;
import es.ubu.lsi.ubumonitor.view.chart.forum.ForumNetwork;
import es.ubu.lsi.ubumonitor.view.chart.forum.ForumTable;
import javafx.concurrent.Worker.State;
import javafx.scene.control.ListView;
import javafx.scene.web.WebView;

public class ForumConnector extends JavaConnectorAbstract {

	private static final ChartType DEFAULT_CHART = ChartType.DEFAULT_FORUM;
	public ForumConnector(WebView webView, MainConfiguration mainConfiguration, MainController mainController, Course actualCourse) {
		super(webView, mainConfiguration, mainController, actualCourse);
		ListView<CourseModule> listViewForum = mainController
				.getSelectionMainController()
				.getSelectionForumController()
				.getListViewForum();
		addChart(new ForumTable(mainController, webView,
				listViewForum));
		addChart(new ForumBar(mainController, listViewForum));
		addChart(new ForumNetwork(mainController, webView, listViewForum));
		currentChart = charts.get(DEFAULT_CHART);
	}

	

	public void manageOptions() {
	}
	
	@Override
	public void updateChart() {
		if (webEngine.getLoadWorker()
				.getState() != State.SUCCEEDED) {
			return;
		}
		manageOptions();
		currentChart.update();

	}
	@Override
	public void updateCharts(String typeChart) {
		Chart chart = charts.get(ChartType.valueOf(typeChart));
		if (currentChart.getChartType() != chart.getChartType()) {
			currentChart.clear();
			currentChart = chart;
		}
		manageOptions();
		currentChart.update();
	}




	
	




}
