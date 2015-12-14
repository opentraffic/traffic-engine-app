import {handleActions} from 'redux-actions'

const initialLocationsState = {
  selected: { text: '', payload: '' },
  sets: [
    {
      text: '',
      payload: 'test'
    }
  ]
}

const locationsReducers = handleActions({
  RECEIVE_LOCATIONS: (state, action) => {
    return Object.assign({}, state, {
        isFetching: false,
        didInvalidate: false,
        sets: action.sets
      })
  },
  UPDATE_SELECTED_LOACATION: (state, action) => {
    return Object.assign({}, state, { selected: action.payload })
  }
}, initialLocationsState)

export default locationsReducers
