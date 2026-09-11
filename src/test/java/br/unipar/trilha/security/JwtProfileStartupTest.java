package br.unipar.trilha.security;

import br.unipar.trilha.entities.Usuario;
import br.unipar.trilha.enums.Perfil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProfileStartupTest {

    private static final String SEGREDO_PRODUCAO =
            "PROD_SECRET_CONFIGURED_WITH_MORE_THAN_32_BYTES";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(JwtTestConfiguration.class);

    @Test
    void prodSemJwtSecretNaoInicia() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "JWT_SECRET=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalArgumentException.class)
                            .hasRootCauseMessage("JWT_SECRET deve possuir ao menos 32 bytes.");
                });
    }

    @Test
    void prodComJwtSecretValidoIniciaEUsaChaveConfigurada() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "JWT_SECRET=" + SEGREDO_PRODUCAO)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    JwtTokenProvider provider = context.getBean(JwtTokenProvider.class);
                    Usuario usuario = Usuario.builder()
                            .id(1L)
                            .login("professor")
                            .nome("Professor")
                            .senha("nao-utilizada-no-teste")
                            .perfil(Perfil.PROFESSOR)
                            .build();

                    String token = provider.gerar(usuario);

                    assertThat(provider.valido(token)).isTrue();
                    assertThat(provider.obterLogin(token)).isEqualTo("professor");

                    JwtTokenProvider providerComOutraChave = new JwtTokenProvider(new JwtProperties(
                            "ANOTHER_SECRET_WITH_MORE_THAN_32_BYTES_LONG",
                            480,
                            "unipar-trilha-api"));
                    assertThat(providerComOutraChave.valido(token)).isFalse();
                });
    }

    @Test
    void devContinuaIniciandoComSegredoLocal() {
        contextRunner
                .withPropertyValues("spring.profiles.active=dev")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    JwtProperties properties = context.getBean(JwtProperties.class);
                    assertThat(properties.secret()).isNotBlank();
                    assertThat(properties.secret().getBytes(StandardCharsets.UTF_8))
                            .hasSizeGreaterThanOrEqualTo(32);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    static class JwtTestConfiguration {

        @Bean
        JwtTokenProvider jwtTokenProvider(JwtProperties properties) {
            return new JwtTokenProvider(properties);
        }
    }
}
