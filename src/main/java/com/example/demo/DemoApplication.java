package com.example.demo;

import Util.HSBCProvidedUtil;
import modules.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@SpringBootApplication
@RestController
public class DemoApplication {

	@GetMapping("/index")
	String home() {
		return "Welcome to Rebalance Fund Service System - Team REST!";
	}

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    /**
     *  Retrieves the portfolios for the customer
     *
     * @param  custId the customers unique id
     *
     * @return 200 with portfolios if request succeed, otherwise 400 for bad request, 500 for internal server error
     */
    @RequestMapping(value = "/portfolios", method = GET)
    @ResponseBody
    public ResponseEntity<HSBCPortfolio[]> getPortfolios(@RequestParam String custId) throws Exception {
        HSBCPortfolio[] portfolios = HSBCProvidedUtil.getPortfolios(custId);
        return new ResponseEntity<>(portfolios, HttpStatus.OK);
    }

    /**
     *  Create the initial preference for a portfolio under a customer
     *
     * @param  portfolio the portfolio data is going to be saved into database
     * @param id the portfolio id of the initial preference
     * @param headers the headers of the request that contains a customer id
     *
     * @return 200 if request succeed, otherwise 400 for bad request, 500 for internal server error
     */
    @RequestMapping(value = "/portfolio/{id}", method = POST, consumes = "application/json")
    @ResponseBody
    public ResponseEntity<String> createInitialPreference(@PathVariable String id, @RequestBody Portfolio portfolio, @RequestHeader HttpHeaders headers) {
        return PreferenceController.createInitialPreference(id,portfolio, headers);
    }

    /**
     *  Update allocations in a initial preference for a portfolio under a customer
     *
     * @param id the portfolio id of the initial preference
     * @param allocations the request body contains new allocation data
     *
     * @return 200 if request succeed, otherwise 400 for bad request, 500 for internal server error
     */
    @RequestMapping(value = "/portfolio/{id}/allocations", method = PUT, consumes = "application/json")
    @ResponseBody
    public ResponseEntity<String> updateAllocation(@PathVariable String id, @RequestBody List<Map<String, Integer>> allocations, @RequestHeader HttpHeaders headers) {
        return PreferenceController.updateAllocations(id, allocations, headers);
    }

    /**
     *  Get the initial preference for a portfolio id
     *
     * @param id the portfolio id of the initial preference
     *
     * @return 200 if request succeed, otherwise 400 for bad request, 500 for internal server error
     */
    @RequestMapping(value = "/portfolio/{id}", method = GET)
    @ResponseBody
    public ResponseEntity<Portfolio> getInitialPreference(@PathVariable String id, @RequestHeader HttpHeaders headers) {
        return PreferenceController.getInitialPreference(id, headers);
    }

    @RequestMapping(value = "/portfolio/{id}/category_holdings", method = GET)
    @ResponseBody
    public ResponseEntity<List<Map<String, Double>>> getCategoryHoldings(@PathVariable String id, @RequestHeader HttpHeaders headers) {
        return RecommendationController.getCategoryHoldings(id, headers);
    }

    /**
     *  Update the deviation of a initial preference for a portfolio id
     *
     * @param id the portfolio id of the initial preference
     * @param request the request body that contains new deviation data
     *
     * @return 200 if request succeed, otherwise 400 for bad request, 500 for internal server error
     */
    @RequestMapping(value = "/portfolio/{id}/deviation", method = PUT, consumes = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Double>> updateDeviation(@PathVariable String id, @RequestBody Map<String, Double> request) {
        return PreferenceController.updateDeviation(id, request);
    }

    /**
     *  Generate a set of recommendation transcations to rebalance the portfolio into the initial preference
     *
     * @param id the portfolio to be rebalanced
     * @param headers the headers of the request that contains a customer id
     *
     * @return 200 if request succeed, otherwise 400 for bad request, 500 for internal server error
     */
    @RequestMapping(value = "/portfolio/{id}/rebalance", method = POST)
    @ResponseBody
    public ResponseEntity<Recommendation> getRebalanceRecommendation(@PathVariable String id, @RequestHeader HttpHeaders headers) {
        return RecommendationController.getRebalanceRecommendation(id,headers);
    }

    /**
     *  Generate a list of recommendations with transactions to rebalance the category portfolio into the initial preference
     *
     * @param id the portfolio to be rebalanced
     * @param headers the headers of the request that contains a customer id
     *
     * @return 200 if request succeed, otherwise 400 for bad request, 500 for internal server error
     */
    @RequestMapping(value = "/portfolio/{id}/category_rebalance", method = POST)
    @ResponseBody
    public ResponseEntity<List<Recommendation>> getCategoryRebalanceTransactions(@PathVariable String id, @RequestHeader HttpHeaders headers) {
        return RecommendationController.getCategoryRebalanceRecomendations(id,headers);
    }

    /**
     *  Executes the set of transactions associated to the given recommendation id.
     *
     * @param id the portfolio to be rebalanced
     * @param recommendation_id the recommendation id of the transactions
     * @param headers the headers of the request that contains a customer id
     *
     * @return 200 if request succeed, otherwise 400 for bad request, 500 for internal server error
     */
    @RequestMapping(value = "/portfolio/{id}/recommendation/{recommendation_id}/execute", method = POST)
    @ResponseBody
    public ResponseEntity<String> executeRecommendation(@PathVariable String id, @PathVariable String recommendation_id, @RequestHeader HttpHeaders headers) {
        return RecommendationController.executeRecommendation(id, recommendation_id, headers);
    }

    /**
     *  Executes the set of transactions associated to the given recommendation id. Duplicate function as executeRecommendation but
     *  returns a portfolio for UI rendering purpose
     *
     * @param id the portfolio to be rebalanced
     * @param recommendation_id the recommendation id of the transactions
     * @param headers the headers of the request that contains a customer id
     *
     * @return 200 if request succeed, otherwise 400 for bad request, 500 for internal server error
     */
    @RequestMapping(value = "/portfolio/{id}/recommendation/{recommendation_id}/executeUI", method = POST)
    @ResponseBody
    public ResponseEntity<HSBCPortfolio[]> executeRecommendationUI(@PathVariable String id, @PathVariable String recommendation_id, @RequestHeader HttpHeaders headers) {
        return RecommendationController.executeRecommendationForUI(id, recommendation_id, headers);
    }

    /**
     *  Modifies the set of transactions associated to the given recommendation id.
     *
     * @param id the portfolio to be modified
     * @param recommendation_id the recommendation id of the transactions
     * @param headers the headers of the request that contains a customer id
     *
     * @return 200 if request succeed, otherwise 400 for bad request, 500 for internal server error
     */
    @RequestMapping(value = "/portfolio/{id}/recommendation/{recommendation_id}/modify", method = PUT)
    @ResponseBody
    public ResponseEntity<Recommendation> updateRecommendation(@PathVariable String id, @PathVariable String recommendation_id, @RequestHeader HttpHeaders headers, @RequestBody List<Transaction> transactions) {
        return RecommendationController.updateRecommendation(id, recommendation_id, headers, transactions);
    }
}