# Consumer Service

This will be a web service that will service http requests for the consumer app.

# Exercise 0: Hello World

This basic structure allows you to build a simple application with Ktor that should use an **embedded server** using **netty**. 
The example should have two endpoints (/plainRoute and /htmlRoute) that responds with `Hello World` in plain text and HTML respectively.


## Proposed steps for this exercise:

* Create the simplest Ktor application using an embedded Netty server listening on port 8080
* Using the **Routing** Feature to configure a GET route for the path “/plainTextRoute” that responds with a plain text `Hello World` using `call.respondText`.
* Add a new GET route for the path "htmlRoute"  that responds with `<h1>Hello World</h1>` using `call.respondText`, passing in the content type.   






# Exercise 1: HTML DSL, Typed Routes and Asynchronous Code

This exercise will teach you how to:

* Implement a simple asynchronous interface using **suspend** functions to retrieve a list of food and food details
* And a simple implementation returning constant values and simulating database/API latency creating a non-blocking **delay** when using that interface.

## Proposed steps for this exercise:

* Create an HTML unordererd list using kotlinx.html DSL with food listing in `GET /` route.
* Create a `/food/{name}` route displaying food's summary or a page returning HTTP 404 error code when not found.
* Use `@Location data class` to build and handle routes in a typed way.
* Create a class `DummyFoodService` that implements the `FoodService` interface returning fake results: `return listOf()`, and `return when (name) { ... }`
* Add a delay of 4000 milliseconds to each call in that class.
* Create a route `GET /food/{name}` and using the repository display the name of the food and its summary
* Change things to use @Location for typing food route







# Exercise 2: Autoreload

This exercise will teach you how to:

Work with the autoreload feature using gradle.

## Proposed steps for this exercise:

* Create a simple application using gradle inside a specific package with a single route responding with a text.
* Launch two different terminals and run: `gradle -t compile`, then `gradle run` in the other. 
* Open your route in the browser.
* Change the text response in your code.
* After a few seconds, reload your browser and verify that it has changed.




# Exercise 3: HTML DSL, POST and Sessions

This exercise teaches you how to:

* Use [kotlinx.html](https://github.com/Kotlin/kotlinx.html) to return HTML.
* Parse input from requests (both query fields via GET and body content using POST).
* Use the **session** object to store session information.
* Use authentication feature.

## Proposed steps for this exercise:

* Create a route that responds to a GET request to the path `/login`, returning an HTML form using **kotlinx.html**. The form should contain two input fields: user, password and a submit button with action POST `/login`.
* Create a route that responds to POST `/login` that validates the credentials (simple check such that user === password). If the credentials are correct, then it should add the user to the session page and redirect to `/` where it respond with plain text `Hello $username` where `$username` is the value stored in the session.
* Update POST `/login` route to use authentication feature using `formAuthentication`.


# Exercise 4: Creating a Reverse Proxy

This exercise will teach you how to:

Create a Reverse Proxy that intercepts all requests, uses Ktor's HTTP client to mimic those requests to wikipedia and
sends the retrieved content to the client unmodified or replacing relevant URLs for html content.

## Proposed steps for this exercise:

* Create an interceptor that handles all the requests, get your path and respond the client with the requested path
* Use http client to request `https://en.wikipedia.org/$path` asynchronously, return its contents and propagate at least Content-Type
* Replace page contents if Content-Type is text/html: “https://en.wikipedia.org” -> “/” and later “//en.wikipedia.org” -> “/” or both at once using a regular expression
* Optional: try to pipe the contents to reduce memory usage
