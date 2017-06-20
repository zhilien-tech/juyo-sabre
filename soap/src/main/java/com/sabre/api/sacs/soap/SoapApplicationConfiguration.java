package com.sabre.api.sacs.soap;

import org.nutz.json.Json;
import org.nutz.json.JsonFormat;
import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.sabre.api.sacs.configuration.ConfigurationConfig;
import com.sabre.api.sacs.contract.bargainfindermax.OTAAirLowFareSearchRS;
import com.sabre.api.sacs.contract.session.SessionCreateRQ;
import com.sabre.api.sacs.contract.soap.MessageHeader;
import com.sabre.api.sacs.errors.ErrorHandlerConfiguration;
import com.sabre.api.sacs.soap.callback.HeaderComposingCallback;
import com.sabre.api.sacs.soap.orchestratedflow.BargainFinderMaxSoapActivity;
import com.sabre.api.sacs.workflow.SharedContext;
import com.sabre.api.sacs.workflow.Workflow;

/**
 * Main configuration class. Adds callbacks to the Spring context, as well as
 * the marshaller used to marshal/unmarshal security header.
 */
@Configuration
@ComponentScan
@Import({ ConfigurationConfig.class, ErrorHandlerConfiguration.class })
@EnableScheduling
public class SoapApplicationConfiguration {

	private static final Log log = Logs.get();

	@Bean
	@Scope("prototype")
	public HeaderComposingCallback travelItineraryHeaderComposingCallback() {
		return new HeaderComposingCallback("TravelItineraryReadRQ");
	}

	//@Bean注解的方法名即为bean在ioc中的name
	@Bean
	@Scope("prototype")
	public HeaderComposingCallback passengerDetailsHeaderComposingCallback() {
		return new HeaderComposingCallback("PassengerDetailsRQ");
	}

	@Bean
	@Scope("prototype")
	public HeaderComposingCallback bargainFinderMaxHeaderComposingCallback() {
		return new HeaderComposingCallback("BargainFinderMaxRQ");
	}

	@Bean
	@Scope("prototype")
	public HeaderComposingCallback enhancedAirBookHeaderComposingCallback() {
		return new HeaderComposingCallback("EnhancedAirBookRQ");
	}

	@Bean
	@Scope("prototype")
	public HeaderComposingCallback sessionCloseHeaderComposingCallback() {
		return new HeaderComposingCallback("SessionCloseRQ");
	}

	@Bean
	public Jaxb2Marshaller securityMarshaller() {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		StringBuffer contextPath = new StringBuffer().append(MessageHeader.class.getPackage().getName()).append(":")
				.append(SessionCreateRQ.class.getPackage().getName());
		marshaller.setContextPath(contextPath.toString());
		return marshaller;
	}

	//主方法测试接口调用
	public static void main(String args[]) {
		//通过注解加载ioc容器
		final ApplicationContext ctx = SpringApplication.run(new Object[] { ConfigurationConfig.class,
				SoapApplicationConfiguration.class }, args);
		//执行接口调用
		Workflow workflow = new Workflow(ctx.getBean(BargainFinderMaxSoapActivity.class));
		SharedContext sCtx = workflow.run();
		String rq = (String) sCtx.getResult("BargainFinderMaxRQ");
		OTAAirLowFareSearchRS rsObj = (OTAAirLowFareSearchRS) sCtx.getResult("BargainFinderMaxRSObj");
		String rs = (String) sCtx.getResult("BargainFinderMaxRS");
		String result = Json.toJson(rsObj, JsonFormat.compact());
		log.info("rq:" + rq);
		log.info("rsObj:" + result);
		log.info("rs:" + rs);
	}

}
