import React, {Component, PropTypes} from 'react'
import {connect} from 'react-redux'

const TimePicker = require('material-ui/lib/time-picker');
const DatePicker = require('material-ui/lib/date-picker/date-picker');
const DatePickerDialog = require('material-ui/lib/date-picker/date-picker-dialog');

const Card= require('material-ui/lib/card/card');
const CardExpandable = require('material-ui/lib/card/card-expandable');
const CardHeader = require('material-ui/lib/card/card-header');
const CardActions = require('material-ui/lib/card/card-actions');

const Avatar = require('material-ui/lib/avatar');
const FontIcon = require('material-ui/lib/font-icon');

import {updateSelectedLocation} from '../actions'
import log from '../log'

import styles from '../style.css'

class TimeRangeSelect extends Component {
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
              title="Date/Time Filter"
              avatar={<Avatar icon={<FontIcon className="material-icons">today</FontIcon>}/>}
              actAsExpander={true}
              showExpandableButton={true} />
          <CardActions expandable={true}>
            <DatePicker
                floatingLabelText="From Date"/>
            <DatePicker
                floatingLabelText="To Date"/>
          </CardActions>
        </Card>

    )
  }
}

export default TimeRangeSelect
