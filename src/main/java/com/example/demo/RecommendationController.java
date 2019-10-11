package com.example.demo;

import Util.DatabaseConnection;
import Util.HSBCProvidedUtil;
import modules.*;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.client.HttpClientErrorException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import static java.lang.Math.abs;

public class RecommendationController {

    private final static String CUSTOMER_ID_KEY = "x-custid";
    public static final Map<String, Double> weights = new HashMap<>();
    static {
        weights.put("1y", 1.0);
        weights.put("5y", 5.0);
        weights.put("9m", 0.9);
        weights.put("3m", 0.3);
        weights.put("6m", 0.6);
        weights.put("10y", 10.0);
        weights.put("3y", 3.0);
    }

    public static ResponseEntity<List<Map<String, Double>>> getCategoryHoldings(@PathVariable String id, @RequestHeader HttpHeaders headers) {
        List<Map<String, Double>> result = new ArrayList<>();

        try {
            String customerId = HSBCProvidedUtil.getCustomerId(headers);
            Holding[] holdings = HSBCProvidedUtil.getHoldings(headers, id);

            if (holdings.length == 0)
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);

            // category to holdings
            Map<Integer, List<Holding>> categoryHoldings = filterIntoCategories(holdings, customerId);

            double totalBalance = 0;
            for (Holding holding : holdings) {
                totalBalance += holding.getBalance().getAmount();
            }

            for (Map.Entry<Integer, List<Holding>> categoryHolding : categoryHoldings.entrySet()) {
                Map<String, Double> entry = new HashMap<>();
                int category = categoryHolding.getKey();
                List<Holding> holdingsForCategory = categoryHolding.getValue();
                double sum = 0;
                for (Holding holding : holdingsForCategory) {
                    sum += holding.getBalance().getAmount();
                }
                entry.put("category", (double) category);
                entry.put("percentage", sum / totalBalance);
                result.add(entry);
            }
        } catch (Exception e) {
            if (e.getMessage().contains("404"))
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    public static ResponseEntity<List<Recommendation>> getCategoryRebalanceRecomendations(@PathVariable String id, @RequestHeader HttpHeaders headers) {
        List<Recommendation> recommendations = new ArrayList<>();
        Portfolio portfolio = PreferenceController.getInitialPreference(id, headers).getBody();

        // proceed only if portfolio preference is of type category
        if (!HSBCProvidedUtil.isValidHeader(headers) || portfolio.getType() == null || !portfolio.getType().equals("category")) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        String customerId = HSBCProvidedUtil.getCustomerId(headers);

        try {
            Holding[] holdings = HSBCProvidedUtil.getHoldings(headers, id);

            if (holdings.length == 0)
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.NOT_FOUND);

            // category to holdings
            Map<Integer, List<Holding>> categoryHoldings = filterIntoCategories(holdings, customerId);

            double totalBalance = 0;
            for (Holding holding : holdings) {
                totalBalance += holding.getBalance().getAmount();
            }

            int numRecommendations = 1;
            List<CategoryRecommendation> categoryRecommendations = new ArrayList<>();
            for (Map.Entry<Integer, List<Holding>> categoryHolding : categoryHoldings.entrySet()) {
                CategoryRecommendation categoryRecommendation =
                        prepareCategoryRecommendations(customerId, categoryHolding.getKey(), categoryHolding.getValue(), portfolio, totalBalance);
                categoryRecommendations.add(categoryRecommendation);
                if (!categoryRecommendation.isSell()) {
                    numRecommendations = 3;
                }
            }

            for (int i=0; i<numRecommendations ; i++) {
                Recommendation recommendation = new Recommendation();
                recommendation.setRecommendationId(Integer.toString(Integer.parseInt(id) + i));
                recommendations.add(recommendation);
            }

            for (CategoryRecommendation categoryRecommendation : categoryRecommendations) {
                if (categoryRecommendation.isSell()) {
                    for (Recommendation recommendation : recommendations) {
                        recommendation.addAllTransactions(categoryRecommendation.getTransactions());
                        recommendation.addExtraCost(categoryRecommendation.getExtraCosts().get(0));
                    }
                } else {
                    for (int i=0; i<recommendations.size(); i++) {
                        if (categoryRecommendation.getTransactions().size() == 0) break;
                        Recommendation recommendation = recommendations.get(i);
                        recommendation.addTransaction(categoryRecommendation.getTransactions().get(i));
                        recommendation.addExtraCost(categoryRecommendation.getExtraCosts().get(i));
                    }
                }
            }

            int dbRecommendations = 0;
            for (int i=0; i<numRecommendations ; i++) {
                if (recommendations.get(i).getTransactions().size() > 0) {
                    insertRecommendation(recommendations.get(i), portfolio);
                    dbRecommendations++;
                }
            }

            if (dbRecommendations == 0) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            }

        } catch (Exception e) {
            if (e.getMessage().contains("404"))
                return new ResponseEntity<>(recommendations, HttpStatus.NOT_FOUND);
            return new ResponseEntity<>(recommendations, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(recommendations, HttpStatus.OK);
    }

    private static Map<Integer, List<Holding>> filterIntoCategories(Holding[] holdings, String customerId) {
        Map<Integer, List<Holding>> categoryHoldings = new HashMap<>();

        for (Holding holding : holdings) {
            FundInfo fundInfo = HSBCProvidedUtil.getFundInfo(customerId, holding.getFundId());
            List<Holding> currHoldings = categoryHoldings.getOrDefault(fundInfo.getCategory(), new ArrayList<>());
            currHoldings.add(holding);
            categoryHoldings.put(fundInfo.getCategory(), currHoldings);
        }

        return categoryHoldings;
    }

    /**
     * Generate a set of recommendation transcations to rebalance the portfolio into the initial preference
     *
     * @param id      the portfolio to be rebalanced
     * @param headers the headers of the request that contains a customer id
     * @return 200 if request succeed, otherwise 400 for bad request, 500 for internal server error
     */
    public static ResponseEntity<Recommendation> getRebalanceRecommendation(@PathVariable String id, @RequestHeader HttpHeaders headers) {
        Recommendation recommendation = new Recommendation();
        recommendation.setRecommendationId(id);     //recommendation_id is currently set to Portfolio_id
        //get the asset mix preference
        Portfolio portfolio = PreferenceController.getInitialPreference(id, headers).getBody();

        if (!HSBCProvidedUtil.isValidHeader(headers)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            Holding[] holdings = HSBCProvidedUtil.getHoldings(headers, id);

            if (holdings == null || holdings.length == 0)
                return new ResponseEntity<>(recommendation, HttpStatus.NOT_FOUND);

            if (prepareRecommendation(recommendation, portfolio, holdings))
                return new ResponseEntity<>(recommendation, HttpStatus.OK);

            insertRecommendation(recommendation, portfolio);

        } catch (Exception e) {
            if (e.getMessage().contains("404")){
                return new ResponseEntity<>(recommendation, HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(recommendation, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(recommendation, HttpStatus.OK);
    }

    /**
     * Insert a recommendation calculated based on a portfolio current holdings and target allocations into the database
     *
     * @param recommendation      the recommendation to be inserted into the database
     *
     */
    public static void insertRecommendation(Recommendation recommendation, Portfolio portfolio) throws SQLException {
        Connection conn = DatabaseConnection.setUpConnection();

        String query = " insert into recommendations (portfolio_id, recommendation_id)"
                + " values (?, ?)  ON DUPLICATE KEY UPDATE recommendation_id=?";
        PreparedStatement preparedStmt_rec = conn.prepareStatement(query);
        preparedStmt_rec.setInt(1, Integer.parseInt(portfolio.getId()));
        preparedStmt_rec.setInt(2, Integer.parseInt(recommendation.getRecommendationId()));
        preparedStmt_rec.setInt(3, Integer.parseInt(recommendation.getRecommendationId()));
        preparedStmt_rec.execute();

        for (int i = 0; i < recommendation.getTransactions().size(); i++) {
            Transaction t = recommendation.getTransactions().get(i);

            // mysql insert statement for transaction table
            String query0 = " insert into transactions (recommendation_id, fund_id, action, units)"
                    + " values (?, ?, ?, ?) ON DUPLICATE KEY UPDATE fund_id=?, action=?, units=?";
            PreparedStatement preparedStmt_trans = conn.prepareStatement(query0);
            preparedStmt_trans.setInt(1, Integer.parseInt(recommendation.getRecommendationId()));
            preparedStmt_trans.setInt(2, t.getFundId());
            preparedStmt_trans.setString(3, t.getAction());
            preparedStmt_trans.setInt(4, t.getUnits());
            preparedStmt_trans.setInt(5, t.getFundId());
            preparedStmt_trans.setString(6, t.getAction());
            preparedStmt_trans.setInt(7, t.getUnits());
            preparedStmt_trans.execute();
        }

        conn.close();
    }

    /**
     * Generate a recommendation based on calculated the difference between a portfolio current holdings and target allocations
     *
     * @param customerId      a recommendation to be populated
     * @param category        the target allocations of a portfolio
     * @param holdings        the portfolio holdings
     * @param portfolio       the current portfolio
     * @param totalBalance    the total balance of all holdings for this portfolio
     *
     * @return CategoryRecommendation that specifies what transactions to make to balance the specified category
     *
     */
    public static CategoryRecommendation prepareCategoryRecommendations(String customerId, int category, List<Holding> holdings, Portfolio portfolio, double totalBalance) {
        double deviation = Double.parseDouble(portfolio.getDeviation());
        double allocation = HSBCProvidedUtil.convertToAllocationMap(portfolio.getAllocations()).get(Integer.toString(category));

        double c_balance = 0;
        for (Holding holding : holdings) {
            c_balance += holding.getBalance().getAmount();
        }

        double c_percentage = c_balance * 100.0f / totalBalance;
        if (abs(allocation - c_percentage) <= deviation) {
            return new CategoryRecommendation(category, new ArrayList<>(), false, new ArrayList<>());
        }

        Map<Integer, Holding> holdingsMap = new HashMap<>();
        for (Holding holding : holdings) {
            holdingsMap.put(holding.getFundId(), holding);
        }

        FundInfo[] fundInfos = getScoredFundInfos(customerId, category);
        List<Transaction> transactions = new ArrayList<>();
        List<Double> extraCosts = new ArrayList<>();

        boolean isSell = false;
        // sell current holdings with lowest scores first
        if ((allocation - c_percentage) < 0) {
            isSell = true;

            for (int i=fundInfos.length-1; i>=0; i--) {
                int fundId = fundInfos[i].getFundId();
                if (!holdingsMap.containsKey(fundId)) continue;
                Holding holding = holdingsMap.get(fundId);

                Transaction transaction = new Transaction();
                transaction.setFundId(fundId);

                double unitPrice = fundInfos[i].getPrice().getAmount();
                int units = (int) Math.round((totalBalance * (allocation / 100.f) - c_balance) / unitPrice);

                if (abs(units) > holding.getUnits()) {
                    units = holding.getUnits();
                }

                if (units == 0)
                    continue;

                c_balance -= abs(units) * unitPrice;
                extraCosts.add(Math.round(units * unitPrice * 100.0) / 100.0);
                transaction.setAction("sell");
                transaction.setUnits(abs(units));
                transactions.add(transaction);
            }
        } else {
            // highest score fund
            if (fundInfos.length > 0) {
                Transaction transaction = buildTransaction(fundInfos[0], "buy", totalBalance, allocation, c_balance, extraCosts);
                if (transaction.getUnits() != 0) {
                    transactions.add(transaction);
                }
            }

            // owned fund
            for (FundInfo fundInfo : fundInfos) {
                if (holdingsMap.containsKey(fundInfo.getFundId())) {
                    Transaction transaction = buildTransaction(fundInfo, "buy", totalBalance, allocation, c_balance, extraCosts);
                    if (transaction.getUnits() != 0) {
                        transactions.add(transaction);
                    }
                    break;
                }
            }

            // lowest per unit cost fund
            double lowestUnitPrice = fundInfos[0].getPrice().getAmount();
            int lowestUnitPriceFundId = fundInfos[0].getFundId();
            for (FundInfo fundInfo : fundInfos) {
                double amount = fundInfo.getPrice().getAmount();
                if (amount < lowestUnitPrice) {
                    lowestUnitPrice = amount;
                    lowestUnitPriceFundId = fundInfo.getFundId();
                }
            }

            for (FundInfo fundInfo : fundInfos) {
                if (fundInfo.getFundId() == lowestUnitPriceFundId) {
                    Transaction transaction = buildTransaction(fundInfo, "buy", totalBalance, allocation, c_balance, extraCosts);
                    if (transaction.getUnits() != 0) {
                        transactions.add(transaction);
                    }
                    break;
                }
            }
        }

        return new CategoryRecommendation(category, transactions, isSell, extraCosts);
    }

    private static FundInfo[] getScoredFundInfos(String customerId, int category) {
        FundInfo[] fundInfos = HSBCProvidedUtil.getFundsInfo(customerId, category);
        for (FundInfo fundInfo : fundInfos) {
            Map<String, Double> averageReturns = fundInfo.getAverageReturns();
            for (Map.Entry<String, Double> performance : averageReturns.entrySet()) {
                String time = performance.getKey();
                double avgReturn = performance.getValue();
                averageReturns.put(time, avgReturn * weights.getOrDefault(time, 0.0));
            }
        }

        Arrays.sort(fundInfos, (FundInfo f1, FundInfo f2) -> (int) (f2.getAverageScore() - f1.getAverageScore()));
        return fundInfos;
    }

    private static Transaction buildTransaction(FundInfo fundInfo, String action, double totalBalance,
                                                double allocation, double currentBalance, List<Double> extraCosts) {
        Transaction transaction = new Transaction();
        transaction.setFundId(fundInfo.getFundId());

        //compute the transaction units
        double unitPrice = fundInfo.getPrice().getAmount();
        int units = (int) Math.round((totalBalance * (allocation / 100.f) - currentBalance) / unitPrice);

        transaction.setAction(action);
        transaction.setUnits(abs(units));

        extraCosts.add(Math.round(units * unitPrice * 100.0) / 100.0);
        return transaction;
    }


    /**
     * Generate a recommendation based on calculated the difference between a portfolio current holdings and target allocations
     *
     * @param recommendation      a recommendation to be populated
     * @param portfolio           the target allocations of a portfolio
     * @param holdings            the current allocations of this portfolio
     *
     * @return True if a recommendation needed for rebalancing a portfolio and the recommendation is populated; otherwise, False when no rebalance is needed for this portfolio
     *
     */
    public static boolean prepareRecommendation(Recommendation recommendation, Portfolio portfolio, Holding[]
            holdings) throws Exception {
        //compute the percentage of each holding and compare with preference percentage
        Map<String, Integer> allocations = HSBCProvidedUtil.convertToAllocationMap(portfolio.getAllocations());
        HashMap<String, Holding> holdingsMap = HSBCProvidedUtil.getHoldingAmount(holdings);

        int totalBalance = 0;
        for (int i = 0; i < holdings.length; i++) {
            totalBalance += holdings[i].getBalance().getAmount();
        }
        double deviation = Double.parseDouble(portfolio.getDeviation());
        boolean inRange = true;

        for (String fundId : allocations.keySet()) {
            //compute a percentage of each fund holding
            double f_balance = holdingsMap.get(fundId).getBalance().getAmount();
            double f_percentage = f_balance * 100.0f / totalBalance;
            // if any percentage difference is outside of the range of the deviation that user set, a recommendation should be provided
            double percentage_diff = (double) allocations.get(fundId) - f_percentage;
            if (abs(percentage_diff) > deviation) {
                inRange = false;
            }
        }

        // if all percentage differences are within the range of the deviation, no recommendation is needed
        if (inRange) {
            return true;
        }

        double extraCost = 0;
        //build the recommendation
        for (String fundId : holdingsMap.keySet()) {
            Holding h = holdingsMap.get(fundId);
            Transaction transaction = new Transaction();
            transaction.setFundId(Integer.parseInt(fundId));

            //compute the transaction units
            double unitPrice = h.getBalance().getAmount() / h.getUnits();
            int units = (int) Math.round((totalBalance * (allocations.get(fundId) / 100.f) - h.getBalance().getAmount()) / unitPrice);

            if (units == 0)
                continue;
            else if (units > 0)
                transaction.setAction("buy");
            else
                transaction.setAction("sell");

            transaction.setUnits(abs(units));
            recommendation.addTransaction(transaction);
            extraCost += units * unitPrice;
        }

        recommendation.setExtraCost(Math.round(extraCost * 100.0) / 100.0); //round up to 2 decimal places
        return false;
    }

    /**
     * Executes the set of transactions associated to the given recommendation id.
     *
     * @param id                the portfolio to be rebalanced
     * @param recommendation_id the recommendation id of the transactions
     * @param headers           the headers of the request that contains a customer id
     * @return 200 if request succeed, otherwise 400 for bad request, 500 for internal server error
     */
    public static ResponseEntity<String> executeRecommendation(@PathVariable String id, @PathVariable String
            recommendation_id, @RequestHeader HttpHeaders headers) {
        Holding[] holdings;

        JSONObject responseErr = new JSONObject();
        // Check for valid input
        if (!HSBCProvidedUtil.isValidHeader(headers)) {
            responseErr.put("message", "Header doesn't contain customer id");
            return new ResponseEntity<>(responseErr.toString(),HttpStatus.BAD_REQUEST);
        }
        if(recommendation_id.isEmpty()){
            responseErr.put("message", "Recommendation id is missing");
            return new ResponseEntity<>(responseErr.toString(),HttpStatus.BAD_REQUEST);
        }
        if(id.isEmpty()){
            responseErr.put("message", "Portfolio id is missing");
            return new ResponseEntity<>(responseErr.toString(),HttpStatus.BAD_REQUEST);
        }

        try {
            // Start connection to database
            Connection conn = DatabaseConnection.setUpConnection();
            if (conn == null) {
                responseErr.put("message", "Failing to connect to database");
                return new ResponseEntity<>(responseErr.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // Get transactions for recommendation id
            List<Transaction> transactions = DatabaseConnection.getTransactions(recommendation_id);
            if (transactions == null || transactions.size() == 0) {
                responseErr.put("message", "No transactions found under this recommendation");
                return new ResponseEntity<>(responseErr.toString(), HttpStatus.NOT_FOUND);
            }

            // Execute transactions through HSBC /transaction endpoint
            Map<Integer, List<Transaction>> response = HSBCProvidedUtil.setTransactions(headers, id, transactions);
            if (response == null) {
                responseErr.put("message", "Failing to execute transactions"); //maybe internal server error since this results from calling mock system
                return new ResponseEntity<>(responseErr.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
            }

            List<Integer> recommendationIds = DatabaseConnection.getRecommendationIds(id);
            // Remove transactions associated with the executed recommendation

            for (Integer recommendationId : recommendationIds) {
                DatabaseConnection.deleteTransactions(Integer.toString(recommendationId));
                DatabaseConnection.deleteRecommendations(Integer.toString(recommendationId));
            }

            // Update customer holdings
            holdings = HSBCProvidedUtil.getHoldings(headers, id);

            // Close connection
            conn.close();
        } catch (HttpClientErrorException httpClientErrorException) {
            return new ResponseEntity<>(httpClientErrorException.getStatusCode());
        } catch (SQLException e) {
            System.err.println("Got an exception!");
            System.err.println(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            System.out.println("Got an exception!");
            System.err.println(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(holdings.toString(), HttpStatus.OK);
    }

    /**
     * Executes the set of transactions associated to the given recommendation id. Duepliacate function as executeRecommendation,
     * but returns a portfolio initial preference rather than list of holdings
     *
     * @param id                the portfolio to be rebalanced
     * @param recommendation_id the recommendation id of the transactions
     * @param headers           the headers of the request that contains a customer id
     * @return 200 if request succeed, otherwise 400 for bad request, 500 for internal server error
     */
    public static ResponseEntity<HSBCPortfolio[]> executeRecommendationForUI(@PathVariable String id, @PathVariable String
            recommendation_id, @RequestHeader HttpHeaders headers) {

        ResponseEntity<String> response = executeRecommendation(id, recommendation_id, headers);
        HSBCPortfolio[] portfolio = new HSBCPortfolio[1];
        HSBCPortfolio emptyPorfolio = new HSBCPortfolio();

        if(response.getStatusCode() == HttpStatus.OK) {
            String custId = headers.get(CUSTOMER_ID_KEY).get(0);
            try {
                HSBCPortfolio[] portfolios = HSBCProvidedUtil.getPortfolios(custId);
                for(int i=0; i<portfolios.length; i++){
                    if(portfolios[i].getPortfolioId().equals(id)){
                        portfolio[0] = portfolios[i];
                        return new ResponseEntity<>(portfolio, HttpStatus.OK);
                    }
                }
            }catch (Exception e){
                //
            }
        }
        portfolio[0] = emptyPorfolio;
        return new ResponseEntity<>(portfolio,response.getStatusCode());
    }

    /**
     * Updates the set of transactions associated to the given recommendation id.
     *
     * @param id                the portfolio to be rebalanced
     * @param recommendation_id the recommendation id of the transactions
     * @param headers           the headers of the request that contains a customer id
     * @param transactions      the list of transactions with updated values
     * @return 200 if request succeed, otherwise 400 for bad request, 500 for internal server error
     */
    public static ResponseEntity<Recommendation> updateRecommendation(@PathVariable String id, @PathVariable String recommendation_id, @RequestHeader HttpHeaders headers, List<Transaction> transactions) {
        Recommendation recommendation;

        // Check for valid input
        if (!HSBCProvidedUtil.isValidHeader(headers) || recommendation_id.isEmpty() || id.isEmpty() || transactions.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            for (Transaction transaction : transactions) {
                // Get current transaction from database
                Transaction currTransaction = DatabaseConnection.getTransaction(recommendation_id, transaction.getFundId());
                if (currTransaction == null) {
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }

                // Verify valid update inputs
                if (!transaction.getAction().equals(currTransaction.getAction()) || transaction.getUnits() + currTransaction.getUnits() < 0) {
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }

                // Recalculate transaction units
                transaction.setUnits(transaction.getUnits() + currTransaction.getUnits());
            }

            // Update transactions in database
            DatabaseConnection.updateTransactions(recommendation_id, transactions);
            ArrayList<Transaction> trans = new ArrayList<>(DatabaseConnection.getTransactions(recommendation_id));
            recommendation = new Recommendation(recommendation_id, trans);
        } catch (HttpClientErrorException httpClientErrorException) {
            return new ResponseEntity<>(httpClientErrorException.getStatusCode());
        } catch (SQLException e) {
            System.err.println("Got an exception!");
            System.err.println(e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            System.out.println("Got an exception!");
            System.err.println(e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(recommendation, HttpStatus.OK);
    }

}