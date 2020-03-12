package de.terrestris.shogun.lib.listener;

import de.terrestris.shogun.lib.model.Group;
import de.terrestris.shogun.lib.model.User;
import de.terrestris.shogun.lib.repository.GroupRepository;
import de.terrestris.shogun.lib.repository.UserRepository;
import de.terrestris.shogun.lib.security.SecurityContextUtil;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LoginListener implements ApplicationListener<InteractiveAuthenticationSuccessEvent> {
    @Autowired
    protected SecurityContextUtil securityContextUtil;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected GroupRepository groupRepository;

    @Override
    public void onApplicationEvent(InteractiveAuthenticationSuccessEvent event)
    {
        Authentication authentication = event.getAuthentication();
        Object principal = authentication.getPrincipal();

        if (!(principal instanceof KeycloakPrincipal)) {
            // TODO Error handling
            return;
        }

        // get user info from authentication object
        KeycloakPrincipal keycloakPrincipal = (KeycloakPrincipal) authentication.getPrincipal();
        String keycloakUserId = keycloakPrincipal.getKeycloakSecurityContext().getIdToken().getSubject();

        // add missing user to shogun db
        User user = userRepository.findByKeycloakId(keycloakUserId);
        if (user == null) {
            User newUser = new User(keycloakUserId, null, null);
            userRepository.save(newUser);
        }

        List<GroupRepresentation> userGroups = securityContextUtil.getKeycloakGroupsForUser(user);
        // add missing groups to shogun db
        userGroups.stream().map(GroupRepresentation::getId).forEach(keycloakGroupId -> {
            Group group = groupRepository.findByKeycloakId(keycloakGroupId);
            if (group == null) {
                Group newGroup = new Group(keycloakGroupId);
                groupRepository.save(newGroup);
            }
        });
    }
}
