package ch.zli.m223.service;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import ch.zli.m223.model.Entry;
import ch.zli.m223.model.Tag;
import ch.zli.m223.repository.TagRepository;

@ApplicationScoped
public class TagService {
    @Inject
    private TagRepository tagRepository;

    @Transactional
    public Tag createTag(Tag tag) {
        tagRepository.persist(tag);
        return tag;
    }

    @Transactional
    public boolean deleteTag(Long id) {
        Tag tag = tagRepository.findById(id);
        if (tag == null) {
            return false;
        }

        // Verknüpfungen in der Zwischentabelle entfernen (Entry ist Besitzer)
        if (tag.getEntries() != null) {
            for (Entry entry : tag.getEntries()) {
                entry.getTags().remove(tag);
            }
        }

        tagRepository.delete(tag);
        return true;
    }

    @Transactional
    public Tag updateTag(Long id, Tag tag) {
        Tag existing = tagRepository.findById(id);
        if (existing == null) {
            return null;
        }

        existing.setTitle(tag.getTitle());
        return existing;
    }

    public List<Tag> findAll() {
        return tagRepository.listAll();
    }
}
