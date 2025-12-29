CREATE DATABASE tempus;

CREATE TABLE zone_info
(
    map_id            int2       NOT NULL,
    zone_type         varchar(6) NOT NULL,
    zone_index        int2       NOT NULL,
    soldier_tier      int2       NOT NULL,
    soldier_rating    int2       NOT NULL,
    soldier_run_count int4 DEFAULT 0,
    demoman_tier      int2       NOT NULL,
    demoman_rating    int2       NOT NULL,
    demoman_run_count int4 DEFAULT 0,
    CONSTRAINT map_id_pk PRIMARY KEY (map_id, zone_type, zone_index),
    CONSTRAINT map_fk FOREIGN KEY (map_id) REFERENCES map (map_id),
    CONSTRAINT soldier_rating_fk FOREIGN KEY (soldier_rating, zone_type) REFERENCES zone_rating (rating, zone_type),
    CONSTRAINT demo_rating_fk FOREIGN KEY (demoman_rating, zone_type) REFERENCES zone_rating (rating, zone_type),
    CONSTRAINT soldier_tier_fk FOREIGN KEY (soldier_tier, zone_type) REFERENCES zone_tier (tier, zone_type),
    CONSTRAINT demoman_tier_fk FOREIGN KEY (demoman_tier, zone_type) REFERENCES zone_tier (tier, zone_type)
);

CREATE TABLE zone_run
(
    map_id         int2       NOT NULL,
    zone_type      varchar(6) NOT NULL,
    zone_index     int2       NOT NULL,
    player_id      int8       NOT NULL,
    class          varchar(7) NOT NULL,
    server_id      int2       NOT NULL,
    time_submitted timestamp  NOT NULL,
    duration       float8     NOT NULL,
    rank           int8,
    CONSTRAINT zone_run_classes CHECK (class = 'soldier' OR class = 'demoman'),
    CONSTRAINT zone_run_pk PRIMARY KEY (map_id, zone_type, zone_index, player_id, class),
    CONSTRAINT zone_info_fk FOREIGN KEY (map_id, zone_type, zone_index) REFERENCES zone_info (map_id, zone_type, zone_index),
    CONSTRAINT player_id_fk FOREIGN KEY (player_id) REFERENCES player_info (player_id),
    CONSTRAINT server_fk FOREIGN KEY (server_id) REFERENCES server (server_id)
);

CREATE TABLE run_group
(
    "group"    varchar(2) NOT NULL,
    multiplier float4,
    CONSTRAINT run_group_pk PRIMARY KEY ("group")
);

CREATE TABLE player_info
(
    player_id    int8        NOT NULL,
    steam_id     varchar(20) NOT NULL,
    name         varchar(64) NOT NULL,
    first_seen   timestamp,
    last_seen    timestamp,
    country_code char(2),
    CONSTRAINT player_pk PRIMARY KEY (player_id),
    CONSTRAINT steam_id_uni UNIQUE (steam_id),
    CONSTRAINT country_fk FOREIGN KEY (country_code) REFERENCES country (country_code)
);

CREATE TABLE map_author
(
    map_id    int2 NOT NULL,
    author_id int8 NOT NULL,
    CONSTRAINT map_author_pk PRIMARY KEY (map_id, author_id),
    CONSTRAINT author_id_fk FOREIGN KEY (author_id) REFERENCES player_info (player_id),
    CONSTRAINT map_fk FOREIGN KEY (map_id) REFERENCES map (map_id)
);

CREATE TABLE zone_tier
(
    tier      int2       NOT NULL,
    zone_type varchar(6) NOT NULL,
    points    float4     NOT NULL,
    CONSTRAINT map_tier_pk PRIMARY KEY (tier, zone_type),
    CONSTRAINT tier_name_fk FOREIGN KEY (tier) REFERENCES tier_name (tier)
);

CREATE TABLE zone_rating
(
    rating    int2       NOT NULL,
    zone_type varchar(6) NOT NULL,
    points    float4     NOT NULL,
    CONSTRAINT rating_pk PRIMARY KEY (rating, zone_type),
    CONSTRAINT rating_range CHECK (rating >= 1 AND rating <= 4)
);

CREATE TABLE tier_name
(
    tier int2 NOT NULL,
    name varchar(32),
    CONSTRAINT map_tier_name_pk PRIMARY KEY (tier),
    CONSTRAINT tier_range CHECK (tier >= 0 AND tier <= 10)
);

CREATE TABLE server
(
    server_id    int2    NOT NULL,
    server_name  varchar(64),
    server_index int2    NOT NULL,
    country_code char(2) NOT NULL,
    CONSTRAINT server_pk PRIMARY KEY (server_id),
    CONSTRAINT country_fk FOREIGN KEY (country_code) REFERENCES country (country_code)
);

CREATE TABLE country
(
    country_code char(2)      NOT NULL,
    name         varchar(128) NOT NULL,
    CONSTRAINT country_pk PRIMARY KEY (country_code)
);

