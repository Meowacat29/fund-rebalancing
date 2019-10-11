package modules;

public class Holding {
    private int fundId;
    private int units;
    private Balance balance;

    public Holding(){}

    public void setFundId(int fundId){this.fundId = fundId;}

    public int getFundId(){
        return this.fundId;
    }

    public void setUnits(int units){this.units = units;}

    public int getUnits(){
        return this.units;
    }

    public void setBalance(Balance balance){this.balance = balance;}

    public void setBalance(double balance){this.balance = new Balance(balance, "CAD");}

    public Balance getBalance(){
        return this.balance;
    }

}
