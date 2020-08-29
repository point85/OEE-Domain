package org.point85.domain.opc.ua;

import java.io.File;
import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import org.point85.domain.DomainUtils;
import org.point85.domain.i18n.DomainLocalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class X509KeyStoreLoader {
	// input to java keytool command
	private static final String KEYSTORE_ALIAS = "opcua";

	private static final String KEY_STORE_TYPE = "PKCS12";

	private static final Logger logger = LoggerFactory.getLogger(X509KeyStoreLoader.class);

	private X509Certificate clientCertificate;
	private KeyPair clientKeyPair;

	X509KeyStoreLoader load(String keystoreName, String password) throws Exception {
		// get the keystore
		KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);

		if (logger.isInfoEnabled()) {
			logger.info("Reading keystore " + keystoreName);
		}

		String configDir = System.getProperty(DomainUtils.CONFIG_DIR);
		File serverKeyStore = new File(configDir + "/ssl/" + keystoreName);

		if (logger.isInfoEnabled()) {
			logger.info("Loading KeyStore at {}", serverKeyStore);
		}

		if (!serverKeyStore.exists()) {
			throw new Exception(DomainLocalizer.instance().getErrorString("no.keystore", serverKeyStore));
		}

		// read existing file
		keyStore.load(new FileInputStream(serverKeyStore), password.toCharArray());

		Key serverPrivateKey = keyStore.getKey(KEYSTORE_ALIAS, password.toCharArray());

		if (serverPrivateKey == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("no.private.key", KEYSTORE_ALIAS));
		}

		if (serverPrivateKey instanceof PrivateKey) {
			clientCertificate = (X509Certificate) keyStore.getCertificate(KEYSTORE_ALIAS);
			PublicKey serverPublicKey = clientCertificate.getPublicKey();
			clientKeyPair = new KeyPair(serverPublicKey, (PrivateKey) serverPrivateKey);
		}

		if (logger.isInfoEnabled()) {
			logger.info("Loaded KeyStore at {}", serverKeyStore);
		}
		return this;
	}

	X509Certificate getClientCertificate() {
		return clientCertificate;
	}

	KeyPair getClientKeyPair() {
		return clientKeyPair;
	}
}
