import React, {Component, PropTypes} from 'react'
import {connect} from 'react-redux'

const TimePicker = require('material-ui/lib/time-picker');

const Card= require('material-ui/lib/card/card');
const CardExpandable = require('material-ui/lib/card/card-expandable');
const CardHeader = require('material-ui/lib/card/card-header');
const CardActions = require('material-ui/lib/card/card-actions');

const FlatButton = require('material-ui/lib/flat-button');

const Avatar = require('material-ui/lib/avatar');
const FontIcon = require('material-ui/lib/font-icon');

import {updateSelectedLocation} from '../actions'
import log from '../log'

import styles from '../style.css'

class DataImport extends Component {
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
              title="Data Import"
              avatar={<Avatar icon={<FontIcon className="material-icons">directions_car</FontIcon>}/>}

              actAsExpander={true}
              showExpandableButton={true} />
          <CardActions expandable={true}>
            <FlatButton label="Import CSV Location Data"/>
          <FlatButton label="Import PBF Location Data"/>
          </CardActions>
        </Card>

    )
  }
}

export default DataImport
