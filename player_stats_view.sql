CREATE VIEW player_stats AS (
WITH player_points AS (SELECT player_id,
                              SUM(points)                                             AS overall_points,
                              SUM(CASE WHEN class = 'soldier' THEN points ELSE 0 END) AS soldier_points,
                              SUM(CASE WHEN class = 'demoman' THEN points ELSE 0 END) AS demoman_points
                       FROM zone_run_scored
                       GROUP BY player_id)
SELECT player_id,
       overall_points,
       RANK() OVER (ORDER BY overall_points DESC) AS overall_rank,
       soldier_points,
       RANK() OVER (ORDER BY soldier_points DESC) AS soldier_rank,
       demoman_points,
       RANK() OVER (ORDER BY demoman_points DESC) AS demoman_rank
FROM player_points);

DROP VIEW player_stats;