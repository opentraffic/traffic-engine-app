var Traffic = Traffic || {};

Traffic.collections = Traffic.collections || {};

Traffic.collections.Routes = Backbone.PageableCollection.extend({
  model: Traffic.models.RouteModel,
  url: '/routelist',
  state: {
    pageSize: 15,
    sortKey: "name",
    order: 1
  },
  mode: 'client'
});