# Consumer Service

This will be a web service that will service http requests for the consumer app.

# Exercise 0: Hello World

This basic structure allows you to build a simple application with Ktor that should use an **embedded server** using **netty**. 
The example should have two endpoints (/plainRoute and /htmlRoute) that responds with `Hello World` in plain text and HTML respectively.


## Proposed steps for this exercise:

* Create the simplest Ktor application using an embedded Netty server listening on port 8080
* Using the **Routing** Feature to configure a GET route for the path “/plainTextRoute” that responds with a plain text `Hello World` using `call.respondText`.
* Add a new GET route for the path "htmlRoute"  that responds with `<h1>Hello World</h1>` using `call.respondText`, passing in the content type.   






# Exercise 2: HTML DSL, Typed Routes and Asynchronous Code

This exercise will teach you how to:

* Create a simple asynchronous interface using **suspend** functions to retrieve a list of movies and a movie summaries
* And a simple implementation returning constant values and simulating database/API latency creating a non-blocking **delay** when using that interface.

## Proposed steps for this exercise:

* Create an HTML unordererd list using kotlinx.html DSL with movie listing in `GET /` route.
* Create a `/movie/{name}` route displaying movie's summary or a page returning HTTP 404 error code when not found.
* Use `@Location data class` to build and handle routes in a typed way.
* Create `interface MovieRepository { suspend fun getMovieNames(): List<String>; suspend fun getMovieSummary(name: String): String }`
* Create a class that implements that interface returning fake results: `return listOf()`, and `return when (name) { ... }`
* Add a delay of 4000 milliseconds to each call in that class or in a separate class DelayedMovieRepository(val original: MovieRepository) : MovieRepository
* Instantiate the implementation with the delay in your main and propagate it to your GET / route, get movie list into a local and then iterate over it to generate a `<ul><li><a href=”/movie/$name”>$name</a></li>...</ul>`
* Create a route `GET /movie/{name}` and using the repository display the name of the movie and its summary
* Change things to use @Location for typing movie route
