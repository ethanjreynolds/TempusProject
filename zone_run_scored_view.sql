CREATE VIEW zone_run_scored AS
WITH zone_run_info AS (SELECT zr.*,
                              CASE
                                  WHEN zr.class::text = 'soldier'::text THEN zi.soldier_tier
                                  ELSE zi.demoman_tier
                                  END              AS tier,
                              CASE
                                  WHEN zr.class::text = 'soldier'::text THEN zi.soldier_rating
                                  ELSE zi.demoman_rating
                                  END              AS rating,
                              LEAST(CASE
                                        WHEN zr.class::text = 'soldier'::text THEN zi.soldier_run_count
                                        ELSE zi.demoman_run_count
                                        END, 1000) AS run_count
                       FROM zone_run zr
                                JOIN zone_info zi ON
                           zi.map_id = zr.map_id AND zi.zone_type = zr.zone_type AND zi.zone_index = zr.zone_index),
     zone_run_grouped AS (SELECT map_id,
                                 zone_type,
                                 zone_index,
                                 player_id,
                                 class,
                                 server_id,
                                 time_submitted,
                                 duration,
                                 rank,
                                 CASE
                                     WHEN rank <= 10 THEN rank::text
                                     WHEN rank > (0.538 * run_count) THEN 'G5'
                                     WHEN rank > (0.205 * run_count) THEN 'G4'
                                     WHEN rank > (0.08 * run_count) THEN 'G3'
                                     WHEN rank > (0.03 * run_count) THEN 'G2'
                                     ELSE 'G1'
                                     END AS "group",
                                 tier,
                                 rating
                          FROM zone_run_info zri)
SELECT zrg.*,
       zt.points + rg.multiplier * zr.points AS points
FROM zone_run_grouped zrg
         JOIN zone_tier zt ON zrg.tier = zt.tier AND zrg.zone_type = zt.zone_type
         JOIN run_group rg ON zrg.group = rg.group
         JOIN zone_rating zr ON zrg.rating = zr.rating AND zrg.zone_type = zr.zone_type;

DROP VIEW zone_run_scored;