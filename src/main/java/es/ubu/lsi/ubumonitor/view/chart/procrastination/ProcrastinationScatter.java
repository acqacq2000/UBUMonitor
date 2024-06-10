package es.ubu.lsi.ubumonitor.view.chart.procrastination;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.poifs.macros.Module;
import org.controlsfx.control.CheckComboBox;

import es.ubu.lsi.ubumonitor.controllers.Controller;
import es.ubu.lsi.ubumonitor.controllers.MainController;
import es.ubu.lsi.ubumonitor.model.ComponentEvent;
import es.ubu.lsi.ubumonitor.model.CourseModule;
import es.ubu.lsi.ubumonitor.model.EnrolledUser;
import es.ubu.lsi.ubumonitor.model.Event;
import es.ubu.lsi.ubumonitor.model.GradeItem;
import es.ubu.lsi.ubumonitor.model.LogLine;
import es.ubu.lsi.ubumonitor.model.ModuleType;
import es.ubu.lsi.ubumonitor.model.TryInformation;
import es.ubu.lsi.ubumonitor.util.I18n;
import es.ubu.lsi.ubumonitor.util.JSArray;
import es.ubu.lsi.ubumonitor.util.JSObject;
import es.ubu.lsi.ubumonitor.view.chart.ChartType;
import es.ubu.lsi.ubumonitor.view.chart.Plotly;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;

public class ProcrastinationScatter<E> extends Plotly {

	private Set<GradeItem> calificaciones;
	private List<CourseModule> modules;
	private ListView<CourseModule> listViewProcrastination;
	private CheckComboBox<ComponentEvent> listViewProcrastinationEvent;
	private ComboBox<String> listViewProcrastinationMetricMode;
	private ImageView imageEvents;
	private ImageView imageMetricMode;
	private List<EnrolledUser> users;
	private List<Event> events;
	
	//Para imprimir el CSV
	private List<ObjectCSVwithNote> objectsCSVwithNote;

	String unit;// Unidad de tiempo
	long maxTotalSeconds;// Tiempo mas largo medido en segundos

	public ProcrastinationScatter(MainController mainController, ListView<CourseModule> listViewProcrastination,
			CheckComboBox<ComponentEvent> listViewProcrastinationEvent,
			ComboBox<String> listViewProcrastinationMetricMode, ImageView imageEvents, ImageView imageMetricMode) {
		super(mainController, ChartType.PROCRASTINATION_SCATTER);

		this.listViewProcrastination = listViewProcrastination;
		this.listViewProcrastinationEvent = listViewProcrastinationEvent;
		this.listViewProcrastinationMetricMode = listViewProcrastinationMetricMode;
		this.imageEvents = imageEvents;
		this.imageMetricMode = imageMetricMode;

		// useRangeDate = true;
		useLegend = true;

	}

	@Override
	public void createData(JSArray data) {
		// Oculto el checkbox de eventos y su imagen y muestro el checkbox de metricas y
		// su imagen
		listViewProcrastinationEvent.setVisible(false);
		imageEvents.setVisible(false);
		listViewProcrastinationMetricMode.setVisible(true);
		imageMetricMode.setVisible(true);
		
		//Reseteo la lista para sacar el CSV
		objectsCSVwithNote = new ArrayList<>();

		this.calificaciones = actualCourse.getGradeItems();

		this.modules = new ArrayList<>(listViewProcrastination.getSelectionModel().getSelectedItems());
		if (modules.size() > 0 && modules.get(0) != null)
			modules = modules.stream().filter(module -> module.getTimeOpened() != null).collect(Collectors.toList());
		else
			modules.clear();

		this.events = TryInformation.EventProcrastincationEventsSubgroup;

		this.users = getSelectedEnrolledUser();

		//System.out.println("CONSTRUCTOR Events: " + events);

		List<TryInformation> tries = getData();
		// Ordenar los tries según el orden de los usuarios en la lista users y luego
		// dentro de cada user en orden de llegada de los eventos
		tries.sort(Comparator.comparing((TryInformation tryInfo) -> {
			EnrolledUser user = tryInfo.getUser();
			return users.indexOf(user); // Obtiene el índice del usuario en la lista users
		}).thenComparingLong(tryInfo -> tryInfo.getFechaSubida().toEpochSecond()));

		// Reinicio selector de unidades de tiempo
		tiempoMaximo(tries);

		//System.out.println("-----------------------------------------------------");
		for (TryInformation tri : tries)
			//System.out.println(tri);
		//System.out.println("-----------------------------------------------------");

		//System.out.println(maxTotalSeconds);
		if (maxTotalSeconds >= 86400) {
			unit = I18n.get("text.days");
		} else if (maxTotalSeconds >= 3600) {
			unit = I18n.get("text.hours");
		} else if (maxTotalSeconds >= 60) {
			unit = I18n.get("text.minutes");
		} else {
			unit = I18n.get("text.seconds");
		}

		for (CourseModule module : modules)
			data.add(createTrace(module, tries));
	}

