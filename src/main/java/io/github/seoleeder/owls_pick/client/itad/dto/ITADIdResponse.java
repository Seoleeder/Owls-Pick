package io.github.seoleeder.owls_pick.client.itad.dto;

public record ITADIdResponse(
        boolean found,
        Game game
) {
    public record Game(
            String id
    ){}
}
