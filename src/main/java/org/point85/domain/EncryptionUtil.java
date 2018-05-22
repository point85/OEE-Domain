package org.point85.domain;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtil {
	private static final String SPEC_ALGORITHM = "AES";
	// private static final String CIPHER = "AES/CBC/PKCS5Padding";
	private static final String CIPHER = "AES";
	private static final String KEY = "!#e9E=3S8nYLj*2y";
	// private static final byte[] keyBytes = KEY.getBytes();
	private static final byte[] keyBytes = new byte[] { 'T', 'h', 'e', 'B', 'e', 's', 't', 'S', 'e', 'c', 'r', 'e', 't',
			'K', 'e', 'y' };
	private static byte[] encryptionKey = "MZygpewJsCpRrfOr".getBytes(StandardCharsets.UTF_8);

	private SecretKeySpec secretKey;
	private Cipher encryptionCipher;
	private Cipher decryptionCipher;

	public EncryptionUtil() throws Exception {
		secretKey = new SecretKeySpec(encryptionKey, SPEC_ALGORITHM);

		encryptionCipher = Cipher.getInstance(SPEC_ALGORITHM);
		encryptionCipher.init(Cipher.ENCRYPT_MODE, secretKey);

		decryptionCipher = Cipher.getInstance(SPEC_ALGORITHM);
		decryptionCipher.init(Cipher.DECRYPT_MODE, secretKey);
	}

	public static String encrypt1(String toEncrypt) throws Exception {
		if (toEncrypt == null) {
			return null;
		}

		Key key = new SecretKeySpec(keyBytes, SPEC_ALGORITHM);
		Cipher cipher = Cipher.getInstance(CIPHER);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		byte[] encryptedBytes = cipher.doFinal(toEncrypt.getBytes());

		return Base64.getEncoder().encodeToString(encryptedBytes);
	}

	public static String decrypt1(String toDecrypt) throws Exception {
		if (toDecrypt == null) {
			return null;
		}

		Key key = new SecretKeySpec(keyBytes, SPEC_ALGORITHM);
		Cipher cipher = Cipher.getInstance(CIPHER);
		cipher.init(Cipher.DECRYPT_MODE, key);

		byte[] decodedBytes = Base64.getDecoder().decode(toDecrypt);
		byte[] decryptedBytes = cipher.doFinal(decodedBytes);
		return new String(decryptedBytes);
	}

	/**
	 * Encrypts the given plain text
	 *
	 * @param plainText
	 *            The plain text to encrypt
	 */
	public static byte[] encrypt3(byte[] plainText) throws Exception {
		SecretKeySpec secretKey = new SecretKeySpec(encryptionKey, SPEC_ALGORITHM);
		Cipher cipher = Cipher.getInstance(SPEC_ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);

		return cipher.doFinal(plainText);
	}

	public static byte[] decrypt3(byte[] cipherText) throws Exception {
		SecretKeySpec secretKey = new SecretKeySpec(encryptionKey, SPEC_ALGORITHM);
		Cipher cipher = Cipher.getInstance(SPEC_ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, secretKey);

		return cipher.doFinal(cipherText);
	}

	public byte[] encrypt4(byte[] plainText) throws Exception {
		return encryptionCipher.doFinal(plainText);
	}

	public byte[] decrypt4(byte[] cipherText) throws Exception {
		return decryptionCipher.doFinal(cipherText);
	}

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
		if (toDecrypt == null || toDecrypt.length() == 0) {
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

	// encode with padding
	public static String encode(String toEncode) {
		byte[] bytes = toEncode.getBytes(StandardCharsets.UTF_8);
		//String encoded = Base64.getEncoder().encodeToString(bytes);
		//System.out.println(encoded);

		// encode without padding
		String encoded = Base64.getEncoder().withoutPadding().encodeToString(bytes);
		System.out.println(encoded);
		return encoded;
	}

	// decode a String
	public static String decode(String toDecode) {
		byte[] bytes = toDecode.getBytes(StandardCharsets.UTF_8);
		byte[] decodedBytes = Base64.getDecoder().decode(bytes);
		String decoded = new String(decodedBytes);
		System.out.println(decoded);
		return decoded;
	}

	public static void main(String args[]) {
		String password = "mypassword";
		password = "!#e9E=3S8nYLj*2y";
		
		String encoded = EncryptionUtil.encode(password);
		String decoded = EncryptionUtil.decode(encoded);
		

	}
}
