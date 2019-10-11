import { FORM_ERROR } from "final-form";
import _ from "lodash";
import React, { Component } from 'react';
import Popup from 'reactjs-popup'
import { Form, Field } from 'react-final-form';

import {HOST_IP, LOCAL_STORAGE_CUST_ID_KEY} from "../constants";

/**
 * A form to update the deviation
 */
class UpdateDeviationForm extends Component {
    constructor(props) {
        super(props);

        this.state = {
            preferences: props.preferences,
            portfolio: props.portfolio,
            deviation: props.preferences.deviation,
        };
    }

    // Sends a PUT request to modify the deviation
    onSubmit = async (values) => {
        const portfolioId = this.state.portfolio.portfolioId;
        const url = `http://${HOST_IP}:8080/portfolio/${portfolioId}/deviation`;

        let { deviation } = values;

        const requestBody = JSON.stringify({
            deviation: deviation,
        });

        try {
            const response = await fetch(url,{
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    "x-custid": localStorage.getItem(LOCAL_STORAGE_CUST_ID_KEY),
                },
                body: requestBody,
            });

            if (response.status === 200) {
                response.text().then((text)=> {
                    deviation = JSON.parse(text).deviation;
                    const deviationNoDecimal = deviation;
                    deviation = (deviation == deviationNoDecimal.toFixed(0))? deviation.toFixed(1): deviation;
                    this.props.updateDeviationCallBack(deviation);
                    this.setState({ deviation });
                })
            } else {
                const jsonObj = await response.json();
                return { [FORM_ERROR]: jsonObj.message };
            }
        } catch (err) {
            return { [FORM_ERROR]: err.toString() };
        }
    };

    validateFormFields = (values) => {
        const errors = {};
        const key = 'deviation';
        const value = values[key];

        // If the key is not in the form values object then that means no value has been entered
        if (!(key in values)) {
            errors[key] = "Required";
        } else if (value < 0.00 || value > 5.00) {
            errors[key] = "Value must be between 0.00 and 5.00";
        }
        return errors;
    };

    render() {
        return (
            <Popup
                trigger={
                    <button className="btn red-btn">
                        Update Deviation
                    </button>
                }
                modal
                closeOnDocumentClick
            >
                {close => (
                    <Form
                        onSubmit={this.onSubmit}
                        validate={this.validateFormFields}
                        render={({ pristine, submitError, handleSubmit, submitting }) => (
                            <form onSubmit={(event) => {
                                const promise = handleSubmit(event);
                                promise && promise.then((formObject) => {
                                    console.log(formObject);
                                    if (_.isEmpty(formObject) || !([FORM_ERROR] in formObject)) {
                                        // if there is no error, then close the modal
                                        close();
                                    }

                                });
                                return promise;
                            }}>

                                <div className="modal-header">
                                    <h4>Update Deviation</h4>
                                </div>

                                <div className="modal-body">
                                    <Field name="deviation">
                                        {({ input, meta }) => (
                                            <div className="form-group">
                                                <label className="col-form-label">Deviation</label>
                                                <input
                                                    {...input}
                                                    className="form-control"
                                                    type="number"
                                                    placeholder={this.state.deviation}
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

                                <div className="error form-submission-error">{submitError}</div>
                            </form>
                        )}
                    />
                )}
            </Popup>
        )
    }
}

export default UpdateDeviationForm;