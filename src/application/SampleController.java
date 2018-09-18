package application;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;


import javafx.scene.media.Media;


import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.SwingWorker;

import javafx.application.Platform;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.media.MediaPlayer;

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
	private Recording selectedRecording;


	// REMEBER TO CHECK IF I NEED THIS IN HERE
	@FXML
	ProgressBar progressBar;
	
	// modified from http://proteo.me.uk/2009/10/sound-level-monitoring-in-java/
	private AudioFormat getAudioFormat(){
		float sampleRate = 8000.0F;
		int sampleSizeInBits = 16;
		int channels = 1;
		boolean signed = true;
		boolean bigEndian = false;

		return new AudioFormat(sampleRate,sampleSizeInBits,channels,signed,bigEndian);
	}

	public void handleCreateRecording(){

	}

	public void handleDeleteRecording(){

	}

	public void handlePlayRecording(){
		if (selectedCreations.size() == 0) {
			return;
		} else if (selectedCreations.size() == 1) {
			// play one creation

			//Recording to play
			if (selectedRecording != null) {
				File recordingToPlay = selectedRecording.getFile();
				Media creationMedia = new Media(recordingToPlay.toURI().toString());
				MediaPlayer mediaPlayer = new MediaPlayer(creationMedia);
				mediaPlayer.play();
			}

		} else {
			// ask the user if they want to randomise the order
			// then play all selected creations
		}

		//Finish this one



	}
	
	public void handleRate(){
		
	}
	public void handleTestMicrophone(){
		// modified from http://proteo.me.uk/2009/10/sound-level-monitoring-in-java/
		double micSum = 0.0;
		AudioFormat audioFormat = getAudioFormat();

		TargetDataLine targetDataLine;
		try {
			targetDataLine = (TargetDataLine) AudioSystem.getTargetDataLine(audioFormat);

			// Setting up the targetDataLine
			targetDataLine.open();
			targetDataLine.start();


			byte [] buffer = new byte[2000];
			for (int i=0; i<25; i++) {
				int bytesRead = targetDataLine.read(buffer,0,buffer.length);

				short max;
				if (bytesRead >=0) {
					max = (short) (buffer[0] + (buffer[1] << 8));
					for (int p=2;p<bytesRead-1;p+=2) {
						short thisValue = (short) (buffer[p] + (buffer[p+1] << 8));
						if (thisValue>max) max=thisValue;
					}
					if(max >= 0 ) {
						//System.out.println(max);
						double micLevel = max/1000.0; 
						if (micLevel > 1.0) {
							micLevel = 1.0;
						}
						micSum = micSum + micLevel;
					}
				}
			}
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Setting the progressBar mic Input level
		progressBar.setProgress(micSum/25);

	}

	public void handlePlayCreations(){

	}

	public void handleDeleteCreations(){

	}

	public void handlePlaySelectedCreations(){

	}

	public void handleNewRecording(){
		// dialog code modified from https://code.makery.ch/blog/javafx-dialogs-official/
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Create a new Recording");
		alert.setHeaderText("Create a new Recording for Creation " + selectedCreationName);
		alert.setContentText("Press OK to start recording for 5 seconds.");

		Creation selectedCreationAtInstant = creations.getCreationByName(selectedCreationName);
		String name = selectedCreationName + System.currentTimeMillis();

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK) {
			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() throws Exception {


					// linux version
					/*ProcessBuilder arecordProcessBuilder = new ProcessBuilder("bash", "-c",
							"arecord -d 5 userdata" + File.separator + name + ".wav");*/
					// windows version
					ProcessBuilder arecordProcessBuilder = new ProcessBuilder("cmd", "/c",
							"ffmpeg -y -t 5 -f dshow -i audio=\"Microphone (Realtek High Definition Audio)\" userdata"
									+ File.separator + name + ".wav");



					Process arecordProcess = arecordProcessBuilder.start();

					arecordProcess.waitFor();

					return null;
				}

				@Override
				protected void done() {
					selectedCreationAtInstant.addRecording(new Recording(new File(
							"userdata" + File.separator + name + ".wav")));
					creations.saveState();

					// must update ui from 'edt' of javafx
					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							updateRecordingList();
						}

					});
				}
			};

			worker.execute();
		} else {

		}
	}

	public void handleNewCreation(){

	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		creations = new Creations();

		creationDataList = FXCollections.observableArrayList(creations.getCreations());
		creationList.setItems(creationDataList);

		creationList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		selectedCreations = FXCollections.emptyObservableList();

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
	 * Updates the list of recordings to only show those from the currently selected creations.
	 */
	private void updateRecordingList() {
		// this can only trigger if there are no creations stored in the application
		if (creations.getCreations().size() == 0) {
			return;
		}

		Creation selectedCreation = creations.getCreationByName(selectedCreationName);

		recordingDataList = FXCollections.observableArrayList(selectedCreation.getRecordings());
		recordingList.setItems(recordingDataList);

		// detects the last selected recording
		recordingList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Recording>() {
			@Override
			public void changed(ObservableValue<? extends Recording> observable, Recording oldValue, Recording newValue) {
				selectedRecording = newValue;
			}
		});
	}
}

