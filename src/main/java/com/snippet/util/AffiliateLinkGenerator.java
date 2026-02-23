package com.snippet.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AffiliateLinkGenerator {

    @Value("${affiliate.kyobo.partner-id:}")
    private String kyoboPartnerId;

    @Value("${affiliate.yes24.partner-id:}")
    private String yes24PartnerId;

    public String generateKyoboLink(String isbn) {
        return "https://product.kyobobook.co.kr/detail/" + isbn
                + (kyoboPartnerId.isEmpty() ? "" : "?partner=" + kyoboPartnerId);
    }

    public String generateYes24Link(String isbn) {
        return "https://www.yes24.com/Product/Search?domain=ALL&query=" + isbn
                + (yes24PartnerId.isEmpty() ? "" : "&partner=" + yes24PartnerId);
    }
}
