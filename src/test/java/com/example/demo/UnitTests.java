package com.example.demo;

import Util.DatabaseConnection;
import Util.HSBCProvidedUtil;
import modules.*;
import org.junit.Test;
import org.springframework.http.HttpHeaders;

import java.util.*;

import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
import static junit.framework.TestCase.*;

public class UnitTests {

    /*
     * tests for isValidHeader
     */

    @Test
    public void isValidHeaderTrue(){
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-custid", "nxqa3cu9r6");
        headers.set("Content-Type", "application/json");
        assertTrue(HSBCProvidedUtil.isValidHeader(headers));

        HttpHeaders headers2 = new HttpHeaders();
        headers2.set("x-custid", "lalala");
        assertTrue(HSBCProvidedUtil.isValidHeader(headers2));
    }

    @Test
    public void isValidHeaderFalse(){
        HttpHeaders headers = new HttpHeaders();
        assertFalse(HSBCProvidedUtil.isValidHeader(headers));

        assertFalse(HSBCProvidedUtil.isValidHeader(null));

        HttpHeaders headers2 = new HttpHeaders();
        headers2.set("x-custid", "");
        assertFalse(HSBCProvidedUtil.isValidHeader(headers2));
    }


    /*
     * tests for isValidDeviation
     */

    @Test
    public void isValidDeviationTrue(){
        assertTrue(PreferenceController.isValidDeviation(0.0));
        assertTrue(PreferenceController.isValidDeviation(1.0));
        assertTrue(PreferenceController.isValidDeviation(5.0));
    }

    @Test
    public void isValidDeviationFalse(){
        assertFalse(PreferenceController.isValidDeviation(-1.0));
        assertFalse(PreferenceController.isValidDeviation(5.1));
        assertFalse(PreferenceController.isValidDeviation(6.0));
    }

    /*
     * tests for isValidAllocations
     */

    @Test
    public void isValidAllocationsTrue() {
        //mock holdings from mock system
        Holding[] holdings = mockHoldingsFor2517972();

        HashMap<String, Integer> allocations = new HashMap<>();
        allocations.put("23456", 30);
        allocations.put("23457", 70);
        assertEquals(PreferenceController.isValidAllocations(holdings, allocations, true),"");
    }
    @Test
    public void isValidAllocationsFalseNotSumTo100() {
        //mock holdings from mock system
        Holding[] holdings = mockHoldingsFor2517972();

        HashMap<String, Integer> allocations = new HashMap<>();
        allocations.put("23456", 30);
        allocations.put("23457", 60);
        assertEquals("Your portfolio preference doesn't sum up to 100", PreferenceController.isValidAllocations(holdings, allocations, true));
    }
    @Test
    public void isValidAllocationsFalseNegativePercentage() {
        //mock holdings from mock system
        Holding[] holdings = mockHoldingsFor2517972();
        
        HashMap<String, Integer> allocations = new HashMap<>();
        allocations.put("23456", -1);
        allocations.put("23457", 101);
        assertEquals("Your allocation contains negative number", PreferenceController.isValidAllocations(holdings, allocations, true));
    }
    @Test
    public void isValidAllocationsFalseInvalidFundId() {
        //mock holdings from mock system
        Holding[] holdings = mockHoldingsFor2517972();

        HashMap<String, Integer> allocations = new HashMap<>();
        allocations.put("1", 40);
        allocations.put("23457", 60);
        assertEquals("Input funds are not contained in your holdings", PreferenceController.isValidAllocations(holdings, allocations, false));
    }

