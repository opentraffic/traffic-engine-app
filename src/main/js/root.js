import React, {Component, PropTypes} from 'react'
import {Provider} from 'react-redux'
import {DevTools, DebugPanel, LogMonitor} from 'redux-devtools/lib/react'

import store from './store'

export default class Root extends Component {
  static propTypes = {
    children: PropTypes.element.isRequired
  }

  render () {
    return (
      <div>
        <Provider store={store}>{this.props.children}</Provider>
        <DebugPanel top left bottom>
          <DevTools store={store} monitor={LogMonitor} visibleOnLoad={false} />
        </DebugPanel>
      </div>
    )
  }
}
