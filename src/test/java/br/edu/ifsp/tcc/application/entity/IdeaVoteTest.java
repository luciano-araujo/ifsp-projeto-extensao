package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdeaVoteTest {

    @Test
    void getIds_shouldReturnNullWithoutCardOrUser() {
        IdeaVote vote = new IdeaVote();

        assertNull(vote.getKanbanItemId());
        assertNull(vote.getUserId());
    }

    @Test
    void getIds_shouldReturnCardAndUserIds() {
        IdeaVote vote = new IdeaVote();
        KanbanItem item = new KanbanItem();
        item.setId(5L);
        User user = new User();
        user.setId(1L);
        vote.setKanbanItem(item);
        vote.setUser(user);

        assertEquals(5L, vote.getKanbanItemId());
        assertEquals(1L, vote.getUserId());
        assertSame(item, vote.getKanbanItem());
        assertSame(user, vote.getUser());
    }

    @Test
    void onCreate_shouldSetCreatedAt() {
        IdeaVote vote = new IdeaVote();

        vote.onCreate();

        assertNotNull(vote.getCreatedAt());
    }
}
