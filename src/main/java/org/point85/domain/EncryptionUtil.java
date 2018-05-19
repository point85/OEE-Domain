package org.point85.domain;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtil {
	private static final String SPEC_ALGORITHM = "AES";
	private static final String CIPHER = "AES/CBC/PKCS5Padding";
	private static final String KEY = "!#e9E=3S8nYLj*2y";
	private static final byte[] keyBytes = KEY.getBytes();

	/*
	 * public static String encrypt(String toEncrypt) throws Exception { if
	 * (toEncrypt == null) { return null; }
	 * 
	 * Key key = new SecretKeySpec(keyBytes, ALGORITHM); Cipher cipher =
	 * Cipher.getInstance(ALGORITHM); cipher.init(Cipher.ENCRYPT_MODE, key); byte[]
	 * encryptedBytes = cipher.doFinal(toEncrypt.getBytes());
	 * 
	 * return Base64.getEncoder().encodeToString(encryptedBytes); }
	 * 
	 * public static String decrypt(String toDecrypt) throws Exception { if
	 * (toDecrypt == null) { return null; }
	 * 
	 * Key key = new SecretKeySpec(keyBytes, ALGORITHM); Cipher cipher =
	 * Cipher.getInstance(ALGORITHM); cipher.init(Cipher.DECRYPT_MODE, key);
	 * 
	 * byte[] decodedBytes = Base64.getDecoder().decode(toDecrypt); byte[]
	 * decryptedBytes = cipher.doFinal(decodedBytes); return new
	 * String(decryptedBytes); }
	 */

	public static String encrypt(String toEncrypt) throws Exception {
		if (toEncrypt == null || toEncrypt.length() == 0) {
			return null;
		}
		
		byte[] encrypted = null;

		Key skeySpec = new SecretKeySpec(keyBytes, SPEC_ALGORITHM);
		Cipher cipher = Cipher.getInstance(CIPHER);
		byte[] iv = new byte[cipher.getBlockSize()];

		IvParameterSpec ivParams = new IvParameterSpec(iv);
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivParams);
		encrypted = cipher.doFinal(toEncrypt.getBytes());

		return new String(encrypted);
	}

	public static String decrypt(String toDecrypt) throws Exception {
		if (toDecrypt == null|| toDecrypt.length() == 0) {
			return null;
		}
		
		byte[] encrypted = toDecrypt.getBytes();
		Cipher cipher = null;

		Key key = new SecretKeySpec(keyBytes, SPEC_ALGORITHM);
		cipher = Cipher.getInstance(CIPHER);

		// the block size (in bytes), or 0 if the underlying algorithm is not a block
		// cipher
		byte[] ivByte = new byte[cipher.getBlockSize()];

		// This class specifies an initialization vector (IV). Examples which use
		// IVs are ciphers in feedback mode, e.g., DES in CBC mode and RSA ciphers with
		// OAEP encoding operation.
		IvParameterSpec ivParamsSpec = new IvParameterSpec(ivByte);
		cipher.init(Cipher.DECRYPT_MODE, key, ivParamsSpec);
		byte[] bytes = cipher.doFinal(encrypted);
		return new String(bytes);
	}
}