	public List<TryInformation> getData() {
		List<TryInformation> tries = new ArrayList<>();

		List<LogLine> logLines = actualCourse.getLogs().getList();

		for (LogLine logLine : logLines) {
//        	//System.out.println("LogLine courseModule: " + logLine.getCourseModule() == null? logLine.getCourseModule().getModuleName():"");
//        	//System.out.println("LogLine event: " + logLine.getEventName() == null? logLine.getCourseModule().getModuleName():"");
//        	//System.out.println("LogLine user: " + logLine.getUser() == null? logLine.getCourseModule().getModuleName():"");
//        	//System.out.println("Lista courseModule: " + modules);
//        	//System.out.println("Lista events: " + events);
//        	//System.out.println("Lista user: " + users);
//        	if (logLine.getEventName().equals(Event.USER_GRADED)) //System.out.println("LogLine: " + logLine.toString());

			if (modules.contains(logLine.getCourseModule()) && events.contains(logLine.getEventName())
					&& users.contains(logLine.getUser())) {

				TryInformation intento = new TryInformation();
				intento.setCourseModule(logLine.getCourseModule());
				intento.setComponentEvent(logLine.getComponentEvent());
				intento.setUser(logLine.getUser());
				intento.setFechaSubida(logLine.getTime());
				tries.add(intento);
			}
		}
		return tries;
	}

	public void tiempoMaximo(List<TryInformation> tries) {

		//System.out.println("Entro a reiniciar maxTotalHeightSeconds");

		maxTotalSeconds = 0L;

		Map<Boolean, List<TryInformation>> partitionedTries = tries.stream().collect(Collectors
				.partitioningBy(tryInfo -> tryInfo.getCourseModule().getModuleType().equals(ModuleType.QUIZ)));

		List<TryInformation> quizTries = partitionedTries.get(true);
		List<TryInformation> assignmentsTries = partitionedTries.get(false);

		// Para los quizzes
		for (int i = 0; i < quizTries.size(); i++) {
			TryInformation tryInfo = quizTries.get(i);

			// Verificar si el evento es "eventoEmpezado"
			if (tryInfo.getComponentEvent().getEventName().equals(Event.QUIZ_ATTEMPT_STARTED)) {
				EnrolledUser user = tryInfo.getUser();

				// Buscar el evento "eventoTerminado" correspondiente para el mismo usuario
				for (int j = i + 1; j < quizTries.size(); j++) {
					TryInformation nextTryInfo = quizTries.get(j);

					if (nextTryInfo.getUser().equals(user) && (nextTryInfo.getComponentEvent().getEventName()
							.equals(Event.QUIZ_ATTEMPT_SUBMITTED)
							|| nextTryInfo.getComponentEvent().getEventName().equals(Event.QUIZ_ATTEMPT_ABANDONED))) {

						long tiempoTranscurridoDesdeApertura = tryInfo.getFechaSubida().toEpochSecond() - tryInfo.getCourseModule().getTimeOpened().getEpochSecond();					
						long tiempoTranscurrido = nextTryInfo.getFechaSubida().toEpochSecond() - tryInfo.getFechaSubida().toEpochSecond();
						
						if (listViewProcrastinationMetricMode.getValue().equalsIgnoreCase(I18n.get("combobox.measureOpening"))){
							//System.out.println("Seleccionado: Desde apertura");
							//System.out.println("Para el modulo " + tryInfo.getCourseModule() + " if: " + maxTotalSeconds + " < " + tiempoTranscurridoDesdeApertura);
							if (maxTotalSeconds < tiempoTranscurridoDesdeApertura)
								maxTotalSeconds = tiempoTranscurridoDesdeApertura;
							
						}else if (listViewProcrastinationMetricMode.getValue().equalsIgnoreCase(I18n.get("combobox.measureStartAttemp"))){
							//System.out.println("Seleccionado: Desde inicio de intento");
							//System.out.println("Para el modulo " + tryInfo.getCourseModule() + " if: " + maxTotalSeconds + " < " + tiempoTranscurrido);
							if (maxTotalSeconds < tiempoTranscurrido)
								maxTotalSeconds = tiempoTranscurrido;
							
						}

						break;
					}
				}
			}
		}

		// Para los assignments
		for (TryInformation tryInfo : assignmentsTries) {

			JSArray datos = new JSArray();

			// Obtener el usuario del intento
			EnrolledUser user = tryInfo.getUser();

			// Calcular la diferencia entre el tiempo de subida y el tiempo de apertura del
			// módulo
			long timeDifference = tryInfo.getFechaSubida().toEpochSecond() - tryInfo.getCourseModule().getTimeOpened().getEpochSecond();
			//System.out.println("Para el modulo " + tryInfo.getCourseModule() + " if: " + maxTotalSeconds + " < " + timeDifference);
			if (maxTotalSeconds < timeDifference)
				maxTotalSeconds = timeDifference;
		}

		//System.out.println("maxTotalHeightSeconds: " + maxTotalSeconds);
	}

