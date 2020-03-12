package de.terrestris.shogun.lib.security;

import de.terrestris.shogun.lib.model.Group;
import de.terrestris.shogun.lib.model.User;
import de.terrestris.shogun.lib.repository.GroupRepository;
import de.terrestris.shogun.lib.repository.UserRepository;
import de.terrestris.shogun.lib.specification.UserSpecification;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SecurityContextUtil {

    private RealmResource realm;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    public SecurityContextUtil() {
        // TODO: create beans for this
        ResteasyClient restClient = new ResteasyClientBuilder()
            .hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY)
            .build();

        Keycloak keycloak = KeycloakBuilder.builder()
            .serverUrl("http://localhost:8000/auth")
            .realm("master")
            .username("admin")
            .password("shogun")
            .clientId("admin-cli")
            .resteasyClient(restClient)
            .build();

        this.realm = keycloak.realm("SpringBootKeycloak");
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserBySession() {
        final Object principal = SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        String userMail;

        if (principal instanceof String) {
            userMail = (String) principal;
        } else if (principal instanceof org.springframework.security.core.userdetails.User) {
            userMail = ((org.springframework.security.core.userdetails.User) principal).getUsername();
        } else if (principal instanceof KeycloakPrincipal) {
            KeycloakPrincipal p = (KeycloakPrincipal) principal;
            p.getKeycloakSecurityContext();
            userMail = (String) principal;
        } else {
            return Optional.empty();
        }

        return userRepository.findOne(UserSpecification.findByMail(userMail));
    }


    public List<GrantedAuthority> getGrantedAuthorities(User user) {

        // TODO fetch from keycloak/auth context

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        return grantedAuthorities;
    }

    /**
     * Returns the current user object from the database.
     *
     * @param authentication
     * @return
     */
    public User getUserFromAuthentication(Authentication authentication) {
        final Object principal = authentication.getPrincipal();
        if (!(principal instanceof KeycloakPrincipal)) {
            return null;
        }
        KeycloakPrincipal<?> keycloakPrincipal = (KeycloakPrincipal<?>) principal;
        String keycloakId = keycloakPrincipal.getKeycloakSecurityContext().getIdToken().getSubject();
        return userRepository.findByKeycloakId(keycloakId);
    }

    /**
     * Get (SHOGun) groups for user based on actual assignment in keycloak
     * @param user The SHOGun user
     * @return List of groups
     */
    public List<Group> getGroupsForUser(User user) {
        List<GroupRepresentation> userGroups = this.getKeycloakGroupsForUser(user);
        if (userGroups == null) {
            return null;
        }

        // return list of Groups that are in SHOGun DB
        return userGroups.stream().
            map(GroupRepresentation::getId).
            map(keycloakGroupId -> groupRepository.findByKeycloakId(keycloakGroupId)).
            collect(Collectors.toList());
    }

    /**
     * Return keycloak GroupRepresentaions (groups) for user
     * @param user
     * @return
     */
    public List<GroupRepresentation> getKeycloakGroupsForUser(User user) {
        UserResource kcUser = this.realm.users().get(user.getKeycloakId());
        return kcUser != null ? kcUser.groups() : null;
    }
}
