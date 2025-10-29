package org.point85.domain.i18n;

/**
 * Provides localization services for the domain classes
 */
public class DomainLocalizer extends Localizer {
	// name of resource bundle with translatable strings for text
	private static final String LANG_BUNDLE_NAME = "i18n/DomainLang";

	// exception strings
	private static final String ERROR_BUNDLE_NAME = "i18n/DomainError";

	// name of resource bundle with translatable strings for UOMs (e.g. time)
	private static final String UNIT_BUNDLE_NAME = "i18n/Unit";

	// Singleton
	private static DomainLocalizer localizer;

	// unit resource manager (e.g. time units)
	private final I18n i18nUnits;

	private DomainLocalizer() {
		setLangBundle(LANG_BUNDLE_NAME);
		setErrorBundle(ERROR_BUNDLE_NAME);

		// common unit strings
		i18nUnits = new I18n(UNIT_BUNDLE_NAME);
	}

	public synchronized static DomainLocalizer instance() {
		if (localizer == null) {
			localizer = new DomainLocalizer();
		}
		return localizer;
	}

	@Override
	public String getLangBundleName() {
		return LANG_BUNDLE_NAME;
	}

	@Override
	public String getErrorBundleName() {
		return ERROR_BUNDLE_NAME;
	}

	/**
	 * get a particular unit string by its key
	 * 
	 * @param key Key for localized unit string
	 * @return Localized unit string
	 */
	public String getUnitString(String key) {
		return i18nUnits.getString(key);
	}
}
