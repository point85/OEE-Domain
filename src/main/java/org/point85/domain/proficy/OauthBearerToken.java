package org.point85.domain.proficy;

import java.time.LocalDateTime;

import com.google.gson.annotations.SerializedName;

public class OauthBearerToken {
	@SerializedName(value = "access_token")
	private String accessToken;

	@SerializedName(value = "token_type")
	private String tokenType;

	@SerializedName(value = "expires_in")
	private int expiresIn;

	private String scope;

	private String jti;

	private transient LocalDateTime expirationTime;

	public String getAccessToken() {
		return accessToken;
	}

	public String getTokenType() {
		return tokenType;
	}

	public int getExpiresIn() {
		return expiresIn;
	}

	public String getScope() {
		return scope;
	}

	public String getJti() {
		return jti;
	}

	public LocalDateTime getExpirationTime() {
		return expirationTime;
	}

	public void setExpirationTime(LocalDateTime expirationTime) {
		this.expirationTime = expirationTime;
	}
}
