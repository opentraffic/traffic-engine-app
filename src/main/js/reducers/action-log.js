import {handleActions} from 'redux-actions'

const initialActionLog = [{
  createdAt: new Date(),
  index: 0,
  text: 'test'
}]

const actionLogReducers = handleActions({
  ADD_ACTION_LOG_ITEM: (state, action) => {
    return [action.payload, ...state]
  }
}, initialActionLog)

export default actionLogReducers
