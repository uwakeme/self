package com.self.profile.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.self.profile.data.repository.PersonRepository
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.RelationshipType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 添加人物 ViewModel。
 *
 * 阶段 3：必填校验 + 保存 + 返回人物图鉴。
 */
@HiltViewModel
class AddPersonViewModel @Inject constructor(
    private val personRepo: PersonRepository
) : ViewModel() {

    private val _form = MutableStateFlow(AddPersonForm())
    val form: StateFlow<AddPersonForm> = _form.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    fun setName(name: String) {
        _form.value = _form.value.copy(name = name)
    }

    fun setRelationship(type: RelationshipType) {
        _form.value = _form.value.copy(relationshipType = type)
    }

    fun setMbti(mbti: String?) {
        _form.value = _form.value.copy(mbti = mbti)
    }

    fun setAvatarColor(color: String) {
        _form.value = _form.value.copy(avatarColor = color)
    }

    fun setCloseness(level: com.self.profile.domain.model.ClosenessLevel) {
        _form.value = _form.value.copy(closenessLevel = level)
    }

    fun addTag(tag: String) {
        val t = tag.trim()
        if (t.isNotBlank() && t !in _form.value.tags) {
            _form.value = _form.value.copy(tags = _form.value.tags + t)
        }
    }

    fun removeTag(tag: String) {
        _form.value = _form.value.copy(tags = _form.value.tags - tag)
    }

    fun isValid(): Boolean = _form.value.name.isNotBlank()

    /**
     * 保存新人物。返回 true 表示成功。
     */
    fun save(onSaved: (Person) -> Unit) {
        if (!isValid() || _saving.value) return
        _saving.value = true
        viewModelScope.launch {
            val f = _form.value
            val now = System.currentTimeMillis()
            val newPerson = Person(
                id = UUID.randomUUID().toString(),
                name = f.name.trim(),
                avatarColor = f.avatarColor,
                avatarInitial = f.name.trim().take(1),
                relationshipType = f.relationshipType,
                mbti = f.mbti,
                closenessLevel = f.closenessLevel,
                isSelf = false,
                tags = f.tags,
                createdAt = now,
                updatedAt = now
            )
            personRepo.upsert(newPerson)
            _saving.value = false
            onSaved(newPerson)
        }
    }
}

data class AddPersonForm(
    val name: String = "",
    val relationshipType: RelationshipType = RelationshipType.FRIEND,
    val mbti: String? = null,
    val avatarColor: String = "#6B8EAE",
    val closenessLevel: com.self.profile.domain.model.ClosenessLevel = com.self.profile.domain.model.ClosenessLevel.MEDIUM,
    val tags: List<String> = emptyList()
)
