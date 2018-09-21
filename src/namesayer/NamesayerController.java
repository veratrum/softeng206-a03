package namesayer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextInputDialog;

public class NamesayerController implements Initializable {

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
	
	private boolean isRecording;

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

	public void PlayAudio(File recordingToPlay) {

		Clip clip;
		try {
			clip = AudioSystem.getClip();
			clip.open(AudioSystem.getAudioInputStream(recordingToPlay));
			clip.start();
			Thread.sleep(clip.getMicrosecondLength()/1000);
		} catch (Exception e) {

		}
	}
	


	public void handleDeleteRecording(){
		// dialog code modified from https://code.makery.ch/blog/javafx-dialogs-official/
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Delete selected Recordings");
		alert.setHeaderText(null);
		alert.setContentText("Are you sure you want to delete " + selectedRecordings.size() + " Recordings?");

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK) {
			Thread deleteRecordings = new Thread(new Task<Void>() {

				@Override
				protected Void call() throws Exception {
					for (Recording recording: selectedRecordings) {
						recording.delete();
						recording.removeSelf();
					}
					
					creations.saveState();

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

			});
			deleteRecordings.start();
		}
	}

	public void handlePlaySelectedRecordings(){
		if (selectedRecordings.size() == 0) {
			return;
		} else if (selectedRecordings.size() == 1) {
			//Recording to play
			if (selectedRecording != null) {
				// Playing audio in background thread
				Task<Void> task = new Task<Void>() {
					@Override
					protected Void call() throws Exception {
						File recordingToPlay = selectedRecording.getFile();
						PlayAudio(recordingToPlay);
						return null;
					}
				};
				new Thread(task).start();
			}
		}
		else {


			// ask the user if they want to randomise the order
			Alert confirmation = new Alert(AlertType.CONFIRMATION);
			confirmation.setTitle("Play Recordings");
			confirmation.setHeaderText("Play Multiple Recordings");
			confirmation.setContentText("You have selected to play multuple recordings\ndo you wish to shuffle the order the selected\nrecordings are played in?");
			ButtonType buttonYes = new ButtonType("Yes");
			ButtonType buttonNo = new ButtonType("No");
			confirmation.getButtonTypes().setAll(buttonYes, buttonNo);
			Optional<ButtonType> result = confirmation.showAndWait();


			Task<Void> task = new Task<Void>() {
				@Override
				protected Void call() throws Exception {
					// If yes - then we need to shuffle the order of the selected recordings.
					List<Recording> recordingsToPlay = new ArrayList<Recording>();
					if (result.get() == buttonYes){

						// Copying the selected to recordings to a list that can be shuffled as the other list is non-modifiable
						for(int i = 0; i < selectedRecordings.size(); i++) {
							recordingsToPlay.add(selectedRecordings.get(i));
						}

						// Shuffling the recordings
						Collections.shuffle(recordingsToPlay);
					}
					else {
						// Copying the selected to recordings to a list of the same name as the one that gets shuffled for consistency.
						for(int i = 0; i < selectedRecordings.size(); i++) {
							recordingsToPlay.add(selectedRecordings.get(i));
						}
					}

					for (Recording currentRecording:recordingsToPlay) {
						File recordingToPlay = currentRecording.getFile();
						PlayAudio(recordingToPlay);
					}

					return null;
				}
			};
			new Thread(task).start();
		}
	}

	public void handleRate(){
		for (Recording recording: selectedRecordings) {
			recording.setBad(!recording.isBad());
		}
		
		creations.saveState();

		recordingList.refresh();
	}

	public void handleTestMicrophone(){

		// Alert the user of what is happening
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Microphone Test");
		alert.setHeaderText("Microphone Test");
		alert.setContentText("Please speak for 3 seconds and the test\nwill average the input microphone volume.");
		alert.showAndWait();


		Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
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
								double micLevel = max/1000.0; //Note: this calculation for mic volume test is based on an approximation of what I determined to be low/average/loud speaking volume.
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
					return null;
				}

				// Setting the progressBar mic Input level
				progressBar.setProgress(micSum/25);

