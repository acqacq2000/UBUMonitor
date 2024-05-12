package es.ubu.lsi.ubumonitor.view.chart.procrastination;

import java.awt.Color;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.controlsfx.control.CheckComboBox;

import es.ubu.lsi.ubumonitor.controllers.MainController;
import es.ubu.lsi.ubumonitor.model.ComponentEvent;
import es.ubu.lsi.ubumonitor.model.CourseModule;
import es.ubu.lsi.ubumonitor.model.EnrolledUser;
import es.ubu.lsi.ubumonitor.model.Event;
import es.ubu.lsi.ubumonitor.model.LogLine;
import es.ubu.lsi.ubumonitor.model.TryInformation;
import es.ubu.lsi.ubumonitor.util.I18n;
import es.ubu.lsi.ubumonitor.util.JSArray;
import es.ubu.lsi.ubumonitor.util.JSObject;
import es.ubu.lsi.ubumonitor.util.UtilMethods;
import es.ubu.lsi.ubumonitor.view.chart.ChartType;
import es.ubu.lsi.ubumonitor.view.chart.Plotly;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;

public class ProcrastinationLines<E> extends Plotly {

    private ListView<CourseModule> listViewProcrastination;
    private CheckComboBox<ComponentEvent> listViewProcrastinationEvent;
    private DatePicker datePickerStart;
    private DatePicker datePickerEnd;

    private Map<CourseModule, Color> colors;
    
    public ProcrastinationLines(MainController mainController, ListView<CourseModule> listViewProcrastination, CheckComboBox<ComponentEvent> listViewProcrastinationEvent, DatePicker datePickerStart, DatePicker datePickerEnd) {
        super(mainController, ChartType.PROCRASTINATION_LINES);

        this.listViewProcrastination = listViewProcrastination;
        this.listViewProcrastinationEvent = listViewProcrastinationEvent;
        this.datePickerStart = datePickerStart;
        this.datePickerEnd = datePickerEnd;

        useRangeDate = true;
        useLegend = true;
    }

    @Override
    public void createData(JSArray data) {
        List<EnrolledUser> users = getSelectedEnrolledUser();
        List<CourseModule> modules = new ArrayList<>(listViewProcrastination.getSelectionModel().getSelectedItems());
        List<ComponentEvent> componentsEvents = new ArrayList<>(listViewProcrastinationEvent.getCheckModel().getCheckedItems());

        List<Event> events = componentsEvents.stream().map(ComponentEvent::getEventName).collect(Collectors.toList());

        List<TryInformation> tries = getData(users, modules, events);
        
        // Ordenar los tries según el orden de los usuarios en la lista users y luego dentro de cada user en orden de llegada de los eventos
        tries.sort(Comparator.comparing((TryInformation tryInfo) -> {
            EnrolledUser user = tryInfo.user;
            return users.indexOf(user); // Obtiene el índice del usuario en la lista users
        }).thenComparingLong(tryInfo -> tryInfo.getFechaSubida().toEpochSecond()));
        
        System.out.println("-----------------------------------------------------");
        for (TryInformation tri: tries) System.out.println(tri);
        System.out.println("-----------------------------------------------------");

        colors = UtilMethods.getRandomColors(modules);
        
        for (CourseModule module : modules) {
        	for (Event event: events) {
        		data.add(createTrace(module, event, tries));
        	}
            
        }
        //System.out.println("TRAZA TOTAL: " + data);
    }

