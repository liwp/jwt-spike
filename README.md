# jwt-spike

## Prerequisites

You will need [Leiningen][1] 1.7.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

### Routes

The spike defines two routes: `/authenticate` and `/protected`.

The `/authenticate` route is used to create a new JWT token with a
`POST` request. The request must include valid basic auth credentials
(username: `foo`, password: `bar`).

The `/protected` route can only the accessed with a `GET` request by
providing a valid JWT token as a bearer token (`Authorization: Bearer
<jwt token>`).

### Test flow

Once the server is running, you should be able to execute the following steps:

`/` is not defined:

```
% http http://localhost:3000/
HTTP/1.1 404 Not Found
Content-Length: 9
Content-Type: text/html;charset=UTF-8
Date: Tue, 02 Sep 2014 12:13:54 GMT
Server: Jetty(7.6.8.v20121106)

Not Found
```

`/authenticate` requires basic auth credentials:

```
% http post http://localhost:3000/authenticate
HTTP/1.1 401 Unauthorized
Content-Length: 13
Content-Type: text/plain;charset=ISO-8859-1
Date: Tue, 02 Sep 2014 12:15:01 GMT
Server: Jetty(7.6.8.v20121106)
WWW-Authenticate: Basic realm="restricted area"

access denied
```

`/protected` requires JWT token:

```
% http get http://localhost:3000/protected
HTTP/1.1 401 Unauthorized
Content-Length: 21
Date: Tue, 02 Sep 2014 12:15:39 GMT
Server: Jetty(7.6.8.v20121106)

access denied (jwt 1)
```

(`(jwt 1)` indicates that authorization failed, because there was no `Authorization` header present)

Let's create a JWT token for us:

```
% http -a foo:bar post http://localhost:3000/authenticate
HTTP/1.1 200 OK
Content-Length: 160
Content-Type: text/html;charset=UTF-8
Date: Tue, 02 Sep 2014 12:16:55 GMT
Server: Jetty(7.6.8.v20121106)

eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhcGkuZXhhbXBsZS5jb20iLCJleHAiOjE0MDk3NDY2MTUsImlhdCI6MTQwOTY2MDIxNX0.OO1g7sOCRtOhCaDS7dO1EQdaDBgbrpg3nmpjHerfLvY
```

And now we can access `/protected` with the returned token:

```
% http get http://localhost:3000/protected "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhcGkuZXhhbXBsZS5jb20iLCJleHAiOjE0MDk3NDY2MTUsImlhdCI6MTQwOTY2MDIxNX0.OO1g7sOCRtOhCaDS7dO1EQdaDBgbrpg3nmpjHerfLvY"
HTTP/1.1 200 OK
Content-Length: 92
Content-Type: text/html;charset=UTF-8
Date: Tue, 02 Sep 2014 12:17:56 GMT
Server: Jetty(7.6.8.v20121106)

Super secret protected area! JWT: {:iss "api.example.com", :exp 1409746615, :iat 1409660215}
```

If we fiddle with the token, our request will fail (the last letter of the token was changed from `Y` to `X`):

```
% http get http://localhost:3000/protected "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhcGkuZXhhbXBsZS5jb20iLCJleHAiOjE0MDk3NDY2MTUsImlhdCI6MTQwOTY2MDIxNX0.OO1g7sOCRtOhCaDS7dO1EQdaDBgbrpg3nmpjHerfLvX"
HTTP/1.1 401 Unauthorized
Content-Length: 21
Date: Tue, 02 Sep 2014 12:18:54 GMT
Server: Jetty(7.6.8.v20121106)

access denied (jwt 2)
```

(`(jwt 2)` here indicates that the JWT token validation failed)

# Some notes

## Secret

The spike uses HMAC and a secret key for signing the tokens. This is
probably the easiest option for us to use: we just need to generate a
secure random secret and put it in an env var. When the time comes to
rotate the key, we can implement the logic for validating the token
with the key that was valid at the time when the token was issued
(`:iat` above).

## Expiration time

In the spike the token is valid for one day after it was issued. I
just did a quick hack with a one minute lifespan and it looks like the
`clj-jwt` token validation function only validated the signature, but
not the expiration time, so we'll need to add some more logic for
that.

I guess a decent expiration time for our web apps would be a week or
two to avoid people having to log in too often.

## License

Copyright © 2014 Listora
