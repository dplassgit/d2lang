"""Makes non-module repositories available to this Bazel module.

Based on:

- https://bazel.build/external/migration#fetch-deps-module-extensions
"""
load(
    "@google_bazel_common//:workspace_defs.bzl",
    "google_common_workspace_rules",
)

def _d2_impl(_module_ctx):
    google_common_workspace_rules()

d2 = module_extension(
    implementation = _d2_impl,
)
