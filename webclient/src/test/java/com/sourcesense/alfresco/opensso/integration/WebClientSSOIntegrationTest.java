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

import java.util.Properties;

import org.junit.Test;

import com.thoughtworks.selenium.SeleneseTestCase;
import com.thoughtworks.selenium.SeleniumException;

public class WebClientSSOIntegrationTest extends SeleneseTestCase {

	private String agent_password;
	private String agent_login;
	private String opensso_url;

	public void setUp() throws Exception {
		Properties properties = new Properties();
		properties.load(getClass().getClassLoader().getResourceAsStream("AMConfig.properties"));
		this.agent_login = (String) properties.get("com.sun.identity.agents.app.username");
		agent_password = (String) properties.get("com.iplanet.am.service.password");
		String openssoNaming = (String) properties.get("com.iplanet.am.naming.url");
		opensso_url = openssoNaming.substring(0, openssoNaming.lastIndexOf('/')).concat("/UI/Login");

		setUp(opensso_url, "*firefox3");
		
		try {
			loginToOpenSSOConsoleAsAmAdmin();
			deleteUsersAndGroups();
		} catch (SeleniumException ex) {
			System.out.println("Groups and Users already cleaned");
		}
	}
	
	@Test
	public void testWebScriptIntegration() throws InterruptedException {
		loginToOpenSSOConsoleAsAmAdmin();
		createUserWithLoginAndPass("opensso3");
		createUserWithLoginAndPass("admin");
		
		logoutFromOpenSSODomain();
	
		callNoneAuthWebScript();
		assertTrue(selenium.isTextPresent("Alfresco Person Search"));
		
		loginToAlfrescoAs("opensso3","opensso3");
		
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
		goToSubjectsPage();
		selenium.type("Entities.tfFilter", "opensso*");
		selenium.click("Entities.btnSearch");
		selenium.waitForPageToLoad("40000");
		selenium.click("Entities.tblSearch.SelectAllImage");
		selenium.click("Entities.tblButtonDelete");
		selenium.waitForPageToLoad("40000");
		selenium.type("Entities.tfFilter", "admin");
		selenium.click("Entities.btnSearch");
		selenium.waitForPageToLoad("40000");
		selenium.click("Entities.tblSearch.SelectAllImage");
		selenium.click("Entities.tblButtonDelete");
		selenium.waitForPageToLoad("40000");
		selenium.click("link=Group");
		selenium.waitForPageToLoad("40000");
		selenium.type("Entities.tfFilter", "group*");
		selenium.click("Entities.btnSearch");
		selenium.waitForPageToLoad("40000");
		selenium.click("Entities.tblSearch.SelectAllImage");
		selenium.click("Entities.tblButtonDelete");
		selenium.waitForPageToLoad("40000");
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
		selenium.type("IDToken2", "amAdmin");
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
