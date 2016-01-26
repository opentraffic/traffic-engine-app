var Traffic = Traffic || {};
Traffic.views = Traffic.views || {};

(function(A, views, models, translator) {

    views.UserMenu = Marionette.Layout.extend({

        template: Handlebars.getTemplate('app', 'user-menu'),

        events : {
            'click #newUser' : 'clickNewUser',
            'click #logout' : 'clickLogout',
            'click #usersLink' : 'clickUsersLink',
            'click #dataManagementLink' : 'clickDataLink'
        },

        clickNewUser: function() {
            A.app.instance.newUserModal = new Backbone.BootstrapModal({
                animate: true,
                content: new views.NewUser({model: new models.UserModel()}),
                title: translator.translate("new_user_dialog_title"),
                showFooter: false
            });

            A.app.instance.newUserModal.on('cancel', function() {
                this.options.content.remove(); //remove previous view
                A.app.instance.newUserModal = null;
            });

            A.app.instance.newUserModal.open();
        },

        clickLogout: function() {
            A.app.instance.vent.trigger('logout:success')
        },

        getUserGridColumns: function() {
            return [{
                name: 'id',
                cell: 'integer',
                editable: false,
                label: translator.translate('id_title')
            },{
                name: 'username',
                cell: 'string',
                editable: false,
                label: translator.translate('username_title')
            }, {
                name: 'role',
                cell: 'string',
                editable: false,
                label: translator.translate('role_title')
            }, {
                name: '',
                cell: 'html',
                editable: false,
                label: '',
                formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                    fromRaw: function (rawValue, obj) {
                        var html = '';
                        var currentUser = A.app.instance.user;
                        if(currentUser.isSuperAdmin()) {
                            html = '<div class="user-actions">';
                            if(currentUser && !obj.isSelf(currentUser.get('username'))) {
                                html += '<button class="btn btn-xs delete-user">' + translator.translate('delete_user') + '</button>';
                            }
                            html += '<button class="btn btn-xs edit-user">' + translator.translate('edit_user') + '</button></div>';
                        }

                        return html;
                    }
                })
            }];
        },

        clickUsersLink: function() {

            var _this = this;

            $.getJSON('/users', function(data) {
                A.app.instance.usersCollection = new A.collections.Users(data);
                A.app.instance.usersCollection.setSorting("id", {mode: 'client'});
                A.app.instance.usersCollection.fullCollection.sort();

                var usersCollection = A.app.instance.usersCollection;

                var usersList = new Backgrid.Grid({
                    columns: _this.getUserGridColumns(),
                    collection: usersCollection
                });

                A.app.instance.usersModal = new Backbone.BootstrapModal({
                    animate: true,
                    content: "<div class='users-table'></div>",
                    title: translator.translate("users_dialog_title"),
                    showFooter: false
                });

                A.app.instance.usersModal.on('cancel', function() {
                    $('.users-table').remove(); //remove previous view
                    A.app.instance.usersModal = null;
                });

                var renderGrid = function() {
                    $('.users-table').append(usersList.render().el)

                    $('.users-table').on('click', '.delete-user', _this.clickDeleteUser);
                    $('.users-table').on('click', '.edit-user', _this.clickEditUser);

                    // Initialize the paginator
                    var paginator = new Backgrid.Extension.Paginator({
                        collection: usersCollection,
                        controls: {
                            rewind: {
                                label: " <<",
                                title: translator.translate('move_to_first')
                            },
                            back: {
                                label: " <",
                                title: translator.translate('move_to_previous')
                            },
                            forward: {
                                label: "> ",
                                title: translator.translate('move_to_next')
                            },
                            fastForward: {
                                label: ">> ",
                                title: translator.translate('move_to_last')
                            }
                        }
                    });

                    // Render the paginator
                    $(usersList.el).after(paginator.render().el);

                    // Initialize a client-side filter to filter on the client
                    // mode pageable collection's cache.
                    var filter = new Backgrid.Extension.ClientSideFilter({
                        collection: usersCollection,
                        fields: ['username', 'role']
                    });

                    // Render the filter
                    $(usersList.el).before(filter.render().el);

                    // Add some space to the filter and move it to the right
                    $(filter.el).css({float: "right", margin: "5px 0px"});

                    //TODO: Fetch some data
                    //usersCollection.fetch({reset: true});
                };
                A.app.instance.usersModal.on('shown', renderGrid);

                A.app.instance.usersModal.open();
            });
        },

        clickDeleteUser: function() {
            //TODO
            console.log('deleting user');
            var confirmDialog = new Backbone.BootstrapModal({
                animate: true,
                content: translator.translate('deletion_confirmation_message'),
                title: translator.translate('deletion_confirmation_title')
            });

            confirmDialog.open();
            var button = this;
            confirmDialog.on('ok', function() {
                var userId = $(button).parents('tr').find('td:first').text();
                var userToDelete = A.app.instance.usersCollection.get(userId);
                if(userToDelete)
                    userToDelete.destroy();

                A.app.instance.usersCollection.remove(userToDelete);
            });
        },

        clickEditUser: function() {
            var userId = $(this).parents('tr').find('td:first').text();
            var userToEdit = A.app.instance.usersCollection.get(userId);

            if(userToEdit) {
                userToEdit.clearState();
                userToEdit.clearPasswords();
            }

            A.app.instance.editUserModal = new Backbone.BootstrapModal({
                animate: true,
                content: new views.EditUser({model: userToEdit}),
                title: translator.translate("edit_user_dialog_title"),
                showFooter: false
            });

            A.app.instance.editUserModal.on('cancel', function() {
                this.options.content.remove(); //remove previous view
                A.app.instance.editUserModal = null;
            });

            A.app.instance.editUserModal.open();

            A.app.instance.editUserModal.$('#roleList').val(userToEdit.get('role'));
        },

        clickDataLink: function() {

            console.log('Data management dialog: TODO');
        },

        onRender: function() {
            this.$el.html(this.template(A.app.instance.Login.userModel.toJSON()));
        }
    });
})(Traffic, Traffic.views, Traffic.models, Traffic.translations);