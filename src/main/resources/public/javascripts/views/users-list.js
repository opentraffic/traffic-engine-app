var Traffic = Traffic || {};
Traffic.views = Traffic.views || {};

(function(A, views, collections, translator) {
  views.UsersList = Marionette.CollectionView.extend({
    tagName: 'ul',
    className: 'list-group',

    /* explicitly set the childview (formerly 'itemView') used to display the models in this collection */
    itemView: views.UserItem,

    initialize: function(){ 

      if(!A.app.instance.usersCollection) {
        this.getUsers();
      }

      A.app.instance.usersCollection.sort();

      /* create a collection using the array of anonymous objects */
      this.collection = A.app.instance.usersCollection;
    },

    getUsers: function(callback) {
      //TODO: API to get /users
      var usersArray = [];
      usersArray.push({username: 'superadmin', role: 'super_admin'});
      usersArray.push({username: 'admin', role: 'admin'});
      usersArray.push({username: 'user', role: 'user'});

      A.app.instance.usersCollection = new collections.Users(usersArray);
    },
    onRender: function(){ console.log('BookCollectionView: onRender') },
    onShow: function(){ console.log('BookCollectionView: onShow') }
  });
})(Traffic, Traffic.views, Traffic.collections, Traffic.translations);