package com.saturdaycoder.easyfetion;
import java.security.*;
import java.security.spec.*;
import java.util.ArrayList;
import java.security.NoSuchAlgorithmException;
import java.util.*;

//import android.util.Base64;
import android.util.Log;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import javax.crypto.*;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.math.BigInteger;

public class Crypto {
	public String nonce = "";
	public String key = "";
	public String signature = "";
	public String cnonce = "";
	public String aeskey = "";
	
	private static String TAG = "EasyFetion";
	
	private static class Base64
	{
	
	    /**
	     * encode
	     *
	     * coverts a byte array to a string populated with
	     * base64 digits. It steps through the byte array
	     * calling a helper methode for each block of three
	     * input bytes
	     *
	     * @param raw The byte array to encode
	     * @return A string in base64 encoding
	     */
	    public static String encode(byte[] raw) {
	        StringBuffer encoded = new StringBuffer();
	        for (int i = 0; i < raw.length; i += 3) {
	            encoded.append(encodeBlock(raw, i));
	        }
	        return encoded.toString();
	    }
	    /*
	     * encodeBlock
	     *
	     * creates 4 base64 digits from three bytes of input data.
	     * we use an integer, block, to hold the 24 bits of input data.
	     *
	     * @return An array of 4 characters
	     */
	    protected static char[] encodeBlock(byte[] raw, int offset) {
	        int block = 0;
	        // how much space left in input byte array
	        int slack = raw.length - offset - 1;
	        // if there are fewer than 3 bytes in this block, calculate end
	        int end = (slack >= 2) ? 2 : slack;
	        // convert signed quantities into unsigned
	        for (int i = 0; i <= end; i++) {
	            byte b = raw[offset + i];
	            int neuter = (b < 0) ? b + 256 : b;
	            block += neuter << (8 * (2 - i));
	        }

	        // extract the base64 digets, which are six bit quantities.
	        char[] base64 = new char[4];
	        for (int i = 0; i < 4; i++) {
	            int sixbit = (block >>> (6 * (3 - i))) & 0x3f;
	            base64[i] = getChar(sixbit);
	        }
	        // pad return block if needed
	        if (slack < 1) base64[2] = '=';
	        if (slack < 2) base64[3] = '=';
	        // always returns an array of 4 characters
	        return base64;
	    }
	   
	    /*
	     * getChar
	     *
	     * encapsulates the translation from six bit quantity
	     * to base64 digit
	     */
	    protected static char getChar(int sixBit) {
	        if (sixBit >= 0 && sixBit <= 25)
	            return (char)('A' + sixBit);
	        if (sixBit >= 26 && sixBit <= 51)
	            return (char)('a' + (sixBit - 26));
	        if (sixBit >= 52 && sixBit <= 61)
	            return (char)('0' + (sixBit - 52));
	        if (sixBit == 62)
	            return '+';
	        if (sixBit == 63)
	            return '/';
	        return '?';
	    }
	   
	    /**
	     * decode
	     *
	     * convert a base64 string into an array of bytes.
	     *
	     * @param base64 A String of base64 digits to decode.
	     * @return A byte array containing the decoded value of
	     *         the base64 input string
	     */
	    public static byte[] decode(String base64) {
	        // how many padding digits?
	        int pad = 0;
	        for (int i = base64.length() - 1; base64.charAt(i) == '='; i--)
	            pad++;
	        // we know know the lenght of the target byte array.
	        int length = base64.length() * 6 / 8 - pad;
	        byte[] raw = new byte[length];
	        int rawIndex = 0;
	        // loop through the base64 value. A correctly formed
	        // base64 string always has a multiple of 4 characters.
	        for (int i = 0; i < base64.length(); i += 4) {
	            int block = (getValue(base64.charAt(i)) << 18)
	                + (getValue(base64.charAt(i + 1)) << 12)
	                + (getValue(base64.charAt(i + 2)) << 6)
	                + (getValue(base64.charAt(i + 3)));
	            // based on the block, the byte array is filled with the
	            // appropriate 8 bit values
	            for (int j = 0; j < 3 && rawIndex + j < raw.length; j++)
	                raw[rawIndex + j] = (byte)((block >> (8 * (2 - j))) & 0xff);
	            rawIndex += 3;
	        }
	        return raw;
	    }
	    /*
	     * getValue
	     *
	     * translates from base64 digits to their 6 bit value
	     */
	    protected static int getValue(char c) {
	        if (c >= 'A' && c <= 'Z')
	            return c - 'A';
	        if (c >= 'a' && c <= 'z')
	            return c - 'a' + 26;
	        if (c >= '0' && c <= '9')
	            return c - '0' + 52;
	        if (c == '+')
	            return 62;
	        if (c == '/')
	            return 63;
	        if (c == '=')
	            return 0;
	        return -1;
	    }
		
	}
	
	public Crypto()
	{
		Random rand = new Random(System.currentTimeMillis());
	
		this.cnonce = String.format("%04X", rand.nextInt(Integer.MAX_VALUE))
			+ String.format("%04X", rand.nextInt(Integer.MAX_VALUE))
			+ String.format("%04X", rand.nextInt(Integer.MAX_VALUE))
			+ String.format("%04X", rand.nextInt(Integer.MAX_VALUE));

	}
	
