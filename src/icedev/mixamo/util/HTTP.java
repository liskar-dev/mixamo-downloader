package icedev.mixamo.util;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpRequest.*;
import java.nio.file.Path;

import org.json.simple.JSONObject;
import org.json.simple.parser.*;

public class HTTP {
	public static boolean PRINT = false;
	public String bearer = null;

	JSONParser parser;
	HttpClient client;
	
	public HTTP(JSONParser parser) {
		this.parser = parser;
		client = HttpClient.newHttpClient();
	}
	
	private void addHeaders(HttpRequest.Builder req) {
		req.header("Accept","application/json");
		req.header("Content-Type","application/json");
		if(bearer != null)
			req.header("Authorization","Bearer " + bearer);
		req.header("X-Api-Key","mixamo2");
	}

	public JSONObject fetch(String url) {
		return fetch(url, null);
	}
	
	public JSONObject fetch(String url, JSONObject body) {
		try {
			var uri = new URI(url);
			
			Builder req = HttpRequest.newBuilder(uri);
			addHeaders(req);
			
			if(body != null) {
				req.POST(BodyPublishers.ofString(body.toJSONString()));
			}
			
			if(PRINT)
				System.out.println("Fetching " + uri);
			
			HttpResponse<String> response = client.send(req.build(), HttpResponse.BodyHandlers.ofString() );
			int status = response.statusCode();
			
			if(status == 429) {
				throw new TooFastException();
			}
			
			if(status < 200 && status > 299) {
				throw new RuntimeException("Status code: " + status);
			}
			
			try {
				return (JSONObject) parser.parse(response.body());
			} catch (ParseException e) {
				System.out.println("Response body: " + response.body());
				throw new RuntimeException(e);
			}
		} catch (IOException | InterruptedException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void download(String url, File target) {
		try {
			var uri = new URI(url);
			
			Builder req = HttpRequest.newBuilder(uri);
//			addHeaders(req);

			if(PRINT)
				System.out.println("Downloading " + uri);
			
			HttpResponse<Path> response = client.send(req.build(), HttpResponse.BodyHandlers.ofFile(target.toPath()));
			int status = response.statusCode();
			
			if(status < 200 && status > 299) {
				throw new RuntimeException("Status code: " + status);
			}
		} catch (IOException | InterruptedException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
