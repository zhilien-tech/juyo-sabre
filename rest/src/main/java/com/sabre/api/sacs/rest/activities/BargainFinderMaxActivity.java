package com.sabre.api.sacs.rest.activities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sabre.api.sacs.configuration.SacsConfiguration;
import com.sabre.api.sacs.rest.common.GenericRestPostCall;
import com.sabre.api.sacs.rest.domain.bargainfindermax.BargainFinderMaxRequest;
import com.sabre.api.sacs.rest.domain.bargainfindermax.CompanyName;
import com.sabre.api.sacs.rest.domain.bargainfindermax.DestinationLocation;
import com.sabre.api.sacs.rest.domain.bargainfindermax.OTAAirLowFareSearchRQ;
import com.sabre.api.sacs.rest.domain.bargainfindermax.OriginDestinationInformation;
import com.sabre.api.sacs.rest.domain.bargainfindermax.OriginLocation;
import com.sabre.api.sacs.rest.domain.bargainfindermax.POS;
import com.sabre.api.sacs.rest.domain.bargainfindermax.RequestorID;
import com.sabre.api.sacs.rest.domain.bargainfindermax.Source;
import com.sabre.api.sacs.rest.domain.generated.BfmV310Response;
import com.sabre.api.sacs.workflow.Activity;
import com.sabre.api.sacs.workflow.SharedContext;

/**
 * Activity to use in workflow. It runs the BargainFinderMax call.
 * Last one in example flow.
 */
@Controller
public class BargainFinderMaxActivity implements Activity {

	@Autowired
	private SacsConfiguration config;

	@Autowired
	private GenericRestPostCall<BargainFinderMaxRequest> call;

	private BargainFinderMaxRequest request;

	public void setRequest(BargainFinderMaxRequest request) {
		this.request = request;
	}

	@Override
	public Activity run(SharedContext context) {
		if (null == request) {
			request = generateRequest();
		}
		String environment = config.getRestProperty("environment");
		System.out.println("environment:" + environment);

		//设置请求
		call.setRequest(request);
		//设置请求地址
		call.setUrl(environment + "/v3.1.0/shop/flights?mode=live");
		//执行请求返回结果
		BfmV310Response response = call.doCall(BfmV310Response.class, context);
		context.putResult("BargainFinderMaxResponse", response);

		//返回null则workflow终止
		return null;
	}

	private BargainFinderMaxRequest generateRequest() {
		List<OriginDestinationInformation> originDestinationInfos = new ArrayList<>();

		//出发到达信息

		List<Source> sourceList = new ArrayList<>();
		//do not forget set the PCC code
		Source source = new Source().withRequestorID(new RequestorID().withID("1").withType("1")
				.withCompanyName(new CompanyName().withCode("TN"))
				.withAdditionalProperty("PseudoCityCode", config.getRestProperty("group")));
		sourceList.add(source);
		POS pos = new POS().withSource(sourceList);

		//<OriginDestinationInformation
		OriginDestinationInformation odi = new OriginDestinationInformation();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, 1);
		odi.setDepartureDateTime(sdf.format(cal.getTime()));

		OriginLocation ol = new OriginLocation();
		ol.setLocationCode("JFK");
		odi.setOriginLocation(ol);

		DestinationLocation dl = new DestinationLocation();
		dl.setLocationCode("LAX");
		odi.setDestinationLocation(dl);
		originDestinationInfos.add(odi);

		BargainFinderMaxRequest bfmreq = new BargainFinderMaxRequest()
				.withOTAAirLowFareSearchRQ(new OTAAirLowFareSearchRQ().withOriginDestinationInformation(
						originDestinationInfos).withPOS(pos));
		return bfmreq;
	}

}
