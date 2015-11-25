import React, {Component, PropTypes} from 'react'
import {connect} from 'react-redux'

const SelectField = require('material-ui/lib/select-field');
const MenuItem = require('material-ui/lib/menus/menu-item');

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
      <SelectField
        className={className}
        onChange={e => {
          dispatch(updateSelectedLocation(e.target.value))
          log(`Selected new destination set: ${e.target.value}`)

        }}
        menuItems={sets}
        />

    )
  }
}

export default connect(s => s.locations)(LocationSelect)
