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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

public class AlfrescoOpenSSOFilterTest {

	private static final String ALFRESCO_URL = "/alfresco/";
	private static final String MOCK_ALFRESCO_URL = "http://localhost:80/alfresco";
	private static final String OPENSSO_LOGIN =  OpenSSOClient.getOpenSSOLoginURL();
	private static final String USERNAME = "user1";
	private static final int HTTP_CODE_OK = 200;
	private static final int HTTP_CODE_REDIRECT = 302;

	private ServletTester tester = new ServletTester();
	private AlfrescoOpenSSOFilter alfrescoFilter;

	@Before
	public void setUp() throws Exception {
		
		tester = new ServletTester();
		tester.setContextPath("/alfresco");
		tester.addServlet(SimpleServlet.class, "/");
		FilterHolder filterHolder = tester.addFilter(AlfrescoOpenSSOFilter.class, "/*", 1);
		tester.addFilter(SimpleFilter.class, "/*", 1);
		tester.start();

		alfrescoFilter = (AlfrescoOpenSSOFilter) filterHolder.getFilter();
		alfrescoFilter.setAlfrescoFacade(mockAlfrescoFacade());
		alfrescoFilter.setOpenSSOClient(new MockOpenSSOClient(USERNAME));
		
		
	}

	@After
	public void tearDown() throws Exception {
		tester.stop();
	}

	@Test
	public void shoulDoChainWhenAuthenticated() throws Exception {
		authenticate();
		HttpTester response = doRequest(ALFRESCO_URL);
		assertEquals(HTTP_CODE_OK, response.getStatus());
		assertTrue(response.getContent().contains("Simple Servlet"));
		assertNotNull(response.getHeader("SimpleFilter"));
		assertEquals(SimpleServlet.SIMPLE_SERVLET_CONTENT,response.getContent().trim());

	}

	@Test
	public void shouldRedirectToOpenSSOWhenLoginRequested() throws IOException, Exception {
		HttpTester response = authenticate();
		assertTrue(response.getHeader("Location").equals(OPENSSO_LOGIN.concat("?goto=").concat(MOCK_ALFRESCO_URL)));
		
		response = doRequest(ALFRESCO_URL);
		assertEquals(HTTP_CODE_OK, response.getStatus());
	}


	@Test
	public void shouldCreateUserInAlfresco() throws Exception {
		authenticate();
		doRequest(ALFRESCO_URL);
		assertTrue(alfrescoFilter.getAlfrescoFacade().existUser(USERNAME));
	}


	@Test
	public void testGetOpenSSOLoginURL() {
		assertEquals(OPENSSO_LOGIN, alfrescoFilter.getOpenSSOLoginURL());
	}
	
	@Test
	public void shouldCreateAlfrescoGroups() throws Exception {
		authenticate();
		doRequest(ALFRESCO_URL);
		assertEquals(3,alfrescoFilter.getAlfrescoFacade().getUserGroups(USERNAME).size());
		
	}
	
	@Test
	public void shouldSynchronizeGroups() throws Exception {
		ArrayList<String> groups = new ArrayList<String>();
		groups.add("group1");
		
		alfrescoFilter.setOpenSSOClient(new MockOpenSSOClient(USERNAME, groups));
		
		authenticate();
		
		doRequest(ALFRESCO_URL);
		
		HttpTester response = doRequest(ALFRESCO_URL);
		
		ArrayList<String> currentGroups = alfrescoFilter.getAlfrescoFacade().getUserGroups("user1");
		
		assertEquals(HTTP_CODE_OK, response.getStatus());
		assertEquals(1,currentGroups.size());
		assertTrue(groups.contains("group1"));
		assertTrue(!groups.contains("marketing"));
	}
	
	
	@Test
	public void shouldLogoutFromAlfresco() throws Exception {
		authenticate();
		doRequest(ALFRESCO_URL);
		
		HttpTester response = new HttpTester();
		HttpTester request = new HttpTester();

		request.setMethod("POST");
		request.setHeader("Host", "localhost");
		request.setURI(ALFRESCO_URL);
		request.setVersion("HTTP/1.1");
		request.setContent("browse%3Aact=browse%3Alogout");
		request.setHeader("Content-Type", "application/x-www-form-urlencoded");
		
		String responses = tester.getResponses(request.generate());
		response.parse(responses);
		assertEquals(HTTP_CODE_REDIRECT, response.getStatus());
		
	}
	
	@Test
	public void shouldLoginAsGuest() throws Exception {
		HttpTester response = doRequest(ALFRESCO_URL);
		assertEquals(HTTP_CODE_OK, response.getStatus());
	}
	
	private HttpTester authenticate() throws IOException, Exception {
		HttpTester response = new HttpTester();
		HttpTester request = new HttpTester();

		request.setMethod("POST");
		request.setHeader("Host", "localhost");
		request.setURI(ALFRESCO_URL);
		request.setVersion("HTTP/1.1");
		request.setContent("browse%3Aact=browse%3Alogin");
		request.setHeader("Content-Type", "application/x-www-form-urlencoded");
		
		String responses = tester.getResponses(request.generate());
		response.parse(responses);
		MockOpenSSOClient openSSOClient =  (MockOpenSSOClient)alfrescoFilter.getOpenSSOClient();
		openSSOClient.setTokenAlwaysValid();
		
		return response;
	}

	private HttpTester doRequest(String URI) throws IOException, Exception {
		HttpTester request = new HttpTester();
		HttpTester response = new HttpTester();
		request.setMethod("GET");
		request.setHeader("Host", "localhost");
		request.setURI(URI);
		request.setVersion("HTTP/1.1");
		String responses = tester.getResponses(request.generate());
		response.parse(responses);
		return response;
	}
	
	
	

	private AlfrescoFacade mockAlfrescoFacade() {
		GenericWebApplicationContext webApplicationContext = new MockAlfrescoApplicationContext();
		MockServletContext mockServletContext = new MockServletContext();
		mockServletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, webApplicationContext);
		AlfrescoFacade mockAlfrescoFacade = new AlfrescoFacade(mockServletContext){
			public ArrayList<String> users = new ArrayList<String>();
			public HashMap<String, List<String>> groups = new HashMap<String, List<String>>();
			
			@Override
			public void createUser(String username, String email, String firstName, String lastName) {
				users.add(username);
			}
			
			@Override
			public Boolean existUser(String username) {
				return users.contains(username);
			}
			
			public ArrayList<String> getUserGroups(String username) {
				return (ArrayList<String>) groups.get(username);
			}
			
			@Override
			protected void setLocale(HttpSession session) {
				// TODO Auto-generated method stub
			}
			
			@Override
			public void createOrUpdateGroups(String principal, List<String> openSSOGroups) {
				groups.remove(principal);
				groups.put(principal, openSSOGroups);
			}
			
			
			@Override
			protected void setAuthenticatedUser(HttpServletRequest req, HttpServletResponse res, HttpSession httpSess, String userName) {
				NodeRef nodeRef = new NodeRef("workspace://SpacesStore/386f7ece-4127-42b5-8543-3de2e2a20d7e");
				User user = new User(userName,"ticket", nodeRef);
				populateSession(httpSess, user);
			}
			
			
			@Override
			public void authenticateAsGuest(HttpSession session) {
			}
		
		};
		
		return mockAlfrescoFacade;
	}
}
