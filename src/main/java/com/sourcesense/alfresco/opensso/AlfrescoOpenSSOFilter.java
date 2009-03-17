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

import com.iplanet.sso.SSOToken;

/**
 * Filter implementation that replace Alfresco's default AuthenticationFilter in
 * order to provide authentication in OpenSSO
 * 
 * @author g.fernandes@sourcesense.com
 * 
 */
public class AlfrescoOpenSSOFilter implements Filter {

	private String openSSOServerURL;
	private OpenSSOClientAdapter openSSOClientAdapter;
	private AlfrescoFacade alfrescoFacade;
	private ServletContext servletContext;

	public void destroy() {

	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		HttpSession httpSession = httpRequest.getSession();
		boolean doChain = true;
		SSOToken token = getOpenSSOClient().createTokenFrom(httpRequest);
		if (token != null) {
			String principal = getOpenSSOClient().getPrincipal(token);
			String email = getOpenSSOClient().getUserAttribute(OpenSSOClientAdapter.ATTR_EMAIL, token);
			String fullName = getOpenSSOClient().getUserAttribute(OpenSSOClientAdapter.ATTR_FULL_NAME, token);
			String firstName = getOpenSSOClient().getUserAttribute(OpenSSOClientAdapter.ATTR_LAST_NAME, token);
			httpSession.setAttribute("OPENSSO_PRINCIPAL", principal);
			if (!getAlfrescoFacade().existUser(principal)) {
				getAlfrescoFacade().createUser(principal, email, firstName, fullName);
			}
			getAlfrescoFacade().setAuthenticatedUser(httpRequest, httpSession, principal);
		} else {
			httpResponse.sendRedirect(buildURLForRedirect(request));
			doChain = false;
		}

		if (doChain) {
			chain.doFilter(request, response);
		}

	}

	public void init(FilterConfig config) throws ServletException {
		openSSOServerURL = config.getInitParameter("opensso.url");
		servletContext = config.getServletContext();
	}

	protected String getOpenSSOLoginURL() {
		return getOpenSSOServerURL() + "/UI/Login";
	}

	protected String buildURLForRedirect(ServletRequest request) {
		String serverURL = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
		String alfrescoContext = ((HttpServletRequest) request).getContextPath();
		return getOpenSSOLoginURL().concat("?goto=").concat(serverURL).concat(alfrescoContext);
	}

	public String getOpenSSOServerURL() {
		return openSSOServerURL;
	}

	public OpenSSOClientAdapter getOpenSSOClient() {
		if (openSSOClientAdapter == null) {
			openSSOClientAdapter = new OpenSSOClientAdapter();
		}
		return openSSOClientAdapter;
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

	public void setOpenSSOClient(OpenSSOClientAdapter openSSOClientAdapter) {
		this.openSSOClientAdapter = openSSOClientAdapter;

	}

}
