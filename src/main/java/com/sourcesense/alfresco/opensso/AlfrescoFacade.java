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

import java.io.Serializable;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.NotSupportedException;
import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.web.app.servlet.AuthenticationHelper;
import org.alfresco.web.bean.LoginBean;
import org.alfresco.web.bean.repository.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Facade for Alfresco operations, such as create user and groups
 * 
 * @author g.fernandes@sourcesense.com
 * 
 */
public class AlfrescoFacade {

	private static Log logger = LogFactory.getLog(AlfrescoFacade.class);

	private TransactionService transactionService;
	private NodeService nodeService;
	private AuthenticationComponent authComponent;
	private AuthenticationService authService;
	private PersonService personService;
	private PermissionService permissionService;
	private AuthenticationService authenticationService;

	public AlfrescoFacade(ServletContext servletContext) {

		WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);

		ServiceRegistry serviceRegistry = (ServiceRegistry) ctx.getBean(ServiceRegistry.SERVICE_REGISTRY);

		transactionService = serviceRegistry.getTransactionService();
		nodeService = serviceRegistry.getNodeService();

		authComponent = (AuthenticationComponent) ctx.getBean("AuthenticationComponent");
		authService = (AuthenticationService) ctx.getBean("AuthenticationService");
		personService = (PersonService) ctx.getBean("personService");
		permissionService = (PermissionService) ctx.getBean("permissionService");
		authenticationService = (AuthenticationService) ctx.getBean("authenticationService");
	}

	protected void setAuthenticatedUser(HttpServletRequest req, HttpSession httpSess, String userName) {
		authComponent.setCurrentUser(userName);

		UserTransaction tx = transactionService.getUserTransaction();
		NodeRef homeSpaceRef = null;
		User user;
		try {
			tx.begin();
			user = new User(userName, authService.getCurrentTicket(), personService.getPerson(userName));
			homeSpaceRef = (NodeRef) nodeService.getProperty(personService.getPerson(userName), ContentModel.PROP_HOMEFOLDER);
			user.setHomeSpaceId(homeSpaceRef.getId());
			tx.commit();
		} catch (Throwable ex) {
			logger.error(ex);

			try {
				tx.rollback();
			} catch (Exception ex2) {
				logger.error("Failed to rollback transaction", ex2);
			}

			if (ex instanceof RuntimeException) {
				throw (RuntimeException) ex;
			} else {
				throw new RuntimeException("Failed to set authenticated user", ex);
			}
		}

		populateSession(httpSess, user);

	}

	protected void populateSession(HttpSession httpSess, User user) {
		httpSess.setAttribute(AuthenticationHelper.AUTHENTICATION_USER, user);
		httpSess.setAttribute(LoginBean.LOGIN_EXTERNAL_AUTH, Boolean.TRUE);
	}

	protected void createUser(String username) {

		UserTransaction tx = transactionService.getUserTransaction();
		try {
			tx.begin();
			authenticationService.createAuthentication(username, username.toCharArray());
			HashMap<QName, Serializable> properties = new HashMap<QName, Serializable>();
			properties.put(ContentModel.PROP_USERNAME, username);
			properties.put(ContentModel.PROP_FIRSTNAME, username);
			properties.put(ContentModel.PROP_LASTNAME, username);
			NodeRef newPerson = personService.createPerson(properties);
			permissionService.setPermission(newPerson, username, permissionService.getAllPermission(), true);
			authenticationService.setAuthenticationEnabled(username, true);

			tx.commit();
		} catch (NotSupportedException e) {
			e.printStackTrace();
		} catch (Throwable ex) {
			logger.error(ex);
			try {
				tx.rollback();
			} catch (Exception ex2) {
				logger.error("Failed to rollback transaction", ex2);
			}
			if (ex instanceof RuntimeException) {
				throw (RuntimeException) ex;
			} else {
				throw new RuntimeException("Failed to set authenticated user", ex);
			}
		}

	}

	protected boolean existUser(String username) {
		boolean exist = false;

		UserTransaction tx = transactionService.getUserTransaction();

		try {
			tx.begin();
			exist = personService.personExists(username);
			tx.commit();
		} catch (Throwable ex) {
			logger.error(ex);
			try {
				tx.rollback();
			} catch (Exception ex2) {
				logger.error("Failed to rollback transaction", ex2);
			}
			if (ex instanceof RuntimeException) {
				throw (RuntimeException) ex;
			} else {
				throw new RuntimeException("Failed to set authenticated user", ex);
			}
		}

		return exist;
	}

}
