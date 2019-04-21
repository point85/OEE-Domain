package org.point85.domain.i18n;

import java.util.ResourceBundle;

/**
 * Base class for concrete localization services
 *
 */
public abstract class Localizer {
	// language text
	private I18n i18nLang;

	// error text
	private I18n i18nError;

	// bundle name for language
	public abstract String getLangBundleName();

	// bundle name for errors
	public abstract String getErrorBundleName();

	/**
	 * Get the language resource bundle
	 * 
	 * @return ResourceBundle
	 */
	public ResourceBundle loadLangBundle() {
		ResourceBundle bundle = ResourceBundle.getBundle(getLangBundleName(), I18n.getBundleLocale());
		setLangResources(bundle);
		return bundle;
	}

	/**
	 * Set the language resource bundle
	 * 
	 * @param bundle ResourceBundle
	 */
	public void setLangResources(ResourceBundle bundle) {
		setLangI18n(new I18n(bundle));
	}

	/**
	 * Set the error resource bundle
	 * 
	 * @param bundle ResourceBundle
	 */
	public void setErrorResources(ResourceBundle bundle) {
		setErrorI18n(new I18n(bundle));
	}

	/**
	 * Set the language resource bundle name
	 * 
	 * @param bundleName ResourceBundle name
	 */
	public void setLangBundle(String bundleName) {
		setLangI18n(new I18n(bundleName));
	}

	/**
	 * Set the error resource bundle name
	 * 
	 * @param bundleName ResourceBundle name
	 */
	public void setErrorBundle(String bundleName) {
		setErrorI18n(new I18n(bundleName));
	}

	/**
	 * Get the language bundle wrapper
	 * 
	 * @return {@link I18n}
	 */
	public I18n getLangI18n() {
		return i18nLang;
	}

	/**
	 * Set the language bundle wrapper
	 * 
	 * @param i18n {@link I18n}
	 */
	private void setLangI18n(I18n i18n) {
		this.i18nLang = i18n;
	}

	/**
	 * Get the error bundle wrapper
	 * 
	 * @return {@link I18n}
	 */
	public I18n getErrorI18n() {
		return i18nError;
	}

	/**
	 * Set the error bundle wrapper
	 * 
	 * @param i18n {@link I18n}
	 */
	private void setErrorI18n(I18n i18n) {
		this.i18nError = i18n;
	}

	/**
	 * Get the localized text string
	 * 
	 * @param key Resource bundle key
	 * @return Localized string
	 */
	public String getLangString(String key) {
		return i18nLang.getString(key);
	}

	/**
	 * Get the localized text string with substitutable parameters
	 * 
	 * @param key       Resource bundle key
	 * @param arguments Substitutable parameters
	 * @return Localized string
	 */
	public String getLangString(String key, Object... arguments) {
		return i18nLang.getString(key, arguments);
	}

	/**
	 * Get the localized error string
	 * 
	 * @param key Resource bundle key
	 * @return Localized string
	 */
	public String getErrorString(String key) {
		return i18nError.getString(key);
	}

	/**
	 * Get the localized error string with substitutable parameters
	 * 
	 * @param key       Resource bundle key
	 * @param arguments Substitutable parameters
	 * @return Localized string
	 */
	public String getErrorString(String key, Object... arguments) {
		return i18nError.getString(key, arguments);
	}
}
