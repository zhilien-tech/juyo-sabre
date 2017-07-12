package com.sabre.api.sacs.soap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sabre.api.sacs.contract.soap.Security;
import com.sabre.api.sacs.contract.transaction.IgnoreTransactionRS;
import com.sabre.api.sacs.soap.pool.SessionPool;
import com.sabre.api.sacs.soap.session.IgnoreTransactionWrapper;
import com.sabre.api.sacs.workflow.SharedContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { SoapApplicationConfiguration.class })
public class SessionPoolTest {

	@Autowired
	@InjectMocks
	private SessionPool testPool;

	@Mock
	private IgnoreTransactionWrapper itWrapper;

	@Before
	public void initMocks() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void shouldReturnSession() {
		testPool.clear();
		//创建Security对象
		Security security = new Security();
		//设置此对象的token
		security.setBinarySecurityToken("BST1");
		testPool.addToPool(security);

		SharedContext context = new SharedContext();
		context.setConversationId("ConvId");
		Security returned = testPool.getFromPool(context);

		Assert.assertEquals("BST1", returned.getBinarySecurityToken());
		Assert.assertEquals(1, testPool.getBusy().size());
		Assert.assertEquals(0, testPool.getAvailable().length);
	}

	@Test
	public void shouldGetSessionAndReturnItToThePool() {
		testPool.clear();
		Mockito.doReturn(new IgnoreTransactionRS()).when(itWrapper)
				.executeRequest(Mockito.any(Security.class), Mockito.any(SharedContext.class));
		Security security = new Security();
		security.setBinarySecurityToken("BST1");

		SharedContext context = new SharedContext();
		context.setConversationId("ConvId");
		testPool.addToPool(security);
		Security returned = testPool.getFromPool(context);
		Assert.assertEquals("BST1", returned.getBinarySecurityToken());
		Assert.assertEquals(1, testPool.getBusy().size());
		Assert.assertEquals(0, testPool.getAvailable().length);
		testPool.returnToPool(context.getConversationId());
		Assert.assertEquals(0, testPool.getBusy().size());
		Assert.assertEquals(1, testPool.getAvailable().length);
	}
}
