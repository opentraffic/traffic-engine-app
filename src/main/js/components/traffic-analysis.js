import React, {Component, PropTypes} from 'react'
import {connect} from 'react-redux'

const TimePicker = require('material-ui/lib/time-picker');

const Card= require('material-ui/lib/card/card');
const CardExpandable = require('material-ui/lib/card/card-expandable');
const CardHeader = require('material-ui/lib/card/card-header');
const CardText = require('material-ui/lib/card/card-Text');

const Avatar = require('material-ui/lib/avatar');
const FontIcon = require('material-ui/lib/font-icon');

import {updateSelectedLocation} from '../actions'
import log from '../log'

import styles from '../style.css'

class TrafficAnalysis extends Component {
  static propTypes = {
    className: PropTypes.string,
    dispatch: PropTypes.func,
    selected: PropTypes.object,
    sets: PropTypes.arrayOf(PropTypes.object).isRequired
  }

  render () {
    const {dispatch, className, selected, sets} = this.props

    return (
        <Card initiallyExpanded={false}>
          <CardHeader
              title="Traffic Analysis"
              avatar={<Avatar icon={<FontIcon className="material-icons">traffic</FontIcon>}/>}

              actAsExpander={true}
              showExpandableButton={true} />

          <CardText expandable={true}>
              [pending chart integration]
          </CardText>

        </Card>

    )
  }
}

export default TrafficAnalysis
