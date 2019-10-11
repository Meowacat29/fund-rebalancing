import {Field, Form} from "react-final-form";
import React from "react";

/**
 * A customer ID form shown on the main page
 */
export const MainForm = (props) => {
    return (
        <div className="centered main-form-wrapper">
            <h3>Please enter a Customer ID:</h3>
            <Form
                onSubmit={props.onSubmit}
                render={({ pristine, submitError, handleSubmit, submitting }) => (
                    <div>
                        <form className="form-inline" onSubmit={handleSubmit}>
                            <div className="form-group mx-sm-3 mb-2">
                                <Field
                                    className="form-control"
                                    name="customerId"
                                    component="input"
                                    placeholder="Customer ID"
                                />
                            </div>

                            <button className="btn mb-2 red-btn" type="submit" disabled={pristine || submitting}>
                                Sign In
                            </button>


                        </form>
                        <div className="mx-sm-3 mb-2 error text-left">{submitError}</div>
                    </div>
                )}
            />
        </div>
    )
};