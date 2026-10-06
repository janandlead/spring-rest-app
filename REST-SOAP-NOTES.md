# REST, SOAP, HTTP Methods, and Spring Request Mapping

## 1. REST

REST means Representational State Transfer. It is an architectural style for distributed systems. A resource, such as an employee, has an identifier; clients exchange representations of that resource, often JSON in HTTP APIs.

REST constraints include client-server separation, stateless requests, caching, a uniform interface, layered systems, and optional code-on-demand. Stateless means each request contains the context needed to understand it; servers can still store resource data in databases. A uniform interface also includes hypermedia links that guide clients through available actions.

Example resource: `/api/v1/employees/101`.

```json
{"id": 101, "name": "Anand", "department": "Engineering"}
```

Using JSON and HTTP alone does not automatically satisfy all REST constraints. See [Fielding's REST description](https://ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm).

## 2. SOAP and REST comparison

SOAP is an XML messaging protocol/framework. A SOAP message has an Envelope, an optional Header, and a Body. A Fault carries error information. SOAP can use HTTP and other transport bindings. SOAP services commonly use WSDL contracts to describe operations and messages.

```xml
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
  <soap:Body>
    <GetEmployee xmlns="urn:employees">
      <id>101</id>
    </GetEmployee>
  </soap:Body>
</soap:Envelope>
```

This is an illustrative SOAP request body; a real service defines its own operation schema.

| Aspect | REST-style HTTP API | SOAP service |
| --- | --- | --- |
| Foundation | Architectural constraints | Structured messaging protocol |
| Data | JSON, XML, text, binary, etc. | XML envelope |
| Typical interaction | HTTP methods on resources | Defined service operations in messages |
| Example | `GET /api/v1/employees/101` | `GetEmployee` operation |
| Errors | HTTP status and application response | SOAP Fault; HTTP status also matters when using HTTP |
| Contract | May document with OpenAPI | Commonly described with WSDL |

Neither approach is inherently secure: both require appropriate authentication, authorization, and transport protection. SOAP extensions can add message-level security.

Sources: [SOAP messaging framework](https://www.w3.org/TR/soap12/), [SOAP primer](https://www.w3.org/TR/soap12-part0/).

## 3. HTTP methods and usages

Safe means the requested operation is read-only; logging can still occur. Idempotent means repeating the request has the same intended server effect as performing it once, even if responses differ.

| Method | Usage | Safe | Idempotent | Spring MVC mapping |
| --- | --- | --- | --- | --- |
| GET | Read a resource | Yes | Yes | `@GetMapping` |
| HEAD | GET response metadata without its body | Yes | Yes | GET mapping supports HEAD |
| POST | Submit data for processing, often creation | No | No guarantee | `@PostMapping` |
| PUT | Create/replace at a known URI | No | Yes | `@PutMapping` |
| PATCH | Apply a partial modification | No | No guarantee | `@PatchMapping` |
| DELETE | Remove a resource association | No | Yes | `@DeleteMapping` |
| OPTIONS | Discover communication options; used in CORS preflight | Yes | Yes | Usually automatic; explicit `@RequestMapping(method = RequestMethod.OPTIONS)` |
| TRACE | Diagnostic request loopback | Yes | Yes | Usually disabled/restricted; not a CRUD endpoint |
| CONNECT | Establish a tunnel | No | No | Typically handled by proxies |

These are the standard general-purpose methods; HTTP also has extension methods, such as WebDAV's PROPFIND.

Sources: [HTTP semantics](https://www.rfc-editor.org/rfc/rfc9110.html#section-9), [PATCH specification](https://www.rfc-editor.org/rfc/rfc5789.html).

### Employee API design examples

These are proposed teaching examples, not endpoints all implemented in this project.

| Request | Intended behavior | Example success response |
| --- | --- | --- |
| `GET /api/v1/employees` | List employees | `200 OK` with an array |
| `GET /api/v1/employees/101` | Read employee 101 | `200 OK` with an object |
| `POST /api/v1/employees` | Create an employee | `201 Created`, ideally with Location |
| `PUT /api/v1/employees/101` | Replace employee 101 | `200 OK` or `204 No Content`; `201` if created |
| `PATCH /api/v1/employees/101` | Change selected employee fields | `200 OK` or `204 No Content` |
| `DELETE /api/v1/employees/101` | Delete employee 101 | `204 No Content` |
| `HEAD /api/v1/employees/101` | Inspect response headers | No response body |
| `OPTIONS /api/v1/employees` | Inspect allowed methods | Allow header |

PUT example: send the complete writable representation, such as `{"name":"Anand","department":"Engineering"}`.

PATCH example: change only department using a documented patch format. Do not assume every server accepts arbitrary partial JSON. Setting department to a fixed value can be idempotent; incrementing a counter generally is not.

Deleting an already deleted employee may return 404 after the first successful deletion. It remains idempotent because the employee stays deleted.

## 4. GET and @GetMapping

Use GET for retrieval, searches, and status checks. Do not use it to create, update, or delete application data. Prefer path and query parameters for inputs; GET request bodies have no generally defined semantics and are unreliable across clients and intermediaries. GET responses can be cached when HTTP caching rules permit it. See [HTTP GET semantics](https://www.rfc-editor.org/rfc/rfc9110.html#section-9.3.1).

`@GetMapping("/status")` is shorthand for `@RequestMapping(path = "/status", method = RequestMethod.GET)`.

### Existing project routes

The current `EmployeeController` has the class-level prefix `@RequestMapping("/api/v1/employees")`.

| Handler | Request | Input and behavior |
| --- | --- | --- |
| `getMessage(name)` | `GET /api/v1/employees/{name}` | Reads a path variable and returns a greeting |
| `getStatus(name)` | `GET /api/v1/employees/status?name=Durga` | Reads a required query parameter and returns a greeting |
| `getProduct(product)` | `GET /api/v1/employees/product` | Reads a Product from the request body, prints its fields, and echoes it |
| `saveProduct(product)` | `POST /api/v1/employees` | Reads a Product from the request body, prints its fields, and echoes it |

There is currently no GET handler for the base path alone.

```java
@GetMapping("/{name}")
public String getMessage(@PathVariable String name) {
    return "Hello, How are you " + name;
}

@GetMapping("/status")
public String getStatus(@RequestParam String name) {
    return "Hello, How are you " + name;
}
```

`@PathVariable` reads a value from the URL path. `@RequestParam` reads a query parameter. In `getStatus`, `name` is required; omitting it produces a bad-request response. Literal paths such as `/status` and `/product` take precedence over `/{name}`.

`@RestController` writes return values to the response body. These greeting methods return strings; the Product handlers return a DTO that can be converted to JSON. The class prefix combines with the method path.

With the application running on the default port, try:

```powershell
curl.exe -i http://localhost:8080/api/v1/employees/Durga
curl.exe -i "http://localhost:8080/api/v1/employees/status?name=Durga"
```

Both requests return `Hello, How are you Durga`.

### Path variables and query parameters

Example methods to place inside a controller with the same class prefix:

```java
@GetMapping("/{id}")
public String getEmployee(@PathVariable("id") long id) {
    return "Requested employee: " + id;
}

@GetMapping("/search")
public String searchEmployees(
        @RequestParam(name = "department", defaultValue = "all") String department) {
    return "Requested department: " + department;
}
```

Use imports from `org.springframework.web.bind.annotation` for these annotations.

- `/api/v1/employees/101` binds 101 to `id`.
- `/api/v1/employees/search?department=Engineering` binds Engineering to `department`.
- `/api/v1/employees/search` uses the default value all.

These methods illustrate binding only; they do not retrieve database records.

## 5. Multiple @GetMapping methods with the same path

### Invalid example: identical mappings

Inside a controller with `@RequestMapping("/api/v1/employees")`:

```java
@GetMapping("/status")
public String firstStatus() {
    return "First";
}

@GetMapping("/status")
public String secondStatus() {
    return "Second";
}
```

Both register the same GET mapping. Spring cannot register two distinct handlers for an identical mapping.

Normal application startup fails with a root cause of **java.lang.IllegalStateException**, containing **Ambiguous mapping**. During context initialization it is commonly wrapped in **BeanCreationException** for `requestMappingHandlerMapping`. Exact wrapper text depends on the Spring version and configuration.

Illustrative diagnostic, not output captured from this application:

```text
BeanCreationException: Error creating bean with name 'requestMappingHandlerMapping'
Caused by: java.lang.IllegalStateException: Ambiguous mapping ...
```

Different Java method names, return types, or argument types do not disambiguate request mappings. A parameter annotated with `@RequestParam` alone does not add a mapping condition. Moving a handler into another controller does not help if the complete mapping is still identical.

### Fix 1: use different paths

```java
@GetMapping("/status")
public String getStatus() {
    return "Running";
}

@GetMapping("/message")
public String getMessage() {
    return "Welcome";
}
```

The project avoids identical mappings: its GET methods map to `/{name}`, `/status`, and `/product`, while POST maps to the base path.

### Fix 2: same path, explicit distinct query conditions

```java
@GetMapping(path = "/status", params = "view=short")
public String shortStatus() {
    return "UP";
}

@GetMapping(path = "/status", params = "view=full")
public String fullStatus() {
    return "Application is running";
}
```

Call `/api/v1/employees/status?view=short` or `/api/v1/employees/status?view=full`. These mapping conditions distinguish the handlers. This pair requires a supported view value unless you also design a fallback.

### Other valid designs

- Combine duplicate handlers into one method if they represent the same operation.
- Use GET and POST on the same path when the operations genuinely have different HTTP semantics.
- Use explicit `produces` or header conditions for intentionally different representations. Ensure requests can select a unique best handler.

Spring considers the complete request mapping, including path, method, parameters, headers, and media-type conditions. The path alone is not the whole identity. See [Spring mapping conditions](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html).

### Startup ambiguity versus request-time ambiguity

Distinct registered mappings can still match one request equally well. For example, GET `/{id}` and GET `/{name}` overlap: both match `/101`. They can produce an **IllegalStateException** about ambiguous handler methods when a request arrives, rather than an identical-mapping startup failure. Use `/by-id/{id}` and `/by-name/{name}`, or one handler, to express the distinction.

Spring's registration and request-selection checks are visible in [AbstractHandlerMethodMapping source](https://github.com/spring-projects/spring-framework/blob/main/spring-webmvc/src/main/java/org/springframework/web/servlet/handler/AbstractHandlerMethodMapping.java).

Also avoid stacking `@GetMapping` and another mapping annotation on the same Java method. Spring warns and uses only the first mapping found. For multiple paths to one handler, use `@GetMapping({"/status", "/health"})`. See [Spring mapping annotation guidance](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html).

## 6. Product DTO, @RequestBody, and @PostMapping

The current `Product` class in `src/main/java/com/durga/srping_rest_app/dto/Product.java` is a data transfer object (DTO). It carries request and response data through private fields with public getters and setters.

| Field | Java type | Example |
| --- | --- | --- |
| `name` | `String` | `Phone` |
| `model` | `String` | `P100` |
| `price` | `double` | `25000.0` |
| `color` | `String` | `Black` |

`@RequestBody Product product` asks Spring to convert the request body into a Product using a configured message converter. For JSON requests, send `Content-Type: application/json`. Returning the Product from a `@RestController` allows it to be serialized into the response body.

The current POST handler is:

```java
@PostMapping
public Product saveProduct(@RequestBody Product product) {
    System.out.println(
            product.getName() + " " + product.getColor() + " "
            + product.getModel() + " " + product.getPrice());
    return product;
}
```

Despite its name, `saveProduct` only prints the fields and echoes the Product. There is no service, repository, database persistence, generated ID, or validation in this implementation. The normal successful response is `200 OK`; the handler does not explicitly set `201 Created` or a Location header.

With the application running, test it in PowerShell:

```powershell
$productJson = '{"name":"Phone","model":"P100","price":25000.0,"color":"Black"}'
Invoke-RestMethod -Method Post `
    -Uri 'http://localhost:8080/api/v1/employees' `
    -ContentType 'application/json' `
    -Body $productJson
```

The response contains the submitted Product fields; JSON property order is not significant.

### Current GET request-body example

`getProduct` uses `@GetMapping("/product")` with `@RequestBody Product product`. It also prints and echoes the submitted Product. A browser address-bar request supplies no JSON body, so it does not satisfy this handler's required body.

This is a request-binding demonstration. As discussed in section 4, GET bodies have no generally defined HTTP semantics and may be rejected or ignored by clients and intermediaries. For retrieval, prefer GET with a product identifier in the path or filters in query parameters. Use the existing POST endpoint to practice sending a JSON body.

## 7. Quick revision

- REST is an architectural style; SOAP defines XML messaging.
- GET reads, POST submits, PUT replaces, PATCH partially modifies, DELETE removes.
- GET must not perform business-data updates.
- Class-level and method-level paths combine into the endpoint path.
- PathVariable binds path values; RequestParam binds query parameters.
- RequestBody converts a request body into a Java object; a DTO carries the data.
- The current Product POST endpoint echoes input with 200 OK and does not save it.
- Identical GET mappings normally stop startup with an ambiguous-mapping IllegalStateException.
- Distinct paths or explicit, unambiguous mapping conditions resolve the conflict.
