import createBrowserHistory from 'history/lib/createBrowserHistory'
import React from 'react'
import {Router, Route} from 'react-router'

import Place from './containers/place'

const Routes = () => (
  <Router history={createBrowserHistory()}>
    <Route path='/' component={Place} />
  </Router>
)

export default Routes
