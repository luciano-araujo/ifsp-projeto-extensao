-- RN05: public identifier (uuid) on the content classes. Existing rows are backfilled before the constraints.
ALTER TABLE kanban_items ADD COLUMN uuid CHAR(36) NULL;
ALTER TABLE scripts ADD COLUMN uuid CHAR(36) NULL;
ALTER TABLE courses ADD COLUMN uuid CHAR(36) NULL;
ALTER TABLE course_modules ADD COLUMN uuid CHAR(36) NULL;
ALTER TABLE course_lessons ADD COLUMN uuid CHAR(36) NULL;

UPDATE kanban_items SET uuid = UUID(), updated_at = updated_at WHERE uuid IS NULL;
UPDATE scripts SET uuid = UUID(), updated_at = updated_at WHERE uuid IS NULL;
UPDATE courses SET uuid = UUID() WHERE uuid IS NULL;
UPDATE course_modules SET uuid = UUID() WHERE uuid IS NULL;
UPDATE course_lessons SET uuid = UUID() WHERE uuid IS NULL;

ALTER TABLE kanban_items MODIFY COLUMN uuid CHAR(36) NOT NULL, ADD CONSTRAINT uk_kanban_items_uuid UNIQUE (uuid);
ALTER TABLE scripts MODIFY COLUMN uuid CHAR(36) NOT NULL, ADD CONSTRAINT uk_scripts_uuid UNIQUE (uuid);
ALTER TABLE courses MODIFY COLUMN uuid CHAR(36) NOT NULL, ADD CONSTRAINT uk_courses_uuid UNIQUE (uuid);
ALTER TABLE course_modules MODIFY COLUMN uuid CHAR(36) NOT NULL, ADD CONSTRAINT uk_course_modules_uuid UNIQUE (uuid);
ALTER TABLE course_lessons MODIFY COLUMN uuid CHAR(36) NOT NULL, ADD CONSTRAINT uk_course_lessons_uuid UNIQUE (uuid);
