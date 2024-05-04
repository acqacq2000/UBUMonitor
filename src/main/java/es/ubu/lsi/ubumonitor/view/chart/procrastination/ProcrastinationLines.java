package es.ubu.lsi.ubumonitor.view.chart.procrastination;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.controlsfx.control.CheckComboBox;

import es.ubu.lsi.ubumonitor.controllers.MainController;
import es.ubu.lsi.ubumonitor.model.ComponentEvent;
import es.ubu.lsi.ubumonitor.model.CourseModule;
import es.ubu.lsi.ubumonitor.model.EnrolledUser;
import es.ubu.lsi.ubumonitor.model.Event;
import es.ubu.lsi.ubumonitor.model.LogLine;
import es.ubu.lsi.ubumonitor.model.TryInformation;
import es.ubu.lsi.ubumonitor.util.JSArray;
import es.ubu.lsi.ubumonitor.util.JSObject;
import es.ubu.lsi.ubumonitor.view.chart.ChartType;
import es.ubu.lsi.ubumonitor.view.chart.Plotly;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;

public class ProcrastinationLines extends Plotly {

    private ListView<CourseModule> listViewProcrastination;
    private CheckComboBox<ComponentEvent> listViewProcrastinationEvent;
    private DatePicker datePickerStart;
    private DatePicker datePickerEnd;

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

        for (CourseModule module : modules) {
            data.add(createTrace(module, tries));
        }
        System.out.println("TRAZA TOTAL: " + data);
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
        System.out.println("-----------------------------------------------------");
        for (TryInformation tri: tries) System.out.println(tri);
        System.out.println("-----------------------------------------------------");

        return tries;
    }

    public JSObject createTrace(CourseModule module, List<TryInformation> tries) {
        // Filtrar los intentos del módulo actual
        List<TryInformation> moduleTries = tries.stream().filter(tryInfo -> tryInfo.courseModule.equals(module)).collect(Collectors.toList());

        // Crear listas para almacenar los valores de las coordenadas x, y y alturas
        JSArray x = new JSArray();
        JSArray y = new JSArray();
        JSArray heights = new JSArray();

        // Iterar sobre cada usuario y calcular la altura total
        for (TryInformation tryInfo : moduleTries) {
            // Obtener el usuario
            EnrolledUser user = tryInfo.user;

            // Agregar el usuario a la lista de coordenadas x
            x.addWithQuote(user.getFullName());

            // Agregar el módulo a la lista de coordenadas y
            y.addWithQuote(module.getModuleName());

            // Calcular la altura total
            long totalHeight = module.getTimeOpened().getEpochSecond() - tryInfo.getFechaSubida().toEpochSecond();
            //System.out.println("TimeTry: " + tryInfo.getFechaSubida() + " - ModuleOpen: " + module.getTimeOpened());
            // Agregar la altura a la lista de alturas
            heights.add(totalHeight);
        }
        System.out.println(heights);

        // Crear la traza para el módulo actual
        JSObject trace = new JSObject();
        trace.put("type", "'bar'");
        trace.putWithQuote("name", module.getModuleName());
        trace.put("x", x);
        trace.put("y", heights);
        trace.put("customdata", y);
        trace.put("hovertemplate", "'<br>%{y} - %{x}: %{customdata}<extra></extra>'");
        
        System.out.println(trace.toString());
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
