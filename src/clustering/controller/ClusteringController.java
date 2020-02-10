package clustering.controller;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.ml.clustering.Clusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.property.editor.AbstractPropertyEditor;
import org.controlsfx.property.editor.DefaultPropertyEditorFactory;
import org.controlsfx.property.editor.Editors;
import org.controlsfx.property.editor.PropertyEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clustering.algorithm.Algorithm;
import clustering.algorithm.Algorithms;
import clustering.controller.collector.ActivityCollector;
import clustering.controller.collector.DataCollector;
import clustering.controller.collector.GradesCollector;
import clustering.controller.collector.LogComponentCollector;
import clustering.controller.collector.LogCourseModuleCollector;
import clustering.controller.collector.LogEventCollector;
import clustering.controller.collector.LogSectionCollector;
import clustering.data.UserData;
import controllers.I18n;
import controllers.MainController;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebView;
import javafx.util.Callback;
import javafx.util.StringConverter;
import model.EnrolledUser;

public class ClusteringController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClusteringController.class);

	private MainController mainController;

	@FXML
	private PropertySheet propertySheet;

	@FXML
	private ListView<Algorithm> algorithmList;

	@FXML
	private Button buttonExecute;

	@FXML
	private WebView webView;

	@FXML
	private TableView<UserData> tableView;

	@FXML
	private CheckComboBox<DataCollector> checkComboBoxLogs;

	@FXML
	private CheckBox checkBoxLogs;

	@FXML
	private CheckBox checkBoxGrades;

	@FXML
	private CheckBox checkBoxActivity;

	@FXML
	private CheckComboBox<Integer> checkComboBoxCluster;

	@FXML
	private TableColumn<UserData, ImageView> columnImage;

	@FXML
	private TableColumn<UserData, String> columnName;

	@FXML
	private TableColumn<UserData, Number> columnCluster;

	private GradesCollector gradesCollector;

	private ActivityCollector activityCollector;

	private static final Callback<Item, PropertyEditor<?>> DEFAULT_PROPERTY_EDITOR_FACTORY = new DefaultPropertyEditorFactory();

	@SuppressWarnings("unchecked")
	public void init(MainController controller) {
		mainController = controller;
		gradesCollector = new GradesCollector(mainController);
		checkComboBoxLogs.disableProperty().bind(checkBoxLogs.selectedProperty().not());
		algorithmList.getItems().setAll(Algorithms.getAlgorithms());
		algorithmList.getSelectionModel().selectedItemProperty()
				.addListener((obs, oldValue, newValue) -> changeAlgorithm(newValue));
		algorithmList.getSelectionModel().selectFirst();
		StringConverter<DistanceMeasure> stringConverter = new StringConverter<DistanceMeasure>() {

			@Override
			public String toString(DistanceMeasure object) {
				return I18n.get("clustering." + object.getClass().getSimpleName());
			}

			@Override
			public DistanceMeasure fromString(String string) {
				return null;
			}
		};

		propertySheet.setPropertyEditorFactory(item -> {
			if (item.getValue() instanceof DistanceMeasure) {
				AbstractPropertyEditor<DistanceMeasure, ComboBox<DistanceMeasure>> editor = (AbstractPropertyEditor<DistanceMeasure, ComboBox<DistanceMeasure>>) Editors
						.createChoiceEditor(item, Algorithms.DISTANCES_LIST);
				editor.getEditor().setConverter(stringConverter);
				return editor;
			}
			return DEFAULT_PROPERTY_EDITOR_FACTORY.call(item);
		});
		buttonExecute.setOnAction(e -> execute());
		initCollectors();
	}

	private void initCollectors() {
		gradesCollector = new GradesCollector(mainController);
		activityCollector = new ActivityCollector(mainController);
		List<DataCollector> collectors = new ArrayList<>();
		collectors.add(new LogComponentCollector(mainController));
		collectors.add(new LogEventCollector(mainController));
		collectors.add(new LogSectionCollector(mainController));
		collectors.add(new LogCourseModuleCollector(mainController));
		checkComboBoxLogs.getItems().setAll(collectors);
	}

	private void changeAlgorithm(Algorithm algorithm) {
		propertySheet.getItems().setAll(algorithm.getParameters().getPropertyItems());
	}

	private void execute() {
		List<EnrolledUser> users = mainController.getListParticipants().getSelectionModel().getSelectedItems();
		Algorithm algorithm = algorithmList.getSelectionModel().getSelectedItem();
		Clusterer<UserData> clusterer = algorithm.getClusterer();

		AlgorithmExecuter executer = new AlgorithmExecuter(clusterer, users);
		List<DataCollector> collectors = new ArrayList<>();
		if (checkBoxLogs.isSelected()) {
			collectors.addAll(checkComboBoxLogs.getCheckModel().getCheckedItems());
		}
		if (checkBoxGrades.isSelected()) {
			collectors.add(gradesCollector);
		}
		if (checkBoxActivity.isSelected()) {
			collectors.add(activityCollector);
		}
		List<UserData> clusters = executer.execute(collectors);
		LOGGER.debug("Parametros: {}", algorithm.getParameters());
		updateTable(new FilteredList<UserData>(FXCollections.observableList(clusters)));
		checkComboBoxCluster.getItems()
				.setAll(IntStream.range(0, executer.getNumClusters()).boxed().collect(Collectors.toList()));
		checkComboBoxCluster.getCheckModel().checkAll();
	}

	private void updateTable(FilteredList<UserData> clusters) {
		columnImage.setCellValueFactory(c -> new SimpleObjectProperty<>(new ImageView(new Image(
				new ByteArrayInputStream(c.getValue().getEnrolledUser().getImageBytes()), 50, 50, true, false))));

		SortedList<UserData> sortedList = new SortedList<>(clusters);
		sortedList.comparatorProperty().bind(tableView.comparatorProperty());
		tableView.setItems(sortedList);
		columnName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEnrolledUser().getFullName()));

		columnCluster.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getCluster()));

//		Set<String> keys = clusters.get(0).getKeys();
//		for (String key : keys) {
//			TableColumn<UserData, Number> column = new TableColumn<>(key);
//			column.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getValue(key)));
//			tableView.getColumns().add(column);
//		}

		checkComboBoxCluster.getCheckModel().getCheckedItems()
				.addListener((ListChangeListener.Change<? extends Integer> c) -> clusters.setPredicate(
						o -> checkComboBoxCluster.getCheckModel().getCheckedItems().contains(o.getCluster())));
	}
}
