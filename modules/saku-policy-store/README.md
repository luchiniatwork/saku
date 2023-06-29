# Saku Policy Store

## Usage

``` shell
$ docker run -p 8080:8080 \
    -e SAKU_PORT=8080 \
    -e SAKU_DB_URL=xxx \
    -e SAKU_ENV_ID=prod \
    luchiniatwork/saku-policy-store:v23.6.0
```


`SAKU_DB_URL` can be either a volume path where the LLVM DB will be
saved or a DataLevin URL (more info
[here](https://github.com/juji-io/datalevin).)

Once running, the API is at `http://localhost:8080/api`. A convenience
GraphiQL can be found at `http://localhost:8080/ide`
