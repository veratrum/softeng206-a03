package application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Creations {

	private List<Creation> creations;
	private CreationLoader creationLoader;
	
	public Creations() {
		creations = new ArrayList<Creation>();
		creationLoader = new CreationLoader(this);

		creationLoader.loadMetadata();
	}
	
	public void addCreation(Creation creation) {
		this.creations.add(creation);
		
		sortCreations();
		
		creationLoader.saveMetadata();
	}
	
	/**
	 * This method should be called only by CreationLoader, instead of addCreation.
	 * We don't want to overwrite metadata.xml with incomplete data while we are reading from it.
	 */
	public void addCreationWithoutSaving(Creation creation) {
		this.creations.add(creation);
		
		sortCreations();
	}
	
	public void deleteCreation(Creation creation) {
		creations.remove(creation);
		
		sortCreations();
		
		creationLoader.saveMetadata();
	}
	
	public void saveState() {
		creationLoader.saveMetadata();
	}
	
	public List<Creation> getCreations() {
		return creations;
	}
	
	public boolean creationExists(String name) {
		for (Creation creation: creations) {
			if (creation.getName().equals(name)) {
				return true;
			}
		}
		
		return false;
	}
	
	public Creation getCreationByName(String name) {
		for (Creation creation: creations) {
			if (creation.getName().equals(name)) {
				return creation;
			}
		}
		
		return null;
	}
	
	public boolean recordingExists(String filename) {
		for (Creation creation: creations) {
			for (Recording recording: creation.getRecordings()) {
				if (recording.getFile().getName().equals(filename)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public String generateRecordingFilename(String creationName) {
		String filename = creationName + ".wav";
		if (!recordingExists(filename)) {
			return filename;
		}
		
		int tries = 2;
		String repeatName = creationName + " (" + tries + ").wav";
		while (recordingExists(repeatName)) {
			tries++;
			repeatName = creationName + " (" + tries + ").wav";
		}
		
		return repeatName;
	}
	
	/**
	 * Sorts all creations in alphabetical order.
	 */
	private void sortCreations() {
		// adapted from https://stackoverflow.com/a/2784576
		Collections.sort(creations, new Comparator<Creation>() {
			@Override
			public int compare(Creation a, Creation b) {
				return a.getName().compareTo(b.getName());
			}
		});
	}
}
