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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.TicketComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.springframework.beans.BeansException;
import org.springframework.web.context.support.GenericWebApplicationContext;


/**
 * Fake WebApplicationContext to test the filter with proxied Spring Beans
 * @author g.fernandes@sourcesense.com
 *
 */
public class MockAlfrescoApplicationContext extends GenericWebApplicationContext {
	
	private Map<String, Class> beans = new HashMap<String, Class>();

	{
		beans.put("ServiceRegistry", ServiceRegistry.class);
		beans.put("AuthenticationComponent", AuthenticationComponent.class);
		beans.put("AuthenticationService", AuthenticationService.class);
		beans.put("personService", PersonService.class);
		beans.put("permissionService", PermissionService.class);
		beans.put("authenticationService", AuthenticationService.class);
		beans.put("authorityService", AuthorityService.class);
		beans.put("ticketComponent", TicketComponent.class);
	}

	@Override
	public  Object getBean(String name) throws BeansException {
		try {
			Class clazz = beans.get(name);
			Object newProxyInstance = Proxy.newProxyInstance(getClassLoader(), new Class[] { clazz }, new InvocationHandler() {
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					return null;
				}
			});
			return newProxyInstance;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		return null;
	}
}
