package com.coffiness.calfit.docs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

@Tag("restdocs")
@ExtendWith(RestDocumentationExtension.class)
public abstract class RestDocsTest {

  protected MockMvc mockMvc;

  protected ObjectMapper objectMapper;

  @BeforeEach
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    this.objectMapper = createObjectMapper();
  }

  protected void setUpMockMvc(
      Object controller, RestDocumentationContextProvider restDocumentation) {
    setUpMockMvc(controller, restDocumentation, (HandlerMethodArgumentResolver[]) null);
  }

  protected void setUpMockMvc(
      Object controller,
      RestDocumentationContextProvider restDocumentation,
      HandlerMethodArgumentResolver... argumentResolvers) {
    MappingJackson2HttpMessageConverter converter =
        new MappingJackson2HttpMessageConverter(objectMapper);
    converter.setDefaultCharset(StandardCharsets.UTF_8);

    ContentNegotiationManager contentNegotiationManager =
        new ContentNegotiationManager(
            new FixedContentNegotiationStrategy(MediaType.APPLICATION_JSON));

    CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
    characterEncodingFilter.setEncoding("UTF-8");
    characterEncodingFilter.setForceEncoding(true);

    var builder =
        MockMvcBuilders.standaloneSetup(controller)
            .apply(
                MockMvcRestDocumentation.documentationConfiguration(restDocumentation)
                    .uris()
                    .withScheme("https")
                    .withHost("api.calfit.com")
                    .withPort(443))
            .setMessageConverters(
                new ByteArrayHttpMessageConverter(),
                new StringHttpMessageConverter(StandardCharsets.UTF_8),
                converter)
            .setContentNegotiationManager(contentNegotiationManager)
            .addFilters(characterEncodingFilter);

    if (argumentResolvers != null) {
      builder.setCustomArgumentResolvers(argumentResolvers);
    }

    this.mockMvc = builder.build();
  }

  private ObjectMapper createObjectMapper() {
    return new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
  }

  protected String toJson(Object object) throws Exception {
    return objectMapper.writeValueAsString(object);
  }
}
