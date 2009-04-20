package com.sourcesense.alfresco.webscript;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.web.scripts.bean.Login;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.web.scripts.Status;
import org.alfresco.web.scripts.WebScriptException;
import org.alfresco.web.scripts.WebScriptRequest;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sourcesense.alfresco.opensso.OpenSSOClient;

public class OpenSSOAuthWebScript extends Login {

	private OpenSSOClient openSSOClient;
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

		String decodedURL = null;
		try {
			decodedURL = URLDecoder.decode(password, "ISO-8859-1");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		
		SSOTokenManager tokenManager;
		boolean isValid = false;
		try {
			tokenManager = SSOTokenManager.getInstance();
			SSOToken createSSOToken = tokenManager.createSSOToken(decodedURL);
			isValid = tokenManager.isValidToken(createSSOToken);
		} catch (SSOException e) {
			e.printStackTrace();
		}
		
		if (isValid) {
			authenticationComponent.setCurrentUser(username);
			Map<String, Object> model = new HashMap<String, Object>();
			model.put("ticket", authenticationService.getCurrentTicket());
			return model;
		} else {
			throw new WebScriptException(HttpServletResponse.SC_FORBIDDEN, "Login failed");
		}
	}

}
