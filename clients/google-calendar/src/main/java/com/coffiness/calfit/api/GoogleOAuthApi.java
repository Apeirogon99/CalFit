package com.coffiness.calfit.api;

import com.coffiness.calfit.dto.GoogleOAuthResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "googleOAuthApi", url = "https://oauth2.googleapis.com")
public interface GoogleOAuthApi {

  @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  GoogleOAuthResponseDto exchange(@RequestBody MultiValueMap<String, String> form);
}
