package icedev.mixamo;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import icedev.mixamo.util.HTTP;

@SuppressWarnings("unchecked")
public class MixamoAPI {
	public static void sleep(int milisec) {
		try {
			Thread.sleep(milisec);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static String extractJobFileName(String jobResult) {
		String unescaped = URLDecoder.decode(jobResult, StandardCharsets.UTF_8);
		Matcher matcher = jobNamePattern.matcher(unescaped);
		matcher.find();
		return matcher.group().substring(51);
	}
	
	public static Pattern jobNamePattern = Pattern.compile("\\bresponse\\-content\\-disposition=.*\\.fbx\\b");
	
	HTTP http;
	
	JSONObject preferences = new JSONObject(); {
		// { format: "fbx7", skin: "false", fps: "60", reducekf: "0" }
		preferences.put("format", "fbx7_unity"); // "fbx7_unity"
		preferences.put("skin", "false");
		preferences.put("fps", "60");
		preferences.put("reducekf", "0"); // 0 no reduction, 1 uniform, 2 non uniform
	}
	
	public MixamoAPI(HTTP client) {
		this.http = client;
	}
	
	public JSONObject fetchPage(int page, int limit) {
		return http.fetch("https://www.mixamo.com/api/v1/products?page=" + page + "&limit="+limit+"&order=date&type=Motion&query=");
	}


	public JSONObject fetchProduct(String animId, String charId) {
		return http.fetch("https://www.mixamo.com/api/v1/products/"+animId+"?similar=0&character_id=" + charId);
	}

	public void exportAnimation(JSONObject gms_hash, String productName, String characterId) {
		JSONArray gmsarray = new JSONArray();
		gmsarray.add(gms_hash);

		JSONObject export = new JSONObject();
		export.put("character_id", characterId);
		export.put("gms_hash", gmsarray);
		export.put("preferences", preferences);
		export.put("product_name", productName);
		export.put("type", "Motion");
		
		http.fetch("https://www.mixamo.com/api/v1/animations/export", export);
		
	}
	
	public String monitorAnimation(String animId, String characterId) {
		while(true) {
			var response = http.fetch("https://www.mixamo.com/api/v1/characters/"+characterId+"/monitor");
			
			String status = (String) response.get("status");
			
			if(status == null) {
				System.err.println(response);
				throw new NullPointerException("status is null");
			}
			
			if(status.equalsIgnoreCase("completed")) {
				String jobResult = (String) response.get("job_result");
				return jobResult;
			}
			
			if(status.equalsIgnoreCase("failed")) {
				return null;
			}
			
			System.out.println("...still processing!");
			sleep(1500);
		}
	}
	

	public void exportAndDownloadAnimation(String name, String id, JSONObject gmsHash, String characterId, File targetFile) {
		exportAnimation(gmsHash, name, characterId);

//		System.out.println("Sleeping...");
		sleep(2500);
		
		String jobResult = monitorAnimation(id, characterId);
		String jobFileName = extractJobFileName(jobResult);
		
		String fileName = name.replaceAll("[<>:\"\\/\\\\|?*]+", "");
		File target = targetFile; // chooseFile(prefix + fileName);

		System.out.println(jobFileName + " -> " + target.getName());
		http.download(jobResult, target);
		
	}
	
	
	
}
