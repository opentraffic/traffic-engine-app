import { combineReducers } from 'redux';

let inititialTrafficAppState = {
    map : {
        baseLayerURL : "https://{s}.tiles.mapbox.com/v4/conveyal.gepida3i/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoiY29udmV5YWwiLCJhIjoiMDliQURXOCJ9.9JWPsqJY7dGIdX777An7Pw",
        zoom : 15,
        center : [50,50]
    }
};


// Updates an entity cache in response to any action with response.entities.
function map(state = inititialTrafficAppState.map, action) {
    if (action.response && action.response.entities) {
        return merge({}, state, action.response.entities);
    }

    return state;
}

const trafficReducer = combineReducers({
    map
});

export default trafficReducer;
