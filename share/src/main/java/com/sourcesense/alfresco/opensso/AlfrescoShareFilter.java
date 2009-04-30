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
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.connector.User;
import org.alfresco.web.site.AuthenticationUtil;
import org.alfresco.web.site.FrameworkHelper;
import org.alfresco.web.site.RequestContext;
import org.alfresco.web.site.RequestUtil;
import org.alfresco.web.site.UserFactory;
import org.alfresco.web.site.exception.RequestContextException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.iplanet.sso.SSOToken;

public class AlfrescoShareFilter  implements Filter {
	private static Log logger = LogFactory.getLog(AlfrescoShareFilter.class);

	public OpenSSOClient getOpenSSOClient() {
		return OpenSSOClient.instance();
	}

	protected String buildURLForRedirect(ServletRequest request) {
		String serverURL = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
		String alfrescoContext = ((HttpServletRequest) request).getContextPath();
		return getOpenSSOLoginURL().concat("?goto=").concat(serverURL).concat(alfrescoContext);
	}

	protected String getOpenSSOLoginURL() {
		return OpenSSOClient.getOpenSSOLoginURL();
	}

	public void init(FilterConfig config) throws ServletException {
	}
	
	private void doLogout(HttpSession httpSession, SSOToken token) {
		getOpenSSOClient().destroyToken(token);
	}

	public void doFilter(ServletRequest sreq, ServletResponse sresp, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) sreq;
		HttpServletResponse res = (HttpServletResponse) sresp;
		HttpSession httpSession = req.getSession();
		
		SSOToken token = getOpenSSOClient().tokenFromRequest(req);

		if(isLogoutRequest(sreq)) {
			doLogout(httpSession, token);
			res.sendRedirect(buildURLForRedirect(sreq));
		}
		
		try {
			RequestContext context = RequestUtil.getRequestContext(req);
			User user = context.getUser();
			if(user != null && !user.getId().equals(UserFactory.USER_GUEST)) {
				chain.doFilter(sreq, sresp);
				return;
			}
		} catch (RequestContextException e) {
			e.printStackTrace();
		}


		if (token == null) {
			res.sendRedirect(buildURLForRedirect(req));
		} else {
			UserFactory userFactory = FrameworkHelper.getUserFactory();
			String user = getOpenSSOClient().getPrincipal(token);
			boolean authenticated = userFactory.authenticate(req, user, token.getTokenID().toString());
			if (authenticated) {
				AuthenticationUtil.login(req, res, user);
			}
			chain.doFilter(sreq, sresp);
			return;
		}
	}

	private boolean isLogoutRequest(ServletRequest sreq) {
		HttpServletRequest req = (HttpServletRequest) sreq;
		if(req.getServletPath().contains("logout")) {
			return true;
		}
		return false;
	}

	public void destroy() {	}


}