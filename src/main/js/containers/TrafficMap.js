import { connect } from 'react-redux';
import Radium, { Style } from 'radium'
import React from 'react';

import { mapMove } from '../actions.js';
import Map from '../components/Map';

function mapStateToProps(state) {
    return {
        map: state.map
    };
}

// Which action creators does it want to receive by props?
function mapDispatchToProps(dispatch) {
    return {
        onMove: (zoom, center) => dispatch(mapMove(zoom, center))
    };
}

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(Radium(Map));

