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

import static org.easymock.classextension.EasyMock.createMock;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.iplanet.sso.SSOToken;

public class MockOpenSSOClient extends OpenSSOClientAdapter {

	private String username;
	private ArrayList<String> groups = new ArrayList<String>();
	private SSOToken token = null;
	private boolean tokenInvalid = true;

	public MockOpenSSOClient(String username) {
		this.username = username;
		getGroups().add("RH");
		getGroups().add("marketing");
		getGroups().add("administration");
	}

	public MockOpenSSOClient(String username, ArrayList<String> groups) {
		this(username);
		this.groups = groups;
	}

	@Override
	public synchronized SSOToken createTokenFrom(HttpServletRequest request) {
		if (tokenInvalid) {
			tokenInvalid = false;
			return null;
		}
		token = createMock(SSOToken.class);
		return token;
	}

	@Override
	public String getPrincipal(SSOToken token) {
		return username;
	}

	@Override
	public String getUserAttribute(String attribute, SSOToken token) {
		return "attributeValue";
	}

	@Override
	public List<String> getGroups(SSOToken token) {
		return groups;
	}

	@Override
	public void destroyToken(SSOToken token) {
		token = null;
		tokenInvalid = true;
	}

	public ArrayList<String> getGroups() {
		return groups;
	}

}
