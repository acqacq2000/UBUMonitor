package es.ubu.lsi.ubumonitor.view.chart.procrastination;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.poifs.macros.Module;
import org.controlsfx.control.CheckComboBox;

import es.ubu.lsi.ubumonitor.controllers.MainController;
import es.ubu.lsi.ubumonitor.model.ComponentEvent;
import es.ubu.lsi.ubumonitor.model.CourseModule;
import es.ubu.lsi.ubumonitor.model.EnrolledUser;
import es.ubu.lsi.ubumonitor.model.Event;
import es.ubu.lsi.ubumonitor.model.GradeItem;
import es.ubu.lsi.ubumonitor.model.LogLine;
import es.ubu.lsi.ubumonitor.model.ModuleType;
import es.ubu.lsi.ubumonitor.model.TryInformation;
import es.ubu.lsi.ubumonitor.util.JSArray;
import es.ubu.lsi.ubumonitor.util.JSObject;
import es.ubu.lsi.ubumonitor.view.chart.ChartType;
import es.ubu.lsi.ubumonitor.view.chart.Plotly;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeView;

public class ProcrastinationScatter<E> extends Plotly{

	private Set<GradeItem> calificaciones;
	private List<CourseModule> modules;
    private ListView<CourseModule> listViewProcrastination;
    private CheckComboBox<ComponentEvent> listViewProcrastinationEvent;
	private List<EnrolledUser> users;
	private List<Event> events;
	
	String unit;//Unidad de tiempo
    long maxTotalHeightSeconds;//Tiempo mas largo medido en segundos
	
	public ProcrastinationScatter(MainController mainController, ListView<CourseModule> listViewProcrastination, CheckComboBox<ComponentEvent> listViewProcrastinationEvent) {
		super(mainController, ChartType.PROCRASTINATION_SCATTER);
		this.listViewProcrastination = listViewProcrastination;
		this.listViewProcrastinationEvent = listViewProcrastinationEvent;
		
	}

	@Override
	public void exportCSV(String path) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createData(JSArray data) {
		// TODO Auto-generated method stu
		
		System.out.println("Deshabilito el checkbox");
		listViewProcrastinationEvent.setDisable(true);
		
		this.calificaciones = actualCourse.getGradeItems();
		
		this.modules = new ArrayList<>(listViewProcrastination.getSelectionModel().getSelectedItems());
        if (modules.size() > 0 && modules.get(0) != null) 
        	modules = modules.stream().filter(module -> module.getTimeOpened() != null).collect(Collectors.toList());
        else modules.clear();

        this.events = TryInformation.EventProcrastincationEventsSubgroup;
        
		this.users = getSelectedEnrolledUser();
		
		System.out.println("CONSTRUCTOR Events: " + events);
		
		
		List<TryInformation> tries = getData();
		// Ordenar los tries según el orden de los usuarios en la lista users y luego dentro de cada user en orden de llegada de los eventos
        tries.sort(Comparator.comparing((TryInformation tryInfo) -> {
            EnrolledUser user = tryInfo.user;
            return users.indexOf(user); // Obtiene el índice del usuario en la lista users
        }).thenComparingLong(tryInfo -> tryInfo.getFechaSubida().toEpochSecond()));
        
        //Reinicio selector de unidades de tiempo
      	tiempoMaximo(tries);
        
        System.out.println("-----------------------------------------------------");
        for (TryInformation tri: tries) System.out.println(tri);
        System.out.println("-----------------------------------------------------");
        
        System.out.println(maxTotalHeightSeconds);
        if (maxTotalHeightSeconds >= 86400) {
            unit = "Dias";
        } else if (maxTotalHeightSeconds >= 3600) {
            unit = "Horas";
        } else if (maxTotalHeightSeconds >= 60) {
            unit = "Minutos";
        } else {
            unit = "Segundos";
        }
        
        for (CourseModule module : modules) 
    		data.add(createTrace(module, tries));
	}
	
