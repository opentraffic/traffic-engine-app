var Traffic = Traffic || {};

Traffic.collections = Traffic.collections || {};

Traffic.collections.Users = Backbone.Collection.extend({
  model: Traffic.models.UserModel,
  comparator: 'username'
});