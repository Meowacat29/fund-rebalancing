package modules;


public class Transaction {
    private String action;  // [buy | sell]
    private int fundId;
    private int units;

    public Transaction(){}

    public Transaction(String action, int fundId, int units) {
        this.action = action;
        this.fundId = fundId;
        this.units = units;
    }

    public void setFundId(int fundId){
        this.fundId = fundId;
    }

    public int getFundId(){ return this.fundId; }

    public void setAction (String action){
        this.action = action;
    }

    public String getAction (){
        return this.action;
    }

    public void setUnits (int units){
        this.units = units;
    }

    public int getUnits (){
        return this.units;
    }

    public boolean equals(Transaction trans) {
        return this.fundId == trans.getFundId() && this.units == trans.getUnits() && this.action.equals(trans.getAction());
    }
}
