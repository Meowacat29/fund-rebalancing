import {FORM_ERROR} from "final-form";
import _ from 'lodash';
import React, {Component} from 'react';
import {Form} from "react-final-form";
import PieChart from 'react-minimal-pie-chart';
import {Redirect} from 'react-router-dom';

import PrefForm from '../forms/PrefForm';
import UpdateDeviationForm from '../forms/UpdateDeviationForm';
import {PhotoProfile} from "../shared/PhotoProfile";
import {BackButton} from '../shared/BackButton';

import {FUND_ID_NAME_MAP, HOST_IP, LOCAL_STORAGE_CUST_ID_KEY} from "../constants";

// different shades of blue color
const colours = ["#0570b0", "#74a9cf", "#bdc9e1","#f1eef6"];

// Color scheme for the pie charts
function generateRandomHexString() {
    const index = getRandomInt(4);
    return colours[index];
}

function getRandomInt(max) {
    return Math.floor(Math.random() * Math.floor(max));
}

let showRebalanceButton = false;

/**
 * Displays all the portfolio information
 */
class Portfolio extends Component {
    constructor(props) {
        super(props);

        this.state = {
            portfolioId : props.match.params.id,
            portfolio: this.props.location.state && this.props.location.state.portfolio
            ? this.props.location.state.portfolio
            : {holdings: []},
            hasPref: false,
            preferences: {},
            showRecommendationScreen: false,
            recommendations:[],
            deviation: 0,
            categoryHoldings: []
        };

        this.addExtraHoldingsData();
    }

    componentDidMount() {
        this.fetchPortfolio();
    }

    // grabs the portfolio data
    fetchPortfolio = () => {
        const url = `http://${HOST_IP}:8080/portfolio/${this.state.portfolioId}`;

        fetch(url, {
            method: "GET",
            headers: {
                "x-custid": localStorage.getItem(LOCAL_STORAGE_CUST_ID_KEY),
            },
        }).then(res => {
            return res.json();
        }).then(resp => {
            const isEmptyObj = Object.keys(resp).length === 0 && resp.constructor === Object;
            const hasNoPref = isEmptyObj || resp.deviation == null;

            this.setState({
                hasPref: !hasNoPref,
                preferences: resp,
                deviation: resp.deviation
            });

            if (this.state.preferences.type == "category") {
                this.getCategoryHoldings();
            }

            this.setShowRebalanceButton();
        }).catch(error => {
            console.log(error);
        });
    };

    // generates the table data for the holdings
    generateHoldings = () => {
        return this.state.portfolio.holdings.map((holding, index) => {
            const {fundId, units, allocPercentage, pieChartColor } = holding;
            const {amount, currency} = holding.balance;

            if (this.state.preferences.type == "fund") {
                return (
                    <tr key={fundId}>
                        <td style={{backgroundColor: pieChartColor}}></td>
                        <td>{fundId}</td>
                        <td>{units}</td>
                        <td>{amount}</td>
                        <td>{currency}</td>
                        <td>{allocPercentage + '%'}</td>
                    </tr>
                );
            } else {
                return (
                    <tr key={fundId}>
                        <td>{fundId}</td>
                        <td>{units}</td>
                        <td>{amount}</td>
                        <td>{currency}</td>
                        <td>{allocPercentage + '%'}</td>
                    </tr>
                );
            }

        });
    };

    // calculate the total amount of holdings balance in order to use for calculations for the pie chart
    // and allocation percentage
    calcTotalHoldingsAmt = () => {
        return this.state.portfolio.holdings.reduce((acc, holding) => {
            const {amount} = holding.balance;
            return acc + (amount);
        }, 0);
    };

    getCategoryHoldings = () => {
        fetch("http://" + HOST_IP + ":8080/portfolio/" + this.state.portfolioId + "/category_holdings",{
            method: "GET",
            headers: {
                "x-custid": localStorage.getItem(LOCAL_STORAGE_CUST_ID_KEY),
            },
        }).then(res => {
                return res.json();
            }).then(resp => {
            console.log(resp);
            console.log(this.state.portfolio.holdings);

            this.setState({
                categoryHoldings: resp
            });
        }).catch(error => {
            console.log(error);
        });
    };

    addExtraHoldingsData = () => {
        const portfolioTotalHoldingsAmt = this.calcTotalHoldingsAmt();

        this.state.portfolio.holdings.forEach((holding, index) => {
            const {amount} = holding.balance;

            const allocPercentage = amount / portfolioTotalHoldingsAmt;

            holding.allocPercentage = +(allocPercentage * 100).toFixed(2);
            holding.pieChartColor = colours[index];
        });
    };

