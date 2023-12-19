create table events
(
    game_id uuid not null,
    version integer,
    date_created timestamp not null default now(),
    action jsonb,
    PRIMARY KEY (game_id, version)
)