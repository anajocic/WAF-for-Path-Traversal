## MySQL
Make sure MySQL server is running: \
1. Check if the container is running: `docker ps`
2. If the container isn't running, check if it exists: `docker ps -a`
3. If the container exists, start it: `docker start container_id`
4. If the container doesn't exist run:
   `docker volume create websec_mysql_data`

   `docker run -d \
     --name websec-mysql56 \
     -p 3308:3306 \
     -e MYSQL_ROOT_PASSWORD=rootpassword \
     -e MYSQL_DATABASE=websec \
     -v websec_mysql_data:/var/lib/mysql \
     mysql:5.6`

## Build websec-api
`mvn clean package`

`java -jar target/web-security.jar`

## Import data
1. `docker exec -it websec-mysql56 mysql -uroot -prootpassword websec`
2. `use websec;`
3. Import data


    INSERT INTO user ( 
    created, 
    updated, 
    email, 
    first_name, 
    last_name, 
    password 
    ) 
    VALUES ( 
    NOW(6), 
    NOW(6), 
    'maja@maja.com', 
    'Maja', 
    'Maja', 
    '$2a$12$u/WmgPhjwQaI4582VG5p3e75fl/FzbOWlaIPQrZDKD685kdEftuAy' 
    ); 

    INSERT INTO user (
    created,
    updated,
    email,
    first_name,
    last_name,
    password
    )
    VALUES (
    NOW(6),
    NOW(6),
    'pera@pera.com',
    'Pera',
    'Peric',
    '$2a$12$3npR28geZxZ7SwS6mi20HOGAvezAVJPAzYyU9bwl7wdHYDiw8Voji'
    );

    INSERT INTO movie ( 
    imdb_score, 
    running_time, 
    year, 
    created, 
    updated, 
    description, 
    director, 
    title 
    ) 
    VALUES 
    (7, 124, 2012, NOW(), NOW(), 'Alien exploration mission', 'Ridley Scott', 'Prometheus'), 
    (8, 117, 1982, NOW(), NOW(), 'A blade runner must pursue and terminate four replicants who stole a ship in space', 'Ridley Scott', 'Blade Runner'), 
    (8, 148, 2010, NOW(), NOW(), 'Dream within a dream', 'Christopher Nolan', 'Inception'); 

    INSERT INTO review (created, movie_title, rating, review_text, updated, user_id) VALUES
    ('2026-03-04 10:00:00', 'Inception', 9.0, 'Odlican film sa zanimljivom pricom.', '2026-03-04 10:00:00', 1),
    ('2026-03-04 10:05:00', 'The Matrix', 8.5, 'Klasik koji vredi pogledati vise puta.', '2026-03-04 10:05:00', 1),

    ('2026-03-04 10:10:00', 'Interstellar', 9.5, 'Vizuelno impresivan i emotivan film.', '2026-03-04 10:10:00', 2),
    ('2026-03-04 10:15:00', 'Gladiator', 8.7, 'Sjajna gluma i epska prica.', '2026-03-04 10:15:00', 2),
    ('2026-03-04 10:20:00', 'The Dark Knight', 9.3, 'Jedan od najboljih superhero filmova.', '2026-03-04 10:20:00', 2);

password for maja@maja: password \
password for pera@pera: password123 \

## Run
`java -jar target/web-security.jar`

## Or run websec-api service
1. Place the websec-api.service in /etc/systemd/system \
2. Run the service \
   `systemctl start websec_api.service`

## Status and Logs

`systemctl start websec_api.service` \
`journalctl -u websec_api -f`
