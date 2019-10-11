import { FORM_ERROR } from "final-form";
import _ from 'lodash';
import React, { Component } from 'react';
import Popup from 'reactjs-popup'
import { Form, Field } from 'react-final-form';

import { FUND_ID_NAME_MAP, HOST_IP, LOCAL_STORAGE_CUST_ID_KEY } from "../constants";

/**
 * The preference form. It handles both create initial portfolio allocation preference and
 * modify portfolio allocation preference.
 */
class PrefForm extends Component {
    constructor(props) {
        super(props);

        let prefType;
        if (props.preferences == undefined) {
            prefType = "fund"
        } else {
            prefType = props.preferences.type;
        }

        this.state = {
            isUpdateMode: !!props.isUpdateMode,
            preferences: props.preferences,
            portfolio: props.portfolio,
            hasSaveSucceeded: false,
            showError: false,
            type: prefType,
            categoryHoldings: []
        };

        this.getCategoryHoldings();
    }

    getCategoryHoldings = () => {
        fetch("http://" + HOST_IP + ":8080/portfolio/" + this.props.portfolio.portfolioId + "/category_holdings",{
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

    onSubmit = async (values) => {
        const portfolioId = this.state.portfolio.portfolioId;
        const postUrl = `http://${HOST_IP}:8080/portfolio/${portfolioId}`;
        const putUrl = `http://${HOST_IP}:8080/portfolio/${portfolioId}/allocations`;

        const { deviation, type } = values;

        // Delete the values so we can iterate over the fund values instead
        delete values.deviation;
        delete values.type;

        const formattedAllocations = [];


        if (this.state.type == "fund") {
            Object.keys(values).forEach(fundKey => {
                const key = fundKey.split("-")[1];
                formattedAllocations.push({
                    fundId: parseInt(key),
                    percentage: parseInt(values[fundKey]),
                });
            });
        } else {
            Object.keys(values).forEach(categoryKey => {
                const key = categoryKey.split("-")[1];
                formattedAllocations.push({
                    category: parseInt(key),
                    percentage: parseInt(values[categoryKey]),
                });
            });
        }

        let requestBody = {};

        // Different request bodies for update preference and create preference
        if (this.state.isUpdateMode) {
            requestBody = formattedAllocations;
        } else {
            requestBody = {
                id: portfolioId,
                deviation: deviation,
                type: this.state.type,
                allocations: formattedAllocations,
            };
        }

        try {
            const response = await fetch(
                this.state.isUpdateMode ? putUrl : postUrl,
                {
                    method: this.state.isUpdateMode ? "PUT" : "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "x-custid": localStorage.getItem(LOCAL_STORAGE_CUST_ID_KEY),
                    },
                    body: JSON.stringify(requestBody),
                }
            );

            if (response.status === 200) {
                console.log("has succeeded");
                this.setState({hasSaveSucceeded: true, showError: false});
                return {};
            } else {
                const jsonObj = await response.json();
                console.log(jsonObj.message);
                this.setState({showError: true});
                return { [FORM_ERROR]: jsonObj.message };
            }
        } catch (err) {
            console.log(err);
            return { [FORM_ERROR]: err.toString() };
        }
    };

    // Verify the form fields
    validateFormFields = (values) => {
        const errors = {};

        if (this.state.type == "fund") {
            this.state.portfolio.holdings.forEach(allocation => {
                const key = `fund-${allocation.fundId}`;
                const value = values[key];

                // If the key is not in the form values object then that means no value has been entered
                if (!(key in values)) {
                    errors[key] = "Required";
                } else if (value < 0.00 || value > 100.00) {
                    // now that the key is in the values obj, check if it is within 0 to 100
                    errors[key] = "Value must be between 0.00 and 100.00";
                }
            });
        }

        return errors;
    };

    changeType = (e) => {
        this.setState({type: e.target.value});
    };

    render() {
        const allocations = this.state.isUpdateMode? this.props.preferences.allocations: this.props.portfolio.holdings;
        const categoryAllocations = this.state.isUpdateMode? this.props.preferences.allocations: this.state.categoryHoldings;
        return (
            <Popup
                trigger={
                    <button className="btn red-btn">
                        {this.state.isUpdateMode? 'Update Preference' : 'Create Initial Preference'}
                    </button>
                }
                modal
                closeOnDocumentClick
            >
                {close => (
                    <Form
                        onSubmit={this.onSubmit}
                        validate={this.validateFormFields}
                        render={({ pristine, submitError, handleSubmit, submitting}) => (
                            <form onSubmit={(event) => {
                                const promise = handleSubmit(event);
                                promise && promise.then((formObject) => {
                                    if (_.isEmpty(formObject) || !([FORM_ERROR] in formObject)) {
                                        // if there is no error, then close the modal
                                        close();
                                        this.props.refreshPortfolioCallback();
                                    }
                                });
                                return promise;
                            }}>

                                <div className="modal-header">
                                    <h4>
                                        {
                                            this.state.isUpdateMode
                                                ? 'Update Preference'
                                                : 'Create Preference'
                                        }
                                    </h4>
                                </div>

                                <div className="modal-body">
                                    <Field name="portfolioId">
                                        {({ input }) => (
                                            <div className="form-group">
                                                <label className="col-form-label">Portfolio ID</label>
                                                <input
                                                    {...input}
                                                    className="form-control"
                                                    value={this.state.portfolio.portfolioId}
                                                    type="text"
                                                    placeholder="Portfolio ID"
                                                    readOnly
                                                />
                                            </div>
                                        )}
                                    </Field>

                                    {
                                        !this.state.isUpdateMode &&
                                            <Field name="deviation">
                                                {({input, meta}) => (
                                                    <div className="form-group">
                                                        <label className="col-form-label">Deviation</label>
                                                        <input
                                                            {...input}
                                                            className="form-control"
                                                            type="number"
                                                            placeholder="[0,5]"
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
                                    }

                                    {
                                        !this.state.isUpdateMode &&
                                            <div className="form-group">
                                                <label className="col-form-label">Type</label>
                                                <Field className="form-control" name="type" component="select" onChange={this.changeType}>
                                                    <option />
                                                    <option value="fund">Fund</option>
                                                    <option value="category">Category</option>
                                                </Field>
                                            </div>
                                    }

                                    {
                                        this.state.type == "fund" &&
                                        <div>
                                            <label className="col-form-label">Allocations</label>
                                            {
                                                allocations.map(allocation => (
                                                    <Field key={allocation.fundId}
                                                           name={`fund-${allocation.fundId}`}
                                                    >
                                                        {({ input, meta }) => (
                                                            <div className="form-group">
                                                                <label className="col-form-label">
                                                                    Fund #{allocation.fundId}
                                                                </label>
                                                                <input
                                                                    {...input}
                                                                    className="form-control"
                                                                    type="number"
                                                                    placeholder = {this.state.isUpdateMode? allocation.percentage : "0-100"}
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
                                                ))
                                            }
                                        </div>
                                    }

                                    {
                                        this.state.type == "category" &&
                                        <div>
                                            <label className="col-form-label">Allocations</label>
                                            {
                                                categoryAllocations.map(allocation => (
                                                    <Field key={allocation.category}
                                                           name={`fund-${allocation.category}`}
                                                    >
                                                        {({input, meta}) => (
                                                            <div className="form-group">
                                                                <label className="col-form-label">
                                                                    Category #{allocation.category}
                                                                </label>
                                                                <input
                                                                    {...input}
                                                                    className="form-control"
                                                                    type="number"
                                                                    placeholder={this.state.isUpdateMode ? allocation.percentage : "0-100"}
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
                                                ))
                                            }
                                        </div>
                                    }

                                </div>
                                
                                <div className="modal-footer">
                                    <button
                                        type="submit"
                                        disabled={pristine || submitting}
                                        className="btn red-btn"
                                    >
                                            Save
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
}

export default PrefForm;