package application;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
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
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class SampleController implements Initializable {

	@FXML
	private ListView<Creation> creationList;
	@FXML
	private ListView<Recording> recordingList;

	private Creations creations;
	private ObservableList<Creation> creationDataList;
	private ObservableList<Recording> recordingDataList;

	private Creation selectedCreation;
	private Recording selectedRecording;
	private ObservableList<Creation> selectedCreations;
	private ObservableList<Recording> selectedRecordings;


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
		// dialog code modified from https://code.makery.ch/blog/javafx-dialogs-official/
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Delete selected Recordings");
		alert.setHeaderText(null);
		alert.setContentText("Are you sure you want to delete " + selectedRecordings.size() + " Recordings?");

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK) {
			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

				@Override
				protected Void doInBackground() throws Exception {
					//for (Recording recording: selectedRecordings) {
					for (Recording recording: selectedRecordings) {
						recording.delete();
					}
					
					return null;
				}
					
				@Override
				protected void done() {
					// must update ui from 'edt' of javafx
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							updateRecordingList();
							recordingList.getSelectionModel().clearSelection();
							recordingList.getSelectionModel().selectFirst();
						}
					});
				}
				
			};
			
			worker.execute();
		}
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
		selectedRecording.setBad(!selectedRecording.isBad());
		
		updateRecordingList();
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
			
			targetDataLine.close();
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
		// dialog code modified from https://code.makery.ch/blog/javafx-dialogs-official/
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Delete selected Creations");
		alert.setHeaderText(null);
		alert.setContentText("Are you sure you want to delete " + selectedCreations.size() + " Creations, "
				+ "as well as their Recordings?");

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK) {
			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

				@Override
				protected Void doInBackground() throws Exception {
					for (Creation creation: selectedCreations) {
						creations.deleteCreation(creation);
						
						creation.delete();

						// must update ui from 'edt' of javafx
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								updateCreationList();
								creationList.getSelectionModel().clearSelection();
								creationList.getSelectionModel().selectFirst();
								updateRecordingList();
							}
						});
					}
					
					return null;
				}
				
			};
			
			worker.execute();
		}
	}

	public void handlePlaySelectedCreations(){

	}

	public void handleNewRecording(){
		// dialog code modified from https://code.makery.ch/blog/javafx-dialogs-official/
		Alert alert1 = new Alert(AlertType.CONFIRMATION);
		alert1.setTitle("Create a new Recording");
		alert1.setHeaderText("Create a new Recording for Creation " + selectedCreation.getName());
		alert1.setContentText("Press OK to start recording for 5 seconds.");

		Creation selectedCreationAtInstant = selectedCreation;
		String name = selectedCreation.getName() + "-" + System.currentTimeMillis();

		Optional<ButtonType> result = alert1.showAndWait();
		if (result.get() == ButtonType.OK) {
			Alert alert2 = new Alert(AlertType.INFORMATION);
			alert2.setTitle("Now recording");
			alert2.setHeaderText(null);
			alert2.setContentText("Say the name " + selectedCreation.getName() + " now. You have 5 seconds.");

			Button alert2OK = (Button) alert2.getDialogPane().lookupButton(ButtonType.OK);
			alert2OK.setDisable(true);
			
			alert2.show();
			
			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() throws Exception {


					// linux version
					ProcessBuilder arecordProcessBuilder = new ProcessBuilder("bash", "-c",
							"arecord -d 5 userdata" + File.separator + name + ".wav");
					// windows version
					/*ProcessBuilder arecordProcessBuilder = new ProcessBuilder("cmd", "/c",
							"ffmpeg -y -t 5 -f dshow -i audio=\"Microphone (Realtek High Definition Audio)\" userdata"
									+ File.separator + name + ".wav");*/
					
					
					Process arecordProcess = arecordProcessBuilder.start();

					arecordProcess.waitFor();

					return null;
				}

				@Override
				protected void done() {
					selectedCreationAtInstant.addRecording(new Recording(selectedCreationAtInstant, new File(
							"userdata" + File.separator + name + ".wav")));
					creations.saveState();

					// must update ui from 'edt' of javafx
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							alert2.close();
							
							updateRecordingList();
						}
					});
				}
			};

			worker.execute();
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

		// the first listener detects the last selected creation
		creationList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Creation>() {
			@Override
			public void changed(ObservableValue<? extends Creation> observable, Creation oldValue, Creation newValue) {
				selectedCreation = newValue;
				updateRecordingList();
				
				recordingList.getSelectionModel().selectFirst();
			}
		});

		/* the second listener records any change in the overall selection of multiple creations */
		creationList.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<Creation>() {
			@SuppressWarnings("unchecked")
			@Override
			public void onChanged(Change<? extends Creation> change) {
				selectedCreations = (ObservableList<Creation>) change.getList();
				
				/* workaround for what seems to be a bug in the jdk when you attempt
				 * a certain sequence of list selections: 
				 * https://bugs.openjdk.java.net/browse/JDK-8173986 */
				if (selectedCreations.size() > 0 && selectedCreations.get(0) == null) {
					ArrayList<Creation> oneSelectedCreation = new ArrayList<Creation>();
					oneSelectedCreation.add(selectedCreation);
					
					selectedCreations = FXCollections.observableArrayList(oneSelectedCreation);
				}
			}
		});

		if (creations.getCreations().size() > 0) {
			selectedCreation = creations.getCreations().get(0);
		}

		creationList.getSelectionModel().selectFirst();
		
		updateRecordingList();

		recordingList.getSelectionModel().selectFirst();
	}
	
	/**
	 * Updates the list of creations when some are removed or added.
	 */
	private void updateCreationList() {
		creationDataList = FXCollections.observableArrayList(creations.getCreations());
		creationList.setItems(creationDataList);
	}

	/**
	 * Updates the list of recordings to only show those from the currently selected creations.
	 */
	private void updateRecordingList() {
		// this can only trigger if there are no creations stored in the application
		if (creations.getCreations().size() == 0) {
			return;
		}

		List<Recording> recordings = new ArrayList<Recording>();
		for (Creation creation: selectedCreations) {
			if (creation == null) {
				continue;
			}
			
			recordings.addAll(creation.getRecordings());
		}

		recordingDataList = FXCollections.observableArrayList(recordings);
		recordingList.setItems(recordingDataList);
		recordingList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// detects the last selected recording
		recordingList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Recording>() {
			@Override
			public void changed(ObservableValue<? extends Recording> observable, Recording oldValue, Recording newValue) {
				selectedRecording = newValue;
			}
		});

		/* records any change in the overall selection of multiple recordings */
		recordingList.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<Recording>() {
			@SuppressWarnings("unchecked")
			@Override
			public void onChanged(Change<? extends Recording> change) {
				selectedRecordings = (ObservableList<Recording>) change.getList();
				
				/* workaround for what seems to be a bug in the jdk when you attempt
				 * a certain sequence of list selections: 
				 * https://bugs.openjdk.java.net/browse/JDK-8173986 */
				if (selectedRecordings.size() > 0 && selectedRecordings.get(0) == null) {
					ArrayList<Recording> oneSelectedRecording = new ArrayList<Recording>();
					oneSelectedRecording.add(selectedRecording);
					
					selectedRecordings = FXCollections.observableArrayList(oneSelectedRecording);
				}
			}
		});
		
		recordingList.refresh();
	}
}

