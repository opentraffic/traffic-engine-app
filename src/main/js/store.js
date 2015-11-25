import {applyMiddleware, compose, createStore} from 'redux'
import {devTools, persistState} from 'redux-devtools'
import thunk from 'redux-thunk'

import reducers from './reducers'

const finalCreateStore = compose(
  applyMiddleware(thunk),
  devTools(),
  persistState(window.location.href.match(/[?&]debug_session=([^&]+)\b/))
)(createStore)

const store = finalCreateStore(reducers)

import fetchLocations from './actions'
store.dispatch(fetchLocations()).then(() =>
  console.log(store.getState())
)

export default store
