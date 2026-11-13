-- Files attached to project cards (RF06). The bytes live in the file storage (local disk or S3);
-- storage_key is a random UUID, never the name the user gave to the file.
-- archived_at is set by the archive job of RN04 (courses published for 30+ days, Glacier Instant Retrieval).
CREATE TABLE materials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL,
    kanban_item_id BIGINT NOT NULL,
    uploaded_by BIGINT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    archived_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_materials_uuid UNIQUE (uuid),
    CONSTRAINT uk_materials_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_materials_kanban_item FOREIGN KEY (kanban_item_id) REFERENCES kanban_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_materials_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_materials_kanban_item_id (kanban_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