				return null;
			}
		};

		new Thread(task).start();

		// Create a new background thread to reset the mic volume meter after 5 seconds.
		Task<Void> backgroundThread = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				Thread.sleep(5000);
				progressBar.setProgress(0.0);

				return null;
			}
		};
		new Thread(backgroundThread).start();

	}

	public void handleDeleteCreations(){
		// dialog code modified from https://code.makery.ch/blog/javafx-dialogs-official/
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Delete selected Names");
		alert.setHeaderText(null);
		alert.setContentText("Are you sure you want to delete " + selectedCreations.size() + " Names, "
				+ "as well as their Recordings?");

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK) {
			Thread deleteCreation = new Thread(new Task<Void>() {

				@Override
				protected Void call() throws Exception {
					for (int i = 0; i < selectedCreations.size(); i++) {
						Creation creation = selectedCreations.get(i);
						
						creations.deleteCreation(creation);

						creation.delete();
					}
					
					return null;
				}
				
				@Override
				protected void done() {
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

			});
			deleteCreation.start();
		}
	}
	
	public void handlePlayAllRecordingsForSelectedNames(){

		// If there are no selected creations finish function call.
		if (selectedCreations.size() == 0) {
			return;
		}
		// If there are more than one selected handle it correctly.
		else if (selectedCreations.size() > 0) {

			// ask the user if they want to randomise the order
			Alert confirmation = new Alert(AlertType.CONFIRMATION);
			confirmation.setTitle("Play Recordings");
			confirmation.setHeaderText("Play Multiple Recordings");
			confirmation.setContentText("You have selected to play multuple recordings\ndo you wish to shuffle the order the selected\nrecordings are played in?");
			ButtonType buttonYes = new ButtonType("Yes");
			ButtonType buttonNo = new ButtonType("No");
			confirmation.getButtonTypes().setAll(buttonYes, buttonNo);
			Optional<ButtonType> result = confirmation.showAndWait();

			// Playing the audio in a background thread so the GUI doesn't freeze
			Task<Void> task = new Task<Void>() {
				@Override
				protected Void call() throws Exception {

					// Create a list to store all recordings to be played.
					List<Recording> allRecordingsToPlay = new ArrayList<Recording>();

					for(Creation currentCreation:selectedCreations) {

						// Get the recordings for the current creation
						List<Recording> currentCreationRecordings = currentCreation.getRecordings();

						for(Recording currentRecording:currentCreationRecordings) {
							// Store all recordings for the current recording in the allRecordings list.
							allRecordingsToPlay.add(currentRecording);
						}
					}

					// If the user selected yes then we need to randomise the list
					if (result.get() == buttonYes){
						Collections.shuffle(allRecordingsToPlay);
					}

					// Now iterate over the list and play all the recordings.
					for (Recording currentRecording : allRecordingsToPlay) {
						File recordingToPlay = currentRecording.getFile();
						PlayAudio(recordingToPlay);
					}
					return null;
				}
			};

			new Thread(task).start();

		}
	}

	public void handleNewRecording(){
		// dialog code modified from https://code.makery.ch/blog/javafx-dialogs-official/
		Alert alert1 = new Alert(AlertType.CONFIRMATION);
		alert1.setTitle("Create a new Recording");
		alert1.setHeaderText("Create a new Recording for Name " + selectedCreation.getName());
		alert1.setContentText("Press OK to start recording for 5 seconds.");

		Optional<ButtonType> result = alert1.showAndWait();
		if (result.get() == ButtonType.OK) {
			doNewRecording(selectedCreation);
		}
	}

	public void handleNewCreation(){
		TextInputDialog enterName = new TextInputDialog("");
		
		enterName.setTitle("Create a new Name");
		enterName.setHeaderText(null);
		enterName.setContentText("Enter a name:");
		
		Optional<String> nameResult = enterName.showAndWait();
		if (nameResult.isPresent()) {
			String newName = nameResult.get();
			
			if (creations.creationExists(newName)) {
				Alert alert1 = new Alert(AlertType.CONFIRMATION);
				alert1.setTitle("Create a new Name");
				alert1.setHeaderText("Name " + newName + " already exists.");
				alert1.setContentText("Would you like to add a new Recording to Name " + newName + "?");
				
				Optional<ButtonType> result1 = alert1.showAndWait();
				if (result1.get() == ButtonType.OK) {
					doNewRecording(creations.getCreationByName(newName));
				}
			} else {
				Creation newCreation = new Creation(newName);
				
				creations.addCreation(newCreation);
				
				Alert alert1 = new Alert(AlertType.CONFIRMATION);
				alert1.setTitle("Create a new Name");
				alert1.setHeaderText("Created Name " + newName + " successfully.");
				alert1.setContentText("Would you like to add a new Recording to Name\n" + newName + "?");
				
				updateCreationList();
				
				creationList.getSelectionModel().clearSelection();
				creationList.getSelectionModel().select(newCreation);
				
				Optional<ButtonType> result1 = alert1.showAndWait();
				if (result1.get() == ButtonType.OK) {
					doNewRecording(creations.getCreationByName(newName));
				}
			}
		}
	}
	
	/**
	 * Helper method to be reused
	 */
	private void doNewRecording(Creation parentCreation) {
		Creation selectedCreationAtInstant = parentCreation;
		String filename = parentCreation.generateRecordingFilename();
		
		Alert alert2 = new Alert(AlertType.INFORMATION);
		alert2.setTitle("Now recording");
		alert2.setHeaderText(null);
		alert2.setContentText("Say the name " + selectedCreationAtInstant.getName() + " now. You have 5 seconds.");

		Button alert2OK = (Button) alert2.getDialogPane().lookupButton(ButtonType.OK);
		alert2OK.setDisable(true);

		alert2.show();

		// stop recording after 5 seconds
		isRecording = true;
		Thread wait5Seconds = new Thread(new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				Thread.sleep(5000);
				isRecording = false;
				
				return null;
			}
		});
		wait5Seconds.start();
		
		// this thread runs concurrently to wait5Seconds, and stops when 5 seconds have passed
		Thread recordAudio = new Thread(new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				AudioFormat audioFormat = getAudioFormat();

				// modified from https://docs.oracle.com/javase/tutorial/sound/capturing.html
				TargetDataLine targetDataLine;
				try {
					targetDataLine = (TargetDataLine) AudioSystem.getTargetDataLine(audioFormat);

					targetDataLine.open();
					
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					int numBytesRead;
					byte[] data = new byte[targetDataLine.getBufferSize() / 5];
					
					targetDataLine.start();
					
					while (isRecording) {
						numBytesRead = targetDataLine.read(data, 0, data.length);
						
						out.write(data, 0, numBytesRead);
					}
					
					targetDataLine.close();
					
					// write the output to a file
					FileOutputStream fileStream = new FileOutputStream("userdata" + File.separator + filename);
					
					out.writeTo(fileStream);
					
					out.close();
					fileStream.close();
				} catch (LineUnavailableException e) {
					e.printStackTrace();
				}

				return null;
			}
			
			
			@Override
			protected void done() {
				selectedCreationAtInstant.addRecording(new Recording(selectedCreationAtInstant, new File(
						"userdata" + File.separator + filename)));
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
		});
		recordAudio.start();
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
				
				recordingList.getSelectionModel().clearSelection();
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

