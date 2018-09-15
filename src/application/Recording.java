package application;

import java.io.File;

public class Recording {

	private File file;
	private boolean isBad;
	
	public Recording(File file) {
		this(file, false);
	}
	
	public Recording(File file, boolean isBad) {
		this.file = file;
		this.isBad = isBad;
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
}
