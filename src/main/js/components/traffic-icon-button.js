
import React, {Component, PropTypes} from 'react'
import {connect} from 'react-redux'

const SvgIcon = require('material-ui/lib/svg-icon');
const IconButton = require('material-ui/lib/icon-button');

class TrafficIconButton extends Component {

  render () {
    return (
      <IconButton><SvgIcon>
        <path fill="#FFFFFF" d="M0.6,8.2h4.8V2C3.2,3.4,1.5,5.6,0.6,8.2z"/>
      <path fill="#FFFFFF" d="M14.8,4.7V0.4l0,0C13.9,0.2,13,0,12,0c-1.3,0-2.6,0.2-3.8,0.6l4.6,4.7C13.4,5,14.1,4.7,14.8,4.7z"/>
      <path fill="#FFFFFF" d="M0,12c0,2.2,0.6,4.2,1.6,5.9h3.9V9.3H0.3C0.1,10.2,0,11.1,0,12z"/>
      <path fill="#FFFFFF" d="M5.5,22.1v-3.6H1.9C2.8,20,4.1,21.2,5.5,22.1z"/>
      <path fill="#FFFFFF" d="M19.3,8.3h4.1c-1.2-3.7-4.2-6.7-8-7.8v4.1C17.4,4.8,19,6.3,19.3,8.3z"/>
      <path fill="#FFFFFF" d="M10.9,9.3H6.6V18h8.2v-4.9C12.8,13,11.1,11.4,10.9,9.3z"/>
      <path fill="#FFFFFF" d="M6.6,22.8C8.2,23.6,10.1,24,12,24c1,0,1.9-0.1,2.8-0.3v-5.2H6.6V22.8z"/>
      <path fill="#FFFFFF" d="M15.4,13.1V18h7c0.1-0.2,0.3-0.5,0.4-0.8l-5-5.1C17.2,12.7,16.3,13,15.4,13.1z"/>
      <path fill="#FFFFFF" d="M23.7,9.5h-4.4c-0.1,0.7-0.3,1.3-0.7,1.8l4.7,4.8c0.4-1.3,0.7-2.6,0.7-4C24,11.1,23.9,10.3,23.7,9.5z"/>
      <path fill="#FFFFFF" d="M11.9,6.1L7,1.1C6.9,1.2,6.8,1.2,6.6,1.3v6.9h4.3C11.1,7.4,11.4,6.7,11.9,6.1z"/>
      <path fill="#FFFFFF" d="M15.4,23.5c2.8-0.8,5.1-2.6,6.7-5h-6.7L15.4,23.5L15.4,23.5z"/>
      <path fill="#FFFFFF" d="M17,11.3c0.1-0.1,0.3-0.3,0.4-0.4c0.1-0.1,0.2-0.3,0.3-0.5c0.3-0.4,0.4-1,0.4-1.5c0-1.6-1.2-2.9-2.8-3.1
      c-0.1,0-0.2,0-0.3,0s-0.2,0-0.3,0c-0.4,0-0.8,0.2-1.2,0.4c-0.2,0.1-0.3,0.2-0.5,0.3c-0.1,0.1-0.3,0.3-0.4,0.4
      c-0.5,0.5-0.7,1.2-0.7,2c0,1.6,1.2,2.9,2.8,3.1c0.1,0,0.2,0,0.3,0s0.2,0,0.3,0C16,11.9,16.6,11.7,17,11.3z"/>
      </SvgIcon></IconButton>

    )
  }
}

export default TrafficIconButton