    @Test
    public void prepareRecommendationTest(){
        Recommendation actual_rec = new Recommendation();
        actual_rec.setRecommendationId("2517972");

        String portfolioId = "2517972";
        // If DB has already existed the record, delete it
        DatabaseConnection.deletePortfolioRecord(portfolioId);

        // Construct portfolio
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-custid", "nxqa3cu9r6");
        headers.set("Content-Type", "application/json");

        Portfolio portfolio = new Portfolio();
        portfolio.setId(portfolioId);
        portfolio.setDeviation( "5");
        portfolio.setType("fund");
        portfolio.setAllocations(prepareAllocationsFor2517972());

        //construct mock holdings
        Holding[] holdings = mockHoldingsFor2517972();

        //build correct recommendation
        Recommendation expected_rec = new Recommendation();
        expected_rec.setRecommendationId("2517972");
        Transaction trans1 = new Transaction();
        trans1.setAction("sell");
        trans1.setFundId(23456);
        trans1.setUnits(822);
        Transaction trans2 = new Transaction();
        trans2.setAction("buy");
        trans2.setFundId(23457);
        trans2.setUnits(1488);
        expected_rec.addTransaction(trans1);
        expected_rec.addTransaction(trans2);
        expected_rec.setExtraCost(24.89);
        try {
            RecommendationController.prepareRecommendation(actual_rec, portfolio, holdings);
            assertThat(actual_rec, sameBeanAs(expected_rec));
        }catch(Exception e){
            System.out.println(e);
        }
    }


    @Test
    public void prepareRecommendationTest_withinDeviation(){
        Recommendation actual_rec = new Recommendation();
        actual_rec.setRecommendationId("2517972");

        String portfolioId = "2517972";
        // If DB has already existed the record, delete it
        DatabaseConnection.deletePortfolioRecord(portfolioId);

        // Construct portfolio
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-custid", "nxqa3cu9r6");
        headers.set("Content-Type", "application/json");
        Portfolio portfolio = new Portfolio();
        portfolio.setId(portfolioId);
        portfolio.setDeviation( "5");
        portfolio.setType("fund");
        portfolio.setAllocations(prepareAllocationsFor2517972ForRecommendation());

        //construct mock holdings
        Holding[] holdings = mockHoldingsFor2517972();

        //build correct recommendation
        Recommendation expected_rec = new Recommendation();
        expected_rec.setRecommendationId(portfolioId);

        try {
            RecommendationController.prepareRecommendation(actual_rec, portfolio, holdings);
            assertThat(actual_rec, sameBeanAs(expected_rec));
        }catch(Exception e){
            System.out.println(e);
        }
    }

    private Holding[] mockHoldingsFor2517972(){
        Holding holding1 = new Holding();
        holding1.setFundId(23457);
        holding1.setUnits(1664);
        holding1.setBalance(114913.62); //type need to change

        Holding holding2 = new Holding();
        holding2.setFundId(23456);
        holding2.setUnits(1569);
        holding2.setBalance(196095.24); //type need to change

        Holding[] holdings = new Holding[2];
        holdings[0] = holding1;
        holdings[1] = holding2;
        return holdings;
    }

    private List<Map<String, Integer>> prepareAllocationsFor2517972(){
        List<Map<String, Integer>> allocations = new ArrayList<>();
        Map<String, Integer> allocationmap = new HashMap<>();
        allocationmap.put("fundId", 23456);
        allocationmap.put("percentage", 30);

        Map<String, Integer> allocationmaptwo = new HashMap<>();
        allocationmaptwo.put("fundId", 23457);
        allocationmaptwo.put("percentage", 70);

        allocations.add(allocationmap);
        allocations.add(allocationmaptwo);
        return allocations;
    }

    private List<Map<String, Integer>> prepareAllocationsFor2517972ForRecommendation() {
        List<Map<String, Integer>> allocations = new ArrayList<>();
        Map<String, Integer> allocationmap = new HashMap<>();
        allocationmap.put("fundId", 23456);
        allocationmap.put("percentage", 63);

        Map<String, Integer> allocationmaptwo = new HashMap<>();
        allocationmaptwo.put("fundId", 23457);
        allocationmaptwo.put("percentage", 37);

        allocations.add(allocationmap);
        allocations.add(allocationmaptwo);
        return allocations;
    }

