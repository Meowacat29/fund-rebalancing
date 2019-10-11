import React from 'react';

import CUSTOMER_PHOTO_1 from '../data/customer1.png';
import CUSTOMER_PHOTO_2 from '../data/customer2.png';
import CUSTOMER_PHOTO_3 from '../data/customer3.png';

const CUSTOMER_NAME_1 = "ALEX";
const CUSTOMER_NAME_2 = "JASMINE";
const CUSTOMER_NAME_3 = "KEVIN";

/**
 * Used to display the user's profile for the demo
 */
export const PhotoProfile = (props) => {
    return (
        <div>
            {(function() {
                switch(props.customerId){
                    case 'nxqa3cu9r6':
                        return(
                        <div>
                        <img className= "photo" src={CUSTOMER_PHOTO_1}/>
                        <div className = "greeting"> Good afternoon! {CUSTOMER_NAME_1} </div>
                        </div>
                        );
                    case "yv9q6aodfa":
                        return(
                        <div>
                        <img className= "photo" src={CUSTOMER_PHOTO_2}/>
                        <div className = "greeting"> Good afternoon! {CUSTOMER_NAME_2} </div>
                        </div>
                        );
                    case "t8ej8u8q5n":
                        return(
                        <div>
                        <img className= "photo" src={CUSTOMER_PHOTO_3}/>
                        <div className = "greeting"> Good afternoon! {CUSTOMER_NAME_3} </div>
                        </div>
                        );
                    default:
                        return(
                        <div>
                        <img className= "photo" src={CUSTOMER_PHOTO_1}/>
                        <div className = "greeting"> Good afternoon! HSBC customer </div>
                        </div>
                        );
                    }
                  })()}
        </div>
    )
};