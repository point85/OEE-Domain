package org.point85.domain.proficy;

import java.util.HashMap;
import java.util.Map;

/**
 * Parameter names in RESTful requests
 *
 */
public class TagsListRequestParameters {
	public static final String TAG_NAME = "tagname";

	public static final String MATCH_SINGLE_CHAR = "?";
	public static final String MATCH_MULTI_CHAR = "*";

	private Map<String, String> parameters = new HashMap<>();

	public Map<String, String> getParameters() {
		return parameters;
	}

	/**
	 * Add a request parameter for the RESTful request
	 * 
	 * @param parameter Parameter name
	 * @param value     Parameter value
	 * @param pattern   Name match pattern
	 */
	public void addRequestParameter(String parameter, String value, String pattern) {
		parameters.put(parameter, value + pattern);
	}
}
