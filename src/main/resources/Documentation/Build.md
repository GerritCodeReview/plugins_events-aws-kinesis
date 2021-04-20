# Build

The events-aws-kinesis plugin can be build as a regular 'in-tree' plugin. That means
that is required to clone a Gerrit source tree first and then to have the plugin
source directory into the `/plugins` path.

Additionally, the `plugins/external_plugin_deps.bzl` file needs to be updated to
match the events-aws-kinesis plugin one.

```shell script
git clone --recursive https://gerrit.googlesource.com/gerrit
cd gerrit
git clone "https://gerrit.googlesource.com/plugins/events-aws-kinesis" plugins/events-aws-kinesis
ln -sf plugins/events-aws-kinesis/external_plugin_deps.bzl plugins/.
bazelisk build plugins/events-aws-kinesis
```

The output is created in

```
bazel-genfiles/plugins/events-aws-kinesis/events-aws-kinesis.jar
```