load("@rules_java//java:defs.bzl", "java_library")
load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)

gerrit_plugin(
    name = "events-aws-kinesis",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: events-aws-kinesis",
        "Gerrit-InitStep: com.googlesource.gerrit.plugins.kinesis.InitConfig",
        "Gerrit-Module: com.googlesource.gerrit.plugins.kinesis.Module",
        "Implementation-Title: Gerrit events listener to send events to AWS Kinesis broker",
        "Implementation-URL: https://gerrit.googlesource.com/plugins/events-aws-kinesis",
    ],
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        ":events-broker-neverlink",
        "@amazon-auth//jar",
        "@amazon-aws-core//jar",
        "@amazon-cloudwatch//jar",
        "@amazon-dynamodb//jar",
        "@amazon-http-client-spi//jar",
        "@amazon-kinesis-client//jar",
        "@amazon-kinesis//jar",
        "@amazon-netty-nio-client//jar",
        "@amazon-profiles//jar",
        "@amazon-regions//jar",
        "@amazon-sdk-core//jar",
        "@amazon-utils//jar",
        "@apache-commons-io//jar",
        "@apache-commons-lang3//jar",
        "@aws-glue-schema-serde//jar",
        "@aws-java-sdk-core//jar",
        "@awssdk-cbor-protocol//jar",
        "@awssdk-json-protocol//jar",
        "@awssdk-kinesis-producer//jar",
        "@awssdk-metrics-spi//jar",
        "@awssdk-protocol-core//jar",
        "@awssdk-query-protocol//jar",
        "@commons-codec//jar",
        "@commons-lang//jar",
        "@io-netty-all//jar",
        "@jackson-annotations//jar",
        "@jackson-core//jar",
        "@jackson-databind//jar",
        "@jackson-dataformat-cbor//jar",
        "@javax-xml-bind//jar",
        "@reactive-streams//jar",
        "@reactor-core//jar",
        "@rxjava//jar",
    ],
)

junit_tests(
    name = "events-aws-kinesis_tests",
    timeout = "long",
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["events-aws-kinesis"],
    deps = [
        ":events-aws-kinesis__plugin_test_deps",
        "//plugins/events-broker",
        "@amazon-http-client-spi//jar",
        "@amazon-kinesis-client//jar",
        "@amazon-kinesis//jar",
        "@awssdk-kinesis-producer//jar",
    ],
)

java_library(
    name = "events-aws-kinesis__plugin_test_deps",
    testonly = 1,
    visibility = ["//visibility:public"],
    exports = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":events-aws-kinesis__plugin",
        "@jackson-annotations//jar",
        "@testcontainers//jar",
        "@docker-java-api//jar",
        "@docker-java-transport//jar",
        "@duct-tape//jar",
        "@visible-assertions//jar",
        "@jna//jar",
        "@amazon-regions//jar",
        "@amazon-auth//jar",
        "@amazon-kinesis//jar",
        "@amazon-aws-core//jar",
        "@amazon-sdk-core//jar",
        "@amazon-profiles//jar",
        "@aws-java-sdk-core//jar",
        "@awssdk-url-connection-client//jar",
        "@amazon-dynamodb//jar",
        "@testcontainer-localstack//jar",
    ],
)

java_library(
    name = "events-broker-neverlink",
    neverlink = 1,
    exports = ["//plugins/events-broker"],
)
