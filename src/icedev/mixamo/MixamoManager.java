package icedev.mixamo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

import icedev.mixamo.util.HTTP;

public class MixamoManager {
	
	MixamoAPI api;
	Set<String> animations = new TreeSet<>();
	
	public File animationsFile = new File("animations.txt");
	public File metadataDirectory = new File("metadata");
	
	public int foundDuplicates = 0;
	JSONParser JSON = new JSONParser();
	
	public MixamoManager(String bearer) {
		HTTP client = new HTTP(JSON);
		client.bearer = bearer;
		api = new MixamoAPI(client);
	}

	private File findFreeName(File dir, String name, String suffix) {
		
		for(int i = 0; ; i++) {
			String fileName = name + (i>0? " " + i : "") + suffix;
			File f = new File(dir, fileName);
			
			if(!f.exists()) {
				return f;
			}
		}
	}
	
	
	public void saveAnimationList() throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("animations.txt"), StandardCharsets.UTF_8), 1024*4);
		for(var anim : animations) {
			writer.write(anim);
			writer.write('\n');
		}
		writer.flush();
		writer.close();
	}
	
	public void loadAnimationList() throws IOException {
		if(new File("animations.txt").exists() == false)
			return;
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("animations.txt"), StandardCharsets.UTF_8), 1024*4);
		reader.lines().forEach(animations::add);
		reader.close();
		
		System.out.println("Read " + animations.size() + " from file");
	}
	

	public void collectAnimationList() {
		for(int page =1; page <= 26; page++) {
			JSONObject res = api.fetchPage(page, 100);
			
			foundDuplicates = 0;
			
			JSONArray anims = (JSONArray) res.get("results");
			for(var anim : anims) {
				String animId = (String) ((JSONObject) anim).get("id");
				
				if(animations.add(animId) == false) {
					foundDuplicates++;
				}
			}
			
			System.out.println("Completed page " + page + " with duplicates: " + foundDuplicates + ", total size: " + animations.size());
			MixamoAPI.sleep(250);
		}
	}
	
	
	public void downloadAnimationMedatada(String characterId) {
		int idx = 0;
		for(var animId : animations) {
			idx++;
			System.out.println(animId + " @ " + idx + "/" + animations.size());
			
			File metaFile = new File(metadataDirectory, animId + ".json");
			
			if(metaFile.exists()) {
				System.out.println("Already exists!!!");
				continue;
			}
			
			var anim = api.fetchProduct(animId, characterId);
			System.out.println("saving... " + anim.get("name"));

			try {
				Files.writeString(metaFile.toPath(), anim.toJSONString());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
			MixamoAPI.sleep(150);
		}
	}

	public void downloadAnimations(String characterId, File directory) throws Exception {
		int idx = 0;
		for(var animId : animations) {
			idx++;
			System.out.println(animId + " @ " + idx + "/" + animations.size());

			File metaFile = new File(metadataDirectory, animId + ".json");
			
			var anim = (JSONObject) JSON.parse(Files.readString(metaFile.toPath()));
			
			String type = (String) anim.get("type");
			var name = (String) anim.get("name");
			
			if(type.equalsIgnoreCase("Motion") == false) {
				System.out.println("Skipping " + type + " " + animId);
				continue;
			}

			System.out.println(name);

			String fileName = name.replaceAll("[<>:\"\\/\\\\|?*]+", "");
			File targetFile = new File(directory, animId + ".fbx");
			
			if(targetFile.exists()) {
				System.out.println("Skipping existing.");
				continue;
			}
			
			directory.mkdirs();
			
			var details = (JSONObject) anim.get("details");
			var gms_hash = (JSONObject) details.get("gms_hash");
			var params = (JSONArray) gms_hash.get("params");

			boolean supportsInPlace = (boolean) details.get("supports_inplace");
			
			if(supportsInPlace) {
				gms_hash.put("inplace", true);
			}
			
			String pvals = (String) params.stream().map((param) -> "" +((JSONArray)param).get(1)).collect(Collectors.joining(","));
			gms_hash.put("params", pvals);
			
			api.exportAndDownloadAnimation(name, animId, gms_hash, characterId, targetFile);
			System.out.println();
			MixamoAPI.sleep(250);
		}
		
//		downloadAnimation((JSONObject) anim, xBotId);
//		downloadAnimation((JSONObject) anim, yBotId);
	}
	
	public void exportFileNames(File sourceDir, File targetDir) throws ParseException, IOException {
		if(targetDir.exists()) {
			for(File f: targetDir.listFiles()) {
				f.delete();
			}
		} else {
			targetDir.mkdirs();
		}
		
		for(File fbx : sourceDir.listFiles()) {
			String animId = fbx.getName();
			animId = animId.substring(0, animId.lastIndexOf('.'));
			
			File metaFile = new File(metadataDirectory, animId + ".json");
			var anim = (JSONObject) JSON.parse(Files.readString(metaFile.toPath()));

			String name = (String) anim.get("name");
			String description = (String) anim.get("description");

			String fileName = (name + " - " + description).replaceAll("[<>:\"\\/\\\\|?*]+", "");
			
			File targetFile = findFreeName(targetDir, fileName, ".fbx");
			System.out.println(targetFile);
			
			Files.copy(fbx.toPath(), targetFile.toPath());
			
		}
	}
}
