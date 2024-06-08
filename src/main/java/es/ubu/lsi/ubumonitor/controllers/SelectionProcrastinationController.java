package es.ubu.lsi.ubumonitor.controllers;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.controlsfx.control.CheckComboBox;

import es.ubu.lsi.ubumonitor.AppInfo;
import es.ubu.lsi.ubumonitor.model.Component;
import es.ubu.lsi.ubumonitor.model.ComponentEvent;
import es.ubu.lsi.ubumonitor.model.Course;
import es.ubu.lsi.ubumonitor.model.CourseModule;
import es.ubu.lsi.ubumonitor.model.ModuleType;
import es.ubu.lsi.ubumonitor.model.TryInformation;
import es.ubu.lsi.ubumonitor.util.I18n;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class SelectionProcrastinationController {

	@FXML
	private ListView<CourseModule> listViewProcrastinationModules;
	
	private FilteredList<CourseModule> filteredProcrastinationModules;
	
	@FXML
	private CheckComboBox<ComponentEvent> checkComboBoxProcrastinationEvents;
	
	private FilteredList<ComponentEvent> filteredProcrastinationEvents;
	

	@FXML
	private TextField textFieldProcrastination, textFieldProcrastinationEvents;

	@FXML
	private CheckBox checkBoxProcrastinationAssigments, checkBoxProcrastinationQuizzes;
	
	@FXML
	private TabPane tabPane;
	
	@FXML
	private ComboBox<String> comboBoxProcrastinationMetricMode;
	
	@FXML
	private ImageView imageEvents;
	
	@FXML
	private ImageView imageMetricMode;

	public void init(MainController mainController, Course actualCourse) {

		tabPane.visibleProperty()
				.bind(mainController.getWebViewTabsController().getProcrastinationTab()
						.selectedProperty());

		fillProcrastinationListView(mainController, actualCourse);
		fillProcrastinationListViewEvents(mainController, actualCourse);
	    fillProcrastinationListViewMetricMode(mainController);
		
	    textFieldProcrastination.textProperty()
		.addListener((ob, oldValue, newValue) -> onChange());
	    
		//Compruebo si estan checkados o no los checkbox
		checkBoxProcrastinationAssigments.selectedProperty()
		.addListener(c -> onChange());
		
		checkBoxProcrastinationQuizzes.selectedProperty()
		.addListener(c -> onChange());
		
		onChange();
		
	    checkComboBoxProcrastinationEvents.getCheckModel().checkAll();
	}

	
	private void fillProcrastinationListView(MainController mainController, Course actualCourse) {
	    filteredProcrastinationModules = new FilteredList<>(actualCourse
	            .getModules()
	            .stream()
	            .filter(cm -> TryInformation.EventProcrastincationModuleTypesSubgroup.contains(cm.getModuleType()))
	            .collect(Collectors.toCollection(FXCollections::observableArrayList)));

	    listViewProcrastinationModules.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

	    listViewProcrastinationModules.setItems(filteredProcrastinationModules);

	    listViewProcrastinationModules.getSelectionModel()
	            .getSelectedItems()
	            .addListener((Change<? extends CourseModule> courseModule) -> {
	                //System.out.println("He saltado el evento de selección de módulos");
	                mainController.getActions().updateListViewProcrastination();
	            });
	}

	private void fillProcrastinationListViewEvents(MainController mainController, Course actualCourse) {
	    //System.out.println("eventos: " + actualCourse.getUniqueComponentsEvents());
	    filteredProcrastinationEvents = new FilteredList<>(
	            FXCollections.observableArrayList(
	                    actualCourse.getUniqueComponentsEvents().stream()
	                            .filter(componentEvent -> TryInformation.EventProcrastincationEventsSubgroup.contains(componentEvent.getEventName()))
	                            .collect(Collectors.toList())));

	    // Definir el comportamiento para cuando se seleccionan elementos en el CheckComboBox
	    checkComboBoxProcrastinationEvents.getCheckModel().checkAll();
	    checkComboBoxProcrastinationEvents.getCheckModel().getCheckedItems()
	            .addListener((Change<? extends ComponentEvent> c) -> {
	                //System.out.println("He saltado el evento de selección de eventos");
	                mainController.getActions().updateListViewProcrastinationEvent();
	            });
	}

	private void fillProcrastinationListViewMetricMode(MainController mainController) {
	    comboBoxProcrastinationMetricMode.getItems().addAll(Arrays.asList(I18n.get("combobox.measureOpening"), I18n.get("combobox.measureStartAttemp")));
	    comboBoxProcrastinationMetricMode.getSelectionModel().selectedItemProperty()
	            .addListener((obs, oldValue, newValue) -> {
	                //System.out.println("He saltado el evento de medidas");
	                mainController.getActions().updateListViewProcrastinationMetricMode();
	            });
	    comboBoxProcrastinationMetricMode.getSelectionModel().select(0);
	}



	private void onChange() {
	    filteredProcrastinationModules.setPredicate(getProcrastinationPredicate());
	    listViewProcrastinationModules.setCellFactory(getListCellCourseModule());
	    onChangeComboBox();
	}

	
	private void onChangeComboBox() {
	    // Obtener los elementos chequeados actualmente
	    List<ComponentEvent> checkedItems = new ArrayList<>(checkComboBoxProcrastinationEvents.getCheckModel().getCheckedItems());

	    // Obtener la lista de eventos que deben mostrarse
	    List<ComponentEvent> eventsToShow = filteredProcrastinationEvents.stream()
	            .filter(Objects::nonNull) // Filtrar los elementos nulos
	            .filter(event -> {
	                boolean showAssignments = checkBoxProcrastinationAssigments.isSelected();
	                boolean showQuizzes = checkBoxProcrastinationQuizzes.isSelected();
	                
	                return (showAssignments && event.getComponent() == Component.ASSIGNMENT)
	                        || (showQuizzes && event.getComponent() == Component.QUIZ);
	            })
	            .collect(Collectors.toList());
	    
	    // Organizar los eventos por tipo de componente
	    Map<Component, List<ComponentEvent>> eventsByComponent = eventsToShow.stream()
	            .collect(Collectors.groupingBy(ComponentEvent::getComponent));

	    // Limpiar el CheckComboBox
	    checkComboBoxProcrastinationEvents.getItems().clear();

	    // Agregar los eventos ordenados por tipo de componente y agregar los separadores
	    eventsByComponent.forEach((component, componentEvents) -> {
	        // Agregar el separador correspondiente al tipo de componente
	        checkComboBoxProcrastinationEvents.getItems().add(new SeparatorComponentEvent(component.getName()));
	        // Agregar los eventos del tipo de componente
	        checkComboBoxProcrastinationEvents.getItems().addAll(componentEvents);
	    });

	    // Configurar el StringConverter
	    checkComboBoxProcrastinationEvents.setConverter(getComponentEventStringConverter());
	    checkComboBoxProcrastinationEvents.setStyle("-fx-font-weight: bold; -fx-font-style: italic;");
	    
	    // Chequear los elementos que estaban chequeados anteriormente, si aún están disponibles
	    checkComboBoxProcrastinationEvents.getCheckModel().clearChecks();
	    for (ComponentEvent item : checkedItems) {
	        if (checkComboBoxProcrastinationEvents.getItems().contains(item)) {
	            checkComboBoxProcrastinationEvents.getCheckModel().check(item);
	        }
	    }
	}


	private Predicate<CourseModule> getProcrastinationPredicate() {
		//Muestros los elementos que cumplen los siguientes filtros
		return procrastination -> {
	        boolean showAssignments = checkBoxProcrastinationAssigments.isSelected();
	        boolean showQuizzes = checkBoxProcrastinationQuizzes.isSelected();

	        boolean moduleTypeMatches = (showAssignments && procrastination.getModuleType() == ModuleType.ASSIGNMENT)
	                                    || (showQuizzes && procrastination.getModuleType() == ModuleType.QUIZ);

	        boolean matchesSearchText = textFieldProcrastination.getText().isEmpty()
	                                        || procrastination.getModuleName().toLowerCase()
	                                            .contains(textFieldProcrastination.getText());

	        return moduleTypeMatches && matchesSearchText;
	    };
	}
	
	
	private Callback<ListView<CourseModule>, ListCell<CourseModule>> getListCellCourseModule() {
		return callback -> new ListCell<CourseModule>() {
			@Override
			public void updateItem(CourseModule courseModule, boolean empty) {
				super.updateItem(courseModule, empty);
				if (empty) {
					setText(null);
					setGraphic(null);
				} else {
					if(courseModule.getTimeOpened() == null) {
						setText(courseModule.getModuleName() + " " + I18n.get("text.withoutOpenDate"));
						setTextFill(Color.GRAY);
	                    setFont(Font.font("System", FontPosture.ITALIC, 12)); // Establece el estilo en cursiva
	                    setDisable(true);

					}else {

						setText(courseModule.getModuleName() + " (" + courseModule.getTimeOpened().atZone(ZoneId.of("Europe/Madrid")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + ")");
						setTextFill(courseModule.isVisible() ? Color.BLACK : Color.GRAY);
	                    setFont(Font.getDefault()); // Restaura la fuente predeterminada si no es nula
	                    setDisable(false);
					}


					try {
						Image image = new Image(AppInfo.IMG_DIR + courseModule.getModuleType()
								.getModName() + ".png");
						setGraphic(new ImageView(image));
					} catch (Exception e) {
						setGraphic(null);
					}
				}
			}
		};
	}
	
	private StringConverter<ComponentEvent> getComponentEventStringConverter() {
		return new StringConverter<ComponentEvent>() {
			@Override
		    public String toString(ComponentEvent componentEvent) {
		        if (componentEvent instanceof SeparatorComponentEvent) {
	                return "------" + I18n.get("chbxcmbbx." + ((SeparatorComponentEvent) componentEvent).getNombre()) + "------";
		        } else {
	                return I18n.get(componentEvent.getEventName());
		        }
		    }

		    @Override
		    public ComponentEvent fromString(String string) {
		        return null; // Implementa este método si es necesario
		    }
	    };
	}
	
	

	public void selectAllAssignmentsAndQuizzes() {
		// Seleccionar todos los elementos
	    listViewProcrastinationModules.getSelectionModel().selectAll();

	    // Obtener los índices de los elementos deshabilitados
	    List<Integer> disabledIndexes = listViewProcrastinationModules.getItems().stream()
	            .filter(module -> module.getTimeOpened() == null)
	            .map(module -> listViewProcrastinationModules.getItems().indexOf(module))
	            .collect(Collectors.toList());

	    // Deseleccionar los elementos deshabilitados por índice
	    disabledIndexes.forEach(index -> listViewProcrastinationModules.getSelectionModel().clearSelection(index));
	    
	    for (CourseModule cm: listViewProcrastinationModules.getSelectionModel().getSelectedItems()) System.out.println(cm.getModuleName());
	}

	public ListView<CourseModule> getListViewProcrastination() {
		return listViewProcrastinationModules;
	}
	
	public CheckComboBox<ComponentEvent> getListViewProcrastinationEvent() {
		return checkComboBoxProcrastinationEvents;
	}
	
	public ComboBox<String> getListViewProcrastinationMetricMode() {
		return comboBoxProcrastinationMetricMode;
	}
	
	public ImageView getImageEvents() {
		return imageEvents;
	}
	
	public ImageView getImageMetricMode() {
		return imageMetricMode;
	}
	
	private void toggleChecks() {
        ObservableList<Integer> selectedIndices = FXCollections.observableArrayList(listViewProcrastinationModules.getSelectionModel().getSelectedIndices());
        
        // Uncheck all the checked items
        listViewProcrastinationModules.getSelectionModel().clearSelection();

        // Re-check all the previously checked items
        for (Integer index : selectedIndices) {
        	listViewProcrastinationModules.getSelectionModel().select(index);
        }
    }
	
	public class SeparatorComponentEvent extends ComponentEvent {
		private String nombre;
	    public SeparatorComponentEvent(String name) {
	        super();
	        this.nombre = name;
	    }
	    public String getNombre() {
			return nombre;
		}
	}

}
