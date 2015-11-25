import 'babel-core/polyfill'
import React from 'react'
import {render} from 'react-dom'

import Root from './root'
import Site from './containers/traffic'

var injectTapEventPlugin = require("react-tap-event-plugin");
injectTapEventPlugin();

render(<Root><Site /></Root>, document.getElementById('root'))
