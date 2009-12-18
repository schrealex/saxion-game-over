package nl.saxion.gameover;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class ScannerServiceImpl {
	private ArrayList<File> files = new ArrayList<File>();

	private ArrayList<String> questionsList = new ArrayList<String>();
	private ArrayList<String> answersList = new ArrayList<String>();
	private ArrayList<String> correctAnswersList = new ArrayList<String>();
	private ArrayList<String> mixedAnswers = new ArrayList<String>();

	private int fileSize = 0;
	private int random = 1;

	// Keep the correct answer number
	int answerNr = 0;

	// The answers set
	int answersSet = 4;

	public ScannerServiceImpl(String inputDir, String outputDir, Map<String, String> settings) {
		if (inputDir != null && !inputDir.equals("") && outputDir != null && !outputDir.equals("")) {
			outputToFile(inputDir, outputDir, settings);
		}
		// System.err.println("Totaal aantal files: " + fileSize);
	}

	public void outputToFile(String fileDir, String outputDir, Map<String, String> settings) {
		// System.err.println("Files directory: " + fileDir);
		// System.err.println("Output directory: " + outputDir + "\n");

		// File(s) ophalen uit de opgegeven directory
		getFiles(new File(fileDir), settings);

		for (File file : files) {
			File csv = new File(file.getParent() + File.separator + new Scanner(file.getName()).findWithinHorizon(".*(?=\\.)", 0) + ".csv");

			// Controleer of een file
			if (files.contains(csv)) {
				if (getExtension(file.getName()).equalsIgnoreCase("csv")) {
					writeToFile(file, outputDir, false);
				}
			} else {
				System.err.println("No files found!");
			}
		}
		// Sla eerst de size van files op in een een variabele om bij te houden
		// hoeveel files er gescanned zijn
		fileSize += files.size();
		// Gooi de lijst met files weer leeg
		files.removeAll(files);
	}

	/**
	 * Methode om de gevonden woorden of teksten weg te schrijven naar een XML
	 * file
	 * 
	 * @param file
	 *            De weg te schrijven file
	 * @param regexp
	 *            De reguliere expressie die wordt gebruikt om de woorden te
	 *            zoeken in de file
	 * @param outputDir
	 *            De directory waar het weg te schrijven bestand naar toe wordt
	 *            geschreven
	 * @param delete
	 *            een boolean waarmee wordt bepaald of de lijst met matches
	 *            wordt leegggegooid
	 */
	public void writeToFile(File file, String outputDir, boolean delete) {
		// Maak een nieuwe output directory aan waar de files neergezet worden
		// outputDir is de hoofdmap waar je de files neer wilt zetten
		File dir = new File(outputDir);

		try {
			String filePath = file.getPath();
//			 System.err.println("File: " + filePath);

			// Check if the directory doesnt exist, else create it
			if (!dir.exists()) {
				// System.err.println("Directory '" + dir +
				// "' doesn't exist and will be created.");
				dir.mkdirs();
			}
			else {
				// System.err.println("Directory '" + dir + "' already exists");
			}

			// System.err.println("Directory where the file(s) are outputted: "
			// + dir);
			FileOutputStream out = new FileOutputStream(dir + File.separator + changeExtension(file).replace(" ", "_"));

			// Read out the questions
			readQuestions(filePath);

			String xml = buildXML();

			if (xml != null) {
				out.write(xml.getBytes());
				out.flush();
			}
		}
		catch (IOException e) {
			System.err.println("Input/Output exeception" + e);
		}
	}

	private void readQuestions(String filePath) {
		try {

			FileReader fr = new FileReader(filePath);
			BufferedReader br = new BufferedReader(fr);

			List<String[]> splitLines = new ArrayList<String[]>();
			String line = "";
			while ((line = br.readLine()) != null) {
				splitLines.add(line.split(";"));
			}

			for (String[] s : splitLines) {
				for (int i = 0; i < s.length; i++) {
					if (i == 0) {
						questionsList.add(s[i]);
					}
					else if (i > 0 && i < 5) {
						if (i == 1) {
							correctAnswersList.add(s[i]);
						}
						answersList.add(s[i]);
					}
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String buildXML() {
		try {
			// ///////////////////////////
			// Creating an empty XML Document

			// We need a Document
			DocumentBuilderFactory docbfac = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docbfac.newDocumentBuilder();
			Document doc = docBuilder.newDocument();

			// //////////////////////
			// Creating the XML tree

			// Create the root questions element and add it to the document
			Element root = doc.createElement("questions");
			doc.appendChild(root);

			// Keep the number of the question
			int qNumber = 1;

			// Keep the number of the correct answer
			int cAnswer = 0;

			// Create questions
			for (String q : questionsList) {

				// Fill mixed answers list
				randomizeAnswerOrder();

				// Create a comment and put it in the questions element
				Comment comment = doc.createComment(" Question number: " + qNumber + " ");
				root.appendChild(comment);

				// Create question number element, add an attribute, and add to
				// questions
				Element questionNr = doc.createElement("question");
				questionNr.setAttribute("number", qNumber++ + "");
				root.appendChild(questionNr);

				// Create question type element, add an attribute, and add to
				// question number
				Element questionType = doc.createElement("questionType");
				String qTypeString = "";

				if (!mixedAnswers.contains("(4 afbeeldingen)")) {
					questionType.setAttribute("typeNr", "1");
					questionNr.appendChild(questionType);
					qTypeString = "Text answers type";
				} else {
					questionType.setAttribute("typeNr", "2");
					questionNr.appendChild(questionType);					
					qTypeString = "Image answers type";
				}

				// Add a text element to the question type
				Text qTypeText = doc.createTextNode(qTypeString);
				questionType.appendChild(qTypeText);

				// Create image element, add an attribute, and add to question
				// number
				Element image = doc.createElement("image");
				questionNr.appendChild(image);

				// Add a text element to the question type
				Text imageText = doc.createTextNode("image url");
				image.appendChild(imageText);

				// Create answers element, add an attribute, and add to question
				// number
				Element question = doc.createElement("question");
				questionNr.appendChild(question);

				// Add a text element to the question type
				Text qText = doc.createTextNode(q);
				question.appendChild(qText);

				// Create answers element, add an attribute, and add to question
				// number
				Element answers = doc.createElement("answers");
				questionNr.appendChild(answers);
				
				String correctAnswerText = "";
				String answerText = "";

				for (int a = 0; a < mixedAnswers.size(); a++) {
					
					int nr = a + 1;
					answerText = nr + "";
					
					if(correctAnswersList.get(cAnswer).equals(mixedAnswers.get(a))) {
						correctAnswerText = answerText;						
					}					

					// Create answer element, add an attribute, and add to
					// question number					
					Element answer = doc.createElement("answer");
					answer.setAttribute("number", answerText);
					answers.appendChild(answer);

					// Add a text element to the question type
					Text aText = doc.createTextNode(mixedAnswers.get(a));
					answer.appendChild(aText);									
				}
				cAnswer++;
				
				
				// Create answer element, add an attribute, and add to question
				// number
				Element correctAnswer = doc.createElement("correctAnswer");
				questionNr.appendChild(correctAnswer);
				
				// Add a text element to the correct answer
				Text cAText = doc.createTextNode(correctAnswerText);
				correctAnswer.appendChild(cAText);
				
				mixedAnswers.clear();
			}

			// ///////////////
			// Output the XML

			// Set up a transformer
			TransformerFactory transfac = TransformerFactory.newInstance();
			Transformer trans = transfac.newTransformer();
			trans.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "html");
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.setOutputProperty(OutputKeys.STANDALONE, "yes");
			trans.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
			trans.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "publicId");

			// Create string from XML tree
			DOMSource source = new DOMSource(doc);
			StringWriter sw = new StringWriter();
			StreamResult result = new StreamResult(sw);

			trans.transform(source, result);
			
			return sw.toString();

		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void randomizeAnswerOrder() {
		for (; answerNr < answersSet; answerNr++) {
			int place = (int) (Math.random() * random);

			if (mixedAnswers.size() == 0) {
				mixedAnswers.add(place, answersList.get(answerNr));
				random++;
			}
			else if (mixedAnswers.size() == 1) {
				mixedAnswers.add(place, answersList.get(answerNr));
				random++;
			}
			else if (mixedAnswers.size() == 2) {
				mixedAnswers.add(place, answersList.get(answerNr));
				random++;
			}
			else if (mixedAnswers.size() == 3) {
				mixedAnswers.add(place, answersList.get(answerNr));
				random = 1;
				place = 0;
			}
		}
		answersSet += 4;
	}

	/**
	 * @param filename
	 *            is de bestandsnaam waarvan de extensie wordt veranderd
	 * @return newFileName is de bestandsnaam met de nieuwe extensie
	 */
	public String changeExtension(File file) {
		String fileName = "";
		if (file.getName().length() > 0) {
			fileName = file.getName().substring(0, (file.getName().lastIndexOf(".") + 1)) + "xml";
		}
		else {
			System.err.println("Geen correcte file opgegeven!");
		}
		return fileName;
	}

	/**
	 * Methode om .csv files uit de opgegeven directory te zoeken en op te slaan
	 * in een lijst met files
	 * 
	 * @param file
	 */
	public void getFiles(File file, Map<String, String> settings) {
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				if (!f.isDirectory() && f.getName().contains(".") && getExtension(f.getName()).equalsIgnoreCase("csv")) {
					// Voeg CSV files toe
					files.add(f);
				}
			}
		}
		else {
			System.err.println("Error");
		}
	}

	/**
	 * Geef de bestandsextensie van de meegegeven filenaam met extensie
	 * 
	 * @param filename
	 *            de file waarvan de extensie wordt teruggegeven
	 * @return de bestandsextensie van de meegegeven file
	 */
	public String getExtension(String filename) {
		return filename.substring(filename.lastIndexOf(".") + 1, filename.length());
	}

	// -------------------------------------------------//
	// Filter voor het scannen van woorden of teksten //
	// -------------------------------------------------//

	/**
	 * Controleer of de meegegeven reguliere expressie matcht aan de meegegeven
	 * match
	 * 
	 * @param match
	 *            is de String waarvan wordt gecontroleerd of deze aan de
	 *            reguliere expressie matcht
	 * @return true wanneer de match aan de reguliere expressie matcht
	 */
	public boolean filterMatch(String match, String regexp) {
		return match.toLowerCase().matches(regexp);
	}
}