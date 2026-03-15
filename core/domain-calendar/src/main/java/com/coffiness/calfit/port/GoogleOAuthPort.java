package com.coffiness.calfit.port;

import com.coffiness.calfit.model.OAuthExchangeResult;
import com.coffiness.calfit.model.OAuthRefreshResult;

public interface GoogleOAuthPort {

  OAuthExchangeResult exchangeAuthorizationCode(String authCode, String redirectUri);

  OAuthRefreshResult refreshAccessToken(String refreshToken);
}
