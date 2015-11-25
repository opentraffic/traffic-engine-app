import React, {Component, PropTypes} from 'react'
import moment from 'moment'

import style from './style.css'

const format = 'MM-DD HH:mm:ss'

export default class LogItem extends Component {
  static propTypes = {
    createdAt: PropTypes.object,
    key: PropTypes.number,
    level: PropTypes.string,
    text: PropTypes.string.isRequired
  }

  render () {
    const {createdAt, text} = this.props
    return (
      <div className={style.logItem}>
        <small className={style.createdAt}>{moment(createdAt).format(format)}</small>
        <span className={style.text}>{text}</span>
      </div>
    )
  }
}
