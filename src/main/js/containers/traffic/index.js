import React, {Component, PropTypes} from 'react'
import {Marker, Popup} from 'react-leaflet'
import {connect} from 'react-redux'

import {updateMapMarker, updateMap} from '../../actions'
import {mapbox} from '../../config'
import LocationSelect from '../../components/location-select'
import TimeRangeSelect from '../../components/time-range-select'
import DataImport from '../../components/data-import'
import DataExport from '../../components/data-export'
import TrafficAnalysis from '../../components/traffic-analysis'
import Routing from '../../components/routing'
import Fullscreen from '../../components/fullscreen'
import log from '../../log'
import Log from '../../components/log'
import Map from '../../components/map'
import TrafficIconButton from '../../components/traffic-icon-button'
import styles from './style.css'

const AppBar = require('material-ui/lib/app-bar');

function printLL (ll) {
  return `[ ${ll[0].toFixed(4)}, ${ll[1].toFixed(4)} ]`
}

class Traffic extends Component {

  static propTypes = {
    dispatch: PropTypes.any,
    map: PropTypes.object
  }

  constructor (props) {
    super(props)
  }


  render () {
    const {dispatch, map, mapMarker} = this.props

    return (
      <Fullscreen>
        <div className={styles.main}>
          <Map
            className={styles.map}
            map={map}
            onChange={state => dispatch(updateMap(state))}
            onClick={e => {
              const {lat, lng} = e.latlng
              log(`Clicked map at ${printLL([lat, lng])}`)

              dispatch(updateMapMarker({
                position: [lat, lng],
                text: ''
              }))
            }}>
          </Map>
          <div className={styles.sideBar}>
          <AppBar
            title="Traffic Engine"
            iconElementLeft={<TrafficIconButton/>}/>
            <div className={styles.scrollable}>
              <LocationSelect className='form-control' />
              <TimeRangeSelect className='form-control' />
              <TrafficAnalysis className='form-control' />
              <Routing className='form-control' />
              <DataImport className='form-control' />
              <DataExport className='form-control' />


            </div>

          </div>
        </div>
      </Fullscreen>
    )
  }
}

function getMapFromEvent (event) {
  let {_layers, _map} = event.target

  if (_map) return _map

  for (let key in _layers) {
    if (_layers.hasOwnProperty(key) && _layers[key]._map) {
      return _layers[key]._map
    }
  }
}

export default connect(s => s)(Traffic)
