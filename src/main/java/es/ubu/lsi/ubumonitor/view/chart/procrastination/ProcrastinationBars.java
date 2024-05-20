package es.ubu.lsi.ubumonitor.view.chart.procrastination;

import java.awt.Color;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.controlsfx.control.CheckComboBox;

import es.ubu.lsi.ubumonitor.controllers.Controller;
import es.ubu.lsi.ubumonitor.controllers.MainController;
import es.ubu.lsi.ubumonitor.controllers.SelectionProcrastinationController.SeparatorComponentEvent;
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
import javafx.util.Pair;

public class ProcrastinationBars<E> extends Plotly {

    private ListView<CourseModule> listViewProcrastination;
    private CheckComboBox<ComponentEvent> listViewProcrastinationEvent;
    private DatePicker datePickerStart;
    private DatePicker datePickerEnd;

    private Map<Pair<CourseModule, Event>, Color> colors;
    String unit;//Unidad de tiempo
    long maxTotalHeightSeconds;//Tiempo mas largo medido en segundos
    
    public ProcrastinationBars(MainController mainController, ListView<CourseModule> listViewProcrastination, CheckComboBox<ComponentEvent> listViewProcrastinationEvent, DatePicker datePickerStart, DatePicker datePickerEnd) {
        super(mainController, ChartType.PROCRASTINATION_BARS);

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
        if (modules.size() > 0 && modules.get(0) != null) 
            modules = modules.stream().filter(module -> module.getTimeOpened() != null).collect(Collectors.toList());
        else modules.clear();
        
        List<ComponentEvent> componentsEvents = new ArrayList<>(listViewProcrastinationEvent.getCheckModel().getCheckedItems());
        componentsEvents.removeIf(event -> event instanceof SeparatorComponentEvent || event == null);

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

        List<Pair<CourseModule, Event>> keys = new ArrayList<>();
        for (CourseModule module : modules) 
        	for (Event event: events) 
        		keys.add(new Pair<>(module, event));

        if (keys.size() > 0) colors = UtilMethods.getRandomColors(keys);
        
        maxTotalHeightSeconds = tries.stream()
                .mapToLong(intento -> (long) (intento.getFechaSubida().toEpochSecond() - intento.courseModule.getTimeOpened().getEpochSecond()))
                .max()
                .orElse(0L);
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
        	for (Event event: events) 
        		data.add(createTrace(module, event, tries));
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
        JSArray heights = new JSArray();
		JSArray registros = new JSArray();
        JSArray colorBar = new JSArray();

        //VARIABLES DE CONTROL
        boolean clarito = false;
        EnrolledUser userAnterior = null;
        int numeroIntentos = 1;
        long tiempoEventoAnterior = 0;
        long tiempoDesdeApertura = 0;
        
        
        // Iterar sobre cada usuario y calcular la altura total
        for (TryInformation tryInfo : moduleEventTries) {
        	        	
        	System.out.println("TRY_INFO: tiempoEventoAnterior: " + tiempoEventoAnterior);
        	JSArray datos = new JSArray(); //USUARIO, MODULO, EVENTO, TIEMPO_TOTAL, (TIEMPO_ACTUAL), INSTANTE
            // Obtener el usuario
            EnrolledUser user = tryInfo.user;
            
            System.out.println("UserAnterior: " + userAnterior + ", user : " + user);
        	//if(userAnterior != user) userAnterior = null; 

            // Agregar el usuario a la lista de coordenadas x
            userNames.addWithQuote(user.getFullName());
            datos.addWithQuote(user.getFullName());
            
            // Agregar el módulo a la lista de coordenadas y
            datos.addWithQuote(module.getModuleName());
            
            // Agregar el evento
            datos.addWithQuote(I18n.get(event));

            // Calcular la altura total
            //long totalHeight = module.getTimeOpened().getEpochSecond() - tryInfo.getFechaSubida().toEpochSecond();
            long diferenciaTiempoEntreEventos = 0;
            if (user.equals(userAnterior)) {
            	tiempoDesdeApertura = tryInfo.getFechaSubida().toEpochSecond() - module.getTimeOpened().getEpochSecond();
	            diferenciaTiempoEntreEventos = tryInfo.getFechaSubida().toEpochSecond() - module.getTimeOpened().getEpochSecond() - tiempoEventoAnterior;
	            
	            heights.add(diferenciaTiempoEntreEventos);
	            
            }else {
            	tiempoDesdeApertura = tryInfo.getFechaSubida().toEpochSecond() - module.getTimeOpened().getEpochSecond();
            	
            	heights.add(tiempoDesdeApertura);
            }
            System.out.println("tiempoEventoAnterior: " + tiempoEventoAnterior);
                        
	        // Convertir segundos a horas, minutos y segundos
	        long horasActuales = diferenciaTiempoEntreEventos / 3600;
	        long minutosActuales = (diferenciaTiempoEntreEventos % 3600) / 60;
	        long segundosActuales = diferenciaTiempoEntreEventos % 60;
	        
	        long horasTotales = tiempoDesdeApertura / 3600;
	        long minutosTotales = (tiempoDesdeApertura % 3600) / 60;
	        long segundosTotales = tiempoDesdeApertura % 60;
	
	        if (horasActuales >= 24 || horasActuales <= -24 || horasTotales >= 24 || horasTotales <= -24) {
	            long diasActuales = horasActuales / 24;
	            long diasTotales = horasTotales / 24;
	            horasActuales = horasActuales % 24;
	            horasTotales = horasTotales % 24;
	            //datos.addWithQuote(String.format("%d días %02d:%02d:%02d", days, hours, minutes, seconds));
	            datos.addWithQuote(String.format("%dd %02dh %02dm %02ds", diasTotales, horasTotales, minutosTotales, segundosTotales));
	            datos.addWithQuote(String.format("%dd %02dh %02dm %02ds", diasActuales, horasActuales, minutosActuales, segundosActuales));
	            
	        } else {
	        	//datos.addWithQuote(String.format("%02d:%02d:%02d", hours, minutes, seconds));
	        	datos.addWithQuote(String.format("%02dh %02dm %02ds", horasTotales, minutosTotales, segundosTotales));
	        	datos.addWithQuote(String.format("%02dh %02dm %02ds", horasActuales, minutosActuales, segundosActuales));
            }

	        
	        datos.addWithQuote(tryInfo.getFechaSubida().withZoneSameInstant(ZoneId.of("Europe/Madrid")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
	        	        
	        Color c = colors.get(new Pair<>(module,event));
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
	        
	        //System.out.println("USUARIO:" + user + ", ANTERIOR: " + userAnterior + ", IGUALES: " + user.equals(userAnterior) + ", CLARITO: " + clarito);
	        
	        colorBar.addWithQuote(String.format("'rgba(%d,%d,%d,1.0)'", c.getRed(),c.getGreen(),c.getBlue()));
	        
        	System.out.println("Usuarios iguales: " + user.equals(userAnterior));
	        if(user.equals(userAnterior))
	        	numeroIntentos++; 
        	else
        		numeroIntentos = 1; 
        	
        	tiempoEventoAnterior = tiempoDesdeApertura;

	        
	        
	        if(numeroIntentos == 1) {
	        	registros.addWithQuote("<b>--------------------INTENTO Nº" + numeroIntentos + "--------------------</b> <br><br>"
        			+ " <b>Tiempo transcurrido (desde apertura):</b> <br> %{customdata[3]} <br><br>"
					+ " <b>Modulo:</b> <br> %{customdata[1]} <br><br>"
					+ " <b>Evento:</b> <br> %{customdata[2]} <br><br>"
					+ " <b>Alumno:</b> <br> %{customdata[0]} <br><br>"
					+ " <b>Instante: </b> <br> %{customdata[5]}"
					+ "<extra></extra>");
	        }else {
	        	registros.addWithQuote("<b>--------------------INTENTO Nº" + numeroIntentos + "--------------------</b> <br><br>"
	        			+ " <b>Tiempo transcurrido (desde apertura):</b> <br> %{customdata[3]} <br><br>"
	        			+ " <b>Tiempo transcurrido (desde evento anterior):</b> <br> %{customdata[4]} <br><br>"
						+ " <b>Modulo:</b> <br> %{customdata[1]} <br><br>"
						+ " <b>Evento:</b> <br> %{customdata[2]} <br><br>"
						+ " <b>Alumno/a:</b> <br> %{customdata[0]} <br><br>"
						+ " <b>Instante: </b> <br> %{customdata[5]}"
						+ "<extra></extra>");
	        }
	        
	        	        
	        userAnterior = user;
	        customdata.add(datos);  
        }
        
        //System.out.println("COLORS:" + colors);
		//System.out.println("COLOR BAR:" + colorBar);
		//System.out.println("COLOR BORDER BAR:" + colorBorderBar);
        
        //System.out.println(customdata);

        // Crear la traza para el módulo actual
        JSObject trace = new JSObject();
        trace.put("type", "'bar'");
        trace.putWithQuote("name", "<b>" + module.getModuleName() + "</b> <br> \t\t" + I18n.get(event));
        trace.put("x", userNames);
        //System.out.println("Heights: " + heights);
        JSArray convertedHeights = new JSArray();
        if (maxTotalHeightSeconds >= 86400) {
            unit = "Dias";
            // Convertir las alturas a días
            convertedHeights = heights.stream().map(height -> (double) ((long) height) / 86400.0).collect(Collectors.toCollection(JSArray::new));
        } else if (maxTotalHeightSeconds >= 3600) {
            unit = "Horas";
            // Convertir las alturas a horas
            convertedHeights = heights.stream().map(height -> (double) ((long) height) / 3600.0).collect(Collectors.toCollection(JSArray::new));
        } else if (maxTotalHeightSeconds >= 60) {
            unit = "Minutos";
            // Convertir las alturas a minutos
            convertedHeights = heights.stream().map(height -> (double) ((long) height) / 60.0).collect(Collectors.toCollection(JSArray::new));
        } else {
            unit = "Segundos";
        }
        heights = convertedHeights;
        System.out.println("Heights: " + heights);

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
    	JSObject xaxis = new JSObject();
		defaultAxisValues(xaxis, getXAxisTitle(), null);

		JSObject yaxis = new JSObject();
		defaultAxisValues(yaxis, getYAxisTitle(), null);

		layout.put("xaxis", xaxis);
		layout.put("yaxis", yaxis);

    }

	@Override
	public String getYAxisTitle() {
		if (maxTotalHeightSeconds > 0)
			return "<b>" + "Número de " + unit.toUpperCase() + " transcurridos." + "</b>";
		else return "";
	}
    
    
	@Override
	public String getXAxisTitle() {
		if (maxTotalHeightSeconds > 0)
			return "<b>" + "Alumnos/as" + "</b>";
		else return "";
	}
}
