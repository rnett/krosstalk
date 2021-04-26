# Krosstalk Tests

Tests for Krosstalk. These also function as good examples.

There are currently three:

* A Krosstalk client and a normal server - [`client-test`](./client-test)
* A Krosstalk client and server using `expect-actual` - [`fullstack-test`](./fullstack-test)
* A test using `expect-actual` with Jetbrains Compose for Desktop - [`compose-test`](./compose-test)
* A test with two microservices, each publishing a client and server, where each service's server
  depends on the other's client - [`microservices-test`](./microservices-test)

The compose test is currently non-functional due
to [this issue](https://issuetracker.google.com/issues/185609826). As a workaround, you can extract
the Krosstalk functionality to a seperate module and then depend on it.