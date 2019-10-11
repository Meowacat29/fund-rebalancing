import React, { Component } from 'react';
import { Redirect } from 'react-router-dom'

import ModifyRecommendationForm from '../forms/ModifyRecommendationForm';
import { BackButton } from "../shared/BackButton";
import { PhotoProfile } from "../shared/PhotoProfile";

import { FUND_ID_NAME_MAP, HOST_IP, LOCAL_STORAGE_CUST_ID_KEY } from "../constants";


const recommendationTitle = ["Highest Scored ", "Currently Owned ", "Lowest Unit Price "];

class Recommendation extends Component {
    constructor(props) {
        super(props);

        this.state = {
            recommendations:
                props.location && props.location.state && props.location.state.recommendations
                    ? props.location.state.recommendations
                    : [],
            portfolio:
               props.location && props.location.state && props.location.state.portfolio
                ? props.location.state.portfolio
                : {},
            portfolioId:
                props.location && props.location.state && props.location.state.portfolioId
                ? props.location.state.portfolioId
                : -1,
            message: "",
        };
    }

    generateTransactionTableData = () => {
      return this.state.recommendations.map(recommendation => (
          recommendation.transactions.map((t, index) => {
              return (

                 <tr key={index}>
                     <td>{(FUND_ID_NAME_MAP.get(t.fundId))?FUND_ID_NAME_MAP.get(t.fundId):t.fundId}</td>
                     <td>{t.units}</td>
                     <td>{t.action}</td>
                 </tr>
              );
          })
      ))
    };

    onUpdateRecommendations = (newrecommendations) => {
        // callback from ModifyRecommendationForm to update parent component Recommendation Component

        this.state.recommendations.map ((recommendation, index) => {
            if (recommendation.recommendationId == newrecommendations[0].recommendationId) {
                this.state.recommendations[index] = newrecommendations[0];
            }
        });

        this.setState({recommendations: this.state.recommendations});
    };

    onExecuteRecommendation = (selectedReommendationId) => {
        const url = "http://" + HOST_IP + ":8080/portfolio/" + this.state.portfolioId + "/recommendation/"+ selectedReommendationId + "/executeUI";
        let hasSucceeded = false;

        fetch(url,{
            method: "POST",
            headers: {
                "x-custid": localStorage.getItem(LOCAL_STORAGE_CUST_ID_KEY),
            },
        }).then(response => {
            // if succeeded, redirect to portfolio page with newest holding to display
            if (response.status === 200) {
                hasSucceeded = true;
            }
            return response.json()
        }).then(responseBody => {
            if (hasSucceeded) {
                if (responseBody instanceof Array){
                    responseBody.forEach((portfolio) => {
                        this.setState({ portfolio: portfolio });
                    });
                }
                this.setState({ oneExecutionSucceed: true, message:"Success!" });
            } else {
                this.setState({ message:responseBody });
            }
        }).catch(error => {
                console.log(error);
            });
    };

    render() {
        if (this.state.oneExecutionSucceed){
            return <Redirect to={{
                pathname: `/portfolios/${this.state.portfolioId}`,
                state: {
                    portfolio: this.state.portfolio,
                }
            }} />;
        }

        const customerId = localStorage.getItem(LOCAL_STORAGE_CUST_ID_KEY);

        return (
            <div>
                <PhotoProfile customerId={customerId}/>

                <h3>REBALANCE RECOMMENDATION</h3>
                {this.state.recommendations.map((recommendation, index) => (
                    <div key={index} className="container">

                        <div className="row">
                            {
                                this.state.recommendations.length > 1 &&
                                <h5>{recommendationTitle[index]} Recommendation ID: {recommendation.recommendationId}</h5>
                            }
                            {
                                this.state.recommendations.length == 1 &&
                                <h5>Recommendation ID: {recommendation.recommendationId}</h5>
                            }
                        </div>

                        <div className="row">
                            <table className="table">
                                <thead>
                                <tr>
                                    <th scope="col">Fund Id</th>
                                    <th scope="col">Units</th>
                                    <th scope="col">Action</th>
                                </tr>
                                {
                                    recommendation.transactions.map((t, index) => {
                                        return (
                                            <tr key={index}>
                                                <td>{t.fundId}</td>
                                                <td>{t.units}</td>
                                                <td>{t.action}</td>
                                            </tr>
                                        );
                                    })
                                }
                                </thead>
                            </table>
                            <div>{"Extra Cost: $" + recommendation.extraCost.toFixed(2) + " CAD"}</div>
                        </div>

                        <div className="inline-buttons-wrapper">
                            <BackButton
                                to={{
                                    pathname: `/portfolios/${this.state.portfolioId}`,
                                    state: {
                                        portfolioId: this.state.portfolioId,
                                        portfolio: this.state.portfolio,
                                    }
                                }}
                            />

                            <button
                                className="btn red-btn"
                                onClick={
                                    () => this.onExecuteRecommendation(recommendation.recommendationId)
                                }
                            >
                                Execute
                            </button>

                            <ModifyRecommendationForm
                                onUpdateRecommendations= {this.onUpdateRecommendations}
                                portfolioId={this.state.portfolioId}
                                recommendationId={recommendation.recommendationId}
                                transactions={recommendation.transactions}
                            />
                        </div>

                        <div>{this.state.message}</div>
                    </div>
                ))}
            </div>
        );
    }
}

export default Recommendation;