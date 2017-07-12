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
import com.sabre.api.sacs.rest.domain.bargainfindermax.BargainFinderMaxRequest;
import com.sabre.api.sacs.rest.domain.bargainfindermax.rq.AirTravelerAvail;
import com.sabre.api.sacs.rest.domain.bargainfindermax.rq.CompanyName;
import com.sabre.api.sacs.rest.domain.bargainfindermax.rq.DestinationLocation;
import com.sabre.api.sacs.rest.domain.bargainfindermax.rq.IntelliSellTransaction;
import com.sabre.api.sacs.rest.domain.bargainfindermax.rq.OTAAirLowFareSearchRQ;
import com.sabre.api.sacs.rest.domain.bargainfindermax.rq.OriginDestinationInformation;
import com.sabre.api.sacs.rest.domain.bargainfindermax.rq.OriginLocation;
import com.sabre.api.sacs.rest.domain.bargainfindermax.rq.POS;
import com.sabre.api.sacs.rest.domain.bargainfindermax.rq.PassengerTypeQuantity;
import com.sabre.api.sacs.rest.domain.bargainfindermax.rq.RequestType;
import com.sabre.api.sacs.rest.domain.bargainfindermax.rq.RequestorID;
import com.sabre.api.sacs.rest.domain.bargainfindermax.rq.Source;
import com.sabre.api.sacs.rest.domain.bargainfindermax.rq.TPAExtensions;
import com.sabre.api.sacs.rest.domain.bargainfindermax.rq.TPAExtensions___;
import com.sabre.api.sacs.rest.domain.bargainfindermax.rq.TravelerInfoSummary;
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
		//		BfmV310Response response = call.doCall(BfmV310Response.class, context);
		//		context.putResult("BargainFinderMaxResponse", response);

		//返回null则workflow终止
		return null;
	}

	private BargainFinderMaxRequest generateRequest() {
		List<OriginDestinationInformation> originDestinationInfos = new ArrayList<>();

		//<POS>
		List<Source> sourceList = new ArrayList<>();
		//do not forget set the PCC code
		//requestorId上不能出现PseudoCityCode
		RequestorID requestorID = new RequestorID().withID("1").withType("1")
				.withCompanyName(new CompanyName().withCode("TN"));
		Source source = new Source().withRequestorID(requestorID).withAdditionalProperty("PseudoCityCode",
				config.getRestProperty("group"));

		sourceList.add(source);
		POS pos = new POS().withSource(sourceList);

		//出发到达信息
		//<OriginDestinationInformation
		OriginDestinationInformation odi1 = odi1();
		OriginDestinationInformation odi2 = odi2();
		originDestinationInfos.add(odi1);
		originDestinationInfos.add(odi2);

		//TravelPreferences  TODO

		//<TravelerInfoSummary>
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
		//<TPAExtensions>
		RequestType rqt = new RequestType();
		rqt.setName("50ITINS");
		IntelliSellTransaction trans = new IntelliSellTransaction();
		trans.withRequestType(rqt);
		TPAExtensions___ tpa = new TPAExtensions___();
		tpa.withIntelliSellTransaction(trans);

		//OTAAirLowFareSearchRQ 不能出现version
		BargainFinderMaxRequest bfmreq = new BargainFinderMaxRequest()
				.withOTAAirLowFareSearchRQ(new OTAAirLowFareSearchRQ().withAdditionalProperty("ResponseType", "OTA")
						.withAdditionalProperty("ResponseVersion", "3.1.0")
						.withAdditionalProperty("Target", "Production").withPOS(pos)
						.withOriginDestinationInformation(originDestinationInfos).withTravelerInfoSummary(tis)
						.withTPAExtensions(tpa));
		return bfmreq;
	}

	private OriginDestinationInformation odi1() {
		OriginDestinationInformation odi = new OriginDestinationInformation();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, 1);
		odi.setDepartureDateTime(sdf.format(cal.getTime()));

		OriginLocation ol = new OriginLocation();
		ol.setLocationCode("DFW");
		odi.setOriginLocation(ol);

		DestinationLocation dl = new DestinationLocation();
		dl.setLocationCode("CDG");
		odi.setDestinationLocation(dl);

		odi.setRPH("1");

		TPAExtensions segTpa = new TPAExtensions();
		Map<String, String> seg = Maps.newHashMap();
		seg.put("Code", "O");
		segTpa.withAdditionalProperty("SegmentType", seg);
		odi.withAdditionalProperty("TPA_Extensions", segTpa);
		return odi;
	}

	private OriginDestinationInformation odi2() {
		OriginDestinationInformation odi = new OriginDestinationInformation();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, 10);
		odi.setDepartureDateTime(sdf.format(cal.getTime()));

		OriginLocation ol = new OriginLocation();
		ol.setLocationCode("CDG");
		odi.setOriginLocation(ol);

		DestinationLocation dl = new DestinationLocation();
		dl.setLocationCode("DFW");
		odi.setDestinationLocation(dl);

		odi.setRPH("2");

		TPAExtensions segTpa = new TPAExtensions();
		Map<String, String> seg = Maps.newHashMap();
		seg.put("Code", "O");
		segTpa.withAdditionalProperty("SegmentType", seg);
		odi.withAdditionalProperty("TPA_Extensions", segTpa);
		return odi;
	}
}
