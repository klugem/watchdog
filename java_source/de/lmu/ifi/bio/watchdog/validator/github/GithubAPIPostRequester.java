package de.lmu.ifi.bio.watchdog.validator.github;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map.Entry;

import com.google.gson.JsonObject;

/**
 * Class that can be used to sent POST requests to the github API from tra vis builds
 * @author kluge
 *
 */
@Deprecated
public abstract class GithubAPIPostRequester {

	protected static final HttpClient CLIENT = HttpClient.newHttpClient();
	protected static final int CREATED_STATUS = 201;
	
	protected final Builder TEMPLATE;
	
	private static final String USER_AGENT = "watchdog-wms-bot";
	private static final String ACCEPT = "application/vnd.github.v3+json";
	private static final String TRAVIS = "TRAVIS";

	/**
	 * prepares the template
	 * @param url
	 * @throws URISyntaxException
	 */
	public GithubAPIPostRequester(String url) throws URISyntaxException {
		System.out.println(url);
		this.TEMPLATE = HttpRequest.newBuilder().uri(new URI(url))
			
        // add header info
        .header("User-Agent", USER_AGENT)
        .header("Accept", ACCEPT)
        .header("Authorization", "token " + getValue("secureToekn")); // TO-DO
	}
	
	/**
	 * prepares an POST request using the given template
	 * @param values
	 * @return
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	protected synchronized HttpResponse<String> prepareAndSendRequest(HashMap<String, String> values) throws IOException, InterruptedException {
		// prepare Json data
		JsonObject obj = new JsonObject();
		for(Entry<String, String> e : values.entrySet())
			obj.addProperty(e.getKey(), e.getValue());
		
		// build the POST request 
		HttpRequest req =  this.TEMPLATE.POST(HttpRequest.BodyPublishers.ofString(obj.toString())).build();
		System.out.println(obj.toString());
		return sendRequest(req);
	}
	
	/**
	 * sends a request using the static http client
	 * @param request
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private static synchronized HttpResponse<String> sendRequest(HttpRequest request) throws IOException, InterruptedException {
		return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
	}
	
	/**
	 * tests, if the expected return code was returned by the request
	 * @param response
	 * @return
	 */
	public static boolean wasSuccessfull(HttpResponse<String> response) {
		return response.statusCode() == CREATED_STATUS;
	}
	
	/**
	 * returns a value
	 * @param value
	 * @return
	 */
	private static String getValue(String value) {
		if(value == null || value.length() == 0) 
			return "";
		else
			return new String(Base64.getDecoder().decode(value)).chars()
				.mapToObj(x -> (char)(!(x >= 48 && x <= 57) ? x-3 : x))
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).reverse().toString();
	}
	
	/**
	 * true, if a tra vis build
	 * @return
	 */
	public static boolean isTraXvisBuild() {
		String v = System.getenv(TRAVIS);
		return v != null && v.length() > 0;
	}
}
