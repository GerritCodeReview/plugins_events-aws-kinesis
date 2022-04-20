load("//tools/bzl:maven_jar.bzl", "maven_jar")

AWS_SDK_VER = "2.16.19"
AWS_KINESIS_VER = "2.3.4"
JACKSON_VER = "2.10.4"

def external_plugin_deps():
    maven_jar(
        name = "junit-platform",
        artifact = "org.junit.platform:junit-platform-commons:1.4.0",
        sha1 = "34d9983705c953b97abb01e1cd04647f47272fe5",
    )

    maven_jar(
        name = "amazon-kinesis-client",
        artifact = "software.amazon.kinesis:amazon-kinesis-client:" + AWS_KINESIS_VER,
        sha1 = "6bb6fcbc5a0f6fd6085f3b1589e738485b0b7867",
    )

    maven_jar(
        name = "amazon-kinesis",
        artifact = "software.amazon.awssdk:kinesis:" + AWS_SDK_VER,
        sha1 = "bec13fc5ef9225d1a10f13fbe1de8cb114448cf8",
    )

    maven_jar(
        name = "amazon-dynamodb",
        artifact = "software.amazon.awssdk:dynamodb:" + AWS_SDK_VER,
        sha1 = "33ec7d291973658779b5777db2a0214a5c469e81",
    )

    maven_jar(
        name = "amazon-cloudwatch",
        artifact = "software.amazon.awssdk:cloudwatch:" + AWS_SDK_VER,
        sha1 = "7585fbe349a92e0a9f040e4194ac89ca32e7983d",
    )

    maven_jar(
        name = "amazon-regions",
        artifact = "software.amazon.awssdk:regions:" + AWS_SDK_VER,
        sha1 = "089f4f3d3ef20b2486f09e71da638c03100eab64",
    )

    maven_jar(
        name = "amazon-netty-nio-client",
        artifact = "software.amazon.awssdk:netty-nio-client:" + AWS_SDK_VER,
        sha1 = "bb674feda8417513a647c7aa8cba9a537068d099",
    )

    maven_jar(
        name = "amazon-utils",
        artifact = "software.amazon.awssdk:utils:" + AWS_SDK_VER,
        sha1 = "53edaa1f884682ac3091293eff3eb024ed0e36bb",
    )

    maven_jar(
        name = "amazon-sdk-core",
        artifact = "software.amazon.awssdk:sdk-core:" + AWS_SDK_VER,
        sha1 = "02a60fd9c138048272ef8b6c80ae67491dd386a9",
    )

    maven_jar(
        name = "amazon-aws-core",
        artifact = "software.amazon.awssdk:aws-core:" + AWS_SDK_VER,
        sha1 = "0f50f5cf2698a0de7d2d77322cbf3fb13f76187f",
    )

    maven_jar(
        name = "amazon-http-client-spi",
        artifact = "software.amazon.awssdk:http-client-spi:" + AWS_SDK_VER,
        sha1 = "e4027e7e0cb064602100b34e19f131983f76f872",
    )

    maven_jar(
        name = "amazon-auth",
        artifact = "software.amazon.awssdk:auth:" + AWS_SDK_VER,
        sha1 = "4163754b2a0eadcb569a35f0666fd5d859e43ef8",
    )

    maven_jar(
        name = "reactive-streams",
        artifact = "org.reactivestreams:reactive-streams:1.0.2",
        sha1 = "323964c36556eb0e6209f65c1cef72b53b461ab8",
    )

    maven_jar(
        name = "reactor-core",
        artifact = "io.projectreactor:reactor-core:3.4.3",
        sha1 = "df23dbdf95f892f7a04292d040fd8b308bd66602",
    )

    maven_jar(
        name = "rxjava",
        artifact = "io.reactivex.rxjava2:rxjava:2.1.14",
        sha1 = "20dbf7496e417da474eda12717bf4653dbbd5a6b",
    )

    maven_jar(
        name = "jackson-databind",
        artifact = "com.fasterxml.jackson.core:jackson-databind:" + JACKSON_VER,
        sha1 = "76e9152e93d4cf052f93a64596f633ba5b1c8ed9",
    )

    maven_jar(
        name = "jackson-annotations",
        artifact = "com.fasterxml.jackson.core:jackson-annotations:" + JACKSON_VER,
        sha1 = "6ae6028aff033f194c9710ad87c224ccaadeed6c",
    )

    maven_jar(
        name = "jackson-core",
        artifact = "com.fasterxml.jackson.core:jackson-core:" + JACKSON_VER,
        sha1 = "8796585e716440d6dd5128b30359932a9eb74d0d",
    )

    maven_jar(
        name = "jackson-dataformat-cbor",
        artifact = "com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:" + JACKSON_VER,
        sha1 = "c854bb2d46138198cb5d4aae86ef6c04b8bc1e70",
    )

    maven_jar(
        name = "events-broker",
        artifact = "com.gerritforge:events-broker:3.5.0-alpha-202108041529",
        sha1 = "309fe8cc08c46593d9990d4e5c448cc85e5a62b0",
    )

    maven_jar(
        name = "io-netty-all",
        artifact = "io.netty:netty-all:4.1.51.Final",
        sha1 = "5e5f741acc4c211ac4572c31c7e5277ec465e4e4",
    )

    maven_jar(
        name = "awssdk-query-protocol",
        artifact = "software.amazon.awssdk:aws-query-protocol:" + AWS_SDK_VER,
        sha1 = "4c88c66daa5039813e879b324636d15fa2802787",
    )

    maven_jar(
        name = "awssdk-protocol-core",
        artifact = "software.amazon.awssdk:protocol-core:" + AWS_SDK_VER,
        sha1 = "6200c1617f87eed0216c6afab35bab2403da140c",
    )

    maven_jar(
        name = "awssdk-json-protocol",
        artifact = "software.amazon.awssdk:aws-json-protocol:" + AWS_SDK_VER,
        sha1 = "16449e555f61607b917dc7f242c1928298de9bdd",
    )

    maven_jar(
        name = "awssdk-cbor-protocol",
        artifact = "software.amazon.awssdk:aws-cbor-protocol:" + AWS_SDK_VER,
        sha1 = "7353a868437576b9e4911779ae66a85ef6be0d9e",
    )

    maven_jar(
        name = "awssdk-metrics-spi",
        artifact = "software.amazon.awssdk:metrics-spi:" + AWS_SDK_VER,
        sha1 = "d8669974b412766751b5eaf9c1edad908bfe5c38",
    )

    maven_jar(
        name = "amazon-profiles",
        artifact = "software.amazon.awssdk:profiles:" + AWS_SDK_VER,
        sha1 = "5add2a843de43bd0acf45e1ab8c2b94c3638dd66",
    )

    maven_jar(
        name = "apache-commons-lang3",
        artifact = "org.apache.commons:commons-lang3:3.12.0",
        sha1 = "c6842c86792ff03b9f1d1fe2aab8dc23aa6c6f0e",
    )

    maven_jar(
        name = "commons-lang",
        artifact = "commons-lang:commons-lang:2.6",
        sha1 = "0ce1edb914c94ebc388f086c6827e8bdeec71ac2",
    )

    TESTCONTAINERS_VERSION = "1.15.3"

    maven_jar(
        name = "testcontainers",
        artifact = "org.testcontainers:testcontainers:" + TESTCONTAINERS_VERSION,
        sha1 = "95c6cfde71c2209f0c29cb14e432471e0b111880",
    )

    maven_jar(
        name = "testcontainer-localstack",
        artifact = "org.testcontainers:localstack:" + TESTCONTAINERS_VERSION,
        sha1 = "7aa69995bdaafb4b06e69fdab9bd98c4fddee43d",
    )

    maven_jar(
        name = "duct-tape",
        artifact = "org.rnorth.duct-tape:duct-tape:1.0.8",
        sha1 = "92edc22a9ab2f3e17c9bf700aaee377d50e8b530",
    )

    maven_jar(
        name = "visible-assertions",
        artifact = "org.rnorth.visible-assertions:visible-assertions:2.1.2",
        sha1 = "20d31a578030ec8e941888537267d3123c2ad1c1",
    )

    maven_jar(
        name = "jna",
        artifact = "net.java.dev.jna:jna:5.5.0",
        sha1 = "0e0845217c4907822403912ad6828d8e0b256208",
    )

    DOCKER_JAVA_VERS = "3.2.8"

    maven_jar(
        name = "docker-java-api",
        artifact = "com.github.docker-java:docker-java-api:" + DOCKER_JAVA_VERS,
        sha1 = "4ac22a72d546a9f3523cd4b5fabffa77c4a6ec7c",
    )

    maven_jar(
        name = "docker-java-transport",
        artifact = "com.github.docker-java:docker-java-transport:" + DOCKER_JAVA_VERS,
        sha1 = "c3b5598c67d0a5e2e780bf48f520da26b9915eab",
    )

    maven_jar(
        name = "aws-java-sdk-core",
        artifact = "com.amazonaws:aws-java-sdk-core:1.11.960",
        sha1 = "18b6b2a5cb83a0e2e33a593302b5dbe0ca2ade64",
    )

    maven_jar(
        name = "awssdk-url-connection-client",
        artifact = "software.amazon.awssdk:url-connection-client:" + AWS_SDK_VER,
        sha1 = "b84ac8bae45841bc65af3c4f55164d9a3399b653",
    )

    maven_jar(
        name = "awssdk-kinesis-producer",
        artifact = "com.amazonaws:amazon-kinesis-producer:0.14.6",
        sha1 = "7f83582df816dccc5217f05ece309a5cd8c7a9a5",
    )

    maven_jar(
        name = "aws-glue-schema-serde",
        artifact = "software.amazon.glue:schema-registry-serde:1.0.0",
        sha1 = "30815b670f89876465caa69b47e6df6fd6875d0f",
    )

    maven_jar(
        name = "apache-commons-io",
        artifact = "commons-io:commons-io:2.4",
        sha1 = "b1b6ea3b7e4aa4f492509a4952029cd8e48019ad",
    )

    maven_jar(
        name = "javax-xml-bind",
        artifact = "javax.xml.bind:jaxb-api:2.3.1",
        sha1 = "8531ad5ac454cc2deb9d4d32c40c4d7451939b5d",
    )
