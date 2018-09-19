package application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Creation {

	private String name;
	private List<Recording> recordings;
	
	public Creation(String name) {
		this.name = name;
		this.recordings = new ArrayList<Recording>();
	}
	
	public void delete() {
		for (Recording recording: recordings) {
			recording.delete();
		}
	}

	public void removeRecording(Recording recording) {
		recordings.remove(recording);
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
	
	public String generateRecordingFilename() {
		LocalDateTime now = LocalDateTime.now();
		String filename = "se206_" + now.getDayOfMonth() + "-" + now.getMonthValue() + "-" + now.getYear() + "_" + now.getHour()
				+ "-" + now.getMinute() + "-" + now.getSecond() + "_" + name + ".wav";
		
		return filename;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
