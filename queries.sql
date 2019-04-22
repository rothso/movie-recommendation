USE imdb;

SET @ID = (SELECT id
           FROM movies
           WHERE LOWER(title) LIKE '%The Avengers%'
           LIMIT 1);

SELECT title
FROM (SELECT t1.movie_id, title, POWER(vote_average, 2) * vote_count as score
      FROM (SELECT id, title, popularity, vote_average, vote_count FROM movies WHERE id != @ID) t0
             LEFT JOIN
           (SELECT movie_id, COUNT(*) as genre_similarity
            FROM movie_genres
            WHERE genre_id IN (
              SELECT genre_id
              FROM movie_genres
              WHERE movie_id = @ID)
            GROUP BY movie_id) t1 on t0.id = t1.movie_id
             LEFT JOIN
           (SELECT movie_id, COUNT(*) as keyword_similarity
            FROM movie_keywords
            WHERE keyword_id IN (
              SELECT keyword_id
              FROM movie_keywords
              WHERE movie_id = @ID
                AND keyword_id IN (
                SELECT keyword_id
                FROM movie_keywords
                GROUP BY keyword_id
                HAVING COUNT(*) > 5))
            GROUP BY movie_id) t2 ON t0.id = t2.movie_id
             LEFT JOIN
           (SELECT movie_id, COUNT(*) as director_similarity
            FROM movie_directors
            WHERE director_id IN (
              SELECT director_id
              FROM movie_directors
              WHERE movie_id = @ID)
            GROUP BY movie_id) t3 ON t0.id = t3.movie_id
             LEFT JOIN
           (SELECT movie_id, COUNT(*) as actor_similarity
            FROM movie_actors
            WHERE actor_id IN (
              SELECT actor_id
              FROM movie_genres
              WHERE movie_id = @ID)
            GROUP BY movie_id) t4 ON t0.id = t4.movie_id
      ORDER BY genre_similarity DESC,
               keyword_similarity DESC,
               director_similarity DESC,
               actor_similarity DESC,
               popularity DESC
      LIMIT 7) t1
ORDER BY score DESC
LIMIT 5;