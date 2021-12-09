# Copyright 2021 VMware, Inc.
# SPDX-License-Identifier: Apache-2.0
"""
Functional test aiming at verifying the end-to-end operation of the
ingestion plugins chaining functionality
"""
import logging
import os
from typing import List
from unittest import mock

from click.testing import Result
from functional.ingestion import utils
from vdk.api.plugin.hook_markers import hookimpl
from vdk.api.plugin.plugin_input import IIngesterPlugin
from vdk.internal.builtin_plugins.run.job_context import JobContext
from vdk.plugin.test_utils.util_funcs import cli_assert_equal
from vdk.plugin.test_utils.util_funcs import CliEntryBasedTestRunner
from vdk.plugin.test_utils.util_plugins import IngestIntoMemoryPlugin

# TODO: Add test cases for the following scenarios
# 1) INGEST_PAYLOAD_PREPROCESS_SEQUENCE=A,B in job code method = C
# 2) INGEST_PAYLOAD_PREPROCESS_SEQUENCE=A,B and VDK_INGEST_METHOD_DEFAULT=C
# 3) INGEST_PAYLOAD_PREPROCESS_SEQUENCE=A,B and VDK_INGEST_METHOD_DEFAULT=C and
#    job code is D
# 4) INGEST_PAYLOAD_PREPROCESS_SEQUENCE=""
# 5) INGEST_PAYLOAD_PREPROCESS_SEQUENCE=A,B - A,B are used for pre-processing,
#    B is used for ingesting and post-processing


log = logging.getLogger(__name__)


class ConvertPayloadValuesToString(IIngesterPlugin):
    def pre_ingest_process(self, payload: List[dict]) -> List[dict]:
        return [{k: str(v) for (k, v) in i.items()} for i in payload]

    @hookimpl
    def initialize_job(self, context: JobContext) -> None:
        log.info("Initialize data job with ConvertPayloadValuesToString Plugin.")

        context.ingester.add_ingester_factory_method("convert-to-string", lambda: self)


@mock.patch.dict(
    os.environ,
    {
        "INGEST_PAYLOAD_PREPROCESS_SEQUENCE": "convert-to-string",
        "INGEST_METHOD_DEFAULT": "memory",
    },
)
def test_chained_ingest_no_direct_method_passed():
    pre_ingest_plugin = ConvertPayloadValuesToString()
    ingest_plugin = IngestIntoMemoryPlugin()
    runner = CliEntryBasedTestRunner(pre_ingest_plugin, ingest_plugin)

    # TODO: Uncomment below lines when implementation is complete
    # Use a sample data job, in which `method` argument is not passed to
    # ingestion method calls.
    # result: Result = runner.invoke(["run", utils.job_path("job-with-no-method")])

    # cli_assert_equal(0, result)

    # expected_object = dict(
    #     int_key=str(42),
    #     str_key="example_str",
    #     bool_key=str(True),
    #     float_key=str(1.23),
    #     nested=str(dict(key="value")),
    # )
    # assert ingest_plugin.payloads[0].payload[0] == expected_object
    # assert ingest_plugin.payloads[0].destination_table == "object_table"

    # expected_rows_object = {"first": "two", "second": "2"}
    # assert ingest_plugin.payloads[1].payload[0] == expected_rows_object
    # assert ingest_plugin.payloads[1].destination_table == "tabular_table"


@mock.patch.dict(
    os.environ,
    {"INGEST_PAYLOAD_PREPROCESS_SEQUENCE": "convert-to-string"},
)
def test_chained_ingest_direct_method_passed():
    pre_ingest_plugin = ConvertPayloadValuesToString()
    ingest_plugin = IngestIntoMemoryPlugin()
    runner = CliEntryBasedTestRunner(pre_ingest_plugin, ingest_plugin)

    # Use a sample data job, in which `method` argument is passed to ingestion
    # method calls.
    result: Result = runner.invoke(["run", utils.job_path("test-ingest-job")])

    cli_assert_equal(0, result)

    # TODO: Uncomment below lines when implementation is complete
    # expected_object = dict(
    #     int_key=str(42),
    #     str_key="example_str",
    #     bool_key=str(True),
    #     float_key=str(1.23),
    #     nested=str(dict(key="value")),
    # )
    # assert ingest_plugin.payloads[0].payload[0] == expected_object
    assert ingest_plugin.payloads[0].destination_table == "object_table"

    # TODO: Uncomment below lines when implementation is complete
    # expected_rows_object = {"first": "two", "second": "2"}
    # assert ingest_plugin.payloads[1].payload[0] == expected_rows_object
    assert ingest_plugin.payloads[1].destination_table == "tabular_table"
