package com.example.demo;

import Util.DatabaseConnection;
import Util.HSBCProvidedUtil;
import modules.Holding;
import modules.Portfolio;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.sql.*;
import java.util.*;

public class PreferenceController {

    /**
     *  Create the initial preference for a portfolio under a customer
     *
     * @param  portfolio the portfolio data is going to be saved into database
     * @param portfolioId the portfolio id of the initial preference
     * @param headers the headers of the request that contains a customer id
     *
     * @return 200 if request succeed, otherwise 400 for bad request, 500 for internal server error
     */
    public static ResponseEntity<String> createInitialPreference(String portfolioId, Portfolio portfolio, HttpHeaders headers){

        JSONObject response = new JSONObject();
        if (portfolio == null) {
            response.put("message", "Portfolio preference information is empty");
            return new ResponseEntity<>(response.toString(), HttpStatus.BAD_REQUEST);
        }
        if(!HSBCProvidedUtil.isValidHeader(headers)){
            response.put("message", "Missing a customer id in the headers");
            return new ResponseEntity<>(response.toString(), HttpStatus.BAD_REQUEST);
        }

        ResponseEntity<String> result = verifyAllocations(portfolioId, HSBCProvidedUtil.convertToAllocationMap(portfolio.getAllocations()), headers, portfolio.categoryType());
        boolean isTypeFund = portfolio.getType().equals("fund");

        if(result != null){
            return result;
        }

        if (!isValidDeviation(Double.parseDouble(portfolio.getDeviation()))) {
            response.put("message", "Your deviation should be in [0,5].");
            return new ResponseEntity<>(response.toString(), HttpStatus.BAD_REQUEST);
        }
        portfolio.setId(portfolioId);

        try {
            Portfolio portfolioFromDB = DatabaseConnection.insertPortfolioRecord(portfolio);
        }catch (Exception e){
            return new ResponseEntity<>(e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(portfolio.toString(), HttpStatus.OK);
    }

    /**
     *  Verify the allocations and fundIds against the holdings of a portfolio under a customer from HSBC Mock System
     *
     * @param portfolioId the portfolio id of the initial preference
     * @param allocations the allocations of the initial preference input by a user
     * @param headers the headers of the request that contains a customer id
     * @param isTypeCategory allocation verification needs to be changed if portfolio type is category
     *
     * @return 200 if allocations are valid, otherwise 400 with error message for bad request, 500 for internal server error
     */

    public static ResponseEntity<String> verifyAllocations(String portfolioId, Map<String, Integer> allocations, HttpHeaders headers, boolean isTypeCategory) {
        //if no holdings, either 404 or 500 depends on HSBC mock system's response
        JSONObject response = new JSONObject();
        try {
            Holding[] holdings = HSBCProvidedUtil.getHoldings(headers, portfolioId);
            // Verify a portfolio id by checking holdings for this portfolio
            if (holdings == null || holdings.length == 0) {
                response.put("message", "There is no funds found in your portfolio");
                return new ResponseEntity<>(response.toString(), HttpStatus.NOT_FOUND);
            }

            String errorMessage = isValidAllocations(holdings, allocations, isTypeCategory);
            if (!errorMessage.equals("")) {
                response.put("message", errorMessage);
                return new ResponseEntity<>(response.toString(), HttpStatus.BAD_REQUEST);
            }
        }catch (Exception e){
            if(e.getMessage().contains("404")){
                return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
            }else{
                // else 500 internal server
                return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return null;
    }

    /**
     *  Verify the allocations contain funds from the holdings of a portfolio under a customer from HSBC Mock System and the allocations percentages sum up to 100
     *
     * @param holdings the holdings of a portfolio under a customer from HSBC Mock System
     * @param allocations the allocations of the initial preference input by a user
     * @param isTypeCategory allocation verification needs to be changed if portfolio type is category
     *
     * @return return "" if allocations are valid, otherwise corresponding error messages
     */

    public static String isValidAllocations(Holding[] holdings, Map<String, Integer> allocations, boolean isTypeCategory){
        int totalPercentage = 0;
        boolean allFundIdsValid = true;
        HashSet<Integer> fundIds = HSBCProvidedUtil.getFundIdsFromHoldings(holdings);
        for (String key : allocations.keySet()) {
            if (!fundIds.contains(Integer.valueOf(key)) && !isTypeCategory) {
                allFundIdsValid = false;
                break;
            }

            if(allocations.get(key).intValue() < 0){
                return "Your allocation contains negative number";
            }
            totalPercentage += allocations.get(key).intValue();
        }

        if(!allFundIdsValid){
            return "Input funds are not contained in your holdings";
        }
        if(totalPercentage != 100){
            return "Your portfolio preference doesn't sum up to 100";
        }
        return "";
    }

    /**
     *  Check if the deviation is within [0,5]
     *
     * @param deviation the deviation of the initial preference
     *
     * @return True if deviation is within [0,5], otherwise, False
     */
    public static boolean isValidDeviation(double deviation){
        return deviation >= 0.0 && deviation <= 5.0;
    }

    /**
     *  Get the initial preference for a portfolio id
     *
     * @param id the portfolio id of the initial preference
     *
     * @return 200 if request succeed, otherwise 400 for bad request, 500 for internal server error
     */
    public static ResponseEntity<Portfolio> getInitialPreference(@PathVariable String id,  HttpHeaders headers) {
        Portfolio portfolio = new Portfolio();
        portfolio.setId(id);

        if (!HSBCProvidedUtil.isValidHeader(headers)) {
            return new ResponseEntity<>(portfolio, HttpStatus.BAD_REQUEST);
        }
        try {
            Connection conn = DatabaseConnection.setUpConnection();
            PreparedStatement statement = conn.prepareStatement("SELECT * from portfolio WHERE  id = " + id);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                String deviation = rs.getString(2);
                portfolio.setDeviation(deviation);
                String type = rs.getString(3);
                portfolio.setType(type);
            }

            PreparedStatement allocationsStatement = conn.prepareStatement("select * from allocations where portfolio_id =" + id);
            ResultSet data = allocationsStatement.executeQuery();

            // HashMap<String, Integer> allocations = new HashMap<>();
            ArrayList<Map<String, Integer>> allocations = new ArrayList<>();

            String type = portfolio.categoryType() ? "category" : "fundId";
            while (data.next()) {
                Map<String, Integer> allocationMap = new HashMap<>();
                Integer fundId = Integer.parseInt(data.getString(1));
                Integer percentage = Integer.parseInt(data.getString(3));
                allocationMap.put(type, fundId);
                allocationMap.put("percentage", percentage);
                allocations.add(allocationMap);
            }
            portfolio.setAllocations(allocations);
            conn.close();

        } catch (Exception e) {

            if(e.getMessage() != null && e.getMessage().contains("404")){
                return new ResponseEntity<>(portfolio,HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(portfolio,HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(portfolio, HttpStatus.OK);
    }

    /**
     *  Update the deviation of a initial preference for a portfolio id
     *
     * @param id the portfolio id of the initial preference
     * @param request the request body that contains new deviation data
     *
     * @return 200 if request succeed, otherwise 400 for bad request, 500 for internal server error
     */
    public static ResponseEntity<Map<String, Double>> updateDeviation(String id, Map<String, Double> request) {
        double deviation = request.getOrDefault("deviation", -1.0);
        if (!isValidDeviation(deviation)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            // create a mysql database connection
            Connection conn = DatabaseConnection.setUpConnection();

            // mysql update statement for portfolio table
            String query = "update portfolio set deviation = ? where id = ?";
            PreparedStatement preparedStmt = conn.prepareStatement(query);
            preparedStmt.setDouble(1, deviation);
            preparedStmt.setInt(2, Integer.parseInt(id));
            preparedStmt.executeUpdate();
            preparedStmt.close();
            conn.close();
        } catch (SQLException e) {
            return new ResponseEntity<>(request, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(request, HttpStatus.OK);
    }

    /**
     *  Update allocations in a initial preference for a portfolio under a customer
     *
     * @param id the portfolio id of the initial preference
     * @param allocations the request body contains new allocation data
     *
     * @return 200 if request succeed, otherwise 400 for bad request, 500 for internal server error
     */
    public static ResponseEntity<String> updateAllocations(String id, List<Map<String, Integer>> allocations, HttpHeaders headers){

        Map<String, Integer> allocationMap;
        boolean isTypeCategory = allocations.get(0).containsKey("category");

        try {
            allocationMap = HSBCProvidedUtil.convertToAllocationMap(allocations);
        }catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        ResponseEntity<String> result = verifyAllocations(id, allocationMap, headers, isTypeCategory);
        if(result != null){
            return result;
        }

        PreferenceController preferenceController = new PreferenceController();
        if (!DatabaseConnection.doesInitialPreferenceExist(id)) {
            return new ResponseEntity<>("Can't update allocations because there is no initial preference under this portfolio.", HttpStatus.BAD_REQUEST);
        }
        // if exceptions happen, then database connection issue and it is internal server error
        try{
            DatabaseConnection.deleteAllocations(id);
            DatabaseConnection.insertAllocations(id, allocationMap);
        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(allocations.toString(), HttpStatus.OK);
    }
}