# Build

The kinesis-events plugin can be build as a regular 'in-tree' plugin. That means
that is required to clone a Gerrit source tree first and then to have the plugin
source directory into the `/plugins` path.

Additionally, the `plugins/external_plugin_deps.bzl` file needs to be updated to
match the kinesis-events plugin one.

```shell script
git clone --recursive https://gerrit.googlesource.com/gerrit
cd gerrit
git clone "https://review.gerrithub.io/GerritForge/kinesis-events" plugins/kinesis-events
ln -sf plugins/kinesis-events/external_plugin_deps.bzl plugins/.
bazelisk build plugins/kinesis-events
```

The output is created in

```
bazel-genfiles/plugins/kinesis-events/kinesis-events.jar
```