	public JSObject createTrace(CourseModule module, List<TryInformation> tries) {
		// Crear el mapa que almacenará las diferencias de tiempo para cada usuario
		// Map<EnrolledUser, Set<Map.Entry<Long, Double>>> mapa = new HashMap<>();

		Map<EnrolledUser, Set<List<Object>>> tiempoIntentoCuestionario = new HashMap<>();

		JSArray x = new JSArray();
		JSArray y = new JSArray();
		JSArray z = new JSArray();
		JSArray registros = new JSArray();
		JSArray customdata = new JSArray();
		JSArray userIds = new JSArray();


		List<TryInformation> moduleTries = tries.stream().filter(tryInfo -> tryInfo.getCourseModule().equals(module))
				.collect(Collectors.toList());

		if (module.getModuleType().equals(ModuleType.QUIZ)) {
			// Iterar sobre cada intento en la lista
			for (int i = 0; i < moduleTries.size(); i++) {
				TryInformation tryInfo = moduleTries.get(i);

				// Verificar si el evento es "eventoEmpezado"
				if (tryInfo.getComponentEvent().getEventName().equals(Event.QUIZ_ATTEMPT_STARTED)) {
					EnrolledUser user = tryInfo.getUser();

					// Buscar el evento "eventoTerminado" correspondiente para el mismo usuario
					for (int j = i + 1; j < moduleTries.size(); j++) {
						TryInformation nextTryInfo = moduleTries.get(j);

						if (nextTryInfo.getUser().equals(user)
								&& (nextTryInfo.getComponentEvent().getEventName().equals(Event.QUIZ_ATTEMPT_SUBMITTED)
										|| nextTryInfo.getComponentEvent().getEventName()
												.equals(Event.QUIZ_ATTEMPT_ABANDONED))) {

							long tiempoTranscurridoDesdeApertura = tryInfo.getFechaSubida().toEpochSecond()
									- module.getTimeOpened().getEpochSecond();

							// Calcular la diferencia de tiempo entre los eventos
							long tiempoInicio = tryInfo.getFechaSubida().toEpochSecond();
							long tiempoFin = nextTryInfo.getFechaSubida().toEpochSecond();
							long tiempoTranscurrido = tiempoFin - tiempoInicio;
							//System.out.println("tiempoInicio: " + tiempoInicio);
							//System.out.println("tiempoFin: " + tiempoFin);
							//System.out.println("Tiempo transcurrido: " + tiempoTranscurrido);

							// Obtener la calificación del usuario
							GradeItem calificacion = calificaciones.stream().filter(
									gradeItem -> gradeItem.toString().equals(tryInfo.getCourseModule().getModuleName()))
									.findFirst().orElse(null);

							if (!Double.isNaN(calificacion.getEnrolledUserGrade(user))) {// Si es == entonces todavia no
																							// tiene nota
								double nota = calificacion.getEnrolledUserGrade(user);
								double notaMaxima = calificacion.getGrademax();
								double notaMinima = calificacion.getGrademin();
								// Crear la entrada y agregarla al mapa
								List<Object> entry = Arrays.asList(tiempoTranscurridoDesdeApertura, tiempoTranscurrido,
										nota, notaMaxima, notaMinima);
								
								//DESCOMENTAR SI QUEREMOS QUE SE RECOJAN TODOS LOS INTENTOS
								//tiempoIntentoCuestionario.computeIfAbsent(user, k -> new HashSet<>()).add(entry);
								//DESCOMENTAR SI QUEREMOS QUE SOLO SE RECOJA EL ULTIMO INTENTO
								tiempoIntentoCuestionario.put(user, new HashSet<>(Collections.singletonList(entry)));
								
								//Añado datos para el CSV
								objectsCSVwithNote.add(
										new ObjectCSVwithNote(
											user.getId(),
											user.getFullName(), 
											module.getModuleName(), 
											tiempoTranscurridoDesdeApertura, 
											tiempoTranscurrido, 
											nota
										)
								);
							}
							// Romper el ciclo interno una vez que se haya encontrado el evento
							// "eventoTerminado"
							break;
						}
					}
				}
			}

			//System.out.println("");
			
			JSArray datos = new JSArray();

			// Iterar las claves del mapa tiempoIntentoCuestionario
			for (EnrolledUser user : tiempoIntentoCuestionario.keySet()) {
				Set<List<Object>> intentos = tiempoIntentoCuestionario.get(user);

				// Procesar cada entrada de intentos
				for (List<Object> intento : intentos) {

					datos = new JSArray();
					long tiempoTranscurridoDesdeApertura = (Long) intento.get(0);
					long tiempoTranscurrido = (Long) intento.get(1);
					double nota = (Double) intento.get(2);
					double notaMaxima = (Double) intento.get(3);
					double notaMinima = (Double) intento.get(4);

					String tiempoFormateado = "";

					// Formatear tiempo
					long horasTotales = tiempoTranscurrido / 3600;
					long minutosTotales = (tiempoTranscurrido % 3600) / 60;
					long segundosTotales = tiempoTranscurrido % 60;

					if (horasTotales >= 24 || horasTotales <= -24) {
						long diasTotales = horasTotales / 24;
						horasTotales = horasTotales % 24;
						// datos.addWithQuote(String.format("%d días %02d:%02d:%02d", days, hours,
						// minutes, seconds));
						tiempoFormateado = String.format("%dd %02dh %02dm %02ds", diasTotales, horasTotales,
								minutosTotales, segundosTotales);

					} else {
						// datos.addWithQuote(String.format("%02d:%02d:%02d", hours, minutes, seconds));
						tiempoFormateado = String.format("%02dh %02dm %02ds", horasTotales, minutosTotales,
								segundosTotales);
					}

					double tiempoTranscurridoAdaptado = tiempoTranscurrido; // Inicialmente lo asignamos al mismo valor

					if (maxTotalSeconds >= 86400) {
						unit = I18n.get("text.days");
						// Convertir las alturas a días
						tiempoTranscurridoAdaptado = tiempoTranscurrido / 86400.0;
					} else if (maxTotalSeconds >= 3600) {
						unit = I18n.get("text.hours");
						// Convertir las alturas a horas
						tiempoTranscurridoAdaptado = tiempoTranscurrido / 3600.0;
					} else if (maxTotalSeconds >= 60) {
						unit = I18n.get("text.minutes");
						// Convertir las alturas a minutos
						tiempoTranscurridoAdaptado = tiempoTranscurrido / 60.0;
					} else {
						unit = I18n.get("text.seconds");
						tiempoTranscurridoAdaptado = (double) tiempoTranscurrido;
					}
					
					String tiempoFormateadoDesdeApertura = "";

					long horasTotalesDesdeApertura = tiempoTranscurridoDesdeApertura / 3600;
					long minutosTotalesDesdeApertura = (tiempoTranscurridoDesdeApertura % 3600) / 60;
					long segundosTotalesDesdeApertura = tiempoTranscurridoDesdeApertura % 60;

					if (horasTotalesDesdeApertura >= 24 || horasTotalesDesdeApertura <= -24) {
						long diasTotalesDesdeApertura = horasTotalesDesdeApertura / 24;
						horasTotalesDesdeApertura = horasTotalesDesdeApertura % 24;
						// datos.addWithQuote(String.format("%d días %02d:%02d:%02d", days, hours,
						// minutes, seconds));
						tiempoFormateadoDesdeApertura = String.format("%dd %02dh %02dm %02ds", diasTotalesDesdeApertura,
								horasTotalesDesdeApertura, minutosTotalesDesdeApertura, segundosTotalesDesdeApertura);

					} else {
						// datos.addWithQuote(String.format("%02d:%02d:%02d", hours, minutes, seconds));
						tiempoFormateadoDesdeApertura = String.format("%02dh %02dm %02ds", horasTotalesDesdeApertura,
								minutosTotalesDesdeApertura, segundosTotalesDesdeApertura);
					}

					double tiempoTranscurridoAdaptadoDesdeApertura = tiempoTranscurridoDesdeApertura; // Inicialmente lo
																										// asignamos al
																										// mismo valor

					if (maxTotalSeconds >= 86400) {
						unit = I18n.get("text.days");
						// Convertir las alturas a días
						tiempoTranscurridoAdaptadoDesdeApertura = tiempoTranscurridoDesdeApertura / 86400.0;
					} else if (maxTotalSeconds >= 3600) {
						unit = I18n.get("text.hours");
						// Convertir las alturas a horas
						tiempoTranscurridoAdaptadoDesdeApertura = tiempoTranscurridoDesdeApertura / 3600.0;
					} else if (maxTotalSeconds >= 60) {
						unit = I18n.get("text.minutes");
						// Convertir las alturas a minutos
						tiempoTranscurridoAdaptadoDesdeApertura = tiempoTranscurridoDesdeApertura / 60.0;
					} else {
						unit = I18n.get("text.seconds");
						tiempoTranscurridoAdaptadoDesdeApertura = (double) tiempoTranscurridoDesdeApertura;
					}

					// Ahora tiempoTranscurridoAdaptado contiene el valor convertido correctamente
					// en la unidad apropiada
					//System.out.println("Tiempo transcurrido: " + tiempoTranscurridoAdaptadoDesdeApertura + " " + unit);

					if (listViewProcrastinationMetricMode.getValue().equalsIgnoreCase(I18n.get("combobox.measureOpening"))){
						//System.out.println("Seleccionado: Desde apertura");
						z.addWithQuote(tiempoTranscurridoAdaptado);
						x.addWithQuote(tiempoTranscurridoAdaptadoDesdeApertura);
					}else if (listViewProcrastinationMetricMode.getValue().equalsIgnoreCase(I18n.get("combobox.measureStartAttemp"))){
						//System.out.println("Seleccionado: Desde inicio de intento");
						x.addWithQuote(tiempoTranscurridoAdaptado);
						z.addWithQuote(tiempoTranscurridoAdaptadoDesdeApertura);
					}
					y.addWithQuote(nota);
					

					// Agregar elementos a datos
					datos.addWithQuote(tiempoFormateadoDesdeApertura);
					datos.addWithQuote(tiempoFormateado);
					datos.addWithQuote(user.getFullName());
					datos.addWithQuote(module.getModuleName());
					datos.addWithQuote(nota);
					datos.addWithQuote(notaMinima);
					datos.addWithQuote(notaMaxima);
					customdata.add(datos);
					
					userIds.add(user.getId());
					//System.out.println("CustomData: " + customdata);

					registros
					.addWithQuote("<b>" + I18n.get("text.elapsedTimeTaskOpening") + ":</b> <br> %{customdata[0]} <br><br>"
							+ "<b>" + I18n.get("text.elapsedTimeStartAttemp") + ":</b> <br> %{customdata[1]} <br><br>"
							+ "<b>" + I18n.get("text.student") + ":</b> <br> %{customdata[2]} <br><br>"
							+ "<b>" + I18n.get("text.module") + ":</b> <br> %{customdata[3]} <br><br>"
							+ "<b>" + I18n.get("text.grade") + ":</b> <br> %{customdata[4]} <br><br>"
							+ "<b>" + I18n.get("text.rangeGrade") + ":</b> <br> [%{customdata[5]},%{customdata[6]}] <br><br>"
							+ "<extra></extra>");
				}

			}
		} else if (module.getModuleType().equals(ModuleType.ASSIGNMENT)) {
			
			for (TryInformation tryInfo : moduleTries) {

				JSArray datos = new JSArray();

				// Obtener el usuario del intento
				EnrolledUser user = tryInfo.getUser();

				// Calcular la diferencia entre el tiempo de subida y el tiempo de apertura del
				// módulo
				long timeDifference = tryInfo.getFechaSubida().toEpochSecond()
						- module.getTimeOpened().getEpochSecond();

				GradeItem calificacion = calificaciones.stream()
						.filter(gradeItem -> gradeItem.toString().equals(tryInfo.getCourseModule().getModuleName()))
						.findFirst().orElse(null);

				// Agregar la diferencia al mapa
				double nota = Double.NaN;
				if (calificacion != null)
					nota = calificacion.getEnrolledUserGrade(user);
				
				if (!Double.isNaN(nota)) {// Si es == entonces todavia no tiene nota
					//System.out.println("Nota: " + nota);
					Map.Entry<Long, Double> timeAndGrade = new AbstractMap.SimpleEntry<>(timeDifference, nota);

					objectsCSVwithNote.add(
							new ObjectCSVwithNote(
								user.getId(),
								user.getFullName(), 
								module.getModuleName(), 
								timeDifference, 
								timeDifference, 
								nota
							)
					);
					//System.out.println(objectsCSVwithNote.get(objectsCSVwithNote.size() - 1));
					// Agregar el par al mapa
					// mapa.computeIfAbsent(user, k -> new HashSet<>()).add(timeAndGrade);

					String tiempoFormateado = "";

					// Formatear tiempo
					long horasTotales = timeAndGrade.getKey() / 3600;
					long minutosTotales = (timeAndGrade.getKey() % 3600) / 60;
					long segundosTotales = timeAndGrade.getKey() % 60;

					if (horasTotales >= 24 || horasTotales <= -24) {
						long diasTotales = horasTotales / 24;
						horasTotales = horasTotales % 24;
						// datos.addWithQuote(String.format("%d días %02d:%02d:%02d", days, hours,
						// minutes, seconds));
						tiempoFormateado = String.format("%dd %02dh %02dm %02ds", diasTotales, horasTotales,
								minutosTotales, segundosTotales);

					} else {
						// datos.addWithQuote(String.format("%02d:%02d:%02d", hours, minutes, seconds));
						tiempoFormateado = String.format("%02dh %02dm %02ds", horasTotales, minutosTotales,
								segundosTotales);
					}

					double tiempoTranscurridoAdaptado = timeDifference; // Inicialmente lo asignamos al mismo valor

					if (maxTotalSeconds >= 86400) {
						unit = I18n.get("text.days");
						// Convertir las alturas a días
						tiempoTranscurridoAdaptado = timeDifference / 86400.0;
					} else if (maxTotalSeconds >= 3600) {
						unit = I18n.get("text.hours");
						// Convertir las alturas a horas
						tiempoTranscurridoAdaptado = timeDifference / 3600.0;
					} else if (maxTotalSeconds >= 60) {
						unit = I18n.get("text.minutes");
						// Convertir las alturas a minutos
						tiempoTranscurridoAdaptado = timeDifference / 60.0;
					} else {
						unit = I18n.get("text.seconds");
						tiempoTranscurridoAdaptado = (double) timeDifference;
					}

					// Ahora tiempoTranscurridoAdaptado contiene el valor convertido correctamente
					// en la unidad apropiada
					//System.out.println("Tiempo transcurrido: " + tiempoTranscurridoAdaptado + " " + unit);

					x.addWithQuote(tiempoTranscurridoAdaptado);
					y.addWithQuote(timeAndGrade.getValue());

					// Agregar elementos a datos
					datos.addWithQuote(tiempoFormateado);
					datos.addWithQuote(user.getFullName());
					datos.addWithQuote(module.getModuleName());
					datos.addWithQuote(timeAndGrade.getValue());
					datos.addWithQuote(calificacion.getGrademin());
					datos.addWithQuote(calificacion.getGrademax());
					customdata.add(datos);
					
					userIds.add(user.getId());
					
					registros
							.addWithQuote("<b>" + I18n.get("text.elapsedTimeTaskOpening") + ":</b> <br> %{customdata[0]} <br><br>"
									+ "<b>" + I18n.get("text.elapsedTimeStartAttemp") + ":</b> <br> %{customdata[0]} <i>" + I18n.get("text.startAttempAndTaskOpeningEquals") + "</i><br><br>"
									+ "<b>" + I18n.get("text.student") + ":</b> <br> %{customdata[1]} <br><br>"
									+ "<b>" + I18n.get("text.module") + ":</b> <br> %{customdata[2]} <br><br>"
									+ "<b>" + I18n.get("text.grade") + ":</b> <br> %{customdata[3]} <br><br>"
									+ "<b>" + I18n.get("text.rangeGrade") + ":</b> <br> [%{customdata[4]},%{customdata[5]}] <br><br>"
									+ "<extra></extra>");

				}

			}
		}

		// //System.out.println("Gradeitems: " + calificaciones);

		

		JSObject trace = new JSObject();

		JSObject marker = new JSObject();
		marker.put("color", rgb(module));
		marker.put("size", 10);

		trace.putWithQuote("name", "<b>" + module.getModuleName() + "</b> <br>");
		trace.put("type", "'scatter'");
		trace.put("mode", "'markers'");
		trace.put("marker", marker);

		trace.put("x", x);
		trace.put("y", y);
		trace.put("userids", userIds);
		trace.put("customdata", customdata);
		trace.put("hovertemplate", registros);

		//System.out.println("TRACE: " + trace);
		
		return trace;
	}
	
	
	@Override
	public void createLayout(JSObject layout) {
	    JSObject xaxis = new JSObject();
	    defaultAxisValues(xaxis, getXAxisTitle(), null);
	    //xaxis.put("rangemode", "'tozero'"); // Asegura que el eje X empiece desde 0

	    JSObject yaxis = new JSObject();
	    defaultAxisValues(yaxis, getYAxisTitle(), null);
	    yaxis.put("rangemode", "'tozero'"); // Asegura que el eje Y empiece desde 0

	    layout.put("xaxis", xaxis);
	    layout.put("yaxis", yaxis);
	}

