CREATE  KEYSPACE IF NOT EXISTS server
    WITH REPLICATION = {
        'class' : 'SimpleStrategy', 'replication_factor' : 1 };

CREATE TABLE IF NOT EXISTS server.users (
    username       varchar,
    realm          varchar,
    ha1            varchar,
    PRIMARY KEY ((username), realm)
);
