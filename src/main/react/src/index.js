import ReactDOM from 'react-dom';
import { BrowserRouter as Router, Route, Switch } from "react-router-dom";
import React from "react";

import { PrivateRoute } from "./PrivateRoute";
import * as serviceWorker from './serviceWorker';
import App from './views/App';
import CustomerPortfolios from "./views/CustomerPortfolios";
import Portfolio from "./views/Portfolio";
import Recommendation from './views/Recommendation';

import { LOCAL_STORAGE_CUST_ID_KEY } from "./constants";
import { NavBar } from "./shared/NavBar";

import './scss/index.scss';

ReactDOM.render(
    <div className="App">
        <NavBar />
        <Router>
            <Switch>
                <Route exact path="/" component={App} />
                <PrivateRoute
                    exact
                    path="/portfolios"
                    component={CustomerPortfolios}
                    authed={localStorage.getItem(LOCAL_STORAGE_CUST_ID_KEY) != null}
                />
                <PrivateRoute
                    path="/portfolios/:id/rebalance"
                    component={Recommendation}
                    authed={localStorage.getItem(LOCAL_STORAGE_CUST_ID_KEY) != null}
                />
                <PrivateRoute
                    path="/portfolios/:id"
                    component={Portfolio}
                    authed={localStorage.getItem(LOCAL_STORAGE_CUST_ID_KEY) != null}
                />
            </Switch>
        </Router>
    </div>,
    document.getElementById('root')
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();
