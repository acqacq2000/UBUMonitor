package es.ubu.lsi.ubumonitor.view.chart.activitystatus;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.ubu.lsi.ubumonitor.controllers.MainController;
import es.ubu.lsi.ubumonitor.model.ActivityCompletion;
import es.ubu.lsi.ubumonitor.model.ActivityCompletion.State;
import es.ubu.lsi.ubumonitor.model.CourseModule;
import es.ubu.lsi.ubumonitor.model.EnrolledUser;
import es.ubu.lsi.ubumonitor.util.DateTimeWrapper;
import es.ubu.lsi.ubumonitor.util.I18n;
import es.ubu.lsi.ubumonitor.util.JSArray;
import es.ubu.lsi.ubumonitor.util.JSObject;
import es.ubu.lsi.ubumonitor.view.chart.ChartType;
import es.ubu.lsi.ubumonitor.view.chart.Tabulator;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.web.WebView;

public class ActivitiesStatusTable extends Tabulator {
	private static final Logger LOGGER = LoggerFactory.getLogger(ActivitiesStatusTable.class);
	private DateTimeWrapper dateTimeWrapper;
	private DatePicker datePickerStart;
	private DatePicker datePickerEnd;
	private ListView<CourseModule> listViewActivity;

	public ActivitiesStatusTable(MainController mainController, DatePicker datePickerStart, DatePicker datePickerEnd,
			ListView<CourseModule> listViewActivity, WebView webView) {
		super(mainController, ChartType.ACTIVITIES_TABLE, webView);
		this.datePickerStart = datePickerStart;
		this.datePickerEnd = datePickerEnd;
		this.listViewActivity = listViewActivity;
		dateTimeWrapper = new DateTimeWrapper();

		useRangeDate = true;
	}

	public String createColumns(List<CourseModule> courseModules) {
		// users columns
		JSObject jsObject = new JSObject();
		JSArray array = new JSArray();

		jsObject.putWithQuote("title", I18n.get("chartlabel.name"));
		jsObject.put("tooltip", true);
		jsObject.put("field", "'name'");
		jsObject.put("frozen", true);
		array.add(jsObject);

		JSObject formatterParams = new JSObject();
		formatterParams.put("allowEmpty", true);
		formatterParams.put("allowTruthy", true);
		String stringFormatterParams = formatterParams.toString();

		JSObject sorterParams = new JSObject();
		sorterParams.putWithQuote("format", dateTimeWrapper.getPattern());
		sorterParams.putWithQuote("alignEmptyValues", "bottom");
		String stringsorterParams = sorterParams.toString();

		for (CourseModule courseModule : courseModules) {
			jsObject = new JSObject();
			jsObject.putWithQuote("align", "center");
			jsObject.put("tooltip", true);

			jsObject.putWithQuote("formatter", "tickCross");
			jsObject.put("topCalc",
					"function(n,r,c){var f=0;return n.forEach(function(n){n&&f++;}),f+'/'+n.length+' ('+(f/n.length||0).toLocaleString(locale,{style:'percent',maximumFractionDigits:2})+')';}");
			jsObject.put("formatterParams", stringFormatterParams);
			jsObject.putWithQuote("sorter", "datetime");
			jsObject.put("sorterParams", stringsorterParams);
			jsObject.putWithQuote("title", courseModule.getModuleName());
			jsObject.putWithQuote("field", "ID" + courseModule.getCmid());

			array.add(jsObject.toString());
		}

		jsObject = new JSObject();
		jsObject.putWithQuote("title", I18n.get("chartlabel.progress"));
		jsObject.putWithQuote("field", "progress");
		jsObject.putWithQuote("formatter", "progress");
		jsObject.putWithQuote("frozen", true);
		jsObject.put("formatterParams", getProgressParam(courseModules.size()));
		array.add(jsObject.toString());
		return array.toString();
	}

	private String getProgressParam(int max) {
		JSObject jsObject = new JSObject();
		jsObject.put("min", 0);
		jsObject.put("max", max);

		jsObject.put("legend", String
				.format("function(value){return value+'/'+%s +' ('+Math.round(value/%s*100||0)+'%%)';}", max, max));

		jsObject.putWithQuote("legendAlign", "center");
		JSArray jsArray = new JSArray();

		jsArray.add(colorToRGB(mainConfiguration.getValue(getChartType(), "firstInterval")));
		jsArray.add(colorToRGB(mainConfiguration.getValue(getChartType(), "secondInterval")));
		jsArray.add(colorToRGB(mainConfiguration.getValue(getChartType(), "thirdInterval")));
		jsArray.add(colorToRGB(mainConfiguration.getValue(getChartType(), "fourthInterval")));
		jsArray.add(colorToRGB(mainConfiguration.getValue(getChartType(), "moreMax")));
		jsObject.put("color",
				String.format(Locale.ROOT, "function(e){return %s[e/%f|0]}", jsArray.toString(), max / 4.0));
		return jsObject.toString();
	}

