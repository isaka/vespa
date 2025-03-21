# Logstash Ouput Plugin for Vespa

Plugin for [Logstash](https://github.com/elastic/logstash) to write to [Vespa](https://vespa.ai). Apache 2.0 license.

## Installation

Download and unpack/install Logstash, then:
```
bin/logstash-plugin install logstash-output-vespa_feed
```

## Development
If you're developing the plugin, you'll want to do something like:
```
# build the gem
./gradlew gem
# run tests
./gradlew test
# install it as a Logstash plugin
/opt/logstash/bin/logstash-plugin install /path/to/logstash-output-vespa/logstash-output-vespa_feed-0.7.0.gem
# profit
/opt/logstash/bin/logstash
```
Some more good info about Logstash Java plugins can be found [here](https://www.elastic.co/guide/en/logstash/current/java-output-plugin.html).

It looks like the JVM options from [here](https://github.com/logstash-plugins/.ci/blob/main/dockerjdk17.env)
are useful to make JRuby's `bundle install` work.

### Integration tests
To run integration tests, you'll need to have a Vespa instance running with an app deployed that supports an "id" field. And Logstash installed.

Check out the `integration-test` directory for more information.

```
cd integration-test
./run_tests.sh
```

### Publishing the gem

Note to self: for some reason, `bundle exec rake publish_gem` fails, but `gem push logstash-output-vespa_feed-$VERSION.gem`
does the trick.

## Usage

Some more Logstash config examples can be found [in this blog post](https://blog.vespa.ai/logstash-vespa-tutorials/), but here's one with all the output plugin options:

```
# read stuff
input {
  # if you want to just send stuff to a "message" field from the terminal
  #stdin {}

  file {
    # let's assume we have some data in a CSV file here
    path => "/path/to/data.csv"
    # read the file from the beginning
    start_position => "beginning"
    # on Logstash restart, forget where we left off and start over again
    sincedb_path => "/dev/null"
  }
}

# parse and transform data here
filter {
  csv {
    # how does the CSV file look like?
    separator => ","
    quote_char => '"'

    # if the first line is the header, we'll skip it
    skip_header => true

    # columns of the CSV file. Make sure you have these fields in the Vespa schema
    columns => ["id", "description", ...]
  }

  # remove fields we don't need
  # NOTE: the fields below are added by Logstash by default. You probably *need* this block
  # otherwise Vespa will reject documents complaining that e.g. @timestamp is an unknown field
  mutate {
    remove_field => ["@timestamp", "@version", "event", "host", "log", "message"]
  }
}

# publish to Vespa
output {
  # for debugging. You can have multiple outputs (just as you can have multiple inputs/filters)
  #stdout {}

  vespa_feed { # including defaults here
  
    # Vespa endpoint
    vespa_url => "http://localhost:8080"
    
    # for HTTPS URLS (e.g. Vespa Cloud), you may want to provide a certificate and key for mTLS authentication
    client_cert => "/home/radu/vespa_apps/myapp/security/clients.pem"
    # make sure the key isn't password-protected
    # if it is, you can create a new key without a password like this:
    # openssl rsa -in myapp_key_with_pass.pem -out myapp_key.pem
    client_key => "/home/radu/vespa_apps/myapp_key.pem"

    # or you can use an auth token (for Vespa Cloud)
    auth_token => "vespa_cloud_TOKEN_GOES_HERE"
    
    # namespace could be static or in the %{field} format, picking from a field in the document
    namespace => "no_default_provide_yours"
    # similarly, doc type could be static or in the %{field} format
    document_type => "no_default_provide_yours_from_schema"
    
    # operation can be "put", "update", "remove" or dynamic (in the %{field} format)
    operation => "put"
    
    # add the create=true parameter to the feed request (for update and put operations)
    create => false

    # take the document ID from this field in each row
    # if the field doesn't exist, we generate a UUID
    id_field => "id"

    # remove fields from the document after using them for writing
    remove_id => false          # if set to true, remove the ID field after using it
    remove_namespace => false   # would remove the namespace field (if dynamic)
    remove_document_type => false # same for document type
    remove_operation => false   # and operation

    # how many HTTP/2 connections to keep open
    max_connections => 1
    # number of streams per connection
    max_streams => 128
    # request timeout (seconds) for each write operation
    operation_timeout => 180
    # after this time (seconds), the circuit breaker will be half-open:
    # it will ping the endpoint to see if it's back,
    # then resume sending requests when it's back
    grace_period => 10
    
    # how many times to retry on transient failures
    max_retries => 10

    # if we we exceed the number of retries or if there are intransient errors,
    # like field not in the schema, invalid operation, we can send the document to a dead letter queue

    # you'd need to set this to true, default is false. NOTE: this overrides whatever is in logstash.yml
    enable_dlq => false

    # the path to the dead letter queue. NOTE: the last part of the path is the pipeline ID,
    # if you want to use the dead letter queue input plugin
    dlq_path => "data/dead_letter_queue"

    # max dead letter queue size (bytes)
    max_queue_size => 1073741824
    # max segment size (i.e. file from the dead letter queue - also in bytes)
    max_segment_size => 10485760
    # flush interval (how often to commit the DLQ to disk, in milliseconds)
    flush_interval => 5000
  }
}
```

Then you can start Logstash while pointing to the config file like:
```
bin/logstash -f logstash.conf
```
