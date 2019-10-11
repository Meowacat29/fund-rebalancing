import React from 'react';
import { Link } from "react-router-dom";

export const BackButton = (props) => {
    return (
        <button className="btn back-cancel-btn back-btn">
            <Link {...props}>
                <i>Back</i>
            </Link>
        </button>
    )
};