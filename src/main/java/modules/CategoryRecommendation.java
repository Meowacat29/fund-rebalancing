package modules;

import java.util.List;

public class CategoryRecommendation {
    private int category;
    private boolean isSell;
    private List<Double> extraCosts;
    private List<Transaction> transactions;

    public CategoryRecommendation(int category, List<Transaction> transactions, boolean isSell, List<Double> extraCosts) {
        this.category = category;
        this.transactions = transactions;
        this.isSell = isSell;
        this.extraCosts = extraCosts;
    }

    public boolean isSell() {
        return this.isSell;
    }

    public List<Transaction> getTransactions() {
        return this.transactions;
    }

    public int getCategory() {
        return this.category;
    }

    public List<Double> getExtraCosts() {
        return this.extraCosts;
    }

}
