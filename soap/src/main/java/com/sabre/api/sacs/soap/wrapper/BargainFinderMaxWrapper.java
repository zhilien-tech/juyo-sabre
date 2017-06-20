package com.sabre.api.sacs.soap.wrapper;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Controller;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;

import com.sabre.api.sacs.contract.bargainfindermax.OTAAirLowFareSearchRQ;
import com.sabre.api.sacs.contract.bargainfindermax.OTAAirLowFareSearchRS;
import com.sabre.api.sacs.soap.callback.HeaderCallback;
import com.sabre.api.sacs.soap.callback.HeaderComposingCallback;
import com.sabre.api.sacs.soap.common.GenericRequestWrapper;

@Controller
@Scope("prototype")
public class BargainFinderMaxWrapper extends GenericRequestWrapper<OTAAirLowFareSearchRQ, OTAAirLowFareSearchRS> {

    @Autowired
    @Qualifier("bargainFinderMaxHeaderComposingCallback")
    private HeaderComposingCallback headerCallback;



    @Override
    protected List<ClientInterceptor> interceptors() {
        return null;
    }

    @Override
    protected Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("com.sabre.api.sacs.contract.bargainfindermax");
        return marshaller;
    }

    @Override
    protected HeaderCallback callback() {
        return headerCallback;
    }

}
