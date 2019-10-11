import { FORM_ERROR } from "final-form";
import _ from "lodash";
import React, { Component } from 'react';
import Popup from 'reactjs-popup'
import { Form, Field } from 'react-final-form';

import { FUND_ID_NAME_MAP, HOST_IP, LOCAL_STORAGE_CUST_ID_KEY } from "../constants";

/**
 * The stretch-goal modify recommendation form
 */
class ModifyRecommendationForm extends Component {
    constructor(props) {
        super(props);

        this.state = {
            transactions: this.props.transactions,
            portfolioId: this.props.portfolioId,
            recommendationId: this.props.recommendationId,
            newRecommendations: [],
            showError: false,
        };
    }

    // triggered function when the submit button is clicked
    onSubmit = async (values) => {
        const portfolioId = this.state.portfolioId;
        const recommendationId = this.state.recommendationId;
        const transactions = this.state.transactions;

        let requestBody = [];
        let modified = false;

        // Fill in the appropriate values in the request body
        for (let i = 0; i < transactions.length; i++) {
            const unitFieldKey = recommendationId + "t" + i.toString() + "units";
            const actionFieldKey = recommendationId + "t" + i.toString() + "action";
            const fundIdIndex = recommendationId + "t" + i.toString() + "fundId";

            let unitValue = 0;
            let fundIdValue = transactions[i].fundId;
            let actionValue = transactions[i].action;

            if (values[unitFieldKey] !== undefined){
                modified = true;
                unitValue = values[unitFieldKey] - this.state.transactions[i].units;
            }

            if (values[actionFieldKey] !== undefined && values[actionFieldKey] !== actionValue) {
                modified = true;
                actionValue = values[actionFieldKey];
            }

            if (values[fundIdIndex] !== undefined && values[fundIdIndex] !== fundIdValue) {
                modified = true;
                fundIdValue = values[fundIdIndex];
            }

            if (modified) {
                const transaction = {"action": actionValue, "fundId": fundIdValue, "units": parseInt(unitValue)}
                requestBody.push(transaction);
            }
        }
        
        const url = "http://" + HOST_IP + ":8080/portfolio/" + portfolioId + "/recommendation/" + recommendationId + "/modify";

        // PUT API request to modify the recommendation endpoint
        fetch(url,{
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                "x-custid": localStorage.getItem(LOCAL_STORAGE_CUST_ID_KEY),
            },
            body: JSON.stringify(requestBody),
        }).then((response) => {
            if (response.status === 200){
                response.text().then( (responseBody)=> {
                    this.setState({ showError: false });
                    this.setState({
                        newRecommendations:[JSON.parse(responseBody)],
                        transactions: JSON.parse(responseBody).transactions
                    });
                    this.props.onUpdateRecommendations(this.state.newRecommendations);
                });
            }else{
                this.setState({showError: true});
                return { [FORM_ERROR]: "Invalid amount of units" };
            }
        }).catch(error => {
            console.log(error);
        });
    };

    render() {
        // Transaction form inputs
        let transactionInputs = this.state.transactions.map((transaction, i) => {
            let actionIndex = this.state.recommendationId + "t" + i.toString() + "action";
            let fundIdIndex = this.state.recommendationId + "t" + i.toString()+ "fundId";
            let unitsIndex = this.state.recommendationId + "t" + i.toString()+ "units";

            return (
                <div>
                    <h5>Transaction {i + 1}</h5>
                    <Field name={fundIdIndex}>
                        {({ input }) => (
                            <div className="form-group">
                                <label className="col-form-label">{(FUND_ID_NAME_MAP.get(transaction.fundId))?FUND_ID_NAME_MAP.get(transaction.fundId)+" ID":transaction.fundId}</label>
                                <input
                                    {...input}
                                    className="form-control"
                                    type="text"
                                    value={transaction.fundId}
                                    readOnly
                                />
                            </div>
                        )}
                    </Field>
                    <Field name= {actionIndex}>
                        {({ input }) => (
                            <div className="form-group">
                                <label className="col-form-label">Action</label>
                                <input
                                    {...input}
                                    className="form-control"
                                    type="text"
                                    value={transaction.action}
                                    readOnly
                                />
                            </div>
                        )}
                    </Field>

                    <Field name={unitsIndex}>
                        {({ input, meta }) => (
                            <div className="form-group">
                                <label className="col-form-label">Units: {transaction.units}</label>
                                <input
                                    {...input}
                                    className="form-control"
                                    type="number"
                                />
                                {
                                    meta.error &&
                                    meta.touched &&
                                    <div className="text-left form-input-error">
                                        {meta.error}
                                    </div>
                                }
                            </div>
                        )}
                    </Field>
                </div>
            )
        });

        // Render the modal form
        return (
            <Popup
                trigger={<button className="btn red-btn">Modify</button>}
                modal
                closeOnDocumentClick
            >
                {close => (
                    <Form
                        onSubmit={this.onSubmit}
                        render={({ pristine, submitError, handleSubmit, submitting}) => (
                            <form onSubmit={(event) => {
                                const promise = handleSubmit(event);
                                promise && promise.then((formObject) => {
                                    if (_.isEmpty(formObject) || !([FORM_ERROR] in formObject)) {
                                        // if there is no error, then close the modal
                                        close();
                                    }
                                });
                                return promise;
                            }}>
                                <div className="modal-header">
                                    <h4>Modify Recommendation</h4>
                                </div>

                                <div className="modal-body">
                                    {transactionInputs}
                                </div>

                                <div className="modal-footer">
                                    <button className="btn red-btn" type="submit" disabled={pristine || submitting}>
                                        Confirm
                                    </button>

                                    <button
                                        className="btn back-cancel-btn"
                                        onClick={close}
                                    >
                                        Cancel
                                    </button>
                                </div>
                                {this.state.showError && <div className="error form-submission-error error-message">{submitError}</div>}
                            </form>
                        )}
                    />
                )}
            </Popup>
        )
    }
};

export default ModifyRecommendationForm;