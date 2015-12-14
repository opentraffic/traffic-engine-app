import React, {Component, PropTypes} from 'react'
import {connect} from 'react-redux'

const SelectField = require('material-ui/lib/select-field');
const MenuItem = require('material-ui/lib/menus/menu-item');

const Card= require('material-ui/lib/card/card');
const CardExpandable = require('material-ui/lib/card/card-expandable');
const CardHeader = require('material-ui/lib/card/card-header');
const CardActions = require('material-ui/lib/card/card-actions');

const Toggle = require('material-ui/lib/toggle');

const Avatar = require('material-ui/lib/avatar');
const FontIcon = require('material-ui/lib/font-icon');

import {updateSelectedLocation} from '../actions'
import log from '../log'

class LocationSelect extends Component {
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
            title="Map Settings"
            avatar={<Avatar icon={<FontIcon className="material-icons">map</FontIcon>}/>}
            actAsExpander={true}
            showExpandableButton={true} />
        <CardActions expandable={true}>

          <SelectField
          floatingLabelStyle={ { transform: 'perspective(1px) scale(0.75) translate3d(2px, -28px, 0)' } }
            floatingLabelText="Location"
            className={className}
            onChange={e => {
              dispatch(updateSelectedLocation(e.target.value))
              log(`Selected new destination set: ${e.target.value}`)

            }}
            menuItems={sets}
            style={{ fontSize: 'large' }}
            />

          <Toggle
            label="Show Traffic Overlay"
            defaultToggled={true}/>

          <Toggle
            label="Show Data Coverage Overlay"
            defaultToggled={false}
            disabled={true}/>
        </CardActions>
      </Card>
    )
  }
}

export default connect(s => s.locations)(LocationSelect)
