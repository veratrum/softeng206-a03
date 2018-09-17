package application;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;

public class SampleController implements Initializable {

	@FXML
	private ListView<Creation> creationList;
	@FXML
	private ListView<Recording> recordingList;

	private Creations creations;
	private ObservableList<Creation> creationDataList;
	private ObservableList<Creation> selectedCreations;
	private ObservableList<Recording> recordingDataList;
	
	private String selectedCreationName;
	
	public void handleCreate(){
		
	}
	
	public void handleDelete(){
		
	}
	
	public void handlePlay(){
		if (selectedCreations.size() == 0) {
			return;
		} else if (selectedCreations.size() == 1) {
			// play one creation
		} else {
			// ask the user if they want to randomise the order
			// then play all selected creations
		}
	}
	
	public void handleRate(){
		
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		creations = new Creations();
		
		creationDataList = FXCollections.observableArrayList(creations.getCreations());
		creationList.setItems(creationDataList);
		
		creationList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		
		// the first listener detects the last selected creation and shows its recordings in the second list
		creationList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Creation>() {
			@Override
			public void changed(ObservableValue<? extends Creation> observable, Creation oldValue, Creation newValue) {
				selectedCreationName = newValue.getName();
				updateRecordingList();
			}
		});
		
		/* the second listener records any change in the overall selection of multiple recordings, and is used
		so we know all selected creations when playing multiple in sequence */
		creationList.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<Creation>() {
			@SuppressWarnings("unchecked")
			@Override
			public void onChanged(Change<? extends Creation> change) {
				selectedCreations = (ObservableList<Creation>) change.getList();
			}
		});
		
		if (creations.getCreations().size() > 0) {
			selectedCreationName = creations.getCreations().get(0).getName();
		}
		
		updateRecordingList();
	}
	
	/**
	 * Updates the list of recordings to only show those from the currently selected creation.
	 */
	private void updateRecordingList() {
		// this can only trigger if there are no creations stored in the application
		if (creations.getCreations().size() == 0) {
			return;
		}
		
		Creation selectedCreation = creations.getCreationByName(selectedCreationName);
		
		recordingDataList = FXCollections.observableArrayList(selectedCreation.getRecordings());
		recordingList.setItems(recordingDataList);
	}
}

