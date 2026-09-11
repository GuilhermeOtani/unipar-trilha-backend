package br.unipar.trilha;

import br.unipar.trilha.entities.Usuario;
import br.unipar.trilha.enums.Perfil;
import br.unipar.trilha.repositories.UsuarioRepository;
import br.unipar.trilha.security.JwtProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InfraestruturaAutenticacaoUsuariosIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtProperties jwt;
    @Autowired EntityManager entityManager;

    private static final String SENHA = "senha.qa123";
    private final Map<Perfil, Usuario> contas = new EnumMap<>(Perfil.class);

    @BeforeEach
    void prepararUsuarios() {
        for (Perfil perfil : Perfil.values()) {
            contas.put(perfil, usuarios.save(Usuario.builder()
                    .login("qa." + perfil.name()).nome("Pessoa " + perfil.name())
                    .senha(encoder.encode(SENHA)).perfil(perfil).ativo(true).build()));
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void healthRespondeSemAutenticacao() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @ParameterizedTest
    @EnumSource(Perfil.class)
    void cadaPerfilAutenticaEConsultaSuaPropriaIdentidade(Perfil perfil) throws Exception {
        Usuario conta = contas.get(perfil);
        String token = login(perfil);
        var claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwt.secret().getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(token).getPayload();
        assertThat(claims.getSubject()).isEqualTo(conta.getLogin());
        assertThat(claims.getIssuer()).isEqualTo(jwt.issuer());
        assertThat(claims.getExpiration()).isAfter(new Date());
        assertThat(claims.get("perfil", String.class)).isEqualTo(perfil.name());
        assertThat(((Number) claims.get("usuarioId")).longValue()).isEqualTo(conta.getId());

        mockMvc.perform(get("/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conta.getId()))
                .andExpect(jsonPath("$.login").value(conta.getLogin()))
                .andExpect(jsonPath("$.nome").value(conta.getNome()))
                .andExpect(jsonPath("$.perfil").value(perfil.name()))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ausente", "malformado", "assinaturaInvalida", "expirado"})
    void credencialInvalidaRetorna401(String caso) throws Exception {
        var request = get("/usuarios/me");
        if (!caso.equals("ausente")) {
            String token = switch (caso) {
                case "malformado" -> "isto-nao-e-um-jwt";
                case "assinaturaInvalida" -> tokenComPrazo(
                        "OUTRA_CHAVE_EXCLUSIVA_DE_TESTE_COM_32_BYTES", Instant.now().plusSeconds(600));
                case "expirado" -> tokenComPrazo(jwt.secret(), Instant.now().minusSeconds(600));
                default -> throw new IllegalArgumentException(caso);
            };
            if (caso.equals("expirado")) {
                // Prova que a assinatura é válida e a rejeição é causada pela expiração.
                assertThatThrownBy(() -> Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(jwt.secret().getBytes(StandardCharsets.UTF_8)))
                        .build().parseSignedClaims(token)).isInstanceOf(ExpiredJwtException.class);
            }
            request.header("Authorization", "Bearer " + token);
        }
        problem(mockMvc.perform(request), 401);
    }

    @ParameterizedTest
    @EnumSource(Perfil.class)
    void administradorCriaUsuarioEListaSomentePerfilSolicitado(Perfil perfilCriado) throws Exception {
        String token = login(Perfil.ADMINISTRADOR);
        String resposta = mockMvc.perform(post("/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(novoUsuario(perfilCriado)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value(perfilCriado.name()))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(resposta).get("id").asLong();
        entityManager.flush();
        entityManager.clear();
        Usuario salvo = usuarios.findById(id).orElseThrow();
        assertThat(salvo.getLogin()).isEqualTo("novo.qa");
        assertThat(salvo.getSenha()).isNotEqualTo(SENHA);
        assertThat(encoder.matches(SENHA, salvo.getSenha())).isTrue();

        String lista = mockMvc.perform(get("/usuarios").param("perfil", perfilCriado.name())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        for (JsonNode item : objectMapper.readTree(lista)) {
            assertThat(item.get("perfil").asText()).isEqualTo(perfilCriado.name());
            assertThat(item.has("senha")).isFalse();
        }
        assertThat(lista).contains("novo.qa");
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = {"PROFESSOR", "ALUNO"})
    void outrosPerfisNaoCriamNemListamUsuarios(Perfil perfil) throws Exception {
        String token = login(perfil);
        long antes = usuarios.count();
        problem(mockMvc.perform(post("/usuarios")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(novoUsuario(Perfil.ADMINISTRADOR))), 403);
        problem(mockMvc.perform(get("/usuarios").param("perfil", "ALUNO")
                .header("Authorization", "Bearer " + token)), 403);
        assertThat(usuarios.count()).isEqualTo(antes);
        assertThat(usuarios.existsByLogin("novo.qa")).isFalse();
    }

    @Test
    void anonimoNaoCriaNemListaUsuarios() throws Exception {
        long antes = usuarios.count();
        problem(mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content(novoUsuario(Perfil.ALUNO))), 401);
        problem(mockMvc.perform(get("/usuarios").param("perfil", "ALUNO")), 401);
        assertThat(usuarios.count()).isEqualTo(antes);
    }

    @Test
    void loginDuplicadoRetorna409SemAlterarUsuarioExistente() throws Exception {
        String token = login(Perfil.ADMINISTRADOR);
        String body = novoUsuario(Perfil.ALUNO);
        mockMvc.perform(post("/usuarios").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        entityManager.flush();
        entityManager.clear();
        Usuario original = usuarios.findByLogin("novo.qa").orElseThrow();
        long quantidade = usuarios.count();

        problem(mockMvc.perform(post("/usuarios").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(novoUsuario(Perfil.ADMINISTRADOR))), 409)
                .andExpect(jsonPath("$.detail").value("Já existe um usuário com este login."));
        entityManager.clear();
        Usuario depois = usuarios.findByLogin("novo.qa").orElseThrow();
        assertThat(usuarios.count()).isEqualTo(quantidade);
        assertThat(depois.getId()).isEqualTo(original.getId());
        assertThat(depois.getPerfil()).isEqualTo(Perfil.ALUNO);
        assertThat(depois.getSenha()).isEqualTo(original.getSenha());
    }

    private String login(Perfil perfil) throws Exception {
        Usuario conta = contas.get(perfil);
        String json = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("login", conta.getLogin(), "senha", SENHA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(conta.getId()))
                .andExpect(jsonPath("$.perfil").value(perfil.name()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInMinutes").value(jwt.expirationMinutes()))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("accessToken").asText();
    }

    private String novoUsuario(Perfil perfil) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "login", "novo.qa", "nome", "Novo usuário", "senha", SENHA, "perfil", perfil.name()));
    }

    private String tokenComPrazo(String segredo, Instant expiracao) {
        Usuario conta = contas.get(Perfil.ALUNO);
        return Jwts.builder().subject(conta.getLogin()).issuer(jwt.issuer())
                .issuedAt(Date.from(Instant.now().minusSeconds(1200)))
                .expiration(Date.from(expiracao))
                .claim("usuarioId", conta.getId()).claim("perfil", conta.getPerfil().name())
                .signWith(Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private ResultActions problem(ResultActions result, int status) throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }
}
