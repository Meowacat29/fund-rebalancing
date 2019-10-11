package com.example.demo;

import Util.HSBCProvidedUtil;
import modules.Holding;
import org.junit.Test;
import org.springframework.http.HttpHeaders;

import static junit.framework.TestCase.*;

public class MockSystemIntegrationTests {

    @Test
    public void getHoldingsTest200(){
        String portfolioId = "2517972";
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-custid", "nxqa3cu9r6");
        headers.set("Content-Type", "application/json");

        try {
            Holding[] holdings = HSBCProvidedUtil.getHoldings(headers, portfolioId);
            if(holdings == null || holdings.length !=2){
                fail();
            }
            assertTrue(holdings[0].getFundId()==23456);
            assertTrue(holdings[1].getFundId() == 23457);

        }catch(Exception e){
            System.out.println(e);
            fail();
        }
    }

}
