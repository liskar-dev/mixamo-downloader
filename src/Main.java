import java.io.File;
import java.util.Scanner;

import icedev.mixamo.MixamoManager;

public class Main {
	static String YBOT_ID = "4f5d21e1-4ccc-41f1-b35b-fb2547bd8493";
	static String XBOT_ID = "2dee24f8-3b49-48af-b735-c6377509eaac";

	public static void main(String[] args) throws Exception {
		String bearer = getBearerFromStdIn();
		
		MixamoManager mixamo = new MixamoManager(bearer);

		mixamo.animationsFile = new File("animations.txt");
		mixamo.metadataDirectory = new File("metadata");

		if(mixamo.animationsFile.exists()) {
			System.out.println("Loading animations");
			mixamo.loadAnimationList();
		} else {
			System.out.println("No animations file present, will try to collect it with 10 tries per each page");
			for(int i=0; i<10; i++) {
				mixamo.collectAnimationList();
				mixamo.saveAnimationList();
			}
		}
		
		String characterId = YBOT_ID;

		System.out.println("Downloading metadata");
		mixamo.downloadAnimationMedatada(characterId);
		
		File characterIdDir = new File(characterId);

		System.out.println("Downloading animations");
		mixamo.downloadAnimations(characterId, characterIdDir);
		
		mixamo.exportFileNames(characterIdDir, new File("Y Bot"));
	}

	
	private static String getBearerFromStdIn() {
		Scanner scanner = new Scanner(System.in);
		System.out.println("To find your BEARER token, paste this into developer console on mixamo.com while logged in:");
		System.out.println("console.log(localStorage.access_token)");
		System.out.println("Paste BEARER token here: ");
		return scanner.nextLine();
	}
}