    @Test
    public void prepareCategoryRecommendationBuyTest() {
        String portfolioId = "9876531";
        String custId = "t8ej8u8q5n";

        // If DB has already existed the record, delete it
        DatabaseConnection.deletePortfolioRecord(portfolioId);

        // Construct portfolio
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-custid", "t8ej8u8q5n");
        headers.set("Content-Type", "application/json");
        HashMap<String, Integer> allocations = new HashMap<>();
        allocations.put("category", 2);
        allocations.put("percentage", 40);
        HashMap<String, Integer> allocationsTwo = new HashMap<>();
        allocationsTwo.put("category", 3);
        allocationsTwo.put("percentage", 60);
        Portfolio portfolio = new Portfolio();
        portfolio.setId(portfolioId);
        portfolio.setDeviation( "5");
        portfolio.setType("category");
        portfolio.setAllocations(Arrays.asList(allocations, allocationsTwo));

        //construct mock holdings
        List<Holding> holdings = new ArrayList<>();
        Holding h1 = new Holding();
        h1.setFundId(23503);
        h1.setUnits(400);
        h1.setBalance(59245.25);
        holdings.add(h1);
        double totalBalance = 366945.20999999996;
        int category = 3;

        //build correct recommendation
        Transaction trans1 = new Transaction();
        trans1.setAction("buy");
        trans1.setFundId(23456);
        trans1.setUnits(1283);
        Transaction trans2 = new Transaction();
        trans2.setAction("buy");
        trans2.setFundId(23503);
        trans2.setUnits(1081);
        Transaction trans3 = new Transaction();
        trans3.setAction("buy");
        trans3.setFundId(23457);
        trans3.setUnits(2394);
        List<Transaction> transactions = Arrays.asList(trans1, trans2, trans3);
        List<Double> extraCosts = Arrays.asList(160952.35, 160950.09, 160948.62);
        CategoryRecommendation expected_rec = new CategoryRecommendation(category, transactions, false, extraCosts);

        try {
            CategoryRecommendation actual_rec = RecommendationController.prepareCategoryRecommendations(custId, category, holdings, portfolio, totalBalance);
            assertThat(actual_rec, sameBeanAs(expected_rec));
        }catch(Exception e){
            System.out.println(e);
        }
    }

    @Test
    public void prepareCategoryRecommendationSellTest(){
        String portfolioId = "9876531";
        String custId = "t8ej8u8q5n";

        // If DB has already existed the record, delete it
        DatabaseConnection.deletePortfolioRecord(portfolioId);

        // Construct portfolio
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-custid", "t8ej8u8q5n");
        headers.set("Content-Type", "application/json");
        HashMap<String, Integer> allocations = new HashMap<>();
        allocations.put("category", 2);
        allocations.put("percentage", 40);
        HashMap<String, Integer> allocationsTwo = new HashMap<>();
        allocationsTwo.put("category", 3);
        allocationsTwo.put("percentage", 60);
        Portfolio portfolio = new Portfolio();
        portfolio.setId(portfolioId);
        portfolio.setDeviation( "5");
        portfolio.setType("category");
        portfolio.setAllocations(Arrays.asList(allocations, allocationsTwo));

        //construct mock holdings
        List<Holding> holdings = new ArrayList<>();
        Holding h1 = new Holding();
        h1.setFundId(23500);
        h1.setUnits(851);
        h1.setBalance(18090.96);
        Holding h2 = new Holding();
        h2.setFundId(23459);
        h2.setUnits(1222);
        h2.setBalance(289609);
        holdings.add(h1);
        holdings.add(h2);
        double totalBalance = 366945.20999999996;
        int category = 2;

        //build correct recommendation
        Transaction trans1 = new Transaction();
        trans1.setAction("sell");
        trans1.setFundId(23500);
        trans1.setUnits(759);
        List<Transaction> transactions = Arrays.asList(trans1);
        List<Double> extraCosts = Arrays.asList(-160892.82);
        CategoryRecommendation expected_rec = new CategoryRecommendation(category, transactions, true, extraCosts);

        try {
            CategoryRecommendation actual_rec = RecommendationController.prepareCategoryRecommendations(custId, category, holdings, portfolio, totalBalance);
            assertThat(actual_rec, sameBeanAs(expected_rec));
        }catch(Exception e){
            System.out.println(e);
        }
    }
}
