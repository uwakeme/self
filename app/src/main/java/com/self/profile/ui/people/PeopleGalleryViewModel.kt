package com.self.profile.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.self.profile.data.datastore.SettingsDataStore
import com.self.profile.data.repository.PersonRepository
import com.self.profile.domain.model.ClosenessLevel
import com.self.profile.domain.model.GallerySortRule
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.RelationshipType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 人物图鉴 ViewModel。
 *
 * 阶段 1：搜索 + 分类筛选
 * 阶段 3：4 种排序规则（最近查看 / 最近修改 / 创建时间 / 自定义）
 * 阶段 6：自定义排序下可上下箭头重排（US-017）
 * Phase 3.2：自定义标签筛选 + 亲密度筛选；搜索扩展到城市/MBTI/标签
 */
@HiltViewModel
class PeopleGalleryViewModel @Inject constructor(
    personRepo: PersonRepository,
    private val settingsStore: SettingsDataStore
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow<RelationshipType?>(null)
    val filter: StateFlow<RelationshipType?> = _filter.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _closenessFilter = MutableStateFlow<ClosenessLevel?>(null)
    val closenessFilter: StateFlow<ClosenessLevel?> = _closenessFilter.asStateFlow()

    private val allOthers = personRepo.observeAll().map { it.filter { p -> !p.isSelf } }

    val state: StateFlow<GalleryUiState> = combine(
        allOthers,
        _query,
        _filter,
        settingsStore.settings,
        combine(_selectedTag, _closenessFilter) { tag, closeness -> tag to closeness }
    ) { all, q, type, settings, (tag, closeness) ->
        fun matchesQuery(p: Person): Boolean = q.isBlank() ||
            p.name.contains(q, ignoreCase = true) ||
            p.mbti?.contains(q, ignoreCase = true) == true ||
            p.tags.any { it.contains(q, ignoreCase = true) } ||
            p.personalBackground?.currentCity?.contains(q, ignoreCase = true) == true ||
            p.personalBackground?.hometown?.contains(q, ignoreCase = true) == true

        val filtered = all.filter { p ->
            matchesQuery(p) &&
                (type == null || p.relationshipType == type) &&
                (tag == null || p.tags.contains(tag)) &&
                (closeness == null || p.closenessLevel == closeness)
        }
        val sorted = sortPeople(filtered, settings.gallerySortRule, settings.galleryCustomOrder)

        GalleryUiState(
            people = sorted,
            total = all.size,
            query = q,
            filter = type,
            sortRule = settings.gallerySortRule,
            availableTags = all.flatMap { it.tags }.distinct().sorted(),
            selectedTag = tag,
            closenessFilter = closeness
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GalleryUiState(
            emptyList(), 0, "", null, GallerySortRule.CREATED_AT,
            emptyList(), null, null
        )
    )

    private fun sortPeople(
        people: List<Person>,
        rule: GallerySortRule,
        customOrder: List<String>?
    ): List<Person> = when (rule) {
        GallerySortRule.RECENTLY_VIEWED -> people.sortedByDescending { it.lastViewedAt ?: 0L }
        GallerySortRule.RECENTLY_MODIFIED -> people.sortedByDescending { it.updatedAt }
        GallerySortRule.CREATED_AT -> people.sortedByDescending { it.createdAt }
        GallerySortRule.CUSTOM -> {
            if (customOrder.isNullOrEmpty()) {
                people.sortedByDescending { it.createdAt }
            } else {
                val orderMap = customOrder.withIndex().associate { (i, id) -> id to i }
                val inOrder = people.filter { it.id in orderMap }.sortedBy { orderMap[it.id] }
                val remaining = people.filter { it.id !in orderMap }.sortedByDescending { it.createdAt }
                inOrder + remaining
            }
        }
    }

    fun setQuery(q: String) { _query.value = q }
    fun setFilter(type: RelationshipType?) { _filter.value = type }
    fun setTag(tag: String?) { _selectedTag.value = tag }
    fun setClosenessFilter(level: ClosenessLevel?) { _closenessFilter.value = level }

    fun setSortRule(rule: GallerySortRule) {
        viewModelScope.launch { settingsStore.setGallerySortRule(rule) }
    }

    fun setCustomOrder(ids: List<String>) {
        viewModelScope.launch { settingsStore.setGalleryCustomOrder(ids) }
    }

    /**
     * 自定义排序下，把 [fromIndex] 位置的项移到 [toIndex]。
     * [displayedIds] 是当前 UI 上按 customOrder 排好序的 person id 列表。
     */
    fun moveCustomOrder(displayedIds: List<String>, fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in displayedIds.indices || toIndex !in displayedIds.indices) return
        val newOrder = displayedIds.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        setCustomOrder(newOrder)
    }

    /**
     * 首次切到 CUSTOM 排序时，若 customOrder 为空，用当前所有人物 id 初始化。
     */
    fun seedCustomOrderIfEmpty() {
        viewModelScope.launch {
            val current = settingsStore.settings.first()
            if (current.gallerySortRule == GallerySortRule.CUSTOM && current.galleryCustomOrder.isNullOrEmpty()) {
                val ids = allOthers.first().map { it.id }
                settingsStore.setGalleryCustomOrder(ids)
            }
        }
    }
}

data class GalleryUiState(
    val people: List<Person>,
    val total: Int,
    val query: String,
    val filter: RelationshipType?,
    val sortRule: GallerySortRule,
    val availableTags: List<String> = emptyList(),
    val selectedTag: String? = null,
    val closenessFilter: ClosenessLevel? = null
)
