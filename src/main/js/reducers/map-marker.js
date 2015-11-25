import {handleActions} from 'redux-actions'

import config from '../config'

const initialMapMarker = {
  isDragging: false,
  position: config.center,
  description: ''
}

const mapMarkerReducers = handleActions({
  UPDATE_MAP_MARKER: (state, action) => {
    return Object.assign({}, action.payload)
  }
}, initialMapMarker)

export default mapMarkerReducers
