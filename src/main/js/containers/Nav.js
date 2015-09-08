import React from 'react';
import Radium, { Style } from 'radium';
class Nav extends React.Component {

    constructor(){
        super();
        this.state = {showSettings: false};
    }

    _onToggleSettings() {
        this.setState({showSettings: !this.state.showSettings});
    }

    render() {

        var settings;
        if(this.state.showSettings) {
            settings = <div className="content">
                Some of this stuff is pretty crazy
            </div>;
        }

        return <section className="Nav">
                    <Style
                        scopeSelector=".Nav"
                        rules={{
                            '.navbar-default': {
                                border: 'none'
                            },
                            '.nav > li > a' : {
                                'padding-left': '5px',
                                'padding-right': '5px'
                            },
                            '.logo' : {
                                height: '30px',
                                'vertical-align': 'middle',
                                'margin-top': '-5px',
                                display: 'inline !important'
                            }
                        }}
                    />
                <nav className="navbar navbar-default">
                    <div className="navbar-header">
                        <a className="navbar-brand" href="#">
                            <img src="/images/opentraffic_logo.png" className="logo"/> Traffic Engine
                        </a>
                    </div>
                    <div className="navbar-collapse navbar-right">
                        <ul className="nav navbar-nav">
                            <li className={this.state.showSettings ? 'active' : ''} onClick={this._onToggleSettings.bind(this)}><a href="#"><span className="glyphicon glyphicon-cog"></span></a></li>
                        </ul>
                    </div>
                </nav>
                {settings}
            </section>;

    }
}

export default Radium(Nav);

