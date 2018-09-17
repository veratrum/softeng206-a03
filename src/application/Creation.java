package application;

import java.util.ArrayList;
import java.util.List;

public class Creation {

	private String name;
	private int bestRecordingIndex;
	private List<Recording> recordings;
	
	public Creation(String name) {
		this.name = name;
		this.bestRecordingIndex = -1;
		this.recordings = new ArrayList<Recording>();
	}
	
	public String getName() {
		return name;
	}
	
	public void addRecording(Recording recording) {
		recordings.add(recording);
	}
	
	public void setBestRecordingIndex(int index) {
		this.bestRecordingIndex = index;
	}
	
	public int getBestRecordingIndex() {
		return bestRecordingIndex;
	}
	
	public Recording getBestRecording() {
		if (bestRecordingIndex == -1 || recordings.size() == 0) {
			return null;
		}
		
		return recordings.get(bestRecordingIndex);
	}
	
	public List<Recording> getRecordings() {
		return recordings;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
