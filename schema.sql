CREATE DATABASE IF NOT EXISTS imdb;
USE imdb;

CREATE TABLE IF NOT EXISTS movies
(
    id           INT UNSIGNED PRIMARY KEY,
    popularity   FLOAT        NOT NULL,
    vote_average FLOAT        NOT NULL,
    vote_count   INT          NOT NULL,
    title        VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS movie_genres
(
    movie_id INT UNSIGNED,
    genre_id INT UNSIGNED,
    PRIMARY KEY (movie_id, genre_id),
    FOREIGN KEY (movie_id) REFERENCES movies (id)
);

CREATE TABLE IF NOT EXISTS movie_keywords
(
    movie_id INT UNSIGNED,
    keyword_id INT UNSIGNED,
    PRIMARY KEY (movie_id, keyword_id),
    FOREIGN KEY (movie_id) REFERENCES movies (id)
);

CREATE TABLE IF NOT EXISTS movie_directors
(
    movie_id INT UNSIGNED PRIMARY KEY,
    director_id INT UNSIGNED,
    FOREIGN KEY (movie_id) REFERENCES movies (id)
);

CREATE TABLE IF NOT EXISTS movie_actors
(
    movie_id INT UNSIGNED,
    actor_id INT UNSIGNED,
    PRIMARY KEY (movie_id, actor_id),
    FOREIGN KEY (movie_id) REFERENCES movies (id)
);