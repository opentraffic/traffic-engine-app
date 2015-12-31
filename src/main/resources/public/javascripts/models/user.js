var Traffic = Traffic || {};

Traffic.models = Traffic.models || {};

Traffic.models.UserModel = Backbone.Model.extend({
  // Define constants to represent the various states and give them descriptive
  // values to help with debugging.
  notAuthState:     'not_auth',
  pendingAuthState: 'auth_pending',
  authSuccessState: 'auth_success',
  authFailState:    'auth_fail',
  authUnknownState: 'auth_unknown',

  defaults: {
    username: '',
    password: '',
    state: '',
    stateDetails: '',
    role: ''
  },

  isLoggedIn: function() {
    this.get('state') == this.authSuccessState;
  },

  reset: function() {
    this.clear().set(this.defaults);
  }
});