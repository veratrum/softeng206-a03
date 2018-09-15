package application;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingWorker;

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
		
		creationLoader.saveMetadata();
	}
	
	/**
	 * This method should be called only by CreationLoader, instead of addCreation.
	 * We don't want to overwrite metadata.xml with incomplete data while we are reading from it.
	 */
	public void addCreationWithoutSaving(Creation creation) {
		this.creations.add(creation);
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
}
