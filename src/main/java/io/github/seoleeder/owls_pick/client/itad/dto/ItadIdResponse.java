package io.github.seoleeder.owls_pick.client.itad.dto;

public record ItadIdResponse(
        boolean found,
        Game game
) {
    public record Game(
            String id
    ){}
}
