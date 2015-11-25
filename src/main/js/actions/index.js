import fetch from 'isomorphic-fetch'
import {stringify} from 'qs'
import {createAction} from 'redux-actions'

export const ADD_ACTION_LOG_ITEM = 'ADD_ACTION_LOG_ITEM'
export const addActionLogItem = createAction(ADD_ACTION_LOG_ITEM, (item) => {
  const payload = typeof item === 'string'
    ? { text: item }
    : item

  return Object.assign({
    createdAt: new Date(),
    level: 'info'
  }, payload)
})

export const UPDATE_MAP = 'UPDATE_MAP'
export const updateMap = createAction(UPDATE_MAP)

export const UPDATE_SELECTED_LOCATION = 'UPDATE_SELECTED_LOCATION'
export function updateSelectedLocation(location) {
  return dispatch => {
    dispatch(updateMap(location))
  }
}

export const REQUEST_LOCATIONS = 'REQUEST_LOCATIONS'
function requestLocations(locations) {
  return {
    type: REQUEST_LOCATIONS,
    locations
  }
}

export const RECEIVE_LOCATIONS = 'RECEIVE_LOCATIONS'
function receiveLocations(locations, json) {
  return {
    type: RECEIVE_LOCATIONS,
    locations,
    sets: json.clusters.map( cluster => ({text: cluster.name, payload: {latlng: [cluster.lat, cluster.lon]}}))
  }
}

export default function fetchLocations(locations) {
  return dispatch => {
    dispatch(requestLocations(locations))
    return fetch(`/api/clusters`)
      .then(response => response.json())
      .then(json => dispatch(receiveLocations(locations, json)))
  }
}
