-- Course publication (RN03/RN04/RN06): status and the moment it was published (the archive job counts 30 days from it).
ALTER TABLE courses
    ADD COLUMN status ENUM('DRAFT', 'PUBLISHED') NOT NULL DEFAULT 'DRAFT' AFTER description,
    ADD COLUMN published_at TIMESTAMP NULL AFTER status,
    ADD INDEX idx_courses_status_published_at (status, published_at);

-- Lesson description and external content link (YouTube, Vimeo or an LMS; checked at publication, RN06).
ALTER TABLE course_lessons
    ADD COLUMN description TEXT NULL AFTER title,
    ADD COLUMN content_url VARCHAR(2048) NULL AFTER description;
