# Copyright (c) 2021 VMware, Inc.
# SPDX-License-Identifier: Apache-2.0
import json
from unittest import mock

import pytest
from taurus.api.plugin.plugin_registry import PluginException
from taurus.vdk.core.errors import UserCodeError
from taurus.vdk.core.errors import VdkConfigurationError
from taurus.vdk.ingest_over_http import IngestOverHttp

payload: dict = {
    "@table": "test_destination_table",
    "@id": "test_id",
    "some_data": "some_test_data",
}


def mocked_requests_post(url, *args, **kwargs):
    class MockResponse:
        def __init__(self, json_data, status_code):
            self.json_data = json_data
            self.status_code = status_code
            self.text = ""

        def json(self):
            return self.json_data

    if url == "http://example.com/data-source":
        return MockResponse(payload, 200)

    return MockResponse(None, 404)


@mock.patch("requests.post", side_effect=mocked_requests_post)
def test_ingest_over_http(mock_post):
    http_ingester: IngestOverHttp = IngestOverHttp()
    http_ingester.ingest_payload(
        payload=[payload],
        destination_table="test_table",
        target="http://example.com/data-source",
    )

    assert len(mock_post.call_args_list) == 1

    mock_post.assert_called_with(
        headers={"Content-Type": "application/octet-stream"},
        json=json.dumps(payload),
        url="http://example.com/data-source",
        verify=False,
    )


@mock.patch("requests.post", side_effect=mocked_requests_post)
def test_ingest_over_http_missing_target(mock_post):
    http_ingester: IngestOverHttp = IngestOverHttp()
    with pytest.raises(VdkConfigurationError):
        http_ingester.ingest_payload(
            payload=[payload],
            destination_table="test_table",
        )


# TODO: test the other failure scenarios