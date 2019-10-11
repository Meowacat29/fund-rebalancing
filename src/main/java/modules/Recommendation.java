package modules;

import java.util.ArrayList;
import java.util.List;

public class Recommendation {
    private String recommendationId;
    private ArrayList<Transaction> transactions = new ArrayList<Transaction>();
    private double extraCost;

    public Recommendation(){}

    public Recommendation(String recommendationId, ArrayList<Transaction> transactions) {
        this.recommendationId = recommendationId;
        this.transactions = transactions;
    }

    public void setRecommendationId(String recommendationId){
        this.recommendationId = recommendationId;
    }

    public String getRecommendationId(){
        return this.recommendationId;
    }

    public void addTransaction(Transaction transaction){
        this.transactions.add(transaction);
    }

    public ArrayList<Transaction> getTransactions(){
        return this.transactions;
    }

    public void addAllTransactions(List<Transaction> transactions) {
        this.transactions.addAll(transactions);
    }

    public void setExtraCost(double extra){ this.extraCost = extra;}

    public void addExtraCost(double extra) {
        this.extraCost += extra;
    }

    public double getExtraCost(){ return this.extraCost;}
}
