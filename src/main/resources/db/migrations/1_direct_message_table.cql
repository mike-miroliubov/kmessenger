CREATE TABLE direct_message (
    from_user text,
    to_user text,
    content text,
    id timeuuid,
    PRIMARY KEY ((to_user, from_user), id)
) WITH CLUSTERING ORDER BY (id DESC);