	@Override
	public String getYAxisTitle() {
		
		return "<b>" + I18n.get("text.gradeObtained") + "</b>";
	}

	@Override
	public String getXAxisTitle() {

		//System.out.println(maxTotalSeconds);
		if (maxTotalSeconds >= 86400) {
			unit = I18n.get("text.days");
		} else if (maxTotalSeconds >= 3600) {
			unit = I18n.get("text.hours");
		} else if (maxTotalSeconds >= 60) {
			unit = I18n.get("text.minutes");
		} else {
			unit = I18n.get("text.seconds");
		}
		
		String desde = "";
		
		if (listViewProcrastinationMetricMode.getValue().equalsIgnoreCase(I18n.get("combobox.measureOpening"))){
			//System.out.println("Seleccionado: Desde apertura");
			desde = I18n.get("text.measureOpening");
		}else if (listViewProcrastinationMetricMode.getValue().equalsIgnoreCase(I18n.get("combobox.measureStartAttemp"))){
			//System.out.println("Seleccionado: Desde inicio de intento");
			desde = I18n.get("text.measureStartAttemp");
		}

		if (maxTotalSeconds > 0)
			return "<b>" + I18n.get("text.procrastinationTimeIn") + unit.toUpperCase() + " (" + desde + ")</b>";
		else
			return "<b>" + I18n.get("text.procrastinationTime") + "</b>";
	}
	
