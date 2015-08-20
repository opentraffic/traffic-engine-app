import React from 'react';
import Nav from './Nav';
import FullHeightMap from './Map';


class App extends React.Component {
    render() {
        return  <div id="layout">
                    <div id="map"><FullHeightMap/></div>
                    <div id="panel"><Nav/></div>
                </div>;
    }
}

export default App;


