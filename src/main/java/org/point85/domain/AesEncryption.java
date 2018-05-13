package org.point85.domain;

import java.security.Key;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AesEncryption {
	private static final String ALGORITHM = "AES";
	private static final String KEY = "!#e9E=3S8nYLj*2y";
	private static final byte[] keyBytes = KEY.getBytes();

	public static String encrypt(String toEncrypt) throws Exception {
		if (toEncrypt == null) {
			return null;
		}

		Key key = new SecretKeySpec(keyBytes, ALGORITHM);
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		byte[] encryptedBytes = cipher.doFinal(toEncrypt.getBytes());

		return Base64.getEncoder().encodeToString(encryptedBytes);
	}

	public static String decrypt(String toDecrypt) throws Exception {
		if (toDecrypt == null) {
			return null;
		}

		Key key = new SecretKeySpec(keyBytes, ALGORITHM);
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, key);

		byte[] decodedBytes = Base64.getDecoder().decode(toDecrypt);
		byte[] decryptedBytes = cipher.doFinal(decodedBytes);
		return new String(decryptedBytes);
	}
}
