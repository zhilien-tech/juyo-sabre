package com.sabre.api.sacs.soap.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Controller;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.client.SoapFaultClientException;

import com.sabre.api.sacs.configuration.SacsConfiguration;
import com.sabre.api.sacs.errors.ErrorHandlingSchedule;
import com.sabre.api.sacs.soap.callback.HeaderCallback;
import com.sabre.api.sacs.soap.callback.HeaderComposingCallback;
import com.sabre.api.sacs.soap.interceptor.FaultInterceptor;
import com.sabre.api.sacs.soap.interceptor.LoggingInterceptor;
import com.sabre.api.sacs.soap.interceptor.SessionPoolInterceptor;
import com.sabre.api.sacs.workflow.SharedContext;

/**
 * The class to be implemented to create a SOAP call.
 * @param <RQ> Request class
 * @param <RS> Response class
 */
//纳入ioc管理
@Controller
@Scope("prototype")
public abstract class GenericRequestWrapper<RQ, RS> extends WebServiceGatewaySupport {

	private static final Logger LOG = LogManager.getLogger("GenericRequestWrapper -> ");

	@Autowired
	private SacsConfiguration configuration;

	@Autowired
	private ErrorHandlingSchedule errorHandler;

	@Autowired
	private FaultInterceptor faultInterceptor;

	@Autowired
	private SessionPoolInterceptor sessionPoolInterceptor;

	@Autowired
	private LoggingInterceptor loggingInterceptor;

	private RQ request;

	private boolean lastInFlow;

	private BlockingQueue<RS> resultLock = new LinkedBlockingQueue<>();

	public void setRequest(RQ request) {
		this.request = request;
	}

	/**
	 * Implementations should return a list of additional interceptors.
	 * The {@link FaultInterceptor} and {@link LoggingInterceptor} are added by default.
	 * Depending on the lastInFlow flag, the {@link SessionPoolInterceptor} is added.
	 * @return
	 */
	protected abstract List<ClientInterceptor> interceptors();

	/**
	 * Implementations should return the marshaller to be used
	 * to marshal/unmarshal request/response.
	 * 具体的实现应该给出用于xml<->bean的解析类
	 * @return
	 */
	protected abstract Jaxb2Marshaller marshaller();

	/**
	 * Implementations should return the callback to be used in the
	 * call. Usually it's an instance of {@link HeaderComposingCallback}.
	 * 具体的实现应该给出webservice调用后的callback处理
	 * @return
	 */
	protected abstract HeaderCallback callback();

	/**
	 * 用于标记一个调用是否是工作流中的最后一个，如果是最后一个，那么调用完成后，session会还回池中
	 */
	public void setLastInFlow(boolean lastInFlow) {
		this.lastInFlow = lastInFlow;
	}

	/**
	 * 实例化 bean的时候执行
	 */
	@PostConstruct
	private void init() {
		/**
		 * 初始化请求拦截器，模板方法模式
		 */
		List<ClientInterceptor> interceptors = interceptors();
		if (interceptors == null) {
			interceptors = new ArrayList<>();
		}
		//如果具体的实现不指定拦截器的话，默认加入faultInterceptor与loggingInterceptor
		interceptors.add(faultInterceptor);
		interceptors.add(loggingInterceptor);
		this.setInterceptors(interceptors.toArray(new ClientInterceptor[0]));

		//设置请求地址(gateway)
		this.setDefaultUri(configuration.getSoapProperty("environment"));

		//设置用于xml<->bean的解析和反解析类
		this.setMarshaller(marshaller());
		this.setUnmarshaller(marshaller());
	}

	/**
	 * Executes the call.
	 * 执行soap调用，调用的时候需要设置workflowContext
	 * @param workflowContext the context in which the call is being run.
	 * @return the response object.
	 * @throws InterruptedException 
	 */
	public RS executeRequest(SharedContext workflowContext) throws InterruptedException {
		new Thread(new SoapCallTask(workflowContext)).start();
		return resultLock.take();
	}

	private class SoapCallTask implements Runnable {

		private SharedContext workflowContext;

		SoapCallTask(SharedContext workflowContext) {
			this.workflowContext = workflowContext;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			RS result = null;
			if (lastInFlow) {
				List<ClientInterceptor> interceptors = new ArrayList<>();
				interceptors.addAll(Arrays.asList(getInterceptors()));
				interceptors.add(sessionPoolInterceptor);
				//toArray调用的时候，如果数组长度不够，会自动扩展
				setInterceptors(interceptors.toArray(new ClientInterceptor[0]));
			}
			callback().setWorkflowContext(workflowContext);
			boolean isFault = false;
			try {
				result = (RS) getWebServiceTemplate().marshalSendAndReceive(request, callback());
			} catch (SoapFaultClientException e) {
				isFault = true;
				workflowContext.setFaulty(true);
				LOG.catching(e);
			}
			if (!isFault) {
				LOG.info("Request succeeded (y)");
			} else {
				errorHandler.addSystemFailure(workflowContext);
			}
			resultLock.offer(result);
		}

	}

}
