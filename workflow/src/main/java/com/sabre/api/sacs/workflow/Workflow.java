package com.sabre.api.sacs.workflow;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class used to run a sequence of activities.
 * 用于执行一系列的activity，
 */
public class Workflow {

	private static final Logger LOG = LogManager.getLogger(Workflow.class);
	/**重试次数*/
	private static final int RERUN_LIMIT = 3;

	/**第一个需要执行的行为*/
	private Activity startActivity;

	/***/
	private SharedContext sharedContext;

	public Workflow(Activity startActivity) {
		this.startActivity = startActivity;
		this.sharedContext = new SharedContext();
		sharedContext.setRerunLimit(RERUN_LIMIT);
	}

	public SharedContext run() {
		sharedContext.setOwner(this);
		sharedContext.setConversationId(createConversationId());
		sharedContext.setRerun(sharedContext.getRerun() + 1);
		Activity next = startActivity;
		LOG.debug("Running workflow with the start activity: " + startActivity.toString());
		LOG.debug("for the " + sharedContext.getRerun() + " time.");
		LOG.debug("With the ConversationID: " + sharedContext.getConversationId());
		LOG.debug("And workflow id: " + this.toString());

		//开始执行activity
		while (next != null && !sharedContext.isFaulty()) {
			next = next.run(sharedContext);
		}
		return sharedContext;
	}

	/**时间戳+下划线+随机8位16进制字符作为会话id*/
	private String createConversationId() {

		StringBuffer buffer = new StringBuffer(getTimestamp());
		buffer.append("-");
		buffer.append(longRandomHexString());
		return buffer.toString();

	}

	private String getTimestamp() {
		SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMddhhmmss");
		return sdf.format(new Date());
	}

	private String longRandomHexString() {
		return RandomStringUtils.randomAlphanumeric(8);
	}

}
