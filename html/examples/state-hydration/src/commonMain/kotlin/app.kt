/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.example

import androidx.compose.runtime.*
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.state.rememberHydratableState


val client = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}

expect fun reload()
expect fun initialTodos(): List<Todo>

@Composable
fun TodoPage() {
    AddTodo()
    TodoList()
}

@Composable
fun AddTodo() {
    var title by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    TextInput {
        name("title")
        placeholder("Title")
        value(title)
        onInput { title = it.value }
    }
    Button(attrs = {
        onClick {
            it.preventDefault()
            scope.launch {
                client.post("/todo") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateTodoDto(title))
                }
                reload()
            }
        }
    }) {
        Text("Add")
    }
}

@Composable
fun TodoList() {
    val todos by rememberHydratableState("todos") {
        initialTodos()
    }
    Ul {
        todos.forEach { todo ->
            TodoElement(todo)
        }
    }
}

@Composable
fun TodoElement(todo: Todo) {
    val scope = rememberCoroutineScope()
    Li(
        attrs = {
            style {
                property("display", "flex")
                property("flex-direction", "row")
                property("gap", "10px")
                property("align-items", "center")
            }
        }
    ) {
        P(attrs = {
            if (todo.completed) {
                style {
                    property("text-decoration-line", "line-through")
                }
            }
        }) {
            Text(todo.title)
        }
        Button(attrs = {
            onClick {
                scope.launch {
                    client.put("/todo/${todo.id}/done")
                    reload()
                }

            }
        }) {
            Text("Complete")
        }
    }
}

@Serializable
data class Todo(val id: Long, val title: String, val completed: Boolean)

@Serializable
data class CreateTodoDto(val title: String)
