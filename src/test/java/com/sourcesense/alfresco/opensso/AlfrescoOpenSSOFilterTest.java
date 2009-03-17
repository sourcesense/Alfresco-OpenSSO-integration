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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.makeThreadSafe;
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

import com.iplanet.sso.SSOToken;

public class AlfrescoOpenSSOFilterTest {

	private static final String MOCK_ALFRESCO_URL = "http://localhost:80/alfresco";
	private static final String OPENSSO_URL = "http://server.domain/opensso";
	private static final String OPENSSO_LOGIN = OPENSSO_URL + "/UI/Login";
	private static final String USERNAME = "user1";
	private static final int HTTP_OK = 200;
	private static final int HTTP_REDIRECT = 302;

	private ServletTester tester = new ServletTester();
	private AlfrescoOpenSSOFilter alfrescoFilter;

	@Before
	public void setUp() throws Exception {
		tester = new ServletTester();
		tester.setContextPath("/alfresco");
		tester.addServlet(SimpleServlet.class, "/");
		FilterHolder filterHolder = tester.addFilter(AlfrescoOpenSSOFilter.class, "/*", 1);
		filterHolder.setInitParameter("opensso.url", OPENSSO_URL);
		tester.addFilter(SimpleFilter.class, "/*", 1);
		tester.start();

		alfrescoFilter = (AlfrescoOpenSSOFilter) filterHolder.getFilter();
		alfrescoFilter.setAlfrescoFacade(mockAlfrescoFacade());
		alfrescoFilter.setOpenSSOClient(mockOpenSSOClient());
		
		
	}

	@After
	public void tearDown() throws Exception {
		tester.stop();
	}

	@Test
	public void shoulDoChainWhenAuthenticated() throws Exception {
		HttpTester response = doSomePostWithCookie("/alfresco/",null);
		
		assertEquals(HTTP_OK, response.getStatus());
		assertTrue(response.getContent().contains("Simple Servlet"));
		assertNotNull(response.getHeader("SimpleFilter"));
		assertEquals(SimpleServlet.SIMPLE_SERVLET_CONTENT,response.getContent().trim());

	}

	@Test
	public void shouldRedirectToOpenSSOWhenNotAuthenticated() throws IOException, Exception {
		OpenSSOClientAdapter mock = createMock(OpenSSOClientAdapter.class);
		expect(mock.createTokenFrom((HttpServletRequest) anyObject())).andStubReturn(null);
		replay(mock);
		alfrescoFilter.setOpenSSOClient(mock);

		HttpTester response = doSomePostWithCookie("/alfresco/",null);

		assertEquals(HTTP_REDIRECT, response.getStatus());
		assertTrue(response.getHeader("Location").equals(OPENSSO_LOGIN.concat("?goto=").concat(MOCK_ALFRESCO_URL)));
	}

	@Test
	public void testGetOpenSSOServerURL() throws Exception {
		assertEquals(OPENSSO_URL, alfrescoFilter.getOpenSSOServerURL());

	}

	@Test
	public void shouldCreateUserInAlfresco() throws Exception {
		doSomePostWithCookie("/alfresco/",null);
		assertTrue(alfrescoFilter.getAlfrescoFacade().existUser(USERNAME));
	}


	@Test
	public void testGetOpenSSOLoginURL() {
		assertEquals(OPENSSO_LOGIN, alfrescoFilter.getOpenSSOLoginURL());
	}
	
	

	private HttpTester doSomePostWithCookie(String URI, String setCookie) throws IOException, Exception {
		HttpTester request = new HttpTester();
		HttpTester response = new HttpTester();
		request.setMethod("GET");
		request.setHeader("Host", "localhost");
		request.setURI(URI);
		request.setVersion("HTTP/1.1");
		if (setCookie != null) {
			request.addHeader("Cookie", setCookie.split(";")[0]);
		}
		String responses = tester.getResponses(request.generate());
		System.out.println(responses);
		response.parse(responses);
		return response;
	}
	
	
	private SSOToken mockSSOToken() {
		return createMock(SSOToken.class);
	}

	private OpenSSOClientAdapter mockOpenSSOClient() {
		OpenSSOClientAdapter mock = createMock(OpenSSOClientAdapter.class);
		makeThreadSafe(mock, true);
		expect(mock.createTokenFrom((HttpServletRequest) anyObject())).andStubReturn(mockSSOToken());
		expect(mock.getPrincipal((SSOToken) anyObject())).andStubReturn(USERNAME);
		expect(mock.getUserAttribute((String)anyObject(),(SSOToken) anyObject())).andStubReturn("attribute");
		replay(mock);
		return mock;
	}

	private AlfrescoFacade mockAlfrescoFacade() {
		GenericWebApplicationContext webApplicationContext = new MockAlfrescoApplicationContext();
		MockServletContext mockServletContext = new MockServletContext();
		mockServletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, webApplicationContext);

		AlfrescoFacade mockAlfrescoFacade = new AlfrescoFacade(mockServletContext){
			public ArrayList<String> users = new ArrayList<String>();
			
			@Override
			protected void createUser(String username, String email, String firstName, String lastName) {
				users.add(username);
			}
			@Override
			protected boolean existUser(String username) {
				return users.contains(username);
			}
			
			@Override
			protected void setAuthenticatedUser(HttpServletRequest req, HttpSession httpSess, String userName) {
				NodeRef nodeRef = new NodeRef("workspace://SpacesStore/386f7ece-4127-42b5-8543-3de2e2a20d7e");
				User user = new User(userName,"ticket", nodeRef);
				populateSession(httpSess, user);
			}
		
		};
		
		return mockAlfrescoFacade;
	}
}