	public String createData(List<EnrolledUser> enrolledUsers, List<CourseModule> courseModules) {
		JSArray array = new JSArray();
		JSObject jsObject;
		Instant init = datePickerStart.getValue()
				.atStartOfDay(ZoneId.systemDefault())
				.toInstant();
		Instant end = datePickerEnd.getValue()
				.plusDays(1)
				.atStartOfDay(ZoneId.systemDefault())
				.toInstant();
		for (EnrolledUser enrolledUser : enrolledUsers) {
			jsObject = new JSObject();
			jsObject.putWithQuote("name", enrolledUser.getFullName());
			int progress = 0;
			for (CourseModule courseModule : courseModules) {
				ActivityCompletion activity = courseModule.getActivitiesCompletion()
						.get(enrolledUser);
				String field = "ID" + courseModule.getCmid();
				if (activity == null || activity.getState() == null) {
					jsObject.putWithQuote(field, "");
				} else {
					switch (activity.getState()) {
					case COMPLETE:
					case COMPLETE_PASS:
						Instant timeCompleted = activity.getTimecompleted();
						if (timeCompleted != null && init.isBefore(timeCompleted) && end.isAfter(timeCompleted)) {
							progress++;
							jsObject.putWithQuote(field, dateTimeWrapper.format(timeCompleted));
						}

						break;
					case COMPLETE_FAIL:
						jsObject.put(field, false);
						break;

					case INCOMPLETE:
						jsObject.putWithQuote(field, "");
						break;
					default:
						jsObject.putWithQuote(field, "");
						break;

					}

				}

			}

			jsObject.put("progress", progress);
			array.add(jsObject.toString());
		}
		return array.toString();
	}

	@Override
	public void update() {
		List<EnrolledUser> enrolledUsers = getSelectedEnrolledUser();

		List<CourseModule> courseModules = listViewActivity.getSelectionModel()
				.getSelectedItems();
		String columns = createColumns(courseModules);
		String tableData = createData(enrolledUsers, courseModules);
		JSObject data = new JSObject();
		data.put("columns", columns);
		data.put("tabledata", tableData);
		LOGGER.debug("Usuarios seleccionados:{}", enrolledUsers);
		LOGGER.debug("Columnas:{}", columns);
		LOGGER.debug("Datos de tabla:{}", data);
		webViewChartsEngine.executeScript(String.format("updateTabulator(%s, %s)", data, getOptions()));

	}

	@Override
	public String getOptions(JSObject jsObject) {

		jsObject.put("invalidOptionWarnings", false);
		jsObject.put("height", "height");
		jsObject.put("tooltipsHeader", true);
		jsObject.put("virtualDom", true);
		jsObject.putWithQuote("layout", "fitColumns");
		jsObject.put("rowClick", "function(e,row){javaConnector.dataPointSelection(row.getPosition());}");
		return jsObject.toString();
	}

	@Override
	public void exportCSV(String path) throws IOException {
		Instant init = datePickerStart.getValue()
				.atStartOfDay(ZoneId.systemDefault())
				.toInstant();
		Instant end = datePickerEnd.getValue()
				.plusDays(1)
				.atStartOfDay(ZoneId.systemDefault())
				.toInstant();
		List<EnrolledUser> enrolledUsers = getSelectedEnrolledUser();
		List<CourseModule> courseModules = listViewActivity
				.getSelectionModel()
				.getSelectedItems();
		List<String> header = new ArrayList<>();
		header.add("userid");
		header.add("fullname");
		for (CourseModule courseModule : courseModules) {
			header.add(courseModule.getModuleName());
			header.add("end date " + courseModule.getModuleName());
		}
		header.add("completed");
		header.add("nCourseModules");
		header.add("percentageCompleted");

		try (CSVPrinter printer = new CSVPrinter(getWritter(path),
				CSVFormat.DEFAULT.withHeader(header.toArray(new String[0])))) {
			for (EnrolledUser enrolledUser : enrolledUsers) {
				printer.print(enrolledUser.getId());
				printer.print(enrolledUser.getFullName());
				int completed = 0;
				for (CourseModule courseModule : courseModules) {

					ActivityCompletion activity = courseModule.getActivitiesCompletion()
							.get(enrolledUser);
					State state = activity.getState();
					Instant timeCompleted = activity.getTimecompleted();

					if ((state == ActivityCompletion.State.COMPLETE || state == ActivityCompletion.State.COMPLETE_PASS)
							&& timeCompleted != null && init.isBefore(timeCompleted) && end.isAfter(timeCompleted)) {
						++completed;
						printer.print(activity.getState()
								.ordinal());
						printer.print(dateTimeWrapper.format(timeCompleted));
					} else {
						printer.print(ActivityCompletion.State.INCOMPLETE.ordinal());
						printer.print(null);
					}

				}
				printer.print(completed);
				printer.print(courseModules.size());
				printer.print(completed / (double) courseModules.size() * 100);
				printer.println();

			}
		}

	}

}