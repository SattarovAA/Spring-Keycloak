package com.example.keycloakdemo.controller;

import com.example.keycloakdemo.model.dto.UserRegistrationDto;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.Response;
import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @PostMapping("/register")
    public String registerUser(@RequestBody UserRegistrationDto request) {
        try {
            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl(authServerUrl)
                    .realm("master")
                    .username("admin")
                    .password("admin")
                    .clientId("admin-cli")
                    .build();

            UserRepresentation user = new UserRepresentation();
            user.setUsername(request.username());
            user.setEnabled(true);
            user.setEmailVerified(true);

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(request.password());
            credential.setTemporary(false);
            user.setCredentials(Collections.singletonList(credential));

            Response response = keycloak.realm(realm).users().create(user);

            if (response.getStatus() == 201) {
                String userId = response.getLocation()
                        .getPath()
                        .replaceAll(".*/([^/]+)$", "$1");

                RoleRepresentation representation = keycloak.realm(realm)
                        .roles()
                        .get(request.role())
                        .toRepresentation();

                keycloak.realm(realm)
                        .users()
                        .get(userId)
                        .roles()
                        .realmLevel()
                        .add(Collections.singletonList(representation));

                return "User registered successfully with role: " + request.role();
            } else {
                return "Error registering user: " + response.getStatusInfo().getReasonPhrase();
            }
        } catch (Exception e) {
            return "Registration failed: " + e.getMessage();
        }
    }
}