    public List<TryInformation> getData(List<EnrolledUser> users, List<CourseModule> modules, List<Event> events) {
        List<TryInformation> tries = new ArrayList<>();

        Instant start = datePickerStart.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = datePickerEnd.getValue().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<LogLine> logLines = actualCourse.getLogs().getList();

        for (LogLine logLine : logLines) {
            if (modules.contains(logLine.getCourseModule()) && events.contains(logLine.getEventName()) && users.contains(logLine.getUser())) {
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

    public JSObject createTrace(CourseModule module, Event event, List<TryInformation> tries) {
        // Filtrar los intentos del módulo actual
    	List<TryInformation> moduleEventTries = tries.stream()
    		    .filter(tryInfo -> tryInfo.courseModule.equals(module) && tryInfo.componentEvent.getEventName().equals(event))
    		    .collect(Collectors.toList());
        // Crear listas para almacenar los valores de las coordenadas x, y y alturas
        JSArray userNames = new JSArray();
        JSArray customdata = new JSArray();
		JSArray moduleEventNames = new JSArray();
        JSArray heights = new JSArray();
		JSArray registros = new JSArray();
        JSArray colorBar = new JSArray();
        JSArray colorBorderBar = new JSArray();

        //VARIABLES DE CONTROL
        boolean clarito = false;
        EnrolledUser userAnterior = null;
        int numeroIntentos = 1;
        long alturaAnterior = 0;
        long alturaTotal = 0;
        
        
        // Iterar sobre cada usuario y calcular la altura total
        for (TryInformation tryInfo : moduleEventTries) {
        	JSArray datos = new JSArray(); //USUARIO, MODULO, EVENTO, TIEMPO, TIEMPO_ACTUAL
            // Obtener el usuario
            EnrolledUser user = tryInfo.user;

            // Agregar el usuario a la lista de coordenadas x
            userNames.addWithQuote(user.getFullName());
            datos.addWithQuote(user.getFullName());
            
            // Agregar el módulo a la lista de coordenadas y
            datos.addWithQuote(module.getModuleName());
            
            // Agregar el evento
            datos.addWithQuote(I18n.get(event));
            
            if (!moduleEventNames.contains(module.getModuleName() + "<br>" + I18n.get(event))) {
            	moduleEventNames.addWithQuote(module.getModuleName() + "<br>" + I18n.get(event));
            	
            }

            // Calcular la altura total
            //long totalHeight = module.getTimeOpened().getEpochSecond() - tryInfo.getFechaSubida().toEpochSecond();
            long totalHeightSeconds = 0;
            if (alturaAnterior == 0) {
	            totalHeightSeconds = tryInfo.getFechaSubida().toEpochSecond() - module.getTimeOpened().getEpochSecond();
            }else {
            	alturaTotal = tryInfo.getFechaSubida().toEpochSecond() - module.getTimeOpened().getEpochSecond();
	            totalHeightSeconds = tryInfo.getFechaSubida().toEpochSecond() - alturaAnterior;
            }
                        
        	heights.add(totalHeightSeconds);
	        // Convertir segundos a horas, minutos y segundos
	        long hours = totalHeightSeconds / 3600;
	        long minutes = (totalHeightSeconds % 3600) / 60;
	        long seconds = totalHeightSeconds % 60;
	        
	        long hoursActual = alturaTotal / 3600;
	        long minutesActual = (alturaTotal % 3600) / 60;
	        long secondsActual = alturaTotal % 60;
	
	        if (hours >= 24 || hours <= -24 || hoursActual >= 24 || hoursActual <= -24) {
	            long days = hours / 24;
	            long daysActual = hoursActual / 24;
	            hours = hours % 24;
	            hoursActual = hoursActual % 24;
	            //datos.addWithQuote(String.format("%d días %02d:%02d:%02d", days, hours, minutes, seconds));
	            datos.addWithQuote(String.format("%dd %02dh %02dm %02ds", days, hours, minutes, seconds));
	            datos.addWithQuote(String.format("%dd %02dh %02dm %02ds", daysActual, hoursActual, minutesActual, secondsActual));

	        } else {
	        	//datos.addWithQuote(String.format("%02d:%02d:%02d", hours, minutes, seconds));
	        	datos.addWithQuote(String.format("%02dh %02dm %02ds", hours, minutes, seconds));
	        	datos.addWithQuote(String.format("%02dh %02dm %02ds", hoursActual, minutesActual, secondsActual));
            }
	        
	        System.out.println("USUARIO:" + user + ", ANTERIOR: " + userAnterior + ", IGUALES: " + user.equals(userAnterior) + ", CLARITO: " + clarito);
	        
	        Color c = colors.get(module);
	        if(user.equals(userAnterior)) {
	        	clarito = !clarito;
	        	if(clarito) {
	        		Color d = c.brighter().brighter();
	        		//System.out.println("CAMBIO COLOR: (" + c.getRed() + ", " + c.getGreen() + ", " + c.getBlue() + ") --> (" + d.getRed() + ", " + d.getGreen() + ", " + d.getBlue() + ")"); 
	        		c = d;
	        	}
	        	
	        }else {
	        	clarito = false;
	        }
	        
	        colorBar.addWithQuote(String.format("'rgba(%d,%d,%d,1.0)'", c.getRed(),c.getGreen(),c.getBlue()));
			colorBorderBar.addWithQuote(String.format("'#%02x%02x%02x'", 0, 0, 0));
	        
	        if(user.equals(userAnterior)) numeroIntentos++; else numeroIntentos = 1;
	        
	        if(alturaTotal == 0) {
	        	registros.addWithQuote("<b>--------------------INTENTO Nº" + numeroIntentos + "--------------------</b> <br><br>"
        			+ " <b>Tiempo transcurrido (desde apertura):</b> <br> %{customdata[3]} <br><br>"
					+ " <b>Modulo:</b> <br> %{customdata[1]} <br><br>"
					+ " <b>Evento:</b> <br> %{customdata[2]} <br><br>"
					+ " <b>Alumno:</b> <br> %{customdata[0]}"
					+ "<extra></extra>");
	        }else {
	        	registros.addWithQuote("<b>--------------------INTENTO Nº" + numeroIntentos + "--------------------</b> <br><br>"
	        			+ " <b>Tiempo transcurrido (desde apertura):</b> <br> %{customdata[3]} <br><br>"
	        			+ " <b>Tiempo transcurrido (desde evento anterior):</b> <br> %{customdata[4]} <br><br>"
						+ " <b>Modulo:</b> <br> %{customdata[1]} <br><br>"
						+ " <b>Evento:</b> <br> %{customdata[2]} <br><br>"
						+ " <b>Alumno:</b> <br> %{customdata[0]}"
						+ "<extra></extra>");
	        }
	        
	        	        
	        userAnterior = user;
	        alturaAnterior = totalHeightSeconds;

	        customdata.add(datos);  
        }
        
        //System.out.println("COLORS:" + colors);
		//System.out.println("COLOR BAR:" + colorBar);
		//System.out.println("COLOR BORDER BAR:" + colorBorderBar);
        
        //System.out.println(customdata);
        System.out.println(moduleEventNames);


        // Crear la traza para el módulo actual
        JSObject trace = new JSObject();
        trace.put("type", "'bar'");
        trace.putWithQuote("name", "<b>" + module.getModuleName() + "</b> <br> \t\t" + I18n.get(event));
        trace.put("x", userNames);
        trace.put("y", heights);//Se pueden enviar fechas directamente???
        JSObject marker = new JSObject();
		marker.put("color", colorBar);
		trace.put("marker", marker);
        trace.put("customdata", customdata);
        trace.put("hovertemplate", registros);
        
        //System.out.println(trace.toString());
        return trace;
    }

    @Override
    public void exportCSV(String path) throws IOException {
        // TODO Auto-generated method stub
    }

    @Override
    public void createLayout(JSObject layout) {
    }
}
