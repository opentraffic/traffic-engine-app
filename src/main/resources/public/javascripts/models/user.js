var Traffic = Traffic || {};

Traffic.models = Traffic.models || {};

Traffic.models.UserModel = Backbone.Model.extend({
  // Define constants to represent the various states and give them descriptive
  // values to help with debugging.
  pendingSignupState:  'sign_pending',
  singupSuccessState: 'signup_success',
  signupFailState:    'signup_fail',

  notAuthState:     'not_auth',
  pendingAuthState: 'auth_pending',
  authSuccessState: 'auth_success',
  authFailState:    'auth_fail',
  authUnknownState: 'auth_unknown',

  defaults: {
    username: '',
    password: '',
    confirm_password: '',
    state: '',
    stateDetails: '',
    role: '',
    token: ''
  },

  isLoggedIn: function() {
    this.get('state') == this.authSuccessState;
  },

  reset: function() {
    this.clear().set(this.defaults);
  }
});