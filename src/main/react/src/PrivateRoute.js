import React from 'react';
import { Route, Redirect } from 'react-router-dom';

/**
 * Component to redirect to a specific path if the user is not authenticated
 */
export function PrivateRoute({component : Component, authed, ...rest}) {
    return (
        <Route
            {... rest}
            render={(props) => authed === true
                ? <Component {...props} />
                : <Redirect to={{pathname: '/', state: {from: props.location}}} />}
        />
    )
}