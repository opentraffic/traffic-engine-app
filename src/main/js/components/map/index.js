import React, {Component, PropTypes} from 'react'
import {Map as BaseMap, TileLayer} from 'react-leaflet'

const ATTRIBUTION = `&copy <a href="https://www.mapbox.com/map-feedback/">Mapbox</a> &copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>`

export default class Map extends Component {
  static propTypes = {
    className: PropTypes.string,
    children: PropTypes.any,
    map: PropTypes.object,
    onChange: PropTypes.func,
    onClick: PropTypes.func
  }

  render () {
    const {className, children, map, onChange, onClick} = this.props
    const {center, mapbox, zoom} = map
    const url = `http://api.tiles.mapbox.com/v4/${mapbox.mapId}/{z}/{x}/{y}.png?access_token=${mapbox.accessToken}`

    return (
      <BaseMap
        center={center}
        className={className}
        zoom={zoom}
        onLeafletClick={onClick}
        onLeafletZoomEnd={e => onChange({ zoom: e.target._zoom })}>
        <TileLayer
          url={url}
          attribution={ATTRIBUTION}
        />
        {children}
      </BaseMap>
    )
  }
}
