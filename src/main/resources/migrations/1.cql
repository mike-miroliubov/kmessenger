CREATE KEYSPACE messenger WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 2 };

USE messenger;

CREATE TABLE direct_message (
    from_user text,
    to_user text,
    content text,
    sent_at timestamp,
    PRIMARY KEY ((to_user, from_user), sent_at)
);