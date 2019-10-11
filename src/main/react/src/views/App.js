import { FORM_ERROR } from "final-form";
import React, { Component } from 'react';
import { Redirect } from 'react-router-dom';

import { MainForm } from "../forms/MainForm";

import { HOST_IP, LOCAL_STORAGE_CUST_ID_KEY } from '../constants';

import '../scss/index.scss';

class App extends Component {
    constructor(props) {
        super(props);
        this.state = {
            isValidCustomer: false,
        };
    }

    onSubmit = async (values) => {
        const customerId = values.customerId;

        const res = await fetch(`http://${HOST_IP}:8080/portfolios?custId=${customerId}`);
        const jsonResult = await res.json();

        // Check if the API response is valid or not. A valid response consists of an array otherwise
        // return an error
        if (jsonResult instanceof Array) {
            // store the customer ID in local storage
            localStorage.setItem(LOCAL_STORAGE_CUST_ID_KEY, customerId);
            this.setState({ isValidCustomer: true });
        } else {
            return { [FORM_ERROR]: "Invalid Customer ID" };
        }
    };

    renderRedirect = () => {
        if (this.state.isValidCustomer) {
            return <Redirect to="/portfolios" />
        }
    };

    render() {
        return (
            <div>
                {this.renderRedirect()}
                <MainForm onSubmit={this.onSubmit} />
            </div>
        );
    }
}

export default App;
