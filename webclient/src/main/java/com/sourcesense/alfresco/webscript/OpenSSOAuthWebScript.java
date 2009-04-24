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
package com.sourcesense.alfresco.webscript;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.web.scripts.bean.Login;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.util.URLDecoder;
import org.alfresco.web.scripts.Status;
import org.alfresco.web.scripts.WebScriptException;
import org.alfresco.web.scripts.WebScriptRequest;

import com.iplanet.sso.SSOToken;
import com.sourcesense.alfresco.opensso.OpenSSOClient;

public class OpenSSOAuthWebScript extends Login {

	private OpenSSOClient openSSOClient = OpenSSOClient.instance();
	private AuthenticationService authenticationService;
	private AuthenticationComponent authenticationComponent;

	public AuthenticationComponent getAuthenticationComponent() {
		return authenticationComponent;
	}

	public void setAuthenticationComponent(AuthenticationComponent authenticationComponent) {
		this.authenticationComponent = authenticationComponent;
	}

	public AuthenticationService getAuthenticationService() {
		return authenticationService;
	}

	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status) {

		String username = req.getParameter("u");
		if (username == null || username.length() == 0) {
			throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST, "Username not specified");
		}
		String password = req.getParameter("pw");

		if (password == null) {
			throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST, "Password not specified");
		}

		String decodedToken = URLDecoder.decode(password);

		SSOToken token = openSSOClient.tokenFromString(decodedToken);

		boolean isValid = openSSOClient.isValid(token);

		try {
			if (isValid) {
				authenticationComponent.setCurrentUser(username);
				Map<String, Object> model = new HashMap<String, Object>();
				model.put("ticket", authenticationService.getCurrentTicket());
				return model;
			} else {
				throw new WebScriptException(HttpServletResponse.SC_FORBIDDEN, "Login failed");
			}
		} finally {
			authenticationService.clearCurrentSecurityContext();
		}
	}

}
