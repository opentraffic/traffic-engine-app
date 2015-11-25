import {tileLayer} from 'leaflet'
import {PropTypes} from 'react'
import {BaseTileLayer} from 'react-leaflet'

export default class CanvasTileLayer extends BaseTileLayer {
  static propTypes = {
    drawTile: PropTypes.func.isRequired
  }

  componentWillMount () {
    super.componentWillMount()
    const {drawTile} = this.props
    this.leafletElement = tileLayer.canvas()
    this.leafletElement.drawTile = drawTile
  }

  componentDidUpdate (prevProps) {
    const {drawTile} = this.props
    if (drawTile && drawTile !== prevProps.drawTile) {
      this.leafletElement.drawTile = drawTile
    }
  }
}
