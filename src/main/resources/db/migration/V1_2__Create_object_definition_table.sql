create table ObjectDefinition (
    id int unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    object_type varchar(255) not null,
    object_name varchar(255) not null
);