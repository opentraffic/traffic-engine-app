import React, { Component, PropTypes } from 'react';
import Radium, { Style } from 'radium';
import { connect } from 'react-redux';

import Nav from './Nav';
import TrafficMap from './TrafficMap';


class App extends React.Component {
    render() {
        return <div id="layout">
            <Style rules={{
                'html, body, div' : {
                    margin: '0px',
                    padding: '0px'
                },
                '#panel' : {
                    position: 'absolute',
                    top: 0,
                    right: 0,
                    overflow: 'auto',
                    width: '350px',
                    height: '100%',
                    background : '#fcfcfc'
                }
            }} />
            <div id="map"><TrafficMap/></div>
            <div id="panel"><Nav/></div>
        </div>;
    }
}



export default  Radium(App);


