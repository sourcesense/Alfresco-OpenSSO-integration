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
package com.sourcesense.alfresco.opensso.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;

import org.alfresco.util.URLEncoder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

public class WebClientSSOIntegrationTest {

	private String agent_password;
	private String agent_login;
	private String opensso_url;
	private Selenium selenium;

	@Before
	public void setUp() throws Exception {
		Properties properties = new Properties();
		properties.load(getClass().getClassLoader().getResourceAsStream("AMConfig.properties"));
		this.agent_login = (String) properties.get("com.sun.identity.agents.app.username");
		agent_password = (String) properties.get("com.iplanet.am.service.password");
		String openssoNaming = (String) properties.get("com.iplanet.am.naming.url");
		opensso_url = openssoNaming.substring(0, openssoNaming.lastIndexOf('/')).concat("/UI/Login");

		selenium = new DefaultSelenium("localhost", 4444, "*firefox3", opensso_url);
		selenium.start();

	}

	@After
	public void clean() throws InterruptedException {
		deleteUsersAndGroups();
		selenium.stop();
	}

	public void testDisplayNonEnglishChars() throws Exception {
		logoutFromOpenSSODomain();
		loginToOpenSSOConsoleAsAmAdmin();
		createUserWithLoginAndPass("opensso1");
		logoutFromOpenSSODomain();
		loginToAlfrescoAs("opensso1", "opensso1");
		selenium.click("link=Create a space in your home space");
		String i18nText = "Aviação Civil";
		selenium.type("dialog:dialog-body:name", i18nText);
		selenium.click("dialog:finish-button");
		selenium.click("link=My Home");
		assertTrue(selenium.isTextPresent(i18nText));
	}

	@Test
	public void testSURFIntegration() throws Exception {
		logoutFromOpenSSODomain();
		loginToOpenSSOConsoleAsAmAdmin();
		createUserWithLoginAndPass("admin");
		createUserWithLoginAndPass("opensso1");
		logoutFromOpenSSODomain();
		loginToAlfrescoAs("admin", "admin");
		String token = getSSOTokenFromCookie();
		String encodedToken = URLEncoder.encode(token);
		String ticket = callLoginWebScript(encodedToken);
		assertTrue(ticket.contains("<ticket>TICKET_"));

	}

