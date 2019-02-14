package de.lmu.ifi.bio.watchdog.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import de.lmu.ifi.bio.watchdog.logger.Logger;

/**
 * stores the information about private keys and pass phrases encrypted so very simple memory copy attacks should not work
 * @author Michael Kluge
 *
 */
public class SSHPassphraseAuth {
	private static final Logger LOGGER = new Logger();
	private static final Random RAND = new SecureRandom();
	private static final String UTF8 = "UTF-8";
	private static final String METHOD = "PBEWithMD5AndDES";
	private static final Pattern ENCRYPTION_PATTERN = Pattern.compile("Proc-Type: [0-9]+,ENCRYPTED"); 
	private static GetPassword requestPassword = new StdinPasswortGetter();
	
	private final Cipher CIPTHER_EN;
	private final Cipher CIPTHER_DE;
	private String authFile;
	private String pass = null;
	private String name;
	private boolean encrypted = false;
	private boolean wasTestSucc = false;

	/**
	 * Constructor
	 * @param authFile
	 */
	public SSHPassphraseAuth(String name, String authFile, boolean noExit) {
		String pw = new BigInteger(130, RAND).toString(32);
		String salt = new BigInteger(130, RAND).toString(32).substring(0, 8);
		this.CIPTHER_EN = getCipher(pw, salt, true);
		this.CIPTHER_DE = getCipher(pw, salt, false);
		try {
			this.name = this.encrypt(char2byte(name.toCharArray()));
			this.authFile = this.encrypt(char2byte(authFile.toCharArray()));
		}
		catch(Exception e) {
			e.printStackTrace();
			LOGGER.error("SSHPassphraseHandler: Failed to encrypt!");
			System.exit(1);
		}
	}
	
	public static void changePasswordRequestType(GetPassword pwgetter) {
		requestPassword = pwgetter;
	}
	
