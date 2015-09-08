import React from 'react';
import L from 'leaflet';
import Radium from 'radium';
import $ from 'jquery'

class Map extends React.Component {

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

        this.leafletElement = L.map('map');
        this.leafletElement.on("moveend", function(){
            this.props.onIncrement(this.leafletElement.getZoom(), this.leafletElement.getCenter());
        });

        L.tileLayer(this.props.map.baseLayerURL, {}).addTo(this.leafletElement);

        this.setState({map: this.leafletElement});

        this.updateMapFromProps();

        window.addEventListener("resize", function(){
            this.updateDimensions();
        }.bind(this));

        this.updateDimensions();
    }

    componentWillUnmount() {
        window.removeEventListener("resize", this.updateDimensions);
    }

    updateMapFromProps() {
        if(this.leafletElement) {
            const map = this.leafletElement;

            map.setView(this.props.map.center, this.props.map.zoom);

            // TODO pop layers on and off stack from props
        }
    }

    render() {

        const map = this.leafletElement;
        const children = map ? React.Children.map(this.props.children, child => {
            return child ? React.cloneElement(child, {map}) : null;
        }) : null;

        return (<div style={[
                    styles.base
                ]}> id="map">{children}</div>);
    }
}

var styles = {
    base: {
        position: 'absolute',
        margin: 0,
        right: '350px'
    }
};

export default Radium(Map);