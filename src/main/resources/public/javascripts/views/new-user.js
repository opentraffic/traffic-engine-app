var Traffic = Traffic || {};
Traffic.views = Traffic.views || {};

(function(A, views, translator, C, dc) {

  views.NewUser = Marionette.Layout.extend({

    template: Handlebars.getTemplate('app', 'new-user'),
    tagName:   'div',
    className: 'new-user-area',

    ui: {
      loginForm:           '#newUserForm',
      usernameField:       'input[name=username]',
      passwordField:       'input[name=password]',
      roleField:           '#roleList',
      successMessage:      '.msg-success',
      generalErrorMessage: '.error-unknown'
    },

    events : {
      'submit form': 'formSubmitted'
    },

    //Specify the model properties that we should rerender the view on.
    modelEvents: {
      'change:state':        'render',
      'change:stateDetails': 'render'
    },

    initialize : function() {
      _.bindAll(this, 'formSubmitted');
    },

    formSubmitted: function(event) {
      // Stop the form from actually submitting to the server.
      event.stopPropagation();
      event.preventDefault();

      this.model.set('username', this.$('input[name=username]').val()); 
      this.model.set('password', this.$('input[name=password]').val());
      this.model.set('role', this.$('#roleList').val());

      // Fire off the global event for the controller so that it handles the server communication.
      A.app.instance.vent.trigger('new-user:submit', this.model)
    },

    onRender : function () {
      // This is where most of the state-dependent logic goes that used to be
      // written as random jQuery calls. Now, since the view is rerendered on
      // each state change, you just have to modify the DOM relative to the
      // initial content specified in the Underscore template.

      switch(this.model.get('state')) {
        case this.model.pendingSignupState:
          // Disable all the form controls and change the button text to show
          // the user that a request is pending.
          this.ui.loginForm.find('input, select, textarea').prop('disabled', true);
          this.ui.loginForm.find('input[type=submit]').val(translator.translate('processing'));
          break;

        case this.model.signupFailState:
          this.ui.generalErrorMessage.show();
          break;

       case this.model.signupSuccessState:
          this.ui.successMessage.show();
          A.app.instance.vent.trigger('new-user:success')
         
          break;

        default:
          this.ui.usernameField.focus();

          break;
      }
    },

    onShow: function() {
      this.ui.usernameField.focus();
    }
  });
})(Traffic, Traffic.views, Traffic.translations, crossfilter, dc);