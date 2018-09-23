package namesayer;

import java.io.File;

public class Recording {

	private Creation creation;
	private File file;
	private boolean isBad;
	
	public Recording(Creation creation, File file) {
		this(creation, file, false);
	}
	
	public Recording(Creation creation, File file, boolean isBad) {
		this.creation = creation;
		this.file = file;
		this.isBad = isBad;
	}
	
	public void delete() {
		file.delete();
	}
	
	public void removeSelf() {
		creation.removeRecording(this);
	}
	
	public void setBad(boolean isBad) {
		this.isBad = isBad;
	}
	
	public boolean isBad() {
		return isBad;
	}
	
	public File getFile() {
		return file;
	}
	
	public Creation getCreation() {
		return creation;
	}
	
	@Override
	public String toString() {
		String representation = file.getName();
		
		if (isBad) {
			representation = "[BAD] " + representation;
		}
		
		return representation;
	}
}
