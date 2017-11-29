package de.lmu.ifi.bio.watchdog.helper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import com.gc.iotools.stream.os.OutputStreamToInputStream;

/**
 * Is used to filter messages that are created by the XMLParser lib which are no error according to XML specs.
 * @author kluge
 *
 */
public class ErrorParserFilter extends OutputStreamToInputStream<String> {

	private static final String ERROR = "[Error]";
	private static final CharSequence CVC1A = "cvc-elt.1.a:";
	private final PrintStream ERR_OUTPUT;
	
	public ErrorParserFilter(PrintStream p) {
		this.ERR_OUTPUT = p;
	}
	
	@Override
	protected String doRead(InputStream i) throws Exception {
		BufferedReader r = new BufferedReader(new InputStreamReader(i));
		String e = null;
		while((e = r.readLine()) != null) {
			if(!(e.startsWith(ERROR) && e.contains(CVC1A))) {
				this.ERR_OUTPUT.println(e);
				this.ERR_OUTPUT.flush();
			}
		}
		return null;
	}
}
