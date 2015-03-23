/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.iep.archaius2;

import com.netflix.archaius.config.polling.PollingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

public class RemoteProperties implements Callable<PollingResponse> {

  private static final Logger LOGGER = LoggerFactory.getLogger(RemoteProperties.class);

  private final URL url;

  public RemoteProperties(String url) throws MalformedURLException {
    this(URI.create(url).toURL());
  }

  public RemoteProperties(URL url) {
    this.url = url;
  }

  private Properties getProps(URL url) throws Exception {
    LOGGER.debug("refreshing properties from: {}", url);
    HttpURLConnection con = (HttpURLConnection) url.openConnection();
    try {
      con.connect();
      int status = con.getResponseCode();
      if (status != 200) {
        throw new IOException("request to " + url + " failed with status code: " + status);
      }

      try (InputStream in = con.getInputStream()) {
        Properties props = new Properties();
        props.load(in);
        return props;
      }
    } finally {
      con.disconnect();
    }
  }

  @Override public PollingResponse call() throws Exception {
    Properties props = getProps(url);
    Map<String, String> propsMap = new HashMap<>();
    for (String k : props.stringPropertyNames()) {
      String v = props.getProperty(k);
      propsMap.put(k, v);
      LOGGER.debug("received property: [{}] = [{}]", k, v);
    }
    return PollingResponse.forSnapshot(propsMap);
  }
}
