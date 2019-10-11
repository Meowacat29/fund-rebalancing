package modules;

import java.util.List;

public class Instructions {
    public int portfolioId;
    public List<Transaction> instructions;

    public Instructions(int portfolioId, List<Transaction> instructions) {
        this.portfolioId = portfolioId;
        this.instructions = instructions;
    }

    public void setPortfolioId(int portfolioId) {
        this.portfolioId = portfolioId;
    }

    public void setInstructions(List<Transaction> instructions) {
        this.instructions = instructions;
    }

    public int getPortfolioId() {
        return this.portfolioId;
    }

    public List<Transaction> getInstructions() {
        return this.instructions;
    }
}