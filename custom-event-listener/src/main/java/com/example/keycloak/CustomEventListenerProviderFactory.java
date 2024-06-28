package com.example.keycloak;

import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class CustomEventListenerProviderFactory implements EventListenerProviderFactory {

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new CustomEventListenerProvider(session);
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
        // Khởi tạo nếu cần
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Khởi tạo sau nếu cần
    }

    @Override
    public void close() {
        // Đóng nếu cần
    }

    @Override
    public String getId() {
        return "custom-event-listener";
    }
}
