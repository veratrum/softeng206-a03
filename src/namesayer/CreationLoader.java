package namesayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CreationLoader {

	private Creations creations;
	private File userdata;
	private File metadata;
	
	public CreationLoader(Creations creations) {
		this.creations = creations;
	}
	
	/**
	 * Loads creation names, recording file paths, each creation's 'best' recording,
	 * and recording quality, that was saved previously as XML.
	 * 
	 * If this is run for the first time (and there is no stored data yet), data will
	 * be generated from the .wav files we were given.
	 */
	public void loadMetadata() {
		/* create the userdata directory if it does not already exist.
		this shouldn't need to happen unless the application is run
		without the folder of wavs we were given */
		userdata = new File("userdata");
		if (!userdata.exists()) {
			userdata.mkdir();
		}
		
		metadata = new File("userdata" + File.separator + "metadata.xml");
		
		if (!metadata.exists()) {
			firstExecution();
		} else {
			/* modified from http://www.tutorialspoint.com/java_xml/java_dom_parse_document.htm
			this block parses metadata.xml and populates the list of creations and their recordings */
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document document = builder.parse(metadata);
				document.getDocumentElement().normalize();
				
				NodeList creationNodes = document.getElementsByTagName("creation");
				
				for (int i = 0; i < creationNodes.getLength(); i++) {
					Node creationNode = creationNodes.item(i);
					Element creationElement = (Element) creationNode;
					
					String creationName = creationElement.getAttribute("name");
					
					NodeList recordingNodes = creationElement.getElementsByTagName("recording");
					
					Creation creation = new Creation(creationName);
					
					for (int j = 0;j < recordingNodes.getLength(); j++) {
						Node recordingNode = recordingNodes.item(j);
						Element recordingElement = (Element) recordingNode;
						
						String recordingPath = recordingElement.getTextContent();
						File recordingFile = new File("userdata" + File.separator + recordingPath);
						
						String isBadString = recordingElement.getAttribute("bad");
						boolean isBad = isBadString.equals("y");
						
						Recording recording = new Recording(creation, recordingFile, isBad);
						creation.addRecording(recording);
					}
					
					creations.addCreationWithoutSaving(creation);
				}
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * This is only run the first time the application is run, and there is no metadata.xml.
	 * Sets up a creation with a recording for every .wav in the userdata folder.
	 * .wavs of duplicate names are merged into the same creation.
	 * 
	 * An example filename is:
	 * se206_2-5-2018_15-23-50_Mason.wav
	 * 
	 * We want to strip the first part leaving just 'Mason' as the creation name.
	 */
	private void firstExecution() {		
		// modified from https://stackoverflow.com/a/4917347
		File[] wavs = userdata.listFiles();
		if (wavs != null) {
			for (File wav: wavs) {
				String filename = wav.getName();
				
				// find the extension of a file and make sure it is .wav
				String[] extensionFragments = filename.split("\\.");
				if (extensionFragments.length > 0 &&
						extensionFragments[extensionFragments.length - 1].equals("wav")) {
					// find the proper name of the recording, stripping out unnecessary parts
					String[] nameFragments = extensionFragments[0].split("_");
					String properName = nameFragments[nameFragments.length - 1];
					
					// capitalise first letter (https://stackoverflow.com/a/3904607)
					properName = properName.substring(0, 1).toUpperCase() + properName.substring(1);
					
					// if a creation with that name already exists, add the recording to it
					if (creations.creationExists(properName)) {
						Recording newRecording = new Recording(creations.getCreationByName(properName), wav);
						creations.getCreationByName(properName).addRecording(newRecording);
					} else {
						// otherwise, set up a new recording
						Creation newCreation = new Creation(properName);
						Recording newRecording = new Recording(newCreation, wav);
						newCreation.addRecording(newRecording);
						
						creations.addCreationWithoutSaving(newCreation);
					}
				}
			}
		}
		
		// generate first metadata.xml with the new creations
		saveMetadata();
	}
	
	/**
	 * Saves data representing which recordings belong to which creation,
	 * which recordings are best, and which are low quality.
	 * Format used is XML.
	 */
	public void saveMetadata() {
		// modified from http://www.tutorialspoint.com/java_xml/java_dom_create_document.htm
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.newDocument();

			Element rootElement = document.createElement("creations");
			document.appendChild(rootElement);
			
			for (Creation creation: creations.getCreations()) {
				Element creationElement = document.createElement("creation");
				
				Attr nameAttr = document.createAttribute("name");
				nameAttr.setValue(creation.getName());
				creationElement.setAttributeNode(nameAttr);
				
				for (int i = 0; i < creation.getRecordings().size(); i++) {
					Recording recording = creation.getRecordings().get(i);
					
					Element recordingElement = document.createElement("recording");
					
					Attr isBadAttr = document.createAttribute("bad");
					isBadAttr.setValue(recording.isBad() ? "y" : "n");
					recordingElement.setAttributeNode(isBadAttr);
					
					recordingElement.appendChild(document.createTextNode(recording.getFile().getName()));
					
					creationElement.appendChild(recordingElement);
				}
				
				rootElement.appendChild(creationElement);
			}
			
			// generate xml and write it to disk
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(metadata);
			transformer.transform(source, result);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		saveSeparateRatingsFile();
	}
	
	/**
	 * Saves the rating data in a text file that is more simply formatted, for use in
	 * other scripts, etc.
	 * 
	 * We weren't sure if this was necessary because the specification was somewhat vague,
	 * but we decided to include it just in case.
	 */
	public void saveSeparateRatingsFile() {
		String data = "";
		for (Creation creation: creations.getCreations()) {
			for (Recording recording: creation.getRecordings()) {
				data += recording.getFile().getName();
				
				if (recording.isBad()) {
					data += " BAD";
				} else {
					data += " GOOD";
				}
				
				data += System.lineSeparator();
			}
		}
		
		File ratingsText = new File("userdata" + File.separator + "ratings.txt");
		try {
			ratingsText.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// https://stackoverflow.com/a/1053475
		try {
			PrintWriter out = new PrintWriter(ratingsText);
			out.write(data);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
