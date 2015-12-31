var Traffic = Traffic || {};

Traffic.models = Traffic.models || {};

Traffic.models.UserModel = Backbone.Model.extend({
  // Define constants to represent the various states and give them descriptive
  // values to help with debugging.
  notAuthState:     'Not Authenticated',
  pendingAuthState: 'Pending Authentication',
  authSuccessState: 'Authentication Success',
  authFailState:    'Authentication Failure',
  authUnknownState: 'Authentication Unknown',

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