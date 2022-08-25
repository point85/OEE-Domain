package org.point85.domain.http;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * OAuthAccessToken contains methods for handling an OAuth 2.0 access token
 *
 */
public class OAuthAccessToken {
	// expiration threshold (seconds)
	private Duration threshold = Duration.ofSeconds(3600);

	private String tokenType = "Bearer";
	private String accessToken;
	private Integer expiresIn;
	private String refreshToken;

	// when token was granted
	private LocalDateTime obtainedOn;

	public OAuthAccessToken() {
		obtainedOn = LocalDateTime.now();
	}

	public OAuthAccessToken(String accessToken) {
		obtainedOn = LocalDateTime.now();
		this.accessToken = accessToken;
	}

	public Integer getExpiresIn() {
		return this.expiresIn;
	}

	public void setExpiresIn(Integer expiresIn) {
		this.expiresIn = expiresIn;
	}

	public LocalDateTime getObtainedOn() {
		return obtainedOn;
	}

	/**
	 * Determine duration before the access token will expire
	 * 
	 * @return Duration before expiration
	 */
	public Duration determineTimeToLive() {
		Duration elapsed = obtainedOn != null ? Duration.between(obtainedOn, LocalDateTime.now()) : Duration.ZERO;
		return expiresIn != null ? Duration.ofSeconds(expiresIn).minus(elapsed) : Duration.ZERO;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String type) {
		tokenType = type;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	/**
	 * Determine if the access token is going to expire by comparing the time to
	 * live to the threshold duration
	 * 
	 * @return True if will expire soon, else false
	 */
	public boolean willExpire() {
		boolean willExpire = false;

		if (determineTimeToLive() != null) {
			willExpire = determineTimeToLive().minus(threshold).isNegative();
		}
		return willExpire;
	}

	/**
	 * Determine if this token has access and refresh tokens
	 * 
	 * @return True if valid, else false
	 */
	public boolean isValid() {
		return this.accessToken != null && this.refreshToken != null;
	}

	public Duration getThreshold() {
		return this.threshold;
	}

	public void setThreshold(Duration threshold) {
		this.threshold = threshold;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Token: " + accessToken).append('\n');
		sb.append("Refresh Token: " + refreshToken).append('\n');
		sb.append("Expiration (sec): " + expiresIn).append('\n');
		sb.append("Type: " + tokenType).append('\n');
		sb.append("TTL: " + determineTimeToLive());

		return sb.toString();
	}
}