	private String callLoginWebScript(String encodedToken) {
		String loginWebScript = "http://localhost:8080/alfresco/s/api/login?u=admin&pw=".concat(encodedToken);
		WebConversation wc = new WebConversation();
		WebRequest req = new GetMethodWebRequest(loginWebScript);
		WebResponse resp;
		try {
			resp = wc.getResponse(req);
			return resp.getText();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		return loginWebScript;
	}

	private String getSSOTokenFromCookie() {
		String allCookies = selenium.getCookie();
		String[] cookies = allCookies.split(";");
		for (int i = 0; i < cookies.length; i++) {
			if (cookies[i].contains("=\"")) {
				String[] cookie = cookies[i].split("=\"");
				String cookieName = cookie[0].trim();
				String cookieValue = cookie[1];
				if (cookieName.equalsIgnoreCase("iPlanetDirectoryPro")) {
					return cookieValue.substring(0, cookieValue.length() - 1);
				}
			}
		}
		return null;
	}

	@Test
	public void testWebScriptIntegration() throws InterruptedException {
		logoutFromOpenSSODomain();
		loginToOpenSSOConsoleAsAmAdmin();
		createUserWithLoginAndPass("opensso3");
		createUserWithLoginAndPass("admin");

		logoutFromOpenSSODomain();

		callNoneAuthWebScript();
		assertTrue(selenium.isTextPresent("Alfresco Person Search"));

		loginToAlfrescoAs("opensso3", "opensso3");

		callGuestWebScript();
		assertTrue(selenium.isTextPresent("Alfresco Keyword Search"));

		callUserWebScript();
		assertTrue(selenium.isTextPresent("Tagging Test UI"));

		callAdminWebScript();
		assertTrue(selenium.isTextPresent("Unauthorized"));

		logoutFromOpenSSODomain();

		loginToAlfrescoAs("admin", "admin");

		callAdminWebScript();
		assertTrue(!selenium.isTextPresent("Unauthorized"));
		assertTrue(selenium.isTextPresent("Web Scripts Installer"));

	}

	private void callNoneAuthWebScript() {
		selenium.open("http://localhost:8080/alfresco/service/api/search/engines");
	}

	private void callGuestWebScript() {
		selenium.open("http://localhost:8080/alfresco/service/api/search/keyword.html?q=readme.html&guest=true");
	}

	private void callUserWebScript() {
		selenium.open("http://localhost:8080/alfresco/service/collaboration/tagActions");
	}

	private void callAdminWebScript() {
		selenium.open("http://localhost:8080/alfresco/service/installer");
	}

	@Test
	public void testSSO() throws Exception {
		loginToOpenSSOConsoleAsAmAdmin();

		createUserWithLoginAndPass("opensso1");
		createUserWithLoginAndPass("opensso2");
		createUserWithLoginAndPass("admin");

		createGroup("group1");
		createGroup("group2");

		associateUserWithGroups("opensso1", "group1");
		associateUserWithGroups("opensso2", "group1", "group2");

		logoutFromOpenSSODomain();

		loginToAlfrescoAs("opensso1", "opensso1");
		assertTrue(selenium.isTextPresent("opensso1"));

		logoutFromAlfresco();

		loginToAlfrescoAs("opensso2", "opensso2");
		assertTrue(selenium.isTextPresent("opensso2"));

		String email = "a@b.com";
		changeUserEmailTo(email);
		assertTrue(selenium.isTextPresent(email));

		logoutFromAlfresco();

		loginToAlfrescoAs("admin", "admin");

		goToAlfrescoGroupManagmentFor("group1");

		assertTrue(selenium.isTextPresent("opensso1"));
		assertTrue(selenium.isTextPresent("opensso2"));

		goToAlfrescoGroupManagmentFor("group2");

		assertTrue(selenium.isTextPresent("opensso2"));
		assertTrue(!selenium.isTextPresent("opensso1"));

		loginToOpenSSOConsoleAsAmAdmin();

		createGroup("group3");
		associateUserWithGroups("opensso2", "group3");

		logoutFromOpenSSODomain();

		loginToAlfrescoAs("opensso2", "opensso2");

		logoutFromOpenSSODomain();

		loginToAlfrescoAs("admin", "admin");

		goToAlfrescoGroupManagmentFor("group1");
		assertTrue(!selenium.isTextPresent("opensso2"));

		goToAlfrescoGroupManagmentFor("group2");
		assertTrue(!selenium.isTextPresent("opensso2"));

		goToAlfrescoGroupManagmentFor("group3");
		assertTrue(selenium.isTextPresent("opensso2"));
	}

	private void logoutFromAlfresco() {
		selenium.click("logout");
		selenium.waitForPageToLoad("40000");

	}

	private void changeUserEmailTo(String email) {
		selenium.click("//img[@alt='User Profile']");
		selenium.waitForPageToLoad("40000");
		selenium.click("//img[@alt='Modify']");
		selenium.waitForPageToLoad("40000");
		selenium.type("dialog:dialog-body:email", email);
		selenium.click("dialog:finish-button");
		selenium.waitForPageToLoad("40000");
	}

	private void goToAlfrescoGroupManagmentFor(String group) {
		selenium.click("//img[@alt='Administration Console']");
		selenium.waitForPageToLoad("40000");
		selenium.click("link=Manage User Groups");
		selenium.waitForPageToLoad("40000");
		selenium.click("link=Root Groups");
		selenium.waitForPageToLoad("40000");
		selenium.click("link=".concat(group));
		selenium.waitForPageToLoad("40000");
	}

	private void logoutFromOpenSSODomain() {
		selenium.open("/opensso/UI/Logout");
	}

	private void loginToAlfrescoAs(String user, String passwd) {
		selenium.open("http://localhost:8080/alfresco/");
		selenium.waitForPageToLoad("40000");
		selenium.click("login");
		selenium.waitForPageToLoad("40000");
		selenium.type("IDToken1", user);
		selenium.type("IDToken2", passwd);
		selenium.click("Login.Submit");
		selenium.waitForPageToLoad("40000");

	}

	private void associateUserWithGroups(String user, String... groups) throws InterruptedException {
		goToSubjectsPage();
		selenium.waitForPageToLoad("40000");
		selenium.click("link=".concat(user));
		selenium.waitForPageToLoad("40000");
		selenium.click("link=Group");
		selenium.waitForPageToLoad("40000");
		for (int second = 0;; second++) {
			if (second >= 60)
				fail("timeout");
			try {
				if (selenium.isTextPresent("Available"))
					break;
			} catch (Exception e) {
			}
			Thread.sleep(1000);
		}
		selenium.click("EntityMembership.addRemoveMembers.RemoveAllButton");

		for (int i = 0; i < groups.length; i++) {
			selenium.type("EntityMembership.tfFilter", groups[i]);
			selenium.click("EntityMembership.btnSearch");
			selenium.waitForPageToLoad("40000");
			selenium.click("EntityMembership.addRemoveMembers.AddAllButton");
			selenium.click("EntityMembership.button1");
			selenium.waitForPageToLoad("40000");
		}

		selenium.click("EntityMembership.button3");
		selenium.waitForPageToLoad("40000");
		goToSubjectsPage();

	}

	private void goToSubjectsPage() throws InterruptedException {
		selenium.open("/opensso/task/Home");
		clickOnLinkWithText("Access Control");
		clickOnLinkWithText("/ (Top Level Realm)");
		clickOnLinkWithText("Subjects");
	}

	private void deleteUsersAndGroups() throws InterruptedException {
		loginToOpenSSOConsoleAsAmAdmin();
		goToSubjectsPage();
		selenium.type("Entities.tfFilter", "opensso*");
		selenium.click("Entities.btnSearch");
		selenium.waitForPageToLoad("40000");
		if(!selenium.isTextPresent("There are no entities")) {
			selenium.click("Entities.tblSearch.SelectAllImage");
			selenium.click("Entities.tblButtonDelete");
			selenium.waitForPageToLoad("40000");
		}
		selenium.type("Entities.tfFilter", "admin");
		selenium.click("Entities.btnSearch");
		selenium.waitForPageToLoad("40000");
		if(!selenium.isTextPresent("There are no entities")) {
			selenium.click("Entities.tblSearch.SelectAllImage");
			selenium.click("Entities.tblButtonDelete");
			selenium.waitForPageToLoad("40000");
		}

		selenium.click("link=Group");
		selenium.waitForPageToLoad("40000");
		selenium.type("Entities.tfFilter", "group*");
		selenium.click("Entities.btnSearch");
		selenium.waitForPageToLoad("40000");
		if(!selenium.isTextPresent("There are no entities")) {
			selenium.click("Entities.tblSearch.SelectAllImage");
			selenium.click("Entities.tblButtonDelete");
			selenium.waitForPageToLoad("40000");
		}
		logoutFromOpenSSODomain();
	}

	private void createGroup(String groupName) throws InterruptedException {
		goToSubjectsPage();
		selenium.click("link=Group");
		selenium.waitForPageToLoad("40000");
		selenium.waitForPageToLoad("40000");
		selenium.click("Entities.tblButtonAdd");
		selenium.waitForPageToLoad("40000");
		selenium.type("psLbl1", groupName);
		selenium.click("EntityAdd.button1");
		selenium.waitForPageToLoad("40000");
	}

	private void createUserWithLoginAndPass(String login) throws InterruptedException {
		goToSubjectsPage();
		selenium.click("Entities.tblButtonAdd");
		selenium.waitForPageToLoad("40000");
		selenium.type("psLbl1", login);
		selenium.type("psLbl2", login);
		selenium.type("psLbl3", login);
		selenium.type("psLbl4", login);
		selenium.type("psLbl5", login);
		selenium.type("EntityAdd.userpassword_confirm", login);
		selenium.click("EntityAdd.button1");
		selenium.waitForPageToLoad("40000");

	}

	private void loginToOpenSSOConsoleAsAmAdmin() {
		logoutFromOpenSSODomain();
		selenium.open("/opensso/UI/Login");
		selenium.type("IDToken1", "amAdmin");
		selenium.type("IDToken2", "12345678");
		selenium.click("Login.Submit");
		selenium.waitForPageToLoad("40000");
	}

	private void clickOnLinkWithText(String label) throws InterruptedException {
		for (int second = 0;; second++) {
			if (second >= 60)
				fail("timeout");
			try {
				if (selenium.isTextPresent(label))
					break;
			} catch (Exception e) {
			}
			Thread.sleep(1000);
		}

		selenium.click("link=".concat(label));
		selenium.waitForPageToLoad("40000");
	}
}
