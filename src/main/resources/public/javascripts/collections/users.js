var Traffic = Traffic || {};

Traffic.collections = Traffic.collections || {};

Traffic.collections.Users = Backbone.PageableCollection.extend({
  model: Traffic.models.UserModel,
  url: '/users',
  state: {
    pageSize: 15,
    sortKey: "id",
    order: 1
  },
  mode: 'client'
});