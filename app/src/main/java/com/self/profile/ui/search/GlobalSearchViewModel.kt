package com.self.profile.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.self.profile.data.repository.PersonRepository
import com.self.profile.domain.model.Person
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 全局搜索命中类别，决定结果分组顺序。
 */
enum class SearchHitKind(val label: String) {
    PERSON("人物"),
    NOTE("随想 / 互动"),
    COMMON("共同点"),
    WISH("愿望"),
    JOURNEY("人生轨迹"),
    ANNIVERSARY("纪念日")
}

/**
 * 一条搜索命中。[person] 是命中所在的人物；非 PERSON 类别展示 [snippet] 摘要。
 */
data class SearchHit(
    val kind: SearchHitKind,
    val person: Person,
    val title: String,
    val snippet: String
)

/**
 * 全局搜索 ViewModel：跨姓名/城市/MBTI/标签/随想笔记/互动记录/共同点/愿望/轨迹/纪念日
 * 内存过滤（个人数据量级无需 FTS），人物列表 Flow 驱动，输入防抖 120ms。
 */
@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    personRepo: PersonRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    @OptIn(FlowPreview::class)
    val results: StateFlow<List<SearchHit>> = combine(
        _query.debounce(120),
        personRepo.observeAll()
    ) { q, people ->
        searchAll(people, q.trim())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun setQuery(q: String) {
        _query.value = q
    }

    companion object {

        /** 纯函数，便于单测 */
        fun searchAll(people: List<Person>, q: String): List<SearchHit> {
            if (q.isBlank()) return emptyList()
            val hits = mutableListOf<SearchHit>()
            people.forEach { p -> collectHits(p, q, hits) }
            return hits.sortedWith(compareBy({ it.kind.ordinal }, { it.person.name }))
        }

        private fun collectHits(p: Person, q: String, out: MutableList<SearchHit>) {
            val bg = p.personalBackground

            // 人物：基础信息 + 标签
            val basicHit = listOfNotNull(
                p.name, p.mbti, p.relationshipType.label,
                bg?.hometown, bg?.currentCity, bg?.university, bg?.major, bg?.job
            ).any { it.contains(q, ignoreCase = true) } ||
                p.tags.any { it.contains(q, ignoreCase = true) }
            if (basicHit) {
                val context = listOfNotNull(
                    bg?.currentCity ?: bg?.hometown,
                    p.mbti,
                    p.tags.firstOrNull()
                ).joinToString(" · ").ifBlank { p.relationshipType.label }
                out += SearchHit(SearchHitKind.PERSON, p, p.name, context)
            }

            // 随想笔记 / 互动记录
            (p.thoughtNotes + p.interactionNotes)
                .filter { it.content.contains(q, ignoreCase = true) }
                .forEach { note ->
                    out += SearchHit(SearchHitKind.NOTE, p, p.name, note.content.abbreviate(50))
                }

            // 共同点
            p.commonGrounds
                .filter { it.content.contains(q, ignoreCase = true) }
                .forEach { g ->
                    out += SearchHit(SearchHitKind.COMMON, p, p.name, g.content.abbreviate(50))
                }

            // 愿望清单
            p.wishList
                .filter { it.content.contains(q, ignoreCase = true) }
                .forEach { w ->
                    out += SearchHit(SearchHitKind.WISH, p, p.name, w.content.abbreviate(50))
                }

            // 人生轨迹
            p.lifeJourney
                .filter { it.title.contains(q, true) || it.description.contains(q, true) || it.year.contains(q) }
                .forEach { j ->
                    out += SearchHit(SearchHitKind.JOURNEY, p, "${j.year} ${j.title}", j.description.abbreviate(50))
                }

            // 纪念日
            p.anniversaries
                .filter { it.title.contains(q, ignoreCase = true) }
                .forEach { a ->
                    out += SearchHit(SearchHitKind.ANNIVERSARY, p, a.title, a.type)
                }
        }

        private fun String.abbreviate(max: Int): String =
            if (length <= max) this else take(max) + "…"
    }
}
