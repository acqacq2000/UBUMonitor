package es.ubu.lsi.ubumonitor.view.chart;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import es.ubu.lsi.ubumonitor.controllers.Controller;
import es.ubu.lsi.ubumonitor.controllers.RiskController;
import es.ubu.lsi.ubumonitor.controllers.configuration.MainConfiguration;
import es.ubu.lsi.ubumonitor.util.I18n;
import es.ubu.lsi.ubumonitor.util.UtilMethods;
import es.ubu.lsi.ubumonitor.view.chart.risk.Bubble;
import javafx.concurrent.Worker.State;
import javafx.scene.web.WebEngine;

public class RiskJavaConnector {
	
	private Chart currentType;
	private WebEngine webViewChartsEngine;
	private File file;
	
	public RiskJavaConnector(RiskController riskController) {
		webViewChartsEngine = riskController.getWebViewChartsEngine();
		currentType = new Bubble(riskController.getMainController());
		currentType.setWebViewChartsEngine(webViewChartsEngine)  ;
	}

	public void updateChart() {
		if (webViewChartsEngine.getLoadWorker().getState() != State.SUCCEEDED) {
			return;
		}
		currentType.update();
		
	}

	public void setDefaultValues() {
		webViewChartsEngine.executeScript("manageButtons('" + Tabs.RISK + "')");
		webViewChartsEngine.executeScript("setLocale('" + Locale.getDefault().toLanguageTag() + "')");
		
	}
	
	public void hideLegend() {

		currentType.hideLegend();
	}

	public void clear() {
		currentType.clear();

	}
	public void updateTabImages() {
		MainConfiguration mainConfiguration = Controller.getInstance().getMainConfiguration();
		boolean legendActive = mainConfiguration.getValue(MainConfiguration.GENERAL, "legendActive");
		boolean generalActive = mainConfiguration.getValue(MainConfiguration.GENERAL, "generalActive");
		boolean groupActive = mainConfiguration.getValue(MainConfiguration.GENERAL, "groupActive");
		webViewChartsEngine.executeScript(String.format("imageButton('%s',%s)", "btnLegend", legendActive));
		webViewChartsEngine.executeScript(String.format("imageButton('%s',%s)", "btnMean", generalActive));
		webViewChartsEngine.executeScript(String.format("imageButton('%s',%s)", "btnGroupMean", groupActive));
		
	}

	public void save() {
		currentType.export();
		
	}

	public Chart getCurrentType() {
		return currentType;
	}

	public String export(File file) {
		this.file = file;
		return currentType.export();
	}

	public void saveImage(String str) throws IOException {
		
		byte[] imgdata = DatatypeConverter.parseBase64Binary(str.substring(str.indexOf(',') + 1));
		BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imgdata));

		ImageIO.write(bufferedImage, "png", file);
		UtilMethods.infoWindow(I18n.get("message.export_png") + file.getAbsolutePath());
	}
	
	public boolean swapLegend() {
		return swap(MainConfiguration.GENERAL, "legendActive");
	}

	public boolean swapGeneral() {
		return swap(MainConfiguration.GENERAL, "generalActive");
	}

	public boolean swapGroup() {
		return swap(MainConfiguration.GENERAL, "groupActive");
	}

	private boolean swap(String category, String name) {
		boolean active = Controller.getInstance().getMainConfiguration().getValue(category, name);
		Controller.getInstance().getMainConfiguration().setValue(category, name, !active);
		return !active;
	}
	
	public String getI18n(String key) {
		return I18n.get(key);
	}
	public void dataPointSelection(int selectedIndex) {

		int index = currentType.onClick(selectedIndex);
		if (index >= 0) {
			currentType.mainController.getSelectionUserController().getListParticipants().scrollTo(index);
			currentType.mainController.getSelectionUserController().getListParticipants().getFocusModel().focus(index);
		}

	}

}
