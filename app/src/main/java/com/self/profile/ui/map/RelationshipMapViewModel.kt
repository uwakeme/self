package com.self.profile.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.self.profile.data.repository.PersonRepository
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.RelationshipType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 关系图谱 ViewModel。
 *
 * 阶段 1：只读渲染 Canvas 图谱 + 统计网格
 * 阶段 4：节点可点击跳详情 + 拖拽节点
 */
@HiltViewModel
class RelationshipMapViewModel @Inject constructor(
    personRepo: PersonRepository
) : ViewModel() {

    val state: StateFlow<MapUiState> = personRepo.observeAll()
        .map { all ->
            val others = all.filter { !it.isSelf }
            MapUiState(
                selfPerson = all.firstOrNull { it.isSelf },
                family = others.filter { it.relationshipType == RelationshipType.FAMILY },
                friend = others.filter { it.relationshipType == RelationshipType.FRIEND },
                colleague = others.filter { it.relationshipType == RelationshipType.COLLEAGUE },
                classmate = others.filter { it.relationshipType == RelationshipType.CLASSMATE }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MapUiState()
        )
}

data class MapUiState(
    val selfPerson: Person? = null,
    val family: List<Person> = emptyList(),
    val friend: List<Person> = emptyList(),
    val colleague: List<Person> = emptyList(),
    val classmate: List<Person> = emptyList()
) {
    val total: Int get() = family.size + friend.size + colleague.size + classmate.size
    val all: List<Person> get() = family + friend + colleague + classmate
}
