package com.sabre.api.sacs.rest.activities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sabre.api.sacs.configuration.SacsConfiguration;
import com.sabre.api.sacs.rest.common.GenericRestPostCall;
import com.sabre.api.sacs.rest.domain.bargainfindermax.AirTravelerAvail;
import com.sabre.api.sacs.rest.domain.bargainfindermax.BargainFinderMaxRequest;
import com.sabre.api.sacs.rest.domain.bargainfindermax.CompanyName;
import com.sabre.api.sacs.rest.domain.bargainfindermax.DestinationLocation;
import com.sabre.api.sacs.rest.domain.bargainfindermax.IntelliSellTransaction;
import com.sabre.api.sacs.rest.domain.bargainfindermax.OTAAirLowFareSearchRQ;
import com.sabre.api.sacs.rest.domain.bargainfindermax.OriginDestinationInformation;
import com.sabre.api.sacs.rest.domain.bargainfindermax.OriginLocation;
import com.sabre.api.sacs.rest.domain.bargainfindermax.POS;
import com.sabre.api.sacs.rest.domain.bargainfindermax.PassengerTypeQuantity;
import com.sabre.api.sacs.rest.domain.bargainfindermax.RequestType;
import com.sabre.api.sacs.rest.domain.bargainfindermax.RequestorID;
import com.sabre.api.sacs.rest.domain.bargainfindermax.Source;
import com.sabre.api.sacs.rest.domain.bargainfindermax.TPAExtensions;
import com.sabre.api.sacs.rest.domain.bargainfindermax.TravelerInfoSummary;
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
		//		call.setUrl(environment + "/v3.1.0/shop/flights");
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
		ol.setLocationCode("JFX");
		odi.setOriginLocation(ol);

		DestinationLocation dl = new DestinationLocation();
		dl.setLocationCode("LAX");
		odi.setDestinationLocation(dl);

		odi.setRPH("1");

		originDestinationInfos.add(odi);

		TPAExtensions segTpa = new TPAExtensions();
		Map<String, String> seg = Maps.newHashMap();
		seg.put("Code", "O");
		segTpa.withAdditionalProperty("SegmentType", seg);
		odi.withAdditionalProperty("TPA_Extensions", segTpa);

		//座位数
		List<PassengerTypeQuantity> pqs = Lists.newArrayList();
		PassengerTypeQuantity pq = new PassengerTypeQuantity();
		pq.setCode("ADT");
		pq.setQuantity(1);
		pqs.add(pq);

		AirTravelerAvail avl = new AirTravelerAvail();
		avl.setPassengerTypeQuantity(pqs);
		List<AirTravelerAvail> avls = Lists.newArrayList();
		avls.add(avl);

		TravelerInfoSummary tis = new TravelerInfoSummary();
		tis.setAdditionalProperty("SeatsRequested", new Integer[] { 1 });
		tis.setAirTravelerAvail(avls);

		//请求多少条数据
		RequestType rqt = new RequestType();
		rqt.setName("50ITINS");
		IntelliSellTransaction trans = new IntelliSellTransaction();
		trans.withRequestType(rqt);
		TPAExtensions tpa = new TPAExtensions();
		tpa.withIntelliSellTransaction(trans);

		BargainFinderMaxRequest bfmreq = new BargainFinderMaxRequest()
				.withOTAAirLowFareSearchRQ(new OTAAirLowFareSearchRQ().withAdditionalProperty("ResponseType", "OTA")
						.withAdditionalProperty("ResponseVersion", "3.1.0")
						.withAdditionalProperty("Target", "Production").withAdditionalProperty("version", "3.1.0")
						.withPOS(pos).withOriginDestinationInformation(originDestinationInfos)
						.withTravelerInfoSummary(tis).withTPAExtensions(tpa));
		return bfmreq;
	}
}
