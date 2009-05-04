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

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.iplanet.sso.SSOToken;

/**
 * Filter implementation that replace Alfresco's default AuthenticationFilter in
 * order to provide authentication in OpenSSO
 * 
 * @author g.fernandes@sourcesense.com
 * 
 */
public class AlfrescoOpenSSOFilter implements Filter {

	private static Log logger = LogFactory.getLog(AlfrescoOpenSSOFilter.class);

	private OpenSSOClient openSSOClient;
	private AlfrescoFacade alfrescoFacade;
	private ServletContext servletContext;

	public void destroy() {

	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		HttpSession httpSession = httpRequest.getSession();
		
		SSOToken token = getOpenSSOClient().createTokenFrom(httpRequest);
		
		boolean isLoginRequest = isLoginRequest(httpRequest);
		boolean isLogoutRequest = isLogoutRequest(httpRequest);
		boolean isGuestRequest =  (token==null  && !isLoginRequest && !isLogoutRequest);
		boolean isNormalRequest = (token!=null && !isLoginRequest && !isLogoutRequest);
		

		if(isLoginRequest) {
			httpSession.invalidate();
			httpResponse.sendRedirect(buildURLForRedirect(request));
		}
		

		if(isGuestRequest) {
			getAlfrescoFacade().authenticateAsGuest(httpSession);
			chain.doFilter(request, response);
		}
		
		if(isLogoutRequest) {
			doLogout(httpSession, token);
			httpResponse.sendRedirect(buildURLForRedirect(request));
		}
		
		if (isNormalRequest) {
			String principal = getOpenSSOClient().getPrincipal(token);
			if (!getAlfrescoFacade().existUser(principal)) {
				String email = getOpenSSOClient().getUserAttribute(OpenSSOClient.ATTR_EMAIL, token);
				String fullName = getOpenSSOClient().getUserAttribute(OpenSSOClient.ATTR_FULL_NAME, token);
				String firstName = getOpenSSOClient().getUserAttribute(OpenSSOClient.ATTR_LAST_NAME, token);
				getAlfrescoFacade().createUser(principal, email, firstName, fullName);
			}
			List<String> groups = getOpenSSOClient().getGroups(token);
			getAlfrescoFacade().createOrUpdateGroups(principal, groups);
			getAlfrescoFacade().setAuthenticatedUser(httpRequest,httpResponse,httpSession, principal);
			chain.doFilter(request, response);
		} 
		
		
		
	}


	private void doLogout(HttpSession httpSession, SSOToken token) {
		getOpenSSOClient().destroyToken(token);
		httpSession.invalidate();
	}
	
	private boolean isLoginRequest(HttpServletRequest request) {
		Enumeration parameterNames = request.getParameterNames();
		while (parameterNames.hasMoreElements()) {
			String  parameter = (String ) parameterNames.nextElement();
			String[] string = request.getParameterValues(parameter);
			for (int i = 0; i < string.length; i++) {
				if(string[i]!=null && string[i].contains(":login")) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isLogoutRequest(HttpServletRequest request) {
		Enumeration parameterNames = request.getParameterNames();
		while (parameterNames.hasMoreElements()) {
			String  parameter = (String ) parameterNames.nextElement();
			String[] string = request.getParameterValues(parameter);
			for (int i = 0; i < string.length; i++) {
				if(string[i]!=null && string[i].contains(":logout")) {
					return true;
				}
			}
		}
		return false;
	}

	public void init(FilterConfig config) throws ServletException {
		servletContext = config.getServletContext();
	}

	protected String getOpenSSOLoginURL() {
		return OpenSSOClient.getOpenSSOLoginURL();
	}

	protected String buildURLForRedirect(ServletRequest request) {
		String serverURL = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
		String alfrescoContext = ((HttpServletRequest) request).getContextPath();
		return getOpenSSOLoginURL().concat("?goto=").concat(serverURL).concat(alfrescoContext);
	}


	public OpenSSOClient getOpenSSOClient() {
		if(openSSOClient == null) {
			openSSOClient = OpenSSOClient.instance();
		}
		return openSSOClient;
	}

	public AlfrescoFacade getAlfrescoFacade() {
		if (alfrescoFacade == null) {
			alfrescoFacade = new AlfrescoFacade(servletContext);
		}
		return alfrescoFacade;
	}

	public void setAlfrescoFacade(AlfrescoFacade alfrescoFacade) {
		this.alfrescoFacade = alfrescoFacade;
	}

	public void setOpenSSOClient(OpenSSOClient openSSOClient) {
		this.openSSOClient = openSSOClient;

	}

}
