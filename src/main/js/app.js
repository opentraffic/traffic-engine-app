import React from 'react';
import { Router, Route, Link } from 'react-router';
import BrowserHistory from 'react-router/lib/BrowserHistory';

// Redux DevTools store enhancers
import { DevTools, DebugPanel, LogMonitor } from 'redux-devtools/lib/react';
import { devTools, persistState } from 'redux-devtools';

// React components for Redux DevTools

import { createStore, compose } from 'redux';
import { Provider } from 'react-redux';

import App from './containers/App';

import trafficReducer from './reducers';

const history = new BrowserHistory();

let finalCreateStore;
let devPanelElement;
if(__DEVTOOLS__) {
    finalCreateStore = compose(
        devTools(),
        persistState(window.location.href.match(/[?&]debug_session=([^&]+)\b/))
    )(createStore);
}
else {
    finalCreateStore = compose(
        persistState(window.location.href.match(/[?&]debug_session=([^&]+)\b/))
    )(createStore);
}

let store = finalCreateStore(trafficReducer);

let rootElement = document.getElementById('app');

if(__DEVTOOLS__) {
   devPanelElement = <DebugPanel top right bottom>
        <DevTools store={store} monitor={LogMonitor} />
    </DebugPanel>;
}

React.render(
    <div>
        <Provider store={store}>
             {() =>
                 <Router history={history}>
                     <Route path="/" component={App}>
                     </Route>
                 </Router>
                 }
        </Provider>
        {devPanelElement}
    </div>,
    rootElement
);
