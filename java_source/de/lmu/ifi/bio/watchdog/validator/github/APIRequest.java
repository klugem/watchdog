package de.lmu.ifi.bio.watchdog.validator.github;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Class used to make API request that return JSON data
 * @author kluge
 *
 * @param <A>
 */
public abstract class APIRequest<A extends Object> {

	public static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create(); 
	public static final int RETRIES = 10;
	private static final int BSIZE = 2048;
	private static final int SLEEP_TIME = 1000;
	
	@SuppressWarnings("unchecked")
	public A makeRequest() throws Exception {
		String content = this.downloadContent();
		return (A) GSON.fromJson(content, this.getJSONClass());
	}
	
	/**
	 * URI that should be downloaded
	 * @return
	 */
	public abstract String getURI(); 
	
	/**
	 * downloads the content of the URI getURI()
	 * @return
	 * @throws Exception 
	 */
	protected String downloadContent() throws Exception {
		ArrayList<Exception> el = new ArrayList<>();
		int tries = 0;
		while(tries < APICompareInfo.RETRIES) {
			try {
				String uri = this.getURI();
				URL u = new URL(uri);
				BufferedReader bfr = new BufferedReader(new InputStreamReader(u.openStream(), StandardCharsets.UTF_8));
				StringBuffer content = new StringBuffer();
				char[] cbuf = new char[BSIZE];
				int read = -1;
				while((read = bfr.read(cbuf)) != -1) {
					content.append(Arrays.copyOfRange(cbuf, 0, read));
				}
				// if no exception in this run --> consider data to be good
				return content.toString();
			} catch(java.io.IOException e) {
				if(e.getMessage().contains("Server returned HTTP response code: 403"))
					throw e;
			}
			catch(Exception e) {
				tries++;
				el.add(e);
				e.printStackTrace();
				System.out.println("retry counter for URL request: " + tries);
				Thread.sleep( SLEEP_TIME);
			}
		}
		// print the exceptions
		tries = 0;
		for(Exception e : el) {
			System.out.println("Run " + tries++ + ":");
			e.printStackTrace(System.out);
		}
		throw el.get(0);
	}
	
	/**
	 * returns the class in which the JSON data should be loaded by GSON
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public abstract Class getJSONClass();
}
