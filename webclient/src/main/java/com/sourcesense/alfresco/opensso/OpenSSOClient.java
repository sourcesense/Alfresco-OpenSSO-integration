/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sourcesense.alfresco.opensso;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.configuration.SystemProperties;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * Adapter of OpenSSO client SDK
 * 
 * @author g.fernandes@sourcesense.com
 * 
 */
public class OpenSSOClient {

	private static Log logger = LogFactory.getLog(OpenSSOClient.class);
	
	public static final String ATTR_UID = "uid";
	public static final String ATTR_LAST_NAME = "sn";
	public static final String ATTR_FULL_NAME = "cn";
	public static final String ATTR_EMAIL = "mail";
	public static final String ATTR_HOME_ADDRESS = "postaladdress";
	public static final String ATTR_TELEFONE = "telephonenumber";
	public static final String ATTR_GROUPS = "memberof";
	public static final String NAMING_URL_PROPERTY = "com.iplanet.am.naming.url";

	protected static SSOTokenManager tokenManager;
	private static OpenSSOClient instance;
	
	public static OpenSSOClient instance() {
		if(instance == null) {
			instance = new OpenSSOClient();
		}
		return instance;
	}

	protected OpenSSOClient() {
		try {
			tokenManager = SSOTokenManager.getInstance();
		} catch (SSOException e) {
			logger.error("Error obtained SSOTOkenManager instance");
		}
	}
	
	
	public static String getOpenSSOLoginURL() {
		String propNaming = SystemPropertiesManager.get(NAMING_URL_PROPERTY);
		return propNaming.replaceAll("namingservice", "UI/Login");
	}
	
	/**
	 * Tries to create an SSOToken based on the HTTP request
	 * 
	 * @param request
	 * @return The token or null if session not valid
	 */
	public  SSOToken createTokenFrom(HttpServletRequest request) {
		SSOToken token = null;
		try {
			token = tokenManager.createSSOToken(request);
			boolean sessionValid = tokenManager.isValidToken(token);
			if (sessionValid) {
				return token;
			}
		} catch (SSOException e) {
			logger.info("Request does not contain a valid session");
		}
		return token;
	}
	
	public boolean isValid(SSOToken tokenID) {
		return tokenManager.isValidToken(tokenID);
	}
	
	public SSOToken tokenFromString(String ticket)  {
		SSOToken token = null;
		try {
			token = tokenManager.createSSOToken(ticket);
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
		} catch (SSOException e) {
			e.printStackTrace();
		}
		return token;
	}
	

	public SSOToken tokenFromRequest(HttpServletRequest request) {
		SSOToken token = null;	
		try {
				token = tokenManager.createSSOToken(request);
			} catch (UnsupportedOperationException e) {
				e.printStackTrace();
			} catch (SSOException e) {
				e.printStackTrace();
			}
		return token;
	}
	
	protected List<String> extractGroupNameFromFQGroup(Set<String> cngroups) {
		ArrayList<String> groups = new ArrayList<String>();
		for (String group : cngroups) {
			String commonName = group.substring(group.indexOf("=") + 1, group.indexOf(","));
			groups.add(commonName);
		}
		if (groups.size() == 0) {
			return null;
		}
		return groups;
	}

	public List<String> getGroups(SSOToken token) {
		try {
			Set<String> cngroups = IdUtils.getIdentity(token).getAttribute(ATTR_GROUPS);
			return extractGroupNameFromFQGroup(cngroups);

		} catch (SSOException e) {
			e.printStackTrace();
		} catch (IdRepoException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getUserAttribute(String attribute, SSOToken token) {
		String attributeValue = null;
		try {
			Set<String> attributeValues = (Set<String>) IdUtils.getIdentity(token).getAttribute(attribute);
			if (!attributeValues.isEmpty()) {
				attributeValue = attributeValues.toArray()[0].toString();
			}
		} catch (SSOException e) {
			e.printStackTrace();
		} catch (IdRepoException e) {
			e.printStackTrace();
		}
		return attributeValue;
	}

	public String getPrincipal(SSOToken token) {
		String principal = null;
		try {
			principal = token.getProperty("UserId");
		} catch (SSOException e) {
			e.printStackTrace();
		}
		return principal;
	}

	public void destroyToken(SSOToken token) {
		try {
			tokenManager.destroyToken(token);
		} catch (SSOException e) {
			e.printStackTrace();
		}
	}
}