	@Override
	public void exportCSV(String path) throws IOException {
	    // Map para almacenar módulos y sus respectivos eventos únicos
	    Map<String, Set<String>> moduloMedidaTiempo = new LinkedHashMap<>();
	    for (ObjectCSVwithNote obj : objectsCSVwithNote) {
	        moduloMedidaTiempo
	            .computeIfAbsent(obj.getNombreModulo(), k -> new LinkedHashSet<>())
	            .add(I18n.get("text.timeSinceOpen"));
	        moduloMedidaTiempo
            .computeIfAbsent(obj.getNombreModulo(), k -> new LinkedHashSet<>())
            .add(I18n.get("text.timeSinceOpen") + " (" + I18n.get("text.formatted") + ")");
	        moduloMedidaTiempo
            .computeIfAbsent(obj.getNombreModulo(), k -> new LinkedHashSet<>())
            .add(I18n.get("text.timeSinceStartAttemp"));
	        moduloMedidaTiempo
            .computeIfAbsent(obj.getNombreModulo(), k -> new LinkedHashSet<>())
            .add(I18n.get("text.timeSinceStartAttemp") + " (" + I18n.get("text.formatted") + ")");
	    }

	    // Crear la lista de cabeceras incluyendo intentos
	    List<String> cabeceras = new ArrayList<>();
		cabeceras.add(I18n.get("text.idStudent"));
		cabeceras.add(I18n.get("text.student"));

	    /*
	    // Crear un map para almacenar el máximo número de intentos por módulo
	    Map<String, Integer> maxIntentosPorModulo = new LinkedHashMap<>();
	    for (ObjectCSVwithNote obj : objectsCSVwithNote) {
	        String modulo = obj.getNombreModulo();
	        maxIntentosPorModulo.put(modulo, maxIntentosPorModulo.getOrDefault(modulo, 0) + 1);
	    }
	    */

	    // Definir las cabeceras para cada intento por módulo
	    for (Map.Entry<String, Set<String>> entry : moduloMedidaTiempo.entrySet()) {
	        String modulo = entry.getKey();
	        /*
	        int maxIntentos = maxIntentosPorModulo.get(modulo);
	        for (int i = 1; i <= maxIntentos; i++) {
	            cabeceras.add("Intento " + i);
	            for (String medida : entry.getValue()) {
	                cabeceras.add(medida);
	            }
	        }
	        
	        cabeceras.add("Intento");
	        */
            for (String medida : entry.getValue()) cabeceras.add("(" + modulo + ") - " + medida);
            cabeceras.add("(" + modulo + ") - " + I18n.get("text.grade"));
	    }
	    
	    Map<String, List<String>> data = new LinkedHashMap<>();
	    
	    for (ObjectCSVwithNote obj : objectsCSVwithNote) data.putIfAbsent(obj.getNombreUsuario(), new ArrayList<>(Collections.nCopies(cabeceras.size(), "")));
	    //System.out.println("data: " + data);
	    for (ObjectCSVwithNote obj : objectsCSVwithNote) {
			data.get(obj.getNombreUsuario()).set(0, String.valueOf(obj.idUsuario));
			data.get(obj.getNombreUsuario()).set(1, obj.nombreUsuario);
	    }
	    
	    for (ObjectCSVwithNote obj : objectsCSVwithNote) {
	        String alumno = obj.getNombreUsuario();
	        String modulo = obj.getNombreModulo();
	        //int intento = obj.getNumeroIntento();
	        long tiempoDesdeApertura = obj.getTiempoTranscurridoDesdeApertura();
	        long tiempoDesdeInicioIntento = obj.getTiempoTranscurridoDesdeInicioIntento();
	        String tiempoTranscurridoDesdeAperturaFormateado = obj.getTiempoTranscurridoDesdeAperturaFormateado();
	        String tiempoDesdeInicioIntentoFormateado = obj.getTiempoTranscurridoDesdeInicioIntentoFormateado();
	        double nota = obj.getNota();
	        //Solo está diseñado para un solo intento de momento
	        for (int indice = 2; indice < cabeceras.size(); indice++) {
	        	//System.out.println("indice: " + cabeceras.get(indice) + ", modulo: " + modulo);
	        	//La idea sería preguntar si el siguiente indice al de intento X esta vacio poner las tres celdas y si no iterar otros 3 indices y volver a probar y así...
	        	if (cabeceras.get(indice).split(" - ")[0].equalsIgnoreCase("(" + modulo + ")")) {
    				data.get(alumno).set(indice, String.valueOf(tiempoDesdeApertura));
    				indice ++;
    				data.get(alumno).set(indice, tiempoTranscurridoDesdeAperturaFormateado);
    				indice ++;
    				data.get(alumno).set(indice, String.valueOf(tiempoDesdeInicioIntento));
    				indice ++;
    				data.get(alumno).set(indice, tiempoDesdeInicioIntentoFormateado);
    				indice ++;
    				data.get(alumno).set(indice, String.valueOf(nota));
	        		break;
	        	}
	        }
	    }
	    
	    //Así ordena alfabéticamente los nombres de los alumnos
  		data = new LinkedHashMap<>(new TreeMap<>(data));

	    // Escribir el archivo CSV con CSVPrinter
	    try (CSVPrinter printer = new CSVPrinter(getWritter(path),
	            CSVFormat.DEFAULT.withHeader(cabeceras.toArray(new String[0])))) {
	    	for(List<String> fila: data.values()) {
	    		//System.out.println(fila);
	    		printer.printRecord(fila);
	    	} 
	    	
	    }
	}
	
