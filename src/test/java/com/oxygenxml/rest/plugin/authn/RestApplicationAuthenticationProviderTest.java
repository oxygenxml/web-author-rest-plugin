package com.oxygenxml.rest.plugin.authn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.oxygenxml.rest.plugin.AuthHeadersMap;

import ro.sync.ecss.extensions.api.webapp.plugin.UserContext;

public class RestApplicationAuthenticationProviderTest {

  private RestApplicationAuthenticationProvider provider;

  private AuthHeadersMap authHeadersMap;

  @Before
  public void setUp() {
    authHeadersMap = mock(AuthHeadersMap.class);
    provider = new RestApplicationAuthenticationProvider(authHeadersMap, null, null);
  }

  @Test
  public void testRegisterUserCredentials_withCookies() {
    Map<String, String> cookies = new HashMap<>();
    cookies.put("session", "12345");
    UserContext userContext = new UserContext(ImmutableMap.of(
      "cookie", "session=12345"), "sessionId");

    boolean hasCredentials = provider.registerUserCredentials(userContext);

    assertTrue(hasCredentials);
    verify(authHeadersMap).setCookiesHeader(eq("sessionId"), anyString());
  }

  @Test
  public void testRegisterUserCredentials_withBearerToken() {
    UserContext userContext = new UserContext(ImmutableMap.of(
      "Authorization", "Bearer token123"), "sessionId");
    boolean hasCredentials = provider.registerUserCredentials(userContext);

    assertTrue(hasCredentials);
    verify(authHeadersMap).setBearerToken("sessionId", "token123");
  }

  @Test
  public void testSerializeCookieHeader() {
    Map<String, String> cookies = new HashMap<>();
    cookies.put("cookie1", "value1");
    cookies.put("cookie2", "value2");

    String serializedCookies = provider.serializeCookieHeader(cookies);

    assertEquals("cookie1=value1; cookie2=value2; ", serializedCookies);
  }
}
