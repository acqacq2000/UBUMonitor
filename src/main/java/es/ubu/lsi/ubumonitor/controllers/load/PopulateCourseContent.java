package es.ubu.lsi.ubumonitor.controllers.load;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import es.ubu.lsi.ubumonitor.model.CourseModule;
import es.ubu.lsi.ubumonitor.model.DataBase;
import es.ubu.lsi.ubumonitor.model.DescriptionFormat;
import es.ubu.lsi.ubumonitor.model.ModuleType;
import es.ubu.lsi.ubumonitor.model.Section;
import es.ubu.lsi.ubumonitor.model.TryInformation;
import es.ubu.lsi.ubumonitor.model.log.logtypes.Course;
import es.ubu.lsi.ubumonitor.util.UtilMethods;
import es.ubu.lsi.ubumonitor.webservice.api.core.course.CoreCourseGetContents;
import es.ubu.lsi.ubumonitor.webservice.webservices.WebService;
import javafx.util.Pair;

public class PopulateCourseContent {
	private WebService webService;
	private DataBase dataBase;


	public PopulateCourseContent(WebService webService, DataBase dataBase) {
		this.webService = webService;
		this.dataBase = dataBase;
		
	}

	public Pair<List<Section>, List<CourseModule>> populateCourseContent(int courseid) {

		try {
			CoreCourseGetContents coreCourseGetContents = new CoreCourseGetContents(courseid);
			coreCourseGetContents.appendExcludecontents(true);
			JSONArray jsonArray = UtilMethods.getJSONArrayResponse(webService, coreCourseGetContents);
			return populateCourseContent(jsonArray);
		} catch (Exception e) {
			return new Pair<>(Collections.emptyList(), Collections.emptyList());
		}
	}

	public  Pair<List<Section>, List<CourseModule>> populateCourseContent(JSONArray jsonArray) {
		List<Section> sections = new ArrayList<>();
		List<CourseModule> courseModules = new ArrayList<>();
		for (int i = 0; i < jsonArray.length(); ++i) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			Section section =populateSection(jsonObject);
			sections.add(section);
			JSONArray modules = jsonObject.getJSONArray(Constants.MODULES);
			for (int j = 0; j < modules.length(); ++j) {
				CourseModule courseModule = populateCourseModule(modules.getJSONObject(j));
				courseModule.setSection(section);
				courseModules.add(courseModule);
			}
		}
		return new Pair<>(sections, courseModules);
	}

	private Section populateSection(JSONObject jsonObject) {
		Section section = dataBase.getSections()
				.getById(jsonObject.getInt(Constants.ID));
		section.setName(jsonObject.optString(Constants.NAME));
		section.setVisible(jsonObject.optInt(Constants.VISIBLE) == 1);
		section.setSummary(jsonObject.optString(Constants.SUMMARY));
		section.setSummaryformat(DescriptionFormat.get(jsonObject.optInt(Constants.SUMMARYFORMAT)));
		section.setHiddenbynumsections(jsonObject.optInt(Constants.HIDDENBYNUMSECTIONS));
		section.setUservisible(jsonObject.optBoolean(Constants.USERVISIBLE));

		return section;

	}

	private CourseModule populateCourseModule(JSONObject jsonObject) {
		//System.out.println("entro en el populat course module" + jsonObject.optInt(Constants.DUEDATE));
		//System.out.println("JSON\n" + jsonObject.toString(6));
		//System.out.println("get type " + module.getModuleType());

		CourseModule module = dataBase.getModules()
				.getById(jsonObject.getInt(Constants.ID));
		
		module.setUrl(jsonObject.optString(Constants.URL));
		module.setModuleName(jsonObject.optString(Constants.NAME));
		module.setInstance(jsonObject.optInt(Constants.INSTANCE));
		module.setVisible(jsonObject.optInt(Constants.VISIBLE) == 1);
		module.setUservisible(jsonObject.optBoolean(Constants.USERVISIBLE));
		module.setVisibleoncoursepage(jsonObject.optInt(Constants.VISIBLEONCOURSEPAGE) == 1);
		module.setModicon(jsonObject.optString(Constants.MODICON));
		module.setModuleType(ModuleType.get(jsonObject.optString(Constants.MODNAME)));
		module.setModplural(jsonObject.optString(Constants.MODPLURAL));
		module.setIndent(jsonObject.optInt(Constants.INDENT));

		// Added
		if (TryInformation.EventProcrastincationModuleTypesSubgroup.contains(module.getModuleType())) {
			System.out.println("get type " + module.getModuleType());
			System.out.println("JSON\n" + jsonObject.toString(6));

			try {

				if (module.getModuleType().equals(ModuleType.ASSIGNMENT)) {

					if (jsonObject.getJSONArray("dates").length() == 1) {

						if (jsonObject.getJSONArray("dates").getJSONObject(0).getString("dataid")
								.equalsIgnoreCase("allowsubmissionsfromdate")) {

							module.setTimeOpened(Instant.ofEpochSecond(
									jsonObject.getJSONArray("dates").getJSONObject(0).getLong("timestamp")));

						} else if (jsonObject.getJSONArray("dates").getJSONObject(0).getString("dataid")
								.equalsIgnoreCase("duedate")) {

							module.setTimeDue(Instant.ofEpochSecond(
									jsonObject.getJSONArray("dates").getJSONObject(0).getLong("timestamp")));
						}
					} else if (jsonObject.getJSONArray("dates").length() == 2) {
						module.setTimeOpened(Instant
								.ofEpochSecond(jsonObject.getJSONArray("dates").getJSONObject(0).getLong("timestamp")));
						module.setTimeDue(Instant
								.ofEpochSecond(jsonObject.getJSONArray("dates").getJSONObject(1).getLong("timestamp")));
					}

				} else if (module.getModuleType().equals(ModuleType.QUIZ)) {
					
					if (jsonObject.getJSONArray("dates").length() == 1) {

						if (jsonObject.getJSONArray("dates").getJSONObject(0).getString("dataid")
								.equalsIgnoreCase("timeopen")) {

							module.setTimeOpened(Instant.ofEpochSecond(
									jsonObject.getJSONArray("dates").getJSONObject(0).getLong("timestamp")));

						} else if (jsonObject.getJSONArray("dates").getJSONObject(0).getString("dataid")
								.equalsIgnoreCase("timeclose")) {

							module.setTimeDue(Instant.ofEpochSecond(
									jsonObject.getJSONArray("dates").getJSONObject(0).getLong("timestamp")));
						}
					} else if (jsonObject.getJSONArray("dates").length() == 2) {
						module.setTimeOpened(Instant
								.ofEpochSecond(jsonObject.getJSONArray("dates").getJSONObject(0).getLong("timestamp")));
						module.setTimeDue(Instant
								.ofEpochSecond(jsonObject.getJSONArray("dates").getJSONObject(1).getLong("timestamp")));
					}
				}

			} catch (JSONException e) {System.err.print("Error capturando fechas --> " + e.getStackTrace());}

			System.out.println("La tarea " + module.getModuleName() + " abrió el " + module.getTimeOpened()
			+ " y cerró el " + module.getTimeDue());

		}

		return module;
	}
}