	public String getString(String string) {
		return I18n.get("procrastination." + string);
	}
}

class ObjectCSVwithNote {

	long tiempoTranscurridoDesdeApertura;
	String tiempoTranscurridoDesdeAperturaFormateado;
	
	long tiempoTranscurridoDesdeInicioIntento;
	String tiempoTranscurridoDesdeInicioIntentoFormateado;
	
	String nombreUsuario;
	String nombreModulo;
	
	double nota;
	int idUsuario;
	
	//Constructor, getter & setter, toString()
	
	public ObjectCSVwithNote(
			int idUsuario,
			String nombreUsuario, 
			String nombreModulo, 
			long tiempoTranscurridoDesdeApertura, 
			long tiempoTranscurridoDesdeInicioIntento,
			double nota) {
		
		this.idUsuario = idUsuario;
		this.nombreUsuario = nombreUsuario;
		this.nombreModulo = nombreModulo;
		this.tiempoTranscurridoDesdeApertura = tiempoTranscurridoDesdeApertura;
		this.tiempoTranscurridoDesdeInicioIntento = tiempoTranscurridoDesdeInicioIntento;
		this.nota = nota;
		
		this.tiempoTranscurridoDesdeAperturaFormateado = formatearSegundos(this.tiempoTranscurridoDesdeApertura);
		this.tiempoTranscurridoDesdeInicioIntentoFormateado = formatearSegundos(this.tiempoTranscurridoDesdeInicioIntento);
	}

	
	
