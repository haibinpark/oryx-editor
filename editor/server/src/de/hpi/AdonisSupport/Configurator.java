package de.hpi.AdonisSupport;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * this class provides access to the configuration file
 * to provide support for different adonis versions (i.e. german or english)
 *
 */
class Translation {
	public String oryxName;
	public String adonisName;
	public String language;
	
	
	public Translation(String oryxName, String adonisName, String language){
		this.oryxName = oryxName;
		this.adonisName = adonisName;
		this.language = language;
	}
	
	public String toString(){
		return "Translation oN "+oryxName+" - aN "+adonisName+" ("+language+")";
		
	}
}

@SuppressWarnings("unused")
public class Configurator {
	//local path is only needed for development
	private static String localPath = "D:\\Development\\Eclipse Oryx\\Oryx\\editor\\etc\\";
	private static String configFile = "adonisStandards.data";
	
	
	private static ArrayList<Translation> shapeTranslations;
	private static Map<String,Map<String,String>> shapeStandards;
		
	private static JSONObject readConfigurations(){
		FileInputStream fileStream = null;
		InputStreamReader fileStreamReader  = null;
		BufferedReader bufferedReader  = null;
		JSONObject values = new JSONObject();
		try {
			try {
				fileStream = new FileInputStream(configFile);
			} catch (FileNotFoundException e){
				String path = localPath+configFile;
				Log.w("configfile not found on filesystem, trying local path\n"+path,e);
				fileStream = new FileInputStream(path);
			}
			fileStreamReader = new InputStreamReader(fileStream,"UTF-8");
			bufferedReader = new BufferedReader(fileStreamReader);
			
			String line = null;
			StringBuilder content = new StringBuilder();

			while ((line = bufferedReader.readLine()) != null){
				content.append(line);
			}
			//System.out.println(content.toString());
			values = new JSONObject(content.toString());
			
			bufferedReader.close();
			fileStreamReader.close();
			fileStream.close();
		
		} catch (Exception e){
			Log.e("Could not initialize config file for Adonis compatibility");
			e.printStackTrace();
			if (bufferedReader != null)
				try {
					bufferedReader.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				} 
			if (fileStreamReader != null )
				try {
					fileStreamReader.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			if (fileStream != null )
				try {
					fileStream.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		}
		return values;
	}
	

	
	/**
	 * read in all mappings from configuration and store them
	 * @throws JSONException
	 */
	public static void initializeMappingTables() {
		if (shapeTranslations != null){
			return;
		}
		shapeTranslations = new ArrayList<Translation>();
		shapeStandards = new HashMap<String,Map<String,String>>();
		
		ArrayList<Translation> locals = null;
		Map<String,String> standards = null;
		JSONObject translations = null;
		JSONObject shape = null;
		JSONObject configuration  = readConfigurations();
		String[] languages = null;
		String[] oryxNames = JSONObject.getNames(configuration);
		String[] attributes = null;
		
		try {
			//read in all stencils
			for (String oryxName : oryxNames){
				// read in translations
				shape = configuration.getJSONObject(oryxName);
				translations = shape.optJSONObject("translation");
				if (translations != null){
					languages = JSONObject.getNames(translations);
					
					for (String language : languages){
						shapeTranslations.add(new Translation(
								oryxName,
								translations.getString(language),
								language));
					}
				}
				// read in the standard values
				standards = new HashMap<String,String>();
				attributes = JSONObject.getNames(shape);
				if (attributes != null){
					for (String attribute : attributes){
						if (!attribute.equals("translation")){
							standards.put(attribute, shape.getString(attribute));
						}
					}
					shapeStandards.put(oryxName, standards);
				}
				
			}
		} catch (JSONException e){
			Log.e("could not create mapping table",e);
		}
	}
	
	public static String getLanguage(String adonisIdentifier){
		initializeMappingTables();
		for (Translation triple : shapeTranslations){
			if (triple.adonisName.equals(adonisIdentifier)){
				return triple.language;
			}
		}
		Log.e("could not find a (oryx) translation for "+adonisIdentifier+" use fall back");
		return "en";
	}
	
	/**
	 * look up the name of adonis identifier in oryx 
	 * @param adonisIdentifier
	 * @return
	 */
	public static String getOryxIdentifier(String adonisIdentifier) {
		initializeMappingTables();
		for (Translation triple : shapeTranslations){
			if (triple.adonisName.equals(adonisIdentifier)){
				return triple.oryxName;
			}
		}
		Log.e("could not find a (oryx) translation for "+adonisIdentifier+" use fall back");
		return adonisIdentifier.toLowerCase();
	}
	
	/**
	 * look up a identifier of oryx in adonis with a given language
	 * @param oryxIdentifier
	 * @param lang
	 * @return
	 */
	public static String getAdonisIdentifier(String oryxIdentifier, String lang){
		initializeMappingTables();
		for (Translation triple : shapeTranslations){
			if (triple.oryxName.equals(oryxIdentifier)
					&& triple.language.equals(lang)){
				return triple.adonisName;
			}
		}
		Log.e("could not find a (adonis) translation for "+oryxIdentifier+" in language "+lang+" - use fallback");
		//adonis mostly uses word starting with upper case
		return oryxIdentifier.substring(0, 0).toUpperCase() + oryxIdentifier.substring(1);
	}
	

	public static String getStandardValue(String oryxName, String attribute, String defaultValue) {
		initializeMappingTables();
		String value = shapeStandards.get(oryxName).get(attribute);
		if (value != null){
			return value;
		}
		Log.e("could not get attribute "+attribute+" of "+oryxName+", use hardcoded value "+defaultValue);
		return defaultValue;
	}

	
	


}