    // returns an array of formatted pie chart data for current allocations
    formatCurrAllocPieChartData = () => {
        const formattedPieChartData = [];

        this.state.portfolio.holdings.forEach(holding => {
            const dataObj = {};
            dataObj.title = holding.fundId;
            dataObj.value = holding.allocPercentage;
            dataObj.color = holding.pieChartColor;
            formattedPieChartData.push(dataObj);
        });

        return formattedPieChartData;
    };

    // formats the pie chart data for the initial portfolio allocation preference
    formatCurrAllocCategoryPieChartData = () => {
        const formattedPieChartData = [];
        this.state.categoryHoldings.forEach((holding, index) => {
            const dataObj = {};
            dataObj.title = holding.category;
            dataObj.value = holding.percentage;
            holding.pieChartColor = colours[index];
            dataObj.color = holding.pieChartColor;
            formattedPieChartData.push(dataObj);
        });

        console.log("currallocationcategorypiechartdata: " + formattedPieChartData);
        return formattedPieChartData;
    };

    formatInitialPrefPieChartData = () => {
        const formattedPieChartData = [];

        this.state.preferences.allocations.forEach(allocation =>{
            const dataObj = {};
            dataObj.title = allocation.fundId;
            dataObj.value = allocation.percentage;

            const matchingHolding = _.find(
                this.state.portfolio.holdings,
                holding => holding.fundId === allocation.fundId
            );

            dataObj.color = matchingHolding ? matchingHolding.pieChartColor : undefined;
            formattedPieChartData.push(dataObj);
        });

        this.setShowRebalanceButton();

        return formattedPieChartData;
    };

    setShowRebalanceButton = () => {
        if (this.state.hasPref) {
            let isOutofBalance = false;

            if (this.state.preferences.type == "category") {
                this.state.preferences.allocations.forEach(allocation => {
                    const matchingHolding = _.find(
                        this.state.categoryHoldings,
                        holding => holding.category == allocation.category
                    );
                    if (matchingHolding && Math.abs(matchingHolding.percentage - allocation.percentage) > this.state.deviation) {
                        isOutofBalance = true;
                    }
                });
            } else {
                this.state.preferences.allocations.forEach(allocation => {
                    const matchingHolding = _.find(
                        this.state.portfolio.holdings,
                        holding => holding.fundId == allocation.fundId
                    );
                    if (matchingHolding && Math.abs(matchingHolding.allocPercentage - allocation.percentage) > this.state.deviation) {
                        isOutofBalance = true;
                    }
                });
            }
            showRebalanceButton = isOutofBalance;
        }
    };

    formatInitialPrefPieChartCategoryData = () => {
        const formattedPieChartData = [];
        this.state.preferences.allocations.forEach(allocation =>{
            const dataObj = {};
            dataObj.title = allocation.category;
            dataObj.value = allocation.percentage;

            const matchingHolding = _.find(this.state.categoryHoldings, holding => holding.category == allocation.category);
            console.log(matchingHolding);
            this.setShowRebalanceButton(matchingHolding, allocation);
            dataObj.color = matchingHolding ? matchingHolding.pieChartColor : undefined;
            formattedPieChartData.push(dataObj);
        });

        console.log("prefallocationpiechartdata: " + formattedPieChartData);
        return formattedPieChartData;
    };

    onRebalanceButtonClick = async () => {
        const portfolioId = this.state.portfolio.portfolioId;

        let url = "http://" + HOST_IP + ":8080/portfolio/" + portfolioId + "/rebalance";

        if (this.state.preferences.type == "category") {
            url = "http://" + HOST_IP + ":8080/portfolio/" + portfolioId + "/category_rebalance";
        }

        // Sends a POST request to the rebalance endpoint
        try {
            const response = await fetch(url,{
                method: "POST",
                headers: {
                    "x-custid": localStorage.getItem(LOCAL_STORAGE_CUST_ID_KEY),
                },
            });

            const data = await response.text();

            const jsonResult = JSON.parse(data);

            if (this.state.preferences.type == "category") {
                if (jsonResult.length <= 0) {
                    return { [FORM_ERROR]: "No recommendations available." };
                } else {
                    this.setState({showRecommendationScreen: true, recommendations: jsonResult});
                }
            } else {
                if (jsonResult.transactions === undefined || jsonResult.transactions.length <= 0) {
                    return { [FORM_ERROR]: "No recommendations available." };
                } else {
                    this.setState({showRecommendationScreen: true, recommendations: [...this.state.recommendations,jsonResult]});
                }
            }
            console.log(jsonResult);

        } catch (err) {
            return { [FORM_ERROR]: err.toString() };
        }
    };

    updateDeviation = (deviation) => {
        this.setShowRebalanceButton();
        this.setState({ deviation: deviation });
    };