	public long getTiempoTranscurridoDesdeApertura() {
		return tiempoTranscurridoDesdeApertura;
	}



	public String getTiempoTranscurridoDesdeAperturaFormateado() {
		return tiempoTranscurridoDesdeAperturaFormateado;
	}



	public long getTiempoTranscurridoDesdeInicioIntento() {
		return tiempoTranscurridoDesdeInicioIntento;
	}



	public String getTiempoTranscurridoDesdeInicioIntentoFormateado() {
		return tiempoTranscurridoDesdeInicioIntentoFormateado;
	}



	public String getNombreUsuario() {
		return nombreUsuario;
	}



	public String getNombreModulo() {
		return nombreModulo;
	}



	public double getNota() {
		return nota;
	}
	
	public int getIdUsuario() {
		return idUsuario;
	}

	@Override
	public String toString() {
		return "ObjectCSV [tiempoTranscurridoDesdeApertura=" + tiempoTranscurridoDesdeApertura
				+ ", tiempoTranscurridoDesdeAperturaFormateado=" + tiempoTranscurridoDesdeAperturaFormateado
				+ ", tiempoTranscurridoDesdeInicioIntento=" + tiempoTranscurridoDesdeInicioIntento
				+ ", tiempoTranscurridoDesdeInicioIntentoFormateado=" + tiempoTranscurridoDesdeInicioIntentoFormateado
				+ ", nombreUsuario=" + nombreUsuario + ", nombreModulo=" + nombreModulo + ", nota=" + nota + "]";
	}

	private String formatearSegundos(long segundos) {
		// Convertir segundos a horas, minutos y segundos
        long horasTotales = segundos / 3600;
        long minutosTotales = (segundos % 3600) / 60;
        long segundosTotales = segundos % 60;

        if (horasTotales >= 24 || horasTotales <= -24) {
            long diasTotales = horasTotales / 24;
            horasTotales = horasTotales % 24;
            return String.format("%dd %02dh %02dm %02ds", diasTotales, horasTotales, minutosTotales, segundosTotales);            
        } else {
        	return String.format("%02dh %02dm %02ds", horasTotales, minutosTotales, segundosTotales);
        }
	}
}
