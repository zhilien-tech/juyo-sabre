package com.sabre.api.sacs.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nutz.json.Json;
import org.nutz.json.JsonFormat;
import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import com.sabre.api.sacs.configuration.ConfigurationConfig;
import com.sabre.api.sacs.rest.activities.BargainFinderMaxActivity;
import com.sabre.api.sacs.rest.activities.LeadPriceCalendarActivity;
import com.sabre.api.sacs.rest.domain.generated.BfmV310Response;
import com.sabre.api.sacs.rest.domain.leadpricecalendar.LeadPriceCalendarResponse;
import com.sabre.api.sacs.workflow.SharedContext;
import com.sabre.api.sacs.workflow.Workflow;

/**
 * Module configuration an main class for running test flow.
 * 扫描这个类的所在包，将符合要求的类实例放入spring的ioc容器
 */
@SpringBootApplication
@ComponentScan
public class RestApplicationConfiguration {

	private static final Logger LOG = LogManager.getLogger(RestApplicationConfiguration.class);

	private static final Log log = Logs.get();

	//主方法测试接口调用
	public static void main(String args[]) {
		//通过注解加载ioc容器
		final ApplicationContext ctx = SpringApplication.run(new Object[] { ConfigurationConfig.class,
				RestApplicationConfiguration.class }, args);
		bargainFinderMax(ctx);
	}

	public static void bargainFinderMax(ApplicationContext ctx) {
		Workflow workflow = new Workflow(ctx.getBean(BargainFinderMaxActivity.class));
		SharedContext sCtx = workflow.run();
		BfmV310Response bfmresp = (BfmV310Response) sCtx.getResult("BargainFinderMaxResponse");
		String result = Json.toJson(bfmresp, JsonFormat.compact());
		log.info("result:" + result);
		LOG.debug(sCtx);
	}

	public static void leadPriceCalendar(ApplicationContext ctx) {
		Workflow workflow = new Workflow(ctx.getBean(LeadPriceCalendarActivity.class));
		SharedContext sCtx = workflow.run();
		LeadPriceCalendarResponse resp = (LeadPriceCalendarResponse) sCtx.getResult("LeadPriceCalendar");
		String result = Json.toJson(resp, JsonFormat.compact());
		log.info("result:" + result);
		LOG.debug(sCtx);
	}

}
