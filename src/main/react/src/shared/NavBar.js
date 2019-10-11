import React from 'react';

import HSBCLOGO from '../HSBC_logo.svg.png';

export const NavBar = () => {
    return (
        <div>
            <header className="App-header">
                <div className="header-content">
                    <span className="header-logo" ><img src={HSBCLOGO} alt="Logo" /></span>
                </div>
            </header>
        </div>
    )
};