	public static byte[] base64Decode(byte raw[]) 
	{
		return Base64.decode(new String(raw));
	}
	public static byte[] base64Encode(byte data[]) 
	{
		return Base64.encode(data).getBytes();
	}

	public char[] generateAesKey()
	{
		String str = "";
		for (int i = 0; i < 64; ++i)
			str += "0";
		return str.toCharArray();
	}
	
	public char[] computeResponse(String userId, String password)
	{
		String psdhex = new String(Crypto.getHashedPassword(userId, password));
		//Log.d(TAG, "auth hashpasswd = \"" + psdhex + "\"");
		
		String modulus = key.substring(0, 256);
		//Log.d(TAG, "modulus = " + modulus);
		String exponent = key.substring(256, 262);
		//Log.d(TAG, "exponent = " + exponent);
		
		byte bpsdhex[] = ascii2hex(psdhex.toCharArray());
		byte baeskey[] = ascii2hex(aeskey.toCharArray());
		byte plainData[] = new byte[nonce.length() + bpsdhex.length + baeskey.length];
		System.arraycopy(nonce.getBytes(), 0, plainData, 0, nonce.length());
		System.arraycopy(bpsdhex, 0, plainData, nonce.length(), bpsdhex.length);
		System.arraycopy(baeskey, 0, plainData, nonce.length() + bpsdhex.length, baeskey.length);
		
		BigInteger bnn = new BigInteger(modulus, 16);
		BigInteger bne = new BigInteger(exponent, 16);
		
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(bnn, bne);
			RSAPublicKey rsapublicKey = (RSAPublicKey) keyFactory.generatePublic(pubKeySpec);

			Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");//"RSA");

		    cipher.init(Cipher.ENCRYPT_MODE, rsapublicKey);
		    byte encryptedData[] = cipher.doFinal(plainData);
		    return hex2ascii(encryptedData);

		} catch (Exception e) {
			Log.e(TAG, "error encrypting data, " + e.getCause().toString() +" " + e.getMessage());
			return null;
		}

	}
	
    // algorithm copied from OpenFetion
	private static byte[] ascii2hex(char ascii[])
	{
		List<Byte> ba = new ArrayList<Byte>();
		int len = ascii.length;		
		String sasc = new String(ascii);
		for (int i = 0; i < len; i += 2)
		{
			String ascstr = sasc.substring(i, i+2);
			Integer it = Integer.parseInt(ascstr, 16);			
			Byte b = it.byteValue();
			ba.add(b);
		}
		byte ret[] = new byte[ba.size()];
		for (int j = 0; j < ret.length; ++j)
		{
			ret[j] = ba.get(j);
		}
		return ret;
	}
    // algorithm copied from OpenFetion
	private static char[] hex2ascii(byte in[])
	{
		String str = "";
		for (int i = 0; i < in.length; ++i)
		{
			str += String.format("%02X", in[i]);
		}
		while (str.length() < in.length * 2)
		{
			str += "00";
		}
		return str.toCharArray();
	}
	
    // algorithm copied from OpenFetion
    private static char[] hashPasswordV1(byte b0[], byte psd[]) 
    {
    	byte tmp[] = new byte[b0.length + psd.length];
    	System.arraycopy(b0, 0, tmp, 0, b0.length);
    	System.arraycopy(psd, 0, tmp, b0.length, psd.length);
    	try {
    		MessageDigest md = MessageDigest.getInstance("SHA-1");
	        md.update(tmp);
	        byte res[] = new byte[20];
	        byte dgst[] = md.digest();
	        System.arraycopy(dgst, 0, res, 0, dgst.length);
	        for (int i = dgst.length; i < 20; ++i)
	        {
	        	res[i] = 0;
	        }
	        return hex2ascii(res);
    	} catch (NoSuchAlgorithmException e)
    	{
    		return null;
    	}
    }
    
    // algorithm copied from OpenFetion
    private static char[] hashPasswordV2(char userId[], char passwordHex[])
    {
    	int uid = Integer.parseInt(new String(userId));
    	
    	String ubid = String.format("%08X", Integer.reverseBytes(uid));
    	
    	byte bubid[] = ascii2hex(ubid.toCharArray());
    	
    	byte bpsd[] = ascii2hex(passwordHex);
    	
    	/*for (int j = 0; j <bubid.length; ++j)
    	{
    		Log.e(TAG, "bubid[" + j + "] = " + bubid[j]);
    	}
    	
    	for (int k = 0; k < bpsd.length; ++k)
    	{
    		Log.e(TAG, "bpsd[" + k + "] = " + bpsd[k]);
    	}
    	*/
    	return hashPasswordV1(bubid, bpsd);
    	
    }
    
    // algorithm copied from OpenFetion
    private static char[] hashPasswordV4(char userId[], char password[])
    {
    	String passwd = new String(password);
 
    	char res[] = hashPasswordV1(SystemConfig.fetionDomainName.getBytes(), passwd.getBytes());

    	if (userId == null || userId.length == 0 || userId[0] == '\0')
    	{
    		return res;
    	}
    	char dst[] = hashPasswordV2(userId, res);
    	return dst;

    }

    // algorithm copied from OpenFetion
	public static char[] getHashedPassword(String userId, String password)
	{
		return hashPasswordV4(userId.toCharArray(), password.toCharArray());
	}
}