CREATE TABLE map
(
    map_id   int2 NOT NULL,
    map_name varchar(128),
    CONSTRAINT map_pk PRIMARY KEY (map_id),
    CONSTRAINT map_name_uni UNIQUE (map_name)
);

CREATE TABLE rank_info
(
    ranks int4range   NOT NULL,
    title varchar(32) NOT NULL,
    CONSTRAINT rank_info_pk PRIMARY KEY (ranks),
    EXCLUDE USING gist (ranks WITH &&)
);

CREATE OR REPLACE FUNCTION zone_run_increment_run_count()
    RETURNS trigger
    LANGUAGE plpgsql
    VOLATILE LEAKPROOF
    RETURNS NULL ON NULL INPUT
    SECURITY INVOKER
    PARALLEL SAFE
    COST 1
AS
$function$
BEGIN
    IF NEW.class = 'soldier' THEN
        UPDATE zone_info
        SET soldier_run_count = soldier_run_count + 1
        WHERE map_id = NEW.map_id
          AND zone_type = NEW.zone_type
          AND zone_index = NEW.zone_index;
    ELSE
        UPDATE zone_info
        SET demoman_run_count = demoman_run_count + 1
        WHERE map_id = NEW.map_id
          AND zone_type = NEW.zone_type
          AND zone_index = NEW.zone_index;
    END IF;
    return NEW;
END;
$function$;

CREATE OR REPLACE FUNCTION zone_run_decrement_run_count()
    RETURNS trigger
    LANGUAGE plpgsql
    VOLATILE LEAKPROOF
    RETURNS NULL ON NULL INPUT
    SECURITY INVOKER
    PARALLEL SAFE
    COST 1
AS
$function$
BEGIN
    IF OLD.class = 'soldier' THEN
        UPDATE zone_info
        SET soldier_run_count = soldier_run_count - 1
        WHERE map_id = OLD.map_id
          AND zone_type = OLD.zone_type
          AND zone_index = OLD.zone_index;
    ELSE
        UPDATE zone_info
        SET demoman_run_count = demoman_run_count - 1
        WHERE map_id = OLD.map_id
          AND zone_type = OLD.zone_type
          AND zone_index = OLD.zone_index;
    END IF;
    return OLD;
END;
$function$;

create function zone_run_rank() returns trigger
    cost 1
    language plpgsql
as
$$
BEGIN
    -- prevent recursion
    IF current_setting('zone_run_rank.running', true) = '1' THEN
        RETURN NEW;
    END IF;
    PERFORM set_config('zone_run_rank.running', '1', true);

    IF TG_OP = 'DELETE' THEN
        WITH sub as (SELECT player_id,
                            RANK() OVER (
                                PARTITION BY map_id, zone_type, zone_index, class
                                ORDER BY duration
                                ) AS rank
                     FROM zone_run
                     WHERE map_id = OLD.map_id
                       AND zone_type = OLD.zone_type
                       AND zone_index = OLD.zone_index
                       AND class = OLD.class)
        UPDATE zone_run zr
        SET rank = sub.rank
        FROM sub
        WHERE zr.player_id = sub.player_id
          AND zr.map_id = OLD.map_id
          AND zr.zone_type = OLD.zone_type
          AND zr.zone_index = OLD.zone_index
          AND zr.class = OLD.class;
        return OLD;
    ELSE
        WITH sub as (SELECT player_id,
                            RANK() OVER (
                                PARTITION BY map_id, zone_type, zone_index, class
                                ORDER BY duration
                                ) AS rank
                     FROM zone_run
                     WHERE map_id = NEW.map_id
                       AND zone_type = NEW.zone_type
                       AND zone_index = NEW.zone_index
                       AND class = NEW.class)
        UPDATE zone_run zr
        SET rank = sub.rank
        FROM sub
        WHERE zr.player_id = sub.player_id
          AND zr.map_id = NEW.map_id
          AND zr.zone_type = NEW.zone_type
          AND zr.zone_index = NEW.zone_index
          AND zr.class = NEW.class;
        PERFORM set_config('zone_run_rank.running', '0', true);
        RETURN NEW;
    END IF;
END;
$$;

CREATE OR REPLACE TRIGGER on_insert
    AFTER INSERT
    ON zone_run
    FOR EACH ROW
EXECUTE PROCEDURE zone_run_increment_run_count();

CREATE OR REPLACE TRIGGER on_delete
    AFTER DELETE
    ON zone_run
    FOR EACH ROW
EXECUTE PROCEDURE zone_run_decrement_run_count();

CREATE OR REPLACE TRIGGER on_modify_update_rank
    AFTER INSERT OR UPDATE OR DELETE
    ON zone_run
    FOR EACH ROW
EXECUTE PROCEDURE zone_run_rank();

-- only manually created index, others are primary keys
CREATE INDEX zone_run_idx ON zone_run USING BTREE (map_id, zone_type, zone_index, class, duration);
