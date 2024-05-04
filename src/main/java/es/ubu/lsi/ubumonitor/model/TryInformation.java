package es.ubu.lsi.ubumonitor.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

public class TryInformation implements Serializable {

	private static final long serialVersionUID = 1L;
	private int id;
	public CourseModule courseModule;
	public ComponentEvent componentEvent;
	public EnrolledUser user;
	public ZonedDateTime fechaSubida;
	
	public TryInformation() {}
	
	public TryInformation(int id) {
		this.id = id;
	}

	public CourseModule getCourseModule() {
		return courseModule;
	}

	public void setCourseModule(CourseModule courseModule) {
		this.courseModule = courseModule;
	}

	public ComponentEvent getComponentEvent() {
		return componentEvent;
	}

	public void setComponentEvent(ComponentEvent componentEvent) {
		this.componentEvent = componentEvent;
	}
	
	public EnrolledUser getUser() {
		return user;
	}

	public void setUser(EnrolledUser user) {
		this.user = user;
	}

	public ZonedDateTime getFechaSubida() {
		return fechaSubida;
	}

	public void setFechaSubida(ZonedDateTime fechaSubida) {
		this.fechaSubida = fechaSubida;
	}

	@Override
	public String toString() {
		return "TryInformation [courseModule=" + courseModule + ", componentEvent=" + componentEvent + ", user=" + user + ", fechaIntento=" + fechaSubida + "]";
	}
	
	public static List<ModuleType> EventProcrastincationModuleTypesSubgroup = Arrays.asList(
		ModuleType.ASSIGNMENT,
		ModuleType.QUIZ
	);

    public static List<Event> EventProcrastincationEventsSubgroup = Arrays.asList(
    	Event.A_SUBMISSION_HAS_BEEN_SUBMITTED,
        Event.QUIZ_ATTEMPT_STARTED,
        Event.QUIZ_ATTEMPT_SUBMITTED
    );

}
