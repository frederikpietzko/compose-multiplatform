package org.jetbrains.compose.web.examples.springssr


import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.transaction.Transactional
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.FormMethod
import org.jetbrains.compose.web.attributes.action
import org.jetbrains.compose.web.attributes.method
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.springframework.web.bind.annotation.GetMapping
import org.jetbrains.compose.web.renderToString
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Form
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextInput
import org.jetbrains.compose.web.dom.Ul
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseBody

@SpringBootApplication
class SpringSsrApplication

fun main(args: Array<String>) {
    runApplication<SpringSsrApplication>(*args)
}

@Controller
class HelloController(
    private val todoService: TodoService,
) {
    @ResponseBody
    @GetMapping("/")
    fun index(): String = renderToString {
        Greeter()
    }

    @Composable
    fun Greeter() {
        Div {
            Text("Hello from Compose HTML SSR on Spring Boot!")
        }
    }

    @ResponseBody
    @GetMapping("/todos")
    fun todoView(): String = renderToString {
        TodoView(todoService)
    }

    @PostMapping("/todos")
    fun addTodo(createTodoDto: CreateTodoDto): String {
        todoService.addTodo(createTodoDto.title)
        // redirect to /todo
        return "redirect:/todos"
    }

    @PostMapping("/complete/{id}")
    fun completeTodo(@PathVariable id: Long): String {
        todoService.completeTodo(id)
        return "redirect:/todos"
    }

}

data class CreateTodoDto(val title: String)

@Composable
fun TodoView(todoService: TodoService) {
    AddTodo(todoService)
    TodoList(todoService)
}

@Composable
fun AddTodo(todoService: TodoService) {
    Form(
        attrs = {
            action("/todos")
            method(FormMethod.Post)
        }
    ) {
        TextInput(attrs = {
            placeholder("Add todo")
            name("title")
        })
        Button(attrs = {
            type(ButtonType.Submit)
        }) {
            Text("Add")
        }
    }
}

@Composable
fun TodoList(todoService: TodoService) {
    val todoState by produceState(initialValue = emptyList<Todo>(), todoService) {
        value = todoService.getTodos()
    }

    Ul {
        todoState.forEach { todo ->
            TodoElement(todo)
        }
    }
}

@Composable
fun TodoElement(todo: Todo) {
    Li {
        Form(attrs = {
            action("/complete/${todo.id}")
            method(FormMethod.Post)
            style {
                property("display", "flex")
                property("flex-direction", "row")
                property("gap", "10px")
                property("align-items", "center")
            }
        }) {
            P(
                attrs = {
                    if (todo.completed) {
                        style {
                            property("text-decoration", "line-through")
                        }
                    }
                }
            ) { Text(todo.title) }
            Button(
                attrs = {
                    type(ButtonType.Submit)
                    style {
                        property("max-height", "2em")
                    }
                }
            ) {
                Text("Complete")
            }
        }

    }
}

@Service
@Transactional
class TodoService(val todoRepository: TodoRepository) {
    fun addTodo(title: String): Todo = todoRepository.save(Todo(title = title))

    fun deleteTodo(id: Long) {
        todoRepository.deleteById(id)
    }

    fun completeTodo(id: Long): Todo? {
        return todoRepository.findByIdOrNull(id)?.apply {
            completed = true
        }
    }

    fun getTodos(): List<Todo> = todoRepository.findAll()
}

interface TodoRepository : JpaRepository<Todo, Long>

@Entity
class Todo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var title: String,
    var completed: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        return other is Todo && other.id == id
    }

    override fun hashCode(): Int {
        return 13 * id.hashCode()
    }
}