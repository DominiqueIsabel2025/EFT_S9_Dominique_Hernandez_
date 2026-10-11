package cl.duoc.authserver;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Configuracion del Authorization Server (OAuth 2.0).
 *
 * Flujos soportados:
 *  - client_credentials : comunicacion maquina-a-maquina (Postman, BFFs hacia core, terminales).
 *  - authorization_code : usuario final con login (cliente1 / admin) + refresh_token.
 *
 * Scopes del sistema:
 *  banking.read / banking.write -> acceso al core-banking-service y notification-service
 *  web.access / mobile.access / atm.operate -> acceso a cada BFF segun el canal
 */
@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

    /** Cadena 1: endpoints del protocolo OAuth2 (/oauth2/token, /oauth2/jwks, ...). */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
        return http.build();
    }

    /** Cadena 2: login de usuarios y health check para Docker. */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .formLogin(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder(); // BCrypt por defecto
    }

    /** Usuarios finales (solo se usan en el flujo authorization_code). */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder,
                                                 @Value("${auth.users.cliente-password}") String clientePass,
                                                 @Value("${auth.users.admin-password}") String adminPass) {
        UserDetails cliente = User.withUsername("cliente1")
                .password(encoder.encode(clientePass)).roles("CLIENTE").build();
        UserDetails admin = User.withUsername("admin")
                .password(encoder.encode(adminPass)).roles("ADMIN").build();
        return new InMemoryUserDetailsManager(cliente, admin);
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(
            PasswordEncoder encoder,
            @Value("${auth.clients.postman-secret}") String postmanSecret,
            @Value("${auth.clients.web-frontend-secret}") String webFrontendSecret,
            @Value("${auth.clients.mobile-app-secret}") String mobileAppSecret,
            @Value("${auth.clients.atm-terminal-secret}") String atmTerminalSecret,
            @Value("${auth.clients.bff-web-secret}") String bffWebSecret,
            @Value("${auth.clients.bff-mobile-secret}") String bffMobileSecret,
            @Value("${auth.clients.bff-atm-secret}") String bffAtmSecret) {

        // Cliente "todo en uno" para pruebas con Postman (todos los scopes).
        RegisteredClient postman = machineClient("postman-client", encoder.encode(postmanSecret),
                "banking.read", "banking.write", "web.access", "mobile.access", "atm.operate");

        // Frontends / terminales: cada uno solo puede hablar con SU BFF.
        RegisteredClient webFrontend = machineClient("web-frontend", encoder.encode(webFrontendSecret), "web.access");
        RegisteredClient mobileApp = machineClient("mobile-app", encoder.encode(mobileAppSecret), "mobile.access");
        RegisteredClient atmTerminal = machineClient("atm-terminal", encoder.encode(atmTerminalSecret), "atm.operate");

        // Credenciales con las que cada BFF se autentica ante el core-banking-service.
        RegisteredClient bffWeb = machineClient("bff-web-client", encoder.encode(bffWebSecret), "banking.read");
        RegisteredClient bffMobile = machineClient("bff-mobile-client", encoder.encode(bffMobileSecret), "banking.read");
        RegisteredClient bffAtm = machineClient("bff-atm-client", encoder.encode(bffAtmSecret), "banking.read", "banking.write");

        // Cliente con usuario final (Authorization Code + refresh token), callback de Postman.
        RegisteredClient postmanUser = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("postman-user-client")
                .clientSecret(encoder.encode(postmanSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("https://oauth.pstmn.io/v1/callback")
                .scope(OidcScopes.OPENID)
                .scope("banking.read").scope("web.access").scope("mobile.access")
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .tokenSettings(tokenSettings())
                .build();

        return new InMemoryRegisteredClientRepository(
                List.of(postman, webFrontend, mobileApp, atmTerminal, bffWeb, bffMobile, bffAtm, postmanUser));
    }

    private RegisteredClient machineClient(String clientId, String encodedSecret, String... scopes) {
        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(encodedSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenSettings(tokenSettings());
        for (String scope : scopes) {
            builder.scope(scope);
        }
        return builder.build();
    }

    private TokenSettings tokenSettings() {
        return TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(30))
                .refreshTokenTimeToLive(Duration.ofHours(8))
                .build();
    }

    /** Par de llaves RSA generado al arrancar; la clave publica se publica en /oauth2/jwks. */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el par de llaves RSA", ex);
        }
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(@Value("${auth.issuer}") String issuer) {
        return AuthorizationServerSettings.builder().issuer(issuer).build();
    }
}
