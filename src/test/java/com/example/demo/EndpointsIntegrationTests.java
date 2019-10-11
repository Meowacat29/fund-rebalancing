package com.example.demo;

import Util.DatabaseConnection;
import Util.HSBCProvidedUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import modules.Allocation;
import modules.Holding;
import modules.Recommendation;
import modules.Transaction;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;


import java.util.HashMap;
import java.util.Map;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class EndpointsIntegrationTests {
    public final static String RECOMMENDATION_ID = "45678";
    public final static String PORTFOLIO_ID_VALID = "2517972";
    public final static String X_CUST_ID_VALID = "nxqa3cu9r6";

    @Before
    public void setUp() {

    }

	@Test
	public void contextLoads() {
	}

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void homeResponse() {
		String body = this.restTemplate.getForObject("/index", String.class);
		assertThat(body).isEqualTo("Welcome to Rebalance Fund Service System - Team REST!");
	}

	@Test
	public void createInitialPreferenceTest404InvalidPortfolioId() {
        String portfolioId = "1";
        DatabaseConnection.deletePortfolioRecord(portfolioId);
        // Construct request body
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-custid", "nxqa3cu9r6");
        headers.set("Content-Type", "application/json");
        JSONObject data = new JSONObject();
        data.put("id",portfolioId);
        data.put("deviation", "3.0");
        data.put("type", "fund");
        data.put("allocations", prepareAllocationsFor2517972());
        HttpEntity<JSONObject> request = new HttpEntity<JSONObject>(data, headers);
        ResponseEntity<String> response = restTemplate.exchange("/portfolio/"+ portfolioId, HttpMethod.POST, request, String.class);
		assertThat(response.getStatusCode().value()).isEqualTo(404);
	}

    @Test
    public void createInitialPreferenceTest400InvalidDeviation() {
        String portfolioId = "2517972";
        // If DB has already existed the record, delete it
        DatabaseConnection.deletePortfolioRecord(portfolioId);

        // Construct request body
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-custid", "nxqa3cu9r6");
        headers.set("Content-Type", "application/json");
        JSONObject data = new JSONObject();
        data.put("id",portfolioId);
        data.put("deviation", "6.0");
        data.put("type", "fund");
        data.put("allocations", prepareAllocationsFor2517972());
        HttpEntity<JSONObject> request = new HttpEntity<JSONObject>(data, headers);
        ResponseEntity<String> response = restTemplate.exchange("/portfolio/"+ portfolioId, HttpMethod.POST, request, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    public void createInitialPreferenceTest400InvalidFundId() {
        String portfolioId = "2517972";
        // If DB has already existed the record, delete it
        DatabaseConnection.deletePortfolioRecord(portfolioId);

        // Construct request body
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-custid", "nxqa3cu9r6");
        headers.set("Content-Type", "application/json");
        JSONObject data = new JSONObject();
        data.put("id",portfolioId);
        data.put("deviation", "6.0");
        data.put("type", "fund");
        data.put("allocations", prepareAllocationsFor2517972());
        HttpEntity<JSONObject> request = new HttpEntity<JSONObject>(data, headers);
        ResponseEntity<String> response = restTemplate.exchange("/portfolio/"+ portfolioId, HttpMethod.POST, request, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }
    @Test
    public void createInitialPreferenceTest400NotSumTo100() {
        String portfolioId = "2517972";
        // If DB has already existed the record, delete it
        DatabaseConnection.deletePortfolioRecord(portfolioId);

        // Construct request body
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-custid", "nxqa3cu9r6");
        headers.set("Content-Type", "application/json");
        JSONObject data = new JSONObject();
        data.put("id",portfolioId);
        data.put("deviation", "6.0");
        data.put("type", "fund");
        data.put("allocations", prepareAllocationsFor2517972NotSumTo100());
        HttpEntity<JSONObject> request = new HttpEntity<JSONObject>(data, headers);
        ResponseEntity<String> response = restTemplate.exchange("/portfolio/"+ portfolioId, HttpMethod.POST, request, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }
    @Test
    public void createInitialPreferenceTest400NegativePercentage() {
        String portfolioId = "2517972";
        // If DB has already existed the record, delete it
        DatabaseConnection.deletePortfolioRecord(portfolioId);

        // Construct request body
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-custid", "nxqa3cu9r6");
        headers.set("Content-Type", "application/json");
        JSONObject data = new JSONObject();
        data.put("id",portfolioId);
        data.put("deviation", "6.0");
        data.put("type", "fund");
        data.put("allocations", prepareAllocationsFor2517972NegNumber());
        HttpEntity<JSONObject> request = new HttpEntity<JSONObject>(data, headers);
        ResponseEntity<String> response = restTemplate.exchange("/portfolio/"+ portfolioId, HttpMethod.POST, request, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }
    @Test
    public void createInitialPreferenceTest404InvalidCustomerHeader() {
        String portfolioId = "2517972";
        // If DB has already existed the record, delete it
        DatabaseConnection.deletePortfolioRecord(portfolioId);

        // Construct request body
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-custid", "nxqa3cu9r61d");
        headers.set("Content-Type", "application/json");
        JSONObject data = new JSONObject();
        data.put("id",portfolioId);
        data.put("deviation", "6.0");
        data.put("type", "fund");
        data.put("allocations", prepareAllocationsFor2517972());
        HttpEntity<JSONObject> request = new HttpEntity<JSONObject>(data, headers);
        ResponseEntity<String> response = restTemplate.exchange("/portfolio/"+ portfolioId, HttpMethod.POST, request, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }
    @Test
    public void createInitialPreferenceTest200() {
        String portfolioId = "2517972";
        // If DB has already existed the record, delete it
        DatabaseConnection.deletePortfolioRecord(portfolioId);

        // Construct request body
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-custid", "nxqa3cu9r6");
        headers.set("Content-Type", "application/json");
        List<Map<String, Integer>> allocations = prepareAllocationsFor2517972();
        JSONObject data = new JSONObject();
        data.put("id",portfolioId);
        data.put("deviation", 5.0);
        data.put("type", "fund");
        data.put("allocations", allocations);
        HttpEntity<JSONObject> request = new HttpEntity<JSONObject>(data, headers);
        ResponseEntity<String> response = restTemplate.exchange("/portfolio/"+ portfolioId, HttpMethod.POST, request, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    private List<Map<String, Integer>> prepareAllocationsFor2517972(){
        Map<String, Integer> allocation = new HashMap<>();
        allocation.put("fundId", 23456);
        allocation.put("percentage", 30);
        Map<String, Integer> allocationTwo = new HashMap<>();
        allocationTwo.put("fundId", 23457);
        allocationTwo.put("percentage", 70);
        return Arrays.asList(allocation, allocationTwo);
    }

    private List<Map<String, Integer>> prepareAllocationsFor2517972NotSumTo100(){
        Map<String, Integer> allocation = new HashMap<>();
        allocation.put("fundId", 23456);
        allocation.put("percentage", 10);
        Map<String, Integer> allocationTwo = new HashMap<>();
        allocationTwo.put("fundId", 23457);
        allocationTwo.put("percentage", 70);
        return Arrays.asList(allocation, allocationTwo);
    }

    private List<Map<String, Integer>> prepareAllocationsFor2517972NegNumber(){
        Map<String, Integer> allocation = new HashMap<>();
        allocation.put("fundId", 23456);
        allocation.put("percentage", -10);
        Map<String, Integer> allocationTwo = new HashMap<>();
        allocationTwo.put("fundId", 23457);
        allocationTwo.put("percentage", 110);
        return Arrays.asList(allocation, allocationTwo);
    }

    @Test
    public void createInitialCategoryPreferenceTest200() {
        String portfolioId = "9876531";
        // If DB has already existed the record, delete it
        DatabaseConnection.deletePortfolioRecord(portfolioId);

        // Construct request body
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-custid", "t8ej8u8q5n");
        headers.set("Content-Type", "application/json");
        HashMap<String, Integer> allocations = new HashMap<>();
        allocations.put("category", 2);
        allocations.put("percentage", 40);
        HashMap<String, Integer> allocationsTwo = new HashMap<>();
        allocationsTwo.put("category", 3);
        allocationsTwo.put("percentage", 60);
        JSONObject data = new JSONObject();
        data.put("id",portfolioId);
        data.put("deviation", "5.0");
        data.put("type", "category");
        data.put("allocations", Arrays.asList(allocations, allocationsTwo));
        HttpEntity<JSONObject> request = new HttpEntity<JSONObject>(data, headers);
        ResponseEntity<String> response = restTemplate.exchange("/portfolio/"+ portfolioId, HttpMethod.POST, request, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    public void getInitialPreference200() {
        String portfolioId = "2517972";
        createInitialPreferenceTest200();
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-custid", "nxqa3cu9r6");
        headers.set("Content-Type", "application/json");
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity<String> response = this.restTemplate.exchange("/portfolio/"+portfolioId, HttpMethod.GET, entity, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("{\"id\":\""+portfolioId+"\",\"deviation\":\"5.0\",\"type\":\"fund\",\"allocations\":[{\"fundId\":23456,\"percentage\":30},{\"fundId\":23457,\"percentage\":70}]}");
    }

    @Test
    public void allocationsPUT200() {
	    String portfolioId = "2517972";

	    // Create initial preference (23456 30%, 23457 70%) for portfolioId 2517972 if it doesn't exist
        createInitialPreferenceTest200();

        // Construct request body
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-custid", "nxqa3cu9r6");
        headers.set("Content-Type", "application/json");
        JSONArray newAllocations = new JSONArray();
        JSONObject data1 = new JSONObject();
        data1.put("fundId",23456);
        data1.put("percentage",20);
        newAllocations.add(data1);
        JSONObject data2 = new JSONObject();
        data2.put("fundId",23457);
        data2.put("percentage",80);
        newAllocations.add(data2);
        HttpEntity<String> request = new HttpEntity<String>(newAllocations.toString(), headers);
        ResponseEntity<String> response = restTemplate.exchange("/portfolio/"+ portfolioId +"/allocations", HttpMethod.PUT, request, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);

    }

	@Test
	public void deviationPUT200() {
        String portfolioId = "2517972";

        // Create initial preference (23456 30%, 23457 70%) with deviation = 5.0
        createInitialPreferenceTest200();

		MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
		Map map = new HashMap<String, String>();
		map.put("Content-Type", "application/json");
		headers.setAll(map);

		Map requestBody = new HashMap();
		requestBody.put("deviation", "4.0");

		HttpEntity<?> request = new HttpEntity<>(requestBody, headers);
		ResponseEntity<Map> response = this.restTemplate.exchange("/portfolio/" + portfolioId + "/deviation", HttpMethod.PUT, request, Map.class);
		Map<String, Double> responseMap = response.getBody();
		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(responseMap.get("deviation")).isEqualTo(4.0);
	}

    @Test
    public void deviationPUT400() {
        String portfolioId = "2517972";

        // Create initial preference (23456 30%, 23457 70%) with deviation = 5.0
        createInitialPreferenceTest200();

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
        Map map = new HashMap<String, String>();
        map.put("Content-Type", "application/json");
        headers.setAll(map);

        Map requestBody = new HashMap();
        requestBody.put("deviation", "6.0");

        HttpEntity<?> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = this.restTemplate.exchange("/portfolio/" + portfolioId + "/deviation", HttpMethod.PUT, request, Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }


    @Test
    public void getRecommendationPOST200() {
        String portfolioId = "2517972";
        // If DB has already existed the record, delete it
        DatabaseConnection.deletePortfolioRecord(portfolioId);
        createInitialPreferenceTest200();

        HttpHeaders header = new HttpHeaders();
        header.set("x-custid", "nxqa3cu9r6");
        header.set("Content-Type", "application/json");
        HttpEntity entity = new HttpEntity(header);
        ResponseEntity<Recommendation> response = restTemplate.exchange("/portfolio/"+ portfolioId+"/rebalance", HttpMethod.POST, entity, Recommendation.class);

        try {
            System.out.println(new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(response.getBody()));
        } catch (Exception e) {
            System.out.println(e);
        }
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertTrue(response.getBody() instanceof Recommendation);
    }

    @Test
    public void getCategoryRecommendationsPOST200() {
        String portfolioId = "9876531";
        // If DB has already existed the record, delete it
        DatabaseConnection.deletePortfolioRecord(portfolioId);
        createInitialCategoryPreferenceTest200();

        HttpHeaders header = new HttpHeaders();
        header.set("x-custid", "t8ej8u8q5n");
        header.set("Content-Type", "application/json");
        HttpEntity entity = new HttpEntity(header);
        ResponseEntity<List<Recommendation>> response = restTemplate.exchange("/portfolio/" + portfolioId + "/category_rebalance", HttpMethod.POST, entity, new ParameterizedTypeReference<List<Recommendation>>() {});

        try {
            System.out.println(new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(response.getBody()));
        } catch (Exception e) {
            System.out.println(e);
        }
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        List<Recommendation> recommendations = response.getBody();
        for (Recommendation recommendation : recommendations) {
            assertTrue(recommendation instanceof Recommendation);
        }
    }

    @Test
    public void getRecommendationPOST404NoneExistingCustomerIdOnMock() {
        String portfolioId = "2517972";
        HttpHeaders header = new HttpHeaders();
        header.set("x-custid", "acoolname");
        header.set("Content-Type", "application/json");
        HttpEntity entity = new HttpEntity(header);
        ResponseEntity<Recommendation> response = restTemplate.exchange("/portfolio/"+portfolioId+"/rebalance", HttpMethod.POST, entity, Recommendation.class);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    public void getRecommendationPOST400NoneExistingPortfolioUnderCustomer() {
        String portfolioId = "2";
        HttpHeaders header = new HttpHeaders();
        header.set("x-custid", "nxqa3cu9r6");
        header.set("Content-Type", "application/json");
        HttpEntity entity = new HttpEntity(header);
        ResponseEntity<Recommendation> response = restTemplate.exchange("/portfolio/"+portfolioId+"/rebalance", HttpMethod.POST, entity, Recommendation.class);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    public void executeRecommendation_200() {
        String recommendationId = "613";
        String portfolioId = "2517972";
        Transaction transaction1 = new Transaction();
        transaction1.setAction("buy");
        transaction1.setUnits(10);
        transaction1.setFundId(23456);
        Transaction transaction2 = new Transaction();
        transaction2.setAction("sell");
        transaction2.setUnits(20);
        transaction2.setFundId(23457);
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction1);
        transactions.add(transaction2);

        HttpHeaders header = new HttpHeaders();
        header.set("x-custid", "nxqa3cu9r6");
        header.set("Content-Type", "application/json");

        try {
            int originalUnits23456= -1;
            int originalUnits23457= -1;
            Holding[] holdings = HSBCProvidedUtil.getHoldings(header,portfolioId);
            if(holdings!=null && holdings.length>0) {
                HashMap<String, Holding> holdingMap = HSBCProvidedUtil.getHoldingAmount(holdings);
                originalUnits23456 = holdingMap.get("23456").getUnits();
                originalUnits23457 = holdingMap.get("23457").getUnits();
            }

            // Remove any recommendations and transactions with matching id
            DatabaseConnection.deleteRecommendations(recommendationId);
            DatabaseConnection.deleteTransactions(recommendationId);

            // Create new recommendation and transactions for portfolio
            DatabaseConnection.insertRecommendations(recommendationId, portfolioId);
            DatabaseConnection.insertTransactions(recommendationId, transactions);

            // Call execute endpoint
            HttpEntity entity = new HttpEntity(header);
            String url = "/portfolio/" + portfolioId + "/recommendation/" + recommendationId + "/execute";
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            assertThat(response.getStatusCode().value()).isEqualTo(200);

            int newUnits23456= -1;
            int newUnits23457 = -1;
            Holding[] newholdings = HSBCProvidedUtil.getHoldings(header,portfolioId);
            if(newholdings!=null && newholdings.length>0) {
                HashMap<String, Holding> newholdingMap = HSBCProvidedUtil.getHoldingAmount(newholdings);
                newUnits23456 = newholdingMap.get("23456").getUnits();
                newUnits23457 = newholdingMap.get("23457").getUnits();
            }

            assertEquals(originalUnits23456,newUnits23456 -10);
            assertEquals(originalUnits23457, newUnits23457 + 20);
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
        }
    }

    @Test
    public void updateRecommendation200() {
        HttpHeaders header = new HttpHeaders();
        header.set("x-custid", X_CUST_ID_VALID);
        header.setContentType(MediaType.APPLICATION_JSON);
        List<Transaction> transactions = new ArrayList<>(Arrays.asList(new Transaction("buy", 23456, 5)));
        List<Transaction> newTransactions = new ArrayList<>(Arrays.asList(new Transaction("buy", 23456, -2)));

        try {
            // Remove any recommendations and transactions with matching id
            DatabaseConnection.deleteRecommendations(RECOMMENDATION_ID);
            DatabaseConnection.deleteTransactions(RECOMMENDATION_ID);

            // Create new recommendation and transactions for portfolio
            DatabaseConnection.insertRecommendations(RECOMMENDATION_ID, PORTFOLIO_ID_VALID);
            DatabaseConnection.insertTransactions(RECOMMENDATION_ID, transactions);

        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
        }

        // Call modify endpoint
        String url = "/portfolio/" + PORTFOLIO_ID_VALID + "/recommendation/" + RECOMMENDATION_ID + "/modify";
        HttpEntity<?> request = new HttpEntity<>(newTransactions, header);
        ResponseEntity<Recommendation> response = restTemplate.exchange(url, HttpMethod.PUT, request, Recommendation.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }
}