	public List<TryInformation> getData() {
        List<TryInformation> tries = new ArrayList<>();

        List<LogLine> logLines = actualCourse.getLogs().getList();

        for (LogLine logLine : logLines) {
//        	System.out.println("LogLine courseModule: " + logLine.getCourseModule() == null? logLine.getCourseModule().getModuleName():"");
//        	System.out.println("LogLine event: " + logLine.getEventName() == null? logLine.getCourseModule().getModuleName():"");
//        	System.out.println("LogLine user: " + logLine.getUser() == null? logLine.getCourseModule().getModuleName():"");
//        	System.out.println("Lista courseModule: " + modules);
//        	System.out.println("Lista events: " + events);
//        	System.out.println("Lista user: " + users);
//        	if (logLine.getEventName().equals(Event.USER_GRADED)) System.out.println("LogLine: " + logLine.toString());

            if (modules.contains(logLine.getCourseModule()) && 
            		events.contains(logLine.getEventName()) && 
            		users.contains(logLine.getUser())) {
            	
                TryInformation intento = new TryInformation();
                intento.courseModule = logLine.getCourseModule();
                intento.componentEvent = logLine.getComponentEvent();
                intento.user = logLine.getUser();
                intento.fechaSubida = logLine.getTime();
                tries.add(intento);
            }
        }
        return tries;
    }
	
public void tiempoMaximo (List<TryInformation> tries) {
	
	System.out.println("Entro a reiniciar maxTotalHeightSeconds");
	
	maxTotalHeightSeconds = 0L;
	
	Map<Boolean, List<TryInformation>> partitionedTries = tries.stream()
		    .collect(Collectors.partitioningBy(tryInfo -> 
		        tryInfo.getCourseModule().getModuleType().equals(ModuleType.QUIZ)
		    ));

	List<TryInformation> quizTries = partitionedTries.get(true);
	List<TryInformation> assignmentsTries = partitionedTries.get(false);

	//Para los quizzes
	for (int i = 0; i < quizTries.size(); i++) {
		TryInformation tryInfo = quizTries.get(i);

		// Verificar si el evento es "eventoEmpezado"
		if (tryInfo.getComponentEvent().getEventName().equals(Event.QUIZ_ATTEMPT_STARTED)) {
			EnrolledUser user = tryInfo.getUser();

			// Buscar el evento "eventoTerminado" correspondiente para el mismo usuario
			for (int j = i + 1; j < quizTries.size(); j++) {
				TryInformation nextTryInfo = quizTries.get(j);

				if (nextTryInfo.getUser().equals(user) && 
						(nextTryInfo.getComponentEvent().getEventName().equals(Event.QUIZ_ATTEMPT_SUBMITTED)
						|| nextTryInfo.getComponentEvent().getEventName().equals(Event.QUIZ_ATTEMPT_ABANDONED))) {

					// Calcular la diferencia de tiempo entre los eventos
					long tiempoInicio = tryInfo.getFechaSubida().toEpochSecond();
					long tiempoFin = nextTryInfo.getFechaSubida().toEpochSecond();
					long tiempoTranscurrido = tiempoFin - tiempoInicio;
			        System.out.println("Para el modulo " + tryInfo.getCourseModule() + " if: " + maxTotalHeightSeconds + " < " + tiempoTranscurrido);
					if (maxTotalHeightSeconds < tiempoTranscurrido) maxTotalHeightSeconds = tiempoTranscurrido;
					
					break;
				}
			}
		}
	}
	
	//Para los assignments
	for (TryInformation tryInfo : assignmentsTries) {
        
        JSArray datos = new JSArray();

        // Obtener el usuario del intento
        EnrolledUser user = tryInfo.getUser();
        
        // Calcular la diferencia entre el tiempo de subida y el tiempo de apertura del módulo
        long timeDifference = tryInfo.getFechaSubida().toEpochSecond() - tryInfo.getCourseModule().getTimeOpened().getEpochSecond();
        System.out.println("Para el modulo " + tryInfo.getCourseModule() + " if: " + maxTotalHeightSeconds + " < " + timeDifference);
        if (maxTotalHeightSeconds < timeDifference) maxTotalHeightSeconds = timeDifference;
	}
	
	System.out.println("maxTotalHeightSeconds: " + maxTotalHeightSeconds);
}
	
public JSObject createTrace(CourseModule module, List<TryInformation> tries) {
    // Crear el mapa que almacenará las diferencias de tiempo para cada usuario
    //Map<EnrolledUser, Set<Map.Entry<Long, Double>>> mapa = new HashMap<>();
    
    Map<EnrolledUser, Set<List<Object>>> tiempoIntentoCuestionario = new HashMap<>();
    
    JSArray x = new JSArray();
    JSArray y = new JSArray();
    JSArray z = new JSArray();
    JSArray registros = new JSArray();
    JSArray customdata = new JSArray();

    List<TryInformation> moduleTries = tries.stream()
            .filter(tryInfo -> tryInfo.courseModule.equals(module))
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

                    if (nextTryInfo.getUser().equals(user) && 
                        (nextTryInfo.getComponentEvent().getEventName().equals(Event.QUIZ_ATTEMPT_SUBMITTED) || 
                         nextTryInfo.getComponentEvent().getEventName().equals(Event.QUIZ_ATTEMPT_ABANDONED))) {
                    	
                    	long tiempoTranscurridoDesdeApertura = tryInfo.getFechaSubida().toEpochSecond() - module.getTimeOpened().getEpochSecond();
                    	
                        // Calcular la diferencia de tiempo entre los eventos
                        long tiempoInicio = tryInfo.getFechaSubida().toEpochSecond();
                        long tiempoFin = nextTryInfo.getFechaSubida().toEpochSecond();
                        long tiempoTranscurrido = tiempoFin - tiempoInicio;
                    	System.out.println("tiempoInicio: " + tiempoInicio);
                    	System.out.println("tiempoFin: " + tiempoFin);
                    	System.out.println("Tiempo transcurrido: " + tiempoTranscurrido);
                    	
                        // Obtener la calificación del usuario
                        GradeItem calificacion = calificaciones.stream()
                                .filter(gradeItem -> gradeItem.toString().equals(tryInfo.getCourseModule().getModuleName()))
                                .findFirst()
                                .orElse(null);

                        if(!Double.isNaN(calificacion.getEnrolledUserGrade(user))) {//Si es == entonces todavia no tiene nota
                        	double nota = calificacion.getEnrolledUserGrade(user);
                            double notaMaxima = calificacion.getGrademax();
                            double notaMinima = calificacion.getGrademin();
                            // Crear la entrada y agregarla al mapa
                            List<Object> entry = Arrays.asList(tiempoTranscurridoDesdeApertura, tiempoTranscurrido, nota, notaMaxima, notaMinima);
                            tiempoIntentoCuestionario.computeIfAbsent(user, k -> new HashSet<>()).add(entry);
                        }
                        // Romper el ciclo interno una vez que se haya encontrado el evento "eventoTerminado"
                        break;
                    }
                }
            }
        }
        
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
                
                //Formatear tiempo
            	long horasTotales = tiempoTranscurrido / 3600;
    	        long minutosTotales = (tiempoTranscurrido % 3600) / 60;
    	        long segundosTotales = tiempoTranscurrido % 60;
    	        
    	        if (horasTotales >= 24 || horasTotales <= -24) {
    	            long diasTotales = horasTotales / 24;
    	            horasTotales = horasTotales % 24;
    	            //datos.addWithQuote(String.format("%d días %02d:%02d:%02d", days, hours, minutes, seconds));
    	            tiempoFormateado = String.format("%dd %02dh %02dm %02ds", diasTotales, horasTotales, minutosTotales, segundosTotales);
    	            
    	        } else {
    	        	//datos.addWithQuote(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    	        	tiempoFormateado = String.format("%02dh %02dm %02ds", horasTotales, minutosTotales, segundosTotales);
                }    
    	        
        		double tiempoTranscurridoAdaptado = tiempoTranscurrido; // Inicialmente lo asignamos al mismo valor

        		if (maxTotalHeightSeconds >= 86400) {
        		    unit = "Días";
        		    // Convertir las alturas a días
        		    tiempoTranscurridoAdaptado = tiempoTranscurrido / 86400.0;
        		} else if (maxTotalHeightSeconds >= 3600) {
        		    unit = "Horas";
        		    // Convertir las alturas a horas
        		    tiempoTranscurridoAdaptado = tiempoTranscurrido / 3600.0;
        		} else if (maxTotalHeightSeconds >= 60) {
        		    unit = "Minutos";
        		    // Convertir las alturas a minutos
        		    tiempoTranscurridoAdaptado = tiempoTranscurrido / 60.0;
        		} else {
        		    unit = "Segundos";
        		    tiempoTranscurridoAdaptado = (double) tiempoTranscurrido;
        		}
        		
        		long horasTotalesDesdeApertura = tiempoTranscurridoDesdeApertura / 3600;
    	        long minutosTotalesDesdeApertura = (tiempoTranscurridoDesdeApertura % 3600) / 60;
    	        long segundosTotalesDesdeApertura = tiempoTranscurridoDesdeApertura % 60;
    	
    	        if (horasTotalesDesdeApertura >= 24 || horasTotalesDesdeApertura <= -24) {
    	            long diasTotalesDesdeApertura = horasTotalesDesdeApertura / 24;
    	            horasTotalesDesdeApertura = horasTotalesDesdeApertura % 24;
    	            //datos.addWithQuote(String.format("%d días %02d:%02d:%02d", days, hours, minutes, seconds));
    	            tiempoFormateado = String.format("%dd %02dh %02dm %02ds", diasTotalesDesdeApertura, horasTotalesDesdeApertura, minutosTotalesDesdeApertura, segundosTotalesDesdeApertura);
    	            
    	        } else {
    	        	//datos.addWithQuote(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    	        	tiempoFormateado = String.format("%02dh %02dm %02ds", horasTotalesDesdeApertura, minutosTotalesDesdeApertura, segundosTotalesDesdeApertura);
                }    
    	        
        		double tiempoTranscurridoAdaptadoDesdeApertura = tiempoTranscurridoDesdeApertura; // Inicialmente lo asignamos al mismo valor

        		if (maxTotalHeightSeconds >= 86400) {
        		    unit = "Días";
        		    // Convertir las alturas a días
        		    tiempoTranscurridoAdaptadoDesdeApertura = tiempoTranscurridoDesdeApertura / 86400.0;
        		} else if (maxTotalHeightSeconds >= 3600) {
        		    unit = "Horas";
        		    // Convertir las alturas a horas
        		    tiempoTranscurridoAdaptadoDesdeApertura = tiempoTranscurridoDesdeApertura / 3600.0;
        		} else if (maxTotalHeightSeconds >= 60) {
        		    unit = "Minutos";
        		    // Convertir las alturas a minutos
        		    tiempoTranscurridoAdaptadoDesdeApertura = tiempoTranscurridoDesdeApertura / 60.0;
        		} else {
        		    unit = "Segundos";
        		    tiempoTranscurridoAdaptadoDesdeApertura = (double) tiempoTranscurridoDesdeApertura;
        		}
    	        
        		// Ahora tiempoTranscurridoAdaptado contiene el valor convertido correctamente en la unidad apropiada
        		System.out.println("Tiempo transcurrido: " + tiempoTranscurridoAdaptadoDesdeApertura + " " + unit);

    	        x.addWithQuote(tiempoTranscurridoAdaptado);
		        y.addWithQuote(nota);
		        z.addWithQuote(tiempoTranscurridoAdaptadoDesdeApertura);
		        
		        // Agregar elementos a datos
	        	datos.addWithQuote(tiempoTranscurridoAdaptadoDesdeApertura);
		        datos.addWithQuote(tiempoFormateado);
		        datos.addWithQuote(user.getFullName());
		        datos.addWithQuote(module.getModuleName());
		        datos.addWithQuote(nota);
		        datos.addWithQuote(notaMinima);
		        datos.addWithQuote(notaMaxima);
		
		        customdata.add(datos);
		        System.out.println("CustomData: " + customdata);
		        
		        registros.addWithQuote(
		                  " <b>Tiempo transcurrido (desde apertura):</b> <br> %{customdata[0]} <br><br>"
		        		+ " <b>Tiempo transcurrido (desde comienzo):</b> <br> %{customdata[0]} <br><br>"
						+ " <b>Alumno/a:</b> <br> %{customdata[1]} <br><br>"
		                + " <b>Modulo:</b> <br> %{customdata[2]} <br><br>"
		                + " <b>Nota:</b> <br> %{customdata[3]} <br><br>"
		                + " <b>Rango nota [mínima,máxima]:</b> <br> [%{customdata[4]},%{customdata[5]}] <br><br>"
		                + " <extra></extra>");
            }
                
        }
    } else if (module.getModuleType().equals(ModuleType.ASSIGNMENT)) {
		for (TryInformation tryInfo : moduleTries) {
	        
	        JSArray datos = new JSArray();

	        // Obtener el usuario del intento
	        EnrolledUser user = tryInfo.getUser();
	        
	        // Calcular la diferencia entre el tiempo de subida y el tiempo de apertura del módulo
	        long timeDifference = tryInfo.getFechaSubida().toEpochSecond() - module.getTimeOpened().getEpochSecond();
	                   
	        GradeItem calificacion = calificaciones.stream()
	                .filter(gradeItem -> gradeItem.toString().equals(tryInfo.getCourseModule().getModuleName()))
	                .findFirst()
	                .orElse(null);
	        
	        // Agregar la diferencia al mapa
	        double nota = calificacion.getEnrolledUserGrade(user);
	        if(!Double.isNaN(nota)) {//Si es == entonces todavia no tiene nota
	        	System.out.println("Nota: " + nota);
	        	Map.Entry<Long, Double> timeAndGrade = new AbstractMap.SimpleEntry<>(timeDifference, nota);

		        // Agregar el par al mapa
		        //mapa.computeIfAbsent(user, k -> new HashSet<>()).add(timeAndGrade);
		        
	        	String tiempoFormateado = "";
                
                //Formatear tiempo
            	long horasTotales = timeAndGrade.getKey() / 3600;
    	        long minutosTotales = (timeAndGrade.getKey() % 3600) / 60;
    	        long segundosTotales = timeAndGrade.getKey() % 60;
    	
    	        if (horasTotales >= 24 || horasTotales <= -24) {
    	            long diasTotales = horasTotales / 24;
    	            horasTotales = horasTotales % 24;
    	            //datos.addWithQuote(String.format("%d días %02d:%02d:%02d", days, hours, minutes, seconds));
    	            tiempoFormateado = String.format("%dd %02dh %02dm %02ds", diasTotales, horasTotales, minutosTotales, segundosTotales);
    	            
    	        } else {
    	        	//datos.addWithQuote(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    	        	tiempoFormateado = String.format("%02dh %02dm %02ds", horasTotales, minutosTotales, segundosTotales);
                }    	        	

    	        double tiempoTranscurridoAdaptado = timeDifference; // Inicialmente lo asignamos al mismo valor

        		if (maxTotalHeightSeconds >= 86400) {
        		    unit = "Días";
        		    // Convertir las alturas a días
        		    tiempoTranscurridoAdaptado = timeDifference / 86400.0;
        		} else if (maxTotalHeightSeconds >= 3600) {
        		    unit = "Horas";
        		    // Convertir las alturas a horas
        		    tiempoTranscurridoAdaptado = timeDifference / 3600.0;
        		} else if (maxTotalHeightSeconds >= 60) {
        		    unit = "Minutos";
        		    // Convertir las alturas a minutos
        		    tiempoTranscurridoAdaptado = timeDifference / 60.0;
        		} else {
        		    unit = "Segundos";
        		    tiempoTranscurridoAdaptado = (double) timeDifference;
        		}

        		// Ahora tiempoTranscurridoAdaptado contiene el valor convertido correctamente en la unidad apropiada
        		System.out.println("Tiempo transcurrido: " + tiempoTranscurridoAdaptado + " " + unit);

    	        
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
		        
		        registros.addWithQuote(
		                  " <b>Tiempo transcurrido (desde apertura):</b> <br> %{customdata[0]} <br><br>"
						+ " <b>Alumno/a:</b> <br> %{customdata[1]} <br><br>"
		                + " <b>Modulo:</b> <br> %{customdata[2]} <br><br>"
		                + " <b>Nota:</b> <br> %{customdata[3]} <br><br>"
		                + " <b>Rango nota [mínima,máxima]:</b> <br> [%{customdata[4]},%{customdata[5]}] <br><br>"
		                + " <extra></extra>");
		        
	        }

	        
	    }
	}
    

    //System.out.println("Gradeitems: " + calificaciones);

    //System.out.println("Mapa: " + mapa);

    JSObject trace = new JSObject();

    JSObject marker = new JSObject();
    //marker.put("color", rgb(module));

    //trace.putWithQuote("name", module.getModuleName());
    trace.put("type", "'scatter3d'");
    //trace.put("mode", "'markers'");
    
    trace.put("x", "[1,2,3]");
    trace.put("y", "[3,4,5]");
    trace.put("z", "[6,7,8]");
    
    
    trace.put("mode", "'markers'");
    trace.put("marker", marker);
    //trace.put("customdata", customdata);
    //trace.put("hovertemplate", registros);

    System.out.println("TRACE" + trace);
    return trace;
}

	@Override
	public void createLayout(JSObject layout) {
		JSObject xaxis = new JSObject();
		defaultAxisValues(xaxis, getXAxisTitle(), null);

		JSObject yaxis = new JSObject();
		defaultAxisValues(yaxis, getYAxisTitle(), null);
		
		JSObject zaxis = new JSObject();
		defaultAxisValues(yaxis, getYAxisTitle(), null);

		JSObject scene = new JSObject();
		
		scene.put("xaxis", xaxis);
		scene.put("yaxis", yaxis);
		scene.put("zaxis", "{zeroline:false,title:'<b>Nota obtenida</b>',automargin:true}");
		
		layout.put("scene", scene);
		
	}
	
	
	@Override
	public String getYAxisTitle() {
		//if (maxTotalHeightSeconds > 0)
			//return "<b>" + "Número de " + unit.toUpperCase() + " transcurridos." + "</b>";
			return "<b>" + "Nota obtenida" + "</b>";
		//else return "";
	}
    
    
	@Override
	public String getXAxisTitle() {
		
		System.out.println(maxTotalHeightSeconds);
        if (maxTotalHeightSeconds >= 86400) {
            unit = "Dias";
        } else if (maxTotalHeightSeconds >= 3600) {
            unit = "Horas";
        } else if (maxTotalHeightSeconds >= 60) {
            unit = "Minutos";
        } else {
            unit = "Segundos";
        }
        
		if (maxTotalHeightSeconds > 0)
			return "<b>" + "Tiempo de procrastinación en " + unit.toUpperCase() + "</b>";
		else return "<b>" + "Tiempo de procrastinación" + "</b>";
	}
}
