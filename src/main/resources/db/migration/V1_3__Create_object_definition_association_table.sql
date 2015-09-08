create table object_definition_associations (
    object1_id int unsigned NOT NULL,
    object2_id int unsigned NOT NULL,
    weight decimal NOT NULL,
    experience_count int NOT NULL,
    PRIMARY KEY (`object1_id`,`object2_id`),
  	CONSTRAINT `FK_OBJECT1_ID` FOREIGN KEY (`object1_id`) 
             REFERENCES `ObjectDefinition` (`id`),
  	CONSTRAINT `FK_OBJECT2_ID` FOREIGN KEY (`object2_id`) 
             REFERENCES `ObjectDefinition` (`id`)
);