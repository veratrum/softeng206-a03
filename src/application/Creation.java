package application;

import java.util.ArrayList;
import java.util.List;

public class Creation {

	private String name;
	private List<Recording> recordings;
	
	public Creation(String name) {
		this.name = name;
		this.recordings = new ArrayList<Recording>();
	}
	
	public String getName() {
		return name;
	}
	
	public void addRecording(Recording recording) {
		recordings.add(recording);
	}
	
	public List<Recording> getRecordings() {
		return recordings;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
