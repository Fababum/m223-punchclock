package ch.zli.m223.service;

import java.util.HashSet;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import ch.zli.m223.dto.EntryDto;
import ch.zli.m223.dto.TimeSummaryDto;
import ch.zli.m223.model.Entry;
import ch.zli.m223.model.Tag;
import ch.zli.m223.repository.CategoryRepository;
import ch.zli.m223.repository.EntryRepository;
import ch.zli.m223.repository.TagRepository;

@ApplicationScoped
public class EntryService {
    @Inject
    private EntryRepository entryRepository;

    @Inject
    private CategoryRepository categoryRepository;

    @Inject
    private TagRepository tagRepository;

    @Inject
    private TimeSummaryService timeSummaryService;

    @Transactional
    public Entry createEntry(Entry entry) {
        entryRepository.persist(entry);
        return entry;
    }

    @Transactional
    public EntryDto createEntry(EntryDto dto) {
        Entry entry = EntryDto.toEntity(dto);
        applyRelations(entry, dto.getCategoryId(), dto.getTagIds());
        entryRepository.persist(entry);
        return EntryDto.fromEntity(entry);
    }

    @Transactional
    public boolean deleteEntry(Long id) {
        return entryRepository.deleteById(id);
    }

    @Transactional
    public Entry updateEntry(Long id, Entry entry) {
        Entry existing = entryRepository.findById(id);
        if (existing == null) {
            return null;
        }

        existing.setCheckIn(entry.getCheckIn());
        existing.setCheckOut(entry.getCheckOut());
        return existing;
    }

    @Transactional
    public EntryDto updateEntry(Long id, EntryDto dto) {
        Entry existing = entryRepository.findById(id);
        if (existing == null) {
            return null;
        }

        existing.setCheckIn(dto.getCheckIn());
        existing.setCheckOut(dto.getCheckOut());
        applyRelations(existing, dto.getCategoryId(), dto.getTagIds());
        return EntryDto.fromEntity(existing);
    }

    public List<Entry> findAll() {
        return entryRepository.listAll();
    }

    @Transactional
    public List<EntryDto> findAllDtos() {
        return entryRepository.listAll().stream().map(EntryDto::fromEntity).toList();
    }

    public List<TimeSummaryDto> getTimeSummaries() {
        List<Entry> entries = entryRepository.listAll();
        return timeSummaryService.calculateSummaryPerDay(entries);
    }

    private void applyRelations(Entry entry, Long categoryId, List<Long> tagIds) {
        if (categoryId != null) {
            entry.setCategory(categoryRepository.findById(categoryId));
        } else {
            entry.setCategory(null);
        }

        if (tagIds != null && !tagIds.isEmpty()) {
            List<Tag> resolved = tagRepository.list("id in ?1", tagIds);
            entry.setTags(new HashSet<>(resolved));
        } else {
            entry.setTags(new HashSet<>());
        }
    }
}
