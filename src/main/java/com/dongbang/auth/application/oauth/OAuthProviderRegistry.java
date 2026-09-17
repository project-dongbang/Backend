package com.dongbang.auth.application.oauth;

import com.dongbang.auth.domain.OAuthProvider;
import com.dongbang.auth.exception.AuthErrorCode;
import com.dongbang.global.exception.GeneralException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class OAuthProviderRegistry {

    private final Map<OAuthProvider, OAuthProviderClient> clients;

    public OAuthProviderRegistry(List<OAuthProviderClient> clients) {
        Map<OAuthProvider, OAuthProviderClient> indexed = new EnumMap<>(OAuthProvider.class);
        clients.forEach(client -> indexed.put(client.provider(), client));
        this.clients = Map.copyOf(indexed);
    }

    public OAuthProviderClient get(OAuthProvider provider) {
        OAuthProviderClient client = clients.get(provider);
        if (client == null) {
            throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
        return client;
    }
}
