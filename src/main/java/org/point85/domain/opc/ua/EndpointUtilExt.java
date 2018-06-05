package org.point85.domain.opc.ua;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.util.EndpointUtil;

public class EndpointUtilExt extends EndpointUtil {
	private static final Pattern ENDPOINT_URL_PATTERN = Pattern
			.compile("(opc.tcp|http|https)://([^:/]+)(:\\d+)?(/.*)?");

	/**
	 * Replace the hostname in the endpoint URL field of {@code endpoint} with a new
	 * one.
	 * <p>
	 * All other fields of the original {@link EndpointDescription} remain equal.
	 * <p>
	 * If the endpoint URL does not match the expected format the original endpoint
	 * is returned.
	 *
	 * @param endpoint
	 *            the {@link EndpointDescription} to modify.
	 * @param hostname
	 *            the new hostname to use. A null value will result in the original
	 *            hostname being used.
	 * @return an updated {@link EndpointDescription} in which the hostname of the
	 *         endpoint URL has been replaced with {@code hostname}.
	 */
	public static EndpointDescription updateUrl(@Nonnull EndpointDescription endpoint, @Nullable String hostname) {

		return updateUrl(endpoint, hostname, -1);
	}

	/**
	 * Replace the hostname and port in the endpoint URL field of {@code endpoint}
	 * with new ones.
	 * <p>
	 * All other fields of the original {@link EndpointDescription} remain equal.
	 * <p>
	 * If the endpoint URL does not match the expected format the original endpoint
	 * is returned.
	 *
	 * @param endpoint
	 *            the {@link EndpointDescription} to modify.
	 * @param hostname
	 *            the new hostname to use. A null value will result in the original
	 *            hostname being used.
	 * @param port
	 *            the new port to use. Any value <= 0 will result in the original
	 *            port being used.
	 * @return an updated {@link EndpointDescription} in which the hostname of the
	 *         endpoint URL has been replaced with {@code hostname}.
	 */
	public static EndpointDescription updateUrl(@Nonnull EndpointDescription endpoint, @Nullable String hostname,
			int port) {

		return new EndpointDescription(updateUrl(endpoint.getEndpointUrl(), hostname, port), endpoint.getServer(),
				endpoint.getServerCertificate(), endpoint.getSecurityMode(), endpoint.getSecurityPolicyUri(),
				endpoint.getUserIdentityTokens(), endpoint.getTransportProfileUri(), endpoint.getSecurityLevel());
	}

	static String updateUrl(String endpointUrl, @Nullable String hostname) {
		return updateUrl(endpointUrl, hostname, -1);
	}

	static String updateUrl(String endpointUrl, @Nullable String hostname, int port) {
		Matcher matcher = ENDPOINT_URL_PATTERN.matcher(endpointUrl);

		if (matcher.matches()) {
			String scheme = matcher.group(1);

			String newHostname = matcher.group(2);
			if (hostname != null) {
				newHostname = hostname;
			}

			String newPort = matcher.group(3); // e.g. ":4840"
			if (port >= 0) {
				newPort = ":" + port;
			} else if (newPort == null) {
				newPort = "";
			}

			String path = matcher.group(4); // e.g. "/" or "/foo" or "/foo/bar"
			if (path == null)
				path = "";

			return scheme + "://" + newHostname + newPort + path;
		} else {
			return endpointUrl;
		}
	}

}
