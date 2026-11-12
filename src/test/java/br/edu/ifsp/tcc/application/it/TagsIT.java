package br.edu.ifsp.tcc.application.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** Team tags CRUD (contract 7.7, decision of 2026-11-12: only ADMIN manages). Plan ids: IT-M1-TAG-xx (new). */
class TagsIT extends AbstractIT {

    @Test
    @DisplayName("IT-TAG-01 Every member lists the 5 default tags of a new team, with colors")
    void defaultTags() {
        Team team = newTeam();
        for (Person p : List.of(ana, carlos, rui)) {
            Resp r = api.get(team.path() + "/tags", p.token());
            assertEquals(200, r.status(), r.body());
            assertEquals(5, r.size());
            List<String> names = new ArrayList<>();
            r.json().forEach(t -> {
                names.add(t.path("name").asString());
                assertTrue(t.path("color").asString().matches("#[0-9A-F]{6}"), "color " + t.path("color"));
                assertTrue(t.path("id").asLong() > 0);
            });
            assertEquals(List.of("Tutorial", "Podcast", "Curso", "Roteiro", "Artigo"), names);
        }
    }

    @Test
    @DisplayName("IT-TAG-02 Admin creates a tag (name is trimmed), it shows in the list")
    void adminCreates() {
        Team team = newTeam();
        Resp r = api.post(team.path() + "/tags", ana.token(), map("name", "  Entrevista  ", "color", "#00AABB"));
        assertEquals(200, r.status(), r.body());
        assertEquals("Entrevista", r.text("name"));
        assertEquals("#00AABB", r.text("color"));
        assertEquals(6, api.get(team.path() + "/tags", rui.token()).size());
    }

    @Test
    @DisplayName("SEC-TAG-03 Creator and reviewer cannot create, rename or delete tags (403)")
    void onlyAdminManages() {
        Team team = newTeam();
        long tagId = team.tagIds().get(0);
        for (Person p : List.of(carlos, rui)) {
            assertEquals(403, api.post(team.path() + "/tags", p.token(), map("name", "X" + p.id(), "color", "#112233")).status());
            assertEquals(403, api.patch(team.path() + "/tags/" + tagId, p.token(), map("name", "Y")).status());
            assertEquals(403, api.delete(team.path() + "/tags/" + tagId, p.token()).status());
        }
        assertEquals(5, api.get(team.path() + "/tags", ana.token()).size());
    }

    @Test
    @DisplayName("IT-TAG-04 Duplicate name is 409 on create and rename (case, accent and spaces ignored)")
    void duplicateIs409() {
        Team team = newTeam();
        for (String dup : List.of("Tutorial", "tutorial", "TUTORIAL", "  Tutorial ", "Tutoríal")) {
            Resp r = api.post(team.path() + "/tags", ana.token(), map("name", dup, "color", "#112233"));
            assertEquals(409, r.status(), "create '" + dup + "': " + r.body());
        }
        Resp rename = api.patch(team.path() + "/tags/" + team.tagIds().get(1), ana.token(), map("name", "tutorial"));
        assertEquals(409, rename.status(), rename.body());
        assertEquals(5, api.get(team.path() + "/tags", ana.token()).size());
    }

    @Test
    @DisplayName("IT-TAG-05 The same name may exist in two different teams")
    void sameNameOtherTeam() {
        Team team = newTeam();
        Resp r = api.post(team.path() + "/tags", ana.token(), map("name", "Exclusiva", "color", "#112233"));
        assertEquals(200, r.status());
        Resp other = api.post(otherTeam.path() + "/tags", bia.token(), map("name", "Exclusiva", "color", "#112233"));
        assertEquals(200, other.status(), other.body());
    }

    @Test
    @DisplayName("IT-TAG-06 Validation: bad color, missing fields and long names are 400")
    void validation() {
        Team team = newTeam();
        for (String color : List.of("red", "#12345", "#1234567", "123456", "#GGGGGG", " #112233", "#11223 ")) {
            Resp r = api.post(team.path() + "/tags", ana.token(), map("name", "Cor" + color.hashCode(), "color", color));
            assertEquals(400, r.status(), "color '" + color + "': " + r.body());
        }
        assertEquals(400, api.post(team.path() + "/tags", ana.token(), map("name", "SemCor")).status());
        assertEquals(400, api.post(team.path() + "/tags", ana.token(), map("color", "#112233")).status());
        assertEquals(400, api.post(team.path() + "/tags", ana.token(), map("name", "   ", "color", "#112233")).status());
        assertEquals(400, api.post(team.path() + "/tags", ana.token(), map("name", "x".repeat(41), "color", "#112233")).status());
        assertEquals(200, api.post(team.path() + "/tags", ana.token(), map("name", "x".repeat(40), "color", "#abcdef")).status());
        Resp message = api.post(team.path() + "/tags", ana.token(), map("name", "Ok", "color", "azul"));
        assertEquals("Cor inválida. Use o formato #RRGGBB.", message.message());
    }