	/**
	 * tests, if the auth file exists and if the file is encrypted
	 * must be called before ssh session can be used!
	 * @param noExit
	 * @return
	 */
	public boolean testAuthFile(boolean noExit) {
		String authFile = this.getAuthFile();
		// check, if the file exists
		if(!(new File(authFile).exists() && new File(authFile).isFile() && (new File(authFile).canRead()))) {
			LOGGER.error("Auth file does not exist or can not be read for the remote executer '"+this.getName()+"'.");
			authFile = null;
			System.gc();
			if(!noExit) System.exit(1);
			return false;
		}
		
		try {
			// open file and check, if it is protected by a pass-phrase
			BufferedReader bf = new BufferedReader(new FileReader(authFile));
			String line;		
			Matcher m;
			while((line = bf.readLine()) != null) {
				m = ENCRYPTION_PATTERN.matcher(line);
	
				// it is encrypted
				if(m.matches()) {
					this.encrypted = true;
				}
			}
			bf.close();
			if(!this.encrypted) {
				LOGGER.warn("Your private ssh key for the remote executer '"+this.getName()+"' is not protected by a passphrase!");
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			LOGGER.error("Failed to read auth file for the remote executer '"+this.getName()+"'.");
			System.exit(1);
		}
		authFile = null;
		System.gc();
		return true;
	}
	
	/**
	 * tests, if a login with that credentials is possible
	 * @param user
	 * @param host
	 * @param port
	 * @param strictHostChecking
	 * @return
	 */
	public boolean testCredentials(String user, String host, int port, boolean strictHostChecking) {
		Session s = this.getSSHSession(user, host, port, strictHostChecking);
		try { 
			s.connect();
			s.disconnect();
			this.wasTestSucc = true;
			} 
		catch(Exception e) { 
			LOGGER.error("Could not connect to host '" + host + "' with user '" + user + "' on port '" + port + "' and the given private auth key.");
			LOGGER.error("SSH connection error: " + e.getMessage());
			return false;
		}
		return s != null;
	}
	
	public boolean wasTestSuccessFull() {
		return this.wasTestSucc;
	}
	
	/**
	 * erase a byte array
	 * @param erase
	 */
	public static void bzero(byte[] erase){
		if(erase==null)
			return;
		for(int i=0; i< erase.length; i++)
			erase[i] = 0;
	}
	
	/**
	 * erase a char array
	 * @param erase
	 */
	public static void czero(char[] erase){
		if(erase==null)
			return;
		for(int i=0; i< erase.length; i++)
			erase[i] = '0';
		
		System.gc();
	}
	
	/**
	 * convert a char array into a byte array
	 * @param input
	 * @throws UnsupportedEncodingException 
	 */
	public static byte[] char2byte(char[] input) throws UnsupportedEncodingException {
	    CharBuffer charBuffer = CharBuffer.wrap(input);
	    ByteBuffer byteBuffer = Charset.forName(UTF8).encode(charBuffer);
	    byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),  byteBuffer.position(), byteBuffer.limit());
	    Arrays.fill(byteBuffer.array(), (byte) 0);
	    Arrays.fill(charBuffer.array(), '\u0000');
	    return bytes;
	}
	
	/**
	 * String should be overwritten as soon, as it is not needed anymore and GC called manually
	 * @return
	 */
	public String getAuthFile() {
		try {
			return new String(this.decrypt(this.authFile), UTF8);
		}
		catch(Exception e) {
			e.printStackTrace();
			LOGGER.error("Failed to decrypt auth file path!");
			System.exit(1);
		}
		return null;
	}
	
	private String getName() {
		try {
			return new String(this.decrypt(this.name), UTF8);
		}
		catch(Exception e) {
			e.printStackTrace();
			LOGGER.error("Failed to decrypt name of executor!");
			System.exit(1);
		}
		return null;
	}
	
	/**
	 * Creates a template for a new ssh session but does not connect to it.
	 * @param user
	 * @param host
	 * @param port
	 * @param strictHostChecking
	 * @return
	 */
	public Session getSSHSession(String user, String host, int port, boolean strictHostChecking) {
		try {
			JSch js = new JSch();
		    Session s = js.getSession(user, host, port);
	    	String file = this.getAuthFile();
	    	
	    	// ask for pass-phrase
	    	if(this.encrypted && this.pass == null) {
	    		this.pass = this.encrypt(requestPassword.requestPasswortInputFromUser(this.getName(), LOGGER));
	    		System.gc();
			}

		    // add pass phrase
		    if(pass == null)
		    	js.addIdentity(file);
		    else {
		    	byte[] b = this.decrypt(this.pass);
		    	js.addIdentity(file, b);
		    	bzero(b);
		    }
	    	file = null;
		    System.gc();		    

		    // disable strict host checking
		    if(!strictHostChecking) {
		    	Properties config = new Properties();
		    	config.put("StrictHostKeyChecking", "no");
		   		s.setConfig(config);
		    }
		    return s;
		}
		catch(Exception e) {
			e.printStackTrace();
			LOGGER.error("Failed to configure ssh session!");
			System.exit(1);
		}
		LOGGER.error("Failed to configure ssh session!");
		return null;
	}

	/**
	 * gets a cipher to en- or decode
	 * @param pw
	 * @param salt
	 * @param encode
	 * @return
	 */
	private Cipher getCipher(String pw, String salt, boolean encode) {
		try {
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(METHOD);
			Cipher pbeCipher = Cipher.getInstance(METHOD);
			SecretKey key = keyFactory.generateSecret(new PBEKeySpec(pw.toCharArray()));
			pbeCipher.init(encode ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key, new PBEParameterSpec(salt.getBytes(), 25));
			return pbeCipher;
		}
		catch(Exception e) {
			e.printStackTrace();
			LOGGER.error("Could not load encryption algorithms!");
			System.exit(1);
		}
		return null;
	}
				
	/**
	 * encrypt a string using the internal class specific password
	 * @param message
	 * @return
	 * @throws GeneralSecurityException
	 * @throws UnsupportedEncodingException
	 */
	private String encrypt(byte[] message) throws GeneralSecurityException, UnsupportedEncodingException {
	    String ret = encodeBase64(this.CIPTHER_EN.doFinal(message));
	    bzero(message);
	    return ret;
	}
	
	/**
	 * decrypt a message using the internal class specific password
	 * @param message
	 * @return
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	private byte[] decrypt(String message) throws GeneralSecurityException, IOException {
	    return this.CIPTHER_DE.doFinal(decodeBase64(message));
	}
	    
    /**
     * encode a string in base64
     * @param bytes
     * @return
     */
    private static String encodeBase64(byte[] bytes) {
        return new String(Base64.getEncoder().encode(bytes));
    }

    /**
     * decode a string in base64
     * @param property
     * @return
     * @throws IOException
     */
    private static byte[] decodeBase64(String message) throws IOException {
        return Base64.getDecoder().decode(message);
    }
}
