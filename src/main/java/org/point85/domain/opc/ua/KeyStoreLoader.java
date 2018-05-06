package org.point85.domain.opc.ua;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateBuilder;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyStoreLoader {

	private static final Pattern IP_ADDR_PATTERN = Pattern
			.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

	private static final String CLIENT_ALIAS = "client-ai";
	private static final char[] PASSWORD = "password".toCharArray();
	private static final String KEY_STORE_TYPE = "PKCS12";

	private static final Logger logger = LoggerFactory.getLogger(KeyStoreLoader.class);

	private X509Certificate clientCertificate;
	private KeyPair clientKeyPair;

	KeyStoreLoader load(File serverKeyStore) throws Exception {
		// get the key store
		KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);

		// File serverKeyStore =
		// baseDir.toPath().resolve("example-client.pfx").toFile();

		logger.info("Loading KeyStore at {}", serverKeyStore);

		if (!serverKeyStore.exists()) {
			// generate a self-signed certificate
			keyStore.load(null, PASSWORD);

			KeyPair keyPair = SelfSignedCertificateGenerator.generateRsaKeyPair(2048);

			SelfSignedCertificateBuilder builder = new SelfSignedCertificateBuilder(keyPair)
					.setCommonName(UaOpcClient.APP_NAME).setOrganization(UaOpcClient.APP_ORG)
					.setOrganizationalUnit(UaOpcClient.APP_UNIT).setLocalityName(UaOpcClient.APP_CITY)
					.setStateName(UaOpcClient.APP_STATE).setCountryCode(UaOpcClient.APP_COUNTRY)
					.setApplicationUri(UaOpcClient.APP_URI).addDnsName("localhost").addIpAddress("127.0.0.1");

			// Get as many hostnames and IP addresses as we can listed in the certificate.
			for (String hostname : HostnameUtil.getHostnames("0.0.0.0")) {
				if (IP_ADDR_PATTERN.matcher(hostname).matches()) {
					builder.addIpAddress(hostname);
				} else {
					builder.addDnsName(hostname);
				}
			}

			X509Certificate certificate = builder.build();

			keyStore.setKeyEntry(CLIENT_ALIAS, keyPair.getPrivate(), PASSWORD, new X509Certificate[] { certificate });
			keyStore.store(new FileOutputStream(serverKeyStore), PASSWORD);
		} else {
			// read existing file
			keyStore.load(new FileInputStream(serverKeyStore), PASSWORD);
		}

		Key serverPrivateKey = keyStore.getKey(CLIENT_ALIAS, PASSWORD);

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
