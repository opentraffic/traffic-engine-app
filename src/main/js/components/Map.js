import React from 'react';
import L from 'leaflet';
import $ from 'jquery'

class FullHeightMap extends React.Component {

    constructor(center, zoom) {
        super();
        this.center = [50,50];
        this.zoom = 13;
    }

    updateDimensions() {
        if(this.leafletElement) {
            $("#map").height($(window).height());
            $("#map").width($(window).width() - 350);
            this.leafletElement.invalidateSize();
        }
    }

    componentWillMount() {
        this.updateDimensions();
    }

    componentDidMount() {

        this.leafletElement = L.map('map').setView(this.center, this.zoom);
        this.setState({map: this.leafletElement});

        L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(this.leafletElement);

        window.addEventListener("resize", function(){
            this.updateDimensions();
        }.bind(this));

        this.updateDimensions();
    }

    componentWillUnmount() {
        window.removeEventListener("resize", this.updateDimensions);
    }

    render() {
        const map = this.leafletElement;
        const children = map ? React.Children.map(this.props.children, child => {
            return child ? React.cloneElement(child, {map}) : null;
        }) : null;

        return (<div id="map">{children}</div>);
    }
}

export default FullHeightMap;