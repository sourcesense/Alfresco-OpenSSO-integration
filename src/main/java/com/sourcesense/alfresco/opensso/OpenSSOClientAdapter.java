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

import javax.servlet.http.HttpServletRequest;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;


/**
 * Adapter of OpenSSO client SDK
 * @author g.fernandes@sourcesense.com
 *
 */
public class OpenSSOClientAdapter {

	protected SSOTokenManager tokenManager;
	private String principal;

	public OpenSSOClientAdapter() {
		try {
			tokenManager = SSOTokenManager.getInstance();
		} catch (SSOException e) {
			e.printStackTrace();
		}
	}

	public boolean isRequestAuthenticated(HttpServletRequest request) {
		boolean sessionValid = true;
		try {
			SSOToken token = tokenManager.createSSOToken(request);
			sessionValid = tokenManager.isValidToken(token);
			if (sessionValid) {
				principal = token.getProperty("UserId");
			}
		} catch (SSOException e) {
			e.printStackTrace();
			sessionValid = false;
		}
		return sessionValid;
	}

	public String getPrincipal() {
		return principal;
	}
}
