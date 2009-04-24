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

import javax.servlet.http.HttpServletRequest;

import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.web.scripts.Authenticator;
import org.alfresco.web.scripts.Description.RequiredAuthentication;
import org.alfresco.web.scripts.servlet.ServletAuthenticatorFactory;
import org.alfresco.web.scripts.servlet.WebScriptServletRequest;
import org.alfresco.web.scripts.servlet.WebScriptServletResponse;

import com.iplanet.sso.SSOToken;
import com.sourcesense.alfresco.opensso.OpenSSOClient;

public class OpenSSOAuthenticationFactory implements ServletAuthenticatorFactory {

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

	private AuthenticationService authenticationService;
	
	private AuthenticationComponent authenticationComponent;

	public Authenticator create(WebScriptServletRequest req, WebScriptServletResponse res) {
		return new OpenSSOAuthenticator(req, res);

	}

	public class OpenSSOAuthenticator implements Authenticator {
		
		private OpenSSOClient openSSOClient = OpenSSOClient.instance();
		
		private WebScriptServletRequest servletReq;


		public OpenSSOAuthenticator(WebScriptServletRequest req, WebScriptServletResponse res) {
			this.servletReq = req;
		}

		public boolean authenticate(RequiredAuthentication required, boolean isGuest) {
			HttpServletRequest httpServletRequest = servletReq.getHttpServletRequest();
			String ticket = httpServletRequest.getParameter("alf_ticket");
			
            if (isGuest && RequiredAuthentication.guest == required) {
                authenticationService.authenticateAsGuest();
                return true;
            } 
            
            if (ticket != null && ticket.length() > 0)  {
            	try
                {
                    authenticationService.validate(ticket);
                    return true;
                }
                catch(AuthenticationException e)
                {
                }
            }

			SSOToken token = openSSOClient.createTokenFrom(httpServletRequest);
			
			if(token==null) return false;
			
			String principal = openSSOClient.getPrincipal(token);
			
			authenticationComponent.setCurrentUser(principal);
			
			return true;
			
		}

	}

}
