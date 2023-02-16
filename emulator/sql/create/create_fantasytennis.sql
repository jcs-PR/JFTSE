CREATE USER 'jftse'@'localhost' IDENTIFIED BY 'jftse' WITH MAX_QUERIES_PER_HOUR 0 MAX_CONNECTIONS_PER_HOUR 0 MAX_UPDATES_PER_HOUR 0;
GRANT USAGE ON * . * TO 'jftse'@'localhost';
CREATE DATABASE `fantasytennis` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;
GRANT ALL PRIVILEGES ON `fantasytennis` . * TO 'jftse'@'localhost' WITH GRANT OPTION;