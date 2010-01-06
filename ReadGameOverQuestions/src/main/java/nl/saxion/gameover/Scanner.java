/**
 * How to use
 * 
 * Give in your configuration file you want to be read
 */

package nl.saxion.gameover;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

public class Scanner {
	private static Map<String, String> settings = new HashMap<String, String>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length > 0 && args[0] != null) {
			loadProperties(new File(args[0]));
			if (settings.containsKey("inputDir") && settings.containsKey("outputDir")) {
				new ScannerServiceImpl(settings.get("inputDir"), settings.get("outputDir"), settings);
			} else {
				printUsage(1);
			}
		} else {
			printUsage(2);
		}
	}

	/**
	 * Methode om de properties file in te laden en vervolgens alle keys en
	 * values op te slaan in een map aan de hand van een key en value
	 * 
	 * @param propertiesFile
	 *            De file waarvan de properties worden ingeladen
	 */
	public static void loadProperties(File propertiesFile) {
		Properties p = new Properties();
		try {
			p.load(new FileInputStream(propertiesFile));

			for (Entry<Object, Object> e : p.entrySet()) {
				if (!e.getKey().equals("") && !e.getValue().equals("")) {
					settings.put(e.getKey().toString(), e.getValue().toString());
				}
			}
		} catch (FileNotFoundException e) {
			System.err.println("Properties file not found: " + e);
		} catch (IOException e) {
			System.err.println("File input stream exception: " + e);
		}
	}

	/**
	 * Functie die het gebruik van de applicatie (syntax) weergeeft
	 * 
	 * @param usage
	 *            Een int waarde die bepaald welke informatie wordt weergegeven
	 */
	private static void printUsage(int usage) {
		if (usage == 1) {
			System.err.println("Configuratie mist één of meerdere properties!");
		} else {
			System.err.println("Gebruik: Scanner <argument>");
			System.err.println("Verplichte argumenten:\n");
			System.err.println("Configuratie bestand <config file> : Het te gebruiken configuratie bestand");
		}
	}
}