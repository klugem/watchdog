package de.lmu.ifi.bio.watchdog.helper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.gc.iotools.stream.os.OutputStreamToInputStream;

/**
 * Is used to filter messages that are created by the XMLParser lib which are no error according to XML specs.
 * @author kluge
 *
 */
public class ErrorParserFilter extends OutputStreamToInputStream<String> {

	private static final String ERROR = "[Error]";
	private static final CharSequence CVC1A = "cvc-elt.1.a: Cannot find the declaration of element ";
	
	@Override
	protected String doRead(InputStream i) throws Exception {
		BufferedReader r = new BufferedReader(new InputStreamReader(i));
		String e = null;
		while((e = r.readLine()) != null) {
			if(!(e.startsWith(ERROR) && e.contains(CVC1A))) {
				System.out.println(e);
				System.out.flush();
			}
		}
		return null;
	}
}
