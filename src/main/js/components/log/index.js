import React, {Component, PropTypes} from 'react'
import {connect} from 'react-redux'

import LogItem from '../log-item'
import style from './style.css'

class Log extends Component {
  static propTypes = {
    actionLog: PropTypes.arrayOf(PropTypes.object).isRequired
  }

  render () {
    const {actionLog} = this.props
    return <div className={style.log}>{actionLog.map((logItem, index) => <LogItem {...logItem} key={index} />)}</div>
  }
}

export default connect(s => s)(Log)
