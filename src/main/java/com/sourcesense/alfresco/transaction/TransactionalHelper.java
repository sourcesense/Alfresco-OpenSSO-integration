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
package com.sourcesense.alfresco.transaction;

import javax.transaction.UserTransaction;

import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Helper to execute  Alfresco method calls in a transaction
 * @author g.fernandes@sourcesense.com
 *
 */
public class TransactionalHelper {
	
	private static Log logger = LogFactory.getLog(TransactionalHelper.class);
	private  TransactionService transactionService;

	public TransactionalHelper(TransactionService txService) {
		this.transactionService = txService;
		
	}
	
	public Object doInTransaction(Transactionable callback) {
		UserTransaction tx = transactionService.getUserTransaction();
		Object result;
		try {
			tx.begin();
			result =  callback.execute();
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
				throw new RuntimeException("Failed to execute transactional method", ex);
			}
		}
		
		return result;
		
	}
	
}