    render() {
        let content, currAllocPieChart, initialPrefPieChart, footerButtons;

        if (this.state.showRecommendationScreen) {
            content = <Redirect
                to={{
                    pathname: `/portfolios/${this.state.portfolioId}/rebalance`,
                    state: {
                        portfolioId: this.state.portfolioId,
                        portfolio: this.state.portfolio,
                        recommendations: this.state.recommendations,
                    }
                }}
            />;
        } else {
            // Render current allocation pie chart even if there is no preference
            let currAllocPieChartData = this.formatCurrAllocPieChartData();
            if (this.state.hasPref && this.state.preferences.type == "category") {
                currAllocPieChartData = this.formatCurrAllocCategoryPieChartData();
            }
            currAllocPieChart = (
                <div>
                    <label className="pie-chart-label">Current Allocation</label>
                    <PieChart
                        data={currAllocPieChartData}
                        radius={40}
                        lineWidth={45}
                        label={({ data, dataIndex }) => data[dataIndex].percentage.toFixed(2) + '%'}
                        labelStyle={{
                            fontSize: '4px',
                            fontFamily: 'sans-serif',
                            fill: '#121212'
                        }}
                        labelPosition={110}
                        style={{height: '280px', width: '360px'}}
                        animate
                    />
                </div>
            );

            if (this.state.hasPref) {
                // Initial pref pie chart
                let initialAllocPieChartdata;

                if (this.state.preferences.type == "category") {
                    initialAllocPieChartdata = this.formatInitialPrefPieChartCategoryData();
                } else {
                    initialAllocPieChartdata = this.formatInitialPrefPieChartData();
                }

                initialPrefPieChart = (
                    <div>
                        <label className="pie-chart-label">Target Allocation (deviation: {this.state.deviation}%)</label>
                        <PieChart
                            data={initialAllocPieChartdata}
                            radius={40}
                            lineWidth={45}
                            label={({ data, dataIndex }) => data[dataIndex].percentage.toFixed(2) + '%'}
                            labelStyle={{
                                fontSize: '4px',
                                fontFamily: 'sans-serif',
                                fill: '#121212'
                            }}
                            labelPosition={110}
                            style={{height: '280px', width: '360px'}}
                            animate
                        />
                    </div>
                );

                footerButtons = (
                    <div>
                        <div className="flex-same-row inline-buttons-wrapper">
                            <BackButton to={'/portfolios'} />
                            <UpdateDeviationForm
                                portfolio={this.state.portfolio}
                                preferences={this.state.preferences}
                                deviation={this.state.deviation}
                                updateDeviationCallBack={this.updateDeviation}
                            />
                            <PrefForm
                                isUpdateMode={true}
                                portfolio={this.state.portfolio}
                                preferences={this.state.preferences}
                                categoryHoldings={this.state.categoryHoldings}
                                refreshPortfolioCallback={this.fetchPortfolio}
                            />
                            {
                                showRebalanceButton &&
                                <Form
                                    onSubmit={this.onRebalanceButtonClick}
                                    render={({ submitError, handleSubmit, submitting }) => (
                                        <form onSubmit={handleSubmit}>
                                            <button type="submit" disabled={submitting} className="btn red-btn">
                                                Rebalance
                                            </button>
                                            <div className="error">{submitError}</div>
                                        </form>
                                    )}
                                />
                            }
                        </div>
                    </div>
                );

            }

            const customerId = localStorage.getItem(LOCAL_STORAGE_CUST_ID_KEY);

            content =  (
                <div className="container">
                    <PhotoProfile customerId={customerId}/>
                    <div className="row">
                        <h2>Portfolio #{this.state.portfolio.portfolioId}</h2>
                    </div>

                    <div className="row">
                        <table className="table" id="table-sort">
                            <thead>
                            <tr>
                                {
                                    (this.state.preferences.type == "fund" || (this.state.preferences.type == "category" && !this.state.hasPref)) &&
                                    <th scope="col">Colour</th>
                                }
                                <th scope="col">Fund Id</th>
                                <th scope="col"># Units</th>
                                <th scope="col">Balance</th>
                                <th scope="col">Currency</th>
                                <th scope="col">Allocation Percentage</th>
                            </tr>
                            {this.generateHoldings()}
                            </thead>
                        </table>
                    </div>

                    <div className="flex-same-row pie-chart-wrapper">
                        {currAllocPieChart}
                        {initialPrefPieChart}
                    </div>

                    {!this.state.hasPref &&  <div className="flex-same-row inline-buttons-wrapper">
                        <BackButton to={'/portfolios'} /><PrefForm portfolio={this.state.portfolio} refreshPortfolioCallback={this.fetchPortfolio}/></div>}

                    {footerButtons}
                </div>
            );
        }
        return content;
    }
}

export default Portfolio;
