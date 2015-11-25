import {addActionLogItem} from './actions'
import {dispatch} from './store'

export default function (l) {
  dispatch(addActionLogItem(l))
}
