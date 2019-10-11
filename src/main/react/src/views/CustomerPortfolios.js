import React, { Component } from 'react';
import { Link } from 'react-router-dom';

import { BackButton } from "../shared/BackButton";
import { PhotoProfile } from "../shared/PhotoProfile";

import { HOST_IP } from "../constants";
import { LOCAL_STORAGE_CUST_ID_KEY } from '../constants';

/**
 * A view that lists out all the portfolios of a specific customer
 */
class CustomerPortfolios extends Component {
    constructor(props) {
        super(props);
        this.state = {
            portfolios: props.portfolios ? props.portfolios : [],
        };
    }

    componentDidMount() {
        const customerId = localStorage.getItem(LOCAL_STORAGE_CUST_ID_KEY);

        fetch("http://" + HOST_IP + ":8080/portfolios?custId=" + customerId)
            .then(res => res.json())
            .then(data => {
                if (data instanceof Array) {
                    this.setState({ portfolios: data });
                } else {
                    this.setState({ errMsg: "Invalid Customer ID" });
                }
            });
    }

    generatePortfolioItems = () => {
        return this.state.portfolios.map((portfolio) => {
            const pid = portfolio.portfolioId;

            return (
                <div className="row" key={pid}>
                    <button
                        type="button"
                        className="list-group-item list-group-item-action"
                    >
                        <Link
                            to={{
                                pathname: "/portfolios/" + pid,
                                state: {
                                    portfolio: portfolio,
                                }
                            }}>{pid}</Link>
                    </button>
                </div>
             );
        });
    };

    render() {
        let header = "Your Portfolios";
        const customerId = localStorage.getItem(LOCAL_STORAGE_CUST_ID_KEY);

        return (
            <div className="container">
                <PhotoProfile customerId={customerId} />

                <div className="row">
                    <h1>{header}</h1>
                </div>

                <div className="table">{this.state.portfolios && this.generatePortfolioItems()}</div>

                <BackButton to={'/'} />

                {this.state.errMsg}
            </div>
        );
    }
}

export default CustomerPortfolios;