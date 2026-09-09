package br.unipar.trilha.dtos;

import br.unipar.trilha.enums.Perfil;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInMinutes,
        Long usuarioId,
        String login,
        String nome,
        Perfil perfil
) {
}
