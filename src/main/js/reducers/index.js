import {combineReducers} from 'redux'
import {routerStateReducer as router} from 'redux-router'

import actionLog from './action-log'
import locations from './locations'
import map from './map'
import mapMarker from './map-marker'


const placeApp = combineReducers({
  actionLog,
  locations,
  map,
  mapMarker,
  router
})

export default placeApp
