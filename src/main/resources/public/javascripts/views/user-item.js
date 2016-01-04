var Traffic = Traffic || {};
Traffic.views = Traffic.views || {};

(function(A, views, translator) {
  views.UserItem = Marionette.ItemView.extend({
    tagName: 'li',
    className: 'list-group-item',

    /* set the template used to display this view */
    template: Handlebars.getTemplate('app', 'user-item')
  });
})(Traffic, Traffic.views, Traffic.translations);