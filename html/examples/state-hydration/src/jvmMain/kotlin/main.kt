package com.example

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.jetbrains.compose.web.renderToStringWithState
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@SpringBootApplication
class Application

fun main() {
    runApplication<Application>()
}

@Component
class ApplicationContextHolder(
    private val applicationContext: ApplicationContext
) {
    init {
        ApplicationContextHolder.applicationContext = applicationContext
    }

    companion object {
        lateinit var applicationContext: ApplicationContext
            private set
    }
}

@ComposeController
class IndexController {
    @RequestMapping("/")
    fun index() = renderToStringWithState { TodoPage() }
}

@RestController
@RequestMapping("/todo")
class TodoController(
    private val todoService: TodoService
) {
    @PostMapping
    fun addTodo(@RequestBody todo: CreateTodoDto) {
        todoService.addTodo(todo.title)
    }

    @PutMapping("/{id}/done")
    fun todoDone(@PathVariable id: Long) {
        todoService.completeTodo(id)
    }
}

@Service
@Transactional
class TodoService(private val todoRepository: TodoRepository) {
    fun addTodo(title: String): Todo = todoRepository.save(TodoEntity(title = title)).toDomain()

    fun deleteTodo(id: Long) {
        todoRepository.deleteById(id)
    }

    fun completeTodo(id: Long): Todo? {
        return todoRepository.findByIdOrNull(id)?.apply {
            completed = true
        }?.toDomain()
    }

    fun getTodos(): List<Todo> = todoRepository.findAll().map(TodoEntity::toDomain)
}

interface TodoRepository : JpaRepository<TodoEntity, Long>

@Entity
class TodoEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var title: String,
    var completed: Boolean = false,
) {
    companion object {
        fun fromDomain(todo: Todo) = TodoEntity(todo.id, todo.title, todo.completed)
    }

    fun toDomain() = Todo(id!!, title, completed)

    override fun equals(other: Any?): Boolean {
        return other is Todo && other.id == id
    }

    override fun hashCode(): Int {
        return 13 * id.hashCode()
    }
}

