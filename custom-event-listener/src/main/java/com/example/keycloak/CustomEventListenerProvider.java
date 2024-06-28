package com.example.keycloak;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.GroupModel;

public class CustomEventListenerProvider implements EventListenerProvider {

    private final KeycloakSession session;

    public CustomEventListenerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        if (EventType.REGISTER.equals(event.getType())) {
            RealmModel realm = session.realms().getRealm(event.getRealmId());
            UserModel user = session.users().getUserById(realm, event.getUserId());
            GroupModel group = session.groups().getGroupByName(realm, null, "Nguoi dan");

            if (group != null && user != null) {
                user.joinGroup(group);
            }
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Không xử lý sự kiện AdminEvent, nhưng phương thức này cần có mặt
    }

    @Override
    public void close() {
    }
}