    @Test
    @DisplayName("IT-TAG-07 PATCH is partial: name only, color only, empty body keeps everything; blank name is 400")
    void partialUpdate() {
        Team team = newTeam();
        long id = team.tagIds().get(0);
        Resp color = api.patch(team.path() + "/tags/" + id, ana.token(), map("color", "#010203"));
        assertEquals(200, color.status(), color.body());
        assertEquals("Tutorial", color.text("name"));
        assertEquals("#010203", color.text("color"));

        Resp name = api.patch(team.path() + "/tags/" + id, ana.token(), map("name", "Tutoriais"));
        assertEquals("Tutoriais", name.text("name"));
        assertEquals("#010203", name.text("color"));

        Resp empty = api.patch(team.path() + "/tags/" + id, ana.token(), map());
        assertEquals(200, empty.status());
        assertEquals("Tutoriais", empty.text("name"));

        assertEquals(400, api.patch(team.path() + "/tags/" + id, ana.token(), map("name", "   ")).status());
        assertEquals(400, api.patch(team.path() + "/tags/" + id, ana.token(), map("color", "nope")).status());

        // changing only the case of its own name is allowed (not a duplicate of itself)
        Resp own = api.patch(team.path() + "/tags/" + id, ana.token(), map("name", "TUTORIAIS"));
        assertEquals(200, own.status(), own.body());
        assertEquals("TUTORIAIS", own.text("name"));
    }

    @Test
    @DisplayName("IT-TAG-08 Deleting a tag keeps the cards and ideas that used it, without tag")
    void deleteKeepsCards() {
        Team team = newTeam();
        long tagId = team.tagIds().get(2);
        Resp idea = api.post(team.path() + "/ideas", ana.token(), map("title", "Ideia com tag", "tagId", tagId));
        assertEquals(200, idea.status(), idea.body());
        assertEquals(tagId, idea.num("tagId"));
        Resp card = api.post(team.path() + "/cards", ana.token(), map("title", "Card com tag", "tagId", tagId,
                "targetAudience", "Estudantes", "pedagogicalObjective", "Aprender o basico"));
        assertEquals(200, card.status(), card.body());

        assertEquals(204, api.delete(team.path() + "/tags/" + tagId, ana.token()).status());
        assertEquals(4, api.get(team.path() + "/tags", ana.token()).size());

        Resp ideas = api.get(team.path() + "/ideas", ana.token());
        assertEquals(1, ideas.size());
        assertNull(ideas.text("0.tagId"));
        assertNull(ideas.text("0.tag"));
        assertEquals(200, api.get("/api/v1/kanban/" + card.num("id"), ana.token()).status());
        assertEquals(404, api.delete(team.path() + "/tags/" + tagId, ana.token()).status(), "second delete is 404");
    }

    @Test
    @DisplayName("IT-TAG-09 Tag ids of another team are 404, even for an admin; non-members get 403; no token 401")
    void isolation() {
        Team team = newTeam();
        long foreignTag = otherTeam.tagIds().get(0);
        assertEquals(404, api.patch(team.path() + "/tags/" + foreignTag, ana.token(), map("name", "Hack")).status());
        assertEquals(404, api.delete(team.path() + "/tags/" + foreignTag, ana.token()).status());
        assertEquals("Tutorial", api.get(otherTeam.path() + "/tags", bia.token()).text("0.name"), "foreign tag untouched");

        assertEquals(403, api.get(team.path() + "/tags", bia.token()).status());
        assertEquals(403, api.post(team.path() + "/tags", bia.token(), map("name", "Z", "color", "#112233")).status());
        assertEquals(403, api.get(team.path() + "/tags", loner.token()).status());
        assertEquals(401, api.get(team.path() + "/tags", null).status());
        assertEquals(401, api.post(team.path() + "/tags", null, map("name", "Z", "color", "#112233")).status());
        assertEquals(401, api.patch(team.path() + "/tags/1", null, map("name", "Z")).status());
        assertEquals(401, api.delete(team.path() + "/tags/1", null).status());
    }

    @Test
    @DisplayName("IT-TAG-10 Concurrent creation of the same tag never creates two (one 200, others 409)")
    void concurrentDuplicates() throws Exception {
        Team team = newTeam();
        var pool = java.util.concurrent.Executors.newFixedThreadPool(6);
        List<java.util.concurrent.Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            results.add(pool.submit(() -> api.post(team.path() + "/tags", ana.token(),
                    map("name", "Corrida", "color", "#112233")).status()));
        }
        int ok = 0, conflict = 0, other = 0;
        for (var f : results) {
            int status = f.get();
            if (status == 200) ok++; else if (status == 409) conflict++; else other++;
        }
        pool.shutdown();
        assertEquals(1, ok, "exactly one creation wins");
        assertEquals(0, other, "no 5xx/400 under a duplicate race");
        assertEquals(5, conflict);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM tags WHERE team_id = ? AND name = 'Corrida'", Integer.class, team.id()));
    }
}
