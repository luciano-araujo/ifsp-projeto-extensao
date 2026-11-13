package br.edu.ifsp.tcc.application.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Link between a course lesson and a material (unique per pair). */
@Entity
@Table(name = "course_lesson_materials")
public class LessonMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private CourseLesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CourseLesson getLesson() { return lesson; }
    public void setLesson(CourseLesson lesson) { this.lesson = lesson; }

    public Long getLessonId() { return lesson != null ? lesson.getId() : null; }

    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
