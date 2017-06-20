package com.sabre.api.sacs.soap.callback;

import org.springframework.ws.client.core.WebServiceMessageCallback;

import com.sabre.api.sacs.workflow.SharedContext;

/**
 * Interface used for callback implementations.
 */
public interface HeaderCallback extends WebServiceMessageCallback {

	/**
	 * 设置工作流上下文，以便在工作流中设置的conversationID可以用于从session池中获取session.
	 * @param workflowContext the SharedContext for the workflow that calls the webservice where the callback
	 * should be used, containing the correct conversationID.
	 */
	void setWorkflowContext(SharedContext workflowContext);
}
