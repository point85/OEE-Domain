package org.point85.domain.opc.ua;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class X509KeyStoreLoader {
	private static final String path = "/ssl/";

	private static final String CLIENT_ALIAS = "client-ai";
	// private static final char[] PASSWORD = "password".toCharArray();
	private static final String KEY_STORE_TYPE = "PKCS12";

	private static final Logger logger = LoggerFactory.getLogger(X509KeyStoreLoader.class);

	private X509Certificate clientCertificate;
	private KeyPair clientKeyPair;

	X509KeyStoreLoader load(String keystoreName, String password) throws Exception {
		// get the key store
		KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
		logger.info("Reading keystore " + keystoreName);
		
		InputStream fis = getClass().getClassLoader().getResourceAsStream(path + keystoreName);
		
		File serverKeyStore = new File(path + keystoreName);

		logger.info("Loading KeyStore at {}", serverKeyStore);

		if (!serverKeyStore.exists()) {
			throw new Exception("KeyStore does not exist.");
		}

		// read existing file
		keyStore.load(new FileInputStream(serverKeyStore), password.toCharArray());

		Key serverPrivateKey = keyStore.getKey(CLIENT_ALIAS, password.toCharArray());

		if (serverPrivateKey instanceof PrivateKey) {
			clientCertificate = (X509Certificate) keyStore.getCertificate(CLIENT_ALIAS);
			PublicKey serverPublicKey = clientCertificate.getPublicKey();
			clientKeyPair = new KeyPair(serverPublicKey, (PrivateKey) serverPrivateKey);
		}

		logger.info("Loaded KeyStore at {}", serverKeyStore);
		return this;
	}

	X509Certificate getClientCertificate() {
		return clientCertificate;
	}

	KeyPair getClientKeyPair() {
		return clientKeyPair;
	}

}
