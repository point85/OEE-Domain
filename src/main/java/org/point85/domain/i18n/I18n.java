package org.point85.domain.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * This class provides support for localizable text by wrapping a
 * ResourceBundle. Text localization is based on the default locale. Missing
 * translations are flagged by returning the key with markers.
 *
 */
public class I18n {
	// text localization is based on the default locale
	private static Locale locale = Locale.getDefault();

	// resource bundle
	private ResourceBundle bundle;

	// bundle file name
	private String fileName;

	/**
	 * Constructor
	 * 
	 * @param bundle ResourceBundle
	 */
	public I18n(ResourceBundle bundle) {
		this.bundle = bundle;
	}

	/**
	 * Constructor
	 * 
	 * @param fileName Name of resource bundle
	 */
	public I18n(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Locale for localization
	 * 
	 * @return Locale
	 */
	public static Locale getBundleLocale() {
		return locale;
	}

	/**
	 * Get the resource bundle
	 * 
	 * @return Resource bundle
	 */
	public ResourceBundle getBundle() {
		if (bundle == null) {
			bundle = ResourceBundle.getBundle(fileName, locale);
		}
		return bundle;
	}

	/**
	 * Get the localized text for this key
	 * 
	 * @param key Bundle key
	 * @return Localized text
	 */
	public String getString(String key) {
		String value = null;

		try {
			value = getBundle().getString(key);
		} catch (MissingResourceException e) {
			value = "!" + key + "!";
		}
		return value;
	}

	/**
	 * Get the localized text for this key with substitutable parameters
	 * 
	 * @param key       Bundle key
	 * @param arguments Array of substitution strings
	 * @return Localized text
	 */
	public String getString(String key, Object... arguments) {
		String value = null;

		try {
			value = MessageFormat.format(getString(key), arguments);
		} catch (MissingResourceException e) {
			value = "!" + key + "!";
		}
		return value;
	}
}
