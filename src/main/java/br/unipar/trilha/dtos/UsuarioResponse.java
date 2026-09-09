package br.unipar.trilha.dtos;

import br.unipar.trilha.enums.Perfil;

public record UsuarioResponse(Long id, String login, String nome, Perfil perfil, Boolean ativo) {
}
