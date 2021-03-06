### Introduction

This load test framework, known as Flic (Framework of load & integration for
Cloud Pub/Sub), for Cloud Pub/Sub is a tool targeted for developers and
companies who wish to benchmark Google Cloud Pub/Sub and Apache Kafka.

The goal of this framework is twofold:

1.  Provide users with a tool that allows them to see how Cloud Pub/Sub performs
    under various conditions.

2.  Provide users with a tool that allows them to benchmark their own Kafka
    configuration.

### Building

These instructions assume you are using [Maven](https://maven.apache.org/).

1.  Make the jar that contains the connector:

    `mvn package`

2. Copy the jar into the GCE resource directory:

    `cp target/driver.jar src/main/resources/gce/`

The resulting jar is at target/driver.jar.

### Pre-Running Steps

1.  Regardless of whether you are running on Google Cloud Platform or not, you
    need to create a project and create a service key that allows you access to
    the Google Cloud Pub/Sub, Storage, and Monitoring APIs.

2.  Create project on Google Cloud Platform. By default, this project will have
    multiple service accounts associated with it (see "IAM & Admin" within GCP
    console). Within this section, find the tab for "Service Accounts". Create a
    new service account and make sure to select "Furnish a new private key".
    Doing this will create the service account and download a private key file
    to your local machine.

3.  Go to the "IAM" tab, find the service account you just created and click on
    the dropdown menu named "Role(s)". Under the "Pub/Sub" submenu, select
    "Pub/Sub Admin".

    If you don't see the service account in the list, add a new permission, use
    the service account as the member name, and select "Pub/Sub Admin" from the
    role dropdown menu in the window.

    Now, the service account you just created should appear in the members list
    on the IAM page with the role Pub/Sub Admin. If the member name is gray,
    don't worry. It will take a few minutes for the account's new permissions to
    make their way through the system.

    Finally, the key file that was downloaded to your machine
    needs to be placed on the machine running the framework. An environment
    variable named GOOGLE_APPLICATION_CREDENTIALS must point to this file. (Tip:
    export this environment variable as part of your shell startup file).

    `export GOOGLE_APPLICATION_CREDENTIALS=/path/to/key/file`

### Important Notes

There are important differences in the clients used for Cloud Pub/Sub and Kafka
in this framework. For Cloud Pub/Sub, we implemented a reasonably optimized
client with parallelization of requests across multiple threads, asynchronous
callback threads to increase client throughput, batching, and rate limitation.
For Kafka, we used the Producer API released by Apache, which includes some of
those same features or rate limits, batching, and asynchronous callback threads,
but of course they're implemented slightly differently to better integrate with
Kafka brokers.

We took care to make sure the data recording was as similar as possible. Both clients
measure total latency as the time a message is added to a batch to be sent until its
callback method is called. Although exactly what load the service sees might differ
slightly, the record of how it responds, its latency and throughput, are equivalent.

### Quickstart

1.  The jar file can be executed with numerous commands and options specified
    from the command line. In a single invocation, the framework allows you to
    use Kafka or Cloud Pub/Sub as the message service, and to publish or consume
    messages from this service. The following command runs a load test for Cloud
    Pub/Sub.

    `java -jar target/driver.jar --project your_project`

2.  To get a list of all available commands, options and defaults, run the
    following.

    `java -jar target/driver.jar --help`

### Common Tests

#### Minimum Latency on a single instance

```bash
--request_rate=1 --message_size=1
```

#### Maximum Throughput on a single instance

```bash
-—message_size=10000 —-batch_size=10 --request_rate=1000000
```

#### Maximum QPS on a single instance

```bash
—-message_size=1 --request_rate=1000000
```

#### Maximum Service Throughput for N instances under 200 ms Publish to Ack Latency

```bash
—-cps_publisher_count=N —-cps_subscriber_count=N*3 --max_publish_latency_test=true --max_publish_latency_millis=200 --request_rate=100
```

### Testing your own Client Libraries

#### Java 
To add your own client libraries, you can create a new package in com.google.pubsub.clients and add your implementations of `com.google.pubsub.clients.common.Task`. You can see examples in the other packages. The important piece is that you need to override the run function that executes a single publish or pull, and records the latencies of that operation.

Then, you need to add your client to the enum in com.google.pubsub.flic.Client.ClientType, and create a new command line flag to set the count of this type of task. If creating a publisher and subscriber you should set a new topic name so that your publisher will publish to a new topic that your subscriber will subscribe from. Otherwise you can reuse a topic so that you can test your task against the publisher or subscriber that shares that topic.  You should also add yourself to the com.google.pubsub.flic.output.SheetsService code so that statistics from your client will be sent to Google Sheets with other tests.

Then, you need to create a startup script for your machine. This can be copied from src/main/resources/gce/ where you'll find many. Most just install Java and then run `driver.jar` setting the main class using the `-cp` flag.

#### Other languages

You must write a server that implements Adapter and response to Start and Execute commands. Start will give you the parameters for the load test, and you should respond to Execute with the latencies it took to complete the operation. You can see an example in python_src. Your server should listen on port 6000. Then you just need to create a startup script, modify the enums as described above, and place the binary in src/main/resources/gce so that it will be uploaded to Google Cloud Storage which you can pull from in your startup script in order to execute your binary. You can see an example of such a startup script in src/main/resources/gce/cps-gcloud-python-publisher_startup_script.sh. You must start your server before executing the jar.
